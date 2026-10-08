package com.techpulse.service;

import com.techpulse.model.Event;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Generates valid RFC 5545 (.ics) iCalendar files for downloading event reminders.
 * Handles text escaping (\, ;, ,, newlines), unique event identifiers, UTC timestamps,
 * and explicitly documents any assumed duration when an event has no endTime.
 */
@Service
public class CalendarIcsService {

    private static final DateTimeFormatter ICS_UTC_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    public static final int DEFAULT_ASSUMED_DURATION_HOURS = 2;

    public String generateIcsForEvent(Event event, String appEventUrl) {
        OffsetDateTime nowUtc = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime start = event.getStartTime();
        boolean assumedEndTime = (event.getEndTime() == null);
        OffsetDateTime end = assumedEndTime
                ? start.plusHours(DEFAULT_ASSUMED_DURATION_HOURS)
                : event.getEndTime();

        StringBuilder descriptionBuilder = new StringBuilder();
        descriptionBuilder.append(event.getDescription() != null ? event.getDescription() : "Not provided");
        descriptionBuilder.append("\n\nOrganizer: ").append(event.getOrganizer() != null ? event.getOrganizer() : "Not provided");
        descriptionBuilder.append("\nTopic / Type: ").append(event.getTopic()).append(" / ").append(event.getEventType());
        descriptionBuilder.append("\nMode: ").append(event.getMode());
        if (assumedEndTime) {
            descriptionBuilder.append("\n\nNOTE: Official end time was not provided by the source. ")
                    .append("A default duration of ")
                    .append(DEFAULT_ASSUMED_DURATION_HOURS)
                    .append(" hours has been assumed for this calendar entry.");
        }
        if (appEventUrl != null && !appEventUrl.isBlank()) {
            descriptionBuilder.append("\nEvent Link: ").append(appEventUrl);
        }

        String location = event.getVenueOrPlatform() != null && !event.getVenueOrPlatform().isBlank()
                ? event.getVenueOrPlatform() + " (" + event.getCity() + ")"
                : event.getCity();

        String uid = "techpulse-event-" + event.getId() + "@techpulse.edu.in";

        StringBuilder ics = new StringBuilder();
        ics.append("BEGIN:VCALENDAR\r\n");
        ics.append("VERSION:2.0\r\n");
        ics.append("PRODID:-//TECHPULSE//Course 2113611 Full Stack Java//EN\r\n");
        ics.append("CALSCALE:GREGORIAN\r\n");
        ics.append("METHOD:PUBLISH\r\n");
        ics.append("X-WR-TIMEZONE:Asia/Kolkata\r\n");
        ics.append("BEGIN:VEVENT\r\n");
        ics.append("UID:").append(escapeIcsText(uid)).append("\r\n");
        ics.append("DTSTAMP:").append(ICS_UTC_FORMAT.format(nowUtc)).append("\r\n");
        ics.append("DTSTART:").append(ICS_UTC_FORMAT.format(start)).append("\r\n");
        ics.append("DTEND:").append(ICS_UTC_FORMAT.format(end)).append("\r\n");
        ics.append("SUMMARY:").append(escapeIcsText(event.getTitle())).append("\r\n");
        ics.append("DESCRIPTION:").append(escapeIcsText(descriptionBuilder.toString())).append("\r\n");
        ics.append("LOCATION:").append(escapeIcsText(location)).append("\r\n");
        if (event.isCancelled()) {
            ics.append("STATUS:CANCELLED\r\n");
        } else {
            ics.append("STATUS:CONFIRMED\r\n");
        }
        ics.append("END:VEVENT\r\n");
        ics.append("END:VCALENDAR\r\n");

        return ics.toString();
    }

    /**
     * Escapes special characters according to RFC 5545 Section 3.3.11.
     */
    public String escapeIcsText(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n")
                .replace("\r", "\\n");
    }
}
