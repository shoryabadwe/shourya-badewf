package com.techpulse.dto;

import com.techpulse.model.CostType;
import com.techpulse.model.DataSourceOrigin;
import com.techpulse.model.EventMode;
import com.techpulse.model.EventType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Data Transfer Objects for Event creation, update, discovery cards, and detailed views.
 */
public class EventDtos {

    public static class EventRequest {
        @NotBlank(message = "Event title is required")
        @Size(max = 220, message = "Title cannot exceed 220 characters")
        private String title;

        @NotBlank(message = "Description is required")
        @Size(max = 3000, message = "Description cannot exceed 3000 characters")
        private String description;

        @NotBlank(message = "Topic is required")
        @Size(max = 80, message = "Topic cannot exceed 80 characters")
        private String topic;

        @NotNull(message = "Event type is required")
        private EventType eventType;

        @NotNull(message = "Event mode is required")
        private EventMode mode;

        @NotBlank(message = "Organizer is required")
        @Size(max = 160, message = "Organizer cannot exceed 160 characters")
        private String organizer;

        @NotBlank(message = "City or 'Online' is required")
        @Size(max = 100, message = "City cannot exceed 100 characters")
        private String city;

        @Size(max = 300, message = "Venue or platform info cannot exceed 300 characters")
        private String venueOrPlatform;

        @NotNull(message = "Start date and time are required")
        private OffsetDateTime startTime;

        private OffsetDateTime endTime;

        private OffsetDateTime registrationDeadline;

        @NotNull(message = "Cost type is required")
        private CostType costType;

        @DecimalMin(value = "0.0", inclusive = true, message = "Price cannot be negative")
        private BigDecimal price;

        @Size(max = 12)
        private String currency = "INR";

        @Size(max = 250)
        private String eligibility;

        @Size(max = 500)
        private String registrationUrl;

        @Size(max = 500)
        private String officialSourceUrl;

        private boolean cancelled = false;

        private boolean registrationClosed = false;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public EventType getEventType() {
            return eventType;
        }

        public void setEventType(EventType eventType) {
            this.eventType = eventType;
        }

        public EventMode getMode() {
            return mode;
        }

        public void setMode(EventMode mode) {
            this.mode = mode;
        }

        public String getOrganizer() {
            return organizer;
        }

