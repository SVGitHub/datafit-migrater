package com.datafit.migrater.service;

import com.datafit.migrater.domain.*;
import com.datafit.migrater.repo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * JobRunnerService: enhanced to
 * - fetch mapping for file patterns
 * - process CSV, Parquet, Excel (basic)
 * - apply ValidationService rules for each row
 * - collect errors, write to temp CSV and upload to S3 when over threshold
 *
 * Note: This is a best-effort implementation; production must add robust error handling and resource cleanup.
 */
@Service
public class JobRunnerService {
    private final JobRepository jobs; private final JobErrorRepository jobErrors; private final SettingsRepository settingsRepo;
    private final ProjectRepository projectRepo; private final MappingRepository mappingRepo;
    private final ValidationService validationService; private final ParquetProcessor parquetProcessor; private final S3Service s3Service; private final RedshiftCopyService redshiftCopyService;
    private final ObjectMapper om = new ObjectMapper();

    @Autowired
    private SftpService sftpService;

    public JobRunnerService(JobRepository jobs, JobErrorRepository jobErrors, SettingsRepository settingsRepo, ProjectRepository projectRepo, MappingRepository mappingRepo, ValidationService validationService, ParquetProcessor parquetProcessor, S3Service s3Service, RedshiftCopyService redshiftCopyService){
        this.jobs = jobs; this.jobErrors = jobErrors; this.settingsRepo = settingsRepo; this.projectRepo = projectRepo; this.mappingRepo = mappingRepo; this.validationService = validationService; this.parquetProcessor = parquetProcessor; this.s3Service = s3Service; this.redshiftCopyService = redshiftCopyService;
    }

    public void runAsync(UUID jobId){
        try(var exec = Executors.newVirtualThreadPerTaskExecutor()){
            exec.submit(() -> { try { run(jobId); } catch(Exception e){ e.printStackTrace(); } });
        }
    }

