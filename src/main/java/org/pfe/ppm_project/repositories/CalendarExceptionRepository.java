package org.pfe.ppm_project.repositories;

import org.pfe.ppm_project.entities.CalendarException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CalendarExceptionRepository extends JpaRepository<CalendarException, Long> {

    List<CalendarException> findByCalendarId(Long calendarId);

    List<CalendarException> findByCalendarIdAndDateBetween(Long calendarId, LocalDate start, LocalDate end);

    Optional<CalendarException> findByCalendarIdAndDate(Long calendarId, LocalDate date);
}
