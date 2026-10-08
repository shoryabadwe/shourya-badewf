package com.techpulse.controller;

import com.techpulse.dto.EventDtos;
import com.techpulse.dto.ParticipationDtos;
import com.techpulse.service.EventService;
import com.techpulse.service.ParticipationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Administrative Operations.
 * Protected by Spring Security role check (@PreAuthorize("hasRole('ADMIN')") and SecurityFilterChain).
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final EventService eventService;
    private final ParticipationService participationService;

    public AdminController(EventService eventService, ParticipationService participationService) {
        this.eventService = eventService;
        this.participationService = participationService;
    }

    @GetMapping("/events")
    public ResponseEntity<List<EventDtos.EventResponse>> listAllEvents() {
        return ResponseEntity.ok(eventService.listAllEventsForAdmin());
    }

    @PostMapping("/events")
    public ResponseEntity<EventDtos.EventResponse> createEvent(@Valid @RequestBody EventDtos.EventRequest request) {
        EventDtos.EventResponse created = eventService.createEventByAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/events/{id}")
    public ResponseEntity<EventDtos.EventResponse> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody EventDtos.EventRequest request
    ) {
        return ResponseEntity.ok(eventService.updateEventByAdmin(id, request));
    }

    @PatchMapping("/events/{id}/cancel")
    public ResponseEntity<EventDtos.EventResponse> setEventCancelled(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body
    ) {
        boolean cancelled = Boolean.TRUE.equals(body.get("cancelled"));
        return ResponseEntity.ok(eventService.toggleCancelledByAdmin(id, cancelled));
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<Map<String, Object>> softDeleteEvent(@PathVariable Long id) {
        eventService.softDeleteEventByAdmin(id);
        return ResponseEntity.ok(Map.of(
                "deleted", true,
                "eventId", id,
                "policy", "Soft-deleted from public discovery. Student bookmarks and participation records remain intact."
        ));
    }

    @GetMapping("/participations")
    public ResponseEntity<List<ParticipationDtos.ParticipationRecordDto>> listAllParticipations() {
        return ResponseEntity.ok(participationService.listAllParticipationsForAdmin());
    }

    @PutMapping("/participations/{id}/verify")
    public ResponseEntity<ParticipationDtos.ParticipationRecordDto> verifyOrCorrectParticipation(
            @PathVariable Long id,
            @Valid @RequestBody ParticipationDtos.AdminVerifyParticipationRequest request
    ) {
        return ResponseEntity.ok(participationService.verifyOrCorrectByAdmin(id, request));
    }
}
