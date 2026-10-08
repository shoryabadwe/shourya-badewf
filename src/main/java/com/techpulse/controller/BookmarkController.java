package com.techpulse.controller;

import com.techpulse.dto.BookmarkDtos;
import com.techpulse.model.User;
import com.techpulse.repository.UserRepository;
import com.techpulse.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * REST Controller for managing student bookmarks and viewing schedule conflict warnings.
 * Ownership is strictly enforced using the authenticated session user.
 */
@RestController
@RequestMapping("/api/bookmarks")
public class BookmarkController {

    private final EventService eventService;
    private final UserRepository userRepository;

    public BookmarkController(EventService eventService, UserRepository userRepository) {
        this.eventService = eventService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<BookmarkDtos.SavedEventsOverviewResponse> getMyBookmarks(Authentication authentication) {
        Long userId = requireUserId(authentication);
        return ResponseEntity.ok(eventService.getSavedEventsForUser(userId));
    }

    @PostMapping("/{eventId}")
    public ResponseEntity<Map<String, Object>> addBookmark(
            @PathVariable Long eventId,
            Authentication authentication
    ) {
        Long userId = requireUserId(authentication);
        eventService.addBookmark(userId, eventId);
        return ResponseEntity.ok(Map.of("bookmarked", true, "eventId", eventId));
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Map<String, Object>> removeBookmark(
            @PathVariable Long eventId,
            Authentication authentication
    ) {
        Long userId = requireUserId(authentication);
        eventService.removeBookmark(userId, eventId);
        return ResponseEntity.ok(Map.of("bookmarked", false, "eventId", eventId));
    }

    private Long requireUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Authentication is required to manage saved events.");
        }
        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .map(User::getId)
                .orElseThrow(() -> new NoSuchElementException("Authenticated user not found."));
    }
}
