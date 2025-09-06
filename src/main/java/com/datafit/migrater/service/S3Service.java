package com.datafit.migrater.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Lazy/deferred S3Service wired to real ClientFactory (per-profile).
 */
@Service
public class S3Service {

    private final ClientFactory clientFactory;
    private final String defaultRegion;
    private final Optional<String> defaultEndpoint;

    public S3Service(ClientFactory clientFactory,
                     @Value("${aws.region:}") String defaultRegion,
                     @Value("${aws.s3.endpoint:}") Optional<String> defaultEndpoint) {
        this.clientFactory = Objects.requireNonNull(clientFactory);
        this.defaultRegion = (defaultRegion == null || defaultRegion.isBlank()) ? null : defaultRegion;
        this.defaultEndpoint = defaultEndpoint;
    }

    public Optional<S3Client> s3ClientForProfile(Optional<UUID> profileId) {
        try {
            if (profileId != null && profileId.isPresent()) {
                Optional<S3Client> p = clientFactory.s3ClientForProfile(profileId.get());
                if (p.isPresent()) return p;
            }
            if (defaultRegion != null) {
                S3ClientBuilder b = S3Client.builder().region(Region.of(defaultRegion));
                defaultEndpoint.ifPresent(e -> b.endpointOverride(URI.create(e)));
                return Optional.of(b.build());
            }
            return Optional.empty();
        } catch (Exception ex) {
            System.err.println("S3Service.s3ClientForProfile: error creating client: " + ex.getMessage());
            return Optional.empty();
        }
    }

    public String uploadFile(UUID profileId, String bucket, String key, Path file) {
        return uploadFile(Optional.ofNullable(profileId), bucket, key, file)
                .orElseThrow(() -> new IllegalStateException("No S3 client available for uploadFile"));
    }

    public String uploadFile(String bucket, String key, Path file) {
        return uploadFile(Optional.empty(), bucket, key, file)
                .orElseThrow(() -> new IllegalStateException("No S3 client available for uploadFile"));
    }

    public Optional<String> uploadFile(Optional<UUID> profileId, String bucket, String key, Path file) {
        Optional<S3Client> clientOpt = s3ClientForProfile(profileId);
        if (clientOpt.isEmpty()) return Optional.empty();
        try {
            S3Client c = clientOpt.get();
            PutObjectRequest req = PutObjectRequest.builder().bucket(bucket).key(key).build();
            c.putObject(req, RequestBody.fromFile(file));
            return Optional.of("s3://" + bucket + "/" + key);
        } catch (Exception ex) {
            System.err.println("S3Service.uploadFile failed: " + ex.getMessage());
            return Optional.empty();
        }
    }

    public String uploadStream(UUID profileId, String bucket, String key, InputStream stream, long contentLength) {
        return uploadStream(Optional.ofNullable(profileId), bucket, key, stream, contentLength)
                .orElseThrow(() -> new IllegalStateException("No S3 client available for uploadStream"));
    }

    public Optional<String> uploadStream(Optional<UUID> profileId, String bucket, String key, InputStream stream, long contentLength) {
        Optional<S3Client> clientOpt = s3ClientForProfile(profileId);
        if (clientOpt.isEmpty()) return Optional.empty();
        try {
            S3Client c = clientOpt.get();
            PutObjectRequest req = PutObjectRequest.builder().bucket(bucket).key(key).build();
            c.putObject(req, RequestBody.fromInputStream(stream, contentLength));
            return Optional.of("s3://" + bucket + "/" + key);
        } catch (Exception ex) {
            System.err.println("S3Service.uploadStream failed: " + ex.getMessage());
            return Optional.empty();
        }
    }

    public Optional<String> uploadStream(String bucket, String key, InputStream stream, long contentLength) {
        return uploadStream(Optional.empty(), bucket, key, stream, contentLength);
    }
}