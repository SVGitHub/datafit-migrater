package com.datafit.migrater.repo;
import com.datafit.migrater.domain.Settings;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface SettingsRepository extends JpaRepository<Settings, UUID> {
    Optional<Settings> findByProjectId(UUID projectId);
}
