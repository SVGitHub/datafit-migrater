package com.datafit.migrater.web;
import com.datafit.migrater.repo.*;
import com.datafit.migrater.domain.*;
import com.datafit.migrater.service.JobRunnerService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * Start and list jobs; added endpoints for job details and error file download.
 */
@RestController @RequestMapping("/api/jobs")
public class JobController {
  private final JobRepository jobs; private final ProjectRepository projects; private final JobRunnerService runner; private final JobErrorRepository jobErrorRepo;
  public JobController(JobRepository j, ProjectRepository p, JobRunnerService r, JobErrorRepository jer){ this.jobs=j; this.projects=p; this.runner=r; this.jobErrorRepo = jer; }
  @PostMapping public String start(@RequestBody Map<String,Object> req){
    var job = new Job();
    job.setProject(projects.findById(java.util.UUID.fromString(String.valueOf(req.get("projectId")))).orElseThrow());
    job.setSourceType(String.valueOf(req.get("sourceType"))); job.setSourcePath(String.valueOf(req.get("folder"))); job.setFileGlob(String.valueOf(req.get("glob")));
    job.setTargetSchema(String.valueOf(req.get("targetSchema"))); job.setTargetTable(String.valueOf(req.get("targetTable")));
    jobs.save(job); runner.runAsync(job.getId()); return job.getId().toString();
  }
  @GetMapping public List<Job> list(@RequestParam String projectId){ return jobs.findTop50ByProjectIdOrderByStartedAtDesc(java.util.UUID.fromString(projectId)); }

  @GetMapping("/{id}") public Job get(@PathVariable String id){ return jobs.findById(java.util.UUID.fromString(id)).orElseThrow(); }
  @GetMapping("/{id}/errors") public List<JobError> errors(@PathVariable String id){ return jobErrorRepo.findByJobId(java.util.UUID.fromString(id)); }

  @GetMapping("/{id}/error-file") public ResponseEntity<FileSystemResource> downloadErrorFile(@PathVariable String id){
    Job job = jobs.findById(java.util.UUID.fromString(id)).orElseThrow();
    if(job.getErrorFileLocal()==null) return ResponseEntity.notFound().build();
    return ResponseEntity.ok(new FileSystemResource(job.getErrorFileLocal()));
  }
}
