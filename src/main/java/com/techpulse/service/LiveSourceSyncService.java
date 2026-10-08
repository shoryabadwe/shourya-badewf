package com.techpulse.service;

import com.techpulse.dto.SourceStatusDto;
import com.techpulse.model.DataSourceOrigin;
import com.techpulse.model.Event;
import com.techpulse.model.SourceRefreshLog;
import com.techpulse.repository.EventRepository;
import com.techpulse.repository.SourceRefreshLogRepository;
import com.techpulse.source.EventSourceAdapter;
import com.techpulse.source.ImportedEventPayload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Coordinates live event data synchronization through the EventSourceAdapter interface.
 *
 * Key Guarantees:
 * 1. If the external source requires credentials/URL that are not configured, clearly sets
 *    status = "LIVE_SOURCE_NOT_CONFIGURED" and never claims sample events are live.
 * 2. Caches imported events in the H2 database so previously fetched events remain accessible
 *    during a network outage, setting staleData = true.
 * 3. Deduplicates imports by (sourceName, externalEventId), falling back conservatively to
 *    (title, startTime, organizer) when externalEventId is missing.
 * 4. Preserves student bookmarks and participation records by updating existing Event rows in place
 *    and never overwriting events where manuallyEdited = true.
 */
@Service
public class LiveSourceSyncService {

    private final EventSourceAdapter eventSourceAdapter;
    private final EventRepository eventRepository;
    private final SourceRefreshLogRepository logRepository;
    private final DateWindowService dateWindowService;

    public LiveSourceSyncService(
            EventSourceAdapter eventSourceAdapter,
            EventRepository eventRepository,
            SourceRefreshLogRepository logRepository,
            DateWindowService dateWindowService
    ) {
        this.eventSourceAdapter = eventSourceAdapter;
        this.eventRepository = eventRepository;
        this.logRepository = logRepository;
        this.dateWindowService = dateWindowService;
    }

    @Transactional
    public SourceStatusDto getOrInitializeStatus() {
        String name = eventSourceAdapter.getSourceName();
        Optional<SourceRefreshLog> existing = logRepository.findBySourceNameIgnoreCase(name);
        if (existing.isPresent()) {
            SourceRefreshLog log = existing.get();
            log.setConfigured(eventSourceAdapter.isConfigured());
            if (!eventSourceAdapter.isConfigured() && !"OK".equals(log.getStatus())) {
                log.setStatus("LIVE_SOURCE_NOT_CONFIGURED");
                log.setStatusMessage("Live source not configured. Displaying clearly labelled demonstration dataset and manual entries.");
            }
            return toDto(logRepository.save(log));
        }

        SourceRefreshLog log = new SourceRefreshLog();
        log.setSourceName(name);
        log.setConfigured(eventSourceAdapter.isConfigured());
        if (eventSourceAdapter.isConfigured()) {
            log.setStatus("READY");
            log.setStatusMessage("Live source configured. Click 'Refresh Live Source' to fetch latest events.");
        } else {
            log.setStatus("LIVE_SOURCE_NOT_CONFIGURED");
            log.setStatusMessage("Live source not configured. Displaying clearly labelled demonstration dataset and manual entries.");
        }
        return toDto(logRepository.save(log));
    }

    @Transactional
    public SourceStatusDto triggerRefresh() {
        String name = eventSourceAdapter.getSourceName();
        OffsetDateTime now = dateWindowService.nowInKolkata();

        SourceRefreshLog log = logRepository.findBySourceNameIgnoreCase(name)
                .orElseGet(() -> {
                    SourceRefreshLog created = new SourceRefreshLog();
                    created.setSourceName(name);
                    return created;
                });

        log.setConfigured(eventSourceAdapter.isConfigured());
        log.setLastAttemptAt(now);

        if (!eventSourceAdapter.isConfigured()) {
            log.setStatus("LIVE_SOURCE_NOT_CONFIGURED");
            log.setStaleData(log.getLastSuccessAt() != null);
            log.setStatusMessage("Live source not configured. Set TECHPULSE_LIVE_SOURCE_ENABLED=true and TECHPULSE_LIVE_SOURCE_URL to enable live fetching.");
            return toDto(logRepository.save(log));
        }

        try {
            List<ImportedEventPayload> payloads = eventSourceAdapter.fetchUpcomingEvents();
            int imported = 0;
            int updated = 0;

            for (ImportedEventPayload payload : payloads) {
                Optional<Event> match = findExistingEvent(name, payload);
                if (match.isPresent()) {
                    Event existing = match.get();
                    // Policy: Do not overwrite fields if an administrator manually edited this event
                    if (!existing.isManuallyEdited()) {
                        applyPayloadToEvent(existing, payload, name, now);
                        existing.setUpdatedAt(now);
                        eventRepository.save(existing);
                        updated++;
                    }
                } else {
                    Event created = new Event();
                    applyPayloadToEvent(created, payload, name, now);
                    created.setCreatedAt(now);
                    created.setUpdatedAt(now);
                    eventRepository.save(created);
                    imported++;
                }
            }

            log.setStatus("OK");
            log.setLastSuccessAt(now);
            log.setStaleData(false);
            log.setImportedCount(imported);
            log.setUpdatedCount(updated);
            log.setStatusMessage(String.format("Successfully synchronized %d new and %d updated event(s) from %s.",
                    imported, updated, name));
            return toDto(logRepository.save(log));

        } catch (Exception ex) {
            boolean hasCachedData = log.getLastSuccessAt() != null;
            log.setStatus(hasCachedData ? "STALE_CACHE" : "ERROR");
            log.setStaleData(hasCachedData);
            log.setStatusMessage("Refresh failed (" + ex.getMessage() + "). " +
                    (hasCachedData
                            ? "Serving previously cached events from database (stale data indicator active)."
                            : "No cached live events available."));
            return toDto(logRepository.save(log));
        }
    }

