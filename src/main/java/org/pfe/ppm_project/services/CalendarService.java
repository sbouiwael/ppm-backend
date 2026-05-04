package org.pfe.ppm_project.services;

import lombok.RequiredArgsConstructor;
import org.pfe.ppm_project.dto.CalendarExceptionDTO;
import org.pfe.ppm_project.dto.WorkCalendarCreateDTO;
import org.pfe.ppm_project.dto.WorkCalendarDTO;
import org.pfe.ppm_project.entities.CalendarException;
import org.pfe.ppm_project.entities.WorkCalendar;
import org.pfe.ppm_project.repositories.CalendarExceptionRepository;
import org.pfe.ppm_project.repositories.WorkCalendarRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

/**
 * Service metier pour la gestion des calendriers de travail.
 * Fournit le CRUD des calendriers et de leurs exceptions (jours feries),
 * ainsi que les calculs de jours ouvres (ajout, comptage, detection des jours chomes).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CalendarService implements ICalendarService {

    private final WorkCalendarRepository calendarRepo;
    private final CalendarExceptionRepository exceptionRepo;

    // ─── CRUD des calendriers ───

    // Cree un nouveau calendrier avec valeurs par defaut si non fournies (8h/jour, Lun-Ven)
    @Override
    public WorkCalendarDTO create(WorkCalendarCreateDTO dto) {
        WorkCalendar cal = WorkCalendar.builder()
                .name(dto.name().trim())
                .description(dto.description())
                .workHoursPerDay(dto.workHoursPerDay() != null ? dto.workHoursPerDay() : 8.0)
                .workDays(dto.workDays() != null ? dto.workDays() : "1,2,3,4,5")
                .build();
        return toDTO(calendarRepo.save(cal));
    }

    // Retourne la liste de tous les calendriers avec leurs exceptions
    @Override
    @Transactional(readOnly = true)
    public List<WorkCalendarDTO> getAll() {
        return calendarRepo.findAll().stream().map(this::toDTO).toList();
    }

    // Retourne un calendrier par son identifiant
    @Override
    @Transactional(readOnly = true)
    public Optional<WorkCalendarDTO> getById(Long id) {
        return calendarRepo.findById(id).map(this::toDTO);
    }

    // Met a jour un calendrier existant (les champs null sont ignores)
    @Override
    public Optional<WorkCalendarDTO> update(Long id, WorkCalendarCreateDTO dto) {
        return calendarRepo.findById(id).map(cal -> {
            if (dto.name() != null) cal.setName(dto.name().trim());
            if (dto.description() != null) cal.setDescription(dto.description());
            if (dto.workHoursPerDay() != null) cal.setWorkHoursPerDay(dto.workHoursPerDay());
            if (dto.workDays() != null) cal.setWorkDays(dto.workDays());
            return toDTO(calendarRepo.save(cal));
        });
    }

    // Supprime un calendrier par son identifiant
    @Override
    public boolean delete(Long id) {
        if (!calendarRepo.existsById(id)) return false;
        calendarRepo.deleteById(id);
        return true;
    }

    // ─── Gestion des exceptions (jours feries / jours ouvres exceptionnels) ───

    // Ajoute une exception a un calendrier. Remplace une exception existante a la meme date.
    @Override
    public CalendarExceptionDTO addException(Long calendarId, CalendarExceptionDTO dto) {
        WorkCalendar cal = calendarRepo.findById(calendarId)
                .orElseThrow(() -> new org.pfe.ppm_project.exception.ResourceNotFoundException("Calendar not found: " + calendarId));

        // Supprime l'exception existante pour cette date (remplacement)
        exceptionRepo.findByCalendarIdAndDate(calendarId, dto.date())
                .ifPresent(exceptionRepo::delete);

        CalendarException ex = CalendarException.builder()
                .calendar(cal)
                .date(dto.date())
                .name(dto.name())
                .working(dto.working())
                .workHours(dto.workHours() != null ? dto.workHours() : 0.0)
                .build();

        return toExDTO(exceptionRepo.save(ex));
    }

    // Retourne toutes les exceptions d'un calendrier
    @Override
    @Transactional(readOnly = true)
    public List<CalendarExceptionDTO> getExceptions(Long calendarId) {
        return exceptionRepo.findByCalendarId(calendarId).stream().map(this::toExDTO).toList();
    }

    // Supprime une exception par son identifiant
    @Override
    public boolean removeException(Long exceptionId) {
        if (!exceptionRepo.existsById(exceptionId)) return false;
        exceptionRepo.deleteById(exceptionId);
        return true;
    }

    // ─── Logique metier : calculs de jours ouvres ───

    // Determine si une date donnee est un jour ouvre selon le calendrier
    @Override
    @Transactional(readOnly = true)
    public boolean isWorkingDay(Long calendarId, LocalDate date) {
        WorkCalendar cal = calendarRepo.findById(calendarId).orElse(null);
        if (cal == null) {
            // Fallback : Lun-Ven sont ouvres par defaut
            DayOfWeek dow = date.getDayOfWeek();
            return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
        }
        return isWorkingDayInternal(cal, date);
    }

    // Ajoute un nombre de jours ouvres a une date de debut.
    // Utilise le calendrier si disponible, sinon le calendrier par defaut (Lun-Ven).
    @Override
    @Transactional(readOnly = true)
    public LocalDate addWorkingDays(Long calendarId, LocalDate startDate, int workingDays) {
        WorkCalendar cal = calendarId != null ? calendarRepo.findById(calendarId).orElse(null) : null;
        if (workingDays <= 0) return startDate;

        LocalDate current = startDate;
        int remaining = workingDays;

        // Avance jour par jour en ne comptant que les jours ouvres
        while (remaining > 0) {
            current = current.plusDays(1);
            if (cal != null ? isWorkingDayInternal(cal, current) : isDefaultWorkingDay(current)) {
                remaining--;
            }
        }
        return current;
    }

    // Compte le nombre de jours ouvres entre deux dates (incluses)
    @Override
    @Transactional(readOnly = true)
    public int countWorkingDays(Long calendarId, LocalDate from, LocalDate to) {
        WorkCalendar cal = calendarId != null ? calendarRepo.findById(calendarId).orElse(null) : null;
        int count = 0;
        LocalDate current = from;

        while (!current.isAfter(to)) {
            if (cal != null ? isWorkingDayInternal(cal, current) : isDefaultWorkingDay(current)) {
                count++;
            }
            current = current.plusDays(1);
        }
        return count;
    }

    // Retourne la liste des jours non ouvres entre deux dates
    @Override
    @Transactional(readOnly = true)
    public List<LocalDate> getNonWorkingDays(Long calendarId, LocalDate from, LocalDate to) {
        WorkCalendar cal = calendarId != null ? calendarRepo.findById(calendarId).orElse(null) : null;
        List<LocalDate> nonWorking = new ArrayList<>();
        LocalDate current = from;

        while (!current.isAfter(to)) {
            boolean working = cal != null ? isWorkingDayInternal(cal, current) : isDefaultWorkingDay(current);
            if (!working) {
                nonWorking.add(current);
            }
            current = current.plusDays(1);
        }
        return nonWorking;
    }

    // ─── Methodes internes ───

    // Verifie si une date est ouvree selon un calendrier donne.
    // Les exceptions ont priorite sur les jours ouvres standard.
    private boolean isWorkingDayInternal(WorkCalendar cal, LocalDate date) {
        // 1. Les exceptions (jours feries, jours ouvres exceptionnels) ont priorite
        Optional<CalendarException> ex = exceptionRepo.findByCalendarIdAndDate(cal.getId(), date);
        if (ex.isPresent()) {
            return ex.get().isWorking();
        }

        // 2. Sinon, on verifie si le jour de la semaine fait partie des jours ouvres
        List<DayOfWeek> workDaysList = cal.getWorkDaysList();
        return workDaysList.contains(date.getDayOfWeek());
    }

    // Calendrier par defaut : Lundi-Vendredi ouvres
    private boolean isDefaultWorkingDay(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
    }

    // ─── Convertisseurs entite -> DTO ───

    // Convertit un WorkCalendar en DTO avec ses exceptions
    private WorkCalendarDTO toDTO(WorkCalendar cal) {
        List<CalendarExceptionDTO> exDtos = cal.getExceptions() != null
                ? cal.getExceptions().stream().map(this::toExDTO).toList()
                : List.of();

        return new WorkCalendarDTO(
                cal.getId(), cal.getName(), cal.getDescription(),
                cal.getWorkHoursPerDay(), cal.getWorkDays(), exDtos
        );
    }

    // Convertit une CalendarException en DTO
    private CalendarExceptionDTO toExDTO(CalendarException ex) {
        return new CalendarExceptionDTO(
                ex.getId(),
                ex.getCalendar().getId(),
                ex.getDate(),
                ex.getName(),
                ex.isWorking(),
                ex.getWorkHours()
        );
    }
}
