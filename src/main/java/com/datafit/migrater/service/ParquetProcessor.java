package com.datafit.migrater.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.parquet.hadoop.*;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * ParquetProcessor reads records from a parquet file and writes them to CSV chunk files.
 * Chunking strategy:
 *  - Rows are grouped by composite key (values concatenated) so that rows with same key go to same file.
 *  - A simple size-based rotation is used: if the current file exceeds chunkSizeBytes, we rotate to a new file.
 *
 * This implementation is memory-conscious: iterates records and writes directly to CSV printers.
 */
@Service
public class ParquetProcessor {

    public static class Result { public List<Path> csvChunks = new ArrayList<>(); public long totalRows=0; }

    public Result process(Path parquetFile, Path outDir, List<String> compositeKeyColumns, long chunkSizeMb, int maxOpenCsvWriters) throws Exception {
        Files.createDirectories(outDir);
        long chunkSizeBytes = Math.max(1, chunkSizeMb) * 1024 * 1024;
        Map<String, CSVPrinter> printers = new LinkedHashMap<>();
        Map<String, BufferedWriter> writers = new LinkedHashMap<>();
        Map<String, Path> writerPaths = new LinkedHashMap<>();
        Result result = new Result();

        try (ParquetReader<GenericRecord> reader = AvroParquetReader.<GenericRecord>builder(
                new org.apache.hadoop.fs.Path(parquetFile.toUri().toString())
        ).build()) {
            GenericRecord record;
            long row = 0;
            while ((record = reader.read()) != null) {
                row++;
                result.totalRows++;
                // build composite key
                String key = buildKey(record, compositeKeyColumns);
                CSVPrinter pr = printers.get(key);
                if (pr == null) {
                    // open new writer for key
                    String base = parquetFile.getFileName().toString().replaceAll("\\.parquet$","") + "-" + key + "-" + System.currentTimeMillis() + ".csv";
                    Path out = outDir.resolve(base);
                    BufferedWriter bw = Files.newBufferedWriter(out, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    pr = new CSVPrinter(bw, CSVFormat.DEFAULT);
                    printers.put(key, pr);
                    writers.put(key, bw);
                    writerPaths.put(key, out);
                    // enforce max open writers by closing oldest if needed
                    if (writers.size() > maxOpenCsvWriters) {
                        String oldest = writers.keySet().iterator().next();
                        printers.get(oldest).close();
                        writers.get(oldest).close();
                        printers.remove(oldest);
                        writers.remove(oldest);
                        result.csvChunks.add(writerPaths.remove(oldest));
                    }
                }
                // write record as comma-separated values - iterate fields in record schema order
                List<String> vals = new ArrayList<>();
                for (var field : record.getSchema().getFields()) {
                    Object v = record.get(field.name());
                    vals.add(v==null?"":v.toString());
                }
                pr.printRecord(vals);
                // rotate file if above size
                Path pth = writerPaths.get(key);
                if (Files.size(pth) > chunkSizeBytes) {
                    pr.close(); writers.get(key).close();
                    printers.remove(key); writers.remove(key);
                    result.csvChunks.add(pth);
                }
            }
        } finally {
            // close remaining printers and record their paths
            for (var e : printers.entrySet()) {
                try { e.getValue().close(); } catch(IOException ignore){}
            }
            for (var e : writers.entrySet()) {
                try { e.getValue().close(); } catch(IOException ignore){}
            }
            result.csvChunks.addAll(writerPaths.values());
        }
        return result;
    }

    private String buildKey(GenericRecord record, List<String> compositeKeyColumns) {
        if (compositeKeyColumns==null || compositeKeyColumns.isEmpty()) return "default";
        List<String> parts = new ArrayList<>();
        for (String k: compositeKeyColumns){
            Object v = record.get(k);
            parts.add(v==null?"":v.toString().replaceAll("[^a-zA-Z0-9_-]","_"));
        }
        return String.join("_", parts);
    }
}
