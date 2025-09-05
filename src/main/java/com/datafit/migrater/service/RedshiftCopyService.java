package com.datafit.migrater.service;

import com.datafit.migrater.domain.Project;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Redshift COPY helper: executes a COPY command from S3 into redshift table.
 */
@Service
public class RedshiftCopyService {

    public void copyFromS3(Project p, String schema, String table, String s3Path) throws Exception {
        try (Connection c = DriverManager.getConnection(p.getJdbcUrl(), p.getDbUser(), p.getDbPassword());
             Statement st = c.createStatement()) {
            String sql = String.format("COPY %s.%s FROM '%s' IAM_ROLE '%s' FORMAT AS CSV IGNOREHEADER 1", schema, table, s3Path, p.getIamRoleArn());
            st.execute(sql);
        }
    }
}
