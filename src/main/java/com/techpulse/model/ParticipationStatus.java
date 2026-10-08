package com.techpulse.model;

/**
 * Student participation lifecycle stage for an event.
 * Credits are non-cumulative totals per event:
 * SAVED = 0, REGISTERED = 5, ATTENDED = 20 (or 50 if competitive WON).
 */
public enum ParticipationStatus {
    SAVED,
    REGISTERED,
    ATTENDED
}
