package com.techpulse.config;

import com.techpulse.model.*;
import com.techpulse.repository.*;
import com.techpulse.service.DateWindowService;
import com.techpulse.service.LiveSourceSyncService;
import com.techpulse.service.ParticipationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * Seeds development-only demo accounts and a clearly labelled Demonstration Dataset
 * if the H2 database is empty on first boot.
 *
 * Because H2 is configured in file mode (./data/techpulse_db), records persist across
 * server restarts and seeding is skipped on subsequent runs.
 */
@Component
public class DemoDataSeeder implements CommandLineRunner {

    private final boolean seedEnabled;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final BookmarkRepository bookmarkRepository;
    private final ParticipationRepository participationRepository;
    private final PasswordEncoder passwordEncoder;
    private final DateWindowService dateWindowService;
    private final ParticipationService participationService;
    private final LiveSourceSyncService liveSourceSyncService;

    public DemoDataSeeder(
            @Value("${techpulse.demo.seed-enabled:true}") boolean seedEnabled,
            UserRepository userRepository,
            EventRepository eventRepository,
            BookmarkRepository bookmarkRepository,
            ParticipationRepository participationRepository,
            PasswordEncoder passwordEncoder,
            DateWindowService dateWindowService,
            ParticipationService participationService,
            LiveSourceSyncService liveSourceSyncService
    ) {
        this.seedEnabled = seedEnabled;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.participationRepository = participationRepository;
        this.passwordEncoder = passwordEncoder;
        this.dateWindowService = dateWindowService;
        this.participationService = participationService;
        this.liveSourceSyncService = liveSourceSyncService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        liveSourceSyncService.getOrInitializeStatus();

        if (!seedEnabled || userRepository.count() > 0) {
            return;
        }

        OffsetDateTime now = dateWindowService.nowInKolkata();
        LocalDate today = now.toLocalDate();

        // 1. Seed Demo Accounts (Development Only)
        User student = new User(
                "Aarav Deshmukh",
                "student@techpulse.edu.in",
                passwordEncoder.encode("Student@123"),
                "Sardar Patel Institute of Technology, Mumbai",
                Role.USER,
                now
        );
        userRepository.save(student);

        User secondStudent = new User(
                "Priya Kulkarni",
                "priya.k@techpulse.edu.in",
                passwordEncoder.encode("Student@123"),
                "Fr. C. Rodrigues Institute of Technology, Navi Mumbai",
                Role.USER,
                now
        );
        userRepository.save(secondStudent);

        User admin = new User(
                "Prof. Rajeshwari Nair (Faculty Coordinator)",
                "admin@techpulse.edu.in",
                passwordEncoder.encode("Admin@123"),
                "Department of Computer Engineering",
                Role.ADMIN,
                now
        );
        userRepository.save(admin);

        // Compute upcoming weekend dates for deterministic "This Weekend" demonstration
        LocalDate upcomingSaturday = (today.getDayOfWeek() == DayOfWeek.SATURDAY)
                ? today
                : (today.getDayOfWeek() == DayOfWeek.SUNDAY ? today.minusDays(1) : today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY)));
        LocalDate upcomingSunday = upcomingSaturday.plusDays(1);

        // Event 1: Today (Online Java Workshop - Free)
        Event e1 = buildDemoEvent(
                "Spring Boot 3 & JPA Production Architecture Lab",
                "Hands-on technical workshop covering Spring Boot 3.2 REST controllers, Spring Data JPA entity design, H2/MySQL migration, and BCrypt session security for undergraduate engineering students.",
                "Java",
                EventType.WORKSHOP,
                EventMode.ONLINE,
                "Mumbai Java User Group (MUJUG)",
                "Online",
                "Live Interactive Session (Zoom + GitHub Codespaces)",
                atKolkata(today, 18, 0),
                atKolkata(today, 20, 30),
                atKolkata(today, 17, 0),
                CostType.FREE,
                BigDecimal.ZERO,
                "Open to B.E. / B.Tech / MCA / B.Sc IT students across India",
                "https://konfhub.com/mujug-spring-boot-lab-demo",
                now
        );
        eventRepository.save(e1);

        // Event 2: This Weekend Saturday (Mumbai Hackathon - Free, overlaps with Event 3 for conflict demo!)
        Event e2 = buildDemoEvent(
                "Mumbai FinTech & AI Systems Hackathon 2026",
                "24-hour inter-collegiate engineering hackathon focused on fraud detection pipelines, LLM guardrails, and low-latency payment reconciliation systems.",
                "AI/ML",
                EventType.HACKATHON,
                EventMode.OFFLINE,
                "IIT Bombay E-Cell & Techfest Guild",
                "Mumbai",
                "Convocation Hall Annex, Powai, Mumbai 400076",
                atKolkata(upcomingSaturday, 9, 30),
                atKolkata(upcomingSunday, 11, 0),
                atKolkata(upcomingSaturday.minusDays(1), 23, 59),
                CostType.FREE,
                BigDecimal.ZERO,
                "Teams of 2 to 4 undergraduate engineering students",
                "https://devfolio.co/mumbai-fintech-ai-hack-demo",
                now
        );
        eventRepository.save(e2);

        // Event 3: This Weekend Saturday (Navi Mumbai Cybersecurity Competition - Paid, overlaps with Event 2!)
        Event e3 = buildDemoEvent(
                "Navi Mumbai Capture The Flag (CTF) & Zero-Trust Arena",
                "Jeopardy-style and Attack-Defense cybersecurity competition testing web exploitation, binary reverse engineering, and container hardening.",
                "Cybersecurity",
                EventType.COMPETITION,
                EventMode.HYBRID,
                "OWASP Navi Mumbai Student Chapter",
                "Navi Mumbai",
                "CIDCO Exhibition & Convention Centre, Vashi, Navi Mumbai 400703",
                atKolkata(upcomingSaturday, 11, 0),
                atKolkata(upcomingSaturday, 17, 30),
                atKolkata(upcomingSaturday.minusDays(1), 18, 0),
                CostType.PAID,
                new BigDecimal("250.00"),
                "Individual or 2-member student teams with valid college ID",
                "https://ctfd.io/owasp-navi-mumbai-demo",
                now
        );
        eventRepository.save(e3);

        // Event 4: Next 7 Days (Mumbai Web Development Workshop - Incomplete endTime for 'Cannot check overlap' demo)
        Event e4 = buildDemoEvent(
                "Modern Web Standards: Accessible Vanilla JS & CSS Grid Systems",
                "Deep dive into building responsive, high-contrast web dashboards without heavy frontend frameworks, focusing on semantic HTML5, CSS custom properties, and Fetch API patterns.",
                "Web Development",
                EventType.WORKSHOP,
                EventMode.OFFLINE,
                "VJTI Technovanza Developer Community",
                "Mumbai",
                "Main Auditorium, Veermata Jijabai Technological Institute, Matunga, Mumbai",
                atKolkata(today.plusDays(4), 14, 0),
                null, // Intentionally null endTime to demonstrate "Cannot check overlap"
                atKolkata(today.plusDays(3), 20, 0),
                CostType.FREE,
                BigDecimal.ZERO,
                "All engineering & diploma students",
                "https://technovanza.org/vanilla-web-workshop-demo",
                now
        );
        eventRepository.save(e4);

        // Event 5: Next 7 Days (Navi Mumbai Data Science Conference - Unknown Price demo)
        Event e5 = buildDemoEvent(
                "Western India Data Engineering & Apache Iceberg Summit",
                "Technical conference featuring talks on real-time stream processing, lakehouse indexing, and reproducible ML pipelines.",
                "Data Science",
                EventType.CONFERENCE,
                EventMode.OFFLINE,
                "Navi Mumbai Cloud & Data Collective",
                "Navi Mumbai",
                "Reliance Corporate Park Convention Hall, Ghansoli, Navi Mumbai",
                atKolkata(today.plusDays(6), 10, 0),
                atKolkata(today.plusDays(6), 17, 0),
                atKolkata(today.plusDays(5), 18, 0),
                CostType.NOT_PROVIDED, // Explicitly demonstrates "Price not provided" (never treated as Free)
                null,
                "Final-year students, postgraduate researchers, and developers",
                "https://konfhub.com/western-india-data-summit-demo",
                now
        );
        eventRepository.save(e5);

        // Event 6: Next 30 Days (Pune Java & Cloud Competition - Paid)
        Event e6 = buildDemoEvent(
                "Deccan Systems Programming & JVM Benchmarking Challenge",
                "Algorithmic and systems optimization competition focused on garbage collection tuning, concurrent data structures in Java 17, and SQL query optimization.",
                "Java",
                EventType.COMPETITION,
                EventMode.HYBRID,
                "COEP Technological University ACM Chapter",
                "Pune",
                "COEP Main Campus, Shivajinagar, Pune & Online Judge",
                atKolkata(today.plusDays(12), 10, 0),
                atKolkata(today.plusDays(12), 18, 0),
                atKolkata(today.plusDays(10), 23, 59),
                CostType.PAID,
                new BigDecimal("150.00"),
                "Undergraduate CS/IT students across Maharashtra",
                "https://acm.coep.org.in/jvm-challenge-demo",
                now
        );
        eventRepository.save(e6);

        // Event 7: Next 30 Days (Online AI/ML Webinar - Free)
        Event e7 = buildDemoEvent(
                "Demystifying Vector Databases & Retrieval-Augmented Generation",
                "Technical webinar walking through HNSW indexing, cosine similarity mathematics, and grounding enterprise search systems.",
                "AI/ML",
                EventType.WEBINAR,
                EventMode.ONLINE,
                "ACM India Eminent Speaker Program",
                "Online",
                "ACM India Webinar Portal",
                atKolkata(today.plusDays(18), 16, 30),
                atKolkata(today.plusDays(18), 18, 0),
                atKolkata(today.plusDays(18), 15, 0),
                CostType.FREE,
                BigDecimal.ZERO,
                "Open to all students and faculty members",
                "https://india.acm.org/webinars/vector-db-demo",
                now
        );
        eventRepository.save(e7);

        // Event 8: Next 30 Days (Bengaluru Hackathon - Free)
        Event e8 = buildDemoEvent(
                "National Open-Source Civic Tech Hackathon",
                "Build open-source digital public infrastructure tools for urban transit, accessible healthcare triage, and water grid telemetry.",
                "Web Development",
                EventType.HACKATHON,
                EventMode.HYBRID,
                "FOSS United & IISc Open Systems Lab",
                "Bengaluru",
                "BIC Domlur, Bengaluru & Remote Tracks",
                atKolkata(today.plusDays(24), 9, 0),
                atKolkata(today.plusDays(25), 17, 0),
                atKolkata(today.plusDays(20), 23, 59),
                CostType.FREE,
                BigDecimal.ZERO,
                "Open to college teams (1–4 members)",
                "https://fossunited.org/civic-hackathon-demo",
                now
        );
        eventRepository.save(e8);

        // Event 9: Next 30 Days (Cancelled Event Demo)
        Event e9 = buildDemoEvent(
                "Hardware IoT & Embedded Rust Bootcamp (Postponed)",
                "Hands-on embedded systems workshop with ESP32 microcontrollers. Marked cancelled to demonstrate cancelled status badge and conflict exclusion.",
                "Other",
                EventType.WORKSHOP,
                EventMode.OFFLINE,
                "Mumbai Makerspace Collective",
                "Mumbai",
                "Andheri East Electronics Lab, Mumbai",
                atKolkata(today.plusDays(15), 10, 0),
                atKolkata(today.plusDays(15), 16, 0),
                atKolkata(today.plusDays(12), 18, 0),
                CostType.PAID,
                new BigDecimal("499.00"),
                "Students with basic C/Rust knowledge",
                "https://mumbaimakers.in/iot-bootcamp-demo",
                now
        );
        e9.setCancelled(true);
        eventRepository.save(e9);

        // Past Events (for demonstrating Saved Past Events, Attended Last 30 Days, Credits, and Win Rate)
        Event pastHackWon = buildDemoEvent(
                "Smart Campus Energy Optimization Hackathon",
                "36-hour inter-college hackathon on smart grid load balancing and anomaly detection.",
                "AI/ML",
                EventType.HACKATHON,
                EventMode.OFFLINE,
                "Sardar Patel Institute of Technology",
                "Mumbai",
                "SPIT Campus, Bhavan's Campus, Munshi Nagar, Andheri West, Mumbai",
                atKolkata(today.minusDays(14), 9, 0),
                atKolkata(today.minusDays(13), 18, 0),
                atKolkata(today.minusDays(16), 23, 59),
                CostType.FREE,
                BigDecimal.ZERO,
                "Undergraduate engineering teams",
                "https://spit.ac.in/smart-energy-hack-demo",
                now
        );
        eventRepository.save(pastHackWon);

        Event pastCompNotWon = buildDemoEvent(
                "All-Mumbai Collegiate Algorithmic Coding Cup",
                "Competitive programming contest with 8 ICPC-style graph and dynamic programming problems.",
                "Java",
                EventType.COMPETITION,
                EventMode.OFFLINE,
                "DJ Sanghvi ACM Student Chapter",
                "Mumbai",
                "Vile Parle West, Mumbai",
                atKolkata(today.minusDays(9), 10, 0),
                atKolkata(today.minusDays(9), 15, 0),
                atKolkata(today.minusDays(11), 23, 59),
                CostType.PAID,
                new BigDecimal("100.00"),
                "Individual college coders",
                "https://djsacm.in/coding-cup-demo",
                now
        );
        eventRepository.save(pastCompNotWon);

        Event pastWorkshopAttended = buildDemoEvent(
                "Linux Kernel & Container Security Fundamentals",
                "Intensive full-day workshop on cgroups, namespaces, and seccomp profiles.",
                "Cybersecurity",
                EventType.WORKSHOP,
                EventMode.ONLINE,
                "Nullcon Student Community",
                "Online",
                "Online Lab Environment",
                atKolkata(today.minusDays(5), 10, 0),
                atKolkata(today.minusDays(5), 16, 0),
                atKolkata(today.minusDays(6), 20, 0),
                CostType.FREE,
                BigDecimal.ZERO,
                "Open to all students",
                "https://nullcon.net/student-linux-workshop-demo",
                now
        );
        eventRepository.save(pastWorkshopAttended);

        // Seed Bookmarks for demo student (includes overlapping e2 and e3, incomplete-time e4, and pastHackWon)
        bookmarkRepository.save(new Bookmark(student, e2, now.minusDays(2)));
        bookmarkRepository.save(new Bookmark(student, e3, now.minusDays(1)));
        bookmarkRepository.save(new Bookmark(student, e4, now.minusHours(12)));
        bookmarkRepository.save(new Bookmark(student, pastHackWon, now.minusDays(20)));

        // Seed Participation records for demo student:
        // 1. pastHackWon -> ATTENDED + WON (50 credits, Admin Verified)
        Participation p1 = new Participation();
        p1.setUser(student);
        p1.setEvent(pastHackWon);
        p1.setStatus(ParticipationStatus.ATTENDED);
        p1.setOutcome(CompetitionOutcome.WON);
        p1.setCredits(participationService.calculateCreditsForRecord(pastHackWon.getEventType(), ParticipationStatus.ATTENDED, CompetitionOutcome.WON));
        p1.setSelfReported(true);
        p1.setVerifiedByAdmin(true);
        p1.setStudentNote("First place in Track B (Smart Metering Anomaly Detector).");
        p1.setAdminVerificationNote("Certificate #SPIT-2026-019 verified by faculty coordinator.");
        p1.setUpdatedAt(now.minusDays(12));
        participationRepository.save(p1);

        // 2. pastCompNotWon -> ATTENDED + NOT_WON (20 credits, Self-Reported)
        Participation p2 = new Participation();
        p2.setUser(student);
        p2.setEvent(pastCompNotWon);
        p2.setStatus(ParticipationStatus.ATTENDED);
        p2.setOutcome(CompetitionOutcome.NOT_WON);
        p2.setCredits(participationService.calculateCreditsForRecord(pastCompNotWon.getEventType(), ParticipationStatus.ATTENDED, CompetitionOutcome.NOT_WON));
        p2.setSelfReported(true);
        p2.setVerifiedByAdmin(false);
        p2.setStudentNote("Solved 5 out of 8 problems; ranked 14th overall.");
        p2.setUpdatedAt(now.minusDays(8));
        participationRepository.save(p2);

        // 3. pastWorkshopAttended -> ATTENDED + NONE (20 credits, Admin Verified; excluded from competition win rate!)
        Participation p3 = new Participation();
        p3.setUser(student);
        p3.setEvent(pastWorkshopAttended);
        p3.setStatus(ParticipationStatus.ATTENDED);
        p3.setOutcome(CompetitionOutcome.NONE);
        p3.setCredits(participationService.calculateCreditsForRecord(pastWorkshopAttended.getEventType(), ParticipationStatus.ATTENDED, CompetitionOutcome.NONE));
        p3.setSelfReported(true);
        p3.setVerifiedByAdmin(true);
        p3.setStudentNote("Completed hands-on container namespace lab exercises.");
        p3.setAdminVerificationNote("Attendance log verified.");
        p3.setUpdatedAt(now.minusDays(4));
        participationRepository.save(p3);

        // 4. e2 (Upcoming Mumbai Hackathon) -> REGISTERED + PENDING (5 credits, Self-Reported)
        Participation p4 = new Participation();
        p4.setUser(student);
        p4.setEvent(e2);
        p4.setStatus(ParticipationStatus.REGISTERED);
        p4.setOutcome(CompetitionOutcome.PENDING);
        p4.setCredits(participationService.calculateCreditsForRecord(e2.getEventType(), ParticipationStatus.REGISTERED, CompetitionOutcome.PENDING));
        p4.setSelfReported(true);
        p4.setVerifiedByAdmin(false);
        p4.setStudentNote("Team 'ByteForge' registered for FinTech track.");
        p4.setUpdatedAt(now.minusDays(1));
        participationRepository.save(p4);
    }

    private OffsetDateTime atKolkata(LocalDate date, int hour, int minute) {
        return ZonedDateTime.of(date, LocalTime.of(hour, minute), DateWindowService.DEFAULT_ZONE).toOffsetDateTime();
    }

    private Event buildDemoEvent(
            String title,
            String description,
            String topic,
            EventType type,
            EventMode mode,
            String organizer,
            String city,
            String venue,
            OffsetDateTime start,
            OffsetDateTime end,
            OffsetDateTime deadline,
            CostType costType,
            BigDecimal price,
            String eligibility,
            String url,
            OffsetDateTime now
    ) {
        Event e = new Event();
        e.setTitle(title);
        e.setDescription(description);
        e.setTopic(topic);
        e.setEventType(type);
        e.setMode(mode);
        e.setOrganizer(organizer);
        e.setCity(city);
        e.setVenueOrPlatform(venue);
        e.setStartTime(start);
        e.setEndTime(end);
        e.setRegistrationDeadline(deadline);
        e.setCostType(costType);
        e.setPrice(price);
        e.setCurrency("INR");
        e.setEligibility(eligibility);
        e.setRegistrationUrl(url);
        e.setOfficialSourceUrl(url);
        e.setSourceName("TECHPULSE Curated Academic Demo Dataset");
        e.setDataOrigin(DataSourceOrigin.DEMO_SEED);
        e.setLastFetchedAt(now);
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        return e;
    }
}
