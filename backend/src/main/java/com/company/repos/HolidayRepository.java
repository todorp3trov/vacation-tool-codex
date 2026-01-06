package com.company.repos;

import com.company.model.Holiday;
import com.company.model.HolidayStatus;
import java.time.LocalDate;
import java.util.List;
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
}
