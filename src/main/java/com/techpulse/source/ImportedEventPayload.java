package com.techpulse.source;

import com.techpulse.model.CostType;
import com.techpulse.model.EventMode;
import com.techpulse.model.EventType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Normalized representation of an event imported from an external source adapter.
 */
public class ImportedEventPayload {
    private String externalEventId;
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
    private CostType costType = CostType.NOT_PROVIDED;
    private BigDecimal price;
    private String currency = "INR";
    private String eligibility;
    private String registrationUrl;
    private String officialSourceUrl;
    private boolean cancelled = false;
    private boolean registrationClosed = false;

    public String getExternalEventId() {
        return externalEventId;
    }

    public void setExternalEventId(String externalEventId) {
        this.externalEventId = externalEventId;
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
