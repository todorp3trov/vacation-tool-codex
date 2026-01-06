package com.company.integration;

import com.company.model.IntegrationConfig;
import com.company.model.IntegrationState;
import com.company.model.IntegrationType;
import com.company.repos.IntegrationConfigRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class VacationBalanceClientTest {

    @Mock
    private IntegrationConfigRepository integrationConfigRepository;

    private VacationBalanceClient client;

    @BeforeEach
    void setUp() {
        client = new VacationBalanceClient(integrationConfigRepository, new RestTemplateBuilder());
    }

    @Test
    void returnsUnavailableWhenIntegrationMissing() {
        when(integrationConfigRepository.findFirstByTypeAndState(IntegrationType.VACATION_BALANCE, IntegrationState.CONFIGURED))
                .thenReturn(Optional.empty());

        VacationBalanceClient.BalanceResult result = client.fetchBalance(UUID.randomUUID());

        assertThat(result.unavailable()).isTrue();
        assertThat(result.balance()).isNull();
    }

    @Test
    void fetchesBalanceFromExternalSystem() {
        IntegrationConfig config = new IntegrationConfig();
        config.setType(IntegrationType.VACATION_BALANCE);
        config.setState(IntegrationState.CONFIGURED);
        config.setEndpointUrl("http://localhost/balance");
        when(integrationConfigRepository.findFirstByTypeAndState(IntegrationType.VACATION_BALANCE, IntegrationState.CONFIGURED))
                .thenReturn(Optional.of(config));
        MockRestServiceServer server = MockRestServiceServer.createServer(client.restTemplate());
        server.expect(requestTo(config.getEndpointUrl()))
                .andRespond(withSuccess("{\"balance\":9}", MediaType.APPLICATION_JSON));

        VacationBalanceClient.BalanceResult result = client.fetchBalance(UUID.randomUUID());

        assertThat(result.unavailable()).isFalse();
        assertThat(result.balance()).isEqualByComparingTo("9");
        server.verify();
    }
}
