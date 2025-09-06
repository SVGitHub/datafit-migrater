package com.datafit.migrater.service;

import com.datafit.migrater.domain.Settings;
import com.datafit.migrater.repo.SettingsRepository;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.lang.reflect.Method;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ClientFactory: builds and caches S3Client instances per profile (UUID).
 *
 * This version uses resilient reflection to read Settings values so it compiles
 * regardless of the exact getter names used in your Settings entity.
 *
 * Replace maybeDecrypt(...) with your real decryption logic if required.
 */
@Service
public class ClientFactory {

    private final SettingsRepository settingsRepo;
    private final Map<UUID, S3Client> s3Cache = new ConcurrentHashMap<>();

    public ClientFactory(SettingsRepository settingsRepo) {
        this.settingsRepo = Objects.requireNonNull(settingsRepo);
    }

    /**
     * Attempts to get an S3Client for given profileId.
     * If settings are not present or contain no S3 config, a default SDK-backed client is returned.
     */
    public Optional<S3Client> s3ClientForProfile(UUID profileId) {
        try {
            if (profileId == null) return Optional.empty();

            if (s3Cache.containsKey(profileId)) {
                return Optional.of(s3Cache.get(profileId));
            }

            // Try find Settings by project id (existing code used findByProjectId earlier)
            Optional<Settings> sOpt;
            try {
                sOpt = settingsRepo.findByProjectId(profileId);
            } catch (Throwable t) {
                // fallback to findById if findByProjectId not present
                try {
                    sOpt = settingsRepo.findById(profileId);
                } catch (Throwable t2) {
                    sOpt = Optional.empty();
                }
            }

            if (sOpt.isEmpty()) {
                // return default client (SDK default provider chain)
                S3Client defaultClient = S3Client.builder().build();
                s3Cache.put(profileId, defaultClient);
                return Optional.of(defaultClient);
            }

            Settings s = sOpt.get();

            // Try to extract region/endpoint/accessKey/secretKey using reflection with multiple candidate names
            String region = firstNonBlank(
                    getStringFromSettings(s, "getAwsRegion", "getRegion", "getS3Region", "getRegionName", "getAws_region", "getRegionValue")
            );

            String endpoint = firstNonBlank(
                    getStringFromSettings(s, "getAwsEndpoint", "getS3Endpoint", "getEndpoint", "getAwsEndpointUrl")
            );

            String accessKey = firstNonBlank(
                    getStringFromSettings(s, "getAwsAccessKey", "getAccessKey", "getAwsKey", "getAccessKeyId", "getAws_access_key")
            );

            String secretKey = firstNonBlank(
                    getStringFromSettings(s, "getAwsSecretKey", "getSecretKey", "getAwsSecret", "getSecretAccessKey", "getAws_secret_key")
            );

            // Build S3Client
            S3ClientBuilder builder = S3Client.builder();
            if (region != null && !region.isBlank()) {
                try { builder.region(Region.of(region)); } catch (Exception ignore) {}
            }

            if (endpoint != null && !endpoint.isBlank()) {
                try { builder.endpointOverride(URI.create(endpoint)); } catch (Exception ignore) {}
            }

            if (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank()) {
                accessKey = maybeDecrypt(accessKey);
                secretKey = maybeDecrypt(secretKey);
                AwsCredentialsProvider creds = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
                builder.credentialsProvider(creds);
            }

            // optional override config
            ClientOverrideConfiguration override = ClientOverrideConfiguration.builder()
                    .apiCallTimeout(Duration.ofMinutes(10))
                    .apiCallAttemptTimeout(Duration.ofMinutes(2))
                    .build();
            builder.overrideConfiguration(override);

            S3Client client = builder.build();
            s3Cache.put(profileId, client);
            return Optional.of(client);

        } catch (Exception ex) {
            System.err.println("ClientFactory.s3ClientForProfile error: " + ex.getMessage());
            return Optional.empty();
        }
    }

    private String firstNonBlank(String... vals) {
        for (String v : vals) if (v != null && !v.isBlank()) return v;
        return null;
    }

    /**
     * Try multiple candidate getter names on Settings via reflection and return first non-null String value.
     */
    private String getStringFromSettings(Settings s, String... getterNames) {
        if (s == null) return null;
        Class<?> cls = s.getClass();
        for (String name : getterNames) {
            try {
                Method m = cls.getMethod(name);
                if (m != null) {
                    Object val = m.invoke(s);
                    if (val != null) return String.valueOf(val);
                }
            } catch (NoSuchMethodException nsme) {
                // try next
            } catch (Throwable t) {
                // ignore other issues for resilience
            }
        }
        // Additionally try reading public fields directly (fallback)
        for (String candidate : getterNames) {
            try {
                java.lang.reflect.Field f = cls.getField(candidate);
                Object val = f.get(s);
                if (val != null) return String.valueOf(val);
            } catch (Throwable ignore) {}
        }
        return null;
    }

    /**
     * Replace with your decryption logic when DB stores encrypted secrets.
     */
    private String maybeDecrypt(String v) {
        return v; // TODO: replace with actual decryption
    }

    @PreDestroy
    public void closeAll() {
        s3Cache.values().forEach(c -> {
            try { c.close(); } catch (Exception ignored) {}
        });
        s3Cache.clear();
    }
}