package com.datafit.migrater.domain;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
public class Settings {
    @Id @GeneratedValue public UUID id;
    @OneToOne public Project project;
    public String compositeKeyCsv; public String sftpHost; public Integer sftpPort; public String sftpUser; public String sftpPassword; public String sftpPrivateKey;
    public Integer chunkSizeMb;
    public Integer maxOpenCsvWriters;
    public Integer rowErrorLimit;
    public Integer retentionDays;
    public String s3Bucket;
    public String s3Folder;

    public java.util.UUID getId(){ return id; } public void setId(java.util.UUID i){ id=i; }
    public Project getProject(){ return project; } public void setProject(Project p){ project=p; }
    public String getCompositeKeyCsv(){ return compositeKeyCsv; } public void setCompositeKeyCsv(String c){ compositeKeyCsv=c; }
    public Integer getChunkSizeMb(){ return chunkSizeMb; } public void setChunkSizeMb(Integer c){ chunkSizeMb=c; }
    public Integer getMaxOpenCsvWriters(){ return maxOpenCsvWriters; } public void setMaxOpenCsvWriters(Integer m){ maxOpenCsvWriters=m; }
    public Integer getRowErrorLimit(){ return rowErrorLimit; } public void setRowErrorLimit(Integer r){ rowErrorLimit=r; }
    public Integer getRetentionDays(){ return retentionDays; } public void setRetentionDays(Integer r){ retentionDays=r; }
    public String getS3Bucket(){ return s3Bucket; } public void setS3Bucket(String s){ s3Bucket=s; }
    public String getS3Folder(){ return s3Folder; } public void setS3Folder(String s){ s3Folder=s; } public String getSftpHost(){ return sftpHost; } public void setSftpHost(String h){ sftpHost=h; } public Integer getSftpPort(){ return sftpPort; } public void setSftpPort(Integer p){ sftpPort=p; } public String getSftpUser(){ return sftpUser; } public void setSftpUser(String u){ sftpUser=u; } public String getSftpPassword(){ return sftpPassword; } public void setSftpPassword(String p){ sftpPassword=p; } public String getSftpPrivateKey(){ return sftpPrivateKey; } public void setSftpPrivateKey(String k){ sftpPrivateKey=k; }
}
