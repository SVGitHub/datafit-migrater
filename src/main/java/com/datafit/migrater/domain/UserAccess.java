package com.datafit.migrater.domain;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
public class UserAccess {
    @Id @GeneratedValue public UUID id;
    public String email;
    public String roleName;
    @ManyToOne public Project project;

    public java.util.UUID getId(){ return id; } public void setId(java.util.UUID i){ id=i; }
    public String getEmail(){ return email; } public void setEmail(String e){ email=e; }
    public String getRoleName(){ return roleName; } public void setRoleName(String r){ roleName=r; }
    public Project getProject(){ return project; } public void setProject(Project p){ project=p; }
}
