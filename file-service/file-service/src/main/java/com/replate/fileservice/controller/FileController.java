package com.replate.fileservice.controller;

import com.replate.fileservice.service.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
// Pas de @RequestMapping au niveau classe pour simplifier le routage Gateway si nécessaire
// Ou @RequestMapping("/")
public class FileController {

    private final FileService fileService;

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
}