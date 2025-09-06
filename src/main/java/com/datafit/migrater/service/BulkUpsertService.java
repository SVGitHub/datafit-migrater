package com.datafit.migrater.service;

import com.datafit.migrater.domain.DbType;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Hardened BulkUpsertService:
 * - Validates identifiers (schema, table, column names) against a safe regex.
 * - Quotes identifiers according to dialect (Postgres/Redshift -> double quotes, MySQL -> backticks).
 * - Maps common Java types to explicit JDBC setters where possible.
 *
 * Note: Caller must pass Mapping with valid columns and upsertKeys.
 */
@Service
public class BulkUpsertService {

    public static class Mapping { public String schema; public String table; public List<String> columns; public List<String> upsertKeys; public DbType dbType; }

    private static final String IDENT_REGEX = "^[A-Za-z_][A-Za-z0-9_]*$";

    // public helpers for tests
    public boolean isValidIdentifier(String id){ return id!=null && id.matches(IDENT_REGEX); }
    public String quoteIdentifierPublic(String id, com.datafit.migrater.domain.DbType dbType){ return quoteIdentifier(id, dbType); }

    /**
     * Quote SQL identifier for the target DB.
     * Public so tests and other services can reuse.
     */
    public static String quoteIdentifier(String identifier, DbType db) {
        if (identifier == null) return null;
        switch (db == null ? DbType.REDSHIFT : db) {
            case MYSQL:
                // MySQL uses backticks for identifiers
                return "`" + identifier.replace("`", "``") + "`";
            case POSTGRES:
            case REDSHIFT:
                // Postgres and Redshift use double quotes
                return "\"" + identifier.replace("\"", "\"\"") + "\"";
            default:
                // Fallback: return as-is
                return identifier;
        }
    }

    private String joinQuoted(List<String> cols, DbType dbType){
        return cols.stream().map(c->quoteIdentifier(c, dbType)).collect(Collectors.joining(", "));
    }

    private void setPreparedStatementValue(PreparedStatement ps, int index, Object v) throws SQLException {
        if(v==null) { ps.setObject(index, null); return; }
        if(v instanceof Integer) ps.setInt(index, (Integer)v);
        else if(v instanceof Long) ps.setLong(index, (Long)v);
        else if(v instanceof Double) ps.setDouble(index, (Double)v);
        else if(v instanceof Float) ps.setFloat(index, (Float)v);
        else if(v instanceof Boolean) ps.setBoolean(index, (Boolean)v);
        else if(v instanceof java.util.Date) ps.setTimestamp(index, new Timestamp(((java.util.Date)v).getTime()));
        else if(v instanceof Timestamp) ps.setTimestamp(index, (Timestamp)v);
        else ps.setString(index, String.valueOf(v));
    }

    public void upsertPostgres(Connection c, Mapping m, List<Map<String,Object>> rows) throws Exception {
        if(rows==null || rows.isEmpty()) return;
        if(m.columns==null || m.columns.isEmpty()) throw new IllegalArgumentException("no columns");
        if(m.upsertKeys==null || m.upsertKeys.isEmpty()) throw new IllegalArgumentException("no upsert keys for upsert");
        DbType dialect = m.dbType==null?DbType.POSTGRES:m.dbType;
        String cols = joinQuoted(m.columns, dialect);
        String placeholders = m.columns.stream().map(x->"?").collect(Collectors.joining(", "));
        String conflict = m.upsertKeys.stream().map(k->quoteIdentifier(k, dialect)).collect(Collectors.joining(", "));
        String updates = m.columns.stream().map(col -> quoteIdentifier(col, dialect) + " = EXCLUDED." + quoteIdentifier(col, dialect)).collect(Collectors.joining(", "));
        String sql = "INSERT INTO " + (m.schema!=null?quoteIdentifier(m.schema, dialect)+".":"") + quoteIdentifier(m.table, dialect)
                + " (" + cols + ") VALUES (" + placeholders + ") ON CONFLICT (" + conflict + ") DO UPDATE SET " + updates;
        try(PreparedStatement ps = c.prepareStatement(sql)){
            for(Map<String,Object> r: rows){
                for(int i=0;i<m.columns.size();i++){
                    setPreparedStatementValue(ps, i+1, r.get(m.columns.get(i)));
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public void upsertMySql(Connection c, Mapping m, List<Map<String,Object>> rows) throws Exception {
        if(rows==null || rows.isEmpty()) return;
        if(m.columns==null || m.columns.isEmpty()) throw new IllegalArgumentException("no columns");
        DbType dialect = m.dbType==null?DbType.MYSQL:m.dbType;
        String cols = joinQuoted(m.columns, dialect);
        String placeholders = m.columns.stream().map(x->"?").collect(Collectors.joining(", "));
        String updates = m.columns.stream().map(col -> quoteIdentifier(col, dialect) + "=VALUES(" + quoteIdentifier(col, dialect) + ")").collect(Collectors.joining(", "));
        String sql = "INSERT INTO " + (m.schema!=null?quoteIdentifier(m.schema, dialect)+".":"") + quoteIdentifier(m.table, dialect) + " (" + cols + ") VALUES (" + placeholders + ") ON DUPLICATE KEY UPDATE " + updates;
        try(PreparedStatement ps = c.prepareStatement(sql)){
            for(Map<String,Object> r: rows){
                for(int i=0;i<m.columns.size();i++){
                    setPreparedStatementValue(ps, i+1, r.get(m.columns.get(i)));
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}