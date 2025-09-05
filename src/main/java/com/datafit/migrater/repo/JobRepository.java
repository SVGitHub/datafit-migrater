package com.datafit.migrater.repo;
import com.datafit.migrater.domain.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface JobRepository extends JpaRepository<Job, UUID> {
    List<Job> findTop50ByProjectIdOrderByStartedAtDesc(UUID projectId);
}
