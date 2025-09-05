package com.datafit.migrater.service;

import com.datafit.migrater.domain.DbType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BulkUpsertServiceTest {

    @Test public void testIdentifierValidation(){
        BulkUpsertService s = new BulkUpsertService();
        assertTrue(s.isValidIdentifier("col_name"));
        assertTrue(s.isValidIdentifier("a"));
        assertFalse(s.isValidIdentifier("1abc"));
        assertFalse(s.isValidIdentifier("bad-char!"));
    }

    @Test public void testQuoteIdentifier(){
        BulkUpsertService s = new BulkUpsertService();
        assertEquals(""col"", s.quoteIdentifierPublic("col", com.datafit.migrater.domain.DbType.POSTGRES));
        assertEquals("`col`", s.quoteIdentifierPublic("col", com.datafit.migrater.domain.DbType.MYSQL));
    }
}
