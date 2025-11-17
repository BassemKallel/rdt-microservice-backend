package com.replate.fileservice.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.UUID;

@Service
public class FileService {

    private final S3Client s3Client;

    @Value("${file.storage.bucket-name}")
    private String bucketName;

    @Value("${file.storage.base-url}")
    private String storageBaseUrl;

    // Constantes pour les tentatives de connexion au démarrage
    private static final int MAX_INIT_ATTEMPTS = 5;
    private static final long INIT_RETRY_WAIT_MS = 5000; // 5 secondes

    public FileService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * S'exécute au démarrage pour vérifier/créer le bucket ET définir la politique d'accès.
     * Cette version est robuste et gère les tentatives (retry) au cas où MinIO
     * ne serait pas encore prêt.
     */
    @PostConstruct
    public void initBucket() {
        for (int attempt = 1; attempt <= MAX_INIT_ATTEMPTS; attempt++) {
            System.out.println("FILE-SERVICE: Tentative " + attempt + "/" + MAX_INIT_ATTEMPTS + " de connexion à MinIO...");
            try {
                // 1. Vérifier si le bucket existe
                try {
                    s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
                    System.out.println("FILE-SERVICE: Bucket '" + bucketName + "' trouvé.");
                } catch (S3Exception e) {
                    if (e.statusCode() == 404) { // NoSuchBucketException
                        System.out.println("FILE-SERVICE: Bucket '" + bucketName + "' non trouvé. Création en cours...");
                        s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
                        System.out.println("FILE-SERVICE: Bucket créé avec succès.");
                    } else {
                        throw e; // Lancer une autre erreur S3 (ex: 403 Access Denied)
                    }
                }

                // 2. DÉFINIR LA POLITIQUE D'ACCÈS PUBLIQUE
                System.out.println("FILE-SERVICE: Définition de la politique d'accès publique...");
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

                // Si tout a réussi, sortir de la boucle
                return;

            } catch (Exception e) {
                System.err.println("FILE-SERVICE: Échec de l'initialisation (Tentative " + attempt + "): " + e.getMessage());
                if (attempt < MAX_INIT_ATTEMPTS) {
                    try {
                        Thread.sleep(INIT_RETRY_WAIT_MS); // Attendre avant de réessayer
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    // Après toutes les tentatives, arrêter l'application
                    throw new RuntimeException("Impossible d'initialiser le bucket MinIO après " + MAX_INIT_ATTEMPTS + " tentatives.", e);
                }
            }
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

        // Le 'objectKey' est le chemin du fichier dans le bucket
        String objectKey = fileType + "/" + UUID.randomUUID() + fileExtension;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        // Construit l'URL publique
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