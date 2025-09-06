package com.datafit.migrater.service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class CleanupService {
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupOldJobs() {
        // TODO: Implement job retention cleanup from DB, S3 and local storage
    }
}