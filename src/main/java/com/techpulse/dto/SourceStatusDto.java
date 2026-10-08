package com.techpulse.dto;

import java.time.OffsetDateTime;

/**
 * DTO exposing live event source configuration, refresh timestamps, and stale-cache status.
 */
public class SourceStatusDto {
    private String sourceName;
    private boolean configured;
    private String status;
    private OffsetDateTime lastAttemptAt;
    private OffsetDateTime lastSuccessAt;
    private boolean staleData;
    private int importedCount;
    private int updatedCount;
    private String statusMessage;
    private String requiredEnvInstructions;

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

    public String getRequiredEnvInstructions() {
        return requiredEnvInstructions;
    }

    public void setRequiredEnvInstructions(String requiredEnvInstructions) {
        this.requiredEnvInstructions = requiredEnvInstructions;
    }
}
