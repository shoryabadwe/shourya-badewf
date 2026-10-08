package com.techpulse.service;

import com.techpulse.dto.BookmarkDtos;
import com.techpulse.dto.EventDtos;
import com.techpulse.model.*;
import com.techpulse.repository.BookmarkRepository;
import com.techpulse.repository.EventRepository;
import com.techpulse.repository.ParticipationRepository;
import com.techpulse.repository.UserRepository;
import com.techpulse.source.ConfigurableRestEventSourceAdapter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core Event Discovery, Filtering, Bookmarking, and Administrative Management Service.
 */
@Service
public class EventService {

    private final EventRepository eventRepository;
    private final BookmarkRepository bookmarkRepository;
    private final ParticipationRepository participationRepository;
    private final UserRepository userRepository;
    private final DateWindowService dateWindowService;
    private final ScheduleConflictService scheduleConflictService;

    public EventService(
            EventRepository eventRepository,
            BookmarkRepository bookmarkRepository,
            ParticipationRepository participationRepository,
            UserRepository userRepository,
            DateWindowService dateWindowService,
            ScheduleConflictService scheduleConflictService
    ) {
        this.eventRepository = eventRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.participationRepository = participationRepository;
        this.userRepository = userRepository;
        this.dateWindowService = dateWindowService;
        this.scheduleConflictService = scheduleConflictService;
    }

