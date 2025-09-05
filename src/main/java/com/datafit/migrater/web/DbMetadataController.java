package com.datafit.migrater.web;

import com.datafit.migrater.repo.ProjectRepository;
import com.datafit.migrater.domain.Project;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.*;

/**
 * Provides DB metadata endpoints for the UI mapping designer.
 */
@RestController
@RequestMapping("/api/dbmeta")
public class DbMetadataController {

    private final ProjectRepository projects;

    public DbMetadataController(ProjectRepository projects){ this.projects = projects; }

    @GetMapping("/projects/{projectId}/tables/{schema}/{table}/columns")
    public List<Map<String,Object>> columns(@PathVariable String projectId, @PathVariable String schema, @PathVariable String table) throws Exception {
        Project p = projects.findById(java.util.UUID.fromString(projectId)).orElseThrow();
        try(Connection c = DriverManager.getConnection(p.getJdbcUrl(), p.getDbUser(), p.getDbPassword())){
            DatabaseMetaData md = c.getMetaData();
            ResultSet rs = md.getColumns(null, schema, table, null);
            List<Map<String,Object>> cols = new ArrayList<>();
            while(rs.next()){
                Map<String,Object> col = new LinkedHashMap<>();
                col.put("name", rs.getString("COLUMN_NAME"));
                col.put("type", rs.getString("TYPE_NAME"));
                cols.add(col);
            }
            return cols;
        }
    }
}
