package com.techpulse.model;

/**
 * Tracks how an event entered the database so the UI never labels demo events as live.
 */
public enum DataSourceOrigin {
    DEMO_SEED,
    MANUAL_ADMIN,
    LIVE_IMPORT
}