    /**
     * Searches and filters upcoming events within the requested date window in Asia/Kolkata.
     */
    @Transactional(readOnly = true)
    public EventDtos.PaginatedEventResponse discoverEvents(
            String search,
            String city,
            String topic,
            String eventType,
            String mode,
            String cost,
            BigDecimal maxPrice,
            String dateWindow,
            String sortBy,
            int page,
            int size,
            Long currentUserId
    ) {
        DateWindowService.DateWindowBounds bounds = dateWindowService.resolveWindow(dateWindow);

        List<Event> baseEvents = "RECENTLY_ADDED".equalsIgnoreCase(sortBy)
                ? eventRepository.findByDeletedFalseOrderByCreatedAtDesc()
                : eventRepository.findByDeletedFalseOrderByStartTimeAsc();

        Set<Long> bookmarkedIds = getBookmarkedEventIds(currentUserId);
        Map<Long, Participation> participationMap = getParticipationMap(currentUserId);

        List<Event> filtered = baseEvents.stream()
                .filter(e -> bounds.contains(e.getStartTime()))
                .filter(e -> matchesSearch(e, search))
                .filter(e -> matchesCity(e, city))
                .filter(e -> matchesTopic(e, topic))
                .filter(e -> matchesType(e, eventType))
                .filter(e -> matchesMode(e, mode))
                .filter(e -> matchesCost(e, cost, maxPrice))
                .collect(Collectors.toList());

        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(50, size));
        int totalElements = filtered.size();
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);
        int fromIndex = Math.min(safePage * safeSize, totalElements);
        int toIndex = Math.min(fromIndex + safeSize, totalElements);

        List<EventDtos.EventResponse> pageContent = filtered.subList(fromIndex, toIndex)
                .stream()
                .map(e -> toEventResponse(e, bookmarkedIds.contains(e.getId()), participationMap.get(e.getId())))
                .collect(Collectors.toList());

        EventDtos.PaginatedEventResponse response = new EventDtos.PaginatedEventResponse();
        response.setContent(pageContent);
        response.setPage(safePage);
        response.setSize(safeSize);
        response.setTotalElements(totalElements);
        response.setTotalPages(Math.max(1, totalPages));
        response.setActiveDateWindow(bounds.windowKey());
        response.setWindowStart(bounds.startInclusive());
        response.setWindowEnd(bounds.endInclusive());
        response.setTimezone(DateWindowService.DEFAULT_ZONE.getId());
        return response;
    }

    @Transactional(readOnly = true)
    public EventDtos.EventResponse getEventDetails(Long eventId, Long currentUserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event not found with ID: " + eventId));
        boolean isBookmarked = currentUserId != null && bookmarkRepository.existsByUserIdAndEventId(currentUserId, eventId);
        Participation participation = currentUserId != null
                ? participationRepository.findByUserIdAndEventId(currentUserId, eventId).orElse(null)
                : null;
        return toEventResponse(event, isBookmarked, participation);
    }

    @Transactional(readOnly = true)
    public Event getRawEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event not found with ID: " + eventId));
    }

    @Transactional
    public EventDtos.EventResponse createEventByAdmin(EventDtos.EventRequest req) {
        validateEventTimingsAndCost(req);
        OffsetDateTime now = dateWindowService.nowInKolkata();

        Event event = new Event();
        applyRequestToEvent(event, req);
        event.setSourceName("TECHPULSE Admin Portal");
        event.setDataOrigin(DataSourceOrigin.MANUAL_ADMIN);
        event.setManuallyEdited(true);
        event.setLastFetchedAt(now);
        event.setCreatedAt(now);
        event.setUpdatedAt(now);

        Event saved = eventRepository.save(event);
        return toEventResponse(saved, false, null);
    }

    @Transactional
    public EventDtos.EventResponse updateEventByAdmin(Long eventId, EventDtos.EventRequest req) {
        validateEventTimingsAndCost(req);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event not found with ID: " + eventId));

        applyRequestToEvent(event, req);
        event.setManuallyEdited(true);
        event.setUpdatedAt(dateWindowService.nowInKolkata());

        Event saved = eventRepository.save(event);
        return toEventResponse(saved, false, null);
    }

    @Transactional
    public EventDtos.EventResponse toggleCancelledByAdmin(Long eventId, boolean cancelled) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event not found with ID: " + eventId));
        event.setCancelled(cancelled);
        event.setManuallyEdited(true);
        event.setUpdatedAt(dateWindowService.nowInKolkata());
        Event saved = eventRepository.save(event);
        return toEventResponse(saved, false, null);
    }

    /**
     * Soft-deletes an event (deleted = true).
     * Policy: Preserves existing student bookmarks and participation history records so students
     * do not lose their earned credits or saved archive when an admin removes an event from public discovery.
     */
    @Transactional
    public void softDeleteEventByAdmin(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event not found with ID: " + eventId));
        event.setDeleted(true);
        event.setManuallyEdited(true);
        event.setUpdatedAt(dateWindowService.nowInKolkata());
        eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<EventDtos.EventResponse> listAllEventsForAdmin() {
        return eventRepository.findByDeletedFalseOrderByStartTimeAsc()
                .stream()
                .map(e -> toEventResponse(e, false, null))
                .collect(Collectors.toList());
    }

    /**
     * Adds a bookmark for the logged-in user (idempotent if already bookmarked).
     */
    @Transactional
    public void addBookmark(Long userId, Long eventId) {
        if (bookmarkRepository.existsByUserIdAndEventId(userId, eventId)) {
            return;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event not found with ID: " + eventId));

        Bookmark bookmark = new Bookmark(user, event, dateWindowService.nowInKolkata());
        bookmarkRepository.save(bookmark);

        // Also initialize a SAVED participation record (0 credits) if the user has no record yet
        if (participationRepository.findByUserIdAndEventId(userId, eventId).isEmpty()) {
            Participation p = new Participation();
            p.setUser(user);
            p.setEvent(event);
            p.setStatus(ParticipationStatus.SAVED);
            p.setOutcome(CompetitionOutcome.NONE);
            p.setCredits(0);
            p.setSelfReported(true);
            p.setVerifiedByAdmin(false);
            p.setUpdatedAt(dateWindowService.nowInKolkata());
            participationRepository.save(p);
        }
    }

    @Transactional
    public void removeBookmark(Long userId, Long eventId) {
        bookmarkRepository.findByUserIdAndEventId(userId, eventId)
                .ifPresent(bookmarkRepository::delete);
    }

    @Transactional(readOnly = true)
    public BookmarkDtos.SavedEventsOverviewResponse getSavedEventsForUser(Long userId) {
        List<Bookmark> bookmarks = bookmarkRepository.findByUserIdOrderByEventStartTimeAsc(userId);
        Map<Long, Participation> participationMap = getParticipationMap(userId);
        OffsetDateTime now = dateWindowService.nowInKolkata();

        List<Event> allSavedEvents = bookmarks.stream().map(Bookmark::getEvent).collect(Collectors.toList());
        List<BookmarkDtos.ConflictWarningDto> conflicts = scheduleConflictService.detectConflicts(allSavedEvents);

        Set<Long> conflictingEventIds = new HashSet<>();
        for (BookmarkDtos.ConflictWarningDto c : conflicts) {
            conflictingEventIds.add(c.getFirstEventId());
            conflictingEventIds.add(c.getSecondEventId());
        }

        List<BookmarkDtos.BookmarkItemDto> upcoming = new ArrayList<>();
        List<BookmarkDtos.BookmarkItemDto> past = new ArrayList<>();
        int incompleteTimeCount = 0;

        for (Bookmark b : bookmarks) {
            Event e = b.getEvent();
            BookmarkDtos.BookmarkItemDto item = new BookmarkDtos.BookmarkItemDto();
            item.setBookmarkId(b.getId());
            item.setSavedAt(b.getSavedAt());
            boolean isPast = e.getStartTime() != null && e.getStartTime().isBefore(now);
            item.setPastEvent(isPast);

            if (e.isCancelled()) {
                item.setOverlapCheckStatus("CANCELLED_SKIPPED");
                item.setOverlapCheckMessage("Event is cancelled — excluded from schedule conflict check.");
            } else if (e.getStartTime() == null || e.getEndTime() == null) {
                item.setOverlapCheckStatus("CANNOT_CHECK_OVERLAP");
                item.setOverlapCheckMessage("Cannot check overlap — end time not provided by organizer.");
                incompleteTimeCount++;
            } else if (conflictingEventIds.contains(e.getId())) {
                item.setOverlapCheckStatus("CONFLICT_DETECTED");
                item.setOverlapCheckMessage("Schedule conflict detected with another saved event.");
            } else {
                item.setOverlapCheckStatus("NO_CONFLICT");
                item.setOverlapCheckMessage("No schedule overlap with other saved events.");
            }

            item.setEvent(toEventResponse(e, true, participationMap.get(e.getId())));
            if (isPast) {
                past.add(item);
            } else {
                upcoming.add(item);
            }
        }

        BookmarkDtos.SavedEventsOverviewResponse overview = new BookmarkDtos.SavedEventsOverviewResponse();
        overview.setUpcomingBookmarks(upcoming);
        overview.setPastBookmarks(past);
        overview.setConflicts(conflicts);
        overview.setIncompleteTimeCount(incompleteTimeCount);
        return overview;
    }

    public void validateEventTimingsAndCost(EventDtos.EventRequest req) {
        if (req.getStartTime() == null) {
            throw new IllegalArgumentException("Event start time is required.");
        }
        if (req.getEndTime() != null && req.getEndTime().isBefore(req.getStartTime())) {
            throw new IllegalArgumentException("Event end time cannot be earlier than the start time.");
        }
        if (req.getRegistrationDeadline() != null && req.getRegistrationDeadline().isAfter(req.getStartTime())) {
            throw new IllegalArgumentException("Registration deadline cannot be after the event start time.");
        }
        if (req.getCostType() == CostType.PAID) {
            if (req.getPrice() == null || req.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Paid events must specify a positive price amount.");
            }
        }
        if (req.getRegistrationUrl() != null && !req.getRegistrationUrl().isBlank()) {
            String valid = ConfigurableRestEventSourceAdapter.validateHttpUrl(req.getRegistrationUrl());
            if (valid == null) {
                throw new IllegalArgumentException("Registration URL must be a valid http:// or https:// address.");
            }
        }
        if (req.getOfficialSourceUrl() != null && !req.getOfficialSourceUrl().isBlank()) {
            String valid = ConfigurableRestEventSourceAdapter.validateHttpUrl(req.getOfficialSourceUrl());
            if (valid == null) {
                throw new IllegalArgumentException("Official source URL must be a valid http:// or https:// address.");
            }
        }
    }

    private void applyRequestToEvent(Event event, EventDtos.EventRequest req) {
        event.setTitle(req.getTitle().trim());
        event.setDescription(req.getDescription().trim());
        event.setTopic(req.getTopic().trim());
        event.setEventType(req.getEventType());
        event.setMode(req.getMode());
        event.setOrganizer(req.getOrganizer().trim());
        event.setCity(req.getCity().trim());
        event.setVenueOrPlatform(req.getVenueOrPlatform() != null && !req.getVenueOrPlatform().isBlank()
                ? req.getVenueOrPlatform().trim() : "Not provided");
        event.setStartTime(req.getStartTime());
        event.setEndTime(req.getEndTime());
        event.setRegistrationDeadline(req.getRegistrationDeadline());
        event.setCostType(req.getCostType());
        if (req.getCostType() == CostType.FREE) {
            event.setPrice(BigDecimal.ZERO);
        } else if (req.getCostType() == CostType.PAID) {
            event.setPrice(req.getPrice());
        } else {
            event.setPrice(null);
        }
        event.setCurrency(req.getCurrency() != null && !req.getCurrency().isBlank() ? req.getCurrency().trim() : "INR");
        event.setEligibility(req.getEligibility() != null && !req.getEligibility().isBlank()
                ? req.getEligibility().trim() : "Not provided");
        event.setRegistrationUrl(req.getRegistrationUrl() != null && !req.getRegistrationUrl().isBlank()
                ? req.getRegistrationUrl().trim() : null);
        event.setOfficialSourceUrl(req.getOfficialSourceUrl() != null && !req.getOfficialSourceUrl().isBlank()
                ? req.getOfficialSourceUrl().trim() : null);
        event.setCancelled(req.isCancelled());
        event.setRegistrationClosed(req.isRegistrationClosed());
    }

    private boolean matchesSearch(Event e, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String q = search.trim().toLowerCase();
        return (e.getTitle() != null && e.getTitle().toLowerCase().contains(q))
                || (e.getTopic() != null && e.getTopic().toLowerCase().contains(q))
                || (e.getOrganizer() != null && e.getOrganizer().toLowerCase().contains(q));
    }

    private boolean matchesCity(Event e, String city) {
        if (city == null || city.isBlank() || "ALL".equalsIgnoreCase(city)) {
            return true;
        }
        String target = city.trim();
        if ("OTHER".equalsIgnoreCase(target)) {
            String c = e.getCity() != null ? e.getCity().toLowerCase() : "";
            return !c.equals("mumbai") && !c.equals("navi mumbai") && !c.equals("online");
        }
        return e.getCity() != null && e.getCity().equalsIgnoreCase(target);
    }

    private boolean matchesTopic(Event e, String topic) {
        if (topic == null || topic.isBlank() || "ALL".equalsIgnoreCase(topic)) {
            return true;
        }
        String target = topic.trim();
        if ("OTHER".equalsIgnoreCase(target)) {
            String t = e.getTopic() != null ? e.getTopic().toLowerCase() : "";
            return !t.equals("ai/ml") && !t.equals("java") && !t.equals("cybersecurity")
                    && !t.equals("web development") && !t.equals("data science");
        }
        return e.getTopic() != null && e.getTopic().equalsIgnoreCase(target);
    }

    private boolean matchesType(Event e, String eventType) {
        if (eventType == null || eventType.isBlank() || "ALL".equalsIgnoreCase(eventType)) {
            return true;
        }
        return e.getEventType() != null && e.getEventType().name().equalsIgnoreCase(eventType.trim());
    }

    private boolean matchesMode(Event e, String mode) {
        if (mode == null || mode.isBlank() || "ALL".equalsIgnoreCase(mode)) {
            return true;
        }
        return e.getMode() != null && e.getMode().name().equalsIgnoreCase(mode.trim());
    }

    /**
     * Never treats unknown prices (NOT_PROVIDED) as FREE.
     */
    private boolean matchesCost(Event e, String cost, BigDecimal maxPrice) {
        if (cost != null && !cost.isBlank() && !"ALL".equalsIgnoreCase(cost)) {
            if ("FREE".equalsIgnoreCase(cost) && e.getCostType() != CostType.FREE) {
                return false;
            }
            if ("PAID".equalsIgnoreCase(cost) && e.getCostType() != CostType.PAID) {
                return false;
            }
            if ("NOT_PROVIDED".equalsIgnoreCase(cost) && e.getCostType() != CostType.NOT_PROVIDED) {
                return false;
            }
        }
        if (maxPrice != null) {
            if (e.getCostType() == CostType.FREE) {
                return maxPrice.compareTo(BigDecimal.ZERO) >= 0;
            }
            if (e.getCostType() == CostType.PAID && e.getPrice() != null) {
                return e.getPrice().compareTo(maxPrice) <= 0;
            }
            // Unknown price cannot be guaranteed to be <= maxPrice
            return false;
        }
        return true;
    }

    private Set<Long> getBookmarkedEventIds(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        return bookmarkRepository.findByUserIdOrderByEventStartTimeAsc(userId)
                .stream()
                .map(b -> b.getEvent().getId())
                .collect(Collectors.toSet());
    }

    private Map<Long, Participation> getParticipationMap(Long userId) {
        if (userId == null) {
            return Collections.emptyMap();
        }
        return participationRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .collect(Collectors.toMap(p -> p.getEvent().getId(), p -> p, (a, b) -> a));
    }

    public EventDtos.EventResponse toEventResponse(Event e, boolean bookmarked, Participation participation) {
        EventDtos.EventResponse dto = new EventDtos.EventResponse();
        dto.setId(e.getId());
        dto.setTitle(e.getTitle());
        dto.setDescription(e.getDescription() != null ? e.getDescription() : "Not provided");
        dto.setTopic(e.getTopic() != null ? e.getTopic() : "Not provided");
        dto.setEventType(e.getEventType());
        dto.setMode(e.getMode());
        dto.setOrganizer(e.getOrganizer() != null ? e.getOrganizer() : "Not provided");
        dto.setCity(e.getCity() != null ? e.getCity() : "Online");
        dto.setVenueOrPlatform(e.getVenueOrPlatform() != null && !e.getVenueOrPlatform().isBlank()
                ? e.getVenueOrPlatform() : "Not provided");
        dto.setStartTime(e.getStartTime());
        dto.setEndTime(e.getEndTime());
        dto.setRegistrationDeadline(e.getRegistrationDeadline());
        dto.setCostType(e.getCostType());
        dto.setPrice(e.getPrice());
        dto.setCurrency(e.getCurrency() != null ? e.getCurrency() : "INR");

        if (e.getCostType() == CostType.FREE) {
            dto.setPriceDisplay("Free");
        } else if (e.getCostType() == CostType.PAID && e.getPrice() != null) {
            String symbol = "INR".equalsIgnoreCase(dto.getCurrency()) ? "₹" : dto.getCurrency() + " ";
            dto.setPriceDisplay("Paid · " + symbol + e.getPrice().stripTrailingZeros().toPlainString());
        } else {
            dto.setPriceDisplay("Price not provided");
        }

        dto.setEligibility(e.getEligibility() != null && !e.getEligibility().isBlank()
                ? e.getEligibility() : "Not provided");
        dto.setRegistrationUrl(e.getRegistrationUrl());
        dto.setOfficialSourceUrl(e.getOfficialSourceUrl());
        dto.setSourceName(e.getSourceName() != null ? e.getSourceName() : "Not provided");
        dto.setDataOrigin(e.getDataOrigin());
        if (e.getDataOrigin() == DataSourceOrigin.DEMO_SEED) {
            dto.setDataOriginLabel("Demonstration Dataset");
        } else if (e.getDataOrigin() == DataSourceOrigin.LIVE_IMPORT) {
            dto.setDataOriginLabel("Live Imported Source");
        } else {
            dto.setDataOriginLabel("Manual Admin Entry");
        }
        dto.setLastFetchedAt(e.getLastFetchedAt());
        dto.setCancelled(e.isCancelled());
        dto.setRegistrationClosed(e.isRegistrationClosed());
        dto.setDeleted(e.isDeleted());
        dto.setCountdownLabel(computeCountdownLabel(e));
        dto.setBookmarked(bookmarked);
        if (participation != null) {
            dto.setParticipationStatus(participation.getStatus().name());
            dto.setCompetitionOutcome(participation.getOutcome().name());
            dto.setParticipationVerified(participation.isVerifiedByAdmin());
        }
        dto.setCreatedAt(e.getCreatedAt());
        return dto;
    }

    private String computeCountdownLabel(Event e) {
        if (e.isCancelled()) {
            return "Cancelled";
        }
        OffsetDateTime now = dateWindowService.nowInKolkata();
        OffsetDateTime start = e.getStartTime();
        if (start == null) {
            return "Date not provided";
        }
        if (start.isBefore(now)) {
            if (e.getEndTime() != null && e.getEndTime().isAfter(now)) {
                return "In progress now";
            }
            return "Event completed";
        }
        Duration diff = Duration.between(now, start);
        long totalHours = diff.toHours();
        if (totalHours < 1) {
            long mins = Math.max(1, diff.toMinutes());
            return "Starts in " + mins + " min";
        }
        if (totalHours < 24) {
            return "Starts in " + totalHours + (totalHours == 1 ? " hour" : " hours");
        }
        long days = diff.toDays();
        if (days == 1) {
            return "Starts tomorrow";
        }
        return "Starts in " + days + " days";
    }
}
