package org.pfe.ppm_project.services;

import org.pfe.ppm_project.dto.CalendarExceptionDTO;
import org.pfe.ppm_project.dto.WorkCalendarCreateDTO;
import org.pfe.ppm_project.dto.WorkCalendarDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ICalendarService {

    // CRUD
    WorkCalendarDTO create(WorkCalendarCreateDTO dto);
    List<WorkCalendarDTO> getAll();
    Optional<WorkCalendarDTO> getById(Long id);
    Optional<WorkCalendarDTO> update(Long id, WorkCalendarCreateDTO dto);
    boolean delete(Long id);

    // Exceptions (holidays)
    CalendarExceptionDTO addException(Long calendarId, CalendarExceptionDTO dto);
    List<CalendarExceptionDTO> getExceptions(Long calendarId);
    boolean removeException(Long exceptionId);

    // Business logic
    boolean isWorkingDay(Long calendarId, LocalDate date);
    LocalDate addWorkingDays(Long calendarId, LocalDate startDate, int workingDays);
    int countWorkingDays(Long calendarId, LocalDate from, LocalDate to);
    List<LocalDate> getNonWorkingDays(Long calendarId, LocalDate from, LocalDate to);
}
