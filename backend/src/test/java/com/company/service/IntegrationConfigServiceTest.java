package com.company.service;

import com.company.dto.AdminDtos.IntegrationConfigRequest;
import com.company.integration.EventPublisher;
import com.company.model.IntegrationConfig;
import com.company.model.IntegrationState;
import com.company.model.IntegrationType;
import com.company.repos.IntegrationConfigRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntegrationConfigServiceTest {

    @Mock
    private IntegrationConfigRepository integrationConfigRepository;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private AuditService auditService;

    private IntegrationConfigService service;

    @BeforeEach
    void setUp() {
        service = new IntegrationConfigService(integrationConfigRepository, eventPublisher, auditService);
    }

    @Test
    void rejectsMissingEndpoint() {
        IntegrationConfigRequest request = new IntegrationConfigRequest("HOLIDAY_API", "", null);
        assertThatThrownBy(() -> service.create(UUID.randomUUID(), request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void disablesConfigAndEmitsEvent() {
        UUID id = UUID.randomUUID();
        IntegrationConfig config = new IntegrationConfig();
        config.setId(id);
        config.setType(IntegrationType.VACATION_BALANCE);
        config.setEndpointUrl("http://example.com");
        config.setState(IntegrationState.CONFIGURED);

        when(integrationConfigRepository.findById(id)).thenReturn(Optional.of(config));
        when(integrationConfigRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.disable(UUID.randomUUID(), id);

        assertThat(config.getState()).isEqualTo(IntegrationState.DISABLED);
        verify(eventPublisher).publishPostCommit(any(), any());
        verify(auditService).recordIntegrationDisabled(any(), any(), any());
    }
}
