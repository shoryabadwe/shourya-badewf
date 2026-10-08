package com.techpulse.service;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * Calculates date discovery windows in the Asia/Kolkata timezone with explicit inclusive boundaries.
 *
 * Boundary Definitions (All in Asia/Kolkata, IST +05:30, inclusive [start, end]):
 * - TODAY:        From current instant (now) through today 23:59:59.999999999+05:30.
 * - THIS_WEEKEND:
 *     * If today is Monday–Friday: Upcoming Saturday 00:00:00+05:30 through Sunday 23:59:59.999999999+05:30.
 *     * If today is Saturday:      Today (Saturday) 00:00:00+05:30 through tomorrow (Sunday) 23:59:59.999999999+05:30.
 *     * If today is Sunday:        Yesterday (Saturday) 00:00:00+05:30 through today (Sunday) 23:59:59.999999999+05:30.
 * - NEXT_7_DAYS:  From current instant (now) through (today + 7 days) at 23:59:59.999999999+05:30.
 * - NEXT_30_DAYS: (Default) From current instant (now) through (today + 30 days) at 23:59:59.999999999+05:30.
 * - ALL_UPCOMING: From current instant (now) through (today + 365 days) at 23:59:59.999999999+05:30.
 */
@Service
public class DateWindowService {

    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Kolkata");

    public record DateWindowBounds(String windowKey, OffsetDateTime startInclusive, OffsetDateTime endInclusive) {
        public boolean contains(OffsetDateTime timestamp) {
            if (timestamp == null) {
                return false;
            }
            return !timestamp.isBefore(startInclusive) && !timestamp.isAfter(endInclusive);
        }
    }

    public OffsetDateTime nowInKolkata() {
        return ZonedDateTime.now(DEFAULT_ZONE).toOffsetDateTime();
    }

    public DateWindowBounds resolveWindow(String windowParam) {
        return resolveWindowFromReference(windowParam, ZonedDateTime.now(DEFAULT_ZONE));
    }

    /**
     * Pure calculation method accepting a reference ZonedDateTime for deterministic unit testing.
     */
    public DateWindowBounds resolveWindowFromReference(String windowParam, ZonedDateTime referenceNow) {
        ZonedDateTime kolkataNow = referenceNow.withZoneSameInstant(DEFAULT_ZONE);
        LocalDate today = kolkataNow.toLocalDate();
        LocalTime endOfDay = LocalTime.of(23, 59, 59, 999_999_999);

        String normalized = (windowParam == null || windowParam.isBlank())
                ? "NEXT_30_DAYS"
                : windowParam.trim().toUpperCase();

        switch (normalized) {
            case "TODAY": {
                OffsetDateTime start = ZonedDateTime.of(today, LocalTime.MIDNIGHT, DEFAULT_ZONE).toOffsetDateTime();
                OffsetDateTime end = ZonedDateTime.of(today, endOfDay, DEFAULT_ZONE).toOffsetDateTime();
                return new DateWindowBounds("TODAY", start, end);
            }
            case "THIS_WEEKEND": {
                DayOfWeek dow = today.getDayOfWeek();
                LocalDate saturday;
                LocalDate sunday;
                if (dow == DayOfWeek.SATURDAY) {
                    saturday = today;
                    sunday = today.plusDays(1);
                } else if (dow == DayOfWeek.SUNDAY) {
                    saturday = today.minusDays(1);
                    sunday = today;
                } else {
                    saturday = today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
                    sunday = saturday.plusDays(1);
                }
                OffsetDateTime start = ZonedDateTime.of(saturday, LocalTime.MIDNIGHT, DEFAULT_ZONE).toOffsetDateTime();
                OffsetDateTime end = ZonedDateTime.of(sunday, endOfDay, DEFAULT_ZONE).toOffsetDateTime();
                return new DateWindowBounds("THIS_WEEKEND", start, end);
            }
            case "NEXT_7_DAYS": {
                OffsetDateTime start = kolkataNow.toOffsetDateTime();
                OffsetDateTime end = ZonedDateTime.of(today.plusDays(7), endOfDay, DEFAULT_ZONE).toOffsetDateTime();
                return new DateWindowBounds("NEXT_7_DAYS", start, end);
            }
            case "ALL_UPCOMING": {
                OffsetDateTime start = kolkataNow.toOffsetDateTime();
                OffsetDateTime end = ZonedDateTime.of(today.plusDays(365), endOfDay, DEFAULT_ZONE).toOffsetDateTime();
                return new DateWindowBounds("ALL_UPCOMING", start, end);
            }
            case "NEXT_30_DAYS":
            default: {
                OffsetDateTime start = kolkataNow.toOffsetDateTime();
                OffsetDateTime end = ZonedDateTime.of(today.plusDays(30), endOfDay, DEFAULT_ZONE).toOffsetDateTime();
                return new DateWindowBounds("NEXT_30_DAYS", start, end);
            }
        }
    }
}
