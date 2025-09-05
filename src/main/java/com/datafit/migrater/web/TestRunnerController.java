package com.datafit.migrater.web;
import com.datafit.migrater.repo.ProjectRepository;
import com.datafit.migrater.domain.Project;
import com.datafit.migrater.service.S3Service;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.*;

/**
 * Endpoint to run quick connectivity checks and report results in JSON.
 * Frontend can call /api/tests/run to execute basic integration checks.
 * Also supports running `mvn test` on the server (useful for local dev), with a timeout.
 */
@RestController @RequestMapping("/api/tests")
public class TestRunnerController {

    private final ProjectRepository projects;
    private final S3Service s3;

    public TestRunnerController(ProjectRepository projects, S3Service s3){ this.projects=projects; this.s3=s3; }

    @PostMapping("/run")
    public Map<String,Object> run(@RequestBody Map<String,Object> req){
        Map<String,Object> out = new LinkedHashMap<>();
        try{
            String projectId = (String)req.get("projectId");
            Project p = projects.findById(java.util.UUID.fromString(projectId)).orElseThrow();
            Map<String,Object> db = new LinkedHashMap<>();
            try(Connection c = DriverManager.getConnection(p.getJdbcUrl(), p.getDbUser(), p.getDbPassword())){ db.put("ok", true); } catch(Exception ex){ db.put("ok", false); db.put("error", ex.getMessage()); }
            out.put("db", db);
            Map<String,Object> s3r = new LinkedHashMap<>();
            try{
                if(p.getS3Bucket()!=null && !p.getS3Bucket().isBlank()){
                    s3r.put("ok", true); s3r.put("bucket", p.getS3Bucket());
                } else {
                    s3r.put("ok", false); s3r.put("error", "no bucket configured in project");
                }
            }catch(Exception ex){ s3r.put("ok", false); s3r.put("error", ex.getMessage()); }
            out.put("s3", s3r);
        }catch(Exception ex){ out.put("error", ex.getMessage()); }
        return out;
    }

    @PostMapping("/run-mvn")
    public Map<String,Object> runMavenTests() throws Exception {
        Map<String,Object> out = new LinkedHashMap<>();
        ProcessBuilder pb = new ProcessBuilder("mvn","-DskipTests=false","test");
        pb.directory(new java.io.File("."));
        pb.redirectErrorStream(true);
        Process p = pb.start();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            long start = System.currentTimeMillis();
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
                if (System.currentTimeMillis() - start > 10 * 60 * 1000) {
                    p.destroyForcibly();
                    sb.append("... timeout after 10 minutes");
                    break;
                }
            }
        }
        int exit = p.waitFor();
        out.put("exitCode", exit);
        out.put("output", sb.toString());
        return out;
    }
}
