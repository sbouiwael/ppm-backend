package org.pfe.ppm_project.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pfe.ppm_project.entities.*;
import org.pfe.ppm_project.enums.DependencyType;
import org.pfe.ppm_project.enums.Role;
import org.pfe.ppm_project.enums.TaskStatus;
import org.pfe.ppm_project.repositories.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Initialisation de la base de données avec des données réelles BIAT.
 * Exécuté au démarrage uniquement si la base est vide.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PortefeuilleRepository portefeuilleRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final TaskDependencyRepository taskDependencyRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Base de données déjà initialisée — aucune insertion.");
            return;
        }

        log.info("Initialisation des données BIAT...");

        // =========================================================
        // 1. UTILISATEURS
        // =========================================================
        String pwd = passwordEncoder.encode("Biat@2026!");

        save(User.builder()
                .firstName("Mohamed").lastName("Ben Ali")
                .email("m.benali@biat.com.tn").password(pwd)
                .role(Role.ADMIN).weeklyCapacity(40).active(true).build());

        save(User.builder()
                .firstName("Sonia").lastName("Trabelsi")
                .email("s.trabelsi@biat.com.tn").password(pwd)
                .role(Role.PMO).weeklyCapacity(40).active(true).build());

        User pm1 = save(User.builder()
                .firstName("Ahmed").lastName("Khelifi")
                .email("a.khelifi@biat.com.tn").password(pwd)
                .role(Role.PM).weeklyCapacity(40).active(true).build());

        User pm2 = save(User.builder()
                .firstName("Fatma").lastName("Mansouri")
                .email("f.mansouri@biat.com.tn").password(pwd)
                .role(Role.PM).weeklyCapacity(40).active(true).build());

        User pm3 = save(User.builder()
                .firstName("Karim").lastName("Bouzid")
                .email("k.bouzid@biat.com.tn").password(pwd)
                .role(Role.PM).weeklyCapacity(40).active(true).build());

        User dev1 = save(User.builder()
                .firstName("Nizar").lastName("Hammami")
                .email("n.hammami@biat.com.tn").password(pwd)
                .role(Role.DEV).weeklyCapacity(40).active(true).build());

        User dev2 = save(User.builder()
                .firstName("Yassine").lastName("Ben Romdhane")
                .email("y.benromdhane@biat.com.tn").password(pwd)
                .role(Role.DEV).weeklyCapacity(40).active(true).build());

        User dev3 = save(User.builder()
                .firstName("Imen").lastName("Chaabane")
                .email("i.chaabane@biat.com.tn").password(pwd)
                .role(Role.DEV).weeklyCapacity(35).active(true).build());

        User dev4 = save(User.builder()
                .firstName("Rami").lastName("Gharbi")
                .email("r.gharbi@biat.com.tn").password(pwd)
                .role(Role.DEV).weeklyCapacity(40).active(true).build());

        User qa1 = save(User.builder()
                .firstName("Rania").lastName("Jebali")
                .email("r.jebali@biat.com.tn").password(pwd)
                .role(Role.QA).weeklyCapacity(40).active(true).build());

        User qa2 = save(User.builder()
                .firstName("Malek").lastName("Zouari")
                .email("m.zouari@biat.com.tn").password(pwd)
                .role(Role.QA).weeklyCapacity(40).active(true).build());

        User devops1 = save(User.builder()
                .firstName("Bilel").lastName("Makhlouf")
                .email("b.makhlouf@biat.com.tn").password(pwd)
                .role(Role.DEVOPS).weeklyCapacity(40).active(true).build());

        User devops2 = save(User.builder()
                .firstName("Houssem").lastName("Souissi")
                .email("h.souissi@biat.com.tn").password(pwd)
                .role(Role.DEVOPS).weeklyCapacity(40).active(true).build());

        save(User.builder()
                .firstName("Leila").lastName("Gharbi")
                .email("l.gharbi@biat.com.tn").password(pwd)
                .role(Role.RH).weeklyCapacity(35).active(true).build());

        log.info("  → {} utilisateurs créés.", userRepository.count());

        // =========================================================
        // 2. PORTEFEUILLES
        // =========================================================
        Portefeuille pf1 = portefeuilleRepository.save(Portefeuille.builder()
                .nom("Transformation Digitale")
                .description("Ensemble des projets visant la modernisation des services bancaires numériques de la BIAT.")
                .build());

        Portefeuille pf2 = portefeuilleRepository.save(Portefeuille.builder()
                .nom("Infrastructure & Sécurité")
                .description("Projets de mise à niveau de l'infrastructure IT et de renforcement de la cybersécurité.")
                .build());

        Portefeuille pf3 = portefeuilleRepository.save(Portefeuille.builder()
                .nom("Conformité & Réglementation")
                .description("Projets de mise en conformité aux normes bancaires internationales (IFRS, Bâle III, etc.).")
                .build());

        log.info("  → {} portefeuilles créés.", portefeuilleRepository.count());

        // =========================================================
        // 3. PROJETS
        // =========================================================

        // Projet 1 : Migration Core Banking
        Project p1 = projectRepository.save(Project.builder()
                .name("Migration Core Banking T24")
                .description("Migration du système central bancaire vers la plateforme Temenos T24 pour améliorer les performances et l'évolutivité du système d'information.")
                .startDate(LocalDate.of(2026, 1, 6))
                .endDate(LocalDate.of(2026, 9, 30))
                .baselineStartDate(LocalDate.of(2026, 1, 6))
                .baselineEndDate(LocalDate.of(2026, 9, 30))
                .projectManager(pm1)
                .portefeuille(pf1)
                .portfolioName("Transformation Digitale")
                .programName("Modernisation SI Bancaire")
                .subProgramName("Core Banking")
                .objective("Remplacer le système legacy par T24 avant fin 2026")
                .calendarName("Calendrier Standard BIAT")
                .progress(35)
                .active(true).build());

        // Projet 2 : Application Mobile BIAT+
        Project p2 = projectRepository.save(Project.builder()
                .name("Application Mobile BIAT+")
                .description("Développement d'une nouvelle application mobile pour les particuliers offrant consultation de solde, virements, paiements factures et gestion de cartes.")
                .startDate(LocalDate.of(2026, 2, 2))
                .endDate(LocalDate.of(2026, 7, 31))
                .baselineStartDate(LocalDate.of(2026, 2, 2))
                .baselineEndDate(LocalDate.of(2026, 7, 31))
                .projectManager(pm2)
                .portefeuille(pf1)
                .portfolioName("Transformation Digitale")
                .programName("Banque Digitale")
                .subProgramName("Mobile Banking")
                .objective("Lancer l'application sur App Store et Play Store avant fin juillet 2026")
                .calendarName("Calendrier Standard BIAT")
                .progress(58)
                .active(true).build());

        // Projet 3 : Mise à niveau Infrastructure Réseau
        Project p3 = projectRepository.save(Project.builder()
                .name("Mise à Niveau Infrastructure Réseau WAN/LAN")
                .description("Renouvellement du parc réseau des agences BIAT (switching, routage, WiFi) et migration vers SD-WAN pour améliorer la résilience et les performances.")
                .startDate(LocalDate.of(2026, 3, 2))
                .endDate(LocalDate.of(2026, 10, 30))
                .baselineStartDate(LocalDate.of(2026, 3, 2))
                .baselineEndDate(LocalDate.of(2026, 10, 30))
                .projectManager(pm3)
                .portefeuille(pf2)
                .portfolioName("Infrastructure & Sécurité")
                .programName("Réseau & Connectivité")
                .subProgramName("WAN/LAN Upgrade")
                .objective("Déployer SD-WAN dans 120 agences avant fin octobre 2026")
                .calendarName("Calendrier Standard BIAT")
                .progress(18)
                .active(true).build());

        // Projet 4 : Conformité IFRS 9
        Project p4 = projectRepository.save(Project.builder()
                .name("Conformité IFRS 9 — Provisionnement ECL")
                .description("Implémentation du modèle de provisionnement ECL (Expected Credit Loss) conformément à la norme IFRS 9 pour le reporting financier de la BIAT.")
                .startDate(LocalDate.of(2025, 10, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .baselineStartDate(LocalDate.of(2025, 10, 1))
                .baselineEndDate(LocalDate.of(2026, 6, 30))
                .projectManager(pm1)
                .portefeuille(pf3)
                .portfolioName("Conformité & Réglementation")
                .programName("Normes Comptables Internationales")
                .subProgramName("IFRS 9")
                .objective("Produire les premiers états IFRS 9 pour l'arrêté juin 2026")
                .calendarName("Calendrier Standard BIAT")
                .progress(72)
                .active(true).build());

        // Projet 5 : Cybersécurité SOC
        Project p5 = projectRepository.save(Project.builder()
                .name("Déploiement SOC (Security Operations Center)")
                .description("Mise en place d'un centre opérationnel de sécurité pour la surveillance en temps réel des menaces et la gestion des incidents de cybersécurité.")
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .baselineStartDate(LocalDate.of(2026, 4, 1))
                .baselineEndDate(LocalDate.of(2026, 12, 31))
                .projectManager(pm3)
                .portefeuille(pf2)
                .portfolioName("Infrastructure & Sécurité")
                .programName("Cybersécurité")
                .subProgramName("SOC & SIEM")
                .objective("Opérationnaliser le SOC avec SIEM Splunk avant décembre 2026")
                .calendarName("Calendrier Standard BIAT")
                .progress(5)
                .active(true).build());

        log.info("  → {} projets créés.", projectRepository.count());

        // =========================================================
        // 4. TÂCHES — Projet 1 : Migration Core Banking T24
        // =========================================================

        // Phase 1 : Analyse & Cadrage
        Task p1_ph1 = taskRepository.save(Task.builder()
                .name("Phase 1 : Analyse & Cadrage")
                .description("Définition du périmètre, cartographie des flux métier et audit de l'existant.")
                .project(p1).wbsNumber("1").mode("SUMMARY").sortOrder(1)
                .durationDays(30.0).workHours(240.0)
                .startDate(LocalDate.of(2026, 1, 6)).endDate(LocalDate.of(2026, 2, 14))
                .status(TaskStatus.DONE).progress(100).active(true).build());

        Task p1_t1 = taskRepository.save(Task.builder()
                .name("Cartographie des flux métier")
                .description("Inventaire et modélisation de tous les flux bancaires existants (virements, crédits, épargne).")
                .project(p1).parentTask(p1_ph1).wbsNumber("1.1").mode("TASK").sortOrder(2)
                .durationDays(10.0).workHours(80.0)
                .startDate(LocalDate.of(2026, 1, 6)).endDate(LocalDate.of(2026, 1, 19))
                .status(TaskStatus.DONE).progress(100).actualWorkHours(82.0).active(true).build());

        Task p1_t2 = taskRepository.save(Task.builder()
                .name("Audit du système legacy")
                .description("Analyse technique approfondie du système bancaire existant (base Oracle, modules COBOL).")
                .project(p1).parentTask(p1_ph1).wbsNumber("1.2").mode("TASK").sortOrder(3)
                .durationDays(10.0).workHours(80.0)
                .startDate(LocalDate.of(2026, 1, 20)).endDate(LocalDate.of(2026, 2, 2))
                .status(TaskStatus.DONE).progress(100).actualWorkHours(90.0).active(true).build());

        Task p1_t3 = taskRepository.save(Task.builder()
                .name("Validation du plan de migration")
                .description("Présentation et validation du plan de migration avec la DSI et la Direction Générale.")
                .project(p1).parentTask(p1_ph1).wbsNumber("1.3").mode("TASK").sortOrder(4)
                .durationDays(5.0).workHours(40.0)
                .startDate(LocalDate.of(2026, 2, 3)).endDate(LocalDate.of(2026, 2, 9))
                .status(TaskStatus.DONE).progress(100).actualWorkHours(38.0).active(true).build());

        // Phase 2 : Développement & Paramétrage
        Task p1_ph2 = taskRepository.save(Task.builder()
                .name("Phase 2 : Développement & Paramétrage T24")
                .description("Paramétrage de T24, développement des adaptateurs et interfaces avec les systèmes tiers.")
                .project(p1).wbsNumber("2").mode("SUMMARY").sortOrder(5)
                .durationDays(80.0).workHours(640.0)
                .startDate(LocalDate.of(2026, 2, 17)).endDate(LocalDate.of(2026, 6, 13))
                .status(TaskStatus.IN_PROGRESS).progress(45).active(true).build());

        Task p1_t4 = taskRepository.save(Task.builder()
                .name("Paramétrage module Dépôts & Épargne")
                .description("Configuration du module dépôts à vue, comptes épargne et dépôts à terme dans T24.")
                .project(p1).parentTask(p1_ph2).wbsNumber("2.1").mode("TASK").sortOrder(6)
                .durationDays(20.0).workHours(160.0)
                .startDate(LocalDate.of(2026, 2, 17)).endDate(LocalDate.of(2026, 3, 16))
                .status(TaskStatus.DONE).progress(100).actualWorkHours(165.0).active(true).build());

        Task p1_t5 = taskRepository.save(Task.builder()
                .name("Paramétrage module Crédits")
                .description("Configuration des produits de crédit (crédit immobilier, crédit conso, leasing).")
                .project(p1).parentTask(p1_ph2).wbsNumber("2.2").mode("TASK").sortOrder(7)
                .durationDays(25.0).workHours(200.0)
                .startDate(LocalDate.of(2026, 3, 17)).endDate(LocalDate.of(2026, 4, 20))
                .status(TaskStatus.IN_PROGRESS).progress(60).actualWorkHours(130.0).active(true).build());

        Task p1_t6 = taskRepository.save(Task.builder()
                .name("Développement interface SWIFT")
                .description("Développement et intégration du connecteur SWIFT pour les virements internationaux.")
                .project(p1).parentTask(p1_ph2).wbsNumber("2.3").mode("TASK").sortOrder(8)
                .durationDays(20.0).workHours(160.0)
                .startDate(LocalDate.of(2026, 4, 21)).endDate(LocalDate.of(2026, 5, 18))
                .status(TaskStatus.NOT_STARTED).progress(0).active(true).build());

        Task p1_t7 = taskRepository.save(Task.builder()
                .name("Développement interface Simt (CIB)")
                .description("Intégration avec la Société Interbancaire de Monétique de Tunisie pour les opérations par carte.")
                .project(p1).parentTask(p1_ph2).wbsNumber("2.4").mode("TASK").sortOrder(9)
                .durationDays(15.0).workHours(120.0)
                .startDate(LocalDate.of(2026, 5, 19)).endDate(LocalDate.of(2026, 6, 8))
                .status(TaskStatus.NOT_STARTED).progress(0).active(true).build());

        // Phase 3 : Tests & Recette
        Task p1_ph3 = taskRepository.save(Task.builder()
                .name("Phase 3 : Tests & Recette")
                .description("Tests unitaires, d'intégration et recette utilisateur (UAT) avant mise en production.")
                .project(p1).wbsNumber("3").mode("SUMMARY").sortOrder(10)
                .durationDays(40.0).workHours(320.0)
                .startDate(LocalDate.of(2026, 6, 16)).endDate(LocalDate.of(2026, 8, 10))
                .status(TaskStatus.NOT_STARTED).progress(0).active(true).build());

        Task p1_t8 = taskRepository.save(Task.builder()
                .name("Tests unitaires et d'intégration")
                .description("Validation des composants T24 développés par l'équipe QA.")
                .project(p1).parentTask(p1_ph3).wbsNumber("3.1").mode("TASK").sortOrder(11)
                .durationDays(15.0).workHours(120.0)
                .startDate(LocalDate.of(2026, 6, 16)).endDate(LocalDate.of(2026, 7, 6))
                .status(TaskStatus.NOT_STARTED).progress(0).active(true).build());

        Task p1_t9 = taskRepository.save(Task.builder()
                .name("Recette utilisateur (UAT)")
                .description("Tests de validation avec les équipes métier (Direction Engagements, Direction Monétique).")
                .project(p1).parentTask(p1_ph3).wbsNumber("3.2").mode("TASK").sortOrder(12)
                .durationDays(20.0).workHours(160.0)
                .startDate(LocalDate.of(2026, 7, 7)).endDate(LocalDate.of(2026, 8, 3))
                .status(TaskStatus.NOT_STARTED).progress(0).active(true).build());

        Task p1_t10 = taskRepository.save(Task.builder()
                .name("Tests de performance et de charge")
                .description("Validation des SLA de performance avec simulation de charge maximale (JMeter).")
                .project(p1).parentTask(p1_ph3).wbsNumber("3.3").mode("TASK").sortOrder(13)
                .durationDays(5.0).workHours(40.0)
                .startDate(LocalDate.of(2026, 8, 4)).endDate(LocalDate.of(2026, 8, 10))
                .status(TaskStatus.NOT_STARTED).progress(0).active(true).build());

        // Phase 4 : Déploiement
        Task p1_ph4 = taskRepository.save(Task.builder()
                .name("Phase 4 : Déploiement Production")
                .description("Migration des données de production, bascule et suivi post-démarrage.")
                .project(p1).wbsNumber("4").mode("SUMMARY").sortOrder(14)
                .durationDays(30.0).workHours(240.0)
                .startDate(LocalDate.of(2026, 8, 17)).endDate(LocalDate.of(2026, 9, 28))
                .status(TaskStatus.NOT_STARTED).progress(0).active(true).build());

        Task p1_t11 = taskRepository.save(Task.builder()
                .name("Migration des données de production")
                .description("ETL et migration des données historiques depuis l'ancien système vers T24.")
                .project(p1).parentTask(p1_ph4).wbsNumber("4.1").mode("TASK").sortOrder(15)
                .durationDays(10.0).workHours(80.0)
                .startDate(LocalDate.of(2026, 8, 17)).endDate(LocalDate.of(2026, 8, 28))
                .status(TaskStatus.NOT_STARTED).progress(0).active(true).build());

        Task p1_t12 = taskRepository.save(Task.builder()
                .name("Bascule et go-live T24")
                .description("Arrêt de l'ancien système, bascule vers T24 et suivi intensif J+30.")
                .project(p1).parentTask(p1_ph4).wbsNumber("4.2").mode("MILESTONE").sortOrder(16)
                .durationDays(1.0).workHours(8.0)
                .startDate(LocalDate.of(2026, 9, 1)).endDate(LocalDate.of(2026, 9, 1))
                .status(TaskStatus.NOT_STARTED).progress(0).active(true).build());

        Task p1_t13 = taskRepository.save(Task.builder()
                .name("Suivi post-démarrage et stabilisation")
                .description("Monitoring renforcé, correction des incidents et stabilisation du système T24 en production.")
                .project(p1).parentTask(p1_ph4).wbsNumber("4.3").mode("TASK").sortOrder(17)
                .durationDays(20.0).workHours(160.0)
                .startDate(LocalDate.of(2026, 9, 2)).endDate(LocalDate.of(2026, 9, 28))
                .status(TaskStatus.NOT_STARTED).progress(0).active(true).build());

        // =========================================================
        // 5. TÂCHES — Projet 2 : Application Mobile BIAT+
        // =========================================================

        Task p2_ph1 = taskRepository.save(Task.builder()
                .name("Phase 1 : UX/UI Design")
                .description("Conception de l'expérience utilisateur et des maquettes graphiques de l'application.")
                .project(p2).wbsNumber("1").mode("SUMMARY").sortOrder(1)
                .durationDays(20.0).workHours(160.0)
                .startDate(LocalDate.of(2026, 2, 2)).endDate(LocalDate.of(2026, 3, 1))
                .status(TaskStatus.DONE).progress(100).active(true).build());

        Task p2_t1 = taskRepository.save(Task.builder()
                .name("Conception wireframes et user flows")
                .description("Création des wireframes pour les parcours principaux : connexion, virements, consultation.")
                .project(p2).parentTask(p2_ph1).wbsNumber("1.1").mode("TASK").sortOrder(2)
                .durationDays(8.0).workHours(64.0)
                .startDate(LocalDate.of(2026, 2, 2)).endDate(LocalDate.of(2026, 2, 13))
                .status(TaskStatus.DONE).progress(100).actualWorkHours(62.0).active(true).build());

        Task p2_t2 = taskRepository.save(Task.builder()
                .name("Design UI haute fidélité (Figma)")
                .description("Création des maquettes haute fidélité respectant la charte graphique BIAT.")
                .project(p2).parentTask(p2_ph1).wbsNumber("1.2").mode("TASK").sortOrder(3)
                .durationDays(8.0).workHours(64.0)
                .startDate(LocalDate.of(2026, 2, 16)).endDate(LocalDate.of(2026, 2, 27))
                .status(TaskStatus.DONE).progress(100).actualWorkHours(70.0).active(true).build());

        Task p2_t3 = taskRepository.save(Task.builder()
                .name("Tests utilisateurs (UX Research)")
                .description("Sessions de tests utilisateurs avec 15 clients BIAT pour valider l'ergonomie.")
                .project(p2).parentTask(p2_ph1).wbsNumber("1.3").mode("TASK").sortOrder(4)
                .durationDays(3.0).workHours(24.0)
                .startDate(LocalDate.of(2026, 2, 27)).endDate(LocalDate.of(2026, 3, 3))
                .status(TaskStatus.DONE).progress(100).actualWorkHours(24.0).active(true).build());

        Task p2_ph2 = taskRepository.save(Task.builder()
                .name("Phase 2 : Développement Backend")
                .description("Développement de l'API REST Spring Boot exposant les services bancaires à l'application mobile.")
                .project(p2).wbsNumber("2").mode("SUMMARY").sortOrder(5)
                .durationDays(50.0).workHours(400.0)
                .startDate(LocalDate.of(2026, 3, 2)).endDate(LocalDate.of(2026, 5, 11))
                .status(TaskStatus.IN_PROGRESS).progress(70).active(true).build());

        Task p2_t4 = taskRepository.save(Task.builder()
                .name("Module Authentification (OAuth2 + OTP)")
                .description("Implémentation de l'authentification biométrique, OTP SMS et OAuth2 avec le serveur d'identité BIAT.")
                .project(p2).parentTask(p2_ph2).wbsNumber("2.1").mode("TASK").sortOrder(6)
                .durationDays(10.0).workHours(80.0)
                .startDate(LocalDate.of(2026, 3, 2)).endDate(LocalDate.of(2026, 3, 16))
                .status(TaskStatus.DONE).progress(100).actualWorkHours(88.0).active(true).build());

        Task p2_t5 = taskRepository.save(Task.builder()
                .name("Module Consultation de solde et mouvements")
                .description("API de consultation des comptes, soldes temps réel et historique des transactions.")
                .project(p2).parentTask(p2_ph2).wbsNumber("2.2").mode("TASK").sortOrder(7)
                .durationDays(8.0).workHours(64.0)
                .startDate(LocalDate.of(2026, 3, 17)).endDate(LocalDate.of(2026, 3, 28))
                .status(TaskStatus.DONE).progress(100).actualWorkHours(60.0).active(true).build());

        Task p2_t6 = taskRepository.save(Task.builder()
                .name("Module Virements nationaux et internationaux")
                .description("API de virement entre comptes propres, vers tiers BIAT et vers d'autres banques (via SIMT/SWIFT).")
                .project(p2).parentTask(p2_ph2).wbsNumber("2.3").mode("TASK").sortOrder(8)
                .durationDays(15.0).workHours(120.0)
                .startDate(LocalDate.of(2026, 3, 31)).endDate(LocalDate.of(2026, 4, 21))
                .status(TaskStatus.IN_PROGRESS).progress(65).actualWorkHours(78.0).active(true).build());

        Task p2_t7 = taskRepository.save(Task.builder()
                .name("Module Paiement de factures (STEG, SONEDE, Topnet)")
                .description("Intégration des APIs de paiement des opérateurs nationaux (STEG, SONEDE, Topnet, Orange).")
                .project(p2).parentTask(p2_ph2).wbsNumber("2.4").mode("TASK").sortOrder(9)
                .durationDays(12.0).workHours(96.0)
                .startDate(LocalDate.of(2026, 4, 22)).endDate(LocalDate.of(2026, 5, 8))
                .status(TaskStatus.NOT_STARTED).progress(0).active(true).build());

        Task p2_ph3 = taskRepository.save(Task.builder()
                .name("Phase 3 : Développement Frontend Mobile")
                .description("Développement de l'application React Native (iOS & Android).")
                .project(p2).wbsNumber("3").mode("SUMMARY").sortOrder(10)
                .durationDays(45.0).workHours(360.0)
                .startDate(LocalDate.of(2026, 3, 2)).endDate(LocalDate.of(2026, 5, 4))
                .status(TaskStatus.IN_PROGRESS).progress(55).active(true).build());

        Task p2_t8 = taskRepository.save(Task.builder()
                .name("Intégration écrans Authentification")
                .description("Développement des écrans de login, biométrie et gestion du profil.")
                .project(p2).parentTask(p2_ph3).wbsNumber("3.1").mode("TASK").sortOrder(11)
                .durationDays(8.0).workHours(64.0)
                .startDate(LocalDate.of(2026, 3, 2)).endDate(LocalDate.of(2026, 3, 13))
                .status(TaskStatus.DONE).progress(100).actualWorkHours(64.0).active(true).build());

        Task p2_t9 = taskRepository.save(Task.builder()
                .name("Intégration écrans Consultation comptes")
                .description("Développement des écrans de tableau de bord, détail compte et historique.")
                .project(p2).parentTask(p2_ph3).wbsNumber("3.2").mode("TASK").sortOrder(12)
                .durationDays(8.0).workHours(64.0)
                .startDate(LocalDate.of(2026, 3, 16)).endDate(LocalDate.of(2026, 3, 27))
                .status(TaskStatus.DONE).progress(100).actualWorkHours(60.0).active(true).build());

        Task p2_t10 = taskRepository.save(Task.builder()
                .name("Intégration écrans Virements")
                .description("Développement des écrans de virement avec validation OTP et confirmation.")
                .project(p2).parentTask(p2_ph3).wbsNumber("3.3").mode("TASK").sortOrder(13)
                .durationDays(12.0).workHours(96.0)
                .startDate(LocalDate.of(2026, 3, 30)).endDate(LocalDate.of(2026, 4, 15))
                .status(TaskStatus.IN_PROGRESS).progress(70).actualWorkHours(67.0).active(true).build());

        Task p2_t11 = taskRepository.save(Task.builder()
                .name("Intégration écrans Paiements")
                .description("Développement des écrans de paiement de factures avec sélection des opérateurs.")
                .project(p2).parentTask(p2_ph3).wbsNumber("3.4").mode("TASK").sortOrder(14)
                .durationDays(10.0).workHours(80.0)
                .startDate(LocalDate.of(2026, 4, 16)).endDate(LocalDate.of(2026, 4, 29))
                .status(TaskStatus.NOT_STARTED).progress(0).active(true).build());

        Task p2_ph4 = taskRepository.save(Task.builder()
                .name("Phase 4 : Sécurité & Audit")
                .description("Tests de pénétration, audit de sécurité et conformité PCI-DSS.")
                .project(p2).wbsNumber("4").mode("SUMMARY").sortOrder(15)
                .durationDays(15.0).workHours(120.0)
                .startDate(LocalDate.of(2026, 5, 12)).endDate(LocalDate.of(2026, 6, 1))
                .status(TaskStatus.NOT_STARTED).progress(0).active(true).build());

        Task p2_t12 = taskRepository.save(Task.builder()
                .name("Test de pénétration (Pentest)")
                .description("Tests d'intrusion réalisés par une équipe externe certifiée CREST.")
                .project(p2).parentTask(p2_ph4).wbsNumber("4.1").mode("TASK").sortOrder(16)
                .durationDays(10.0).workHours(80.0)
                .startDate(LocalDate.of(2026, 5, 12)).endDate(LocalDate.of(2026, 5, 25))
                .status(TaskStatus.NOT_STARTED).progress(0).active(true).build());

        Task p2_t13 = taskRepository.save(Task.builder()
                .name("Correction des vulnérabilités identifiées")
                .description("Correction des failles détectées lors du pentest et re-test de validation.")
                .project(p2).parentTask(p2_ph4).wbsNumber("4.2").mode("TASK").sortOrder(17)
                .durationDays(5.0).workHours(40.0)
                .startDate(LocalDate.of(2026, 5, 26)).endDate(LocalDate.of(2026, 6, 1))
                .status(TaskStatus.NOT_STARTED).progress(0).active(true).build());

        Task p2_ph5 = taskRepository.save(Task.builder()
                .name("Phase 5 : Publication & Lancement")
                .description("Soumission sur App Store et Google Play, campagne de lancement marketing.")
                .project(p2).wbsNumber("5").mode("SUMMARY").sortOrder(18)
                .durationDays(15.0).workHours(120.0)
                .startDate(LocalDate.of(2026, 6, 15)).endDate(LocalDate.of(2026, 7, 3))
                .status(TaskStatus.NOT_STARTED).progress(0).active(true).build());

        Task p2_t14 = taskRepository.save(Task.builder()
                .name("Soumission Apple App Store & Google Play")
                .description("Préparation des stores listings, screenshots et soumission pour validation Apple/Google.")
                .project(p2).parentTask(p2_ph5).wbsNumber("5.1").mode("TASK").sortOrder(19)
                .durationDays(5.0).workHours(40.0)
                .startDate(LocalDate.of(2026, 6, 15)).endDate(LocalDate.of(2026, 6, 22))
                .status(TaskStatus.NOT_STARTED).progress(0).active(true).build());

        Task p2_t15 = taskRepository.save(Task.builder()
                .name("Campagne de lancement et communication")
                .description("Coordination avec le département marketing pour la campagne de lancement BIAT+.")
                .project(p2).parentTask(p2_ph5).wbsNumber("5.2").mode("MILESTONE").sortOrder(20)
                .durationDays(1.0).workHours(8.0)
                .startDate(LocalDate.of(2026, 7, 1)).endDate(LocalDate.of(2026, 7, 1))
                .status(TaskStatus.NOT_STARTED).progress(0).active(true).build());

        // =========================================================
        // 6. TÂCHES — Projet 3 : Mise à Niveau Réseau WAN/LAN (18%)
        // =========================================================

        Task p3_ph1 = taskRepository.save(Task.builder()
                .name("Phase 1 : Audit et Cadrage Réseau")
                .description("Audit de l'infrastructure réseau existante dans les 120 agences et définition de l'architecture SD-WAN cible.")
                .project(p3).wbsNumber("1").mode("SUMMARY").sortOrder(1)
                .durationDays(25.0).workHours(200.0)
                .startDate(LocalDate.of(2026, 3, 2)).endDate(LocalDate.of(2026, 4, 3))
                .status(TaskStatus.DONE).progress(100).active(true).build());

        Task p3_t1 = taskRepository.save(Task.builder()
                .name("Audit infrastructure réseau agences (120 sites)")
                .description("Inventaire et évaluation de l'équipement réseau existant : switches, routeurs, liaisons WAN MPLS.")
                .project(p3).parentTask(p3_ph1).wbsNumber("1.1").mode("TASK").sortOrder(2)
                .durationDays(15.0).workHours(120.0)
                .startDate(LocalDate.of(2026, 3, 2)).endDate(LocalDate.of(2026, 3, 20))
                .status(TaskStatus.DONE).progress(100).actualWorkHours(125.0).active(true).build());

        Task p3_t2 = taskRepository.save(Task.builder()
                .name("Conception architecture SD-WAN cible")
                .description("Design de l'architecture SD-WAN (Cisco Viptela) avec redondance 4G/MPLS pour les agences critiques.")
                .project(p3).parentTask(p3_ph1).wbsNumber("1.2").mode("TASK").sortOrder(3)
                .durationDays(10.0).workHours(80.0)
                .startDate(LocalDate.of(2026, 3, 23)).endDate(LocalDate.of(2026, 4, 3))
                .status(TaskStatus.DONE).progress(100).actualWorkHours(85.0).active(true).build());

        Task p3_ph2 = taskRepository.save(Task.builder()
                .name("Phase 2 : Appel d'Offres et Contractualisation")
                .description("Lancement de l'appel d'offres, évaluation des soumissionnaires et signature des contrats.")
                .project(p3).wbsNumber("2").mode("SUMMARY").sortOrder(4)
                .durationDays(30.0).workHours(240.0)
                .startDate(LocalDate.of(2026, 4, 6)).endDate(LocalDate.of(2026, 5, 16))
                .status(TaskStatus.IN_PROGRESS).progress(50).active(true).build());

        Task p3_t3 = taskRepository.save(Task.builder()
                .name("Rédaction et publication du cahier des charges")
                .description("Préparation du dossier d'appel d'offres réseau : spécifications techniques, SLA, critères d'évaluation.")
                .project(p3).parentTask(p3_ph2).wbsNumber("2.1").mode("TASK").sortOrder(5)
                .durationDays(10.0).workHours(80.0)
                .startDate(LocalDate.of(2026, 4, 6)).endDate(LocalDate.of(2026, 4, 17))
                .status(TaskStatus.DONE).progress(100).actualWorkHours(78.0).active(true).build());

        Task p3_t4 = taskRepository.save(Task.builder()
                .name("Évaluation des offres et sélection fournisseur")
                .description("Analyse des propositions de Cisco, Fortinet et Huawei — grille multicritères technique/financière.")
                .project(p3).parentTask(p3_ph2).wbsNumber("2.2").mode("TASK").sortOrder(6)
                .durationDays(10.0).workHours(80.0)
                .startDate(LocalDate.of(2026, 4, 20)).endDate(LocalDate.of(2026, 5, 1))
                .status(TaskStatus.IN_PROGRESS).progress(60).actualWorkHours(48.0).active(true).build());

        Task p3_t5 = taskRepository.save(Task.builder()
                .name("Négociation et signature des contrats")
                .description("Finalisation des conditions contractuelles, délais de livraison et garanties SLA.")
                .project(p3).parentTask(p3_ph2).wbsNumber("2.3").mode("TASK").sortOrder(7)
                .durationDays(10.0).workHours(80.0)
                .startDate(LocalDate.of(2026, 5, 4)).endDate(LocalDate.of(2026, 5, 15))
                .status(TaskStatus.NOT_STARTED).progress(0).active(true).build());

        Task p3_ph3 = taskRepository.save(Task.builder()
                .name("Phase 3 : Déploiement par Vagues")
                .description("Déploiement progressif du SD-WAN dans les agences en 4 vagues géographiques.")
                .project(p3).wbsNumber("3").mode("SUMMARY").sortOrder(8)
                .durationDays(100.0).workHours(800.0)
                .startDate(LocalDate.of(2026, 5, 18)).endDate(LocalDate.of(2026, 10, 2))
                .status(TaskStatus.NOT_STARTED).progress(0).active(true).build());

        Task p3_t6 = taskRepository.save(Task.builder()
                .name("Vague 1 : Agences Grand Tunis (30 sites)")
                .description("Déploiement pilote SD-WAN dans les 30 agences de la région Grand Tunis.")
                .project(p3).parentTask(p3_ph3).wbsNumber("3.1").mode("TASK").sortOrder(9)
                .durationDays(25.0).workHours(200.0)
                .startDate(LocalDate.of(2026, 5, 18)).endDate(LocalDate.of(2026, 6, 19))
                .status(TaskStatus.NOT_STARTED).progress(0).active(true).build());

        Task p3_t7 = taskRepository.save(Task.builder()
                .name("Vague 2 : Agences Nord (30 sites)")
                .description("Déploiement SD-WAN dans les agences de Bizerte, Béja, Jendouba et Nabeul.")
                .project(p3).parentTask(p3_ph3).wbsNumber("3.2").mode("TASK").sortOrder(10)
                .durationDays(25.0).workHours(200.0)
                .startDate(LocalDate.of(2026, 6, 22)).endDate(LocalDate.of(2026, 7, 24))
                .status(TaskStatus.NOT_STARTED).progress(0).active(true).build());

        Task p3_t8 = taskRepository.save(Task.builder()
                .name("Vague 3 : Agences Centre (30 sites)")
                .description("Déploiement SD-WAN dans les agences de Sousse, Monastir, Mahdia et Kairouan.")
                .project(p3).parentTask(p3_ph3).wbsNumber("3.3").mode("TASK").sortOrder(11)
                .durationDays(25.0).workHours(200.0)
                .startDate(LocalDate.of(2026, 7, 27)).endDate(LocalDate.of(2026, 8, 28))
                .status(TaskStatus.NOT_STARTED).progress(0).active(true).build());

        Task p3_t9 = taskRepository.save(Task.builder()
                .name("Vague 4 : Agences Sud (30 sites)")
                .description("Déploiement SD-WAN dans les agences de Sfax, Gabès, Gafsa, Tozeur et Médenine.")
                .project(p3).parentTask(p3_ph3).wbsNumber("3.4").mode("TASK").sortOrder(12)
                .durationDays(25.0).workHours(200.0)
                .startDate(LocalDate.of(2026, 8, 31)).endDate(LocalDate.of(2026, 10, 1))
                .status(TaskStatus.NOT_STARTED).progress(0).active(true).build());

        Task p3_t10 = taskRepository.save(Task.builder()
                .name("Recette finale et mise en production")
                .description("Validation globale de l'infrastructure SD-WAN sur les 120 agences et clôture du projet.")
                .project(p3).wbsNumber("4").mode("MILESTONE").sortOrder(13)
                .durationDays(1.0).workHours(8.0)
                .startDate(LocalDate.of(2026, 10, 30)).endDate(LocalDate.of(2026, 10, 30))
                .status(TaskStatus.NOT_STARTED).progress(0).active(true).build());

        // =========================================================
        // 7. TÂCHES — Projet 4 : IFRS 9 (déjà avancé à 72%)
        // =========================================================

        Task p4_ph1 = taskRepository.save(Task.builder()
                .name("Phase 1 : Modélisation ECL")
                .description("Développement du modèle de provisionnement Expected Credit Loss selon IFRS 9.")
                .project(p4).wbsNumber("1").mode("SUMMARY").sortOrder(1)
                .durationDays(60.0).workHours(480.0)
                .startDate(LocalDate.of(2025, 10, 1)).endDate(LocalDate.of(2025, 12, 26))
                .status(TaskStatus.DONE).progress(100).active(true).build());

        Task p4_t1 = taskRepository.save(Task.builder()
                .name("Segmentation du portefeuille de crédit")
                .description("Classification des expositions en stages 1, 2 et 3 selon les critères IFRS 9.")
                .project(p4).parentTask(p4_ph1).wbsNumber("1.1").mode("TASK").sortOrder(2)
                .durationDays(20.0).workHours(160.0)
                .startDate(LocalDate.of(2025, 10, 1)).endDate(LocalDate.of(2025, 10, 28))
                .status(TaskStatus.DONE).progress(100).actualWorkHours(168.0).active(true).build());

        Task p4_t2 = taskRepository.save(Task.builder()
                .name("Développement modèle PD/LGD/EAD")
                .description("Modélisation statistique des paramètres de risque (probabilité de défaut, perte en cas de défaut).")
                .project(p4).parentTask(p4_ph1).wbsNumber("1.2").mode("TASK").sortOrder(3)
                .durationDays(30.0).workHours(240.0)
                .startDate(LocalDate.of(2025, 10, 29)).endDate(LocalDate.of(2025, 12, 9))
                .status(TaskStatus.DONE).progress(100).actualWorkHours(250.0).active(true).build());

        Task p4_t3 = taskRepository.save(Task.builder()
                .name("Validation modèle par Risk Management")
                .description("Validation indépendante du modèle ECL par la Direction des Risques.")
                .project(p4).parentTask(p4_ph1).wbsNumber("1.3").mode("TASK").sortOrder(4)
                .durationDays(10.0).workHours(80.0)
                .startDate(LocalDate.of(2025, 12, 10)).endDate(LocalDate.of(2025, 12, 24))
                .status(TaskStatus.DONE).progress(100).actualWorkHours(75.0).active(true).build());

        Task p4_ph2 = taskRepository.save(Task.builder()
                .name("Phase 2 : Développement SI")
                .description("Intégration du modèle ECL dans les systèmes d'information de la BIAT.")
                .project(p4).wbsNumber("2").mode("SUMMARY").sortOrder(5)
                .durationDays(55.0).workHours(440.0)
                .startDate(LocalDate.of(2026, 1, 5)).endDate(LocalDate.of(2026, 3, 27))
                .status(TaskStatus.DONE).progress(100).active(true).build());

        Task p4_t4 = taskRepository.save(Task.builder()
                .name("Développement moteur de calcul ECL")
                .description("Implémentation du moteur de calcul des provisions ECL dans l'entrepôt de données.")
                .project(p4).parentTask(p4_ph2).wbsNumber("2.1").mode("TASK").sortOrder(6)
                .durationDays(30.0).workHours(240.0)
                .startDate(LocalDate.of(2026, 1, 5)).endDate(LocalDate.of(2026, 2, 16))
                .status(TaskStatus.DONE).progress(100).actualWorkHours(248.0).active(true).build());

        Task p4_t5 = taskRepository.save(Task.builder()
                .name("Intégration reporting Finacle")
                .description("Interfaçage du moteur ECL avec Finacle pour alimentation automatique des états comptables.")
                .project(p4).parentTask(p4_ph2).wbsNumber("2.2").mode("TASK").sortOrder(7)
                .durationDays(25.0).workHours(200.0)
                .startDate(LocalDate.of(2026, 2, 17)).endDate(LocalDate.of(2026, 3, 25))
                .status(TaskStatus.DONE).progress(100).actualWorkHours(195.0).active(true).build());

        Task p4_ph3 = taskRepository.save(Task.builder()
                .name("Phase 3 : Tests & Arrêtés Pilotes")
                .description("Tests de la chaîne complète et production des premiers arrêtés IFRS 9 en mode pilote.")
                .project(p4).wbsNumber("3").mode("SUMMARY").sortOrder(8)
                .durationDays(40.0).workHours(320.0)
                .startDate(LocalDate.of(2026, 3, 30)).endDate(LocalDate.of(2026, 5, 25))
                .status(TaskStatus.IN_PROGRESS).progress(50).active(true).build());

        Task p4_t6 = taskRepository.save(Task.builder()
                .name("Tests de non-régression")
                .description("Validation que l'intégration n'impacte pas les traitements comptables existants.")
                .project(p4).parentTask(p4_ph3).wbsNumber("3.1").mode("TASK").sortOrder(9)
                .durationDays(10.0).workHours(80.0)
                .startDate(LocalDate.of(2026, 3, 30)).endDate(LocalDate.of(2026, 4, 13))
                .status(TaskStatus.DONE).progress(100).actualWorkHours(82.0).active(true).build());

        Task p4_t7 = taskRepository.save(Task.builder()
                .name("Arrêté pilote mars 2026")
                .description("Production de l'arrêté IFRS 9 en mode pilote sur les données de mars 2026.")
                .project(p4).parentTask(p4_ph3).wbsNumber("3.2").mode("TASK").sortOrder(10)
                .durationDays(15.0).workHours(120.0)
                .startDate(LocalDate.of(2026, 4, 14)).endDate(LocalDate.of(2026, 5, 4))
                .status(TaskStatus.IN_PROGRESS).progress(40).actualWorkHours(48.0).active(true).build());

        Task p4_t8 = taskRepository.save(Task.builder()
                .name("Validation Direction Comptable & Auditeurs")
                .description("Présentation des résultats aux auditeurs externes (Deloitte) et à la Direction Comptable.")
                .project(p4).parentTask(p4_ph3).wbsNumber("3.3").mode("TASK").sortOrder(11)
                .durationDays(10.0).workHours(80.0)
                .startDate(LocalDate.of(2026, 5, 5)).endDate(LocalDate.of(2026, 5, 19))
                .status(TaskStatus.NOT_STARTED).progress(0).active(true).build());

        Task p4_ph4 = taskRepository.save(Task.builder()
                .name("Phase 4 : Production Officielle")
                .description("Production de l'arrêté officiel IFRS 9 pour l'arrêté semestriel juin 2026.")
                .project(p4).wbsNumber("4").mode("SUMMARY").sortOrder(12)
                .durationDays(20.0).workHours(160.0)
                .startDate(LocalDate.of(2026, 6, 1)).endDate(LocalDate.of(2026, 6, 26))
                .status(TaskStatus.NOT_STARTED).progress(0).active(true).build());

        Task p4_t9 = taskRepository.save(Task.builder()
                .name("Arrêté officiel juin 2026 (IFRS 9)")
                .description("Production de l'arrêté semestriel officiel IFRS 9 à soumettre à la BCT.")
                .project(p4).parentTask(p4_ph4).wbsNumber("4.1").mode("MILESTONE").sortOrder(13)
                .durationDays(1.0).workHours(8.0)
                .startDate(LocalDate.of(2026, 6, 30)).endDate(LocalDate.of(2026, 6, 30))
                .status(TaskStatus.NOT_STARTED).progress(0).active(true).build());

        // =========================================================
        // 8. TÂCHES — Projet 5 : SOC (juste démarré)
        // =========================================================

        Task p5_ph1 = taskRepository.save(Task.builder()
                .name("Phase 1 : Cadrage & Architecture SOC")
                .description("Définition de l'architecture cible du SOC et sélection des solutions technologiques.")
                .project(p5).wbsNumber("1").mode("SUMMARY").sortOrder(1)
                .durationDays(30.0).workHours(240.0)
                .startDate(LocalDate.of(2026, 4, 1)).endDate(LocalDate.of(2026, 5, 12))
                .status(TaskStatus.IN_PROGRESS).progress(20).active(true).build());

        Task p5_t1 = taskRepository.save(Task.builder()
                .name("Cartographie des assets et risques SI")
                .description("Inventaire des actifs informatiques critiques et évaluation des risques de sécurité.")
                .project(p5).parentTask(p5_ph1).wbsNumber("1.1").mode("TASK").sortOrder(2)
                .durationDays(15.0).workHours(120.0)
                .startDate(LocalDate.of(2026, 4, 1)).endDate(LocalDate.of(2026, 4, 21))
                .status(TaskStatus.IN_PROGRESS).progress(30).actualWorkHours(36.0).active(true).build());

        Task p5_t2 = taskRepository.save(Task.builder()
                .name("Sélection et PoC SIEM (Splunk vs IBM QRadar)")
                .description("Évaluation des solutions SIEM en condition réelle sur une maquette lab BIAT.")
                .project(p5).parentTask(p5_ph1).wbsNumber("1.2").mode("TASK").sortOrder(3)
                .durationDays(15.0).workHours(120.0)
                .startDate(LocalDate.of(2026, 4, 22)).endDate(LocalDate.of(2026, 5, 12))
                .status(TaskStatus.NOT_STARTED).progress(0).active(true).build());

        Task p5_ph2 = taskRepository.save(Task.builder()
                .name("Phase 2 : Déploiement Infrastructure SOC")
                .description("Installation et configuration de l'infrastructure SIEM, SOAR et outils de détection.")
                .project(p5).wbsNumber("2").mode("SUMMARY").sortOrder(4)
                .durationDays(60.0).workHours(480.0)
                .startDate(LocalDate.of(2026, 5, 13)).endDate(LocalDate.of(2026, 8, 3))
                .status(TaskStatus.NOT_STARTED).progress(0).active(true).build());

        Task p5_t3 = taskRepository.save(Task.builder()
                .name("Installation et configuration Splunk")
                .description("Déploiement de Splunk Enterprise Security sur les serveurs BIAT.")
                .project(p5).parentTask(p5_ph2).wbsNumber("2.1").mode("TASK").sortOrder(5)
                .durationDays(20.0).workHours(160.0)
                .startDate(LocalDate.of(2026, 5, 13)).endDate(LocalDate.of(2026, 6, 9))
                .status(TaskStatus.NOT_STARTED).progress(0).active(true).build());

        Task p5_t4 = taskRepository.save(Task.builder()
                .name("Intégration des sources de logs (AD, Firewalls, Serveurs)")
                .description("Connexion des sources de logs : Active Directory, pare-feux Palo Alto, serveurs Linux/Windows.")
                .project(p5).parentTask(p5_ph2).wbsNumber("2.2").mode("TASK").sortOrder(6)
                .durationDays(25.0).workHours(200.0)
                .startDate(LocalDate.of(2026, 6, 10)).endDate(LocalDate.of(2026, 7, 14))
                .status(TaskStatus.NOT_STARTED).progress(0).active(true).build());

        Task p5_t5 = taskRepository.save(Task.builder()
                .name("Développement des règles de détection et playbooks")
                .description("Création des règles de corrélation Splunk et des playbooks de réponse aux incidents.")
                .project(p5).parentTask(p5_ph2).wbsNumber("2.3").mode("TASK").sortOrder(7)
                .durationDays(15.0).workHours(120.0)
                .startDate(LocalDate.of(2026, 7, 15)).endDate(LocalDate.of(2026, 8, 3))
                .status(TaskStatus.NOT_STARTED).progress(0).active(true).build());

        log.info("  → {} tâches créées.", taskRepository.count());

        // =========================================================
        // 9. AFFECTATIONS DE TÂCHES
        // =========================================================

        // Projet 1 : Core Banking T24
        assign(p1_t1, pm1, 40);
        assign(p1_t1, dev1, 80);
        assign(p1_t2, dev1, 80);
        assign(p1_t2, dev2, 80);
        assign(p1_t3, pm1, 40);
        assign(p1_t4, dev1, 80);
        assign(p1_t4, dev2, 80);
        assign(p1_t5, dev2, 100);
        assign(p1_t5, dev3, 100);
        assign(p1_t6, dev1, 80);
        assign(p1_t6, dev4, 80);
        assign(p1_t7, dev4, 80);
        assign(p1_t7, dev3, 60);
        assign(p1_t8, qa1, 80);
        assign(p1_t8, qa2, 80);
        assign(p1_t9, qa1, 80);
        assign(p1_t9, qa2, 80);
        assign(p1_t10, qa1, 40);
        assign(p1_t10, devops1, 40);
        assign(p1_t11, dev1, 40);
        assign(p1_t11, devops1, 80);
        assign(p1_t12, pm1, 8);
        assign(p1_t12, devops1, 8);
        assign(p1_t13, devops1, 80);
        assign(p1_t13, devops2, 80);

        // Projet 2 : Application Mobile
        assign(p2_t1, dev3, 40);
        assign(p2_t2, dev3, 64);
        assign(p2_t3, pm2, 24);
        assign(p2_t4, dev1, 80);
        assign(p2_t5, dev2, 64);
        assign(p2_t6, dev2, 60);
        assign(p2_t6, dev4, 60);
        assign(p2_t7, dev4, 48);
        assign(p2_t7, dev3, 48);
        assign(p2_t8, dev3, 64);
        assign(p2_t9, dev3, 64);
        assign(p2_t10, dev3, 48);
        assign(p2_t10, dev4, 48);
        assign(p2_t11, dev4, 80);
        assign(p2_t12, qa1, 80);
        assign(p2_t12, devops2, 40);
        assign(p2_t13, dev1, 40);
        assign(p2_t13, qa1, 40);
        assign(p2_t14, devops1, 40);
        assign(p2_t15, pm2, 8);

        // Projet 3 : Réseau WAN/LAN
        assign(p3_t1, devops1, 60);
        assign(p3_t1, devops2, 60);
        assign(p3_t2, pm3, 40);
        assign(p3_t2, devops1, 40);
        assign(p3_t3, pm3, 40);
        assign(p3_t4, pm3, 40);
        assign(p3_t5, pm3, 40);
        assign(p3_t6, devops1, 100);
        assign(p3_t6, devops2, 100);
        assign(p3_t7, devops1, 100);
        assign(p3_t7, devops2, 100);
        assign(p3_t8, devops1, 100);
        assign(p3_t8, devops2, 100);
        assign(p3_t9, devops1, 100);
        assign(p3_t9, devops2, 100);
        assign(p3_t10, pm3, 8);
        assign(p3_t10, devops1, 8);

        // Projet 4 : IFRS 9
        assign(p4_t1, dev2, 80);
        assign(p4_t2, dev2, 120);
        assign(p4_t2, dev4, 120);
        assign(p4_t3, pm1, 40);
        assign(p4_t4, dev2, 120);
        assign(p4_t4, dev4, 120);
        assign(p4_t5, dev4, 100);
        assign(p4_t5, dev2, 100);
        assign(p4_t6, qa2, 80);
        assign(p4_t7, dev2, 60);
        assign(p4_t7, dev4, 60);
        assign(p4_t8, pm1, 40);
        assign(p4_t9, pm1, 8);

        // Projet 5 : SOC
        assign(p5_t1, devops1, 60);
        assign(p5_t1, devops2, 60);
        assign(p5_t2, devops1, 60);
        assign(p5_t2, devops2, 60);
        assign(p5_t3, devops1, 80);
        assign(p5_t3, devops2, 80);
        assign(p5_t4, devops1, 100);
        assign(p5_t4, devops2, 100);
        assign(p5_t5, devops1, 60);
        assign(p5_t5, devops2, 60);

        log.info("  → {} affectations créées.", taskAssignmentRepository.count());

        // =========================================================
        // 9. DÉPENDANCES ENTRE TÂCHES (quelques exemples FS)
        // =========================================================

        // Core Banking : chaîne séquentielle
        dep(p1_t1, p1_t2);   // Cartographie → Audit
        dep(p1_t2, p1_t3);   // Audit → Validation plan
        dep(p1_t3, p1_t4);   // Plan validé → Paramétrage Dépôts
        dep(p1_t4, p1_t5);   // Dépôts → Crédits
        dep(p1_t5, p1_t6);   // Crédits → Interface SWIFT
        dep(p1_t6, p1_t7);   // SWIFT → SIMT
        dep(p1_t7, p1_t8);   // SIMT → Tests
        dep(p1_t8, p1_t9);   // Tests → UAT
        dep(p1_t9, p1_t10);  // UAT → Tests charge
        dep(p1_t10, p1_t11); // Tests charge → Migration données
        dep(p1_t11, p1_t12); // Migration → Go-live
        dep(p1_t12, p1_t13); // Go-live → Stabilisation

        // Mobile BIAT+ : dépendances clés
        dep(p2_t1, p2_t2);   // Wireframes → Design UI
        dep(p2_t2, p2_t3);   // Design → Tests UX
        dep(p2_t4, p2_t5);   // Auth → Consultation
        dep(p2_t5, p2_t6);   // Consultation → Virements
        dep(p2_t6, p2_t7);   // Virements → Paiements
        dep(p2_t8, p2_t9);   // Écrans Auth → Écrans Comptes
        dep(p2_t9, p2_t10);  // Écrans Comptes → Écrans Virements
        dep(p2_t10, p2_t11); // Écrans Virements → Écrans Paiements
        dep(p2_t12, p2_t13); // Pentest → Corrections
        dep(p2_t13, p2_t14); // Corrections → Soumission stores

        // Réseau WAN/LAN
        dep(p3_t1, p3_t2);   // Audit → Conception architecture
        dep(p3_t2, p3_t3);   // Architecture → Cahier des charges
        dep(p3_t3, p3_t4);   // CDC → Évaluation offres
        dep(p3_t4, p3_t5);   // Évaluation → Contractualisation
        dep(p3_t5, p3_t6);   // Contrats → Vague 1 Grand Tunis
        dep(p3_t6, p3_t7);   // Vague 1 → Vague 2 Nord
        dep(p3_t7, p3_t8);   // Vague 2 → Vague 3 Centre
        dep(p3_t8, p3_t9);   // Vague 3 → Vague 4 Sud
        dep(p3_t9, p3_t10);  // Vague 4 → Recette finale

        // IFRS 9
        dep(p4_t1, p4_t2);
        dep(p4_t2, p4_t3);
        dep(p4_t3, p4_t4);
        dep(p4_t4, p4_t5);
        dep(p4_t5, p4_t6);
        dep(p4_t6, p4_t7);
        dep(p4_t7, p4_t8);
        dep(p4_t8, p4_t9);

        // SOC
        dep(p5_t1, p5_t2);
        dep(p5_t2, p5_t3);
        dep(p5_t3, p5_t4);
        dep(p5_t4, p5_t5);

        log.info("  → {} dépendances créées.", taskDependencyRepository.count());
        log.info("Initialisation terminée avec succès !");
    }

    private User save(User u) {
        return userRepository.save(u);
    }

    private void assign(Task task, User user, int hours) {
        taskAssignmentRepository.save(TaskAssignment.builder()
                .task(task).user(user).assignedHours(hours).active(true).build());
    }

    private void dep(Task predecessor, Task successor) {
        taskDependencyRepository.save(TaskDependency.builder()
                .predecessor(predecessor).successor(successor)
                .type(DependencyType.FS).build());
    }
}