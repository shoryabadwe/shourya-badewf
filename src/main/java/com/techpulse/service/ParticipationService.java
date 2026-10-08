package com.techpulse.service;

import com.techpulse.dto.ParticipationDtos;
import com.techpulse.model.*;
import com.techpulse.repository.BookmarkRepository;
import com.techpulse.repository.EventRepository;
import com.techpulse.repository.ParticipationRepository;
import com.techpulse.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Handles student participation tracking, non-cumulative credit calculation,
 * competition win-rate calculation, and engagement badge progression.
 *
 * Credit Rules (total per event, never cumulative across repeated clicks):
 * - SAVED:      0 credits
 * - REGISTERED: 5 credits
 * - ATTENDED:   20 total credits (or 50 total credits if event is HACKATHON/COMPETITION and outcome is WON)
 *
 * Win Rate Formula:
 *   wins ÷ completed competitions (HACKATHON or COMPETITION with status ATTENDED and outcome WON or NOT_WON) × 100
 *   Excludes workshops, conferences, webinars, registration-only records, and PENDING outcomes.
 */
@Service
public class ParticipationService {

    private final ParticipationRepository participationRepository;
    private final BookmarkRepository bookmarkRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final DateWindowService dateWindowService;

    public ParticipationService(
            ParticipationRepository participationRepository,
            BookmarkRepository bookmarkRepository,
            EventRepository eventRepository,
            UserRepository userRepository,
            DateWindowService dateWindowService
    ) {
        this.participationRepository = participationRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.dateWindowService = dateWindowService;
    }

    /**
     * Calculates non-cumulative total credits for a single event participation record.
     */
    public int calculateCreditsForRecord(EventType eventType, ParticipationStatus status, CompetitionOutcome outcome) {
        if (status == null || status == ParticipationStatus.SAVED) {
            return 0;
        }
        if (status == ParticipationStatus.REGISTERED) {
            return 5;
        }
        if (status == ParticipationStatus.ATTENDED) {
            boolean isCompetitive = eventType != null && eventType.isCompetitive();
            if (isCompetitive && outcome == CompetitionOutcome.WON) {
                return 50;
            }
            return 20;
        }
        return 0;
    }

    /**
     * Normalizes competition outcome based on event type and participation status.
     * Non-competitive events (workshops, webinars, conferences) cannot record competition outcomes.
     * Outcome WON or NOT_WON requires status ATTENDED.
     */
    public CompetitionOutcome normalizeOutcome(EventType eventType, ParticipationStatus status, CompetitionOutcome requestedOutcome) {
        if (eventType == null || !eventType.isCompetitive()) {
            return CompetitionOutcome.NONE;
        }
        if (status != ParticipationStatus.ATTENDED) {
            if (requestedOutcome == CompetitionOutcome.WON || requestedOutcome == CompetitionOutcome.NOT_WON) {
                throw new IllegalArgumentException("Competition result (Won / Not Won) can only be recorded when participation status is Attended.");
            }
            return requestedOutcome == CompetitionOutcome.PENDING ? CompetitionOutcome.PENDING : CompetitionOutcome.NONE;
        }
        if (requestedOutcome == null || requestedOutcome == CompetitionOutcome.NONE) {
            return CompetitionOutcome.PENDING;
        }
        return requestedOutcome;
    }

    @Transactional
    public ParticipationDtos.ParticipationRecordDto upsertStudentParticipation(Long userId, ParticipationDtos.ParticipationUpsertRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new NoSuchElementException("Event not found with ID: " + request.getEventId()));

        ParticipationStatus status = request.getStatus();
        CompetitionOutcome normalizedOutcome = normalizeOutcome(event.getEventType(), status, request.getOutcome());
        int credits = calculateCreditsForRecord(event.getEventType(), status, normalizedOutcome);

        Participation record = participationRepository.findByUserIdAndEventId(userId, event.getId())
                .orElseGet(() -> {
                    Participation p = new Participation();
                    p.setUser(user);
                    p.setEvent(event);
                    return p;
                });

        // If status or outcome changed after admin verification, reset verification flag so self-reported changes require re-verification
        boolean statusOrOutcomeChanged = record.getId() != null &&
                (record.getStatus() != status || record.getOutcome() != normalizedOutcome);

