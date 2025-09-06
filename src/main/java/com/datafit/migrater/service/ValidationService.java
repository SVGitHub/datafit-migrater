package com.datafit.migrater.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * ValidationService now supports parsing a mapping JSON (from MappingDef.mappingJson)
 * The mapping JSON is expected to contain 'columns' array with objects { source, target, required, defaultValue, transform, format }
 */
@Service
public class ValidationService {
    private final ObjectMapper om = new ObjectMapper();

    public static class Rule { public String source; public String target; public boolean required; public String transform; public String format; public String defaultValue; }

    public static class Result { public Map<String,Object> out = new LinkedHashMap<>(); public List<String> errors = new ArrayList<>(); }

    /**
     * Helper DTO for mapping info, used by tests and UI mapping.
     */
    public static class MappingInfo {
        public List<?> columns = Collections.emptyList();
        public List<String> upsertKeys = Collections.emptyList();
    }
    public List<Rule> parseRules(String mappingJson) throws Exception {
        List<Rule> rules = new ArrayList<>();
        if (mappingJson == null || mappingJson.isBlank()) return rules;
        JsonNode root = om.readTree(mappingJson);
        JsonNode cols = root.path("columns");
        if (cols.isArray()) {
            for (JsonNode n: cols) {
                Rule r = new Rule();
                r.source = n.path("source").asText();
                r.target = n.path("target").asText();
                r.required = n.path("required").asBoolean(false);
                r.defaultValue = n.path("defaultValue").isMissingNode()?null:n.path("defaultValue").asText(null);
                r.transform = n.path("transform").isMissingNode()?null:n.path("transform").asText(null);
                r.format = n.path("format").isMissingNode()?null:n.path("format").asText(null);
                rules.add(r);
            }
        }
        return rules;
    }

    public Result apply(List<Rule> rules, Map<String,Object> row){
        Result r = new Result();
        for(Rule rule: rules){
            Object raw = row.get(rule.source);
            if((raw==null || String.valueOf(raw).isEmpty()) && rule.defaultValue!=null) raw = rule.defaultValue;
            if(rule.required && (raw==null || String.valueOf(raw).isEmpty())) r.errors.add("Missing required: "+rule.target);
            // Transformations (simple): date -> keep as string, int/decimal -> parse
            Object val = raw;
            if(raw!=null && rule.transform!=null){
                try {
                    switch(rule.transform.toLowerCase()){
                        case "int": val = Integer.parseInt(String.valueOf(raw)); break;
                        case "long": val = Long.parseLong(String.valueOf(raw)); break;
                        case "double": val = Double.parseDouble(String.valueOf(raw)); break;
                        case "bool": val = Boolean.parseBoolean(String.valueOf(raw)); break;
                        case "date": val = String.valueOf(raw); break;
                        default: val = raw;
                    }
                } catch(Exception ex){ r.errors.add("Transform error for "+rule.target+": "+ex.getMessage()); }
            }
            r.out.put(rule.target, val);
        }
        return r;
    }

    /**
     * Parse mapping JSON and return MappingInfo (columns + upsertKeys).
     */
    public MappingInfo getMappingInfo(String mappingJson) {
        MappingInfo mi = new MappingInfo();
        try {
            if (mappingJson == null || mappingJson.isBlank()) {
                return mi;
            }
            JsonNode root = om.readTree(mappingJson);

            if (root.has("columns")) {
                mi.columns = om.convertValue(
                        root.get("columns"),
                        new TypeReference<List<?>>() {}
                );
            }
            if (root.has("upsertKeys")) {
                mi.upsertKeys = om.convertValue(
                        root.get("upsertKeys"),
                        new TypeReference<List<String>>() {}
                );
            }
        } catch (Exception e) {
            System.err.println("ValidationService.getMappingInfo failed: " + e.getMessage());
        }
        return mi;
    }
}
