package com.replate.fileservice.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
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

    public FileService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * S'exécute au démarrage pour vérifier/créer le bucket ET définir la politique d'accès.
     */
    @PostConstruct
    public void initBucket() {
        // 1. Vérifier si le bucket existe
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            System.out.println("FILE-SERVICE: Bucket '" + bucketName + "' trouvé.");
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                System.out.println("FILE-SERVICE: Bucket '" + bucketName + "' non trouvé. Création en cours...");
                try {
                    s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
                    System.out.println("FILE-SERVICE: Bucket créé avec succès.");
                } catch (S3Exception createEx) {
                    System.err.println("FILE-SERVICE: Impossible de créer le bucket: " + createEx.getMessage());
                    return;
                }
            } else {
                System.err.println("FILE-SERVICE: Erreur de connexion MinIO: " + e.getMessage());
                return;
            }
        }

        // 2. DÉFINIR LA POLITIQUE D'ACCÈS PUBLIQUE (FIX ACCESS DENIED)
        try {
            System.out.println("FILE-SERVICE: Définition de la politique d'accès publique pour le bucket " + bucketName);

            String policyJson = String.format("""
            {
                "Version": "2012-10-17",
                "Statement": [
                    {
                        "Effect": "Allow",
                        "Principal": "*",
                        "Action": ["s3:GetObject"],
                        "Resource": ["arn:aws:s3:::%s/*"]
                    }
                ]
            }
            """, bucketName);

            PutBucketPolicyRequest policyRequest = PutBucketPolicyRequest.builder()
                    .bucket(bucketName)
                    .policy(policyJson)
                    .build();

            s3Client.putBucketPolicy(policyRequest);
            System.out.println("FILE-SERVICE: Politique d'accès publique définie avec succès.");

        } catch (Exception e) {
            System.err.println("FILE-SERVICE: Erreur lors de la définition de la politique du bucket: " + e.getMessage());
        }
    }

    /**
     * Stocke le fichier binaire dans MinIO et renvoie son URL d'accès.
     */
    public String storeFileAndGetUrl(MultipartFile file, String fileType) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Le fichier est vide.");
        }

        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String objectKey = fileType + "/" + UUID.randomUUID() + fileExtension;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        return storageBaseUrl + objectKey;
    }

    /**
     * Supprime un fichier du bucket MinIO.
     */
    public void deleteFile(String objectKey) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();
            s3Client.deleteObject(deleteRequest);
            System.out.println("FILE-SERVICE: Fichier supprimé : " + objectKey);
        } catch (S3Exception e) {
            System.err.println("FILE-SERVICE: Erreur lors de la suppression du fichier " + objectKey + ": " + e.getMessage());
            throw new RuntimeException("Erreur lors de la suppression du fichier", e);
        }
    }
}