package com.datafit.migrater.repo;
import com.datafit.migrater.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface ProjectRepository extends JpaRepository<Project, UUID> {}
