# COMPLETE CODE WALKTHROUGH & VIVA TEACHING GUIDE

**Project:** TECHPULSE — Discover. Participate. Achieve.  
**Course:** Full Stack Java Programming (2113611)

This document walks you through every part of the generated codebase so you can understand it deeply and present it confidently to your professor.

---

## 1. Project Overview: What Problem It Solves and How It Works

### The Problem
College students hear about hackathons, workshops, webinars, and coding competitions from scattered WhatsApp groups, posters, and emails. Because there is no central place:
1. Students miss registration deadlines or cannot filter events by their city (`Mumbai`, `Navi Mumbai`, `Online`), budget (`Free` vs `Paid`), or upcoming weekend.
2. Students accidentally save two events happening at the same time without realizing their schedules clash.
3. Students have no organized record of which events they registered for, attended, or won, making it hard to track their co-curricular progress during the semester.

### How TECHPULSE Works
- **Single Spring Boot Server:** Everything runs inside one Java 17 + Spring Boot 3.2.5 application (`TechPulseApplication.java`).
- **Static Frontend (`src/main/resources/static/`):** Spring Boot directly serves `index.html`, `css/styles.css`, and `js/app.js` to the browser.
- **REST API Communication:** When you interact with the webpage, Vanilla JavaScript uses `fetch()` to send HTTP requests (`/api/events`, `/api/bookmarks`, `/api/participations`, `/api/auth`, `/api/admin`) to Java `@RestController` classes.
- **File-Based H2 Database (`./data/techpulse_db`):** Spring Data JPA stores users, events, bookmarks, and participation records on disk so data survives server restarts.

---

## 2. Folder Structure: Explaining Every Important Folder and File

