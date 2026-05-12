package org.pfe.ppm_project.config;

import lombok.RequiredArgsConstructor;
import org.pfe.ppm_project.entities.CalendarException;
import org.pfe.ppm_project.entities.WorkCalendar;
import org.pfe.ppm_project.repositories.WorkCalendarRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Initialisation des calendriers de travail tunisiens au premier demarrage.
 * Implemente CommandLineRunner pour s'executer automatiquement apres le demarrage de Spring.
 * Cree 3 calendriers par defaut (Standard 5j, 6 jours, 24/7) avec les jours feries tunisiens.
 * Ne s'execute qu'une seule fois — verifie si des calendriers existent deja en base.
 */
@Component // Enregistre comme bean Spring — detecte automatiquement au scan
@RequiredArgsConstructor
public class CalendarDataSeeder implements CommandLineRunner {

    private final WorkCalendarRepository calendarRepo;

    @Override
    public void run(String... args) {
        // Evite de recreer les calendriers si ils existent deja (idempotence)
        if (calendarRepo.count() > 0) return;

        // ── 1. Standard Tunisie (Lun-Ven, 8h/j) — calendrier le plus courant pour le secteur public ──
        WorkCalendar standard = WorkCalendar.builder()
                .name("Standard Tunisie")
                .description("Calendrier standard tunisien : Lundi-Vendredi, 8h/jour, avec jours feries nationaux")
                .workHoursPerDay(8.0)
                .workDays("1,2,3,4,5") // 1=Lundi ... 5=Vendredi (format ISO)
                .build();

        addTunisianHolidays2026(standard);
        calendarRepo.save(standard);

        // ── 2. Tunisie 6j (Lun-Sam) — courant dans le secteur prive tunisien ──
        WorkCalendar sixDays = WorkCalendar.builder()
                .name("Tunisie 6 jours")
                .description("Calendrier 6 jours : Lundi-Samedi, 8h/jour, secteur prive")
                .workHoursPerDay(8.0)
                .workDays("1,2,3,4,5,6") // Inclut le samedi
                .build();

        addTunisianHolidays2026(sixDays);
        calendarRepo.save(sixDays);

        // ── 3. Calendrier 24/7 — pour les equipes de maintenance ou production continue ──
        WorkCalendar continuous = WorkCalendar.builder()
                .name("24/7 Continu")
                .description("Calendrier continu : 7 jours/semaine, 8h/jour, uniquement jours feries")
                .workHoursPerDay(8.0)
                .workDays("1,2,3,4,5,6,7") // Tous les jours de la semaine
                .build();

        addTunisianHolidays2026(continuous);
        calendarRepo.save(continuous);
    }

    // Ajoute tous les jours feries tunisiens 2026 (fixes + islamiques) a un calendrier
    private void addTunisianHolidays2026(WorkCalendar cal) {
        // ── Jours feries fixes (dates nationales — ne changent pas d'annee en annee) ──
        addHoliday(cal, 2026, 1, 1,   "Jour de l'An");
        addHoliday(cal, 2026, 1, 14,  "Fete de la Revolution et de la Jeunesse");
        addHoliday(cal, 2026, 3, 20,  "Fete de l'Independance");
        addHoliday(cal, 2026, 4, 9,   "Journee des Martyrs");
        addHoliday(cal, 2026, 5, 1,   "Fete du Travail");
        addHoliday(cal, 2026, 7, 25,  "Fete de la Republique");
        addHoliday(cal, 2026, 8, 13,  "Journee de la Femme");
        addHoliday(cal, 2026, 10, 15, "Fete de l'Evacuation");

        // ── Jours feries islamiques 2026 (dates approximatives — basees sur le calendrier lunaire) ──
        // Mouled (Naissance du Prophete) - ~5 septembre 2026
        addHoliday(cal, 2026, 9, 5,   "Mouled (Naissance du Prophete)");

        // Isra et Miraj - ~18 janvier 2026
        addHoliday(cal, 2026, 1, 18,  "Isra et Miraj");

        // Aid el-Fitr (fin du Ramadan) - ~21-22 mars 2026
        addHoliday(cal, 2026, 3, 21,  "Aid el-Fitr (1er jour)");
        addHoliday(cal, 2026, 3, 22,  "Aid el-Fitr (2eme jour)");

        // Aid el-Adha (fete du sacrifice) - ~28-29 mai 2026
        addHoliday(cal, 2026, 5, 28,  "Aid el-Adha (1er jour)");
        addHoliday(cal, 2026, 5, 29,  "Aid el-Adha (2eme jour)");

        // Ras el-Am el-Hijri (Nouvel an islamique) - ~17 juin 2026
        addHoliday(cal, 2026, 6, 17,  "Ras el-Am el-Hijri (Nouvel An Hegire)");
    }

    // Cree une exception de calendrier (jour ferie = jour non travaille avec 0h)
    private void addHoliday(WorkCalendar cal, int year, int month, int day, String name) {
        CalendarException ex = CalendarException.builder()
                .calendar(cal)
                .date(LocalDate.of(year, month, day))
                .name(name)
                .working(false) // false = jour non travaille
                .workHours(0.0) // 0 heures de travail ce jour-la
                .build();
        cal.addException(ex);
    }
}
