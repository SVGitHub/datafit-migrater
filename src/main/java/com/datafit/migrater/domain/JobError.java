package com.datafit.migrater.domain;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
public class JobError {
    @Id @GeneratedValue public UUID id;
    @ManyToOne public Job job;
    public Long rowNum;
    @Lob @Column(columnDefinition = "CLOB") public String reason;

    public java.util.UUID getId(){ return id; } public void setId(java.util.UUID i){ id=i; }
    public Job getJob(){ return job; } public void setJob(Job j){ job=j; }
    public Long getRowNum(){ return rowNum; } public void setRowNum(Long r){ rowNum=r; }
    public String getReason(){ return reason; } public void setReason(String r){ reason=r; }
}