```text
techpulse-app/
├── pom.xml                                      # Maven configuration (Java 17, Spring Boot 3.2.5, JPA, Security, Validation, H2)
├── README.md                                    # Setup instructions, Windows commands, demo credentials, troubleshooting
├── PROJECT_REPORT.md                            # Formal academic project report with editable student placeholders
├── VIVA_QUESTIONS.md                            # Beginner-friendly concept explanations & 20 viva Q&As
├── CODE_WALKTHROUGH.md                          # This step-by-step code walkthrough & feature teacher
└── src/
    ├── main/
    │   ├── java/com/techpulse/
    │   │   ├── TechPulseApplication.java        # Main @SpringBootApplication entry point; sets default timezone to Asia/Kolkata
    │   │   ├── config/
    │   │   │   └── DemoDataSeeder.java          # Seeds demo student/admin accounts & 12 clearly labelled demo events on first boot
    │   │   ├── controller/
    │   │   │   ├── AuthController.java          # Handles /api/auth/login, /register, /logout, /me, and /api/csrf
    │   │   │   ├── EventController.java         # Handles /api/events, /api/events/{id}, /calendar.ics, and /api/sources/*
    │   │   │   ├── BookmarkController.java      # Handles /api/bookmarks (GET, POST, DELETE) for logged-in user
    │   │   │   ├── ParticipationController.java # Handles /api/participations/summary and POST /api/participations
    │   │   │   ├── AdminController.java         # Handles /api/admin/** (Add/Edit/Cancel/Soft-Delete events & Verify participation)
    │   │   │   └── GlobalExceptionHandler.java  # Converts Java exceptions into clean HTTP 400/401/403/404 JSON responses
    │   │   ├── dto/
    │   │   │   ├── AuthDtos.java                # LoginRequest, RegisterRequest, UserProfileResponse
    │   │   │   ├── EventDtos.java               # EventRequest, EventResponse, PaginatedEventResponse
    │   │   │   ├── BookmarkDtos.java            # BookmarkItemDto, ConflictWarningDto, SavedEventsOverviewResponse
    │   │   │   ├── ParticipationDtos.java       # ParticipationUpsertRequest, AdminVerifyParticipationRequest, ActivitySummaryResponse
    │   │   │   └── SourceStatusDto.java         # Status of external event source adapter
    │   │   ├── model/
    │   │   │   ├── User.java                    # @Entity mapped to "users" table
    │   │   │   ├── Event.java                   # @Entity mapped to "events" table (uses BigDecimal price & soft-delete flag)
    │   │   │   ├── Bookmark.java                # @Entity mapped to "bookmarks" table with unique (user_id, event_id)
    │   │   │   ├── Participation.java           # @Entity mapped to "participations" table with unique (user_id, event_id)
    │   │   │   ├── SourceRefreshLog.java        # @Entity mapped to "source_refresh_logs" table
    │   │   │   └── [Enums]                      # Role, EventType, EventMode, CostType, DataSourceOrigin, ParticipationStatus, CompetitionOutcome
    │   │   ├── repository/
    │   │   │   ├── UserRepository.java          # Spring Data JPA interface for User queries
    │   │   │   ├── EventRepository.java         # Spring Data JPA interface for Event queries
    │   │   │   ├── BookmarkRepository.java      # Spring Data JPA interface for Bookmark queries
    │   │   │   ├── ParticipationRepository.java # Spring Data JPA interface for Participation queries
    │   │   │   └── SourceRefreshLogRepository.java
    │   │   ├── security/
    │   │   │   ├── SecurityConfig.java          # Spring Security filter chain, BCrypt encoder, CSRF cookie setup, role checks
    │   │   │   └── CustomUserDetailsService.java# Bridges UserRepository with Spring Security authentication
    │   │   ├── service/
    │   │   │   ├── DateWindowService.java       # Calculates TODAY, THIS_WEEKEND, NEXT_7_DAYS, NEXT_30_DAYS in Asia/Kolkata
    │   │   │   ├── EventService.java            # Combined filtering, pagination, bookmarking, admin CRUD & validation
    │   │   │   ├── ScheduleConflictService.java # Interval overlap detection (startA < endB && startB < endA)
    │   │   │   ├── ParticipationService.java    # Non-cumulative credits (0/5/20/50), win rate, and badge calculation
    │   │   │   ├── CalendarIcsService.java      # RFC 5545 .ics calendar file generator
    │   │   │   └── LiveSourceSyncService.java   # Coordinates external feed import, caching, deduplication, and stale-data status
    │   │   └── source/
    │   │       ├── EventSourceAdapter.java      # Java interface for pluggable event sources
    │   │       ├── ImportedEventPayload.java    # Normalized payload object for imported events
    │   │       └── ConfigurableRestEventSourceAdapter.java # HTTP client adapter reading env vars
    │   └── resources/
    │       ├── application.properties           # H2 file database config, timezone, session settings
    │       └── static/
    │           ├── index.html                   # Single-page HTML5 layout containing all 6 views
    │           ├── css/styles.css               # Plain CSS3 with Bright & Night themes using CSS variables
    │           └── js/app.js                    # Vanilla JS frontend logic, fetch helper, and DOM rendering
    └── test/
        └── java/com/techpulse/
            └── TechPulseVerificationTests.java  # Automated JUnit 5 verification tests
```

---

## 3. Frontend Architecture (`index.html`, `styles.css`, `app.js`)

### HTML5 Layout (`src/main/resources/static/index.html`)
- Uses semantic HTML5 tags: `<header>`, `<nav>`, `<main>`, `<section>`, `<article>`, `<form>`, `<table>`, and `<footer>`.
- Contains six `<section class="view-section">` blocks:
  1. `#view-explore` (Explore Events)
  2. `#view-details` (Event Details)
  3. `#view-saved` (My Saved Events)
  4. `#view-activity` (My Activity & Achievements)
  5. `#view-auth` (Login & Register)
  6. `#view-admin` (Admin Dashboard)
- Hash navigation (`#explore`, `#event/2`, `#saved`, `#activity`, `#auth`, `#admin`) switches the visible section without reloading the entire browser page.

### Plain CSS3 Themes (`src/main/resources/static/css/styles.css`)
- Uses CSS Custom Properties (variables) defined on `:root, [data-theme="bright"]` and overridden on `[data-theme="night"]`.
- **Bright Mode:** Warm white canvas (`#FAF8F5`), crisp white surface cards (`#FFFFFF`), dark text (`#111827`), and teal accents (`#0F766E`).
- **Night Mode:** Deep navy canvas (`#0B1120`), muted slate panels (`#111827`), light text (`#F1F5F9`), and cyan accents (`#06B6D4`).
- `initTheme()` and `toggleTheme()` in `app.js` check `localStorage.getItem('techpulse_theme')` first, fall back to `window.matchMedia('(prefers-color-scheme: dark)')`, and update `document.documentElement.setAttribute('data-theme', theme)`.

