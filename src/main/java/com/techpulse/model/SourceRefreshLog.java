package com.techpulse.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Tracks the status of external live event source synchronization,
 * allowing the UI to show whether live sources are configured, active, or serving stale cached data.
 */
@Entity
@Table(name = "source_refresh_logs")
public class SourceRefreshLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String sourceName;

    @Column(nullable = false)
    private boolean configured;

    /**
     * Possible values: LIVE_SOURCE_NOT_CONFIGURED, OK, STALE_CACHE, ERROR
     */
    @Column(nullable = false, length = 40)
    private String status;

    @Column
    private OffsetDateTime lastAttemptAt;

    @Column
    private OffsetDateTime lastSuccessAt;

    @Column(nullable = false)
    private boolean staleData = false;

    @Column(nullable = false)
    private int importedCount = 0;

    @Column(nullable = false)
    private int updatedCount = 0;

    @Column(length = 500)
    private String statusMessage;

    public SourceRefreshLog() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public boolean isConfigured() {
        return configured;
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getLastAttemptAt() {
        return lastAttemptAt;
    }

    public void setLastAttemptAt(OffsetDateTime lastAttemptAt) {
        this.lastAttemptAt = lastAttemptAt;
    }

    public OffsetDateTime getLastSuccessAt() {
        return lastSuccessAt;
    }

    public void setLastSuccessAt(OffsetDateTime lastSuccessAt) {
        this.lastSuccessAt = lastSuccessAt;
    }

    public boolean isStaleData() {
        return staleData;
    }

    public void setStaleData(boolean staleData) {
        this.staleData = staleData;
    }

    public int getImportedCount() {
        return importedCount;
    }

    public void setImportedCount(int importedCount) {
        this.importedCount = importedCount;
    }

    public int getUpdatedCount() {
        return updatedCount;
    }

    public void setUpdatedCount(int updatedCount) {
        this.updatedCount = updatedCount;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
}
