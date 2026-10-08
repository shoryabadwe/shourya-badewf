# COLLEGE MINI PROJECT REPORT

## Project Title: **TECHPULSE — Discover. Participate. Achieve.**

- **Course Name:** Full Stack Java Programming  
- **Course Code:** 2113611  
- **Student Name(s):** `[Enter Student Name(s) Here]`  
- **Roll / Enrollment Number(s):** `[Enter Roll / Enrollment Number(s) Here]`  
- **Department / Branch:** `[Enter Department, e.g., Computer Engineering / Information Technology]`  
- **College / Institution:** `[Enter College Name Here]`  
- **Project Guide / Faculty Coordinator:** `[Enter Guide Name Here]`  
- **Academic Year / Semester:** `[Enter Academic Year / Semester Here]`

---

## 1. Abstract

Undergraduate engineering students frequently miss out on relevant technology events—such as inter-collegiate hackathons, hands-on programming workshops, cybersecurity competitions, and technical conferences—because announcements are scattered across disparate social channels and lack structured filtering by date, location, mode, and cost. Furthermore, students lack a unified system to check for schedule clashes among bookmarked events or to maintain a structured ledger of their co-curricular participation and competitive outcomes.

**TECHPULSE — Discover. Participate. Achieve.** is a full-stack web application built using **Java 17**, **Spring Boot 3.2.5**, **Spring Data JPA**, **Spring Security**, a **file-based H2 relational database**, and a responsive **HTML5, Plain CSS3, and Vanilla JavaScript** frontend served directly from Spring Boot's static resources. The system computes timezone-aware discovery windows (`Asia/Kolkata`) on the server, supports combined multi-attribute filtering without treating unknown prices as free, detects interval overlaps among saved events, and calculates non-cumulative participation credits (`0 / 5 / 20 / 50`) and competition win rates (`wins ÷ completed competitions × 100`) with administrative verification support.

---

## 2. Objectives

1. **Centralized Event Discovery:** Enable students to search and filter upcoming technology events across city (`Mumbai`, `Navi Mumbai`, `Online`, etc.), topic (`Java`, `AI/ML`, `Cybersecurity`, `Web Development`, `Data Science`), event type, delivery mode, cost, and server-computed date windows (`Today`, `This Weekend`, `Next 7 Days`, `Next 30 Days`).
2. **Accurate Financial & Temporal Representation:** Represent monetary fees using `java.math.BigDecimal`, store timestamps with timezone offsets (`OffsetDateTime` in `Asia/Kolkata`), and never misclassify unknown prices as free or incomplete event times as schedule conflicts.
3. **Schedule Conflict Detection:** Automatically warn students when two saved, non-cancelled events have overlapping time intervals (`startA < endB && startB < endA`).
4. **Idempotent Participation & Achievement Tracking:** Allow students to record self-reported progression (`Saved → Registered → Attended`) and competitive outcomes (`Pending / Won / Not Won`), calculating non-cumulative credits and competition win rates on the backend.
5. **Role-Based Security & Faculty Verification:** Enforce `ROLE_USER` and `ROLE_ADMIN` access control via Spring Security with BCrypt password hashing and CSRF protection, enabling administrators to manage events (with history-preserving soft deletion) and verify student participation records.

---

## 3. Technology Stack