### Vanilla JavaScript & CSRF Fetch Helper (`src/main/resources/static/js/app.js`)
- `apiFetch(url, options)` wraps the browser's `fetch()` API.
- For mutating requests (`POST`, `PUT`, `PATCH`, `DELETE`), `readCsrfCookie()` reads the `XSRF-TOKEN` cookie set by Spring Security and sends it in the `X-XSRF-TOKEN` request header.

---

## 4. Backend Architecture (Controllers, Services, Repositories, Entities, DTOs)

1. **Controllers (`com.techpulse.controller`):** Annotated with `@RestController`. Their job is ONLY to accept HTTP requests, validate input parameters, check who is logged in, call a Service method, and return an HTTP `ResponseEntity`.
2. **Services (`com.techpulse.service`):** Annotated with `@Service` and `@Transactional`. All business rules—such as date window math (`DateWindowService`), schedule conflict detection (`ScheduleConflictService`), and credit/win-rate calculations (`ParticipationService`)—live here.
3. **Repositories (`com.techpulse.repository`):** Interfaces extending `JpaRepository<Entity, Long>`. Spring Data JPA automatically generates the SQL queries at runtime.
4. **Entities (`com.techpulse.model`):** Java classes annotated with `@Entity` and `@Table` that map directly to database tables (`User`, `Event`, `Bookmark`, `Participation`, `SourceRefreshLog`).
5. **DTOs (`com.techpulse.dto`):** Plain Java classes used for JSON request bodies and responses so internal fields (like `User.passwordHash`) are never exposed to the browser.

---

## 5. Database Design (Tables, Primary Keys, Foreign Keys, Relationships)

| Table Name | Primary Key | Foreign Keys | Unique Constraints | Relationship Explanation |
| :--- | :--- | :--- | :--- | :--- |
| `users` | `id` | None | `uk_users_email (email)` | One user can have many bookmarks and many participation records (`1:N`). |
| `events` | `id` | None | Indexed on `start_time` and `(source_name, external_event_id)` | One event can be bookmarked or participated in by many users (`1:N`). |
| `bookmarks` | `id` | `user_id` $\rightarrow$ `users.id`<br>`event_id` $\rightarrow$ `events.id` | `uk_bookmarks_user_event (user_id, event_id)` | Many-to-One (`@ManyToOne`) to `User` and `Event`. Prevents duplicate bookmarks. |
| `participations` | `id` | `user_id` $\rightarrow$ `users.id`<br>`event_id` $\rightarrow$ `events.id` | `uk_participations_user_event (user_id, event_id)` | Many-to-One (`@ManyToOne`) to `User` and `Event`. Ensures one participation row per student per event. |
| `source_refresh_logs` | `id` | None | `source_name` | Stores last attempt, last success, and stale-data status for live event sources. |

---

## 6. Step-by-Step Feature Teaching & Complete Request Flows

Below, we trace each core feature from the user's click in the browser all the way to the H2 database and back.
*(Technical terms used below: **Endpoint** = a specific URL path on the server like `/api/events`; **Query String** = key-value parameters after `?` in a URL; **DTO** = Data Transfer Object carried as JSON; **Idempotent** = performing the same action multiple times produces the exact same database state without duplicates.)*

---

### Feature 1: Loading Upcoming Events & Searching/Filtering

1. **What the user does:** Opens the application (or chooses filters like City = `Mumbai`, Date Window = `NEXT_30_DAYS` and clicks **Apply Filters**).
2. **Which JavaScript function runs:** `loadExploreEvents()` in `src/main/resources/static/js/app.js`. It reads the form values, builds a `URLSearchParams` query string, and calls `apiFetch('/api/events?' + params.toString())`.
3. **Which HTTP request is sent:**  
   `GET /api/events?city=Mumbai&topic=ALL&eventType=ALL&mode=ALL&cost=ALL&dateWindow=NEXT_30_DAYS&sortBy=SOONEST&page=0&size=9`
