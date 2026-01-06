package com.company.integration;

import com.company.model.IntegrationConfig;
import com.company.model.IntegrationState;
import com.company.model.IntegrationType;
import com.company.ops.EventPublishMonitor;
import com.company.repos.IntegrationConfigRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class ExternalDeductionClientTest {

    @Mock
    private IntegrationConfigRepository integrationConfigRepository;

    @Mock
    private EventPublishMonitor eventPublishMonitor;

    private ExternalDeductionClient client;

    @BeforeEach
    void setUp() {
        client = new ExternalDeductionClient(integrationConfigRepository, new RestTemplateBuilder(), eventPublishMonitor);
    }

    @Test
    void returnsUnavailableWhenIntegrationMissing() {
        when(integrationConfigRepository.findFirstByTypeAndState(IntegrationType.VACATION_BALANCE, IntegrationState.CONFIGURED))
                .thenReturn(Optional.empty());

        ExternalDeductionClient.DeductionResult result = client.deduct(UUID.randomUUID(), UUID.randomUUID(), 3);

        assertThat(result.unavailable()).isTrue();
        verify(eventPublishMonitor, never()).recordDeductionFailure(any(UUID.class), any(String.class));
    }

    @Test
    void sendsIdempotencyKeyAndProcessesSuccess() {
        IntegrationConfig config = new IntegrationConfig();
        config.setType(IntegrationType.VACATION_BALANCE);
        config.setState(IntegrationState.CONFIGURED);
        config.setEndpointUrl("http://localhost/deduct");
        when(integrationConfigRepository.findFirstByTypeAndState(IntegrationType.VACATION_BALANCE, IntegrationState.CONFIGURED))
                .thenReturn(Optional.of(config));
        MockRestServiceServer server = MockRestServiceServer.createServer(client.restTemplate());
        UUID requestId = UUID.randomUUID();
        server.expect(requestTo(config.getEndpointUrl()))
                .andExpect(header("Idempotency-Key", requestId.toString()))
                .andRespond(withSuccess("{\"status\":\"SUCCESS\"}", MediaType.APPLICATION_JSON));

        ExternalDeductionClient.DeductionResult result = client.deduct(requestId, UUID.randomUUID(), 2);

        assertThat(result.success()).isTrue();
        server.verify();
    }

    @Test
    void recordsFailureAfterRetries() {
        IntegrationConfig config = new IntegrationConfig();
        config.setType(IntegrationType.VACATION_BALANCE);
        config.setState(IntegrationState.CONFIGURED);
        config.setEndpointUrl("http://localhost/deduct");
        when(integrationConfigRepository.findFirstByTypeAndState(IntegrationType.VACATION_BALANCE, IntegrationState.CONFIGURED))
                .thenReturn(Optional.of(config));
        MockRestServiceServer server = MockRestServiceServer.createServer(client.restTemplate());
        UUID requestId = UUID.randomUUID();
        server.expect(ExpectedCount.times(3), requestTo(config.getEndpointUrl()))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        ExternalDeductionClient.DeductionResult result = client.deduct(requestId, UUID.randomUUID(), 4);

        assertThat(result.unavailable()).isTrue();
        verify(eventPublishMonitor).recordDeductionFailure(requestId, "External balance system unavailable");
        server.verify();
    }
}
