package com.datafit.migrater.domain;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
public class MappingDef {
    @Id @GeneratedValue public UUID id;
    @ManyToOne public Project project;
    public String filePattern;
    @Lob @Column(columnDefinition = "CLOB") public String mappingJson;

    public java.util.UUID getId(){ return id; } public void setId(java.util.UUID i){ id=i; }
    public Project getProject(){ return project; } public void setProject(Project p){ project = p; }
    public String getFilePattern(){ return filePattern; } public void setFilePattern(String f){ filePattern=f; }
    public String getMappingJson(){ return mappingJson; } public void setMappingJson(String m){ mappingJson = m; }
}
