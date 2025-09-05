package com.datafit.migrater.domain;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
public class Job {
    @Id @GeneratedValue public UUID id;
    @ManyToOne public Project project;
    public String sourceType;
    public String sourcePath;
    public String fileGlob;
    public String targetSchema;
    public String targetTable;
    @Enumerated(EnumType.STRING) public JobStatus status = JobStatus.PENDING;
    public OffsetDateTime startedAt;
    public OffsetDateTime finishedAt;
    public Long totalRows;
    public Long successRows;
    public Long errorRows;
    public String errorFileLocal;
    public String errorFileS3;

    public java.util.UUID getId(){ return id; } public void setId(java.util.UUID i){ id=i; }
    public Project getProject(){ return project; } public void setProject(Project p){ project=p; }
    public String getSourceType(){ return sourceType; } public void setSourceType(String s){ sourceType=s; }
    public String getSourcePath(){ return sourcePath; } public void setSourcePath(String s){ sourcePath=s; }
    public String getFileGlob(){ return fileGlob; } public void setFileGlob(String g){ fileGlob=g; }
    public String getTargetSchema(){ return targetSchema; } public void setTargetSchema(String t){ targetSchema=t; }
    public String getTargetTable(){ return targetTable; } public void setTargetTable(String t){ targetTable=t; }
    public JobStatus getStatus(){ return status; } public void setStatus(JobStatus s){ status=s; }
    public OffsetDateTime getStartedAt(){ return startedAt; } public void setStartedAt(OffsetDateTime s){ startedAt=s; }
    public OffsetDateTime getFinishedAt(){ return finishedAt; } public void setFinishedAt(OffsetDateTime f){ finishedAt=f; }
    public Long getTotalRows(){ return totalRows; } public void setTotalRows(Long t){ totalRows=t; }
    public Long getSuccessRows(){ return successRows; } public void setSuccessRows(Long s){ successRows=s; }
    public Long getErrorRows(){ return errorRows; } public void setErrorRows(Long e){ errorRows=e; }
    public String getErrorFileLocal(){ return errorFileLocal; } public void setErrorFileLocal(String f){ errorFileLocal=f; }
    public String getErrorFileS3(){ return errorFileS3; } public void setErrorFileS3(String f){ errorFileS3=f; }
}
