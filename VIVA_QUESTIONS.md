# VIVA PREPARATION GUIDE — TECHPULSE (Course Code: 2113611)

This guide explains the core concepts behind **TECHPULSE — Discover. Participate. Achieve.** in clear, beginner-friendly language so you can confidently answer questions during your Full Stack Java Programming lab viva.

---

## Part A: Core Concept Explanations

### 1. What is Spring Boot and why did we use it?
- **Simple Explanation:** Spring Boot is a Java framework that makes it fast and easy to build web applications and REST APIs without writing complex XML configuration files.
- **How TECHPULSE uses it:**
  - `@SpringBootApplication` in `TechPulseApplication.java` starts an embedded Apache Tomcat web server on port `8080`.
  - It automatically serves our frontend files (`index.html`, `styles.css`, `app.js`) from `src/main/resources/static/` and routes API calls (`/api/**`) to our Java controller classes.

### 2. What is a REST API and how does the frontend talk to the backend?
- **Simple Explanation:** A REST API (Representational State Transfer Application Programming Interface) allows the browser (JavaScript) and the server (Java) to exchange data using standard HTTP methods (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`) and JSON text.
- **How TECHPULSE uses it:**
  - When a student selects filters on the Explore page, `app.js` calls `fetch('/api/events?city=Mumbai&dateWindow=NEXT_30_DAYS')`.
  - `EventController.java` receives the `GET` request, asks `EventService.java` for matching events, and returns a JSON object (`PaginatedEventResponse`) which `app.js` renders into HTML cards.

### 3. What is the difference between JDBC, JPA, Hibernate, and Spring Data JPA?
- **Simple Explanation:**
  - **JDBC (Java Database Connectivity):** The low-level Java API where you manually open connections and write raw SQL strings like `SELECT * FROM events`.
  - **JPA (Jakarta Persistence API):** A standard specification that lets you map Java classes (`@Entity`) directly to database tables.
  - **Hibernate:** The engine (ORM — Object-Relational Mapper) that implements JPA and automatically translates Java objects into SQL queries.
  - **Spring Data JPA:** A layer on top of JPA where you only write an interface like `public interface EventRepository extends JpaRepository<Event, Long>`, and Spring automatically generates the SQL queries for saving, deleting, and finding rows.

### 4. How does file-based H2 database persistence work?
- **Simple Explanation:** H2 is a lightweight relational SQL database written in Java. If configured in *in-memory* mode (`jdbc:h2:mem:`), all data disappears when the server stops. In TECHPULSE, we configured **file-based mode** in `application.properties`:
  ```properties
  spring.datasource.url=jdbc:h2:file:./data/techpulse_db;AUTO_SERVER=TRUE;DB_CLOSE_ON_EXIT=FALSE
  ```
- **Result:** All users, events, bookmarks, and participation credits are saved to `./data/techpulse_db.mv.db` on your hard drive and survive server restarts.

### 5. How are Java OOP Principles (Classes, Objects, Encapsulation, Interfaces, Inheritance, Polymorphism) used in TECHPULSE?
- **Classes & Objects:** `Event`, `User`, `Bookmark`, and `Participation` are classes (`src/main/java/com/techpulse/model/`). Every row loaded from the database is an object instance of these classes.
- **Encapsulation:** All entity fields (such as `private BigDecimal price;` in `Event.java` or `private String passwordHash;` in `User.java`) are `private` and accessed only through controlled `public` getter and setter methods.
- **Abstraction & Interfaces:** `EventSourceAdapter.java` (`com.techpulse.source.EventSourceAdapter`) is a Java `interface` defining `fetchUpcomingEvents()`. `LiveSourceSyncService` depends on the `EventSourceAdapter` interface rather than a hardcoded class, so new event sources can be plugged in without modifying controller or service code (**Polymorphism**).
- **Inheritance:** Our repositories (`EventRepository`, `UserRepository`) extend `JpaRepository`, inheriting methods like `save()`, `findById()`, and `count()`.

### 6. How does Validation work on both Frontend and Backend?
- **Simple Explanation:** Never trust browser input alone because someone can bypass HTML forms using tools like Postman or `curl`.
- **How TECHPULSE uses it:**
  - **DTO Bean Validation:** Request classes in `AuthDtos.java` and `EventDtos.java` use annotations like `@NotBlank`, `@Email`, `@Size`, and `@DecimalMin("0.0")`.
  - **Business Rule Validation:** `EventService.validateEventTimingsAndCost()` checks that `endTime` is not before `startTime`, `registrationDeadline` is not after `startTime`, and paid events have a positive `BigDecimal` price.
  - **Global Exception Handling:** `GlobalExceptionHandler.java` (`@RestControllerAdvice`) catches `MethodArgumentNotValidException` and `IllegalArgumentException` and returns clean HTTP `400 Bad Request` JSON messages.

### 7. How do Authentication, BCrypt, Roles, and CSRF Protection work?
- **BCrypt Password Hashing:** Plain-text passwords are never stored in the database. `SecurityConfig.java` defines a `BCryptPasswordEncoder` bean that hashes passwords with a random salt (e.g., `$2a$10$...`).
- **Server-Side Sessions:** When a user logs in via `POST /api/auth/login`, Spring Security stores the authenticated `SecurityContext` in an `HttpSession` on the server and sends an `HttpOnly` `JSESSIONID` cookie to the browser.
- **Role-Based Access Control:** Users have `Role.USER` or `Role.ADMIN`. `SecurityConfig.java` and `@PreAuthorize("hasRole('ADMIN')")` in `AdminController.java` ensure that only administrators can call `/api/admin/**`.
- **CSRF Protection:** Cross-Site Request Forgery protection is enabled using `CookieCsrfTokenRepository.withHttpOnlyFalse()`. Our Vanilla JS helper `apiFetch()` in `app.js` reads the `XSRF-TOKEN` cookie and attaches it in the `X-XSRF-TOKEN` HTTP header on every `POST`, `PUT`, `PATCH`, and `DELETE` request.

### 8. How does Java Date Window Filtering work (`DateWindowService.java`)?
- All date windows are computed on the Java backend in the `Asia/Kolkata` (`IST`, `+05:30`) timezone using `ZonedDateTime` and `OffsetDateTime`:
  - **`TODAY`:** Today `00:00:00+05:30` to `23:59:59.999999999+05:30` (inclusive).
  - **`THIS_WEEKEND`:** If today is Monday–Friday, it selects the upcoming Saturday `00:00:00+05:30` through Sunday `23:59:59.999999999+05:30` using `TemporalAdjusters.next(DayOfWeek.SATURDAY)`. If today is already Saturday or Sunday, it selects the current weekend's Saturday through Sunday.
  - **`NEXT_7_DAYS`:** Current instant `now` through `today + 7 days` at `23:59:59.999999999+05:30`.
  - **`NEXT_30_DAYS` (Default):** Current instant `now` through `today + 30 days` at `23:59:59.999999999+05:30`.

### 9. How does Schedule Overlap Detection work (`ScheduleConflictService.java`)?
- Comparing only dates is wrong because a morning event (9 AM–11 AM) and an afternoon event (2 PM–4 PM) on the same day do not clash.
- Instead, `ScheduleConflictService.intervalsOverlap()` checks exact time intervals:
  ```java
  return startA.isBefore(endB) && startB.isBefore(endA);
  ```
- If either event is missing its `endTime`, the service does **not** guess or invent a conflict; it sets the status to `CANNOT_CHECK_OVERLAP` ("Cannot check overlap — end time not provided"). Cancelled events are also skipped.

### 10. How are Participation Credits and Competition Win Rate calculated (`ParticipationService.java`)?
- **Non-Cumulative Credit Rules:**
  - `SAVED`: `0` credits
  - `REGISTERED`: `5` credits
  - `ATTENDED`: `20` total credits
  - `ATTENDED` + `WON` (on a `HACKATHON` or `COMPETITION`): `50` total credits
- **Why Non-Cumulative & Idempotent:** Each `(user_id, event_id)` pair has at most one row in the `participations` table (`uk_participations_user_event`). When a student updates an event from `REGISTERED` (5) to `ATTENDED` (20), that row's credit value changes from `5` to `20` (not `25`). Clicking "Save" ten times updates the same row and recalculates totals from current database rows, so extra credits are never awarded.
- **Competition Win Rate Formula:**
  $$\text{Win Rate} = \frac{\text{Wins}}{\text{Completed Hackathons/Competitions with Won or Not Won outcome}} \times 100$$
  Workshops, conferences, webinars, `REGISTERED`-only records, and `PENDING` outcomes are excluded from both numerator and denominator. If the denominator is `0`, the backend returns `"N/A"` with a clear explanation.

---

## Part B: 20 Quick Viva Questions & Model Answers

1. **Q: Why did you use `BigDecimal` instead of `double` or `float` for event prices in `Event.java`?**  
   **A:** Binary floating-point types (`double` and `float`) cannot represent base-10 decimals like `0.10` exactly and cause rounding errors in financial calculations. `java.math.BigDecimal` stores exact decimal precision (`DECIMAL(10,2)` in SQL).

2. **Q: How does TECHPULSE handle events where the organiser did not mention the ticket price?**  
   **A:** We use the `CostType` enum with three values: `FREE`, `PAID`, and `NOT_PROVIDED`. Unknown prices are stored as `NOT_PROVIDED` with `price = null`, displayed as `"Price not provided"`, and excluded when a user filters for `FREE` events.

3. **Q: What happens to a student's bookmarks and earned credits if an administrator deletes an event?**  
   **A:** In `EventService.softDeleteEventByAdmin()`, we perform a **soft delete** by setting `event.setDeleted(true)` rather than running a SQL `DELETE`. This hides the event from public discovery while keeping foreign keys in `bookmarks` and `participations` intact.

4. **Q: How do you prevent a student from bookmarking the same event twice?**  
   **A:** First, `EventService.addBookmark()` checks `bookmarkRepository.existsByUserIdAndEventId(userId, eventId)`. Second, the `bookmarks` table enforces a database-level `@UniqueConstraint` (`uk_bookmarks_user_event`) on `(user_id, event_id)`.

5. **Q: How do you ensure Student A cannot modify Student B's participation records?**  
   **A:** `ParticipationController` never accepts a `userId` from the request body. Instead, it extracts the logged-in user's email directly from Spring Security's server-side `Authentication` object (`requireUserId(authentication)`).

6. **Q: Why does clicking "Open Official Registration" not automatically award 5 registration credits?**  
   **A:** Because an external link (`target="_blank"`) opens another website (such as KonfHub or Devfolio), and our server has no way of knowing if the student actually completed the external form. Therefore, students explicitly record their status as **Self-Reported**, and faculty administrators can verify it (`verifiedByAdmin = true`).

7. **Q: How is the `.ics` calendar file generated when a student clicks "Add to Calendar"?**  
   **A:** `EventController.downloadCalendarIcs()` calls `CalendarIcsService.generateIcsForEvent()`, which builds an RFC 5545 `VCALENDAR` / `VEVENT` text stream with `Content-Type: text/calendar`, escapes special characters (`\`, `;`, `,`, newlines), and explicitly notes a 2-hour assumed duration in the `DESCRIPTION` if `endTime` is null.

8. **Q: What HTTP status codes does your GlobalExceptionHandler return?**  
   **A:** `400 Bad Request` for Bean Validation or `IllegalArgumentException`, `401 Unauthorized` for invalid login credentials or unauthenticated access, `403 Forbidden` when a non-admin accesses `/api/admin/**`, and `404 Not Found` for `NoSuchElementException`.

9. **Q: How does the theme toggle work and remember the user's choice?**  
   **A:** `styles.css` defines CSS variables under `[data-theme="bright"]` and `[data-theme="night"]`. On first visit, `initTheme()` in `app.js` checks `localStorage.getItem('techpulse_theme')`; if none is saved, it checks `window.matchMedia('(prefers-color-scheme: dark)')`. Toggling saves the new choice to `localStorage`.

10. **Q: How do you protect against Cross-Site Scripting (XSS) when displaying external event descriptions?**  
    **A:** On the backend, `ConfigurableRestEventSourceAdapter.sanitizeText()` strips HTML tags and `validateHttpUrl()` rejects non-`http(s)` URLs (blocking `javascript:` links). On the frontend, `app.js` uses `escapeHtml()` and assigns event descriptions via `element.textContent`.

11. **Q: What is the purpose of `@Transactional` in `EventService` and `ParticipationService`?**  
    **A:** `@Transactional` ensures that database operations inside a method execute as a single atomic unit of work: if an exception occurs mid-method, Hibernate rolls back all changes automatically.

12. **Q: How does deduplication work when refreshing events from a live source?**  
    **A:** `LiveSourceSyncService.findExistingEvent()` first looks up `(sourceName, externalEventId)`. If `externalEventId` is not provided by the feed, it uses a conservative fallback matching `(title, startTime, organizer)` case-insensitively.

13. **Q: What happens during a live source refresh if an admin had previously edited an imported event?**  
    **A:** When an admin edits an event, `event.setManuallyEdited(true)` is set. During refresh, `LiveSourceSyncService` checks `if (!existing.isManuallyEdited())` before updating fields, protecting manual admin corrections from being overwritten.

14. **Q: What happens if the live event source URL is unreachable or times out during a refresh?**  
    **A:** `LiveSourceSyncService.triggerRefresh()` catches the exception, keeps all previously cached events in the H2 database, sets `status = "STALE_CACHE"` and `staleData = true` in `SourceRefreshLog`, and displays a stale-data warning in the UI.

15. **Q: Why are Workshops, Conferences, and Webinars excluded from the Competition Win Rate?**  
    **A:** Because workshops, conferences, and webinars are learning sessions rather than win/loss contests. Including them in the denominator would artificially lower a student's competitive win rate.

16. **Q: What are the four engagement badge tiers in TECHPULSE?**  
    **A:** `Explorer` (0–49 credits), `Builder` (50–149 credits), `Challenger` (150–299 credits), and `Champion` (300+ credits). They are explicitly labelled as application engagement badges rather than official certifications.

17. **Q: How does `CommandLineRunner` work in `DemoDataSeeder.java`?**  
    **A:** Spring Boot automatically runs `DemoDataSeeder.run()` after the application context and database schema initialize. It checks `if (userRepository.count() > 0) return;` so demo data is only inserted once when the H2 database file is brand new.

18. **Q: What is a DTO and why didn't you expose JPA Entities directly in all requests?**  
    **A:** A DTO (Data Transfer Object) separates the external JSON API structure from internal database columns (for example, hiding `User.passwordHash` in `UserProfileResponse` and adding computed fields like `countdownLabel` and `priceDisplay` in `EventResponse`).

19. **Q: How can this project be migrated from H2 to MySQL in production?**  
    **A:** `pom.xml` includes `mysql-connector-j` and `application.properties` reads `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_DRIVER`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, and `SPRING_JPA_DATABASE_PLATFORM` from environment variables, requiring zero Java code changes.

20. **Q: How does the WhatsApp share button work in `app.js`?**  
    **A:** It constructs the application's deep link (`window.location.origin + '/#event/' + ev.id`), builds a message string with the event title and IST start time, encodes it with `encodeURIComponent()`, and links to `https://wa.me/?text=...`.
