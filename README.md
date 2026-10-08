# TECHPULSE — Discover. Participate. Achieve.

**Course Name:** Full Stack Java Programming  
**Course Code:** 2113611  
**Technology Stack:** Java 17 · Spring Boot 3.2.5 · Spring Data JPA · Spring Security · H2 File Database · HTML5 · Plain CSS3 · Vanilla JavaScript

---

## 1. Required Software

To compile, test, and run **TECHPULSE** locally on your computer, install:

1. **Java Development Kit (JDK) 17** (or compatible JDK 17+ such as Eclipse Temurin 17 or Oracle JDK 17).
   - Verify in Command Prompt / PowerShell:
     ```bat
     java -version
     ```
2. **Apache Maven 3.8+**
   - Verify in Command Prompt / PowerShell:
     ```bat
     mvn -version
     ```
3. **Modern Web Browser** (Google Chrome, Microsoft Edge, or Mozilla Firefox).

---

## 2. Exact Commands to Run on Windows

Open **Command Prompt (`cmd.exe`)** or **Windows PowerShell**, navigate to the project root folder (where `pom.xml` is located), and run:

### Step 1: Compile the Java Application
```bat
mvn clean compile
```

### Step 2: Run the Automated JUnit 5 Verification Tests
```bat
mvn test
```

### Step 3: Start the Spring Boot Server
```bat
mvn spring-boot:run
```

Alternatively, you can package and run the executable JAR:
```bat
mvn clean package -DskipTests
java -jar target\techpulse-app-1.0.0.jar
```

---

## 3. How to Open the Application

Once `mvn spring-boot:run` reports `Started TechPulseApplication`, open your browser and visit:

- **Main Application (Frontend + REST Backend):**  
  `http://localhost:8080`
- **H2 Database Web Console (For Viva Database Inspection):**  
  `http://localhost:8080/h2-console`
  - **JDBC URL:** `jdbc:h2:file:./data/techpulse_db;AUTO_SERVER=TRUE;DB_CLOSE_ON_EXIT=FALSE`
  - **User Name:** `sa`
  - **Password:** *(leave empty)*

---

## 4. Development-Only Demo Accounts

When the application starts with an empty database (`techpulse.demo.seed-enabled=true`), `DemoDataSeeder.java` automatically creates two development-only accounts for lab evaluation and viva demonstration:

| Role | Email Address | Password | Pre-seeded State |
| :--- | :--- | :--- | :--- |
| **Student (`ROLE_USER`)** | `student@techpulse.edu.in` | `Student@123` | Has 4 saved events (including an intentional Saturday schedule clash and an incomplete-time event) and 4 participation records (95 credits, `Builder` badge, `50.0% (1 / 2)` competition win rate). |
| **Administrator (`ROLE_ADMIN`)** | `admin@techpulse.edu.in` | `Admin@123` | Full access to the **Admin Dashboard** (`/api/admin/**`) to add/edit/cancel/soft-delete events, trigger live source refreshes, and verify student participation records. |

> **Security Note:** These accounts are strictly for academic demonstration and local testing. Passwords are hashed using Spring Security's `BCryptPasswordEncoder` and are never stored in browser `localStorage` or `sessionStorage`.

---

## 5. Live-Source Configuration

TECHPULSE implements a clean adapter architecture (`com.techpulse.source.EventSourceAdapter`) with `ConfigurableRestEventSourceAdapter.java`.

By default, no external API keys are hardcoded, and the UI transparently displays **"Live source not configured"**.

To connect a live JSON event feed on Windows Command Prompt before starting the server:
```bat
set TECHPULSE_LIVE_SOURCE_ENABLED=true
set TECHPULSE_LIVE_SOURCE_URL=https://your-documented-event-feed.example.org/events.json
set TECHPULSE_LIVE_SOURCE_API_KEY=your_optional_bearer_token
mvn spring-boot:run
```

Or in Windows PowerShell:
```powershell
$env:TECHPULSE_LIVE_SOURCE_ENABLED="true"
$env:TECHPULSE_LIVE_SOURCE_URL="https://your-documented-event-feed.example.org/events.json"
mvn spring-boot:run
```

