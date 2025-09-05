
package com.datafit.migrater.web;
import com.datafit.migrater.repo.ProjectRepository;
import com.datafit.migrater.repo.UserAccessRepository;
import com.datafit.migrater.repo.SettingsRepository;
import com.datafit.migrater.domain.Project;
import com.datafit.migrater.domain.Settings;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * Project endpoints. Listing filters projects to only those the user has access to (unless admin).
 */
@RestController @RequestMapping("/api/projects")
public class ProjectController {
  private final ProjectRepository repo;
  private final UserAccessRepository accessRepo;
  private final SettingsRepository settingsRepo;
  public ProjectController(ProjectRepository r, UserAccessRepository accessRepo, SettingsRepository settingsRepo){ this.repo = r; this.accessRepo = accessRepo; this.settingsRepo = settingsRepo; }

  private String currentEmail(){
    Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if(p instanceof OAuth2User) return ((OAuth2User)p).getAttribute("email"); return null;
  }

  private boolean isAdmin(String email){
    return accessRepo.findByEmail(email).stream().anyMatch(ua->"ADMIN".equalsIgnoreCase(ua.getRoleName()));
  }

  @GetMapping public List<Map<String,Object>> list(){
    String email = currentEmail();
    List<Project> projects = repo.findAll();
    if(email==null) return List.of();
    if(isAdmin(email)){
      // admin: return all projects
    } else {
      var access = accessRepo.findByEmail(email);
      var pids = access.stream().map(ua->ua.getProject()).filter(Objects::nonNull).map(Project::getId).toList();
      projects = projects.stream().filter(p->pids.contains(p.getId())).toList();
    }
    List<Map<String,Object>> out = new ArrayList<>();
    for(Project p: projects){ Map<String,Object> m = new LinkedHashMap<>(); m.put("id", p.getId()); m.put("name", p.getName()); m.put("dbType", p.getDbType()); out.add(m); }
    return out;
  }

  @PostMapping public Map<String,Object> create(@RequestBody Project p){
    String email = currentEmail();
    if(email==null || !isAdmin(email)) throw new RuntimeException("admin required to create projects");
    repo.save(p); return Map.of("id", p.getId());
  }

  @PutMapping public Map<String,Object> update(@RequestParam String projectId, @RequestBody Map<String,Object> body){
    String email = currentEmail();
    if(email==null || !isAdmin(email)) throw new RuntimeException("admin required to update projects");
    var p = repo.findById(java.util.UUID.fromString(projectId)).orElseThrow();
    if(body.containsKey("s3Bucket")) p.setS3Bucket((String)body.get("s3Bucket"));
    if(body.containsKey("s3Folder")) p.setS3Folder((String)body.get("s3Folder"));
    if(body.containsKey("knownHosts")) p.setKnownHosts((String)body.get("knownHosts"));
    if(body.containsKey("jdbcUrl")) p.setJdbcUrl((String)body.get("jdbcUrl"));
    if(body.containsKey("dbUser")) p.setDbUser((String)body.get("dbUser"));
    if(body.containsKey("dbPassword")) p.setDbPassword((String)body.get("dbPassword"));
    if(body.containsKey("iamRoleArn")) p.setIamRoleArn((String)body.get("iamRoleArn"));
    repo.save(p);

    // update settings if provided
    var settings = settingsRepo.findByProjectId(p.getId()).orElseGet(()->{ Settings s=new Settings(); s.setProject(p); return s; });
    if(body.containsKey("sftpHost")) settings.setSftpHost((String)body.get("sftpHost"));
    if(body.containsKey("sftpPort")) settings.setSftpPort(body.get("sftpPort") instanceof Number ? ((Number)body.get("sftpPort")).intValue() : null);
    if(body.containsKey("sftpUser")) settings.setSftpUser((String)body.get("sftpUser"));
    if(body.containsKey("sftpPassword")) settings.setSftpPassword((String)body.get("sftpPassword"));
    settingsRepo.save(settings);

    return Map.of("ok", true);
  }

  @GetMapping("/{id}/details")
  public Map<String,Object> details(@PathVariable String id){
    String email = currentEmail();
    var p = repo.findById(java.util.UUID.fromString(id)).orElseThrow();
    Map<String,Object> out = new LinkedHashMap<>();
    out.put("id", p.getId()); out.put("name", p.getName()); out.put("dbType", p.getDbType()); out.put("s3Bucket", p.getS3Bucket()); out.put("s3Folder", p.getS3Folder());
    if(isAdmin(email)){
      out.put("knownHosts", p.getKnownHosts()); out.put("jdbcUrl", p.getJdbcUrl()); out.put("dbUser", p.getDbUser()); out.put("iamRoleArn", p.getIamRoleArn());
      var settingsOpt = settingsRepo.findByProjectId(p.getId()); if(settingsOpt.isPresent()){ var s = settingsOpt.get(); out.put("sftpHost", s.getSftpHost()); out.put("sftpPort", s.getSftpPort()); out.put("sftpUser", s.getSftpUser()); out.put("sftpPassword", s.getSftpPassword()); }
    }
    return out;
  }
}
