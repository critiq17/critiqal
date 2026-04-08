package org.acme.utils;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.CheckConstraint;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;

@ApplicationScoped
public class R2StorageService {

    private final S3Client s3;

    @ConfigProperty(name = "r2.bucket") String bucket;
    @ConfigProperty(name = "r2.public-url") String publicUrl;

    public R2StorageService(
            @ConfigProperty(name = "r2.account-id") String accountId,
            @ConfigProperty(name = "r2.access-key") String accessKey,
            @ConfigProperty(name = "r2.secret-key") String secretKey
    ) {
        this.s3 = S3Client.builder()
                .endpointOverride(URI.create(
                        "https://" + accountId + ".r2.cloudflarestorage.com"
                ))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .region(Region.of("auto"))
                .build();
    }

    public String upload(String key, byte[] data, String contentType) {
        s3.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .contentLength((long) data.length)
                        .build(),
                RequestBody.fromBytes(data)
        );
        return publicUrl + "/" + key;
    }

    public void delete(String key) {
        s3.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build()
        );
    }

    public void deleteByUrl(String url) {
        if (url == null || !url.startsWith(publicUrl)) return;
        var key = url.replace(publicUrl + "/", "");
        delete(key);
    }
}
