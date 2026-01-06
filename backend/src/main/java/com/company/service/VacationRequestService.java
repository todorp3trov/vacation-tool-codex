package com.company.service;

import com.company.integration.EventPublisher;
import com.company.integration.VacationBalanceClient;
import com.company.model.User;
import com.company.model.VacationRequest;
import com.company.model.VacationRequestStatus;
import com.company.repos.UserRepository;
import com.company.repos.VacationRequestRepository;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VacationRequestService {
    private static final Logger log = LoggerFactory.getLogger(VacationRequestService.class);

    private final VacationRequestRepository vacationRequestRepository;
    private final UserRepository userRepository;
    private final DayCountService dayCountService;
    private final VacationBalanceClient vacationBalanceClient;
    private final BalanceSessionCache balanceSessionCache;
    private final EventPublisher eventPublisher;
    private final AuditService auditService;

    public VacationRequestService(VacationRequestRepository vacationRequestRepository,
                                  UserRepository userRepository,
                                  DayCountService dayCountService,
                                  VacationBalanceClient vacationBalanceClient,
                                  BalanceSessionCache balanceSessionCache,
                                  EventPublisher eventPublisher,
                                  AuditService auditService) {
        this.vacationRequestRepository = vacationRequestRepository;
        this.userRepository = userRepository;
        this.dayCountService = dayCountService;
        this.vacationBalanceClient = vacationBalanceClient;
        this.balanceSessionCache = balanceSessionCache;
        this.eventPublisher = eventPublisher;
        this.auditService = auditService;
    }

    @Transactional
    public VacationSubmissionResult submit(UUID userId, HttpSession session, LocalDate startDate, LocalDate endDate) {
        validateDates(startDate, endDate);
        validateMinimumNotice(startDate);

        long numberOfDays = dayCountService.computeNumberOfDays(startDate, endDate);
        if (numberOfDays <= 0) {
            return VacationSubmissionResult.failure("invalid_range", "No working days in selected range");
        }

        VacationBalanceClient.BalanceResult balanceResult = vacationBalanceClient.fetchBalance(userId);
        if (balanceResult.unavailable()) {
            balanceSessionCache.storeUnavailable(session, userId);
            eventPublisher.publishImmediate("ExternalBalanceSystemUnavailable", Map.of(
                    "employeeId", userId.toString(),
                    "startDate", startDate.toString(),
                    "endDate", endDate.toString()
            ));
            auditService.recordSubmissionBlocked(userId, balanceResult.reason());
            return VacationSubmissionResult.externalUnavailable(balanceResult.reason());
        }

        balanceSessionCache.store(session, userId, balanceResult.balance());
        BigDecimal officialBalance = balanceResult.balance();
        if (BigDecimal.valueOf(numberOfDays).compareTo(officialBalance) > 0) {
            return VacationSubmissionResult.failure("insufficient_balance", "Requested days exceed remaining balance");
        }

        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return VacationSubmissionResult.failure("user_not_found", "User not found");
        }
        User user = userOptional.get();

        VacationRequest request = new VacationRequest();
        request.setUser(user);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setNumberOfDays((int) numberOfDays);
        request.setStatus(VacationRequestStatus.PENDING);
        request.setSubmittedAt(Instant.now());
        request.setRequestCode(generateRequestCode(userId, startDate));

        VacationRequest saved = vacationRequestRepository.save(request);

        eventPublisher.publishPostCommit("VacationRequested", Map.of(
                "requestId", saved.getId().toString(),
                "requestCode", saved.getRequestCode(),
                "employeeId", userId.toString(),
                "startDate", startDate.toString(),
                "endDate", endDate.toString(),
                "requestedDays", numberOfDays
        ));
        auditService.recordSubmission(userId, saved.getId(), saved.getRequestCode(), saved.getNumberOfDays());
        return VacationSubmissionResult.success(saved);
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate and endDate are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be on or after startDate");
        }
    }

    private void validateMinimumNotice(LocalDate startDate) {
        LocalDate threshold = LocalDate.now().plusDays(14);
        if (startDate.isBefore(threshold)) {
            throw new IllegalArgumentException("Requests must be submitted at least 14 days in advance");
        }
    }

    private String generateRequestCode(UUID userId, LocalDate startDate) {
        String base = startDate != null ? startDate.toString() : "";
        String suffix = Long.toString(Instant.now().truncatedTo(ChronoUnit.MILLIS).toEpochMilli());
        return "VR-" + userId.toString().substring(0, 8) + "-" + base + "-" + suffix;
    }
}