        record.setStatus(status);
        record.setOutcome(normalizedOutcome);
        record.setCredits(credits);
        record.setSelfReported(true);
        if (statusOrOutcomeChanged) {
            record.setVerifiedByAdmin(false);
        }
        if (request.getStudentNote() != null) {
            record.setStudentNote(request.getStudentNote().trim());
        }
        record.setUpdatedAt(dateWindowService.nowInKolkata());

        Participation saved = participationRepository.save(record);
        return toDto(saved);
    }

    @Transactional
    public ParticipationDtos.ParticipationRecordDto verifyOrCorrectByAdmin(
            Long participationId, ParticipationDtos.AdminVerifyParticipationRequest request) {
        Participation record = participationRepository.findById(participationId)
                .orElseThrow(() -> new NoSuchElementException("Participation record not found with ID: " + participationId));

        Event event = record.getEvent();
        ParticipationStatus status = request.getStatus();
        CompetitionOutcome normalizedOutcome = normalizeOutcome(event.getEventType(), status, request.getOutcome());
        int credits = calculateCreditsForRecord(event.getEventType(), status, normalizedOutcome);

        record.setStatus(status);
        record.setOutcome(normalizedOutcome);
        record.setCredits(credits);
        record.setVerifiedByAdmin(request.isVerifiedByAdmin());
        record.setAdminVerificationNote(request.getAdminVerificationNote() != null
                ? request.getAdminVerificationNote().trim()
                : null);
        record.setUpdatedAt(dateWindowService.nowInKolkata());

        Participation saved = participationRepository.save(record);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public ParticipationDtos.ActivitySummaryResponse buildActivitySummary(Long userId) {
        List<Participation> records = participationRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        long savedBookmarksCount = bookmarkRepository.countByUserId(userId);

        long registeredCount = 0;
        long attendedCount = 0;
        long competitionsParticipatedCount = 0;
        long winsCount = 0;
        long completedCompetitionsWithOutcome = 0;
        int totalCredits = 0;
        long attendedLast30Days = 0;

        OffsetDateTime now = dateWindowService.nowInKolkata();
        OffsetDateTime thirtyDaysAgo = now.minusDays(30);

        for (Participation p : records) {
            Event event = p.getEvent();
            EventType type = event.getEventType();
            ParticipationStatus status = p.getStatus();
            CompetitionOutcome outcome = p.getOutcome();

            // Recalculate dynamically from current status and outcome to guarantee accuracy
            int recordCredits = calculateCreditsForRecord(type, status, outcome);
            totalCredits += recordCredits;

            if (status == ParticipationStatus.REGISTERED) {
                registeredCount++;
            } else if (status == ParticipationStatus.ATTENDED) {
                attendedCount++;
                OffsetDateTime eventTime = event.getStartTime();
                if (eventTime != null && !eventTime.isBefore(thirtyDaysAgo) && !eventTime.isAfter(now.plusDays(1))) {
                    attendedLast30Days++;
                }
                if (type != null && type.isCompetitive()) {
                    competitionsParticipatedCount++;
                    if (outcome == CompetitionOutcome.WON) {
                        winsCount++;
                        completedCompetitionsWithOutcome++;
                    } else if (outcome == CompetitionOutcome.NOT_WON) {
                        completedCompetitionsWithOutcome++;
                    }
                }
            }
        }

        ParticipationDtos.ActivitySummaryResponse summary = new ParticipationDtos.ActivitySummaryResponse();
        summary.setEventsSaved(savedBookmarksCount);
        summary.setRegisteredEventsCount(registeredCount);
        summary.setAttendedEventsCount(attendedCount);
        summary.setCompetitionsParticipatedCount(competitionsParticipatedCount);
        summary.setWinsCount(winsCount);
        summary.setTotalCredits(totalCredits);
        summary.setAttendedLast30DaysCount(attendedLast30Days);

        // Win rate calculation: wins / completed competitions with WON or NOT_WON * 100
        summary.setWinRateNumerator(winsCount);
        summary.setWinRateDenominator(completedCompetitionsWithOutcome);
        if (completedCompetitionsWithOutcome == 0) {
            summary.setWinRateAvailable(false);
            summary.setWinRatePercentage(null);
            summary.setWinRateDisplay("N/A");
            summary.setWinRateExplanation(
                    "No completed hackathons or competitions with a recorded Won/Not Won outcome yet. " +
                    "(Workshops, conferences, webinars, registrations, and pending outcomes are excluded.)"
            );
        } else {
            double pct = ((double) winsCount / (double) completedCompetitionsWithOutcome) * 100.0;
            double rounded = Math.round(pct * 10.0) / 10.0;
            summary.setWinRateAvailable(true);
            summary.setWinRatePercentage(rounded);
            summary.setWinRateDisplay(rounded + "% (" + winsCount + " / " + completedCompetitionsWithOutcome + ")");
            summary.setWinRateExplanation(String.format(
                    "%d win(s) out of %d completed hackathon/competition event(s) with a recorded Won/Not Won outcome.",
                    winsCount, completedCompetitionsWithOutcome
            ));
        }

        // Engagement Badge calculation:
        // Explorer: 0-49, Builder: 50-149, Challenger: 150-299, Champion: 300+
        applyBadgeMetrics(summary, totalCredits);

        summary.setActivityHistory(records.stream().map(this::toDto).collect(Collectors.toList()));
        return summary;
    }

    private void applyBadgeMetrics(ParticipationDtos.ActivitySummaryResponse summary, int credits) {
        summary.setBadgeDisclaimer("Application engagement badge based on self-reported and admin-verified activity — not an official academic certification.");
        if (credits < 50) {
            summary.setCurrentBadge("Explorer");
            summary.setBadgeTierRange("0 – 49 credits");
            summary.setNextBadge("Builder");
            summary.setCreditsToNextBadge(50 - credits);
            summary.setBadgeProgressPercent((int) Math.min(100, Math.round((credits / 50.0) * 100)));
        } else if (credits < 150) {
            summary.setCurrentBadge("Builder");
            summary.setBadgeTierRange("50 – 149 credits");
            summary.setNextBadge("Challenger");
            summary.setCreditsToNextBadge(150 - credits);
            summary.setBadgeProgressPercent((int) Math.min(100, Math.round(((credits - 50) / 100.0) * 100)));
        } else if (credits < 300) {
            summary.setCurrentBadge("Challenger");
            summary.setBadgeTierRange("150 – 299 credits");
            summary.setNextBadge("Champion");
            summary.setCreditsToNextBadge(300 - credits);
            summary.setBadgeProgressPercent((int) Math.min(100, Math.round(((credits - 150) / 150.0) * 100)));
        } else {
            summary.setCurrentBadge("Champion");
            summary.setBadgeTierRange("300+ credits");
            summary.setNextBadge("Max Tier Reached");
            summary.setCreditsToNextBadge(0);
            summary.setBadgeProgressPercent(100);
        }
    }

    @Transactional(readOnly = true)
    public List<ParticipationDtos.ParticipationRecordDto> listAllParticipationsForAdmin() {
        return participationRepository.findAllByOrderByUpdatedAtDesc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ParticipationDtos.ParticipationRecordDto toDto(Participation p) {
        ParticipationDtos.ParticipationRecordDto dto = new ParticipationDtos.ParticipationRecordDto();
        dto.setId(p.getId());
        dto.setUserId(p.getUser().getId());
        dto.setUserFullName(p.getUser().getFullName());
        dto.setUserEmail(p.getUser().getEmail());
        dto.setEventId(p.getEvent().getId());
        dto.setEventTitle(p.getEvent().getTitle());
        dto.setEventType(p.getEvent().getEventType());
        dto.setEventTopic(p.getEvent().getTopic());
        dto.setEventStartTime(p.getEvent().getStartTime());
        dto.setEventDeleted(p.getEvent().isDeleted());
        dto.setCompetitiveEvent(p.getEvent().getEventType() != null && p.getEvent().getEventType().isCompetitive());
        dto.setStatus(p.getStatus());
        dto.setOutcome(p.getOutcome());
        dto.setCredits(calculateCreditsForRecord(p.getEvent().getEventType(), p.getStatus(), p.getOutcome()));
        dto.setSelfReported(p.isSelfReported());
        dto.setVerifiedByAdmin(p.isVerifiedByAdmin());
        dto.setVerificationLabel(p.isVerifiedByAdmin() ? "Admin Verified" : "Self-Reported");
        dto.setStudentNote(p.getStudentNote());
        dto.setAdminVerificationNote(p.getAdminVerificationNote());
        dto.setUpdatedAt(p.getUpdatedAt());
        return dto;
    }
}
