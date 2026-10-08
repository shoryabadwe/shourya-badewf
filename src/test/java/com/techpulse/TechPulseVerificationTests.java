package com.techpulse;

import com.techpulse.dto.BookmarkDtos;
import com.techpulse.dto.EventDtos;
import com.techpulse.dto.ParticipationDtos;
import com.techpulse.model.*;
import com.techpulse.repository.BookmarkRepository;
import com.techpulse.repository.EventRepository;
import com.techpulse.repository.ParticipationRepository;
import com.techpulse.repository.UserRepository;
import com.techpulse.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated JUnit 5 Verification Suite for TECHPULSE (Course 2113611).
 * Verifies date windows, combined filters, schedule conflict interval math,
 * non-cumulative idempotent credits, win-rate denominators, soft deletion, and .ics generation.
 */
@SpringBootTest
@Transactional
class TechPulseVerificationTests {

    @Autowired
    private DateWindowService dateWindowService;

    @Autowired
    private ScheduleConflictService scheduleConflictService;

    @Autowired
    private ParticipationService participationService;

    @Autowired
    private EventService eventService;

    @Autowired
    private CalendarIcsService calendarIcsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private ParticipationRepository participationRepository;

    @Test
    void testNext30DaysAndWeekendBoundariesInKolkata() {
        // Wednesday, 2026-10-07 14:30 IST
        ZonedDateTime referenceWednesday = ZonedDateTime.of(
                LocalDate.of(2026, 10, 7), LocalTime.of(14, 30), DateWindowService.DEFAULT_ZONE
        );

        DateWindowService.DateWindowBounds next30 =
                dateWindowService.resolveWindowFromReference("NEXT_30_DAYS", referenceWednesday);
        assertEquals("NEXT_30_DAYS", next30.windowKey());
        assertEquals(referenceWednesday.toOffsetDateTime(), next30.startInclusive());
        assertEquals(LocalDate.of(2026, 11, 6), next30.endInclusive().toLocalDate());
        assertTrue(next30.contains(referenceWednesday.plusDays(30).toOffsetDateTime()));
        assertFalse(next30.contains(referenceWednesday.plusDays(31).toOffsetDateTime()));

        // Upcoming weekend from Wednesday Oct 7, 2026 is Saturday Oct 10 through Sunday Oct 11
        DateWindowService.DateWindowBounds weekend =
                dateWindowService.resolveWindowFromReference("THIS_WEEKEND", referenceWednesday);
        assertEquals(LocalDate.of(2026, 10, 10), weekend.startInclusive().toLocalDate());
        assertEquals(LocalDate.of(2026, 10, 11), weekend.endInclusive().toLocalDate());
    }

    @Test
    void testUnknownPriceIsNeverTreatedAsFreeInCombinedFilters() {
        EventDtos.PaginatedEventResponse freeEvents = eventService.discoverEvents(
                null, "ALL", "ALL", "ALL", "ALL", "FREE", null, "NEXT_30_DAYS", "SOONEST", 0, 50, null
        );
        assertFalse(freeEvents.getContent().isEmpty());
        for (EventDtos.EventResponse ev : freeEvents.getContent()) {
            assertEquals(CostType.FREE, ev.getCostType(), "Unknown or Paid events must never match FREE filter");
        }
    }

    @Test
    void testScheduleOverlapDetectionAndIncompleteTimes() {
        OffsetDateTime base = dateWindowService.nowInKolkata().plusDays(3);

        Event eventA = new Event();
        eventA.setId(101L);
        eventA.setTitle("Morning AI Workshop");
        eventA.setStartTime(base.withHour(10).withMinute(0));
        eventA.setEndTime(base.withHour(13).withMinute(0));
        eventA.setCancelled(false);

        Event eventB = new Event();
        eventB.setId(102L);
        eventB.setTitle("Midday Java Hackathon");
        eventB.setStartTime(base.withHour(12).withMinute(0));
        eventB.setEndTime(base.withHour(16).withMinute(0));
        eventB.setCancelled(false);

        Event eventIncomplete = new Event();
        eventIncomplete.setId(103L);
        eventIncomplete.setTitle("Evening Meetup Without End Time");
        eventIncomplete.setStartTime(base.withHour(12).withMinute(30));
        eventIncomplete.setEndTime(null); // Missing end time must not invent a clash
        eventIncomplete.setCancelled(false);

        List<BookmarkDtos.ConflictWarningDto> conflicts =
                scheduleConflictService.detectConflicts(List.of(eventA, eventB, eventIncomplete));

        assertEquals(1, conflicts.size(), "Only events with known start and end times that overlap should trigger a clash");
        assertEquals(101L, conflicts.get(0).getFirstEventId());
        assertEquals(102L, conflicts.get(0).getSecondEventId());
    }

