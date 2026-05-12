package org.pfe.ppm_project.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Service de stockage de fichiers sur le systeme de fichiers local.
 * Gere la creation de dossiers de projet, l'upload et le listing des fichiers.
 * Les sous-dossiers autorises sont : fonctions, P.V et contrats.
 */
@Service
public class FileStorageService implements IFileStorageService {

    // Sous-repertoires autorises pour chaque projet
    private static final Set<String> ALLOWED_SUBDIRS = Set.of("fonctions", "P.V", "contrats");

    // Repertoire racine de stockage (configurable via application.properties)
    private final Path baseDir;

    // Initialise le repertoire de base a partir de la configuration
    public FileStorageService(@Value("${file.upload.base-dir:./projects}") String baseDirStr) {
        this.baseDir = Paths.get(baseDirStr).toAbsolutePath().normalize();
    }

    // Cree les sous-dossiers standard (fonctions, P.V, contrats) pour un nouveau projet
    public void createProjectFolders(String projectName) throws IOException {
        String safeName = sanitize(projectName);
        for (String sub : ALLOWED_SUBDIRS) {
            Files.createDirectories(baseDir.resolve(safeName).resolve(sub));
        }
    }

    // Enregistre un fichier dans le sous-dossier specifie d'un projet. Retourne le nom du fichier sauvegarde.
    public String storeFile(String projectName, String subdirectory, MultipartFile file) throws IOException {
        validateSubdirectory(subdirectory);
        String safeName = sanitize(projectName);
        Path dir = baseDir.resolve(safeName).resolve(subdirectory).normalize();
        Files.createDirectories(dir);

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new IllegalArgumentException("File name is missing");
        }
        // Rejet des caracteres de controle (null bytes, etc. — techniques d'evasion sur certains FS)
        for (int i = 0; i < originalName.length(); i++) {
            if (originalName.charAt(i) < 32) {
                throw new IllegalArgumentException("Invalid file name (control characters not allowed)");
            }
        }
        // Extraction du nom de fichier seul (strip des composants de chemin)
        String filename = Paths.get(originalName).getFileName().toString();
        Path target = dir.resolve(filename).normalize();

        // Defense en profondeur : verifier que le chemin final reste sous le dossier projet.
        // Bloque les CVE de type "Zip Slip" / path traversal meme si getFileName() est contourne.
        if (!target.startsWith(dir)) {
            throw new IllegalArgumentException("Path traversal attempt detected");
        }

        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return filename;
    }

    // Liste les fichiers presents dans un sous-dossier d'un projet
    public List<String> listFiles(String projectName, String subdirectory) throws IOException {
        validateSubdirectory(subdirectory);
        String safeName = sanitize(projectName);
        Path dir = baseDir.resolve(safeName).resolve(subdirectory).normalize();

        // Defense en profondeur : verifier que dir reste sous baseDir
        if (!dir.startsWith(baseDir)) {
            throw new IllegalArgumentException("Path traversal attempt detected");
        }

        if (!Files.exists(dir)) {
            return List.of();
        }

        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
        }
    }

    // Verifie que le sous-dossier demande fait partie des sous-dossiers autorises
    private void validateSubdirectory(String subdirectory) {
        if (!ALLOWED_SUBDIRS.contains(subdirectory)) {
            throw new IllegalArgumentException(
                    "Invalid subdirectory: " + subdirectory + ". Allowed: " + ALLOWED_SUBDIRS);
        }
    }

    // Nettoie le nom du projet pour un usage sur le systeme de fichiers (supprime les caracteres speciaux)
    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9._\\-\\s]", "_").trim();
    }
}
