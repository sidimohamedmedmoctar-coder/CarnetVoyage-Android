package com.carnetvoyage.backend.service;

import com.carnetvoyage.backend.exception.ApiException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class UploadService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    private Path uploadPath;

    @PostConstruct
    public void init() {
        uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer le dossier d'upload", e);
        }
    }

    public String storeFile(MultipartFile file) {
        // Vérifs basiques
        if (file == null || file.isEmpty()) {
            throw new ApiException("Le fichier est vide", HttpStatus.BAD_REQUEST);
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            throw new ApiException("Nom de fichier invalide", HttpStatus.BAD_REQUEST);
        }

        // Vérifier que c'est bien une image
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ApiException("Seules les images sont autorisées", HttpStatus.BAD_REQUEST);
        }

        // Générer un nom unique : uuid + extension
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalName.substring(dotIndex);
        }
        String newFilename = UUID.randomUUID() + extension;

        // Sauvegarder
        try {
            Path targetLocation = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), targetLocation);
            return newFilename;
        } catch (IOException e) {
            throw new ApiException("Erreur lors de la sauvegarde", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void deleteFile(String filename) {
        if (filename == null || filename.isEmpty()) return;
        try {
            Path filePath = uploadPath.resolve(filename);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // On log mais on n'arrête pas le flux pour autant
            System.err.println("Erreur suppression fichier : " + e.getMessage());
        }
    }
}