package com.datafit.migrater.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple manager for CSV printers with LRU closing when maxOpen reached.
 */
public class CSVWriterManager {
    private final int maxOpen;
    private final Map<String, CSVPrinter> printers = new LinkedHashMap<>();

    public CSVWriterManager(int maxOpen) { this.maxOpen = Math.max(1, maxOpen); }

    public synchronized CSVPrinter getOrCreate(String key, Path path) throws IOException {
        if (printers.containsKey(key)) return printers.get(key);
        if (printers.size() >= maxOpen) {
            var it = printers.entrySet().iterator();
            var oldest = it.next();
            oldest.getValue().close();
            it.remove();
        }
        BufferedWriter bw = Files.newBufferedWriter(path);
        CSVPrinter pr = new CSVPrinter(bw, CSVFormat.DEFAULT);
        printers.put(key, pr);
        return pr;
    }

    public synchronized void closeAll() {
        for (var p: printers.values()) {
            try { p.close(); } catch(IOException ignore) {}
        }
        printers.clear();
    }
}
