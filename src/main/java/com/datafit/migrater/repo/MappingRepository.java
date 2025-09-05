package com.datafit.migrater.repo;
import com.datafit.migrater.domain.MappingDef;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface MappingRepository extends JpaRepository<MappingDef, UUID> {
    List<MappingDef> findByProjectId(UUID projectId);
}
