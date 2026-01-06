package com.company.service;

import com.company.dto.AdminDtos.HolidayAdminItem;
import com.company.integration.EventPublisher;
import com.company.model.Holiday;
import com.company.model.HolidayStatus;
import com.company.repos.HolidayRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class HolidayService {

    private final HolidayRepository holidayRepository;
    private final EventPublisher eventPublisher;
    private final AuditService auditService;

    public HolidayService(HolidayRepository holidayRepository,
                          EventPublisher eventPublisher,
                          AuditService auditService) {
        this.holidayRepository = holidayRepository;
        this.eventPublisher = eventPublisher;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<Integer> listImportedYears() {
        return holidayRepository.findYearsWithStatus(HolidayStatus.IMPORTED);
    }

    @Transactional(readOnly = true)
    public List<HolidayAdminItem> listImportedByYear(int year) {
        return holidayRepository.findByYear(HolidayStatus.IMPORTED, year).stream()
                .map(this::toAdminItem)
                .toList();
    }

    @Transactional
    public HolidayAdminItem deprecate(UUID actorId, UUID holidayId, String reason) {
        Optional<Holiday> optional = holidayRepository.findById(holidayId);
        if (optional.isEmpty()) {
            return null;
        }
        Holiday holiday = optional.get();
        holiday.setStatus(HolidayStatus.DEPRECATED);
        if (StringUtils.hasText(reason)) {
            holiday.setDeprecationReason(reason.trim());
        }
        Holiday saved = holidayRepository.save(holiday);
        eventPublisher.publishPostCommit("HolidayDeprecated", Map.of(
                "holidayId", saved.getId().toString(),
                "date", saved.getDate().toString()
        ));
        auditService.recordHolidayDeprecation(actorId, saved.getId(), reason);
        return toAdminItem(saved);
    }

    private HolidayAdminItem toAdminItem(Holiday holiday) {
        return new HolidayAdminItem(
                holiday.getId(),
                holiday.getDate() != null ? holiday.getDate().toString() : null,
                holiday.getName(),
                holiday.getStatus().name(),
                holiday.getDeprecationReason()
        );
    }
}
