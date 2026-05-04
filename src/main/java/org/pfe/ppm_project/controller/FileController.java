package org.pfe.ppm_project.controller;

import lombok.RequiredArgsConstructor;
import org.pfe.ppm_project.services.IFileStorageService;
import org.pfe.ppm_project.services.IProjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Controller de gestion des fichiers attaches aux projets.
 * Permet l'upload et le listing de fichiers organises par projet et sous-repertoire.
 * Les fichiers sont stockes sur le systeme de fichiers local via FileStorageService.
 * L'upload est reserve aux ADMIN, PMO et PM ; le listing est ouvert aux utilisateurs authentifies.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/files") // Fichiers toujours lies a un projet
@RequiredArgsConstructor
public class FileController {

    private final IFileStorageService fileStorageService; // Service de stockage fichiers sur le disque
    private final IProjectService projectService; // Pour verifier que le projet existe

    // Upload d'un fichier dans un sous-repertoire du projet (ex: "documents", "livrables", etc.)
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PMO','PM')") // Seuls les gestionnaires peuvent uploader des fichiers
    public ResponseEntity<?> uploadFile(
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file, // Fichier envoye en multipart
            @RequestParam String subdirectory) { // Sous-repertoire de classement

        // Verifie que le projet existe avant de stocker le fichier
        return projectService.getById(projectId).map(project -> {
            try {
                // Stocke le fichier et retourne le nom du fichier sauvegarde
                String filename = fileStorageService.storeFile(project.name(), subdirectory, file);
                return ResponseEntity.ok(Map.of("filename", filename));
            } catch (IllegalArgumentException e) {
                // Erreur de validation (type de fichier non autorise, taille excessive, etc.)
                return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
            } catch (Exception e) {
                // Erreur technique (probleme d'ecriture sur le disque, etc.)
                return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload file"));
            }
        }).orElse(ResponseEntity.notFound().build()); // Projet non trouve
    }

    // Liste des fichiers dans un sous-repertoire d'un projet
    @GetMapping
    public ResponseEntity<?> listFiles(
            @PathVariable Long projectId,
            @RequestParam String subdirectory) {

        return projectService.getById(projectId).map(project -> {
            try {
                List<String> files = fileStorageService.listFiles(project.name(), subdirectory);
                return ResponseEntity.ok(files);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body(Map.of("error", "Failed to list files"));
            }
        }).orElse(ResponseEntity.notFound().build());
    }
}