    @Transactional
    public void run(UUID jobId) throws Exception {
        Job job = jobs.findById(jobId).orElseThrow();
        job.setStatus(JobStatus.RUNNING); job.setStartedAt(OffsetDateTime.now()); jobs.save(job);
        Project proj = job.getProject();
        Settings cfg = settingsRepo.findByProjectId(proj.getId()).orElseGet(()->{ Settings s=new Settings(); s.setProject(proj); return settingsRepo.save(s); });

        Path sourceDir = null; List<Path> files = new ArrayList<>();
        if("SFTP".equalsIgnoreCase(job.getSourceType())){
            // fetch files from SFTP into a temp dir
            Path tempDir = Files.createTempDirectory("sftp-"+job.getId());
            // project knownHosts may be stored; write to temp file if present
            Path knownHostsFile = null;
            if(proj.getKnownHosts()!=null && !proj.getKnownHosts().isBlank()){
                knownHostsFile = tempDir.resolve("known_hosts");
                Files.writeString(knownHostsFile, proj.getKnownHosts());
            }
            // TODO: sourcePath expected to be remote path; credentials currently expected in Settings or Project (not implemented) - using Settings placeholders
            Settings s = cfg;
            String host = s.getSftpHost();
            int port = s.getSftpPort()==null?22:s.getSftpPort();
            String user = s.getSftpUser();
            String password = s.getSftpPassword();
            // call fetchToLocal (this requires real credentials configured)
            try{
                List<Path> downloaded = sftpService.fetchToLocal(host, port, user, password, job.getSourcePath(), tempDir, job.getFileGlob(), knownHostsFile);
                files.addAll(downloaded);
                sourceDir = tempDir;
            } catch(Exception ex){ throw new RuntimeException("SFTP fetch failed: "+ex.getMessage(), ex); }
        } else {
            sourceDir = Path.of(job.getSourcePath()==null?".":job.getSourcePath());
            try(var ds = Files.newDirectoryStream(sourceDir, job.getFileGlob()==null?"*":job.getFileGlob())){ for(Path p: ds) files.add(p); }
        }

        long total=0, success=0, errors=0;
        Path errFile = Files.createTempFile("job-"+job.getId()+"-errors-",".csv");
        try(BufferedWriter errW = Files.newBufferedWriter(errFile); CSVPrinter errPr = new CSVPrinter(errW, CSVFormat.DEFAULT.withHeader("row","reason","raw"))){
            for(Path f: files){
                String fname = f.getFileName().toString().toLowerCase();
                if(fname.endsWith(".csv")){
                    try(BufferedReader br = Files.newBufferedReader(f)){
                        String header = br.readLine();
                        List<String> headers = header==null?List.of():List.of(header.split(","));
                        String line; long rowNum=1;
                        while((line = br.readLine())!=null){
                            rowNum++; total++;
                            Map<String,Object> row = mapCsvRow(headers, line);
                            MappingDef mapping = findMappingForFile(proj.getId(), f.getFileName().toString());
                            Object rawRules = mapping == null ? null : validationService.parseRules(mapping.getMappingJson());
                            List<ValidationService.Rule> rules = rawRules == null
                                    ? Collections.emptyList()
                                    : om.convertValue(rawRules, new TypeReference<List<ValidationService.Rule>>() {});                            var res = validationService.apply(rules, row);
                            if(!res.errors.isEmpty()){
                                errors += res.errors.size();
                                for(String er: res.errors) errPr.printRecord(rowNum, er, line);
                            } else {
                                success++;
                            }
                        }
                    }
                } else if(fname.endsWith(".parquet")){
                    List<String> composite = Arrays.asList((cfg.getCompositeKeyCsv()==null?"":cfg.getCompositeKeyCsv()).split(",")).stream().filter(s->!s.isBlank()).toList();
                    var result = parquetProcessor.process(f, Files.createTempDirectory("parquet-chunks"), composite, cfg.getChunkSizeMb()==null?512:cfg.getChunkSizeMb(), cfg.getMaxOpenCsvWriters()==null?16:cfg.getMaxOpenCsvWriters());
                    total += result.totalRows;
                    // upload chunks to S3 and call COPY for Redshift if configured
                    if((proj.getDbType()!=null && proj.getDbType().name().equalsIgnoreCase("REDSHIFT")) && proj.getS3Bucket()!=null){
                        for(Path chunk: result.csvChunks){
                            String keyPrefix = (cfg.getS3Folder()==null?proj.getS3Folder():cfg.getS3Folder());
                            String s3path = s3Service.uploadFile(proj.getS3Bucket(), keyPrefix + "/staged/" + job.getId(), chunk);
                            // call Redshift COPY for the table
                            redshiftCopyService.copyFromS3(proj, job.getTargetSchema(), job.getTargetTable(), s3path);
                        }
                    } else {
                        // For non-redshift, we would collect rows and do bulk upserts - TODO
                        for(Path chunk: result.csvChunks){
                            try(BufferedReader br = Files.newBufferedReader(chunk)){
                                String header = br.readLine();
                                List<String> headers = header==null?List.of():List.of(header.split(","));
                                String line; long rowNum=1;
                                while((line=br.readLine())!=null){
                                    rowNum++;
                                    Map<String,Object> row = mapCsvRow(headers, line);
                                    MappingDef mapping = findMappingForFile(proj.getId(), f.getFileName().toString());
                                    var rules = mapping==null? List.of() : validationService.parseRules(mapping.getMappingJson());
                                    var res = validationService.apply(rules, row);
                                    if(!res.errors.isEmpty()){
                                        errors += res.errors.size();
                                        for(String er: res.errors) errPr.printRecord(rowNum, er, line);
                                    } else { success++; }
                                }
                            }
                        }
                    }
                } else if(fname.endsWith(".xlsx") || fname.endsWith(".xls")){
                    try(InputStream is = Files.newInputStream(f); XSSFWorkbook wb = new XSSFWorkbook(is)){
                        var sh = wb.getSheetAt(0);
                        Iterator<Row> it = sh.iterator();
                        List<String> headers = new ArrayList<>();
                        if(it.hasNext()){
                            var r = it.next();
                            for(var c: r) headers.add(c.getStringCellValue());
                        }
                        long rowNum=1;
                        while(it.hasNext()){
                            var r = it.next(); rowNum++; total++;
                            Map<String,Object> row = new HashMap<>();
                            for(int i=0;i<headers.size();i++){
                                var cell = r.getCell(i); String v = cell==null?null:cell.toString(); row.put(headers.get(i), v);
                            }
                            MappingDef mapping = findMappingForFile(proj.getId(), f.getFileName().toString());
                            var rules = mapping==null? List.of() : validationService.parseRules(mapping.getMappingJson());
                            var res = validationService.apply(rules, row);
                            if(!res.errors.isEmpty()){ errors += res.errors.size(); for(String er: res.errors) errPr.printRecord(rowNum, er, row.toString()); } else { success++; }
                        }
                    }
                }

                if(errors > (cfg.getRowErrorLimit()==null?10000:cfg.getRowErrorLimit())){
                    String s3Path = s3Service.uploadFile(cfg.getS3Bucket()==null?proj.getS3Bucket():cfg.getS3Bucket(), (cfg.getS3Folder()==null?proj.getS3Folder():cfg.getS3Folder())+"/errors/"+job.getId(), errFile);
                    job.setErrorFileS3(s3Path);
                    try{ Files.deleteIfExists(errFile); } catch(Exception ignore){}
                    break;
                }
            } // files
        }

        job.setTotalRows(total); job.setSuccessRows(success); job.setErrorRows(errors);
        if(job.getErrorFileS3()==null && Files.exists(errFile)) job.setErrorFileLocal(errFile.toAbsolutePath().toString());
        job.setStatus(errors==0? JobStatus.SUCCEEDED : JobStatus.FAILED);
        job.setFinishedAt(OffsetDateTime.now());
        jobs.save(job);
    }

    private Map<String,Object> mapCsvRow(List<String> headers, String line){
        String[] parts = line.split(","); Map<String,Object> row = new LinkedHashMap<>();
        for(int i=0;i<headers.size() && i<parts.length;i++) row.put(headers.get(i), parts[i]);
        return row;
    }

    private MappingDef findMappingForFile(java.util.UUID projectId, String fileName){
        List<MappingDef> maps = mappingRepo.findByProjectId(projectId);
        for(MappingDef m: maps){
            try{
                String pattern = m.getFilePattern();
                if(pattern==null || pattern.isBlank()) continue;
                if(fileName.matches(pattern)) return m;
            }catch(Exception ignored){}
        }
        return null;
    }
}
