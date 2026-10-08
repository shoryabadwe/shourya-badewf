package com.techpulse.model;

/**
 * Cost classification of an event.
 * Unknown prices are explicitly represented as NOT_PROVIDED and never treated as FREE.
 */
public enum CostType {
    FREE,
    PAID,
    NOT_PROVIDED
}
