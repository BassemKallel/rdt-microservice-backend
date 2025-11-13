package com.replate.fileservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
public class MinIOConfig {

    @Value("${minio.endpoint.url}")
    private String endpointUrl;

    @Value("${minio.access.key}")
    private String accessKey;

    @Value("${minio.secret.key}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        // Configuration cruciale pour MinIO
        S3Configuration s3Config = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build();

        return S3Client.builder()
                .endpointOverride(URI.create(endpointUrl.trim())) // .trim() par sécurité
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.US_EAST_1)
                .serviceConfiguration(s3Config)
                .build();
    }
}