4. **Which Java controller and service methods run:**
   - `EventController.discoverEvents()` receives the query parameters and calls `eventService.discoverEvents(...)`.
   - `EventService.discoverEvents()` calls `dateWindowService.resolveWindow("NEXT_30_DAYS")`, which computes `startInclusive` (current IST time) and `endInclusive` (`today + 30 days at 23:59:59.999999999+05:30`).
   - `EventService` filters the events by the date window and each active filter (`matchesSearch`, `matchesCity`, `matchesTopic`, `matchesType`, `matchesMode`, `matchesCost`), slices the requested page, and converts each `Event` entity into an `EventDtos.EventResponse` (calculating `countdownLabel` and `priceDisplay`).
5. **What the database does:** `eventRepository.findByDeletedFalseOrderByStartTimeAsc()` executes a `SELECT` query on the `events` table where `deleted = false` ordered by `start_time ASC`.
6. **How the response updates the screen:** `loadExploreEvents()` receives the `PaginatedEventResponse` JSON, updates `#server-window-range` with the active IST window, maps each event through `renderEventCardHtml(ev)`, inserts the HTML into `#explore-events-container`, and binds click handlers via `attachCardListeners()`.

#### Check Your Understanding (Feature 1):
1. Why is the `NEXT_30_DAYS` date window calculated in `DateWindowService.java` on the backend instead of using the visitor's browser clock?
2. What happens in `EventService.matchesCost()` when a user filters for `FREE` events and an event has `CostType.NOT_PROVIDED`?
3. Why does `eventRepository.findByDeletedFalseOrderByStartTimeAsc()` check `DeletedFalse`?

---

### Feature 2: Saving (Bookmarking) an Event & Detecting Schedule Conflicts

1. **What the user does:** Clicks **Save Event** on an event card, then clicks **My Saved Events** in the navigation bar.
2. **Which JavaScript function runs:**
   - Clicking Save calls `toggleBookmark(eventId, false)` in `app.js`, which invokes `apiFetch('/api/bookmarks/' + eventId, { method: 'POST' })`.
   - Opening the Saved tab calls `loadSavedEventsView()`, which invokes `apiFetch('/api/bookmarks')`.
3. **Which HTTP requests are sent:**
   - `POST /api/bookmarks/{eventId}` (with `X-XSRF-TOKEN` header)
   - `GET /api/bookmarks`
4. **Which Java controller and service methods run:**
   - `BookmarkController.addBookmark()` verifies the logged-in user and calls `eventService.addBookmark(userId, eventId)`.
   - `BookmarkController.getMyBookmarks()` calls `eventService.getSavedEventsForUser(userId)`, which passes all saved events to `scheduleConflictService.detectConflicts(allSavedEvents)`.
   - `ScheduleConflictService.detectConflicts()` compares every pair of non-cancelled saved events that have both `startTime` and `endTime` using `intervalsOverlap()`:
     ```java
     return startA.isBefore(endB) && startB.isBefore(endA);
     ```
   - If an event has `endTime == null`, `EventService` marks its status as `"CANNOT_CHECK_OVERLAP"`.
5. **What the database does:**
   - `bookmarkRepository.existsByUserIdAndEventId(userId, eventId)` checks if the bookmark already exists. If not, `bookmarkRepository.save(bookmark)` inserts a row into `bookmarks`.
   - `bookmarkRepository.findByUserIdOrderByEventStartTimeAsc(userId)` fetches all saved events for that user.
6. **How the response updates the screen:** `loadSavedEventsView()` renders red conflict warning banners in `#saved-conflicts-banner` for any overlapping events, yellow notices for incomplete-time events (`Cannot check overlap`), and separates cards into `#saved-upcoming-container` and `#saved-past-container`.

#### Check Your Understanding (Feature 2):
1. Why does `ScheduleConflictService` use `startA.isBefore(endB) && startB.isBefore(endA)` instead of just checking if two events are on the same date?
2. What does TECHPULSE display if a saved event has a `startTime` but its `endTime` is `null`?
3. How does the database prevent duplicate bookmarks if a user double-clicks the Save button rapidly?

---

### Feature 3: Recording Attendance or a Competition Win & Calculating Credits/Win Rate

