package com.company.service;

import com.company.integration.EventPublisher;
import com.company.integration.ExternalDeductionClient;
import com.company.model.ExternalDeductionStatus;
import com.company.model.User;
import com.company.model.UserStatus;
import com.company.model.VacationRequest;
import com.company.model.VacationRequestStatus;
import com.company.repos.UserRepository;
import com.company.repos.VacationRequestRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HrProcessingServiceTest {

    @Mock
    private VacationRequestRepository vacationRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExternalDeductionClient externalDeductionClient;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private AuditService auditService;

    private HrProcessingService service;
    private UUID hrId;
    private UUID requestId;
    private User employee;
    private User hrUser;

    @BeforeEach
    void setUp() {
        service = new HrProcessingService(vacationRequestRepository, userRepository, externalDeductionClient, eventPublisher, auditService);
        hrId = UUID.randomUUID();
        requestId = UUID.randomUUID();
        employee = new User();
        employee.setId(UUID.randomUUID());
        employee.setDisplayName("Employee");
        employee.setStatus(UserStatus.ACTIVE);
        hrUser = new User();
        hrUser.setId(hrId);
        hrUser.setDisplayName("HR User");
    }

    @Test
    void processesApprovedRequestAndEmitsEvent() {
        VacationRequest request = buildRequest();
        when(vacationRequestRepository.findApprovedUnprocessedById(requestId, VacationRequestStatus.APPROVED))
                .thenReturn(Optional.of(request));
        when(userRepository.findById(hrId)).thenReturn(Optional.of(hrUser));
        when(externalDeductionClient.deduct(request.getId(), employee.getId(), request.getNumberOfDays()))
                .thenReturn(ExternalDeductionClient.DeductionResult.successResult());
        when(vacationRequestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        HrProcessingResult result = service.process(hrId, requestId, "hr note");

        assertThat(result.success()).isTrue();
        assertThat(result.request().getStatus()).isEqualTo(VacationRequestStatus.PROCESSED);
        assertThat(result.request().getHr()).isEqualTo(hrUser);
        assertThat(result.request().getHrNotes()).isEqualTo("hr note");
        assertThat(result.request().getExternalDeductionStatus()).isEqualTo(ExternalDeductionStatus.SUCCESS);
        assertThat(result.request().getProcessedAt()).isNotNull();

        verify(eventPublisher).publishPostCommit(eq("VacationProcessed"), any());
        verify(auditService).recordProcessingSuccess(hrId, requestId, request.getRequestCode());
    }

    @Test
    void blocksWhenExternalUnavailableAndEmitsFailureEvent() {
        VacationRequest request = buildRequest();
        when(vacationRequestRepository.findApprovedUnprocessedById(requestId, VacationRequestStatus.APPROVED))
                .thenReturn(Optional.of(request));
        when(userRepository.findById(hrId)).thenReturn(Optional.of(hrUser));
        when(externalDeductionClient.deduct(request.getId(), employee.getId(), request.getNumberOfDays()))
                .thenReturn(ExternalDeductionClient.DeductionResult.unavailableResult("down"));

        HrProcessingResult result = service.process(hrId, requestId, "hr note");

        assertThat(result.externalUnavailable()).isTrue();
        assertThat(request.getStatus()).isEqualTo(VacationRequestStatus.APPROVED);
        assertThat(request.getProcessedAt()).isNull();
        verify(eventPublisher).publishImmediate(eq("ExternalBalanceSystemUnavailable"), any());
        verify(vacationRequestRepository, never()).save(any());
        verify(auditService).recordProcessingFailure(hrId, requestId, request.getRequestCode(), "down");
    }

    private VacationRequest buildRequest() {
        VacationRequest request = new VacationRequest();
        request.setId(requestId);
        request.setUser(employee);
        request.setStatus(VacationRequestStatus.APPROVED);
        request.setNumberOfDays(3);
        request.setStartDate(LocalDate.now().plusDays(20));
        request.setEndDate(LocalDate.now().plusDays(22));
        request.setRequestCode("VR-123");
        return request;
    }
}
