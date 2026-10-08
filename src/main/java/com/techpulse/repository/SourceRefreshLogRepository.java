package com.techpulse.repository;

import com.techpulse.model.SourceRefreshLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SourceRefreshLogRepository extends JpaRepository<SourceRefreshLog, Long> {
    Optional<SourceRefreshLog> findBySourceNameIgnoreCase(String sourceName);
}