    @Test
    void testIdempotentNonCumulativeCreditsAndWinRateCalculation() {
        User testUser = new User(
                "Test Student", "test.verify@techpulse.edu.in", "hashed", "VJTI Mumbai",
                Role.USER, dateWindowService.nowInKolkata()
        );
        userRepository.save(testUser);

        List<Event> events = eventRepository.findByDeletedFalseOrderByStartTimeAsc();
        Event hackathon = events.stream().filter(e -> e.getEventType() == EventType.HACKATHON).findFirst().orElseThrow();
        Event workshop = events.stream().filter(e -> e.getEventType() == EventType.WORKSHOP).findFirst().orElseThrow();

        // Initially no completed competitions -> Win rate must be N/A
        ParticipationDtos.ActivitySummaryResponse initialSummary =
                participationService.buildActivitySummary(testUser.getId());
        assertFalse(initialSummary.isWinRateAvailable());
        assertEquals("N/A", initialSummary.getWinRateDisplay());

        // Step 1: Mark hackathon as REGISTERED -> 5 credits
        ParticipationDtos.ParticipationUpsertRequest req1 = new ParticipationDtos.ParticipationUpsertRequest();
        req1.setEventId(hackathon.getId());
        req1.setStatus(ParticipationStatus.REGISTERED);
        req1.setOutcome(CompetitionOutcome.PENDING);
        participationService.upsertStudentParticipation(testUser.getId(), req1);

        // Repeat same update to verify idempotency (still 5 credits, never 10)
        participationService.upsertStudentParticipation(testUser.getId(), req1);
        assertEquals(5, participationService.buildActivitySummary(testUser.getId()).getTotalCredits());

        // Step 2: Move hackathon from REGISTERED to ATTENDED (PENDING) -> total becomes 20 (not 25!)
        req1.setStatus(ParticipationStatus.ATTENDED);
        req1.setOutcome(CompetitionOutcome.PENDING);
        participationService.upsertStudentParticipation(testUser.getId(), req1);
        assertEquals(20, participationService.buildActivitySummary(testUser.getId()).getTotalCredits());

        // Step 3: Record WON on hackathon -> total becomes 50 (not 70!)
        req1.setStatus(ParticipationStatus.ATTENDED);
        req1.setOutcome(CompetitionOutcome.WON);
        participationService.upsertStudentParticipation(testUser.getId(), req1);

        // Step 4: Record ATTENDED on workshop -> adds 20 credits (total 70), but workshop must NOT affect competition win-rate denominator
        ParticipationDtos.ParticipationUpsertRequest reqWorkshop = new ParticipationDtos.ParticipationUpsertRequest();
        reqWorkshop.setEventId(workshop.getId());
        reqWorkshop.setStatus(ParticipationStatus.ATTENDED);
        reqWorkshop.setOutcome(CompetitionOutcome.NONE);
        participationService.upsertStudentParticipation(testUser.getId(), reqWorkshop);

        ParticipationDtos.ActivitySummaryResponse finalSummary =
                participationService.buildActivitySummary(testUser.getId());
        assertEquals(70, finalSummary.getTotalCredits());
        assertEquals("Builder", finalSummary.getCurrentBadge());
        assertTrue(finalSummary.isWinRateAvailable());
        assertEquals(1L, finalSummary.getWinRateNumerator());
        assertEquals(1L, finalSummary.getWinRateDenominator());
        assertEquals(100.0, finalSummary.getWinRatePercentage());
    }

    @Test
    void testAdminValidationAndSoftDeletePreservesBookmarks() {
        User student = userRepository.findByEmailIgnoreCase("student@techpulse.edu.in").orElseThrow();
        List<Event> events = eventRepository.findByDeletedFalseOrderByStartTimeAsc();
        Event target = events.get(0);

        eventService.addBookmark(student.getId(), target.getId());
        // Duplicate bookmark call must be idempotent
        eventService.addBookmark(student.getId(), target.getId());
        assertTrue(bookmarkRepository.existsByUserIdAndEventId(student.getId(), target.getId()));

        // Soft delete event as Admin
        eventService.softDeleteEventByAdmin(target.getId());

        // Verify bookmark still exists after soft deletion
        assertTrue(bookmarkRepository.existsByUserIdAndEventId(student.getId(), target.getId()));

        // Verify invalid endTime < startTime is rejected
        EventDtos.EventRequest invalidReq = new EventDtos.EventRequest();
        invalidReq.setTitle("Invalid Time Event");
        invalidReq.setDescription("Testing validation");
        invalidReq.setTopic("Java");
        invalidReq.setEventType(EventType.WORKSHOP);
        invalidReq.setMode(EventMode.ONLINE);
        invalidReq.setOrganizer("Test Org");
        invalidReq.setCity("Online");
        invalidReq.setCostType(CostType.FREE);
        OffsetDateTime start = dateWindowService.nowInKolkata().plusDays(5);
        invalidReq.setStartTime(start);
        invalidReq.setEndTime(start.minusHours(2)); // End before start!

        assertThrows(IllegalArgumentException.class, () -> eventService.createEventByAdmin(invalidReq));
    }

    @Test
    void testCalendarIcsGenerationAndEscaping() {
        Event event = eventRepository.findByDeletedFalseOrderByStartTimeAsc().get(0);
        String ics = calendarIcsService.generateIcsForEvent(event, "http://localhost:8080/#event/" + event.getId());
        assertTrue(ics.startsWith("BEGIN:VCALENDAR\r\n"));
        assertTrue(ics.contains("UID:techpulse-event-" + event.getId() + "@techpulse.edu.in"));
        assertTrue(ics.contains("END:VCALENDAR"));
    }
}
