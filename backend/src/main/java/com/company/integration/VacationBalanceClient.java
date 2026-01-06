package com.company.integration;

import com.company.model.IntegrationConfig;
import com.company.model.IntegrationState;
import com.company.model.IntegrationType;
import com.company.repos.IntegrationConfigRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
public class VacationBalanceClient {
    private static final Logger log = LoggerFactory.getLogger(VacationBalanceClient.class);

    private final IntegrationConfigRepository integrationConfigRepository;
    private final RestTemplate restTemplate;

    public VacationBalanceClient(IntegrationConfigRepository integrationConfigRepository,
                                 RestTemplateBuilder builder) {
        this.integrationConfigRepository = integrationConfigRepository;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(3))
                .build();
    }

    public BalanceResult fetchBalance(UUID userId) {
        Optional<IntegrationConfig> configOptional = integrationConfigRepository
                .findFirstByTypeAndState(IntegrationType.VACATION_BALANCE, IntegrationState.CONFIGURED);
        if (configOptional.isEmpty()) {
            return BalanceResult.unavailable("Integration not configured");
        }
        IntegrationConfig config = configOptional.get();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.add("X-User-Id", userId.toString());
        if (StringUtils.hasText(config.getAuthToken())) {
            headers.setBearerAuth(config.getAuthToken());
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        int[] backoffSeconds = new int[]{0, 1, 2, 4};
        for (int attempt = 0; attempt < backoffSeconds.length; attempt++) {
            if (backoffSeconds[attempt] > 0) {
                sleep(backoffSeconds[attempt]);
            }
            try {
                ResponseEntity<BalanceApiResponse> response = restTemplate
                        .exchange(config.getEndpointUrl(), HttpMethod.GET, entity, BalanceApiResponse.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null
                        && response.getBody().balance() != null) {
                    return BalanceResult.available(response.getBody().balance());
                }
                log.warn("Unexpected response from INT-001: status={}, body={}", response.getStatusCode(), response.getBody());
            } catch (ResourceAccessException ex) {
                log.warn("Timeout reaching INT-001 on attempt {}", attempt + 1);
            } catch (Exception ex) {
                log.warn("Error calling INT-001 on attempt {}: {}", attempt + 1, ex.getMessage());
            }
        }
        return BalanceResult.unavailable("External balance system unavailable");
    }

    private void sleep(int seconds) {
        try {
            Thread.sleep(Duration.ofSeconds(seconds).toMillis());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    RestTemplate restTemplate() {
        return restTemplate;
    }

    public record BalanceApiResponse(BigDecimal balance) {
    }

    public record BalanceResult(BigDecimal balance, boolean unavailable, String reason) {
        public static BalanceResult available(BigDecimal balance) {
            return new BalanceResult(balance, false, null);
        }

        public static BalanceResult unavailable(String reason) {
            return new BalanceResult(null, true, reason);
        }
    }
}
