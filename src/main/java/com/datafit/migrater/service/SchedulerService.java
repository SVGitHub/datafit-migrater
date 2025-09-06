package com.datafit.migrater.service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SchedulerService {
    @Scheduled(cron = "0 0 * * * *")
    public void runScheduledJobs() {
        // TODO: Implement batch job execution from DB schedule
    }
}