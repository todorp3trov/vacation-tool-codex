package com.company.repos;

import com.company.model.TeamMembership;
import com.company.model.TeamMembershipStatus;
import com.company.model.TeamStatus;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamMembershipRepository extends JpaRepository<TeamMembership, UUID> {

    @Query("""
            SELECT tm.team.id FROM TeamMembership tm
            WHERE tm.user.id = :userId
              AND tm.status = :status
              AND tm.team.status = :teamStatus
            """)
    List<UUID> findActiveTeamIdsForUser(@Param("userId") UUID userId,
                                        @Param("status") TeamMembershipStatus status,
                                        @Param("teamStatus") TeamStatus teamStatus);

    @Query("""
            SELECT tm.user.id FROM TeamMembership tm
            WHERE tm.team.id IN :teamIds
              AND tm.status = :status
              AND tm.team.status = :teamStatus
            """)
    List<UUID> findActiveUserIdsForTeams(@Param("teamIds") Collection<UUID> teamIds,
                                         @Param("status") TeamMembershipStatus status,
                                         @Param("teamStatus") TeamStatus teamStatus);

    List<TeamMembership> findByUserId(UUID userId);

    List<TeamMembership> findByTeamId(UUID teamId);
}