| Layer | Technology Used | Purpose in TECHPULSE |
| :--- | :--- | :--- |
| **Frontend Structure** | HTML5 (`index.html`) | Semantic views, accessible form labels, ARIA live regions, and modal dialogs. |
| **Frontend Styling** | Plain CSS3 (`styles.css`) | Custom properties (CSS variables) for Bright & Night themes, responsive grid layouts, focus states, and tabular numerals. |
| **Frontend Logic** | Vanilla JavaScript (`app.js`) | `fetch()` REST API calls, CSRF header injection (`X-XSRF-TOKEN`), safe DOM updates (`textContent` / `escapeHtml`), and hash routing. |
| **Backend Framework** | Java 17 + Spring Boot 3.2.5 | Embedded Tomcat server serving static files and `/api/**` REST controllers in a single process. |
| **Persistence & ORM** | Spring Data JPA (Hibernate) | Object-Relational Mapping, entity relationships, and transactional service methods. |
| **Relational Database** | H2 Database (File Mode) | Persistent local storage (`./data/techpulse_db`) surviving server restarts; configurable for MySQL migration. |
| **Security** | Spring Security 6 | Server-side `HttpSession`, `BCryptPasswordEncoder`, `CookieCsrfTokenRepository`, and `@PreAuthorize` role enforcement. |
| **Validation & Testing** | Jakarta Bean Validation + JUnit 5 | Request DTO validation (`@NotBlank`, `@Email`, `@DecimalMin`) and automated service verification tests. |
| **Build Tool** | Apache Maven (`pom.xml`) | Dependency management, compilation, testing, and Spring Boot execution. |

---

## 4. System Architecture

TECHPULSE follows a layered **Controller — Service — Repository — Database** architecture:

```text
+-----------------------------------------------------------------------+
| Browser Client (HTML5 + Plain CSS3 + Vanilla JavaScript)              |
| - Renders Explore, Details, Saved, Activity, Login/Register, Admin    |
| - Reads XSRF-TOKEN cookie and sends X-XSRF-TOKEN on POST/PUT/DELETE   |
+-----------------------------------+-----------------------------------+
                                    | HTTP / JSON REST APIs + .ics stream
                                    v
+-----------------------------------------------------------------------+
| Spring Security Filter Chain (SecurityConfig.java)                    |
| - Server-side HttpSession, BCrypt authentication, CSRF validation     |
| - Enforces ROLE_ADMIN on /api/admin/** and auth on /api/bookmarks/**  |
+-----------------------------------+-----------------------------------+
                                    |
                                    v
+-----------------------------------------------------------------------+
| REST Controllers (com.techpulse.controller.*)                         |
| - EventController, AuthController, BookmarkController,                |
|   ParticipationController, AdminController, GlobalExceptionHandler    |
+-----------------------------------+-----------------------------------+
                                    |
                                    v
+-----------------------------------------------------------------------+
| Business Services (com.techpulse.service.*)                           |
| - DateWindowService (Asia/Kolkata date windows)                       |
| - EventService (combined filters, soft delete, validation)            |
| - ScheduleConflictService (interval overlap check)                    |
| - ParticipationService (non-cumulative credits, win rate, badges)     |
| - CalendarIcsService (RFC 5545 .ics export)                           |
| - LiveSourceSyncService + EventSourceAdapter (external feed sync)     |
+-----------------------------------+-----------------------------------+
                                    |
                                    v
+-----------------------------------------------------------------------+
| Spring Data JPA Repositories & H2 File Database (./data/techpulse_db) |
| - Tables: users, events, bookmarks, participations,                   |
|           source_refresh_logs                                         |
+-----------------------------------------------------------------------+
```

---

## 5. Database Schema & Entity Relationships

1. **`users` (`User.java`)**
   - **Primary Key:** `id` (`BIGINT AUTO_INCREMENT`)
   - **Unique Constraint:** `uk_users_email` on `email`
   - **Fields:** `full_name`, `email`, `password_hash` (BCrypt), `college_or_institution`, `role` (`USER` / `ADMIN`), `created_at`
2. **`events` (`Event.java`)**
   - **Primary Key:** `id` (`BIGINT AUTO_INCREMENT`)
   - **Fields:** `title`, `description`, `topic`, `event_type`, `mode`, `organizer`, `city`, `venue_or_platform`, `start_time`, `end_time`, `registration_deadline`, `cost_type` (`FREE` / `PAID` / `NOT_PROVIDED`), `price` (`DECIMAL(10,2)`), `currency`, `eligibility`, `registration_url`, `official_source_url`, `source_name`, `external_event_id`, `data_origin` (`DEMO_SEED` / `MANUAL_ADMIN` / `LIVE_IMPORT`), `cancelled`, `registration_closed`, `deleted` (soft-delete flag), `manually_edited`, `last_fetched_at`, `created_at`, `updated_at`
