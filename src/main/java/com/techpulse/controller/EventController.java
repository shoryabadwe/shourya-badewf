package com.techpulse.controller;

import com.techpulse.dto.EventDtos;
import com.techpulse.dto.SourceStatusDto;
import com.techpulse.model.Event;
import com.techpulse.model.User;
import com.techpulse.repository.UserRepository;
import com.techpulse.service.CalendarIcsService;
import com.techpulse.service.EventService;
import com.techpulse.service.LiveSourceSyncService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

/**
 * REST Controller for public Event Discovery, Combined Filtering, Event Details,
 * .ics Calendar Export, and Live Source Status.
 */
@RestController
@RequestMapping("/api")
public class EventController {

    private final EventService eventService;
    private final CalendarIcsService calendarIcsService;
    private final LiveSourceSyncService liveSourceSyncService;
    private final UserRepository userRepository;

    public EventController(
            EventService eventService,
            CalendarIcsService calendarIcsService,
            LiveSourceSyncService liveSourceSyncService,
            UserRepository userRepository
    ) {
        this.eventService = eventService;
        this.calendarIcsService = calendarIcsService;
        this.liveSourceSyncService = liveSourceSyncService;
        this.userRepository = userRepository;
    }

    @GetMapping("/events")
    public ResponseEntity<EventDtos.PaginatedEventResponse> discoverEvents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "ALL") String city,
            @RequestParam(required = false, defaultValue = "ALL") String topic,
            @RequestParam(required = false, defaultValue = "ALL") String eventType,
            @RequestParam(required = false, defaultValue = "ALL") String mode,
            @RequestParam(required = false, defaultValue = "ALL") String cost,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false, defaultValue = "NEXT_30_DAYS") String dateWindow,
            @RequestParam(required = false, defaultValue = "SOONEST") String sortBy,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "9") int size,
            Authentication authentication
    ) {
        Long currentUserId = resolveUserId(authentication);
        EventDtos.PaginatedEventResponse result = eventService.discoverEvents(
                search, city, topic, eventType, mode, cost, maxPrice, dateWindow, sortBy, page, size, currentUserId
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<EventDtos.EventResponse> getEventDetails(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(eventService.getEventDetails(id, currentUserId));
    }

    @GetMapping("/events/{id}/calendar.ics")
    public ResponseEntity<byte[]> downloadCalendarIcs(
            @PathVariable Long id,
            @RequestParam(required = false) String appUrl
    ) {
        Event event = eventService.getRawEventOrThrow(id);
        String icsContent = calendarIcsService.generateIcsForEvent(event, appUrl);
        String safeFileName = "techpulse-event-" + event.getId() + ".ics";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeFileName + "\"")
                .contentType(MediaType.parseMediaType("text/calendar; charset=UTF-8"))
                .body(icsContent.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/sources/status")
    public ResponseEntity<SourceStatusDto> getSourceStatus() {
        return ResponseEntity.ok(liveSourceSyncService.getOrInitializeStatus());
    }

    @PostMapping("/sources/refresh")
    public ResponseEntity<SourceStatusDto> refreshSource() {
        return ResponseEntity.ok(liveSourceSyncService.triggerRefresh());
    }

    private Long resolveUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .map(User::getId)
                .orElse(null);
    }
}
