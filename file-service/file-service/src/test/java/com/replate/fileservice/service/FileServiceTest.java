package com.replate.fileservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private S3Client s3Client;

    private FileService fileService;

    @BeforeEach
    void setUp() {
        // Injection manuelle car FileService utilise @Value
        fileService = new FileService(s3Client);
        ReflectionTestUtils.setField(fileService, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(fileService, "storageBaseUrl", "http://localhost:9000/test-bucket/");
    }

    @Test
    void storeFile_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "content".getBytes()
        );

        String fileUrl = fileService.storeFileAndGetUrl(file, "profiles");

        assertNotNull(fileUrl);
        assertTrue(fileUrl.startsWith("http://localhost:9000/test-bucket/profiles/"));
        assertTrue(fileUrl.endsWith(".jpg"));

        // Vérifie que l'appel S3 a bien eu lieu
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void storeFile_EmptyFile_ThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);
        assertThrows(IOException.class, () -> fileService.storeFileAndGetUrl(emptyFile, "profiles"));
    }
}