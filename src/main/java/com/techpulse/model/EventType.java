package com.techpulse.model;

/**
 * Technology event types.
 * Note: Only HACKATHON and COMPETITION are eligible for competition win-rate calculations.
 */
public enum EventType {
    HACKATHON,
    WORKSHOP,
    CONFERENCE,
    WEBINAR,
    COMPETITION;

    public boolean isCompetitive() {
        return this == HACKATHON || this == COMPETITION;
    }
}
