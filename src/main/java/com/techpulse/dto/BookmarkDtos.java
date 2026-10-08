package com.techpulse.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Data Transfer Objects for saved bookmarks and schedule conflict detection.
 */
public class BookmarkDtos {

    public static class BookmarkItemDto {
        private Long bookmarkId;
        private OffsetDateTime savedAt;
        private boolean pastEvent;
        private String overlapCheckStatus; // "NO_CONFLICT", "CONFLICT_DETECTED", "CANNOT_CHECK_OVERLAP", "CANCELLED_SKIPPED"
        private String overlapCheckMessage;
        private EventDtos.EventResponse event;

        public Long getBookmarkId() {
            return bookmarkId;
        }

        public void setBookmarkId(Long bookmarkId) {
            this.bookmarkId = bookmarkId;
        }

        public OffsetDateTime getSavedAt() {
            return savedAt;
        }

        public void setSavedAt(OffsetDateTime savedAt) {
            this.savedAt = savedAt;
        }

        public boolean isPastEvent() {
            return pastEvent;
        }

        public void setPastEvent(boolean pastEvent) {
            this.pastEvent = pastEvent;
        }

        public String getOverlapCheckStatus() {
            return overlapCheckStatus;
        }

        public void setOverlapCheckStatus(String overlapCheckStatus) {
            this.overlapCheckStatus = overlapCheckStatus;
        }

        public String getOverlapCheckMessage() {
            return overlapCheckMessage;
        }

        public void setOverlapCheckMessage(String overlapCheckMessage) {
            this.overlapCheckMessage = overlapCheckMessage;
        }

        public EventDtos.EventResponse getEvent() {
            return event;
        }

        public void setEvent(EventDtos.EventResponse event) {
            this.event = event;
        }
    }

    public static class ConflictWarningDto {
        private Long firstEventId;
        private String firstEventTitle;
        private OffsetDateTime firstStart;
        private OffsetDateTime firstEnd;
        private Long secondEventId;
        private String secondEventTitle;
        private OffsetDateTime secondStart;
        private OffsetDateTime secondEnd;
        private String explanation;

        public Long getFirstEventId() {
            return firstEventId;
        }

        public void setFirstEventId(Long firstEventId) {
            this.firstEventId = firstEventId;
        }

        public String getFirstEventTitle() {
            return firstEventTitle;
        }

        public void setFirstEventTitle(String firstEventTitle) {
            this.firstEventTitle = firstEventTitle;
        }

        public OffsetDateTime getFirstStart() {
            return firstStart;
        }

        public void setFirstStart(OffsetDateTime firstStart) {
            this.firstStart = firstStart;
        }

        public OffsetDateTime getFirstEnd() {
            return firstEnd;
        }

        public void setFirstEnd(OffsetDateTime firstEnd) {
            this.firstEnd = firstEnd;
        }

        public Long getSecondEventId() {
            return secondEventId;
        }

        public void setSecondEventId(Long secondEventId) {
            this.secondEventId = secondEventId;
        }

        public String getSecondEventTitle() {
            return secondEventTitle;
        }

        public void setSecondEventTitle(String secondEventTitle) {
            this.secondEventTitle = secondEventTitle;
        }

        public OffsetDateTime getSecondStart() {
            return secondStart;
        }

        public void setSecondStart(OffsetDateTime secondStart) {
            this.secondStart = secondStart;
        }

        public OffsetDateTime getSecondEnd() {
            return secondEnd;
        }

        public void setSecondEnd(OffsetDateTime secondEnd) {
            this.secondEnd = secondEnd;
        }

        public String getExplanation() {
            return explanation;
        }

        public void setExplanation(String explanation) {
            this.explanation = explanation;
        }
    }

    public static class SavedEventsOverviewResponse {
        private List<BookmarkItemDto> upcomingBookmarks;
        private List<BookmarkItemDto> pastBookmarks;
        private List<ConflictWarningDto> conflicts;
        private int incompleteTimeCount;

        public List<BookmarkItemDto> getUpcomingBookmarks() {
            return upcomingBookmarks;
        }

        public void setUpcomingBookmarks(List<BookmarkItemDto> upcomingBookmarks) {
            this.upcomingBookmarks = upcomingBookmarks;
        }

        public List<BookmarkItemDto> getPastBookmarks() {
            return pastBookmarks;
        }

        public void setPastBookmarks(List<BookmarkItemDto> pastBookmarks) {
            this.pastBookmarks = pastBookmarks;
        }

        public List<ConflictWarningDto> getConflicts() {
            return conflicts;
        }

        public void setConflicts(List<ConflictWarningDto> conflicts) {
            this.conflicts = conflicts;
        }

        public int getIncompleteTimeCount() {
            return incompleteTimeCount;
        }

        public void setIncompleteTimeCount(int incompleteTimeCount) {
            this.incompleteTimeCount = incompleteTimeCount;
        }
    }
}
