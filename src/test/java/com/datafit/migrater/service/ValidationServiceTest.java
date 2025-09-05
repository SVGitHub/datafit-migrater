package com.datafit.migrater.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ValidationServiceTest {

    @Test public void testParseAndMappingInfo(){
        ValidationService vs = new ValidationService();
        String json = "{ \"columns\": [ { \"source\": \"a\", \"target\": \"col1\" }, { \"source\": \"b\", \"target\": \"col2\" } ], \"upsertKeys\": [\"col1\"] }";
        var rules = assertDoesNotThrow(() -> vs.parseRules(json));
        assertEquals(2, rules.size());
        var mi = vs.getMappingInfo(json);
        assertEquals(2, mi.columns.size());
        assertEquals(1, mi.upsertKeys.size());
    }
}
