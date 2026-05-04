package org.pfe.ppm_project.services;

import lombok.RequiredArgsConstructor;
import org.pfe.ppm_project.dto.TaskDependencyCreateRequest;
import org.pfe.ppm_project.dto.TaskDependencyDTO;
import org.pfe.ppm_project.entities.Task;
import org.pfe.ppm_project.entities.TaskDependency;
import org.pfe.ppm_project.enums.AuditAction;
import org.pfe.ppm_project.enums.DependencyType;
import org.pfe.ppm_project.exception.InvalidOperationException;
import org.pfe.ppm_project.exception.ResourceNotFoundException;
import org.pfe.ppm_project.repositories.TaskDependencyRepository;
import org.pfe.ppm_project.repositories.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Service metier pour la gestion des dependances entre taches.
 * Applique les regles metier : pas d'auto-dependance, pas de doublons,
 * uniquement entre taches actives du meme projet.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TaskDependencyService implements ITaskDependencyService {

    private final TaskDependencyRepository dependencyRepository;
    private final TaskRepository taskRepository;
    private final IAuditLogService auditLogService;

    // Cree une dependance entre deux taches apres validation des regles metier
    @Override
    public TaskDependencyDTO createDependency(TaskDependencyCreateRequest req) {

        // Validations des champs obligatoires
        if (req == null) throw new InvalidOperationException("request is required");
        if (req.predecessorTaskId() == null) throw new InvalidOperationException("predecessorTaskId is required");
        if (req.successorTaskId() == null) throw new InvalidOperationException("successorTaskId is required");

        // Regle : une tache ne peut pas dependre d'elle-meme
        if (req.predecessorTaskId().equals(req.successorTaskId())) {
            throw new InvalidOperationException("A task cannot depend on itself");
        }

        // Verification anti-doublon (avant la contrainte unique en base)
        if (dependencyRepository.existsByPredecessor_IdAndSuccessor_Id(req.predecessorTaskId(), req.successorTaskId())) {
            throw new InvalidOperationException("Dependency already exists");
        }

        // Detection de cycle : verifier si le predecesseur est deja atteignable depuis le successeur
        // (evite les dependances circulaires ex: A->B->C->A)
        if (wouldCreateCycle(req.predecessorTaskId(), req.successorTaskId())) {
            throw new InvalidOperationException("Circular dependency detected: adding this dependency would create a cycle");
        }

        // Chargement des taches depuis la base (necessite pour acceder au projet)
        Task predecessor = taskRepository.findById(req.predecessorTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("Predecessor task not found: " + req.predecessorTaskId()));

        Task successor = taskRepository.findById(req.successorTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("Successor task not found: " + req.successorTaskId()));

        // Regle : les taches inactives ne peuvent pas avoir de dependances
        if (!predecessor.isActive() || !successor.isActive()) {
            throw new InvalidOperationException("Cannot create dependency with inactive tasks");
        }

        // Regle : les deux taches doivent appartenir au meme projet
        Long predProjectId = predecessor.getProject().getId();
        Long succProjectId = successor.getProject().getId();

        if (predProjectId == null || succProjectId == null || !predProjectId.equals(succProjectId)) {
            throw new InvalidOperationException("Dependency must be between tasks of the same project");
        }

        // Type par defaut : FS (Fin-Debut) si non specifie
        DependencyType type = (req.type() != null) ? req.type() : DependencyType.FS;

        TaskDependency dep = TaskDependency.builder()
                .predecessor(predecessor)
                .successor(successor)
                .type(type)
                .build();

        TaskDependency saved = dependencyRepository.save(dep);

        Long projectId = predecessor.getProject() != null ? predecessor.getProject().getId() : null;
        auditLogService.log(
                AuditAction.DEPENDENCY_ADD, "DEPENDENCY", saved.getId(),
                predecessor.getName() + " -> " + successor.getName(),
                "Dependency " + saved.getType() + " added: '" + predecessor.getName()
                        + "' -> '" + successor.getName() + "'",
                projectId, null
        );

        return toDTO(saved);
    }

    // Retourne les dependances ou la tache donnee est successeur (ses predecesseurs)
    @Override
    @Transactional(readOnly = true)
    public List<TaskDependencyDTO> getPredecessorsOfTask(Long taskId) {
        if (taskId == null) throw new InvalidOperationException("taskId is required");
        return dependencyRepository.findBySuccessor_Id(taskId).stream()
                .map(this::toDTO)
                .toList();
    }

    // Retourne les dependances ou la tache donnee est predecesseur (ses successeurs)
    @Override
    @Transactional(readOnly = true)
    public List<TaskDependencyDTO> getSuccessorsOfTask(Long taskId) {
        if (taskId == null) throw new InvalidOperationException("taskId is required");
        return dependencyRepository.findByPredecessor_Id(taskId).stream()
                .map(this::toDTO)
                .toList();
    }

    // Supprime une dependance par son identifiant
    @Override
    public void deleteDependency(Long id) {
        if (id == null) throw new InvalidOperationException("id is required");
        // Charge avant suppression pour capturer le contexte dans l'audit
        TaskDependency dep = dependencyRepository.findById(id).orElse(null);
        dependencyRepository.deleteById(id);
        if (dep != null) {
            Task pred = dep.getPredecessor();
            Long projectId = pred != null && pred.getProject() != null ? pred.getProject().getId() : null;
            auditLogService.log(
                    AuditAction.DEPENDENCY_REMOVE, "DEPENDENCY", id,
                    pred != null ? pred.getName() + " -> " +
                            (dep.getSuccessor() != null ? dep.getSuccessor().getName() : "?") : "?",
                    "Dependency removed",
                    projectId, null
            );
        }
    }

    // Retourne toutes les dependances d'un projet (via le projet du predecesseur)
    @Override
    @Transactional(readOnly = true)
    public List<TaskDependencyDTO> getByProject(Long projectId) {
        if (projectId == null) throw new InvalidOperationException("projectId is required");
        return dependencyRepository.findByPredecessor_Project_Id(projectId).stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Verifie si l'ajout de la dependance predecessorId -> successorId creerait un cycle.
     *
     * ALGORITHME — BFS (Breadth-First Search) sur le graphe oriente des dependances :
     *   - On veut ajouter l'arete  P → S  (predecesseur → successeur).
     *   - Un cycle serait cree si P est deja atteignable en partant de S.
     *   - On lance un BFS depuis S en suivant les aretes sortantes existantes.
     *   - Si on tombe sur P, le cycle existerait → on rejette la creation.
     *
     * EXEMPLE CONCRET :
     *   Graphe existant : A → B → C
     *   Tentative d'ajout : C → A (predecessorId=C, successorId=A)
     *   BFS depuis A :
     *     file = [A]  visited = {}
     *     depile A → successeurs de A = {B} → file = [B]  visited = {A}
     *     depile B → successeurs de B = {C} → file = [C]  visited = {A, B}
     *     depile C → C == predecessorId (C) → CYCLE DETECTE → return true
     *
     * POURQUOI BFS et pas DFS ?
     *   Les deux fonctionnent pour la detection de cycle.
     *   BFS utilise une file explicite (ArrayDeque) → pas de StackOverflowError
     *   sur des graphes tres profonds, contrairement a la recursion DFS.
     *
     * IMPORTANCE metier :
     *   Un cycle rendrait propagateProgressToParent() (TaskService) infiniment recursif.
     *   La detection doit avoir lieu AVANT la sauvegarde en base.
     *
     * COMPLEXITE : O(N + E) ou N = taches et E = dependances du projet.
     */
    private boolean wouldCreateCycle(Long predecessorId, Long successorId) {
        Set<Long> visited = new HashSet<>();
        Queue<Long> queue = new ArrayDeque<>();
        queue.add(successorId);

        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (current.equals(predecessorId)) return true;
            if (!visited.add(current)) continue; // deja explore — evite les boucles sur graphes avec multi-chemins

            // Suivre tous les successeurs du noeud courant (aretes sortantes)
            dependencyRepository.findByPredecessor_Id(current)
                    .forEach(dep -> {
                        if (dep.getSuccessor() != null) {
                            queue.add(dep.getSuccessor().getId());
                        }
                    });
        }
        return false; // predecessorId non atteignable depuis successorId — pas de cycle
    }

    // Convertit une entite TaskDependency en DTO avec les noms des taches
    private TaskDependencyDTO toDTO(TaskDependency d) {
        Task pred = d.getPredecessor();
        Task succ = d.getSuccessor();
        return new TaskDependencyDTO(
                d.getId(),
                pred != null ? pred.getId() : null,
                pred != null ? pred.getName() : null,
                succ != null ? succ.getId() : null,
                succ != null ? succ.getName() : null,
                d.getType(),
                d.getCreatedAt()
        );
    }
}
