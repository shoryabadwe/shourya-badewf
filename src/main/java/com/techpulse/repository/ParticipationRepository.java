package com.techpulse.repository;

import com.techpulse.model.Participation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    List<Participation> findByUserIdOrderByUpdatedAtDesc(Long userId);

    List<Participation> findAllByOrderByUpdatedAtDesc();

    Optional<Participation> findByUserIdAndEventId(Long userId, Long eventId);
}
