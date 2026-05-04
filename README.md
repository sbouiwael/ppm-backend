# PPM Project - Backend (Spring Boot)

## Project Portfolio Management - API REST

Application backend pour la gestion de portefeuilles de projets (PPM), construite avec **Spring Boot 3.4.5** et **Java 21**. Cette API fournit tous les services necessaires pour gerer des projets, taches, utilisateurs, portefeuilles, affectations de ressources, dependances de taches et fichiers de projet.

---

## Table des matieres

1. [Stack technique](#stack-technique)
2. [Architecture du projet](#architecture-du-projet)
3. [Pre-requis](#pre-requis)
4. [Installation et demarrage](#installation-et-demarrage)
5. [Configuration](#configuration)
6. [Schema de la base de donnees](#schema-de-la-base-de-donnees)
7. [Entites et modele de donnees](#entites-et-modele-de-donnees)
8. [DTOs (Data Transfer Objects)](#dtos-data-transfer-objects)
9. [Repositories](#repositories)
10. [Services et logique metier](#services-et-logique-metier)
11. [API Endpoints](#api-endpoints)
12. [Securite et CORS](#securite-et-cors)
13. [Gestion des fichiers](#gestion-des-fichiers)
14. [Enumerations](#enumerations)
15. [Documentation API (Swagger)](#documentation-api-swagger)
16. [Tests](#tests)
17. [Structure des dossiers](#structure-des-dossiers)

---

## Stack technique

| Composant          | Technologie              | Version   |
|--------------------|--------------------------|-----------|
| Langage            | Java                     | 21        |
| Framework          | Spring Boot              | 3.4.5     |
| ORM                | Spring Data JPA/Hibernate| -         |
| Base de donnees    | MySQL                    | 8+        |
| Securite           | Spring Security          | -         |
| Documentation API  | Springdoc OpenAPI/Swagger | 2.7.0     |
| Build              | Apache Maven             | 3.9+      |
| Boilerplate        | Lombok                   | 1.18.42   |
| Serveur            | Tomcat embarque          | -         |

---

## Architecture du projet

Le projet suit une architecture **en couches** (layered architecture) :

```
Controller (REST API)
    |
    v
Service (Logique metier)
    |
    v
Repository (Acces donnees - JPA)
    |
    v
Entity (Modele de donnees - Hibernate/MySQL)
```

**Patterns utilises :**
- **DTO Pattern** : Separation entre entites JPA et objets de transfert API (records Java)
- **Mapper Pattern** : Classes utilitaires statiques pour la conversion Entity <-> DTO
- **Interface-Implementation** : Chaque service a une interface (`IXxxService`) et son implementation
- **Soft Delete** : Desactivation (champ `active=false`) au lieu de suppression physique
- **Normalisation** : Validation et normalisation des donnees via `@PrePersist` / `@PreUpdate`

---

## Pre-requis

- **Java 21** (JDK) installe et configure dans le `PATH`
- **MySQL 8+** en cours d'execution
- **Maven 3.9+** (ou utiliser le wrapper `mvnw` inclus)
- **Port 8082** disponible

---

## Installation et demarrage

### 1. Cloner le repository

```bash
git clone <url-du-repo>
cd back-main
```

### 2. Creer la base de donnees MySQL

```sql
CREATE DATABASE PPM;
```

> La creation des tables est automatique (`ddl-auto=update`).

### 3. Configurer la connexion (si necessaire)

Editer `src/main/resources/application.properties` :

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/PPM?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=
```

### 4. Lancer l'application

**Avec Maven wrapper :**
```bash
./mvnw spring-boot:run
```

**Avec Maven installe :**
```bash
mvn spring-boot:run
```

**Ou compiler puis executer :**
```bash
mvn clean package -DskipTests
java -jar target/PPM_project-0.0.1-SNAPSHOT.jar
```

L'API est accessible sur : `http://localhost:8082`

---

## Configuration

**Fichier : `src/main/resources/application.properties`**

| Propriete | Valeur | Description |
|-----------|--------|-------------|
| `server.port` | `8082` | Port du serveur |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/PPM?createDatabaseIfNotExist=true` | URL de la base MySQL |
| `spring.datasource.username` | `root` | Utilisateur MySQL |
| `spring.datasource.password` | *(vide)* | Mot de passe MySQL |
| `spring.jpa.hibernate.ddl-auto` | `update` | Generation automatique du schema |
| `spring.jpa.show-sql` | `true` | Afficher les requetes SQL dans la console |
| `file.upload.base-dir` | `./projects` | Repertoire de base pour les fichiers uploades |
| `spring.servlet.multipart.max-file-size` | `50MB` | Taille max par fichier |
| `spring.servlet.multipart.max-request-size` | `50MB` | Taille max par requete |

---

## Schema de la base de donnees

```
users
  |-- id (PK, BIGINT, AUTO_INCREMENT)
  |-- first_name (VARCHAR, NOT NULL)
  |-- last_name (VARCHAR, NOT NULL)
  |-- email (VARCHAR, NOT NULL, UNIQUE)
  |-- password (VARCHAR, NOT NULL)
  |-- role (ENUM: PM, PMO, DEV, QA, DEVOPS, RH, ADMIN)
  |-- weekly_capacity (INT, NOT NULL)
  |-- active (BOOLEAN, DEFAULT true)
  |-- created_at (DATETIME, NOT NULL)

portefeuilles
  |-- id (PK, BIGINT, AUTO_INCREMENT)
  |-- nom (VARCHAR, NOT NULL)
  |-- description (VARCHAR(2000), NULLABLE)

projects
  |-- id (PK, BIGINT, AUTO_INCREMENT)
  |-- name (VARCHAR, NOT NULL)
  |-- description (VARCHAR(2000), NULLABLE)
  |-- start_date (DATE, NOT NULL)
  |-- end_date (DATE, NULLABLE)
  |-- active (BOOLEAN, DEFAULT true)
  |-- progress (INT, 0-100, DEFAULT 0)
  |-- project_manager_id (FK -> users.id, NOT NULL)
  |-- portefeuille_id (FK -> portefeuilles.id, NULLABLE)
  |-- portfolio_name (VARCHAR, NULLABLE)
  |-- program_name (VARCHAR, NULLABLE)
  |-- sub_program_name (VARCHAR, NULLABLE)
  |-- objective (VARCHAR, NULLABLE)
  |-- calendar_name (VARCHAR, NULLABLE)
  |-- baseline_start_date (DATE, NULLABLE)
  |-- baseline_end_date (DATE, NULLABLE)
  |-- created_at (DATETIME, NOT NULL)
  |-- updated_at (DATETIME)

tasks
  |-- id (PK, BIGINT, AUTO_INCREMENT)
  |-- name (VARCHAR, NOT NULL)
  |-- description (VARCHAR(2000), NULLABLE)
  |-- project_id (FK -> projects.id, NOT NULL)
  |-- parent_task_id (FK -> tasks.id, NULLABLE)
  |-- wbs_number (VARCHAR, DEFAULT '1')
  |-- mode (VARCHAR, DEFAULT 'TASK')
  |-- duration_days (DOUBLE, NOT NULL, DEFAULT 1.0)
  |-- work_hours (DOUBLE, NOT NULL, DEFAULT 8.0)
  |-- baseline_duration_days (DOUBLE, NULLABLE)
  |-- baseline_start_date (DATE, NULLABLE)
  |-- baseline_end_date (DATE, NULLABLE)
  |-- actual_work_hours (DOUBLE, NULLABLE)
  |-- calendar_name (VARCHAR, NULLABLE)
  |-- sort_order (INT, NOT NULL, DEFAULT 0)
  |-- start_date (DATE, NULLABLE)
  |-- end_date (DATE, NULLABLE)
  |-- status (ENUM: NOT_STARTED, IN_PROGRESS, DONE, BLOCKED)
  |-- progress (INT, 0-100, DEFAULT 0)
  |-- active (BOOLEAN, DEFAULT true)
  |-- created_at (DATETIME, NOT NULL)
  |-- updated_at (DATETIME)

task_assignments
  |-- id (PK, BIGINT, AUTO_INCREMENT)
  |-- task_id (FK -> tasks.id, NOT NULL)
  |-- user_id (FK -> users.id, NOT NULL)
  |-- assigned_hours (INT, NOT NULL)
  |-- active (BOOLEAN, DEFAULT true)
  |-- created_at (DATETIME, NOT NULL)
  |-- UNIQUE(task_id, user_id)

task_dependencies
  |-- id (PK, BIGINT, AUTO_INCREMENT)
  |-- predecessor_task_id (FK -> tasks.id, NOT NULL)
  |-- successor_task_id (FK -> tasks.id, NOT NULL)
  |-- type (ENUM: FS, SS, FF, SF, DEFAULT FS)
  |-- created_at (DATETIME, NOT NULL)
  |-- UNIQUE(predecessor_task_id, successor_task_id)
```

### Relations

```
users 1 ──< N projects          (projectManager)
users 1 ──< N task_assignments  (user)
portefeuilles 1 ──< N projects  (portefeuille)
projects 1 ──< N tasks          (project)
tasks 1 ──< N tasks             (parentTask - hierarchie)
tasks 1 ──< N task_assignments  (task)
tasks 1 ──< N task_dependencies (predecessor)
tasks 1 ──< N task_dependencies (successor)
```

---

## Entites et modele de donnees

### User (`entities/User.java`)

| Champ | Type | Contraintes | Description |
|-------|------|-------------|-------------|
| `id` | Long | PK, auto-genere | Identifiant unique |
| `firstName` | String | NOT NULL | Prenom |
| `lastName` | String | NOT NULL | Nom de famille |
| `email` | String | NOT NULL, UNIQUE | Adresse email |
| `password` | String | NOT NULL | Mot de passe |
| `role` | Role (enum) | NOT NULL | Role dans l'organisation |
| `weeklyCapacity` | Integer | NOT NULL | Capacite hebdomadaire (heures) |
| `active` | boolean | DEFAULT true | Statut actif/inactif |
| `createdAt` | LocalDateTime | NOT NULL, immutable | Date de creation |

### Portefeuille (`entities/Portefeuille.java`)

| Champ | Type | Contraintes | Description |
|-------|------|-------------|-------------|
| `id` | Long | PK, auto-genere | Identifiant unique |
| `nom` | String | NOT NULL | Nom du portefeuille |
| `description` | String | VARCHAR(2000), nullable | Description detaillee |
| `projects` | List\<Project\> | OneToMany | Projets associes |

### Project (`entities/Project.java`)

| Champ | Type | Contraintes | Description |
|-------|------|-------------|-------------|
| `id` | Long | PK, auto-genere | Identifiant unique |
| `name` | String | NOT NULL | Nom du projet |
| `description` | String | VARCHAR(2000), nullable | Description |
| `startDate` | LocalDate | NOT NULL | Date de debut |
| `endDate` | LocalDate | nullable | Date de fin |
| `active` | boolean | DEFAULT true | Statut actif |
| `progress` | Integer | 0-100, DEFAULT 0 | Progression en % |
| `projectManager` | User | ManyToOne, NOT NULL | Chef de projet |
| `portefeuille` | Portefeuille | ManyToOne, nullable | Portefeuille parent |
| `portfolioName` | String | nullable | Nom portfolio (MS Project) |
| `programName` | String | nullable | Nom programme |
| `subProgramName` | String | nullable | Nom sous-programme |
| `objective` | String | nullable | Objectif strategique |
| `calendarName` | String | nullable | Nom du calendrier |
| `baselineStartDate` | LocalDate | nullable | Debut planifie (baseline) |
| `baselineEndDate` | LocalDate | nullable | Fin planifiee (baseline) |
| `createdAt` | LocalDateTime | NOT NULL, immutable | Date de creation |
| `updatedAt` | LocalDateTime | auto-update | Derniere modification |

**Normalisation automatique :** Validation dates (debut <= fin), progress 0-100, trim des chaines.

### Task (`entities/Task.java`)

| Champ | Type | Contraintes | Description |
|-------|------|-------------|-------------|
| `id` | Long | PK | Identifiant |
| `name` | String | NOT NULL | Nom de la tache |
| `description` | String | VARCHAR(2000) | Description |
| `project` | Project | ManyToOne, NOT NULL | Projet parent |
| `parentTask` | Task | ManyToOne, nullable | Tache parente (hierarchie WBS) |
| `wbsNumber` | String | DEFAULT '1' | Numero WBS |
| `mode` | String | DEFAULT 'TASK' | Mode (TASK, etc.) |
| `durationDays` | Double | NOT NULL, DEFAULT 1.0 | Duree en jours |
| `workHours` | Double | NOT NULL, DEFAULT 8.0 | Heures de travail |
| `baselineDurationDays` | Double | nullable | Duree baseline |
| `baselineStartDate` | LocalDate | nullable | Debut baseline |
| `baselineEndDate` | LocalDate | nullable | Fin baseline |
| `actualWorkHours` | Double | nullable | Heures reelles |
| `calendarName` | String | nullable | Nom calendrier |
| `sortOrder` | Integer | NOT NULL, DEFAULT 0 | Ordre d'affichage |
| `startDate` | LocalDate | nullable | Date de debut |
| `endDate` | LocalDate | nullable | Date de fin (calculee) |
| `status` | TaskStatus | NOT NULL, DEFAULT NOT_STARTED | Statut |
| `progress` | Integer | 0-100, DEFAULT 0 | Progression % |
| `active` | boolean | DEFAULT true | Statut actif |

**Normalisation automatique :** Calcul de `endDate` = `startDate` + `durationDays`, validation des bornes.

### TaskAssignment (`entities/TaskAssignment.java`)

| Champ | Type | Contraintes | Description |
|-------|------|-------------|-------------|
| `id` | Long | PK | Identifiant |
| `task` | Task | ManyToOne, NOT NULL | Tache assignee |
| `user` | User | ManyToOne, NOT NULL | Utilisateur assigne |
| `assignedHours` | Integer | NOT NULL, >= 0 | Heures affectees |
| `active` | boolean | DEFAULT true | Statut actif |
| `createdAt` | LocalDateTime | NOT NULL | Date de creation |

**Contrainte UNIQUE** sur `(task_id, user_id)`.

### TaskDependency (`entities/TaskDependency.java`)

| Champ | Type | Contraintes | Description |
|-------|------|-------------|-------------|
| `id` | Long | PK | Identifiant |
| `predecessor` | Task | ManyToOne, NOT NULL | Tache predecesseur |
| `successor` | Task | ManyToOne, NOT NULL | Tache successeur |
| `type` | DependencyType | DEFAULT FS | Type de dependance |
| `createdAt` | LocalDateTime | NOT NULL | Date de creation |

**Contrainte UNIQUE** sur `(predecessor_task_id, successor_task_id)`.

---

## DTOs (Data Transfer Objects)

Tous les DTOs sont des **Java Records** (immutables) :

| DTO | Champs principaux | Usage |
|-----|-------------------|-------|
| `UserDTO` | id, firstName, lastName, email, role, weeklyCapacity, active, createdAt | Reponse API utilisateurs |
| `ProjectDTO` | id, name, description, dates, active, progress, portfolioFields, portefeuilleId | Reponse API projets |
| `ProjectCreateUpdateDTO` | Tous sauf id et portefeuilleId | Creation/modification projets |
| `TaskDTO` | id, name, description, projectId, parentTaskId, ganttFields, status, progress | Reponse API taches |
| `TaskAssignmentDTO` | id, taskId, userId, assignedHours, active, createdAt | Reponse API affectations |
| `TaskDependencyDTO` | id, predecessorTaskId, successorTaskId, type, createdAt | Reponse API dependances |
| `TaskDependencyCreateRequest` | predecessorTaskId, successorTaskId, type | Creation dependances |
| `PortefeuilleDTO` | id, nom, description, projects (List\<ProjectDTO\>) | Reponse API portefeuilles |
| `PortefeuilleCreateUpdateDTO` | nom, description | Creation/modification portefeuilles |
| `MyTaskDTO` | assignmentId, taskId, taskName, taskStatus, taskProgress, startDate, endDate, wbsNumber, assignedHours, projectId, projectName, durationDays, workHours | Tableau de bord personnel (`/assignments/me`) |
| `ResourceCapacityDTO` | userId, firstName, lastName, email, role, weeklyCapacity, totalAssignedHours, utilizationPct, capacityStatus | Vue de charge (`/capacity`) |

---

## Repositories

| Repository | Entite | Methodes personnalisees |
|------------|--------|-------------------------|
| `UserRepository` | User | `findByEmail(String email)` |
| `PortefeuilleRepository` | Portefeuille | *(aucune)* |
| `ProjectRepository` | Project | `findByProjectManagerId(Long)`, `findByActiveTrue()` |
| `TaskRepository` | Task | `findByProjectId(Long)`, `findByProjectIdOrderBySortOrderAsc(Long)`, `findByParentTaskId(Long)` |
| `TaskAssignmentRepository` | TaskAssignment | `findByTaskId`, `findByUserId`, `sumActiveAssignedHoursByTask` (JPQL), `existsByTaskIdAndUserId`, `findByTaskIdWithRefs` (eager load), `findByUserIdWithRefs` (eager load), `sumActiveAssignedHoursByTaskExcluding` |
| `TaskDependencyRepository` | TaskDependency | `findBySuccessor_Id`, `findByPredecessor_Id`, `existsByPredecessor_IdAndSuccessor_Id`, `findByPredecessor_Project_Id` |

---

## Services et logique metier

### UserService

| Methode | Description |
|---------|-------------|
| `createUser(User)` | Cree un nouvel utilisateur |
| `getAllUsers()` | Retourne tous les utilisateurs |
| `getUserById(Long)` | Retourne un utilisateur par son ID |
| `getUserByEmail(String)` | Retourne un utilisateur par email |
| `updateUser(Long, User)` | Met a jour un utilisateur (exception si introuvable) |
| `deactivateUser(Long)` | Desactive un utilisateur (soft delete) |

### ProjectService

| Methode | Description |
|---------|-------------|
| `create(ProjectCreateUpdateDTO)` | Cree un projet (valide name, startDate, managerId; cree les dossiers fichiers) |
| `getAll()` | Retourne tous les projets en DTO |
| `getById(Long)` | Retourne un projet par ID en DTO |
| `getByManager(Long)` | Retourne les projets d'un chef de projet |
| `update(Long, ProjectCreateUpdateDTO)` | Met a jour un projet |
| `deactivate(Long)` | Desactive un projet |

### TaskService

| Methode | Description |
|---------|-------------|
| `createTask(Task)` | Cree une tache |
| `getTasksByProject(Long)` | Retourne les taches d'un projet (triees par sortOrder) |
| `getTaskById(Long)` | Retourne une tache par ID |
| `updateTask(Long, Task)` | Met a jour tous les champs d'une tache |
| `deactivateTask(Long)` | Desactive une tache |

### TaskAssignmentService

| Methode | Description | Validation |
|---------|-------------|------------|
| `assignUserToTask(TaskAssignment)` | Affecte un utilisateur a une tache | Verifie duplication, heures <= workHours |
| `getAssignmentsByTask(Long)` | Affectations d'une tache (eager load) | - |
| `getAssignmentsByUser(Long)` | Affectations d'un utilisateur (eager load) | - |
| `updateAssignedHours(Long, Integer)` | Met a jour les heures affectees | Verifie total <= workHours |
| `deactivateAssignment(Long)` | Desactive une affectation | - |

**Regle metier cle :** La somme des heures affectees a une tache ne peut pas depasser les `workHours` de la tache.

### TaskDependencyService

| Methode | Description | Validation |
|---------|-------------|------------|
| `createDependency(TaskDependencyCreateRequest)` | Cree une dependance | Anti-boucle, meme projet, pas de doublons, pas de taches inactives |
| `getPredecessorsOfTask(Long)` | Predecesseurs d'une tache | - |
| `getSuccessorsOfTask(Long)` | Successeurs d'une tache | - |
| `deleteDependency(Long)` | Supprime une dependance | - |
| `getByProject(Long)` | Toutes les dependances d'un projet | - |

**Validations :**
- Interdit les auto-dependances (tache ne peut pas dependre d'elle-meme)
- Interdit les doublons
- Les deux taches doivent appartenir au meme projet
- Les taches inactives ne peuvent pas avoir de dependances
- Type null converti en FS (Finish-to-Start) par defaut

### PortefeuilleService

| Methode | Description |
|---------|-------------|
| `create(PortefeuilleCreateUpdateDTO)` | Cree un portefeuille |
| `getAll()` | Retourne tous les portefeuilles avec projets |
| `getById(Long)` | Retourne un portefeuille par ID |
| `update(Long, PortefeuilleCreateUpdateDTO)` | Met a jour un portefeuille |
| `delete(Long)` | Supprime (desaffecte tous les projets d'abord) |
| `addProject(Long, Long)` | Ajoute un projet au portefeuille |
| `removeProject(Long, Long)` | Retire un projet du portefeuille |
| `getUnassignedProjects()` | Projets sans portefeuille |

### CapacityService

| Methode | Description |
|---------|-------------|
| `getCapacityOverview(role, projectId)` | Retourne la liste des utilisateurs actifs avec leur taux d'utilisation. Filtre optionnel par role ou projet. Trie par utilizationPct decroissant. |

**Logique :** Pour chaque utilisateur actif, somme les `assignedHours` de toutes ses affectations actives sur des taches actives (optionnellement scope a un projet). Calcule `utilizationPct` = `totalAssignedHours / weeklyCapacity * 100`.

### FileStorageService

| Methode | Description |
|---------|-------------|
| `createProjectFolders(String)` | Cree les sous-dossiers (fonctions, P.V, contrats) |
| `storeFile(String, String, MultipartFile)` | Upload un fichier dans un sous-dossier du projet |
| `listFiles(String, String)` | Liste les fichiers d'un sous-dossier |

**Sous-dossiers autorises :** `fonctions`, `P.V`, `contrats`

---

## API Endpoints

### Users (`/api/users`)

| Methode | URL | Corps requete | Reponse | Description |
|---------|-----|---------------|---------|-------------|
| `POST` | `/api/users` | User JSON | `200` UserDTO | Creer un utilisateur |
| `GET` | `/api/users` | - | `200` List\<UserDTO\> | Lister tous les utilisateurs |
| `GET` | `/api/users/{id}` | - | `200` UserDTO / `404` | Obtenir par ID |
| `GET` | `/api/users/email/{email}` | - | `200` UserDTO / `404` | Obtenir par email |
| `PUT` | `/api/users/{id}` | User JSON | `200` UserDTO | Modifier un utilisateur |
| `DELETE` | `/api/users/{id}` | - | `204` No Content | Desactiver un utilisateur |

### Projects (`/api/projects`)

| Methode | URL | Corps requete | Reponse | Description |
|---------|-----|---------------|---------|-------------|
| `POST` | `/api/projects` | ProjectCreateUpdateDTO | `200` ProjectDTO / `400` | Creer un projet |
| `GET` | `/api/projects` | - | `200` List\<ProjectDTO\> | Lister tous les projets |
| `GET` | `/api/projects/{id}` | - | `200` ProjectDTO / `404` | Obtenir par ID |
| `GET` | `/api/projects/manager/{managerId}` | - | `200` List\<ProjectDTO\> | Projets d'un chef de projet |
| `PUT` | `/api/projects/{id}` | ProjectCreateUpdateDTO | `200` ProjectDTO / `400` | Modifier un projet |
| `DELETE` | `/api/projects/{id}` | - | `204` / `404` | Desactiver un projet |

### Tasks (`/api/tasks`)

| Methode | URL | Corps requete | Reponse | Description |
|---------|-----|---------------|---------|-------------|
| `POST` | `/api/tasks` | TaskDTO | `201` TaskDTO | Creer une tache |
| `GET` | `/api/tasks/project/{projectId}` | - | `200` List\<TaskDTO\> | Taches d'un projet |
| `GET` | `/api/tasks/{id}` | - | `200` TaskDTO / `404` | Obtenir par ID |
| `PUT` | `/api/tasks/{id}` | TaskDTO | `200` TaskDTO | Modifier une tache |
| `DELETE` | `/api/tasks/{id}` | - | `204` | Desactiver une tache |

### Task Assignments (`/api/assignments`)

| Methode | URL | Corps requete | Reponse | Description |
|---------|-----|---------------|---------|-------------|
| `POST` | `/api/assignments` | TaskAssignmentDTO | `201` TaskAssignmentDTO | Affecter un utilisateur |
| `GET` | `/api/assignments/task/{taskId}` | - | `200` List\<TaskAssignmentDTO\> | Affectations par tache |
| `GET` | `/api/assignments/user/{userId}` | - | `200` List\<TaskAssignmentDTO\> | Affectations par utilisateur |
| `GET` | `/api/assignments/me` | - | `200` List\<MyTaskDTO\> | **Mes taches** — taches assignees a l'utilisateur courant (JWT) |
| `PUT` | `/api/assignments/{id}/hours/{hours}` | - | `200` TaskAssignmentDTO | Modifier heures |
| `DELETE` | `/api/assignments/{id}` | - | `204` | Desactiver une affectation |

> `GET /api/assignments/me` : extrait le `userId` du token JWT courant, retourne uniquement les affectations actives sur des taches actives. Champ cle : `MyTaskDTO` contient taskName, taskStatus, taskProgress, projectName, wbsNumber, endDate pour le tableau de bord personnel.

### Task Dependencies (`/api/dependencies`)

| Methode | URL | Corps requete | Reponse | Description |
|---------|-----|---------------|---------|-------------|
| `POST` | `/api/dependencies` | TaskDependencyCreateRequest | `201` TaskDependencyDTO | Creer une dependance |
| `GET` | `/api/dependencies/predecessors/{taskId}` | - | `200` List\<TaskDependencyDTO\> | Predecesseurs |
| `GET` | `/api/dependencies/successors/{taskId}` | - | `200` List\<TaskDependencyDTO\> | Successeurs |
| `GET` | `/api/dependencies/project/{projectId}` | - | `200` List\<TaskDependencyDTO\> | Dependances du projet |
| `DELETE` | `/api/dependencies/{id}` | - | `204` | Supprimer une dependance |

### Portfolios (`/api/portefeuilles`)

| Methode | URL | Corps requete | Reponse | Description |
|---------|-----|---------------|---------|-------------|
| `POST` | `/api/portefeuilles` | PortefeuilleCreateUpdateDTO | `200` PortefeuilleDTO / `400` | Creer un portefeuille |
| `GET` | `/api/portefeuilles` | - | `200` List\<PortefeuilleDTO\> | Lister tous |
| `GET` | `/api/portefeuilles/{id}` | - | `200` PortefeuilleDTO / `404` | Obtenir par ID |
| `PUT` | `/api/portefeuilles/{id}` | PortefeuilleCreateUpdateDTO | `200` PortefeuilleDTO / `400` | Modifier |
| `DELETE` | `/api/portefeuilles/{id}` | - | `204` / `404` | Supprimer |
| `POST` | `/api/portefeuilles/{id}/projects/{projectId}` | - | `200` PortefeuilleDTO / `400` | Ajouter un projet |
| `DELETE` | `/api/portefeuilles/{id}/projects/{projectId}` | - | `200` PortefeuilleDTO / `400` | Retirer un projet |
| `GET` | `/api/portefeuilles/unassigned-projects` | - | `200` List\<ProjectDTO\> | Projets non affectes |

### Capacity Planning (`/api/capacity`)

| Methode | URL | Params query | Reponse | RBAC | Description |
|---------|-----|-------------|---------|------|-------------|
| `GET` | `/api/capacity` | `role` (opt), `projectId` (opt) | `200` List\<ResourceCapacityDTO\> | ADMIN, PMO, PM, RH | Vue de charge par ressource |

**Calcul :** `utilizationPct = (totalAssignedHours / weeklyCapacity) * 100`

**Statuts de capacite :**

| Statut | Seuil |
|--------|-------|
| `OVERLOADED` | > 100% |
| `BALANCED` | 75% – 100% |
| `UNDERUTILIZED` | < 75% |
| `NO_CAPACITY` | weeklyCapacity = 0 |

Filtres : `?role=DEV` (filtre par role), `?projectId=10` (heures limitees a ce projet). Resultats tries par `utilizationPct` decroissant.

---

### Files (`/api/projects/{projectId}/files`)

| Methode | URL | Params | Reponse | Description |
|---------|-----|--------|---------|-------------|
| `POST` | `/api/projects/{projectId}/files` | `file` (multipart), `subdirectory` (form) | `200` `{"filename":"..."}` | Upload un fichier |
| `GET` | `/api/projects/{projectId}/files` | `subdirectory` (query) | `200` List\<String\> | Lister les fichiers |

---

## Securite et CORS

### Securite (`config/SecurityConfig.java`)

Architecture **JWT stateless** :

- **CSRF** : Desactive (inutile sans session)
- **Sessions** : STATELESS — aucune session cote serveur
- **Authentification** : Token Bearer JWT dans le header `Authorization`
- **RBAC** : Double enforcement — `authorizeHttpRequests()` + `@PreAuthorize` sur les methodes

| Type d'endpoint | Acces |
|----------------|-------|
| `POST /api/auth/**` | Public (login/register) |
| `GET /swagger-ui/**`, `/v3/api-docs/**` | Public |
| `GET /actuator/health/**` | Public (Kubernetes probes) |
| `GET /actuator/**` | ADMIN uniquement |
| `* /api/**` | Token JWT valide requis |

**Headers de securite** : `X-Frame-Options: SAMEORIGIN`, `X-Content-Type-Options: nosniff`, HSTS 1 an, Referrer-Policy strict.

### Authentification (`/api/auth`)

| Methode | URL | Corps requete | Reponse | Description |
|---------|-----|---------------|---------|-------------|
| `POST` | `/api/auth/login` | `{"email":"...","password":"..."}` | `{"token":"...","user":{...}}` | Connexion — retourne un JWT |
| `POST` | `/api/auth/register` | User JSON | `201` UserDTO | Inscription (ADMIN only) |

Le token JWT encode : `userId`, `email`, `role`. Duree de validite configurable via `jwt.expiration`.

### CORS (`config/CorsConfig.java`)

| Parametre | Valeur |
|-----------|--------|
| Origins autorisees | `http://localhost:4200` |
| Methodes autorisees | GET, POST, PUT, DELETE, PATCH, OPTIONS |
| Headers autorises | `*` (tous) |
| Credentials | Non |

---

## Gestion des fichiers

Le systeme de fichiers cree automatiquement une arborescence a la creation d'un projet :

```
./projects/
  └── <nom-du-projet>/
      ├── fonctions/     (documents fonctionnels)
      ├── P.V/           (proces-verbaux)
      └── contrats/      (contrats et documents legaux)
```

**Contraintes :**
- Taille maximale par fichier : 50 MB
- Sous-dossiers autorises : `fonctions`, `P.V`, `contrats`
- Les noms de fichiers et projets sont assainis (caracteres speciaux supprimes)

---

## Enumerations

### Role
```
PM      - Chef de projet (Project Manager)
PMO     - Bureau de gestion de projet (Project Management Office)
DEV     - Developpeur
QA      - Assurance qualite
DEVOPS  - DevOps
RH      - Ressources humaines
ADMIN   - Administrateur
```

### TaskStatus
```
NOT_STARTED  - Non commencee
IN_PROGRESS  - En cours
DONE         - Terminee
BLOCKED      - Bloquee
```

### DependencyType
```
FS  - Finish to Start (Fin a Debut) - par defaut
SS  - Start to Start (Debut a Debut)
FF  - Finish to Finish (Fin a Fin)
SF  - Start to Finish (Debut a Fin)
```

---

## Documentation API (Swagger)

Une documentation interactive de l'API est disponible via Swagger UI :

```
http://localhost:8082/swagger-ui.html
```

Specification OpenAPI (JSON) :
```
http://localhost:8082/v3/api-docs
```

---

## Tests

Lancer les tests unitaires :

```bash
./mvnw test
```

### Suites de tests

| Suite | Tests | Description |
|-------|-------|-------------|
| `CapacityServiceTest` | 9 | Calcul charge, statuts OVERLOADED/BALANCED/UNDERUTILIZED/NO_CAPACITY, filtres role et projet |
| `MyTasksServiceTest` | 7 | Endpoint `/assignments/me`, extraction userId depuis JWT |
| `ProjectServiceTest` | 5 | Creation, desactivation, getAll |
| `UserServiceTest` | 8 | CRUD utilisateur |
| `LoginAttemptServiceTest` | 9 | Securite — tentatives de connexion |

**Echecs pre-existants (non lies a Wave 1) :**
- `TaskDependencyServiceTest` : 4 echecs (UnnecessaryStubbings + logique)
- `TaskServiceProgressTest` : 2 erreurs
- `PpmProjectApplicationTests.contextLoads` : necessite les variables d'env reelles

---

## Structure des dossiers

```
back-main/
├── pom.xml
├── mvnw / mvnw.cmd
├── src/
│   ├── main/
│   │   ├── java/org/pfe/ppm_project/
│   │   │   ├── PpmProjectApplication.java          (Point d'entree)
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java              (Configuration securite)
│   │   │   │   └── CorsConfig.java                  (Configuration CORS)
│   │   │   ├── enums/
│   │   │   │   ├── Role.java                        (PM, PMO, DEV, QA, DEVOPS, RH, ADMIN)
│   │   │   │   ├── TaskStatus.java                  (NOT_STARTED, IN_PROGRESS, DONE, BLOCKED)
│   │   │   │   └── DependencyType.java              (FS, SS, FF, SF)
│   │   │   ├── entities/
│   │   │   │   ├── User.java
│   │   │   │   ├── Portefeuille.java
│   │   │   │   ├── Project.java
│   │   │   │   ├── Task.java
│   │   │   │   ├── TaskAssignment.java
│   │   │   │   └── TaskDependency.java
│   │   │   ├── dto/
│   │   │   │   ├── UserDTO.java
│   │   │   │   ├── ProjectDTO.java
│   │   │   │   ├── ProjectCreateUpdateDTO.java
│   │   │   │   ├── TaskDTO.java
│   │   │   │   ├── TaskAssignmentDTO.java
│   │   │   │   ├── TaskDependencyDTO.java
│   │   │   │   ├── TaskDependencyCreateRequest.java
│   │   │   │   ├── PortefeuilleDTO.java
│   │   │   │   ├── PortefeuilleCreateUpdateDTO.java
│   │   │   │   ├── MyTaskDTO.java                       (Wave 1 — tableau de bord personnel)
│   │   │   │   └── ResourceCapacityDTO.java             (Wave 1 — planification capacite)
│   │   │   ├── mapper/
│   │   │   │   ├── UserMapper.java
│   │   │   │   ├── TaskMapper.java
│   │   │   │   └── TaskAssignmentMapper.java
│   │   │   ├── repositories/
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── PortefeuilleRepository.java
│   │   │   │   ├── ProjectRepository.java
│   │   │   │   ├── TaskRepository.java
│   │   │   │   ├── TaskAssignmentRepository.java
│   │   │   │   └── TaskDependencyRepository.java
│   │   │   ├── services/
│   │   │   │   ├── IUserService.java
│   │   │   │   ├── UserService.java
│   │   │   │   ├── IPortefeuilleService.java
│   │   │   │   ├── PortefeuilleService.java
│   │   │   │   ├── IProjectService.java
│   │   │   │   ├── ProjectService.java
│   │   │   │   ├── ITaskService.java
│   │   │   │   ├── TaskService.java
│   │   │   │   ├── ITaskAssignmentService.java
│   │   │   │   ├── TaskAssignmentService.java
│   │   │   │   ├── ITaskDependencyService.java
│   │   │   │   ├── TaskDependencyService.java
│   │   │   │   ├── FileStorageService.java
│   │   │   │   ├── ICapacityService.java                (Wave 1)
│   │   │   │   └── CapacityService.java                 (Wave 1)
│   │   │   └── controller/
│   │   │       ├── UserController.java
│   │   │       ├── PortefeuilleController.java
│   │   │       ├── ProjectController.java
│   │   │       ├── TaskController.java
│   │   │       ├── TaskAssignmentController.java
│   │   │       ├── TaskDependencyController.java
│   │   │       ├── FileController.java
│   │   │       └── CapacityController.java              (Wave 1)
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/org/pfe/ppm_project/
│           └── PpmProjectApplicationTests.java
└── target/                                          (Build output)
```

---

## Metriques du projet

| Metrique | Valeur |
|----------|--------|
| Fichiers Java source | 55 |
| Entites JPA | 6 |
| DTOs | 11 |
| Repositories | 6 |
| Services (interfaces + implementations) | 15 |
| Controllers | 8 |
| Endpoints API | ~44 |
| Enumerations | 3 |
| Mappers | 3 |
| Classes de configuration | 2 |

### Historique des versions

| Version | Date | Contenu |
|---------|------|---------|
| Wave 0 | Initial | Core entities, CRUD, JWT auth, Gantt, WBS, task dependencies |
| Wave 1 | 2026-04-13 | My Tasks (`/assignments/me`), Capacity Planning (`/capacity`), CapacityService |
| Wave 2 | 2026-04-13 | Audit Trail, Notifications Center, Portfolio Executive Dashboard |

---

## Wave 2 — Nouvelles fonctionnalites

### Feature 1 — Audit Trail (`/api/audit`)

Journal d'audit immuable des actions metier significatives.

**Entites:** `AuditLog` (table `audit_logs`)
**Enums:** `AuditAction` (CREATE, UPDATE, DELETE, STATUS_CHANGE, ASSIGN, UNASSIGN, DEPENDENCY_ADD, DEPENDENCY_REMOVE)

**Endpoints:**

| Endpoint | Roles | Description |
|----------|-------|-------------|
| `GET /api/audit` | ADMIN, PMO | Recherche paginee avec filtres (entityType, actorId, action, from, to) |
| `GET /api/audit/entity/{type}/{id}` | ADMIN, PMO, PM | Historique d'une entite |
| `GET /api/audit/project/{id}` | ADMIN, PMO, PM | Audit trail d'un projet |
| `GET /api/audit/actor/{id}` | ADMIN | Actions d'un acteur |

**Integrations:** loggees automatiquement dans ProjectService, TaskService, TaskAssignmentService, UserService, PortefeuilleService, TaskDependencyService.

---

### Feature 2 — Notifications Center (`/api/notifications`)

Centre de notifications personnel, par utilisateur.

**Entites:** `Notification` (table `notifications`)
**Enums:** `NotificationType` (TASK_ASSIGNED, DEADLINE_APPROACHING, TASK_OVERDUE, OVERLOAD_WARNING, PROJECT_UPDATE, DEPENDENCY_BLOCKED)

**Endpoints:**

| Endpoint | Description |
|----------|-------------|
| `GET /api/notifications/me` | Toutes mes notifications (newest-first) |
| `GET /api/notifications/me/unread` | Mes notifications non lues |
| `GET /api/notifications/me/unread-count` | Compteur pour le badge (`{"count": N}`) |
| `PUT /api/notifications/{id}/read` | Marquer une notification comme lue |
| `PUT /api/notifications/me/read-all` | Tout marquer comme lu (bulk) |
| `DELETE /api/notifications/{id}` | Supprimer une notification |

**Integrations:** notification TASK_ASSIGNED creee automatiquement lors de chaque affectation dans TaskAssignmentService.
**Anti-spam:** une notification identique (meme type + entite) non lue n'est pas dupliquee.

---

### Feature 3 — Portfolio Executive Dashboard (`/api/dashboard/portfolio`)

Tableau de bord executif avec classification de sante des projets.

**Endpoint:** `GET /api/dashboard/portfolio` (ADMIN, PMO, PM)

**Classification de sante (logique Microsoft PPM) :**
- `COMPLETED` — progress >= 100
- `DELAYED` — endDate < today AND progress < 100
- `AT_RISK` — endDate <= today+7j AND progress < 80
- `ON_TRACK` — tous les autres

**DTO:** `PortfolioDashboardDTO` avec `ProjectHealthSummary` et `PortfolioSummary` imbriques.

**KPIs retournes :** totalPortfolios, totalActiveProjects, delayedProjectsCount, completedProjectsCount, onTrackProjectsCount, atRiskProjectsCount, averageProgress, totalActiveTasks, overdueTasks, tasksByStatus, totalActiveUsers, usersByRole, projectHealthOverview (trie par sante), portfolioSummaries.

---

## Statistiques Wave 2

| Metrique | Valeur |
|----------|--------|
| Nouveaux fichiers Java | 14 |
| Nouvelles entites JPA | 2 (AuditLog, Notification) |
| Nouveaux DTOs | 3 (AuditLogDTO, NotificationDTO, PortfolioDashboardDTO) |
| Nouveaux repositories | 2 (AuditLogRepository, NotificationRepository) |
| Nouveaux services | 4 (IAuditLogService + impl, INotificationService + impl) |
| Nouveaux controllers | 2 (AuditLogController, NotificationController) |
| Services modifies | 6 (Project, Task, TaskAssignment, User, Portefeuille, TaskDependency) |
| Nouveaux tests | 14 (AuditLogServiceTest x6, NotificationServiceTest x8) |
| Total tests | 61 (tous verts) |
