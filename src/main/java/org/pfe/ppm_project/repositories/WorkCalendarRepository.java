package org.pfe.ppm_project.repositories;

import org.pfe.ppm_project.entities.WorkCalendar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkCalendarRepository extends JpaRepository<WorkCalendar, Long> {
    Optional<WorkCalendar> findByName(String name);
}
