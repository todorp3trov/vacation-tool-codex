package com.company.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "holidays")
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HolidayStatus status = HolidayStatus.IMPORTED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deprecation_reason")
    private String deprecationReason;

    @PrePersist
    public void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HolidayStatus getStatus() {
        return status;
    }

    public void setStatus(HolidayStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getDeprecationReason() {
        return deprecationReason;
    }

    public void setDeprecationReason(String deprecationReason) {
        this.deprecationReason = deprecationReason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Holiday holiday)) return false;
        return Objects.equals(id, holiday.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