1. **What the user does:** Opens **My Activity** (or **Event Details**), selects `Attended` and `Won` for a Hackathon, and clicks **Save Update**.
2. **Which JavaScript function runs:** The event listener in `loadActivityView()` (or `loadEventDetails()`) calls `apiFetch('/api/participations', { method: 'POST', body: JSON.stringify({ eventId, status, outcome }) })` followed by `loadActivityView()` (`GET /api/participations/summary`).
3. **Which HTTP requests are sent:**
   - `POST /api/participations`
   - `GET /api/participations/summary`
4. **Which Java controller and service methods run:**
   - `ParticipationController.upsertMyParticipation()` calls `participationService.upsertStudentParticipation(userId, request)`.
   - `ParticipationService.normalizeOutcome()` verifies that `WON` / `NOT_WON` is only allowed when the event is a `HACKATHON` or `COMPETITION` and the status is `ATTENDED`.
   - `ParticipationService.calculateCreditsForRecord()` assigns non-cumulative credits: `SAVED = 0`, `REGISTERED = 5`, `ATTENDED = 20`, or `ATTENDED + WON (competitive) = 50`.
   - `ParticipationService.buildActivitySummary(userId)` loops through all participation records of the student, sums the non-cumulative credits, determines the engagement badge (`Explorer`, `Builder`, `Challenger`, `Champion`), and computes the competition win rate:
     $$\text{Win Rate} = \frac{\text{winsCount}}{\text{completedCompetitionsWithOutcome}} \times 100$$
5. **What the database does:** `participationRepository.findByUserIdAndEventId(userId, eventId)` finds the existing row (if any) and updates it in place (`UPDATE participations SET status=?, outcome=?, credits=?, self_reported=true ...`) or inserts a new row.
6. **How the response updates the screen:** `loadActivityView()` updates the badge progress bar (`#activity-badge-panel`), the 8 metric boxes (`#activity-stats-grid`), and the activity history table (`#activity-history-table-wrapper`).

#### Check Your Understanding (Feature 3):
1. If a student first marks an event as `Registered` (5 credits) and later updates it to `Attended` (20 credits), why is the student's credit total for that event `20` and not `25`?
2. Which events are included in the denominator of the Competition Win Rate, and which are excluded?
3. What is displayed for Competition Win Rate when a student has attended 3 workshops and 0 completed competitions?

---

### Feature 4: Adding, Editing, or Soft-Deleting an Event as an Administrator

1. **What the user does:** Signs in as `admin@techpulse.edu.in`, opens **Admin Dashboard**, fills out the event form (or clicks **Soft Delete** on an existing event and confirms the modal).
2. **Which JavaScript function runs:** The `#admin-event-form` submit listener (or `.js-admin-delete-ev` listener) in `app.js` calls `apiFetch('/api/admin/events', { method: 'POST', body: ... })` or `apiFetch('/api/admin/events/' + id, { method: 'DELETE' })`.
3. **Which HTTP requests are sent:**
   - `POST /api/admin/events` (or `PUT /api/admin/events/{id}`)
   - `DELETE /api/admin/events/{id}`
4. **Which Java controller and service methods run:**
   - Spring Security first verifies that the session user has `ROLE_ADMIN`. If a normal student calls this URL, `SecurityConfig` immediately returns `403 Forbidden`.
   - `AdminController.createEvent()` validates the DTO with `@Valid` and calls `eventService.createEventByAdmin(request)`.
   - `EventService.validateEventTimingsAndCost()` verifies that `endTime >= startTime`, `registrationDeadline <= startTime`, and paid events have a positive `BigDecimal` price.
   - For deletion, `AdminController.softDeleteEvent()` calls `eventService.softDeleteEventByAdmin(id)`, which sets `event.setDeleted(true)`.
5. **What the database does:** Inserts or updates the row in the `events` table with `data_origin = 'MANUAL_ADMIN'` and `manually_edited = true`. On soft delete, it runs `UPDATE events SET deleted = true WHERE id = ?`.
6. **How the response updates the screen:** A toast notification confirms the action and `loadAdminEventsTable()` refreshes the administrator's table.

