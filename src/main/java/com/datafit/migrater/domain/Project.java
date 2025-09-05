package com.datafit.migrater.domain;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
public class Project {
    @Id @GeneratedValue public UUID id;
    public String name;
    @Enumerated(EnumType.STRING) public DbType dbType;
    public String jdbcUrl;
    public String dbUser;
    public String dbPassword;
    public String s3Bucket;
    public String s3Folder;
    public String iamRoleArn;
    @Lob @Column(columnDefinition="CLOB") public String knownHosts;

    public java.util.UUID getId(){ return id; } public void setId(java.util.UUID i){ id=i; }
    public String getName(){ return name; } public void setName(String n){ name=n; }
    public DbType getDbType(){ return dbType; } public void setDbType(DbType d){ dbType=d; }
    public String getJdbcUrl(){ return jdbcUrl; } public void setJdbcUrl(String j){ jdbcUrl=j; }
    public String getDbUser(){ return dbUser; } public void setDbUser(String u){ dbUser=u; }
    public String getDbPassword(){ return dbPassword; } public void setDbPassword(String p){ dbPassword=p; }
    public String getS3Bucket(){ return s3Bucket; } public void setS3Bucket(String b){ s3Bucket=b; }
    public String getS3Folder(){ return s3Folder; } public void setS3Folder(String f){ s3Folder=f; }
    public String getIamRoleArn(){ return iamRoleArn; } public void setIamRoleArn(String r){ iamRoleArn=r; }
    public String getKnownHosts(){ return knownHosts; } public void setKnownHosts(String kh){ knownHosts = kh; }
}
