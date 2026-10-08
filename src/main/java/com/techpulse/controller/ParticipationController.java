package com.techpulse.controller;

import com.techpulse.dto.ParticipationDtos;
import com.techpulse.model.User;
import com.techpulse.repository.UserRepository;
import com.techpulse.service.ParticipationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

/**
 * REST Controller for student participation tracking, credit calculation, badges, and competition win rates.
 * Enforces ownership so students can only view and update their own participation records.
 */
@RestController
@RequestMapping("/api/participations")
public class ParticipationController {

    private final ParticipationService participationService;
    private final UserRepository userRepository;

    public ParticipationController(ParticipationService participationService, UserRepository userRepository) {
        this.participationService = participationService;
        this.userRepository = userRepository;
    }

    @GetMapping("/summary")
    public ResponseEntity<ParticipationDtos.ActivitySummaryResponse> getMyActivitySummary(Authentication authentication) {
        Long userId = requireUserId(authentication);
        return ResponseEntity.ok(participationService.buildActivitySummary(userId));
    }

    @PostMapping
    public ResponseEntity<ParticipationDtos.ParticipationRecordDto> upsertMyParticipation(
            @Valid @RequestBody ParticipationDtos.ParticipationUpsertRequest request,
            Authentication authentication
    ) {
        Long userId = requireUserId(authentication);
        ParticipationDtos.ParticipationRecordDto updated =
                participationService.upsertStudentParticipation(userId, request);
        return ResponseEntity.ok(updated);
    }

    private Long requireUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Authentication is required to access participation records.");
        }
        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .map(User::getId)
                .orElseThrow(() -> new NoSuchElementException("Authenticated user not found."));
    }
}
