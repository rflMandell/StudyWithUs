package com.studywithus.media.infrastructure;

import com.studywithus.media.domain.ObjectStoragePort;
import com.studywithus.shared.domain.DomainException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.media.storage-type", havingValue = "s3")
public class S3ObjectStorageAdapter implements ObjectStoragePort {

    @Value("${app.media.s3.endpoint}")
    private String endpoint;

    @Value("${app.media.s3.public-endpoint:${app.media.s3.endpoint}}")
    private String publicEndpoint;

    @Value("${app.media.s3.region:us-east-1}")
    private String region;

    @Value("${app.media.s3.bucket:studywithus-media}")
    private String bucket;

    @Value("${app.media.s3.access-key}")
    private String accessKey;

    @Value("${app.media.s3.secret-key}")
    private String secretKey;

    private S3Client s3Client;
    private S3Presigner s3Presigner;

    @PostConstruct
    public void init() {
        StaticCredentialsProvider credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
        );
        S3Configuration s3Config = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build();

        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(credentials)
                .serviceConfiguration(s3Config)
                .build();

        this.s3Presigner = S3Presigner.builder()
                .endpointOverride(URI.create(publicEndpoint))
                .region(Region.of(region))
                .credentialsProvider(credentials)
                .serviceConfiguration(s3Config)
                .build();

        ensureBucketExists();
    }

    private void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException ex) {
            log.info("Creating S3 bucket: {}", bucket);
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        } catch (Exception ex) {
            log.warn("Could not verify S3 bucket {} on startup: {}", bucket, ex.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        if (s3Presigner != null) s3Presigner.close();
        if (s3Client != null) s3Client.close();
    }

    @Override
    public PresignedUploadResult generatePresignedUploadUrl(
            String storageKey,
            String contentType,
            long contentLength,
            Duration expiration
    ) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(expiration)
                .putObjectRequest(putRequest)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
        return new PresignedUploadResult(
                presigned.url().toString(),
                Instant.now().plus(expiration),
                Map.of("Content-Type", contentType)
        );
    }

    @Override
    public boolean exists(String storageKey) {
        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(storageKey).build());
            return true;
        } catch (NoSuchKeyException ex) {
            return false;
        }
    }

    @Override
    public StoredObjectMetadata getMetadata(String storageKey) {
        try {
            HeadObjectResponse head = s3Client.headObject(
                    HeadObjectRequest.builder().bucket(bucket).key(storageKey).build()
            );
            return new StoredObjectMetadata(head.contentLength(), head.contentType());
        } catch (NoSuchKeyException ex) {
            throw new DomainException(HttpStatus.NOT_FOUND, "STORAGE_OBJECT_NOT_FOUND", "Object not found in S3 storage");
        }
    }

    @Override
    public byte[] download(String storageKey) {
        try {
            return s3Client.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucket).key(storageKey).build()
            ).asByteArray();
        } catch (NoSuchKeyException ex) {
            throw new DomainException(HttpStatus.NOT_FOUND, "STORAGE_OBJECT_NOT_FOUND", "Object not found in S3 storage");
        }
    }

    @Override
    public void upload(String storageKey, byte[] data, String contentType) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(storageKey)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(data)
        );
    }

    @Override
    public void delete(String storageKey) {
        s3Client.deleteObject(
                DeleteObjectRequest.builder().bucket(bucket).key(storageKey).build()
        );
    }

    @Override
    public String getPublicUrl(String storageKey) {
        String base = publicEndpoint.endsWith("/") ? publicEndpoint.substring(0, publicEndpoint.length() - 1) : publicEndpoint;
        return base + "/" + bucket + "/" + storageKey;
    }
}
