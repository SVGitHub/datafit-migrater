package com.datafit.migrater.web;

import com.datafit.migrater.domain.MappingDef;
import com.datafit.migrater.repo.MappingRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import com.datafit.migrater.repo.ProjectRepository;
import com.datafit.migrater.domain.Project;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * CRUD for mapping definitions used by the mapping designer.
 */
@RestController
@RequestMapping("/api/mappings")
public class MappingController {

    private final MappingRepository repo;
    private final ProjectRepository projectRepo;

    public MappingController(MappingRepository repo, ProjectRepository projectRepo){ this.repo = repo; this.projectRepo = projectRepo; }

    private String currentEmail(){
        Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(p instanceof OAuth2User) return ((OAuth2User)p).getAttribute("email");
        return null;
    }

    @GetMapping
    public List<MappingDef> list(@RequestParam String projectId){
        // TODO: enforce project access
        return repo.findByProjectId(java.util.UUID.fromString(projectId));
    }

    @PostMapping
    public Map<String,Object> save(@RequestBody Map<String,Object> body) throws Exception{
        String projectId = (String)body.get("projectId");
        String pattern = (String)body.get("filePattern");
        Object mappingJson = body.get("mappingJson");
        // validate mapping targets exist in DB
        java.util.List<String> targets = new java.util.ArrayList<>();
        if(mappingJson!=null){
            com.fasterxml.jackson.databind.ObjectMapper _om = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = _om.readTree(mappingJson.toString());
            com.fasterxml.jackson.databind.JsonNode cols = root.path("columns");
            if(cols.isArray()) for(com.fasterxml.jackson.databind.JsonNode n: cols) if(n.has("target")) targets.add(n.get("target").asText());
        }
        if(!targets.isEmpty()){
            Project proj = projectRepo.findById(java.util.UUID.fromString(projectId)).orElseThrow();
            try(java.sql.Connection c = java.sql.DriverManager.getConnection(proj.getJdbcUrl(), proj.getDbUser(), proj.getDbPassword())){
                java.sql.DatabaseMetaData md = c.getMetaData();
                java.util.Set<String> avail = new java.util.HashSet<>();
                // fetch all columns for schema/table wildcard
                java.sql.ResultSet rs = md.getColumns(null, null, null, null);
                while(rs.next()){ avail.add(rs.getString("COLUMN_NAME")); }
                for(String t: targets) if(!avail.contains(t)) throw new RuntimeException("Target column not found in DB metadata: "+t);
            }
        }
        MappingDef md = new MappingDef();
        com.datafit.migrater.domain.Project p = new com.datafit.migrater.domain.Project(); p.setId(java.util.UUID.fromString(projectId));
        md.setProject(p); md.setFilePattern(pattern); md.setMappingJson(mappingJson==null?null:mappingJson.toString());
        repo.save(md);
        return Map.of("id", md.getId());
    }
}
