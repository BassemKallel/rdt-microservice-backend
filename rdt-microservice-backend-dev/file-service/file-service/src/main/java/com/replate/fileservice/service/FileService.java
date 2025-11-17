package com.replate.fileservice.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.UUID;

@Service
public class FileService {

    private final S3Client s3Client;

    @Value("${file.storage.bucket-name}")
    private String bucketName;

    @Value("${file.storage.base-url}")
    private String storageBaseUrl;

    // Injection standard par constructeur (Pas de cycle ici)
    public FileService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * S'exécute au démarrage pour vérifier/créer le bucket.
     */
    @PostConstruct
    public void initBucket() {
        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            s3Client.headBucket(headBucketRequest);
            System.out.println("FILE-SERVICE: Bucket '" + bucketName + "' trouvé.");
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                System.out.println("FILE-SERVICE: Bucket '" + bucketName + "' non trouvé. Création en cours...");
                try {
                    CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                            .bucket(bucketName)
                            .build();
                    s3Client.createBucket(createBucketRequest);
                    System.out.println("FILE-SERVICE: Bucket créé avec succès.");
                } catch (S3Exception createEx) {
                    System.err.println("FILE-SERVICE: Impossible de créer le bucket: " + createEx.getMessage());
                }
            } else {
                System.err.println("FILE-SERVICE: Erreur de connexion MinIO: " + e.getMessage());
            }
        }
    }

    public String storeFileAndGetUrl(MultipartFile file, String fileType) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Le fichier est vide.");
        }

        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // Génère un nom unique : profiles/uuid.jpg
        String objectKey = fileType + "/" + UUID.randomUUID() + fileExtension;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        return storageBaseUrl + objectKey;
    }
}