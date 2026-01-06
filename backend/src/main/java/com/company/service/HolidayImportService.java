package com.company.service;

import com.company.dto.AdminDtos.HolidayImportResponse;
import com.company.integration.EventPublisher;
import com.company.model.Holiday;
import com.company.model.HolidayStatus;
import com.company.model.IntegrationConfig;
import com.company.model.IntegrationType;
import com.company.repos.HolidayRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Service
public class HolidayImportService {
    private static final Logger log = LoggerFactory.getLogger(HolidayImportService.class);
    private static final int MIN_YEAR = 1950;
    private static final int MAX_YEAR = 2100;

    private final IntegrationConfigService integrationConfigService;
    private final HolidayRepository holidayRepository;
    private final EventPublisher eventPublisher;
    private final AuditService auditService;
    private final RestTemplate restTemplate;

    public HolidayImportService(IntegrationConfigService integrationConfigService,
                                HolidayRepository holidayRepository,
                                EventPublisher eventPublisher,
                                AuditService auditService,
                                RestTemplateBuilder builder) {
        this.integrationConfigService = integrationConfigService;
        this.holidayRepository = holidayRepository;
        this.eventPublisher = eventPublisher;
        this.auditService = auditService;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Transactional
    public HolidayImportResponse importForYear(UUID actorId, int year) {
        if (year < MIN_YEAR || year > MAX_YEAR) {
            auditService.recordHolidayImport(actorId, year, 0, 0, "failure");
            return new HolidayImportResponse(year, 0, 0, "failure", "Year out of allowed range");
        }
        Optional<IntegrationConfig> configOptional = integrationConfigService.findActive(IntegrationType.HOLIDAY_API);
        if (configOptional.isEmpty()) {
            auditService.recordHolidayImport(actorId, year, 0, 0, "failure");
            return new HolidayImportResponse(year, 0, 0, "failure", "Holiday integration not configured");
        }
        IntegrationConfig config = configOptional.get();

        String url = config.getEndpointUrl();
        if (!url.contains("?")) {
            url = url + "?year=" + year;
        } else {
            url = url + "&year=" + year;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (StringUtils.hasText(config.getAuthToken())) {
            headers.setBearerAuth(config.getAuthToken());
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        HolidayPayload[] payloads = fetchPayloads(url, entity);
        if (payloads == null) {
            auditService.recordHolidayImport(actorId, year, 0, 0, "failure");
            return new HolidayImportResponse(year, 0, 0, "failure", "Unable to fetch holidays");
        }

        int imported = 0;
        int skipped = 0;
        for (HolidayPayload payload : payloads) {
            if (payload == null || !StringUtils.hasText(payload.date()) || !StringUtils.hasText(payload.name())) {
                skipped++;
                continue;
            }
            LocalDate date;
            try {
                date = LocalDate.parse(payload.date());
            } catch (Exception ex) {
                skipped++;
                continue;
            }
            try {
                upsertHoliday(date, payload.name().trim());
                imported++;
            } catch (Exception ex) {
                skipped++;
            }
        }

        String outcome;
        if (imported == 0) {
            outcome = "failure";
        } else if (skipped > 0) {
            outcome = "partial";
        } else {
            outcome = "success";
        }
        eventPublisher.publishPostCommit("HolidayImported", Map.of(
                "year", year,
                "imported", imported,
                "skipped", skipped
        ));
        auditService.recordHolidayImport(actorId, year, imported, skipped, outcome);
        String message = outcome.equals("failure")
                ? "No holidays imported"
                : outcome.equals("partial")
                ? "Imported " + imported + " holidays, skipped " + skipped
                : "Imported " + imported + " holidays";
        return new HolidayImportResponse(year, imported, skipped, outcome, message);
    }

    private HolidayPayload[] fetchPayloads(String url, HttpEntity<Void> entity) {
        int attempts = 3;
        for (int attempt = 0; attempt < attempts; attempt++) {
            if (attempt > 0) {
                sleep(Duration.ofSeconds(2));
            }
            try {
                ResponseEntity<HolidayPayload[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, HolidayPayload[].class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    return response.getBody();
                }
                log.warn("Holiday API unexpected status {} body={}", response.getStatusCode(), Arrays.toString(response.getBody()));
            } catch (ResourceAccessException ex) {
                log.warn("Holiday API timeout attempt {}", attempt + 1);
            } catch (Exception ex) {
                log.warn("Holiday API error attempt {}: {}", attempt + 1, ex.getMessage());
            }
        }
        return null;
    }

    private void upsertHoliday(LocalDate date, String name) {
        Optional<Holiday> existing = holidayRepository.findByDateAndName(date, name);
        if (existing.isPresent()) {
            Holiday holiday = existing.get();
            holiday.setStatus(HolidayStatus.IMPORTED);
            holiday.setDeprecationReason(null);
            holidayRepository.save(holiday);
            return;
        }
        Holiday holiday = new Holiday();
        holiday.setDate(date);
        holiday.setName(name);
        holiday.setStatus(HolidayStatus.IMPORTED);
        holidayRepository.save(holiday);
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    record HolidayPayload(String date, String name) {
    }
}