    private Optional<Event> findExistingEvent(String sourceName, ImportedEventPayload payload) {
        if (payload.getExternalEventId() != null && !payload.getExternalEventId().isBlank()) {
            Optional<Event> byExtId = eventRepository.findBySourceNameIgnoreCaseAndExternalEventId(
                    sourceName, payload.getExternalEventId().trim());
            if (byExtId.isPresent()) {
                return byExtId;
            }
        }
        // Conservative fallback deduplication: (title + startTime + organizer)
        if (payload.getTitle() != null && payload.getStartTime() != null && payload.getOrganizer() != null) {
            return eventRepository.findByTitleIgnoreCaseAndStartTimeAndOrganizerIgnoreCase(
                    payload.getTitle().trim(),
                    payload.getStartTime(),
                    payload.getOrganizer().trim()
            );
        }
        return Optional.empty();
    }

    private void applyPayloadToEvent(Event target, ImportedEventPayload payload, String sourceName, OffsetDateTime now) {
        target.setTitle(payload.getTitle());
        target.setDescription(payload.getDescription() != null ? payload.getDescription() : "Not provided");
        target.setTopic(payload.getTopic() != null ? payload.getTopic() : "Other");
        target.setEventType(payload.getEventType());
        target.setMode(payload.getMode());
        target.setOrganizer(payload.getOrganizer() != null ? payload.getOrganizer() : "Not provided");
        target.setCity(payload.getCity() != null ? payload.getCity() : "Online");
        target.setVenueOrPlatform(payload.getVenueOrPlatform() != null ? payload.getVenueOrPlatform() : "Not provided");
        target.setStartTime(payload.getStartTime());
        target.setEndTime(payload.getEndTime());
        target.setRegistrationDeadline(payload.getRegistrationDeadline());
        target.setCostType(payload.getCostType());
        target.setPrice(payload.getPrice());
        target.setCurrency(payload.getCurrency() != null ? payload.getCurrency() : "INR");
        target.setEligibility(payload.getEligibility() != null ? payload.getEligibility() : "Not provided");
        target.setRegistrationUrl(payload.getRegistrationUrl());
        target.setOfficialSourceUrl(payload.getOfficialSourceUrl());
        target.setSourceName(sourceName);
        target.setExternalEventId(payload.getExternalEventId());
        target.setDataOrigin(DataSourceOrigin.LIVE_IMPORT);
        target.setLastFetchedAt(now);
        target.setCancelled(payload.isCancelled());
        target.setRegistrationClosed(payload.isRegistrationClosed());
    }

    private SourceStatusDto toDto(SourceRefreshLog log) {
        SourceStatusDto dto = new SourceStatusDto();
        dto.setSourceName(log.getSourceName());
        dto.setConfigured(log.isConfigured());
        dto.setStatus(log.getStatus());
        dto.setLastAttemptAt(log.getLastAttemptAt());
        dto.setLastSuccessAt(log.getLastSuccessAt());
        dto.setStaleData(log.isStaleData());
        dto.setImportedCount(log.getImportedCount());
        dto.setUpdatedCount(log.getUpdatedCount());
        dto.setStatusMessage(log.getStatusMessage());
        dto.setRequiredEnvInstructions(eventSourceAdapter.getConfigurationInstructions());
        return dto;
    }
}
