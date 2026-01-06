package com.company.service;

import com.company.dto.AdminDtos.IntegrationConfigDto;
import com.company.dto.AdminDtos.IntegrationConfigRequest;
import com.company.integration.EventPublisher;
import com.company.model.IntegrationConfig;
import com.company.model.IntegrationState;
import com.company.model.IntegrationType;
import com.company.repos.IntegrationConfigRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class IntegrationConfigService {

    private final IntegrationConfigRepository integrationConfigRepository;
    private final EventPublisher eventPublisher;
    private final AuditService auditService;

    public IntegrationConfigService(IntegrationConfigRepository integrationConfigRepository,
                                    EventPublisher eventPublisher,
                                    AuditService auditService) {
        this.integrationConfigRepository = integrationConfigRepository;
        this.eventPublisher = eventPublisher;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<IntegrationConfigDto> listConfigs() {
        return integrationConfigRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public IntegrationConfigDto create(UUID actorId, IntegrationConfigRequest request) {
        validate(request);
        IntegrationConfig config = new IntegrationConfig();
        config.setType(parseType(request.type()));
        config.setEndpointUrl(request.endpointUrl().trim());
        config.setState(IntegrationState.CONFIGURED);
        if (StringUtils.hasText(request.authToken())) {
            config.setAuthToken(request.authToken().trim());
        }
        IntegrationConfig saved = integrationConfigRepository.save(config);
        publishConfiguredEvent(saved);
        auditService.recordIntegrationConfigured(actorId, saved.getId(), saved.getType().name(), saved.getEndpointUrl());
        return toDto(saved);
    }

    @Transactional
    public IntegrationConfigDto update(UUID actorId, UUID id, IntegrationConfigRequest request) {
        validate(request);
        Optional<IntegrationConfig> configOptional = integrationConfigRepository.findById(id);
        if (configOptional.isEmpty()) {
            return null;
        }
        IntegrationConfig config = configOptional.get();
        config.setType(parseType(request.type()));
        config.setEndpointUrl(request.endpointUrl().trim());
        config.setState(IntegrationState.CONFIGURED);
        if (StringUtils.hasText(request.authToken())) {
            config.setAuthToken(request.authToken().trim());
        }
        IntegrationConfig saved = integrationConfigRepository.save(config);
        publishConfiguredEvent(saved);
        auditService.recordIntegrationConfigured(actorId, saved.getId(), saved.getType().name(), saved.getEndpointUrl());
        return toDto(saved);
    }

    @Transactional
    public IntegrationConfigDto disable(UUID actorId, UUID id) {
        Optional<IntegrationConfig> configOptional = integrationConfigRepository.findById(id);
        if (configOptional.isEmpty()) {
            return null;
        }
        IntegrationConfig config = configOptional.get();
        config.setState(IntegrationState.DISABLED);
        IntegrationConfig saved = integrationConfigRepository.save(config);
        eventPublisher.publishPostCommit("IntegrationDisabled", Map.of(
                "id", saved.getId().toString(),
                "type", saved.getType().name()
        ));
        auditService.recordIntegrationDisabled(actorId, saved.getId(), saved.getType().name());
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public Optional<IntegrationConfig> findActive(IntegrationType type) {
        return integrationConfigRepository.findFirstByTypeAndState(type, IntegrationState.CONFIGURED);
    }

    private void validate(IntegrationConfigRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request_required");
        }
        if (!StringUtils.hasText(request.type())) {
            throw new IllegalArgumentException("type_required");
        }
        if (!StringUtils.hasText(request.endpointUrl())) {
            throw new IllegalArgumentException("endpoint_required");
        }
    }

    private IntegrationType parseType(String raw) {
        return IntegrationType.valueOf(raw.trim().toUpperCase());
    }

    private IntegrationConfigDto toDto(IntegrationConfig config) {
        return new IntegrationConfigDto(
                config.getId(),
                config.getType().name(),
                config.getState().name(),
                config.getEndpointUrl(),
                StringUtils.hasText(config.getAuthToken()),
                config.getUpdatedAt()
        );
    }

    private void publishConfiguredEvent(IntegrationConfig config) {
        eventPublisher.publishPostCommit("IntegrationConfigured", Map.of(
                "id", config.getId().toString(),
                "type", config.getType().name(),
                "endpoint", config.getEndpointUrl()
        ));
    }
}
