package com.techpulse.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Represents a technology event (hackathon, workshop, conference, webinar, competition).
 * Uses BigDecimal for monetary price and OffsetDateTime for timezone-aware timestamps.
 * Supports soft deletion (deleted = true) so student bookmarks and participation history remain intact.
 */
@Entity
@Table(name = "events", indexes = {
        @Index(name = "idx_events_start_time", columnList = "startTime"),
        @Index(name = "idx_events_source_ext", columnList = "sourceName, externalEventId")
})
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 220)
    private String title;

    @Column(nullable = false, length = 3000)
    private String description;

    @Column(nullable = false, length = 80)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventMode mode;

    @Column(nullable = false, length = 160)
    private String organizer;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(length = 300)
    private String venueOrPlatform;

    @Column(nullable = false)
    private OffsetDateTime startTime;

    @Column
    private OffsetDateTime endTime;

    @Column
    private OffsetDateTime registrationDeadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private CostType costType = CostType.NOT_PROVIDED;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 12)
    private String currency = "INR";

    @Column(length = 250)
    private String eligibility;

    @Column(length = 500)
    private String registrationUrl;

    @Column(length = 500)
    private String officialSourceUrl;

    @Column(nullable = false, length = 100)
    private String sourceName;

    @Column(length = 160)
    private String externalEventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private DataSourceOrigin dataOrigin = DataSourceOrigin.MANUAL_ADMIN;

    @Column
    private OffsetDateTime lastFetchedAt;

    @Column(nullable = false)
    private boolean cancelled = false;

    @Column(nullable = false)
    private boolean registrationClosed = false;

    /**
     * Soft deletion flag: when an admin deletes an event, it is hidden from public Explore discovery
     * while preserving foreign-key references in student Bookmarks and Participation records.
     */
    @Column(nullable = false)
    private boolean deleted = false;

    /**
     * Protects administrator edits from being overwritten during automated live-source refreshes.
     */
    @Column(nullable = false)
    private boolean manuallyEdited = false;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    public Event() {
    }

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

    public String getExternalEventId() {
        return externalEventId;
    }

    public void setExternalEventId(String externalEventId) {
        this.externalEventId = externalEventId;
    }

    public DataSourceOrigin getDataOrigin() {
        return dataOrigin;
    }

    public void setDataOrigin(DataSourceOrigin dataOrigin) {
        this.dataOrigin = dataOrigin;
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

    public boolean isManuallyEdited() {
        return manuallyEdited;
    }

    public void setManuallyEdited(boolean manuallyEdited) {
        this.manuallyEdited = manuallyEdited;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