        public void setOrganizer(String organizer) {
            this.organizer = organizer;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getVenueOrPlatform() {
            return venueOrPlatform;
        }

        public void setVenueOrPlatform(String venueOrPlatform) {
            this.venueOrPlatform = venueOrPlatform;
        }

        public OffsetDateTime getStartTime() {
            return startTime;
        }

        public void setStartTime(OffsetDateTime startTime) {
            this.startTime = startTime;
        }

        public OffsetDateTime getEndTime() {
            return endTime;
        }

        public void setEndTime(OffsetDateTime endTime) {
            this.endTime = endTime;
        }

        public OffsetDateTime getRegistrationDeadline() {
            return registrationDeadline;
        }

        public void setRegistrationDeadline(OffsetDateTime registrationDeadline) {
            this.registrationDeadline = registrationDeadline;
        }

        public CostType getCostType() {
            return costType;
        }

        public void setCostType(CostType costType) {
            this.costType = costType;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getEligibility() {
            return eligibility;
        }

        public void setEligibility(String eligibility) {
            this.eligibility = eligibility;
        }

        public String getRegistrationUrl() {
            return registrationUrl;
        }

        public void setRegistrationUrl(String registrationUrl) {
            this.registrationUrl = registrationUrl;
        }

        public String getOfficialSourceUrl() {
            return officialSourceUrl;
        }

        public void setOfficialSourceUrl(String officialSourceUrl) {
            this.officialSourceUrl = officialSourceUrl;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }

        public boolean isRegistrationClosed() {
            return registrationClosed;
        }

        public void setRegistrationClosed(boolean registrationClosed) {
            this.registrationClosed = registrationClosed;
        }
    }

    public static class EventResponse {
        private Long id;
        private String title;
        private String description;
        private String topic;
        private EventType eventType;
        private EventMode mode;
        private String organizer;
        private String city;
        private String venueOrPlatform;
        private OffsetDateTime startTime;
        private OffsetDateTime endTime;
        private OffsetDateTime registrationDeadline;
        private CostType costType;
        private BigDecimal price;
        private String currency;
        private String priceDisplay;
        private String eligibility;
        private String registrationUrl;
        private String officialSourceUrl;
        private String sourceName;
        private DataSourceOrigin dataOrigin;
        private String dataOriginLabel;
        private OffsetDateTime lastFetchedAt;
        private boolean cancelled;
        private boolean registrationClosed;
        private boolean deleted;
        private String countdownLabel;
        private boolean bookmarked;
        private String participationStatus;
        private String competitionOutcome;
        private boolean participationVerified;
        private OffsetDateTime createdAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public EventType getEventType() {
            return eventType;
        }

        public void setEventType(EventType eventType) {
            this.eventType = eventType;
        }

        public EventMode getMode() {
            return mode;
        }

        public void setMode(EventMode mode) {
            this.mode = mode;
        }

        public String getOrganizer() {
            return organizer;
        }

        public void setOrganizer(String organizer) {
            this.organizer = organizer;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getVenueOrPlatform() {
            return venueOrPlatform;
        }

        public void setVenueOrPlatform(String venueOrPlatform) {
            this.venueOrPlatform = venueOrPlatform;
        }

        public OffsetDateTime getStartTime() {
            return startTime;
        }

        public void setStartTime(OffsetDateTime startTime) {
            this.startTime = startTime;
        }

        public OffsetDateTime getEndTime() {
            return endTime;
        }

        public void setEndTime(OffsetDateTime endTime) {
            this.endTime = endTime;
        }

        public OffsetDateTime getRegistrationDeadline() {
            return registrationDeadline;
        }

        public void setRegistrationDeadline(OffsetDateTime registrationDeadline) {
            this.registrationDeadline = registrationDeadline;
        }

        public CostType getCostType() {
            return costType;
        }

        public void setCostType(CostType costType) {
            this.costType = costType;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getPriceDisplay() {
            return priceDisplay;
        }

        public void setPriceDisplay(String priceDisplay) {
            this.priceDisplay = priceDisplay;
        }

        public String getEligibility() {
            return eligibility;
        }

        public void setEligibility(String eligibility) {
            this.eligibility = eligibility;
        }

        public String getRegistrationUrl() {
            return registrationUrl;
        }

        public void setRegistrationUrl(String registrationUrl) {
            this.registrationUrl = registrationUrl;
        }

        public String getOfficialSourceUrl() {
            return officialSourceUrl;
        }

        public void setOfficialSourceUrl(String officialSourceUrl) {
            this.officialSourceUrl = officialSourceUrl;
        }

        public String getSourceName() {
            return sourceName;
        }

        public void setSourceName(String sourceName) {
            this.sourceName = sourceName;
        }

        public DataSourceOrigin getDataOrigin() {
            return dataOrigin;
        }

        public void setDataOrigin(DataSourceOrigin dataOrigin) {
            this.dataOrigin = dataOrigin;
        }

        public String getDataOriginLabel() {
            return dataOriginLabel;
        }

        public void setDataOriginLabel(String dataOriginLabel) {
            this.dataOriginLabel = dataOriginLabel;
        }

        public OffsetDateTime getLastFetchedAt() {
            return lastFetchedAt;
        }

        public void setLastFetchedAt(OffsetDateTime lastFetchedAt) {
            this.lastFetchedAt = lastFetchedAt;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }

        public boolean isRegistrationClosed() {
            return registrationClosed;
        }

        public void setRegistrationClosed(boolean registrationClosed) {
            this.registrationClosed = registrationClosed;
        }

        public boolean isDeleted() {
            return deleted;
        }

        public void setDeleted(boolean deleted) {
            this.deleted = deleted;
        }

        public String getCountdownLabel() {
            return countdownLabel;
        }

        public void setCountdownLabel(String countdownLabel) {
            this.countdownLabel = countdownLabel;
        }

        public boolean isBookmarked() {
            return bookmarked;
        }

        public void setBookmarked(boolean bookmarked) {
            this.bookmarked = bookmarked;
        }

        public String getParticipationStatus() {
            return participationStatus;
        }

        public void setParticipationStatus(String participationStatus) {
            this.participationStatus = participationStatus;
        }

        public String getCompetitionOutcome() {
            return competitionOutcome;
        }

        public void setCompetitionOutcome(String competitionOutcome) {
            this.competitionOutcome = competitionOutcome;
        }

        public boolean isParticipationVerified() {
            return participationVerified;
        }

        public void setParticipationVerified(boolean participationVerified) {
            this.participationVerified = participationVerified;
        }

        public OffsetDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(OffsetDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }

    public static class PaginatedEventResponse {
        private List<EventResponse> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private String activeDateWindow;
        private OffsetDateTime windowStart;
        private OffsetDateTime windowEnd;
        private String timezone;

        public List<EventResponse> getContent() {
            return content;
        }

        public void setContent(List<EventResponse> content) {
            this.content = content;
        }

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public long getTotalElements() {
            return totalElements;
        }

        public void setTotalElements(long totalElements) {
            this.totalElements = totalElements;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }

        public String getActiveDateWindow() {
            return activeDateWindow;
        }

        public void setActiveDateWindow(String activeDateWindow) {
            this.activeDateWindow = activeDateWindow;
        }

        public OffsetDateTime getWindowStart() {
            return windowStart;
        }

        public void setWindowStart(OffsetDateTime windowStart) {
            this.windowStart = windowStart;
        }

        public OffsetDateTime getWindowEnd() {
            return windowEnd;
        }

        public void setWindowEnd(OffsetDateTime windowEnd) {
            this.windowEnd = windowEnd;
        }

        public String getTimezone() {
            return timezone;
        }

        public void setTimezone(String timezone) {
            this.timezone = timezone;
        }
    }
}
