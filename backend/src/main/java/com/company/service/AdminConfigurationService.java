package com.company.service;

import com.company.dto.AdminDtos.AdminTeamRequest;
import com.company.dto.AdminDtos.AdminTeamView;
import com.company.dto.AdminDtos.AdminUserRequest;
import com.company.dto.AdminDtos.AdminUserView;
import com.company.integration.EventPublisher;
import com.company.model.Role;
import com.company.model.Team;
import com.company.model.TeamMembership;
import com.company.model.TeamMembershipStatus;
import com.company.model.TeamStatus;
import com.company.model.User;
import com.company.model.UserStatus;
import com.company.repos.RoleRepository;
import com.company.repos.TeamMembershipRepository;
import com.company.repos.TeamRepository;
import com.company.repos.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminConfigurationService {
    private static final Logger log = LoggerFactory.getLogger(AdminConfigurationService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TeamRepository teamRepository;
    private final TeamMembershipRepository teamMembershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;
    private final EventPublisher eventPublisher;
    private final AuditService auditService;
    private final RbacCacheInvalidator rbacCacheInvalidator;

    public AdminConfigurationService(UserRepository userRepository,
                                     RoleRepository roleRepository,
                                     TeamRepository teamRepository,
                                     TeamMembershipRepository teamMembershipRepository,
                                     PasswordEncoder passwordEncoder,
                                     RoleService roleService,
                                     EventPublisher eventPublisher,
                                     AuditService auditService,
                                     RbacCacheInvalidator rbacCacheInvalidator) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.teamRepository = teamRepository;
        this.teamMembershipRepository = teamMembershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleService = roleService;
        this.eventPublisher = eventPublisher;
        this.auditService = auditService;
        this.rbacCacheInvalidator = rbacCacheInvalidator;
    }

    @Transactional(readOnly = true)
    public List<AdminUserView> listUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(this::toUserView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminTeamView> listTeams() {
        List<Team> teams = teamRepository.findAll();
        return teams.stream()
                .map(this::toTeamView)
                .toList();
    }

    @Transactional
    public AdminUserView createUser(UUID actorId, AdminUserRequest request) {
        validateUserRequest(request, true);
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new IllegalArgumentException("username_exists");
        }
        User user = new User();
        user.setUsername(request.username().trim());
        user.setDisplayName(request.displayName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(parseUserStatus(request.status(), UserStatus.ACTIVE));
        user.setRoles(resolveRoles(request.roles()));

        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("username_conflict");
        }

        Set<UUID> teamIds = request.teamIds() != null ? new HashSet<>(request.teamIds()) : Set.of();
        syncMemberships(actorId, user, teamIds);

        eventPublisher.publishPostCommit("UserInvited", Map.of(
                "userId", user.getId().toString(),
                "username", user.getUsername()
        ));
        auditService.recordUserInvite(actorId, user.getId(), user.getUsername());
        rbacCacheInvalidator.invalidateForUser(user.getId());
        if (!teamIds.isEmpty()) {
            rbacCacheInvalidator.invalidateForTeams(teamIds);
        }
        return toUserView(user);
    }

    @Transactional
    public AdminUserView updateUser(UUID actorId, UUID userId, AdminUserRequest request) {
        validateUserRequest(request, false);
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return null;
        }
        User user = userOptional.get();

        if (StringUtils.hasText(request.username()) && !user.getUsername().equalsIgnoreCase(request.username().trim())) {
            if (userRepository.existsByUsernameIgnoreCase(request.username().trim())) {
                throw new IllegalArgumentException("username_exists");
            }
            user.setUsername(request.username().trim());
        }
        if (StringUtils.hasText(request.displayName())) {
            user.setDisplayName(request.displayName().trim());
        }
        if (StringUtils.hasText(request.password())) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        UserStatus previousStatus = user.getStatus();
        if (StringUtils.hasText(request.status())) {
            user.setStatus(parseUserStatus(request.status(), previousStatus));
        }
        if (request.roles() != null && !request.roles().isEmpty()) {
            user.setRoles(resolveRoles(request.roles()));
        }

        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("username_conflict");
        }

        Set<UUID> teamIds = request.teamIds() != null ? new HashSet<>(request.teamIds()) : Set.of();
        syncMemberships(actorId, user, teamIds);

        if (previousStatus == UserStatus.DISABLED && user.getStatus() == UserStatus.ACTIVE) {
            eventPublisher.publishPostCommit("UserActivated", Map.of(
                    "userId", user.getId().toString(),
                    "username", user.getUsername()
            ));
            auditService.recordUserActivation(actorId, user.getId(), user.getUsername());
        } else if (previousStatus == UserStatus.ACTIVE && user.getStatus() == UserStatus.DISABLED) {
            eventPublisher.publishPostCommit("UserDisabled", Map.of(
                    "userId", user.getId().toString(),
                    "username", user.getUsername()
            ));
            auditService.recordUserDisabled(actorId, user.getId(), user.getUsername());
        } else {
            auditService.recordUserUpdate(actorId, user.getId(), user.getUsername());
        }
        rbacCacheInvalidator.invalidateForUser(user.getId());
        if (!teamIds.isEmpty()) {
            rbacCacheInvalidator.invalidateForTeams(teamIds);
        }
        return toUserView(user);
    }

    @Transactional
    public AdminTeamView createTeam(UUID actorId, AdminTeamRequest request) {
        if (request == null || !StringUtils.hasText(request.name())) {
            throw new IllegalArgumentException("name_required");
        }
        Team team = new Team();
        team.setName(request.name().trim());
        team.setStatus(parseTeamStatus(request.status(), TeamStatus.ACTIVE));

        team = teamRepository.save(team);
        if (team.getStatus() == TeamStatus.ACTIVE && request.memberIds() != null && !request.memberIds().isEmpty()) {
            addMembers(actorId, team, new HashSet<>(request.memberIds()));
        }
        eventPublisher.publishPostCommit("TeamCreated", Map.of(
                "teamId", team.getId().toString(),
                "name", team.getName()
        ));
        auditService.recordTeamCreated(actorId, team.getId(), team.getName());
        return toTeamView(team);
    }

    @Transactional
    public AdminTeamView updateTeam(UUID actorId, UUID teamId, AdminTeamRequest request) {
        Optional<Team> teamOptional = teamRepository.findById(teamId);
        if (teamOptional.isEmpty()) {
            return null;
        }
        Team team = teamOptional.get();
        if (StringUtils.hasText(request.name())) {
            team.setName(request.name().trim());
        }
        TeamStatus previousStatus = team.getStatus();
        if (StringUtils.hasText(request.status())) {
            team.setStatus(parseTeamStatus(request.status(), previousStatus));
        }
        team = teamRepository.save(team);

        Set<UUID> memberIds = request.memberIds() != null ? new HashSet<>(request.memberIds()) : null;
        Set<UUID> touchedUsers = new HashSet<>();
        if (memberIds != null) {
            touchedUsers = syncTeamMemberships(actorId, team, memberIds);
        }

        if (previousStatus == TeamStatus.ACTIVE && team.getStatus() == TeamStatus.ARCHIVED) {
            eventPublisher.publishPostCommit("TeamArchived", Map.of(
                    "teamId", team.getId().toString(),
                    "name", team.getName()
            ));
            auditService.recordTeamArchived(actorId, team.getId(), team.getName());
        } else {
            auditService.recordTeamUpdated(actorId, team.getId(), team.getName());
        }
        rbacCacheInvalidator.invalidateForTeams(List.of(team.getId()));
        if (!touchedUsers.isEmpty()) {
            touchedUsers.forEach(rbacCacheInvalidator::invalidateForUser);
        }
        return toTeamView(team);
    }

    private void validateUserRequest(AdminUserRequest request, boolean requirePassword) {
        if (request == null) {
            throw new IllegalArgumentException("request_required");
        }
        if (!StringUtils.hasText(request.username())) {
            throw new IllegalArgumentException("username_required");
        }
        if (!StringUtils.hasText(request.displayName())) {
            throw new IllegalArgumentException("display_name_required");
        }
        if (requirePassword && !StringUtils.hasText(request.password())) {
            throw new IllegalArgumentException("password_required");
        }
        if (request.roles() == null || request.roles().isEmpty()) {
            throw new IllegalArgumentException("roles_required");
        }
    }

    private void syncMemberships(UUID actorId, User user, Set<UUID> requestedTeamIds) {
        List<TeamMembership> existingMemberships = teamMembershipRepository.findByUserId(user.getId());
        Set<UUID> remaining = new HashSet<>(requestedTeamIds);

        for (TeamMembership membership : existingMemberships) {
            UUID teamId = membership.getTeam().getId();
            if (membership.getTeam().getStatus() == TeamStatus.ARCHIVED) {
                membership.setStatus(TeamMembershipStatus.INACTIVE);
                teamMembershipRepository.save(membership);
                remaining.remove(teamId);
                continue;
            }
            if (requestedTeamIds.contains(teamId)) {
                remaining.remove(teamId);
                if (membership.getStatus() != TeamMembershipStatus.ACTIVE) {
                    membership.setStatus(TeamMembershipStatus.ACTIVE);
                    teamMembershipRepository.save(membership);
                }
            } else {
                membership.setStatus(TeamMembershipStatus.INACTIVE);
                teamMembershipRepository.save(membership);
                eventPublisher.publishPostCommit("TeamMembershipRemoved", Map.of(
                        "userId", user.getId().toString(),
                        "teamId", teamId.toString()
                ));
                auditService.recordTeamMembershipRemoved(actorId, user.getId(), teamId);
            }
        }

        if (!remaining.isEmpty()) {
            addMembers(actorId, user, remaining);
        }
    }

    private Set<UUID> syncTeamMemberships(UUID actorId, Team team, Set<UUID> requestedUserIds) {
        List<TeamMembership> existingMemberships = teamMembershipRepository.findByTeamId(team.getId());
        Set<UUID> remaining = new HashSet<>(requestedUserIds);
        Set<UUID> touchedUsers = new HashSet<>();

        for (TeamMembership membership : existingMemberships) {
            UUID userId = membership.getUser().getId();
            if (requestedUserIds.contains(userId)) {
                remaining.remove(userId);
                if (team.getStatus() == TeamStatus.ARCHIVED) {
                    membership.setStatus(TeamMembershipStatus.INACTIVE);
                    teamMembershipRepository.save(membership);
                    touchedUsers.add(userId);
                } else if (membership.getStatus() != TeamMembershipStatus.ACTIVE) {
                    membership.setStatus(TeamMembershipStatus.ACTIVE);
                    teamMembershipRepository.save(membership);
                    touchedUsers.add(userId);
                }
            } else {
                membership.setStatus(TeamMembershipStatus.INACTIVE);
                teamMembershipRepository.save(membership);
                eventPublisher.publishPostCommit("TeamMembershipRemoved", Map.of(
                        "userId", userId.toString(),
                        "teamId", team.getId().toString()
                ));
                auditService.recordTeamMembershipRemoved(actorId, userId, team.getId());
                touchedUsers.add(userId);
            }
        }
        if (!remaining.isEmpty() && team.getStatus() != TeamStatus.ARCHIVED) {
            addMembers(actorId, team, remaining);
            touchedUsers.addAll(remaining);
        }
        return touchedUsers;
    }

    private void addMembers(UUID actorId, User user, Set<UUID> teamIds) {
        for (UUID teamId : teamIds) {
            Team team = teamRepository.findById(teamId)
                    .orElseThrow(() -> new IllegalArgumentException("team_not_found"));
            if (team.getStatus() == TeamStatus.ARCHIVED) {
                throw new IllegalStateException("team_archived");
            }
            TeamMembership membership = new TeamMembership();
            membership.setTeam(team);
            membership.setUser(user);
            membership.setStatus(TeamMembershipStatus.ACTIVE);
            teamMembershipRepository.save(membership);
            eventPublisher.publishPostCommit("TeamMembershipAdded", Map.of(
                    "userId", user.getId().toString(),
                    "teamId", team.getId().toString()
            ));
            auditService.recordTeamMembershipAdded(actorId, user.getId(), team.getId());
        }
    }

    private void addMembers(UUID actorId, Team team, Set<UUID> userIds) {
        for (UUID userId : userIds) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("user_not_found"));
            TeamMembership membership = new TeamMembership();
            membership.setTeam(team);
            membership.setUser(user);
            membership.setStatus(TeamMembershipStatus.ACTIVE);
            teamMembershipRepository.save(membership);
            eventPublisher.publishPostCommit("TeamMembershipAdded", Map.of(
                    "userId", user.getId().toString(),
                    "teamId", team.getId().toString()
            ));
            auditService.recordTeamMembershipAdded(actorId, user.getId(), team.getId());
        }
    }

    private Set<Role> resolveRoles(List<String> requestedRoles) {
        Set<Role> roles = new HashSet<>();
        for (String codeRaw : requestedRoles) {
            if (!StringUtils.hasText(codeRaw)) {
                continue;
            }
            String code = codeRaw.trim().toUpperCase();
            Optional<Role> roleOptional = roleRepository.findByCode(code);
            if (roleOptional.isEmpty()) {
                throw new IllegalArgumentException("role_not_found");
            }
            roles.add(roleOptional.get());
        }
        return roles;
    }

    private UserStatus parseUserStatus(String raw, UserStatus fallback) {
        if (!StringUtils.hasText(raw)) {
            return fallback;
        }
        try {
            return UserStatus.valueOf(raw.trim().toUpperCase());
        } catch (Exception ex) {
            return fallback;
        }
    }

    private TeamStatus parseTeamStatus(String raw, TeamStatus fallback) {
        if (!StringUtils.hasText(raw)) {
            return fallback;
        }
        try {
            return TeamStatus.valueOf(raw.trim().toUpperCase());
        } catch (Exception ex) {
            return fallback;
        }
    }

    private AdminUserView toUserView(User user) {
        List<TeamMembership> memberships = teamMembershipRepository.findByUserId(user.getId());
        List<UUID> teamIds = memberships.stream()
                .filter(tm -> tm.getStatus() == TeamMembershipStatus.ACTIVE && tm.getTeam().getStatus() == TeamStatus.ACTIVE)
                .map(tm -> tm.getTeam().getId())
                .toList();
        List<String> roles = roleService.toRoleCodes(user.getRoles());
        return new AdminUserView(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getStatus().name(),
                roles,
                teamIds,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private AdminTeamView toTeamView(Team team) {
        List<TeamMembership> memberships = teamMembershipRepository.findByTeamId(team.getId());
        List<UUID> memberIds = memberships.stream()
                .filter(tm -> tm.getStatus() == TeamMembershipStatus.ACTIVE)
                .map(tm -> tm.getUser().getId())
                .toList();
        return new AdminTeamView(
                team.getId(),
                team.getName(),
                team.getStatus().name(),
                memberIds,
                team.getCreatedAt()
        );
    }
}
