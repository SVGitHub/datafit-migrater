package com.datafit.migrater.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * S3 helper using AWS SDK v2.
 * Expects credentials / region via environment or instance profile.
 */
@Service
public class S3Service {

    private final S3Client s3;

    public S3Service() {
        this.s3 = S3Client.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    public String uploadFile(String bucket, String keyPrefix, Path file) throws IOException {
        Objects.requireNonNull(bucket, "bucket required");
        String key = (keyPrefix == null || keyPrefix.isBlank()) ? file.getFileName().toString() : (keyPrefix + "/" + file.getFileName().toString());
        PutObjectRequest req = PutObjectRequest.builder().bucket(bucket).key(key).build();
        s3.putObject(req, RequestBody.fromFile(file));
        return "s3://" + bucket + "/" + key;
    }

    public String uploadStream(String bucket, String key, InputStream is, long contentLength) throws IOException {
        Objects.requireNonNull(bucket, "bucket required");
        PutObjectRequest req = PutObjectRequest.builder().bucket(bucket).key(key).build();
        s3.putObject(req, RequestBody.fromInputStream(is, contentLength));
        return "s3://" + bucket + "/" + key;
    }

    public void deletePrefix(String bucket, String keyPrefix) {
        if (bucket==null || bucket.isBlank() || keyPrefix==null) return;
        String prefix = keyPrefix.endsWith("/") ? keyPrefix : keyPrefix + "/";
        ListObjectsV2Request listReq = ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).build();
        ListObjectsV2Response listRes = s3.listObjectsV2(listReq);
        List<S3Object> objs = listRes.contents();
        if (objs == null || objs.isEmpty()) return;
        List<String> keys = objs.stream().map(S3Object::key).collect(Collectors.toList());
        var del = Delete.builder().objects(keys.stream().map(k->software.amazon.awssdk.services.s3.model.ObjectIdentifier.builder().key(k).build()).collect(Collectors.toList())).build();
        DeleteObjectsRequest dor = DeleteObjectsRequest.builder().bucket(bucket).delete(del).build();
        s3.deleteObjects(dor);
    }
}
