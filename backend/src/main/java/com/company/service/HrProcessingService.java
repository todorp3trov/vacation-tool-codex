package com.company.service;

import com.company.dto.HrProcessingDetail;
import com.company.dto.HrProcessingItem;
import com.company.integration.ExternalDeductionClient;
import com.company.integration.EventPublisher;
import com.company.model.ExternalDeductionStatus;
import com.company.model.User;
import com.company.model.VacationRequest;
import com.company.model.VacationRequestStatus;
import com.company.repos.UserRepository;
import com.company.repos.VacationRequestRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HrProcessingService {

    private final VacationRequestRepository vacationRequestRepository;
    private final UserRepository userRepository;
    private final ExternalDeductionClient externalDeductionClient;
    private final EventPublisher eventPublisher;
    private final AuditService auditService;

    public HrProcessingService(VacationRequestRepository vacationRequestRepository,
                               UserRepository userRepository,
                               ExternalDeductionClient externalDeductionClient,
                               EventPublisher eventPublisher,
                               AuditService auditService) {
        this.vacationRequestRepository = vacationRequestRepository;
        this.userRepository = userRepository;
        this.externalDeductionClient = externalDeductionClient;
        this.eventPublisher = eventPublisher;
        this.auditService = auditService;
    }

    public List<HrProcessingItem> loadQueue() {
        return vacationRequestRepository.findUnprocessedApproved(VacationRequestStatus.APPROVED)
                .stream()
                .map(this::toItem)
                .toList();
    }

    public HrProcessingDetail loadDetail(UUID requestId) {
        Optional<VacationRequest> optional = vacationRequestRepository.findApprovedUnprocessedById(
                requestId,
                VacationRequestStatus.APPROVED
        );
        return optional.map(vr -> new HrProcessingDetail(toItem(vr))).orElse(null);
    }

    @Transactional
    public HrProcessingResult process(UUID hrId, UUID requestId, String hrNotes) {
        Optional<VacationRequest> optional = vacationRequestRepository.findApprovedUnprocessedById(
                requestId,
                VacationRequestStatus.APPROVED
        );
        if (optional.isEmpty()) {
            return HrProcessingResult.notFoundResult();
        }
        VacationRequest request = optional.get();
        if (request.getStatus() != VacationRequestStatus.APPROVED || request.getProcessedAt() != null) {
            return HrProcessingResult.invalidState("Only approved requests can be processed");
        }

        Optional<User> hrUserOptional = userRepository.findById(hrId);
        if (hrUserOptional.isEmpty()) {
            return HrProcessingResult.failure("hr_not_found", "HR user not found");
        }
        User hrUser = hrUserOptional.get();

        auditService.recordProcessingAttempt(hrId, request.getId(), request.getRequestCode());

        ExternalDeductionClient.DeductionResult deductionResult = externalDeductionClient.deduct(
                request.getId(),
                request.getUser().getId(),
                request.getNumberOfDays()
        );
        if (!deductionResult.success()) {
            String reason = deductionResult.message() != null ? deductionResult.message() : "External deduction failed";
            auditService.recordProcessingFailure(hrId, request.getId(), request.getRequestCode(), reason);
            if (deductionResult.unavailable()) {
                eventPublisher.publishImmediate("ExternalBalanceSystemUnavailable", Map.of(
                        "employeeId", request.getUser().getId().toString(),
                        "requestId", request.getId().toString()
                ));
                return HrProcessingResult.externalUnavailable(reason);
            }
            return HrProcessingResult.failure("deduction_failed", reason);
        }

        request.setHr(hrUser);
        request.setStatus(VacationRequestStatus.PROCESSED);
        request.setProcessedAt(Instant.now());
        if (hrNotes != null) {
            request.setHrNotes(hrNotes);
        }
        request.setExternalDeductionStatus(ExternalDeductionStatus.SUCCESS);

        VacationRequest saved = vacationRequestRepository.save(request);
        eventPublisher.publishPostCommit("VacationProcessed", Map.of(
                "requestId", saved.getId().toString(),
                "requestCode", saved.getRequestCode(),
                "employeeId", saved.getUser().getId().toString(),
                "hrId", hrId.toString(),
                "startDate", saved.getStartDate().toString(),
                "endDate", saved.getEndDate().toString(),
                "requestedDays", saved.getNumberOfDays(),
                "processedAt", saved.getProcessedAt().toString()
        ));
        auditService.recordProcessingSuccess(hrId, saved.getId(), saved.getRequestCode());
        return HrProcessingResult.successResult(saved);
    }

    private HrProcessingItem toItem(VacationRequest request) {
        return new HrProcessingItem(
                request.getId(),
                request.getUser().getId(),
                request.getUser().getDisplayName(),
                request.getStartDate(),
                request.getEndDate(),
                request.getNumberOfDays(),
                request.getStatus().name(),
                request.getRequestCode(),
                request.getManagerNotes(),
                request.getHrNotes()
        );
    }
}
