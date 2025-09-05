package com.datafit.migrater.repo;
import com.datafit.migrater.domain.JobError;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface JobErrorRepository extends JpaRepository<JobError, UUID> {
    List<JobError> findByJobId(UUID jobId);
}
