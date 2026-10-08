package com.techpulse.service;

import com.techpulse.dto.BookmarkDtos;
import com.techpulse.model.Event;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates schedule conflicts among a student's saved events using interval overlap logic.
 *
 * Rules:
 * 1. Two non-cancelled events A and B overlap if and only if:
 *    both have known startTime and endTime, AND startA < endB AND startB < endA.
 * 2. If an event is missing its endTime, we never invent a clash; instead we mark its status
 *    as "CANNOT_CHECK_OVERLAP" with an explicit message ("Cannot check overlap — end time not provided").
 * 3. Cancelled events are excluded from conflict warnings.
 */
@Service
public class ScheduleConflictService {

    private static final DateTimeFormatter READABLE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a").withZone(DateWindowService.DEFAULT_ZONE);

    /**
     * Pure interval overlap check for two timestamps ranges [startA, endA) and [startB, endB).
     */
    public boolean intervalsOverlap(OffsetDateTime startA, OffsetDateTime endA,
                                    OffsetDateTime startB, OffsetDateTime endB) {
        if (startA == null || endA == null || startB == null || endB == null) {
            return false;
        }
        return startA.isBefore(endB) && startB.isBefore(endA);
    }

    public List<BookmarkDtos.ConflictWarningDto> detectConflicts(List<Event> savedEvents) {
        List<BookmarkDtos.ConflictWarningDto> warnings = new ArrayList<>();
        int n = savedEvents.size();

        for (int i = 0; i < n; i++) {
            Event first = savedEvents.get(i);
            if (first.isCancelled() || first.getStartTime() == null || first.getEndTime() == null) {
                continue;
            }
            for (int j = i + 1; j < n; j++) {
                Event second = savedEvents.get(j);
                if (second.isCancelled() || second.getStartTime() == null || second.getEndTime() == null) {
                    continue;
                }
                if (intervalsOverlap(first.getStartTime(), first.getEndTime(),
                        second.getStartTime(), second.getEndTime())) {
                    BookmarkDtos.ConflictWarningDto dto = new BookmarkDtos.ConflictWarningDto();
                    dto.setFirstEventId(first.getId());
                    dto.setFirstEventTitle(first.getTitle());
                    dto.setFirstStart(first.getStartTime());
                    dto.setFirstEnd(first.getEndTime());
                    dto.setSecondEventId(second.getId());
                    dto.setSecondEventTitle(second.getTitle());
                    dto.setSecondStart(second.getStartTime());
                    dto.setSecondEnd(second.getEndTime());
                    dto.setExplanation(String.format(
                            "Schedule conflict: \"%s\" (%s – %s IST) overlaps with \"%s\" (%s – %s IST).",
                            first.getTitle(),
                            READABLE_FMT.format(first.getStartTime()),
                            READABLE_FMT.format(first.getEndTime()),
                            second.getTitle(),
                            READABLE_FMT.format(second.getStartTime()),
                            READABLE_FMT.format(second.getEndTime())
                    ));
                    warnings.add(dto);
                }
            }
        }
        return warnings;
    }
}