#### Check Your Understanding (Feature 4):
1. What happens if a logged-in student (`ROLE_USER`) tries to send a `POST /api/admin/events` request using browser developer tools?
2. What validation error occurs if an administrator enters an event `endTime` that is earlier than its `startTime`?
3. Why does `softDeleteEventByAdmin()` set `deleted = true` instead of deleting the row from the `events` table?

---

## 7. Login Flow: Sessions, Password Hashing, Roles, and Access Checks

1. **Registration (`POST /api/auth/register`):**
   - `AuthController.register()` checks `userRepository.existsByEmailIgnoreCase(email)`.
   - It hashes the plain-text password using `passwordEncoder.encode(request.getPassword())` (`BCryptPasswordEncoder`) and saves the `User` with `Role.USER`.
2. **Login (`POST /api/auth/login`):**
   - `AuthController.login()` calls `authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password))`.
   - Spring Security calls `CustomUserDetailsService.loadUserByUsername(email)` and verifies the password hash with BCrypt.
   - `AuthController` stores the authenticated `SecurityContext` in the server-side `HttpSession` (`HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY`).
   - The browser receives an `HttpOnly` `JSESSIONID` session cookie. Passwords are never saved in `localStorage`.

---

## 8. Live-Data Flow: Fetching, Database Caching, Refresh Failures, and Live vs. Sample Events

1. **Adapter Interface (`EventSourceAdapter.java`):** Defines `isConfigured()` and `fetchUpcomingEvents()`.
2. **When Unconfigured (Default):** `ConfigurableRestEventSourceAdapter.isConfigured()` returns `false` unless `TECHPULSE_LIVE_SOURCE_ENABLED=true` and `TECHPULSE_LIVE_SOURCE_URL` are set. `LiveSourceSyncService` sets `status = "LIVE_SOURCE_NOT_CONFIGURED"`, and the UI displays **"Live source not configured"**.
3. **When Configured & Refreshed (`POST /api/sources/refresh`):**
   - `ConfigurableRestEventSourceAdapter` fetches the JSON feed via `java.net.http.HttpClient`, strips HTML tags (`sanitizeText`), and validates HTTP/HTTPS URLs (`validateHttpUrl`).
   - `LiveSourceSyncService` deduplicates each item by `(sourceName, externalEventId)` or fallback `(title, startTime, organizer)`, saves it in H2 with `DataSourceOrigin.LIVE_IMPORT`, and skips overwriting any event where `manuallyEdited == true`.
4. **During an Outage:** If the remote feed fails, `LiveSourceSyncService` catches the exception, preserves all previously cached events in H2, and sets `status = "STALE_CACHE"` (`staleData = true`).
5. **Honest Origin Labelling:** Every event carries `DataSourceOrigin` (`DEMO_SEED` $\rightarrow$ *"Demonstration Dataset"*, `MANUAL_ADMIN` $\rightarrow$ *"Manual Admin Entry"*, `LIVE_IMPORT` $\rightarrow$ *"Live Imported Source"*). Sample events are never described as live or verified.

---

## 9. Java Syllabus Connections (Actual Examples from This Project)

| Java Syllabus Topic | Exact File & Line/Method in TECHPULSE |
| :--- | :--- |
| **Classes & Objects** | `Event.java`, `User.java`, `Bookmark.java`, `Participation.java` in `com.techpulse.model` |
| **Encapsulation** | Private fields (`private BigDecimal price;`, `private boolean verifiedByAdmin;`) with public getters/setters in `Event.java` and `Participation.java` |
| **Interfaces & Polymorphism** | `EventSourceAdapter.java` interface implemented by `ConfigurableRestEventSourceAdapter.java` and injected into `LiveSourceSyncService.java` |
| **Enums with Methods** | `EventType.java` (`public boolean isCompetitive() { return this == HACKATHON || this == COMPETITION; }`) |
| **Java 17 Records** | `public record DateWindowBounds(String windowKey, OffsetDateTime startInclusive, OffsetDateTime endInclusive)` in `DateWindowService.java` |
| **Collections & Streams API** | `baseEvents.stream().filter(...).collect(Collectors.toList())` in `EventService.java`; `Set<Long>`, `Map<Long, Participation>`, `List<Event>` |
| **Java Time API (`java.time`)** | `ZoneId.of("Asia/Kolkata")`, `ZonedDateTime`, `OffsetDateTime`, `Duration`, `TemporalAdjusters` in `DateWindowService.java` and `EventService.java` |
| **Exact Decimal Math** | `java.math.BigDecimal` in `Event.java` and `EventService.matchesCost()` |
| **Exception Handling** | `try-catch` in `LiveSourceSyncService.triggerRefresh()` and `@ExceptionHandler` methods in `GlobalExceptionHandler.java` |

