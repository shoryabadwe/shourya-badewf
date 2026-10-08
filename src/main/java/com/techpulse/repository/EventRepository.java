package com.techpulse.repository;

import com.techpulse.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByDeletedFalseOrderByStartTimeAsc();

    List<Event> findByDeletedFalseOrderByCreatedAtDesc();

    Optional<Event> findBySourceNameIgnoreCaseAndExternalEventId(String sourceName, String externalEventId);

    Optional<Event> findByTitleIgnoreCaseAndStartTimeAndOrganizerIgnoreCase(
            String title, OffsetDateTime startTime, String organizer
    );
}
