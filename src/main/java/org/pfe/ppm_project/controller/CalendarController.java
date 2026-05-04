package org.pfe.ppm_project.controller;

import lombok.RequiredArgsConstructor;
import org.pfe.ppm_project.dto.CalendarExceptionDTO;
import org.pfe.ppm_project.dto.WorkCalendarCreateDTO;
import org.pfe.ppm_project.dto.WorkCalendarDTO;
import org.pfe.ppm_project.services.ICalendarService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller CRUD des calendriers de travail et de leurs exceptions (jours feries).
 * Les calendriers definissent les jours ouvres et les heures de travail par jour.
 * Les exceptions permettent d'ajouter des jours feries ou des jours speciaux.
 * Inclut aussi des endpoints de logique metier (verification jour ouvre, liste des jours non travailles).
 * La modification est reservee aux ADMIN et PMO ; la lecture est ouverte.
 */
@RestController
@RequestMapping("/api/calendars")
@RequiredArgsConstructor
public class CalendarController {

    private final ICalendarService calendarService;

    // ─── CRUD des calendriers ───

    // Creation d'un nouveau calendrier de travail
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PMO')") // Seuls ADMIN et PMO peuvent definir des calendriers
    public ResponseEntity<WorkCalendarDTO> create(@RequestBody WorkCalendarCreateDTO dto) {
        return new ResponseEntity<>(calendarService.create(dto), HttpStatus.CREATED);
    }

    // Liste de tous les calendriers disponibles — pour les listes deroulantes
    @GetMapping
    public ResponseEntity<List<WorkCalendarDTO>> getAll() {
        return ResponseEntity.ok(calendarService.getAll());
    }

    // Recuperation d'un calendrier par son ID avec ses exceptions
    @GetMapping("/{id}")
    public ResponseEntity<WorkCalendarDTO> getById(@PathVariable Long id) {
        return calendarService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Mise a jour d'un calendrier existant (nom, jours ouvres, heures/jour)
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PMO')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody WorkCalendarCreateDTO dto) {
        return calendarService.update(id, dto)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Suppression d'un calendrier
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PMO')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return calendarService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    // ─── Gestion des exceptions (jours feries / jours speciaux) ───

    // Ajout d'un jour ferie ou jour special a un calendrier
    @PostMapping("/{calendarId}/exceptions")
    @PreAuthorize("hasAnyRole('ADMIN','PMO')")
    public ResponseEntity<CalendarExceptionDTO> addException(
            @PathVariable Long calendarId,
            @RequestBody CalendarExceptionDTO dto) {
        return new ResponseEntity<>(calendarService.addException(calendarId, dto), HttpStatus.CREATED);
    }

    // Liste de toutes les exceptions d'un calendrier (jours feries configures)
    @GetMapping("/{calendarId}/exceptions")
    public ResponseEntity<List<CalendarExceptionDTO>> getExceptions(@PathVariable Long calendarId) {
        return ResponseEntity.ok(calendarService.getExceptions(calendarId));
    }

    // Suppression d'une exception (jour ferie) d'un calendrier
    @DeleteMapping("/exceptions/{exceptionId}")
    @PreAuthorize("hasAnyRole('ADMIN','PMO')")
    public ResponseEntity<Void> removeException(@PathVariable Long exceptionId) {
        return calendarService.removeException(exceptionId)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    // ─── Endpoints de logique metier ───

    // Verifie si une date donnee est un jour ouvre selon le calendrier specifie
    // Utile pour la planification automatique des taches
    @GetMapping("/{calendarId}/is-working-day")
    public ResponseEntity<Boolean> isWorkingDay(
            @PathVariable Long calendarId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(calendarService.isWorkingDay(calendarId, date));
    }

    // Retourne la liste des jours non travailles dans un intervalle de dates
    // Utile pour le calcul de duree des taches et l'affichage dans le Gantt
    @GetMapping("/{calendarId}/non-working-days")
    public ResponseEntity<List<LocalDate>> getNonWorkingDays(
            @PathVariable Long calendarId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(calendarService.getNonWorkingDays(calendarId, from, to));
    }
}
