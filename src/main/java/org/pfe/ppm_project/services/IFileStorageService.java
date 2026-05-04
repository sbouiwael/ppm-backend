package org.pfe.ppm_project.services;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Abstraction pour le stockage de fichiers de projet.
 * L'implementation par defaut est locale (FileStorageService).
 * En production cloud, on pourra substituer une implementation S3/MinIO
 * sans modifier les controllers.
 */
public interface IFileStorageService {

    /**
     * Cree les sous-dossiers standard pour un nouveau projet.
     */
    void createProjectFolders(String projectName) throws IOException;

    /**
     * Enregistre un fichier dans le sous-dossier specifie d'un projet.
     *
     * @return le nom du fichier sauvegarde
     */
    String storeFile(String projectName, String subdirectory, MultipartFile file) throws IOException;

    /**
     * Liste les fichiers presents dans un sous-dossier d'un projet.
     *
     * @return liste triee des noms de fichiers
     */
    List<String> listFiles(String projectName, String subdirectory) throws IOException;
}