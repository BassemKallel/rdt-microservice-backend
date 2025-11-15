package com.replate.fileservice.controller;

import com.replate.fileservice.dto.DeleteRequest;
import com.replate.fileservice.service.FileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/") // Mappé à la racine pour le filtre Gateway
public class FileController {

    private final FileService fileService;

    @Value("${file.storage.base-url}")
    private String storageBaseUrl;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type) {
        try {
            String fileUrl = fileService.storeFileAndGetUrl(file, type);
            return ResponseEntity.ok(fileUrl);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Erreur fichier: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erreur serveur: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteFile(@RequestBody DeleteRequest request) {
        try {
            // Extrait la "clé" (le chemin) de l'URL complète
            // (ex: "profiles/uuid.jpg")
            String objectKey = request.getFileUrl().substring(storageBaseUrl.length());

            fileService.deleteFile(objectKey);
            return ResponseEntity.ok("Fichier supprimé avec succès.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erreur lors de la suppression: " + e.getMessage());
        }
    }
}