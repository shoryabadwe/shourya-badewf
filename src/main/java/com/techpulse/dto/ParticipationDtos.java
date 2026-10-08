package com.techpulse.dto;

import com.techpulse.model.CompetitionOutcome;
import com.techpulse.model.EventType;
import com.techpulse.model.ParticipationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Data Transfer Objects for participation tracking, credits, badges, and competition win rates.
 */
public class ParticipationDtos {

    public static class ParticipationUpsertRequest {
        @NotNull(message = "Event ID is required")
        private Long eventId;

        @NotNull(message = "Participation status is required")
        private ParticipationStatus status;

        private CompetitionOutcome outcome = CompetitionOutcome.NONE;

        @Size(max = 400, message = "Note cannot exceed 400 characters")
        private String studentNote;

        public Long getEventId() {
            return eventId;
        }

        public void setEventId(Long eventId) {
            this.eventId = eventId;
        }

        public ParticipationStatus getStatus() {
            return status;
        }

        public void setStatus(ParticipationStatus status) {
            this.status = status;
        }

        public CompetitionOutcome getOutcome() {
            return outcome;
        }

        public void setOutcome(CompetitionOutcome outcome) {
            this.outcome = outcome;
        }

        public String getStudentNote() {
            return studentNote;
        }

        public void setStudentNote(String studentNote) {
            this.studentNote = studentNote;
        }
    }

    public static class AdminVerifyParticipationRequest {
        @NotNull(message = "Participation status is required")
        private ParticipationStatus status;

        @NotNull(message = "Outcome is required")
        private CompetitionOutcome outcome;

        private boolean verifiedByAdmin;

        @Size(max = 400, message = "Verification note cannot exceed 400 characters")
        private String adminVerificationNote;

        public ParticipationStatus getStatus() {
            return status;
        }

        public void setStatus(ParticipationStatus status) {
            this.status = status;
        }

        public CompetitionOutcome getOutcome() {
            return outcome;
        }

        public void setOutcome(CompetitionOutcome outcome) {
            this.outcome = outcome;
        }

        public boolean isVerifiedByAdmin() {
            return verifiedByAdmin;
        }

        public void setVerifiedByAdmin(boolean verifiedByAdmin) {
            this.verifiedByAdmin = verifiedByAdmin;
        }

        public String getAdminVerificationNote() {
            return adminVerificationNote;
        }

        public void setAdminVerificationNote(String adminVerificationNote) {
            this.adminVerificationNote = adminVerificationNote;
        }
    }

    public static class ParticipationRecordDto {
        private Long id;
        private Long userId;
        private String userFullName;
        private String userEmail;
        private Long eventId;
        private String eventTitle;
        private EventType eventType;
        private String eventTopic;
        private OffsetDateTime eventStartTime;
        private boolean eventDeleted;
        private boolean competitiveEvent;
        private ParticipationStatus status;
        private CompetitionOutcome outcome;
        private int credits;
        private boolean selfReported;
        private boolean verifiedByAdmin;
        private String verificationLabel;
        private String studentNote;
        private String adminVerificationNote;
        private OffsetDateTime updatedAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getUserFullName() {
            return userFullName;
        }

        public void setUserFullName(String userFullName) {
            this.userFullName = userFullName;
        }

        public String getUserEmail() {
            return userEmail;
        }

        public void setUserEmail(String userEmail) {
            this.userEmail = userEmail;
        }

        public Long getEventId() {
            return eventId;
        }

        public void setEventId(Long eventId) {
            this.eventId = eventId;
        }

        public String getEventTitle() {
            return eventTitle;
        }

        public void setEventTitle(String eventTitle) {
            this.eventTitle = eventTitle;
        }

        public EventType getEventType() {
            return eventType;
        }

        public void setEventType(EventType eventType) {
            this.eventType = eventType;
        }

        public String getEventTopic() {
            return eventTopic;
        }

        public void setEventTopic(String eventTopic) {
            this.eventTopic = eventTopic;
        }

        public OffsetDateTime getEventStartTime() {
            return eventStartTime;
        }

        public void setEventStartTime(OffsetDateTime eventStartTime) {
            this.eventStartTime = eventStartTime;
        }

        public boolean isEventDeleted() {
            return eventDeleted;
        }

        public void setEventDeleted(boolean eventDeleted) {
            this.eventDeleted = eventDeleted;
        }

        public boolean isCompetitiveEvent() {
            return competitiveEvent;
        }

        public void setCompetitiveEvent(boolean competitiveEvent) {
            this.competitiveEvent = competitiveEvent;
        }

        public ParticipationStatus getStatus() {
            return status;
        }

        public void setStatus(ParticipationStatus status) {
            this.status = status;
        }

        public CompetitionOutcome getOutcome() {
            return outcome;
        }

        public void setOutcome(CompetitionOutcome outcome) {
            this.outcome = outcome;
        }

        public int getCredits() {
            return credits;
        }

        public void setCredits(int credits) {
            this.credits = credits;
        }

        public boolean isSelfReported() {
            return selfReported;
        }

        public void setSelfReported(boolean selfReported) {
            this.selfReported = selfReported;
        }

        public boolean isVerifiedByAdmin() {
            return verifiedByAdmin;
        }

        public void setVerifiedByAdmin(boolean verifiedByAdmin) {
            this.verifiedByAdmin = verifiedByAdmin;
        }

