package com.techpulse.source;

import java.util.List;

/**
 * Backend adapter interface so supported live event sources can be added
 * without rewriting controllers or persistence logic.
 */
public interface EventSourceAdapter {

    /**
     * Human-readable name of the external source (e.g. "KonfHub / Public JSON Feed").
     */
    String getSourceName();

    /**
     * True only if the required URL / API credentials are configured in environment variables.
     */
    boolean isConfigured();

    /**
     * Instructions explaining how to configure this source via environment variables.
     */
    String getConfigurationInstructions();

    /**
     * Fetches and normalizes upcoming technology events from the external provider.
     *
     * @throws Exception if the remote endpoint fails or returns invalid data
     */
    List<ImportedEventPayload> fetchUpcomingEvents() throws Exception;
}
