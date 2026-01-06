package com.company.integration;

import com.company.model.IntegrationConfig;
import com.company.model.IntegrationState;
import com.company.model.IntegrationType;
import com.company.ops.EventPublishMonitor;
import com.company.repos.IntegrationConfigRepository;
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
public class ExternalDeductionClient {
    private static final Logger log = LoggerFactory.getLogger(ExternalDeductionClient.class);

    private final IntegrationConfigRepository integrationConfigRepository;
    private final RestTemplate restTemplate;
    private final EventPublishMonitor eventPublishMonitor;

    public ExternalDeductionClient(IntegrationConfigRepository integrationConfigRepository,
                                   RestTemplateBuilder builder,
                                   EventPublishMonitor eventPublishMonitor) {
        this.integrationConfigRepository = integrationConfigRepository;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(3))
                .build();
        this.eventPublishMonitor = eventPublishMonitor;
    }

    public DeductionResult deduct(UUID requestId, UUID employeeId, int numberOfDays) {
        Optional<IntegrationConfig> configOptional = integrationConfigRepository
                .findFirstByTypeAndState(IntegrationType.VACATION_BALANCE, IntegrationState.CONFIGURED);
        if (configOptional.isEmpty()) {
            return DeductionResult.unavailableResult("Integration not configured");
        }
        IntegrationConfig config = configOptional.get();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        IdempotencyHelper.apply(headers, requestId);
        if (StringUtils.hasText(config.getAuthToken())) {
            headers.setBearerAuth(config.getAuthToken());
        }
        DeductionRequest payload = new DeductionRequest(requestId.toString(), employeeId.toString(), numberOfDays);
        HttpEntity<DeductionRequest> entity = new HttpEntity<>(payload, headers);

        int[] backoffSeconds = new int[]{0, 1, 2};
        for (int attempt = 0; attempt < backoffSeconds.length; attempt++) {
            if (backoffSeconds[attempt] > 0) {
                sleep(backoffSeconds[attempt]);
            }
            try {
                log.info("Calling INT-001 deduction attempt={} requestId={}", attempt + 1, requestId);
                ResponseEntity<DeductionResponse> response = restTemplate
                        .exchange(config.getEndpointUrl(), HttpMethod.POST, entity, DeductionResponse.class);
                if (response.getStatusCode().is2xxSuccessful()) {
                    return DeductionResult.successResult();
                }
                log.warn("INT-001 deduction unexpected status attempt={} status={}", attempt + 1, response.getStatusCode());
            } catch (ResourceAccessException ex) {
                log.warn("INT-001 deduction timeout attempt={} requestId={}", attempt + 1, requestId);
            } catch (Exception ex) {
                log.warn("INT-001 deduction error attempt={} requestId={} error={}", attempt + 1, requestId, ex.getMessage());
            }
        }
        eventPublishMonitor.recordDeductionFailure(requestId, "External balance system unavailable");
        return DeductionResult.unavailableResult("External balance system unavailable");
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

    public record DeductionRequest(String requestId, String userId, int days) {
    }

    public record DeductionResponse(String status, String message) {
    }

    public record DeductionResult(boolean success, boolean unavailable, String message) {
        public static DeductionResult successResult() {
            return new DeductionResult(true, false, null);
        }

        public static DeductionResult unavailableResult(String message) {
            return new DeductionResult(false, true, message);
        }

        public static DeductionResult failureResult(String message) {
            return new DeductionResult(false, false, message);
        }
    }
}