---

## 10. Viva Presentation Script (2-Minute Intro + 5-Minute Live Demo)

### Two-Minute Introduction
> "Good morning/afternoon Professor. My project for Full Stack Java Programming (2113611) is **TECHPULSE — Discover. Participate. Achieve.**  
> Engineering students often miss technology events or face schedule clashes because event details are scattered across social media. TECHPULSE solves this using a unified **Java 17 and Spring Boot 3.2.5** web application backed by a **file-based H2 relational database** and a clean **HTML5, CSS3, and Vanilla JavaScript** interface served directly from Spring Boot's static resources.  
> On the backend, Java services compute discovery date windows in the `Asia/Kolkata` timezone, filter events using `BigDecimal` for accurate pricing without treating unknown prices as free, detect schedule overlaps using interval math, and calculate non-cumulative participation credits and competition win rates with role-based Spring Security and faculty verification."

### Five-Minute Live Demonstration Steps
1. **Minute 1 — Theme Toggle, Source Transparency & Date Window Filtering:**
   - Show the **Explore Events** page. Point out the **Active Server Window (IST)** box and the **"Live source not configured"** transparency banner showing that initial events are clearly labelled `Demonstration Dataset`.
   - Toggle between **Bright Mode** and **Night Mode** in the header.
   - Filter by **Date Window = This Weekend (Sat–Sun)** and **City = Mumbai**, then click **Reset**.
2. **Minute 2 — Event Details, `.ics` Calendar Download & WhatsApp Share:**
   - Click **Details** on *"Mumbai FinTech & AI Systems Hackathon 2026"*.
   - Click **Add to Calendar (.ics)** to download the RFC 5545 calendar file and **Copy Event Link**.
3. **Minute 3 — Login, Saved Events & Schedule Conflict Detection:**
   - Click **Login / Register**, click **Fill Student Demo (`student@techpulse.edu.in`)**, and sign in.
   - Open **My Saved Events**. Show the red **Schedule Overlap Warning** detecting the clash between the Saturday Mumbai Hackathon and the Navi Mumbai CTF Competition (`startA < endB && startB < endA`), and the yellow **"Cannot check overlap"** badge on the workshop without an end time.
4. **Minute 4 — Participation Credits, Badges & Competition Win Rate:**
   - Open **My Activity**. Show the **Builder** badge (`95 credits`), the **50.0% (1 / 2)** competition win rate (explaining why the attended Linux workshop is excluded from the denominator), and update an event's status from `Registered` (5 Cr) to `Attended` (20 Cr) to prove credits are non-cumulative and idempotent.
5. **Minute 5 — Admin Dashboard, Validation & Soft Deletion:**
   - Log out, sign in with **Fill Admin Demo (`admin@techpulse.edu.in`)**, and open **Admin Dashboard**.
   - Show the **Verify Student Participation** table and toggle verification on a student record.
   - Show **Soft Delete** confirmation on an event, explaining how `deleted = true` preserves student bookmarks and credit history.

---

## 11. Twenty Viva Questions Tied to This Implementation
*(See `VIVA_QUESTIONS.md` Part B for the complete set of 20 implementation-specific questions and concise answers.)*

---

## 12. Honest Project Limitations

1. **Not a Global Event Search Engine:** TECHPULSE only displays events from configured JSON feeds, manual administrator entries, and the clearly labelled academic demonstration dataset.
2. **Self-Reported Attendance Until Faculty Verification:** Opening an external registration URL cannot automatically verify completion on third-party websites; therefore, student updates are marked **Self-Reported** until verified by an administrator (`ROLE_ADMIN`).
3. **Overlap Detection Requires Known End Times:** When an organiser omits `endTime`, the backend reports `"Cannot check overlap"` rather than guessing during conflict checks.
