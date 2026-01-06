package com.company.repos;

import com.company.model.Holiday;
import com.company.model.HolidayStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, UUID> {

    @Query("""
            SELECT h FROM Holiday h
            WHERE h.status = :status
              AND h.date BETWEEN :startDate AND :endDate
            ORDER BY h.date ASC
            """)
    List<Holiday> findForRange(@Param("status") HolidayStatus status,
                               @Param("startDate") LocalDate startDate,
                               @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT h FROM Holiday h
            WHERE h.date = :date
              AND h.name = :name
            """)
    Optional<Holiday> findByDateAndName(@Param("date") LocalDate date, @Param("name") String name);

    @Query("""
            SELECT h FROM Holiday h
            WHERE h.status = :status
              AND EXTRACT(YEAR FROM h.date) = :year
            ORDER BY h.date ASC
            """)
    List<Holiday> findByYear(@Param("status") HolidayStatus status, @Param("year") int year);

    @Query("""
            SELECT DISTINCT CAST(EXTRACT(YEAR FROM h.date) AS int)
            FROM Holiday h
            WHERE h.status = :status
            ORDER BY 1 DESC
            """)
    List<Integer> findYearsWithStatus(@Param("status") HolidayStatus status);
}