        public String getVerificationLabel() {
            return verificationLabel;
        }

        public void setVerificationLabel(String verificationLabel) {
            this.verificationLabel = verificationLabel;
        }

        public String getStudentNote() {
            return studentNote;
        }

        public void setStudentNote(String studentNote) {
            this.studentNote = studentNote;
        }

        public String getAdminVerificationNote() {
            return adminVerificationNote;
        }

        public void setAdminVerificationNote(String adminVerificationNote) {
            this.adminVerificationNote = adminVerificationNote;
        }

        public OffsetDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(OffsetDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }
    }

    public static class ActivitySummaryResponse {
        private long eventsSaved;
        private long registeredEventsCount;
        private long attendedEventsCount;
        private long competitionsParticipatedCount;
        private long winsCount;
        private int totalCredits;
        private long attendedLast30DaysCount;

        // Win rate metrics
        private boolean winRateAvailable;
        private Double winRatePercentage;
        private String winRateDisplay;
        private long winRateNumerator;
        private long winRateDenominator;
        private String winRateExplanation;

        // Engagement Badge metrics
        private String currentBadge;
        private String badgeTierRange;
        private String nextBadge;
        private int creditsToNextBadge;
        private int badgeProgressPercent;
        private String badgeDisclaimer;

        private List<ParticipationRecordDto> activityHistory;

        public long getEventsSaved() {
            return eventsSaved;
        }

        public void setEventsSaved(long eventsSaved) {
            this.eventsSaved = eventsSaved;
        }

        public long getRegisteredEventsCount() {
            return registeredEventsCount;
        }

        public void setRegisteredEventsCount(long registeredEventsCount) {
            this.registeredEventsCount = registeredEventsCount;
        }

        public long getAttendedEventsCount() {
            return attendedEventsCount;
        }

        public void setAttendedEventsCount(long attendedEventsCount) {
            this.attendedEventsCount = attendedEventsCount;
        }

        public long getCompetitionsParticipatedCount() {
            return competitionsParticipatedCount;
        }

        public void setCompetitionsParticipatedCount(long competitionsParticipatedCount) {
            this.competitionsParticipatedCount = competitionsParticipatedCount;
        }

        public long getWinsCount() {
            return winsCount;
        }

        public void setWinsCount(long winsCount) {
            this.winsCount = winsCount;
        }

        public int getTotalCredits() {
            return totalCredits;
        }

        public void setTotalCredits(int totalCredits) {
            this.totalCredits = totalCredits;
        }

        public long getAttendedLast30DaysCount() {
            return attendedLast30DaysCount;
        }

        public void setAttendedLast30DaysCount(long attendedLast30DaysCount) {
            this.attendedLast30DaysCount = attendedLast30DaysCount;
        }

        public boolean isWinRateAvailable() {
            return winRateAvailable;
        }

        public void setWinRateAvailable(boolean winRateAvailable) {
            this.winRateAvailable = winRateAvailable;
        }

        public Double getWinRatePercentage() {
            return winRatePercentage;
        }

        public void setWinRatePercentage(Double winRatePercentage) {
            this.winRatePercentage = winRatePercentage;
        }

        public String getWinRateDisplay() {
            return winRateDisplay;
        }

        public void setWinRateDisplay(String winRateDisplay) {
            this.winRateDisplay = winRateDisplay;
        }

        public long getWinRateNumerator() {
            return winRateNumerator;
        }

        public void setWinRateNumerator(long winRateNumerator) {
            this.winRateNumerator = winRateNumerator;
        }

        public long getWinRateDenominator() {
            return winRateDenominator;
        }

        public void setWinRateDenominator(long winRateDenominator) {
            this.winRateDenominator = winRateDenominator;
        }

        public String getWinRateExplanation() {
            return winRateExplanation;
        }

        public void setWinRateExplanation(String winRateExplanation) {
            this.winRateExplanation = winRateExplanation;
        }

        public String getCurrentBadge() {
            return currentBadge;
        }

        public void setCurrentBadge(String currentBadge) {
            this.currentBadge = currentBadge;
        }

        public String getBadgeTierRange() {
            return badgeTierRange;
        }

        public void setBadgeTierRange(String badgeTierRange) {
            this.badgeTierRange = badgeTierRange;
        }

        public String getNextBadge() {
            return nextBadge;
        }

        public void setNextBadge(String nextBadge) {
            this.nextBadge = nextBadge;
        }

        public int getCreditsToNextBadge() {
            return creditsToNextBadge;
        }

        public void setCreditsToNextBadge(int creditsToNextBadge) {
            this.creditsToNextBadge = creditsToNextBadge;
        }

        public int getBadgeProgressPercent() {
            return badgeProgressPercent;
        }

        public void setBadgeProgressPercent(int badgeProgressPercent) {
            this.badgeProgressPercent = badgeProgressPercent;
        }

        public String getBadgeDisclaimer() {
            return badgeDisclaimer;
        }

        public void setBadgeDisclaimer(String badgeDisclaimer) {
            this.badgeDisclaimer = badgeDisclaimer;
        }

        public List<ParticipationRecordDto> getActivityHistory() {
            return activityHistory;
        }

        public void setActivityHistory(List<ParticipationRecordDto> activityHistory) {
            this.activityHistory = activityHistory;
        }
    }
}