3. **`bookmarks` (`Bookmark.java`)**
   - **Primary Key:** `id` (`BIGINT AUTO_INCREMENT`)
   - **Foreign Keys:** `user_id` $\rightarrow$ `users(id)` (`@ManyToOne`), `event_id` $\rightarrow$ `events(id)` (`@ManyToOne`)
   - **Unique Constraint:** `uk_bookmarks_user_event` on `(user_id, event_id)`
4. **`participations` (`Participation.java`)**
   - **Primary Key:** `id` (`BIGINT AUTO_INCREMENT`)
   - **Foreign Keys:** `user_id` $\rightarrow$ `users(id)` (`@ManyToOne`), `event_id` $\rightarrow$ `events(id)` (`@ManyToOne`)
   - **Unique Constraint:** `uk_participations_user_event` on `(user_id, event_id)`
   - **Fields:** `status` (`SAVED` / `REGISTERED` / `ATTENDED`), `outcome` (`NONE` / `PENDING` / `WON` / `NOT_WON`), `credits` (`0 / 5 / 20 / 50`), `self_reported`, `verified_by_admin`, `student_note`, `admin_verification_note`, `updated_at`
5. **`source_refresh_logs` (`SourceRefreshLog.java`)**
   - **Primary Key:** `id` (`BIGINT AUTO_INCREMENT`)
   - **Fields:** `source_name`, `configured`, `status`, `last_attempt_at`, `last_success_at`, `stale_data`, `imported_count`, `updated_count`, `status_message`

---

## 6. Key Implemented Features

1. **Server-Side `Asia/Kolkata` Date Window Engine:** Computes exact inclusive boundaries for `TODAY`, `THIS_WEEKEND`, `NEXT_7_DAYS`, and `NEXT_30_DAYS`.
2. **Multi-Attribute Combined Filtering:** Combines search keyword, city, topic, event type, mode, cost classification, maximum price (`BigDecimal`), and date window in a single query pipeline.
3. **RFC 5545 `.ics` Calendar Generator:** Exports standards-compliant `.ics` files with character escaping and an explicit note when a 2-hour default duration is assumed for events lacking an end time.
4. **Interval Overlap Conflict Detector:** Identifies overlapping saved events using `startA.isBefore(endB) && startB.isBefore(endA)` while explicitly reporting `"Cannot check overlap"` when an event lacks an end time.
5. **Non-Cumulative Credits & Competition Win-Rate Calculator:** Computes student credits (`Saved=0`, `Registered=5`, `Attended=20`, `Won Competition=50`) idempotently from current database state and calculates competition win rate strictly over completed `HACKATHON` and `COMPETITION` records with `WON` or `NOT_WON` outcomes.
6. **Soft Deletion & Live Source Adapter:** Preserves student bookmarks and participation history when administrators delete events (`deleted = true`) or refresh external event feeds.

---

## 7. Honest Limitations

1. **No Global Worldwide Event Coverage:** The application covers manually entered college events, the curated academic demonstration dataset, and configured JSON feeds. It does not scrape or index every event worldwide.
2. **External Registration & Attendance Are Not Automatically Verified:** Clicking an external registration link opens the organiser's site in a new tab; it cannot verify whether the student completed an external form. Student participation entries are explicitly labelled **Self-Reported** until a faculty administrator marks them **Admin Verified**.
3. **Incomplete Organiser Timestamps:** When an event organiser does not provide an `endTime`, the application cannot verify whether the event overlaps with later sessions on the same day and displays `"Cannot check overlap"`.

---

## 8. Future Scope

1. **Migration to Managed MySQL / PostgreSQL:** Using the pre-configured `SPRING_DATASOURCE_*` environment variables for multi-instance college deployment.
2. **Certificate PDF Upload & QR Verification:** Allowing students to attach participation certificates for automated hash/QR verification by faculty coordinators.
3. **Email & Browser Push Reminders:** Sending automated notifications 24 hours before a bookmarked event's registration deadline.