- When configured, clicking **Refresh Source** fetches events from `TECHPULSE_LIVE_SOURCE_URL` using Java's `HttpClient`, sanitizes external descriptions as plain text, validates HTTP/HTTPS URLs, deduplicates records by `(sourceName, externalEventId)` (or conservative fallback `title + startTime + organizer`), and caches them in H2.
- If the remote endpoint fails during a later refresh, `LiveSourceSyncService` serves previously cached events from H2 with a clear **Stale Data Cache Active** indicator.

---

## 6. Data Persistence & Future MySQL Migration

### File-Based H2 Persistence
The database is stored on disk at `./data/techpulse_db.mv.db` (`jdbc:h2:file:./data/techpulse_db`).
- Any accounts registered, events added/edited by admin, bookmarks saved, or participation credits recorded **survive server restarts**.
- To reset the database to a clean initial demo state, stop the server, delete the `./data` folder, and run `mvn spring-boot:run` again.

### Future MySQL Migration
`src/main/resources/application.properties` uses environment variable placeholders so you can switch to MySQL without changing Java code:
```bat
set SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/techpulse_db
set SPRING_DATASOURCE_DRIVER=com.mysql.cj.jdbc.Driver
set SPRING_DATASOURCE_USERNAME=root
set SPRING_DATASOURCE_PASSWORD=your_mysql_password
set SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.MySQLDialect
mvn spring-boot:run
```

---

## 7. Troubleshooting

1. **Port 8080 is already in use:**
   Run on a custom port such as `8081`:
   ```bat
   set PORT=8081
   mvn spring-boot:run
   ```
2. **`'mvn' is not recognized as an internal or external command`:**
   Add your Apache Maven `bin` folder (e.g., `C:\Program Files\Apache\maven\bin`) to your Windows `PATH` environment variable, and ensure `JAVA_HOME` points to your JDK 17 installation.
3. **H2 Database Lock Error (`Database may be already in use`):**
   Ensure another instance of `TechPulseApplication` is not already running in a second terminal window. Note that `AUTO_SERVER=TRUE` is enabled in the JDBC URL to allow simultaneous H2 Console connections.

---

## 8. Which Features Use Real Data vs. Demonstration Data

| Feature / Component | Data Type | Explanation |
| :--- | :--- | :--- |
| **Java Date Window Calculations (`DateWindowService`)** | **Real Server Computation** | Dynamically computed in Java using `ZoneId.of("Asia/Kolkata")` (`TODAY`, `THIS_WEEKEND`, `NEXT_7_DAYS`, `NEXT_30_DAYS`). |
| **Authentication, BCrypt & CSRF (`SecurityConfig`, `AuthController`)** | **Real Spring Security** | Real BCrypt password hashing, HTTP sessions (`JSESSIONID`), CSRF cookies (`XSRF-TOKEN`), and backend `ROLE_ADMIN` enforcement. |
| **Bookmarks, Schedule Conflict Detection & Participation Credits** | **Real H2 Database Persistence** | Stored in `bookmarks` and `participations` tables in `./data/techpulse_db.mv.db`. Interval overlaps and win rates are calculated by Java services. |
| **Calendar `.ics` Export (`CalendarIcsService`)** | **Real RFC 5545 File Generation** | Dynamically generated downloadable `.ics` file with proper text escaping and UTC/IST timestamps. |
| **Initial 12 Seeded Events (`DemoDataSeeder`)** | **Demonstration Dataset** | Clearly labelled in the UI as **`Demonstration Dataset`** (`DataSourceOrigin.DEMO_SEED`) with dates anchored relative to the current server date so filters and conflict demos work immediately. |
| **Admin-Created Events (`AdminController`)** | **Real Manual User Data** | Labelled in the UI as **`Manual Admin Entry`** (`DataSourceOrigin.MANUAL_ADMIN`). |
| **External Feed Sync (`ConfigurableRestEventSourceAdapter`)** | **Real HTTP Client / Configurable** | Shows **`Live source not configured`** until `TECHPULSE_LIVE_SOURCE_URL` is provided; imported events are labelled **`Live Imported Source`**. |
