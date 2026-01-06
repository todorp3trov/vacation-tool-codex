package com.company.service;

import com.company.dto.AdminDtos.HolidayImportResponse;
import com.company.integration.EventPublisher;
import com.company.model.IntegrationType;
import com.company.repos.HolidayRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HolidayImportServiceTest {

    @Mock
    private IntegrationConfigService integrationConfigService;
    @Mock
    private HolidayRepository holidayRepository;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private AuditService auditService;

    private HolidayImportService service;

    @BeforeEach
    void setUp() {
        service = new HolidayImportService(
                integrationConfigService,
                holidayRepository,
                eventPublisher,
                auditService,
                new RestTemplateBuilder()
        );
    }

    @Test
    void rejectsYearOutOfRange() {
        HolidayImportResponse response = service.importForYear(UUID.randomUUID(), 1800);
        assertThat(response.outcome()).isEqualTo("failure");
        verify(auditService).recordHolidayImport(any(), anyInt(), anyInt(), anyInt(), any());
        verifyNoInteractions(holidayRepository);
    }

    @Test
    void failsWhenIntegrationMissing() {
        when(integrationConfigService.findActive(IntegrationType.HOLIDAY_API)).thenReturn(Optional.empty());

        HolidayImportResponse response = service.importForYear(UUID.randomUUID(), 2025);

        assertThat(response.outcome()).isEqualTo("failure");
        assertThat(response.message()).containsIgnoringCase("not configured");
    }
}
