package com.techpulse.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Stores a student's participation status and competition outcome for an event.
 * Enforces a unique constraint on (user_id, event_id) so repeated updates are idempotent
 * and never award duplicate credits.
 */
@Entity
@Table(name = "participations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_participations_user_event", columnNames = {"user_id", "event_id"})
})
public class Participation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParticipationStatus status = ParticipationStatus.SAVED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CompetitionOutcome outcome = CompetitionOutcome.NONE;

    /**
     * Non-cumulative total credits for this event record:
     * SAVED = 0, REGISTERED = 5, ATTENDED = 20, ATTENDED + WON (competitive) = 50.
     */
    @Column(nullable = false)
    private int credits = 0;

    /**
     * Student-entered updates are always marked selfReported = true until verified by an administrator.
     */
    @Column(nullable = false)
    private boolean selfReported = true;

    @Column(nullable = false)
    private boolean verifiedByAdmin = false;

    @Column(length = 400)
    private String studentNote;

    @Column(length = 400)
    private String adminVerificationNote;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    public Participation() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public ParticipationStatus getStatus() {
        return status;
    }

    public void setStatus(ParticipationStatus status) {
        this.status = status;
    }

    public CompetitionOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(CompetitionOutcome outcome) {
        this.outcome = outcome;
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public boolean isSelfReported() {
        return selfReported;
    }

    public void setSelfReported(boolean selfReported) {
        this.selfReported = selfReported;
    }

    public boolean isVerifiedByAdmin() {
        return verifiedByAdmin;
    }

    public void setVerifiedByAdmin(boolean verifiedByAdmin) {
        this.verifiedByAdmin = verifiedByAdmin;
    }

    public String getStudentNote() {
        return studentNote;
    }

    public void setStudentNote(String studentNote) {
        this.studentNote = studentNote;
    }

    public String getAdminVerificationNote() {
        return adminVerificationNote;
    }

    public void setAdminVerificationNote(String adminVerificationNote) {
        this.adminVerificationNote = adminVerificationNote;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
