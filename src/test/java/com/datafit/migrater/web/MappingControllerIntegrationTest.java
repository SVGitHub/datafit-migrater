package com.datafit.migrater.web;

import com.datafit.migrater.domain.Project;
import com.datafit.migrater.repo.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class MappingControllerIntegrationTest {

    @Autowired ProjectRepository projectRepository;
    @Autowired MappingController mappingController;

    @Test public void testSaveMappingValidatesTargets() throws Exception {
        // create H2 in-memory DB and table
        String url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
        try(Connection c = DriverManager.getConnection(url, "sa", "")){
            try(Statement st = c.createStatement()){
                st.execute("CREATE TABLE test_table(id INT PRIMARY KEY, name VARCHAR(100))");
            }
        }
        Project p = new Project();
        p.setName("h2proj"); p.setJdbcUrl(url); p.setDbUser("sa"); p.setDbPassword("");
        projectRepository.save(p);

        String mappingJson = "{ \"columns\": [ { \"source\": \"id\", \"target\": \"ID\" }, { \"source\": \"name\", \"target\": \"NAME\" } ] }";
        Map<String,Object> body = Map.of("projectId", p.getId().toString(), "filePattern", ".*", "mappingJson", mappingJson);
        var res = mappingController.save(body);
        assertNotNull(res.get("id"));
    }
}
