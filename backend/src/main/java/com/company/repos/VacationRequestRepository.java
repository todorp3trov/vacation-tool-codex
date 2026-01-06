package com.company.repos;

import com.company.model.VacationRequest;
import com.company.model.VacationRequestStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VacationRequestRepository extends JpaRepository<VacationRequest, UUID> {

    @Query("""
            SELECT vr FROM VacationRequest vr
            JOIN FETCH vr.user u
            WHERE u.id = :userId
              AND vr.startDate <= :endDate
              AND vr.endDate >= :startDate
            """)
    List<VacationRequest> findOverlappingForUser(@Param("userId") UUID userId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT vr FROM VacationRequest vr
            JOIN FETCH vr.user u
            WHERE u.id IN :userIds
              AND vr.status IN :statuses
              AND vr.startDate <= :endDate
              AND vr.endDate >= :startDate
            """)
    List<VacationRequest> findTeamVacations(@Param("userIds") Collection<UUID> userIds,
                                            @Param("statuses") Collection<VacationRequestStatus> statuses,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);
}
