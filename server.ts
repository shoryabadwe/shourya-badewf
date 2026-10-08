import express, { Request, Response } from 'express';
import fs from 'fs';
import path from 'path';
import crypto from 'crypto';

/**
 * Preview Server for AI Studio Sandbox (where JDK/Maven are not installed in the container).
 * Serves the exact Spring Boot static frontend (src/main/resources/static/index.html, css/styles.css, js/app.js)
 * and mirrors the Java Spring Boot REST API contract (/api/**) with file-backed persistence (./data/preview_db.json).
 */

const app = express();
app.use(express.json());

const STATIC_DIR = path.resolve(process.cwd(), 'src/main/resources/static');
const DATA_DIR = path.resolve(process.cwd(), 'data');
const DB_FILE = path.join(DATA_DIR, 'techpulse_preview_db.json');

interface UserRecord {
  id: number;
  fullName: string;
  email: string;
  passwordHash: string;
  collegeOrInstitution: string;
  role: 'USER' | 'ADMIN';
  createdAt: string;
}

interface EventRecord {
  id: number;
  title: string;
  description: string;
  topic: string;
  eventType: 'HACKATHON' | 'WORKSHOP' | 'CONFERENCE' | 'WEBINAR' | 'COMPETITION';
  mode: 'ONLINE' | 'OFFLINE' | 'HYBRID';
  organizer: string;
  city: string;
  venueOrPlatform: string;
  startTime: string;
  endTime: string | null;
  registrationDeadline: string | null;
  costType: 'FREE' | 'PAID' | 'NOT_PROVIDED';
  price: number | null;
  currency: string;
  eligibility: string;
  registrationUrl: string | null;
  officialSourceUrl: string | null;
  sourceName: string;
  externalEventId: string | null;
  dataOrigin: 'DEMO_SEED' | 'MANUAL_ADMIN' | 'LIVE_IMPORT';
  lastFetchedAt: string;
  cancelled: boolean;
  registrationClosed: boolean;
  deleted: boolean;
  manuallyEdited: boolean;
  createdAt: string;
  updatedAt: string;
}

interface BookmarkRecord {
  id: number;
  userId: number;
  eventId: number;
  savedAt: string;
}

interface ParticipationRecord {
  id: number;
  userId: number;
  eventId: number;
  status: 'SAVED' | 'REGISTERED' | 'ATTENDED';
  outcome: 'NONE' | 'PENDING' | 'WON' | 'NOT_WON';
  credits: number;
  selfReported: boolean;
  verifiedByAdmin: boolean;
  studentNote: string | null;
  adminVerificationNote: string | null;
  updatedAt: string;
}

interface SourceStatusRecord {
  sourceName: string;
  configured: boolean;
  status: string;
  lastAttemptAt: string | null;
  lastSuccessAt: string | null;
  staleData: boolean;
  importedCount: number;
  updatedCount: number;
  statusMessage: string;
  requiredEnvInstructions: string;
}

interface DatabaseSchema {
  users: UserRecord[];
  events: EventRecord[];
  bookmarks: BookmarkRecord[];
  participations: ParticipationRecord[];
  sourceStatus: SourceStatusRecord;
}

function hashPassword(raw: string): string {
  return crypto.createHash('sha256').update('techpulse_salt_' + raw).digest('hex');
}

function getKolkataNow(): Date {
  return new Date();
}

function toKolkataIso(date: Date): string {
  const utcMs = date.getTime() + date.getTimezoneOffset() * 60000;
  const istMs = utcMs + 5.5 * 3600000;
  const ist = new Date(istMs);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${ist.getFullYear()}-${pad(ist.getMonth() + 1)}-${pad(ist.getDate())}T${pad(ist.getHours())}:${pad(ist.getMinutes())}:${pad(ist.getSeconds())}+05:30`;
}

function atKolkataOffsetDays(daysFromToday: number, hour: number, minute: number): string {
  const now = new Date();
  const utcMs = now.getTime() + now.getTimezoneOffset() * 60000;
  const istNow = new Date(utcMs + 5.5 * 3600000);
  istNow.setDate(istNow.getDate() + daysFromToday);
  istNow.setHours(hour, minute, 0, 0);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${istNow.getFullYear()}-${pad(istNow.getMonth() + 1)}-${pad(istNow.getDate())}T${pad(istNow.getHours())}:${pad(istNow.getMinutes())}:00+05:30`;
}

function daysToUpcomingSaturday(): number {
  const now = new Date();
  const utcMs = now.getTime() + now.getTimezoneOffset() * 60000;
  const istNow = new Date(utcMs + 5.5 * 3600000);
  const dow = istNow.getDay(); // 0=Sun .. 6=Sat
  if (dow === 6) return 0;
  if (dow === 0) return -1;
  return 6 - dow;
}

function createInitialSeed(): DatabaseSchema {
  const nowIso = toKolkataIso(getKolkataNow());
  const satOffset = daysToUpcomingSaturday();
  const sunOffset = satOffset + 1;

  const users: UserRecord[] = [
    {
      id: 1,
      fullName: 'Aarav Deshmukh',
      email: 'student@techpulse.edu.in',
      passwordHash: hashPassword('Student@123'),
      collegeOrInstitution: 'Sardar Patel Institute of Technology, Mumbai',
      role: 'USER',
      createdAt: nowIso
    },
    {
      id: 2,
      fullName: 'Priya Kulkarni',
      email: 'priya.k@techpulse.edu.in',
      passwordHash: hashPassword('Student@123'),
      collegeOrInstitution: 'Fr. C. Rodrigues Institute of Technology, Navi Mumbai',
      role: 'USER',
      createdAt: nowIso
    },
    {
      id: 3,
      fullName: 'Prof. Rajeshwari Nair (Faculty Coordinator)',
      email: 'admin@techpulse.edu.in',
      passwordHash: hashPassword('Admin@123'),
      collegeOrInstitution: 'Department of Computer Engineering',
      role: 'ADMIN',
      createdAt: nowIso
    }
  ];

  const events: EventRecord[] = [
    {
      id: 1,
      title: 'Spring Boot 3 & JPA Production Architecture Lab',
      description: 'Hands-on technical workshop covering Spring Boot 3.2 REST controllers, Spring Data JPA entity design, H2/MySQL migration, and BCrypt session security for undergraduate engineering students.',
      topic: 'Java',
      eventType: 'WORKSHOP',
      mode: 'ONLINE',
      organizer: 'Mumbai Java User Group (MUJUG)',
      city: 'Online',
      venueOrPlatform: 'Live Interactive Session (Zoom + GitHub Codespaces)',
      startTime: atKolkataOffsetDays(0, 22, 30),
      endTime: atKolkataOffsetDays(0, 23, 50),
      registrationDeadline: atKolkataOffsetDays(0, 21, 0),
      costType: 'FREE',
      price: 0,
      currency: 'INR',
      eligibility: 'Open to B.E. / B.Tech / MCA / B.Sc IT students across India',
      registrationUrl: 'https://konfhub.com/mujug-spring-boot-lab-demo',
      officialSourceUrl: 'https://konfhub.com/mujug-spring-boot-lab-demo',
      sourceName: 'TECHPULSE Curated Academic Demo Dataset',
      externalEventId: null,
      dataOrigin: 'DEMO_SEED',
      lastFetchedAt: nowIso,
      cancelled: false,
      registrationClosed: false,
      deleted: false,
      manuallyEdited: false,
      createdAt: nowIso,
      updatedAt: nowIso
    },
    {
      id: 2,
      title: 'Mumbai FinTech & AI Systems Hackathon 2026',
      description: '24-hour inter-collegiate engineering hackathon focused on fraud detection pipelines, LLM guardrails, and low-latency payment reconciliation systems.',
      topic: 'AI/ML',
      eventType: 'HACKATHON',
      mode: 'OFFLINE',
      organizer: 'IIT Bombay E-Cell & Techfest Guild',
      city: 'Mumbai',
      venueOrPlatform: 'Convocation Hall Annex, Powai, Mumbai 400076',
      startTime: atKolkataOffsetDays(satOffset, 9, 30),
      endTime: atKolkataOffsetDays(sunOffset, 11, 0),
      registrationDeadline: atKolkataOffsetDays(satOffset - 1, 23, 59),
      costType: 'FREE',
      price: 0,
      currency: 'INR',
      eligibility: 'Teams of 2 to 4 undergraduate engineering students',
      registrationUrl: 'https://devfolio.co/mumbai-fintech-ai-hack-demo',
      officialSourceUrl: 'https://devfolio.co/mumbai-fintech-ai-hack-demo',
      sourceName: 'TECHPULSE Curated Academic Demo Dataset',
      externalEventId: null,
      dataOrigin: 'DEMO_SEED',
      lastFetchedAt: nowIso,
      cancelled: false,
      registrationClosed: false,
      deleted: false,
      manuallyEdited: false,
      createdAt: nowIso,
      updatedAt: nowIso
    },
    {
      id: 3,
      title: 'Navi Mumbai Capture The Flag (CTF) & Zero-Trust Arena',
      description: 'Jeopardy-style and Attack-Defense cybersecurity competition testing web exploitation, binary reverse engineering, and container hardening.',
      topic: 'Cybersecurity',
      eventType: 'COMPETITION',
      mode: 'HYBRID',
      organizer: 'OWASP Navi Mumbai Student Chapter',
      city: 'Navi Mumbai',
      venueOrPlatform: 'CIDCO Exhibition & Convention Centre, Vashi, Navi Mumbai 400703',
      startTime: atKolkataOffsetDays(satOffset, 11, 0),
      endTime: atKolkataOffsetDays(satOffset, 17, 30),
      registrationDeadline: atKolkataOffsetDays(satOffset - 1, 18, 0),
      costType: 'PAID',
      price: 250,
      currency: 'INR',
      eligibility: 'Individual or 2-member student teams with valid college ID',
      registrationUrl: 'https://ctfd.io/owasp-navi-mumbai-demo',
      officialSourceUrl: 'https://ctfd.io/owasp-navi-mumbai-demo',
      sourceName: 'TECHPULSE Curated Academic Demo Dataset',
      externalEventId: null,
      dataOrigin: 'DEMO_SEED',
      lastFetchedAt: nowIso,
      cancelled: false,
      registrationClosed: false,
      deleted: false,
      manuallyEdited: false,
      createdAt: nowIso,
      updatedAt: nowIso
    },
    {
      id: 4,
      title: 'Modern Web Standards: Accessible Vanilla JS & CSS Grid Systems',
      description: 'Deep dive into building responsive, high-contrast web dashboards without heavy frontend frameworks, focusing on semantic HTML5, CSS custom properties, and Fetch API patterns.',
      topic: 'Web Development',
      eventType: 'WORKSHOP',
      mode: 'OFFLINE',
      organizer: 'VJTI Technovanza Developer Community',
      city: 'Mumbai',
      venueOrPlatform: 'Main Auditorium, Veermata Jijabai Technological Institute, Matunga, Mumbai',
      startTime: atKolkataOffsetDays(4, 14, 0),
      endTime: null,
      registrationDeadline: atKolkataOffsetDays(3, 20, 0),
      costType: 'FREE',
      price: 0,
      currency: 'INR',
      eligibility: 'All engineering & diploma students',
      registrationUrl: 'https://technovanza.org/vanilla-web-workshop-demo',
      officialSourceUrl: 'https://technovanza.org/vanilla-web-workshop-demo',
      sourceName: 'TECHPULSE Curated Academic Demo Dataset',
      externalEventId: null,
      dataOrigin: 'DEMO_SEED',
      lastFetchedAt: nowIso,
      cancelled: false,
      registrationClosed: false,
      deleted: false,
      manuallyEdited: false,
      createdAt: nowIso,
      updatedAt: nowIso
    },
    {
      id: 5,
      title: 'Western India Data Engineering & Apache Iceberg Summit',
      description: 'Technical conference featuring talks on real-time stream processing, lakehouse indexing, and reproducible ML pipelines.',
      topic: 'Data Science',
      eventType: 'CONFERENCE',
      mode: 'OFFLINE',
      organizer: 'Navi Mumbai Cloud & Data Collective',
      city: 'Navi Mumbai',
      venueOrPlatform: 'Reliance Corporate Park Convention Hall, Ghansoli, Navi Mumbai',
      startTime: atKolkataOffsetDays(6, 10, 0),
      endTime: atKolkataOffsetDays(6, 17, 0),
      registrationDeadline: atKolkataOffsetDays(5, 18, 0),
      costType: 'NOT_PROVIDED',
      price: null,
      currency: 'INR',
      eligibility: 'Final-year students, postgraduate researchers, and developers',
      registrationUrl: 'https://konfhub.com/western-india-data-summit-demo',
      officialSourceUrl: 'https://konfhub.com/western-india-data-summit-demo',
      sourceName: 'TECHPULSE Curated Academic Demo Dataset',
      externalEventId: null,
      dataOrigin: 'DEMO_SEED',
      lastFetchedAt: nowIso,
      cancelled: false,
      registrationClosed: false,
      deleted: false,
      manuallyEdited: false,
      createdAt: nowIso,
      updatedAt: nowIso
    },
    {
      id: 6,
      title: 'Deccan Systems Programming & JVM Benchmarking Challenge',
      description: 'Algorithmic and systems optimization competition focused on garbage collection tuning, concurrent data structures in Java 17, and SQL query optimization.',
      topic: 'Java',
      eventType: 'COMPETITION',
      mode: 'HYBRID',
      organizer: 'COEP Technological University ACM Chapter',
      city: 'Pune',
      venueOrPlatform: 'COEP Main Campus, Shivajinagar, Pune & Online Judge',
      startTime: atKolkataOffsetDays(12, 10, 0),
      endTime: atKolkataOffsetDays(12, 18, 0),
      registrationDeadline: atKolkataOffsetDays(10, 23, 59),
      costType: 'PAID',
      price: 150,
      currency: 'INR',
      eligibility: 'Undergraduate CS/IT students across Maharashtra',
      registrationUrl: 'https://acm.coep.org.in/jvm-challenge-demo',
      officialSourceUrl: 'https://acm.coep.org.in/jvm-challenge-demo',
      sourceName: 'TECHPULSE Curated Academic Demo Dataset',
      externalEventId: null,
      dataOrigin: 'DEMO_SEED',
      lastFetchedAt: nowIso,
      cancelled: false,
      registrationClosed: false,
      deleted: false,
      manuallyEdited: false,
      createdAt: nowIso,
      updatedAt: nowIso
    },
    {
      id: 7,
      title: 'Demystifying Vector Databases & Retrieval-Augmented Generation',
      description: 'Technical webinar walking through HNSW indexing, cosine similarity mathematics, and grounding enterprise search systems.',
      topic: 'AI/ML',
      eventType: 'WEBINAR',
      mode: 'ONLINE',
      organizer: 'ACM India Eminent Speaker Program',
      city: 'Online',
      venueOrPlatform: 'ACM India Webinar Portal',
      startTime: atKolkataOffsetDays(18, 16, 30),
      endTime: atKolkataOffsetDays(18, 18, 0),
      registrationDeadline: atKolkataOffsetDays(18, 15, 0),
      costType: 'FREE',
      price: 0,
      currency: 'INR',
      eligibility: 'Open to all students and faculty members',
      registrationUrl: 'https://india.acm.org/webinars/vector-db-demo',
      officialSourceUrl: 'https://india.acm.org/webinars/vector-db-demo',
      sourceName: 'TECHPULSE Curated Academic Demo Dataset',
      externalEventId: null,
      dataOrigin: 'DEMO_SEED',
      lastFetchedAt: nowIso,
      cancelled: false,
      registrationClosed: false,
      deleted: false,
      manuallyEdited: false,
      createdAt: nowIso,
      updatedAt: nowIso
    },
    {
      id: 8,
      title: 'National Open-Source Civic Tech Hackathon',
      description: 'Build open-source digital public infrastructure tools for urban transit, accessible healthcare triage, and water grid telemetry.',
      topic: 'Web Development',
      eventType: 'HACKATHON',
      mode: 'HYBRID',
      organizer: 'FOSS United & IISc Open Systems Lab',
      city: 'Bengaluru',
      venueOrPlatform: 'BIC Domlur, Bengaluru & Remote Tracks',
      startTime: atKolkataOffsetDays(24, 9, 0),
      endTime: atKolkataOffsetDays(25, 17, 0),
      registrationDeadline: atKolkataOffsetDays(20, 23, 59),
      costType: 'FREE',
      price: 0,
      currency: 'INR',
      eligibility: 'Open to college teams (1–4 members)',
      registrationUrl: 'https://fossunited.org/civic-hackathon-demo',
      officialSourceUrl: 'https://fossunited.org/civic-hackathon-demo',
      sourceName: 'TECHPULSE Curated Academic Demo Dataset',
      externalEventId: null,
      dataOrigin: 'DEMO_SEED',
      lastFetchedAt: nowIso,
      cancelled: false,
      registrationClosed: false,
      deleted: false,
      manuallyEdited: false,
      createdAt: nowIso,
      updatedAt: nowIso
    },
    {
      id: 9,
      title: 'Hardware IoT & Embedded Rust Bootcamp (Postponed)',
      description: 'Hands-on embedded systems workshop with ESP32 microcontrollers. Marked cancelled to demonstrate cancelled status badge and conflict exclusion.',
      topic: 'Other',
      eventType: 'WORKSHOP',
      mode: 'OFFLINE',
      organizer: 'Mumbai Makerspace Collective',
      city: 'Mumbai',
      venueOrPlatform: 'Andheri East Electronics Lab, Mumbai',
      startTime: atKolkataOffsetDays(15, 10, 0),
      endTime: atKolkataOffsetDays(15, 16, 0),
      registrationDeadline: atKolkataOffsetDays(12, 18, 0),
      costType: 'PAID',
      price: 499,
      currency: 'INR',
      eligibility: 'Students with basic C/Rust knowledge',
      registrationUrl: 'https://mumbaimakers.in/iot-bootcamp-demo',
      officialSourceUrl: 'https://mumbaimakers.in/iot-bootcamp-demo',
      sourceName: 'TECHPULSE Curated Academic Demo Dataset',
      externalEventId: null,
      dataOrigin: 'DEMO_SEED',
      lastFetchedAt: nowIso,
      cancelled: true,
      registrationClosed: false,
      deleted: false,
      manuallyEdited: false,
      createdAt: nowIso,
      updatedAt: nowIso
    },
    {
      id: 10,
      title: 'Smart Campus Energy Optimization Hackathon',
      description: '36-hour inter-college hackathon on smart grid load balancing and anomaly detection.',
      topic: 'AI/ML',
      eventType: 'HACKATHON',
      mode: 'OFFLINE',
      organizer: 'Sardar Patel Institute of Technology',
      city: 'Mumbai',
      venueOrPlatform: "SPIT Campus, Bhavan's Campus, Munshi Nagar, Andheri West, Mumbai",
      startTime: atKolkataOffsetDays(-14, 9, 0),
      endTime: atKolkataOffsetDays(-13, 18, 0),
      registrationDeadline: atKolkataOffsetDays(-16, 23, 59),
      costType: 'FREE',
      price: 0,
      currency: 'INR',
      eligibility: 'Undergraduate engineering teams',
      registrationUrl: 'https://spit.ac.in/smart-energy-hack-demo',
      officialSourceUrl: 'https://spit.ac.in/smart-energy-hack-demo',
      sourceName: 'TECHPULSE Curated Academic Demo Dataset',
      externalEventId: null,
      dataOrigin: 'DEMO_SEED',
      lastFetchedAt: nowIso,
      cancelled: false,
      registrationClosed: false,
      deleted: false,
      manuallyEdited: false,
      createdAt: nowIso,
      updatedAt: nowIso
    },
    {
      id: 11,
      title: 'All-Mumbai Collegiate Algorithmic Coding Cup',
      description: 'Competitive programming contest with 8 ICPC-style graph and dynamic programming problems.',
      topic: 'Java',
      eventType: 'COMPETITION',
      mode: 'OFFLINE',
      organizer: 'DJ Sanghvi ACM Student Chapter',
      city: 'Mumbai',
      venueOrPlatform: 'Vile Parle West, Mumbai',
      startTime: atKolkataOffsetDays(-9, 10, 0),
      endTime: atKolkataOffsetDays(-9, 15, 0),
      registrationDeadline: atKolkataOffsetDays(-11, 23, 59),
      costType: 'PAID',
      price: 100,
      currency: 'INR',
      eligibility: 'Individual college coders',
      registrationUrl: 'https://djsacm.in/coding-cup-demo',
      officialSourceUrl: 'https://djsacm.in/coding-cup-demo',
      sourceName: 'TECHPULSE Curated Academic Demo Dataset',
      externalEventId: null,
      dataOrigin: 'DEMO_SEED',
      lastFetchedAt: nowIso,
      cancelled: false,
      registrationClosed: false,
      deleted: false,
      manuallyEdited: false,
      createdAt: nowIso,
      updatedAt: nowIso
    },
    {
      id: 12,
      title: 'Linux Kernel & Container Security Fundamentals',
      description: 'Intensive full-day workshop on cgroups, namespaces, and seccomp profiles.',
      topic: 'Cybersecurity',
      eventType: 'WORKSHOP',
      mode: 'ONLINE',
      organizer: 'Nullcon Student Community',
      city: 'Online',
      venueOrPlatform: 'Online Lab Environment',
      startTime: atKolkataOffsetDays(-5, 10, 0),
      endTime: atKolkataOffsetDays(-5, 16, 0),
      registrationDeadline: atKolkataOffsetDays(-6, 20, 0),
      costType: 'FREE',
      price: 0,
      currency: 'INR',
      eligibility: 'Open to all students',
      registrationUrl: 'https://nullcon.net/student-linux-workshop-demo',
      officialSourceUrl: 'https://nullcon.net/student-linux-workshop-demo',
      sourceName: 'TECHPULSE Curated Academic Demo Dataset',
      externalEventId: null,
      dataOrigin: 'DEMO_SEED',
      lastFetchedAt: nowIso,
      cancelled: false,
      registrationClosed: false,
      deleted: false,
      manuallyEdited: false,
      createdAt: nowIso,
      updatedAt: nowIso
    }
  ];

  const bookmarks: BookmarkRecord[] = [
    { id: 1, userId: 1, eventId: 2, savedAt: nowIso },
    { id: 2, userId: 1, eventId: 3, savedAt: nowIso },
    { id: 3, userId: 1, eventId: 4, savedAt: nowIso },
    { id: 4, userId: 1, eventId: 10, savedAt: nowIso }
  ];

  const participations: ParticipationRecord[] = [
    {
      id: 1,
      userId: 1,
      eventId: 10,
      status: 'ATTENDED',
      outcome: 'WON',
      credits: 50,
      selfReported: true,
      verifiedByAdmin: true,
      studentNote: 'First place in Track B (Smart Metering Anomaly Detector).',
      adminVerificationNote: 'Certificate #SPIT-2026-019 verified by faculty coordinator.',
      updatedAt: nowIso
    },
    {
      id: 2,
      userId: 1,
      eventId: 11,
      status: 'ATTENDED',
      outcome: 'NOT_WON',
      credits: 20,
      selfReported: true,
      verifiedByAdmin: false,
      studentNote: 'Solved 5 out of 8 problems; ranked 14th overall.',
      adminVerificationNote: null,
      updatedAt: nowIso
    },
    {
      id: 3,
      userId: 1,
      eventId: 12,
      status: 'ATTENDED',
      outcome: 'NONE',
      credits: 20,
      selfReported: true,
      verifiedByAdmin: true,
      studentNote: 'Completed hands-on container namespace lab exercises.',
      adminVerificationNote: 'Attendance log verified.',
      updatedAt: nowIso
    },
    {
      id: 4,
      userId: 1,
      eventId: 2,
      status: 'REGISTERED',
      outcome: 'PENDING',
      credits: 5,
      selfReported: true,
      verifiedByAdmin: false,
      studentNote: "Team 'ByteForge' registered for FinTech track.",
      adminVerificationNote: null,
      updatedAt: nowIso
    }
  ];

  return {
    users,
    events,
    bookmarks,
    participations,
    sourceStatus: {
      sourceName: 'Public Tech Feed Adapter',
      configured: false,
      status: 'LIVE_SOURCE_NOT_CONFIGURED',
      lastAttemptAt: null,
      lastSuccessAt: null,
      staleData: false,
      importedCount: 0,
      updatedCount: 0,
      statusMessage: 'Live source not configured. Displaying clearly labelled demonstration dataset and manual entries.',
      requiredEnvInstructions: 'Set environment variables TECHPULSE_LIVE_SOURCE_ENABLED=true, TECHPULSE_LIVE_SOURCE_URL=<https://your-event-feed-endpoint>, and optionally TECHPULSE_LIVE_SOURCE_API_KEY=<token> before starting Spring Boot.'
    }
  };
}

function loadDb(): DatabaseSchema {
  if (!fs.existsSync(DATA_DIR)) {
    fs.mkdirSync(DATA_DIR, { recursive: true });
  }
  if (!fs.existsSync(DB_FILE)) {
    const seed = createInitialSeed();
    fs.writeFileSync(DB_FILE, JSON.stringify(seed, null, 2), 'utf-8');
    return seed;
  }
  return JSON.parse(fs.readFileSync(DB_FILE, 'utf-8'));
}

function saveDb(db: DatabaseSchema) {
  fs.writeFileSync(DB_FILE, JSON.stringify(db, null, 2), 'utf-8');
}

let db = loadDb();
const sessions = new Map<string, number>();

function parseCookies(req: Request): Record<string, string> {
  const raw = req.headers.cookie || '';
  const out: Record<string, string> = {};
  raw.split(';').forEach(pair => {
    const idx = pair.indexOf('=');
    if (idx > 0) {
      const k = pair.slice(0, idx).trim();
      const v = decodeURIComponent(pair.slice(idx + 1).trim());
      out[k] = v;
    }
  });
  return out;
}

function getSessionUser(req: Request): UserRecord | null {
  const cookies = parseCookies(req);
  const sid = cookies['JSESSIONID'];
  if (!sid || !sessions.has(sid)) return null;
  const uid = sessions.get(sid)!;
  return db.users.find(u => u.id === uid) || null;
}

function ensureCsrfCookie(req: Request, res: Response): string {
  const cookies = parseCookies(req);
  let token = cookies['XSRF-TOKEN'];
  if (!token) {
    token = crypto.randomBytes(16).toString('hex');
    res.setHeader('Set-Cookie', `XSRF-TOKEN=${token}; Path=/; SameSite=Lax`);
  }
  return token;
}

function computeCountdownLabel(ev: EventRecord): string {
  if (ev.cancelled) return 'Cancelled';
  const now = new Date();
  const start = new Date(ev.startTime);
  if (start < now) {
    if (ev.endTime && new Date(ev.endTime) > now) return 'In progress now';
    return 'Event completed';
  }
  const diffHours = Math.floor((start.getTime() - now.getTime()) / 3600000);
  if (diffHours < 1) {
    const mins = Math.max(1, Math.floor((start.getTime() - now.getTime()) / 60000));
    return `Starts in ${mins} min`;
  }
  if (diffHours < 24) {
    return `Starts in ${diffHours} ${diffHours === 1 ? 'hour' : 'hours'}`;
  }
  const days = Math.floor(diffHours / 24);
  if (days === 1) return 'Starts tomorrow';
  return `Starts in ${days} days`;
}

function toEventResponse(ev: EventRecord, userId: number | null) {
  const bookmarked = userId !== null && db.bookmarks.some(b => b.userId === userId && b.eventId === ev.id);
  const part = userId !== null ? db.participations.find(p => p.userId === userId && p.eventId === ev.id) : undefined;
  let priceDisplay = 'Price not provided';
  if (ev.costType === 'FREE') priceDisplay = 'Free';
  else if (ev.costType === 'PAID' && ev.price !== null) priceDisplay = `Paid · ₹${ev.price}`;

  let dataOriginLabel = 'Manual Admin Entry';
  if (ev.dataOrigin === 'DEMO_SEED') dataOriginLabel = 'Demonstration Dataset';
  else if (ev.dataOrigin === 'LIVE_IMPORT') dataOriginLabel = 'Live Imported Source';

  return {
    ...ev,
    priceDisplay,
    dataOriginLabel,
    countdownLabel: computeCountdownLabel(ev),
    bookmarked,
    participationStatus: part ? part.status : null,
    competitionOutcome: part ? part.outcome : null,
    participationVerified: part ? part.verifiedByAdmin : false
  };
}

function resolveDateWindow(windowParam: string) {
  const key = (windowParam || 'NEXT_30_DAYS').toUpperCase();
  const satOffset = daysToUpcomingSaturday();
  if (key === 'TODAY') {
    return {
      windowKey: 'TODAY',
      startInclusive: atKolkataOffsetDays(0, 0, 0),
      endInclusive: atKolkataOffsetDays(0, 23, 59)
    };
  }
  if (key === 'THIS_WEEKEND') {
    return {
      windowKey: 'THIS_WEEKEND',
      startInclusive: atKolkataOffsetDays(satOffset, 0, 0),
      endInclusive: atKolkataOffsetDays(satOffset + 1, 23, 59)
    };
  }
  if (key === 'NEXT_7_DAYS') {
    return {
      windowKey: 'NEXT_7_DAYS',
      startInclusive: toKolkataIso(new Date()),
      endInclusive: atKolkataOffsetDays(7, 23, 59)
    };
  }
  if (key === 'ALL_UPCOMING') {
    return {
      windowKey: 'ALL_UPCOMING',
      startInclusive: toKolkataIso(new Date()),
      endInclusive: atKolkataOffsetDays(365, 23, 59)
    };
  }
  return {
    windowKey: 'NEXT_30_DAYS',
    startInclusive: toKolkataIso(new Date()),
    endInclusive: atKolkataOffsetDays(30, 23, 59)
  };
}

// ============================================================================
// REST API Endpoints (Mirroring Spring Boot Controllers)
// ============================================================================

app.get('/api/csrf', (req, res) => {
  const token = ensureCsrfCookie(req, res);
  res.json({ token, headerName: 'X-XSRF-TOKEN' });
});

app.get('/api/auth/me', (req, res) => {
  const token = ensureCsrfCookie(req, res);
  const user = getSessionUser(req);
  if (!user) {
    return res.json({ authenticated: false, id: null, fullName: null, email: null, collegeOrInstitution: null, role: null, csrfToken: token });
  }
  return res.json({
    authenticated: true,
    id: user.id,
    fullName: user.fullName,
    email: user.email,
    collegeOrInstitution: user.collegeOrInstitution,
    role: user.role,
    csrfToken: token
  });
});

app.post('/api/auth/login', (req, res) => {
  const { email, password } = req.body || {};
  if (!email || !password) return res.status(400).json({ error: 'Email and password are required.' });
  const user = db.users.find(u => u.email.toLowerCase() === String(email).trim().toLowerCase());
  if (!user || user.passwordHash !== hashPassword(String(password))) {
    return res.status(401).json({ error: 'Invalid email or password.' });
  }
  const sid = crypto.randomBytes(18).toString('hex');
  sessions.set(sid, user.id);
  const token = ensureCsrfCookie(req, res);
  res.setHeader('Set-Cookie', [
    `JSESSIONID=${sid}; Path=/; HttpOnly; SameSite=Lax`,
    `XSRF-TOKEN=${token}; Path=/; SameSite=Lax`
  ]);
  return res.json({
    authenticated: true,
    id: user.id,
    fullName: user.fullName,
    email: user.email,
    collegeOrInstitution: user.collegeOrInstitution,
    role: user.role,
    csrfToken: token
  });
});

app.post('/api/auth/register', (req, res) => {
  const { fullName, email, password, collegeOrInstitution } = req.body || {};
  if (!fullName || String(fullName).trim().length < 2) {
    return res.status(400).json({ error: 'Full name must be at least 2 characters.' });
  }
  if (!email || !String(email).includes('@')) {
    return res.status(400).json({ error: 'Please enter a valid email address.' });
  }
  if (!password || String(password).length < 8) {
    return res.status(400).json({ error: 'Password must be at least 8 characters long.' });
  }
  const normEmail = String(email).trim().toLowerCase();
  if (db.users.some(u => u.email.toLowerCase() === normEmail)) {
    return res.status(400).json({ error: 'An account with this email address is already registered.' });
  }
  const newUser: UserRecord = {
    id: db.users.reduce((m, u) => Math.max(m, u.id), 0) + 1,
    fullName: String(fullName).trim(),
    email: normEmail,
    passwordHash: hashPassword(String(password)),
    collegeOrInstitution: collegeOrInstitution ? String(collegeOrInstitution).trim() : 'Not provided',
    role: 'USER',
    createdAt: toKolkataIso(new Date())
  };
  db.users.push(newUser);
  saveDb(db);

  const sid = crypto.randomBytes(18).toString('hex');
  sessions.set(sid, newUser.id);
  const token = ensureCsrfCookie(req, res);
  res.setHeader('Set-Cookie', [
    `JSESSIONID=${sid}; Path=/; HttpOnly; SameSite=Lax`,
    `XSRF-TOKEN=${token}; Path=/; SameSite=Lax`
  ]);
  return res.status(201).json({
    authenticated: true,
    id: newUser.id,
    fullName: newUser.fullName,
    email: newUser.email,
    collegeOrInstitution: newUser.collegeOrInstitution,
    role: newUser.role,
    csrfToken: token
  });
});

app.post('/api/auth/logout', (req, res) => {
  const cookies = parseCookies(req);
  const sid = cookies['JSESSIONID'];
  if (sid) sessions.delete(sid);
  res.json({ message: 'Logged out successfully' });
});

app.get('/api/sources/status', (_req, res) => {
  res.json(db.sourceStatus);
});

app.post('/api/sources/refresh', (req, res) => {
  const user = getSessionUser(req);
  if (!user) return res.status(401).json({ error: 'Authentication required.' });
  db.sourceStatus.lastAttemptAt = toKolkataIso(new Date());
  saveDb(db);
  res.json(db.sourceStatus);
});

app.get('/api/events', (req, res) => {
  const user = getSessionUser(req);
  const userId = user ? user.id : null;
  const search = String(req.query.search || '').trim().toLowerCase();
  const city = String(req.query.city || 'ALL');
  const topic = String(req.query.topic || 'ALL');
  const eventType = String(req.query.eventType || 'ALL');
  const mode = String(req.query.mode || 'ALL');
  const cost = String(req.query.cost || 'ALL');
  const maxPriceRaw = req.query.maxPrice !== undefined && req.query.maxPrice !== '' ? Number(req.query.maxPrice) : null;
  const dateWindow = String(req.query.dateWindow || 'NEXT_30_DAYS');
  const sortBy = String(req.query.sortBy || 'SOONEST');
  const page = Math.max(0, parseInt(String(req.query.page || '0'), 10) || 0);
  const size = Math.max(1, Math.min(50, parseInt(String(req.query.size || '9'), 10) || 9));

  const bounds = resolveDateWindow(dateWindow);
  const startBound = new Date(bounds.startInclusive);
  const endBound = new Date(bounds.endInclusive);

  let list = db.events.filter(e => !e.deleted);
  if (sortBy.toUpperCase() === 'RECENTLY_ADDED') {
    list.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
  } else {
    list.sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime());
  }

  list = list.filter(e => {
    const st = new Date(e.startTime);
    if (st < startBound || st > endBound) return false;
    if (search) {
      const match = e.title.toLowerCase().includes(search) ||
        e.topic.toLowerCase().includes(search) ||
        e.organizer.toLowerCase().includes(search);
      if (!match) return false;
    }
    if (city !== 'ALL') {
      if (city === 'OTHER') {
        const c = e.city.toLowerCase();
        if (c === 'mumbai' || c === 'navi mumbai' || c === 'online') return false;
      } else if (e.city.toLowerCase() !== city.toLowerCase()) {
        return false;
      }
    }
    if (topic !== 'ALL') {
      if (topic === 'OTHER') {
        const t = e.topic.toLowerCase();
        if (['ai/ml', 'java', 'cybersecurity', 'web development', 'data science'].includes(t)) return false;
      } else if (e.topic.toLowerCase() !== topic.toLowerCase()) {
        return false;
      }
    }
    if (eventType !== 'ALL' && e.eventType !== eventType) return false;
    if (mode !== 'ALL' && e.mode !== mode) return false;
    if (cost !== 'ALL' && e.costType !== cost) return false;
    if (maxPriceRaw !== null && !isNaN(maxPriceRaw)) {
      if (e.costType === 'FREE') return maxPriceRaw >= 0;
      if (e.costType === 'PAID' && e.price !== null) return e.price <= maxPriceRaw;
      return false;
    }
    return true;
  });

  const totalElements = list.length;
  const totalPages = Math.max(1, Math.ceil(totalElements / size));
  const slice = list.slice(page * size, page * size + size).map(e => toEventResponse(e, userId));

  res.json({
    content: slice,
    page,
    size,
    totalElements,
    totalPages,
    activeDateWindow: bounds.windowKey,
    windowStart: bounds.startInclusive,
    windowEnd: bounds.endInclusive,
    timezone: 'Asia/Kolkata'
  });
});

app.get('/api/events/:id', (req, res) => {
  const id = parseInt(req.params.id, 10);
  const ev = db.events.find(e => e.id === id);
  if (!ev) return res.status(404).json({ error: 'Event not found with ID: ' + id });
  const user = getSessionUser(req);
  res.json(toEventResponse(ev, user ? user.id : null));
});

app.get('/api/events/:id/calendar.ics', (req, res) => {
  const id = parseInt(req.params.id, 10);
  const ev = db.events.find(e => e.id === id);
  if (!ev) return res.status(404).send('Event not found');
  const fmtUtc = (iso: string) => new Date(iso).toISOString().replace(/[-:]/g, '').replace(/\.\d{3}/, '');
  const startUtc = fmtUtc(ev.startTime);
  const assumed = !ev.endTime;
  const endIso = ev.endTime ? ev.endTime : new Date(new Date(ev.startTime).getTime() + 2 * 3600000).toISOString();
  const endUtc = fmtUtc(endIso);
  const esc = (s: string) => (s || '').replace(/\\/g, '\\\\').replace(/;/g, '\\;').replace(/,/g, '\\,').replace(/\r?\n/g, '\\n');
  let desc = `${ev.description}\n\nOrganizer: ${ev.organizer}\nTopic / Type: ${ev.topic} / ${ev.eventType}`;
  if (assumed) {
    desc += `\n\nNOTE: Official end time was not provided by the source. A default duration of 2 hours has been assumed for this calendar entry.`;
  }
  const ics = [
    'BEGIN:VCALENDAR',
    'VERSION:2.0',
    'PRODID:-//TECHPULSE//Course 2113611 Full Stack Java//EN',
    'CALSCALE:GREGORIAN',
    'METHOD:PUBLISH',
    'X-WR-TIMEZONE:Asia/Kolkata',
    'BEGIN:VEVENT',
    `UID:techpulse-event-${ev.id}@techpulse.edu.in`,
    `DTSTAMP:${fmtUtc(new Date().toISOString())}`,
    `DTSTART:${startUtc}`,
    `DTEND:${endUtc}`,
    `SUMMARY:${esc(ev.title)}`,
    `DESCRIPTION:${esc(desc)}`,
    `LOCATION:${esc(ev.venueOrPlatform || ev.city)}`,
    `STATUS:${ev.cancelled ? 'CANCELLED' : 'CONFIRMED'}`,
    'END:VEVENT',
    'END:VCALENDAR',
    ''
  ].join('\r\n');
  res.setHeader('Content-Disposition', `attachment; filename="techpulse-event-${ev.id}.ics"`);
  res.setHeader('Content-Type', 'text/calendar; charset=UTF-8');
  res.send(ics);
});

app.get('/api/bookmarks', (req, res) => {
  const user = getSessionUser(req);
  if (!user) return res.status(401).json({ error: 'Authentication required.' });
  const userBookmarks = db.bookmarks
    .filter(b => b.userId === user.id)
    .map(b => ({ bookmark: b, event: db.events.find(e => e.id === b.eventId)! }))
    .filter(x => Boolean(x.event))
    .sort((a, b) => new Date(a.event.startTime).getTime() - new Date(b.event.startTime).getTime());

  const conflicts: any[] = [];
  const conflictingIds = new Set<number>();

  for (let i = 0; i < userBookmarks.length; i++) {
    const first = userBookmarks[i].event;
    if (first.cancelled || !first.startTime || !first.endTime) continue;
    for (let j = i + 1; j < userBookmarks.length; j++) {
      const second = userBookmarks[j].event;
      if (second.cancelled || !second.startTime || !second.endTime) continue;
      if (new Date(first.startTime) < new Date(second.endTime) && new Date(second.startTime) < new Date(first.endTime)) {
        conflictingIds.add(first.id);
        conflictingIds.add(second.id);
        conflicts.push({
          firstEventId: first.id,
          firstEventTitle: first.title,
          firstStart: first.startTime,
          firstEnd: first.endTime,
          secondEventId: second.id,
          secondEventTitle: second.title,
          secondStart: second.startTime,
          secondEnd: second.endTime,
          explanation: `Schedule conflict: "${first.title}" overlaps with "${second.title}".`
        });
      }
    }
  }

  const upcomingBookmarks: any[] = [];
  const pastBookmarks: any[] = [];
  let incompleteTimeCount = 0;
  const now = new Date();

  for (const { bookmark, event } of userBookmarks) {
    const isPast = new Date(event.startTime) < now;
    let overlapCheckStatus = 'NO_CONFLICT';
    let overlapCheckMessage = 'No schedule overlap with other saved events.';
    if (event.cancelled) {
      overlapCheckStatus = 'CANCELLED_SKIPPED';
      overlapCheckMessage = 'Event is cancelled — excluded from schedule conflict check.';
    } else if (!event.startTime || !event.endTime) {
      overlapCheckStatus = 'CANNOT_CHECK_OVERLAP';
      overlapCheckMessage = 'Cannot check overlap — end time not provided by organizer.';
      incompleteTimeCount++;
    } else if (conflictingIds.has(event.id)) {
      overlapCheckStatus = 'CONFLICT_DETECTED';
      overlapCheckMessage = 'Schedule conflict detected with another saved event.';
    }
    const item = {
      bookmarkId: bookmark.id,
      savedAt: bookmark.savedAt,
      pastEvent: isPast,
      overlapCheckStatus,
      overlapCheckMessage,
      event: toEventResponse(event, user.id)
    };
    if (isPast) pastBookmarks.push(item);
    else upcomingBookmarks.push(item);
  }

  res.json({ upcomingBookmarks, pastBookmarks, conflicts, incompleteTimeCount });
});

app.post('/api/bookmarks/:eventId', (req, res) => {
  const user = getSessionUser(req);
  if (!user) return res.status(401).json({ error: 'Authentication required.' });
  const eventId = parseInt(req.params.eventId, 10);
  const ev = db.events.find(e => e.id === eventId);
  if (!ev) return res.status(404).json({ error: 'Event not found.' });
  if (!db.bookmarks.some(b => b.userId === user.id && b.eventId === eventId)) {
    db.bookmarks.push({
      id: db.bookmarks.reduce((m, b) => Math.max(m, b.id), 0) + 1,
      userId: user.id,
      eventId,
      savedAt: toKolkataIso(new Date())
    });
    if (!db.participations.some(p => p.userId === user.id && p.eventId === eventId)) {
      db.participations.push({
        id: db.participations.reduce((m, p) => Math.max(m, p.id), 0) + 1,
        userId: user.id,
        eventId,
        status: 'SAVED',
        outcome: 'NONE',
        credits: 0,
        selfReported: true,
        verifiedByAdmin: false,
        studentNote: null,
        adminVerificationNote: null,
        updatedAt: toKolkataIso(new Date())
      });
    }
    saveDb(db);
  }
  res.json({ bookmarked: true, eventId });
});

app.delete('/api/bookmarks/:eventId', (req, res) => {
  const user = getSessionUser(req);
  if (!user) return res.status(401).json({ error: 'Authentication required.' });
  const eventId = parseInt(req.params.eventId, 10);
  db.bookmarks = db.bookmarks.filter(b => !(b.userId === user.id && b.eventId === eventId));
  saveDb(db);
  res.json({ bookmarked: false, eventId });
});

function calcCredits(eventType: string, status: string, outcome: string): number {
  if (status === 'SAVED') return 0;
  if (status === 'REGISTERED') return 5;
  if (status === 'ATTENDED') {
    const comp = eventType === 'HACKATHON' || eventType === 'COMPETITION';
    if (comp && outcome === 'WON') return 50;
    return 20;
  }
  return 0;
}

function toParticipationDto(p: ParticipationRecord) {
  const u = db.users.find(x => x.id === p.userId)!;
  const ev = db.events.find(x => x.id === p.eventId)!;
  const comp = ev.eventType === 'HACKATHON' || ev.eventType === 'COMPETITION';
  const credits = calcCredits(ev.eventType, p.status, p.outcome);
  return {
    id: p.id,
    userId: u.id,
    userFullName: u.fullName,
    userEmail: u.email,
    eventId: ev.id,
    eventTitle: ev.title,
    eventType: ev.eventType,
    eventTopic: ev.topic,
    eventStartTime: ev.startTime,
    eventDeleted: ev.deleted,
    competitiveEvent: comp,
    status: p.status,
    outcome: p.outcome,
    credits,
    selfReported: p.selfReported,
    verifiedByAdmin: p.verifiedByAdmin,
    verificationLabel: p.verifiedByAdmin ? 'Admin Verified' : 'Self-Reported',
    studentNote: p.studentNote,
    adminVerificationNote: p.adminVerificationNote,
    updatedAt: p.updatedAt
  };
}

app.get('/api/participations/summary', (req, res) => {
  const user = getSessionUser(req);
  if (!user) return res.status(401).json({ error: 'Authentication required.' });
  const records = db.participations
    .filter(p => p.userId === user.id)
    .sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime());

  const eventsSaved = db.bookmarks.filter(b => b.userId === user.id).length;
  let registeredEventsCount = 0;
  let attendedEventsCount = 0;
  let competitionsParticipatedCount = 0;
  let winsCount = 0;
  let completedCompetitionsWithOutcome = 0;
  let totalCredits = 0;
  let attendedLast30DaysCount = 0;

  const now = new Date();
  const thirtyDaysAgo = new Date(now.getTime() - 30 * 86400000);

  for (const p of records) {
    const ev = db.events.find(e => e.id === p.eventId);
    if (!ev) continue;
    const comp = ev.eventType === 'HACKATHON' || ev.eventType === 'COMPETITION';
    const cr = calcCredits(ev.eventType, p.status, p.outcome);
    totalCredits += cr;

    if (p.status === 'REGISTERED') registeredEventsCount++;
    else if (p.status === 'ATTENDED') {
      attendedEventsCount++;
      const st = new Date(ev.startTime);
      if (st >= thirtyDaysAgo && st <= new Date(now.getTime() + 86400000)) {
        attendedLast30DaysCount++;
      }
      if (comp) {
        competitionsParticipatedCount++;
        if (p.outcome === 'WON') {
          winsCount++;
          completedCompetitionsWithOutcome++;
        } else if (p.outcome === 'NOT_WON') {
          completedCompetitionsWithOutcome++;
        }
      }
    }
  }

  let winRateAvailable = false;
  let winRatePercentage: number | null = null;
  let winRateDisplay = 'N/A';
  let winRateExplanation = 'No completed hackathons or competitions with a recorded Won/Not Won outcome yet. (Workshops, conferences, webinars, registrations, and pending outcomes are excluded.)';

  if (completedCompetitionsWithOutcome > 0) {
    winRateAvailable = true;
    winRatePercentage = Math.round((winsCount / completedCompetitionsWithOutcome) * 1000) / 10;
    winRateDisplay = `${winRatePercentage}% (${winsCount} / ${completedCompetitionsWithOutcome})`;
    winRateExplanation = `${winsCount} win(s) out of ${completedCompetitionsWithOutcome} completed hackathon/competition event(s) with a recorded Won/Not Won outcome.`;
  }

  let currentBadge = 'Explorer';
  let badgeTierRange = '0 – 49 credits';
  let nextBadge = 'Builder';
  let creditsToNextBadge = 50 - totalCredits;
  let badgeProgressPercent = Math.min(100, Math.round((totalCredits / 50) * 100));

  if (totalCredits >= 300) {
    currentBadge = 'Champion';
    badgeTierRange = '300+ credits';
    nextBadge = 'Max Tier Reached';
    creditsToNextBadge = 0;
    badgeProgressPercent = 100;
  } else if (totalCredits >= 150) {
    currentBadge = 'Challenger';
    badgeTierRange = '150 – 299 credits';
    nextBadge = 'Champion';
    creditsToNextBadge = 300 - totalCredits;
    badgeProgressPercent = Math.min(100, Math.round(((totalCredits - 150) / 150) * 100));
  } else if (totalCredits >= 50) {
    currentBadge = 'Builder';
    badgeTierRange = '50 – 149 credits';
    nextBadge = 'Challenger';
    creditsToNextBadge = 150 - totalCredits;
    badgeProgressPercent = Math.min(100, Math.round(((totalCredits - 50) / 100) * 100));
  }

  res.json({
    eventsSaved,
    registeredEventsCount,
    attendedEventsCount,
    competitionsParticipatedCount,
    winsCount,
    totalCredits,
    attendedLast30DaysCount,
    winRateAvailable,
    winRatePercentage,
    winRateDisplay,
    winRateNumerator: winsCount,
    winRateDenominator: completedCompetitionsWithOutcome,
    winRateExplanation,
    currentBadge,
    badgeTierRange,
    nextBadge,
    creditsToNextBadge,
    badgeProgressPercent,
    badgeDisclaimer: 'Application engagement badge based on self-reported and admin-verified activity — not an official academic certification.',
    activityHistory: records.map(toParticipationDto)
  });
});

app.post('/api/participations', (req, res) => {
  const user = getSessionUser(req);
  if (!user) return res.status(401).json({ error: 'Authentication required.' });
  const { eventId, status, outcome, studentNote } = req.body || {};
  const ev = db.events.find(e => e.id === Number(eventId));
  if (!ev) return res.status(404).json({ error: 'Event not found.' });

  const comp = ev.eventType === 'HACKATHON' || ev.eventType === 'COMPETITION';
  let normOutcome: 'NONE' | 'PENDING' | 'WON' | 'NOT_WON' = 'NONE';
  if (comp) {
    if (status !== 'ATTENDED') {
      if (outcome === 'WON' || outcome === 'NOT_WON') {
        return res.status(400).json({ error: 'Competition result (Won / Not Won) can only be recorded when participation status is Attended.' });
      }
      normOutcome = outcome === 'PENDING' ? 'PENDING' : 'NONE';
    } else {
      normOutcome = outcome && outcome !== 'NONE' ? outcome : 'PENDING';
    }
  }

  const credits = calcCredits(ev.eventType, status, normOutcome);
  let record = db.participations.find(p => p.userId === user.id && p.eventId === ev.id);
  if (record) {
    if (record.status !== status || record.outcome !== normOutcome) {
      record.verifiedByAdmin = false;
    }
    record.status = status;
    record.outcome = normOutcome;
    record.credits = credits;
    record.selfReported = true;
    if (studentNote !== undefined && studentNote !== '') record.studentNote = String(studentNote).trim();
    record.updatedAt = toKolkataIso(new Date());
  } else {
    record = {
      id: db.participations.reduce((m, p) => Math.max(m, p.id), 0) + 1,
      userId: user.id,
      eventId: ev.id,
      status,
      outcome: normOutcome,
      credits,
      selfReported: true,
      verifiedByAdmin: false,
      studentNote: studentNote ? String(studentNote).trim() : null,
      adminVerificationNote: null,
      updatedAt: toKolkataIso(new Date())
    };
    db.participations.push(record);
  }
  saveDb(db);
  res.json(toParticipationDto(record));
});

app.get('/api/admin/events', (req, res) => {
  const user = getSessionUser(req);
  if (!user || user.role !== 'ADMIN') return res.status(403).json({ error: 'Forbidden: Administrator privileges required.' });
  const list = db.events
    .filter(e => !e.deleted)
    .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime())
    .map(e => toEventResponse(e, null));
  res.json(list);
});

app.post('/api/admin/events', (req, res) => {
  const user = getSessionUser(req);
  if (!user || user.role !== 'ADMIN') return res.status(403).json({ error: 'Forbidden: Administrator privileges required.' });
  const b = req.body || {};
  if (!b.title || !b.description || !b.startTime) {
    return res.status(400).json({ error: 'Title, description, and start time are required.' });
  }
  if (b.endTime && new Date(b.endTime) < new Date(b.startTime)) {
    return res.status(400).json({ error: 'Event end time cannot be earlier than the start time.' });
  }
  if (b.registrationDeadline && new Date(b.registrationDeadline) > new Date(b.startTime)) {
    return res.status(400).json({ error: 'Registration deadline cannot be after the event start time.' });
  }
  if (b.costType === 'PAID' && (!b.price || Number(b.price) <= 0)) {
    return res.status(400).json({ error: 'Paid events must specify a positive price amount.' });
  }
  const nowIso = toKolkataIso(new Date());
  const created: EventRecord = {
    id: db.events.reduce((m, e) => Math.max(m, e.id), 0) + 1,
    title: String(b.title).trim(),
    description: String(b.description).trim(),
    topic: String(b.topic || 'Other').trim(),
    eventType: b.eventType || 'WORKSHOP',
    mode: b.mode || 'ONLINE',
    organizer: String(b.organizer || 'Not provided').trim(),
    city: String(b.city || 'Online').trim(),
    venueOrPlatform: b.venueOrPlatform ? String(b.venueOrPlatform).trim() : 'Not provided',
    startTime: b.startTime,
    endTime: b.endTime || null,
    registrationDeadline: b.registrationDeadline || null,
    costType: b.costType || 'NOT_PROVIDED',
    price: b.costType === 'FREE' ? 0 : (b.costType === 'PAID' ? Number(b.price) : null),
    currency: 'INR',
    eligibility: b.eligibility ? String(b.eligibility).trim() : 'Not provided',
    registrationUrl: b.registrationUrl || null,
    officialSourceUrl: b.officialSourceUrl || null,
    sourceName: 'TECHPULSE Admin Portal',
    externalEventId: null,
    dataOrigin: 'MANUAL_ADMIN',
    lastFetchedAt: nowIso,
    cancelled: Boolean(b.cancelled),
    registrationClosed: Boolean(b.registrationClosed),
    deleted: false,
    manuallyEdited: true,
    createdAt: nowIso,
    updatedAt: nowIso
  };
  db.events.push(created);
  saveDb(db);
  res.status(201).json(toEventResponse(created, null));
});

app.put('/api/admin/events/:id', (req, res) => {
  const user = getSessionUser(req);
  if (!user || user.role !== 'ADMIN') return res.status(403).json({ error: 'Forbidden: Administrator privileges required.' });
  const id = parseInt(req.params.id, 10);
  const ev = db.events.find(e => e.id === id);
  if (!ev) return res.status(404).json({ error: 'Event not found.' });
  const b = req.body || {};
  if (b.endTime && new Date(b.endTime) < new Date(b.startTime)) {
    return res.status(400).json({ error: 'Event end time cannot be earlier than the start time.' });
  }
  if (b.registrationDeadline && new Date(b.registrationDeadline) > new Date(b.startTime)) {
    return res.status(400).json({ error: 'Registration deadline cannot be after the event start time.' });
  }
  ev.title = String(b.title || ev.title).trim();
  ev.description = String(b.description || ev.description).trim();
  ev.topic = String(b.topic || ev.topic).trim();
  ev.eventType = b.eventType || ev.eventType;
  ev.mode = b.mode || ev.mode;
  ev.organizer = String(b.organizer || ev.organizer).trim();
  ev.city = String(b.city || ev.city).trim();
  ev.venueOrPlatform = b.venueOrPlatform ? String(b.venueOrPlatform).trim() : 'Not provided';
  ev.startTime = b.startTime || ev.startTime;
  ev.endTime = b.endTime || null;
  ev.registrationDeadline = b.registrationDeadline || null;
  ev.costType = b.costType || ev.costType;
  ev.price = ev.costType === 'FREE' ? 0 : (ev.costType === 'PAID' ? Number(b.price) : null);
  ev.eligibility = b.eligibility ? String(b.eligibility).trim() : 'Not provided';
  ev.registrationUrl = b.registrationUrl || null;
  ev.officialSourceUrl = b.officialSourceUrl || null;
  ev.cancelled = Boolean(b.cancelled);
  ev.registrationClosed = Boolean(b.registrationClosed);
  ev.manuallyEdited = true;
  ev.updatedAt = toKolkataIso(new Date());
  saveDb(db);
  res.json(toEventResponse(ev, null));
});

app.patch('/api/admin/events/:id/cancel', (req, res) => {
  const user = getSessionUser(req);
  if (!user || user.role !== 'ADMIN') return res.status(403).json({ error: 'Forbidden: Administrator privileges required.' });
  const id = parseInt(req.params.id, 10);
  const ev = db.events.find(e => e.id === id);
  if (!ev) return res.status(404).json({ error: 'Event not found.' });
  ev.cancelled = Boolean(req.body?.cancelled);
  ev.manuallyEdited = true;
  ev.updatedAt = toKolkataIso(new Date());
  saveDb(db);
  res.json(toEventResponse(ev, null));
});

app.delete('/api/admin/events/:id', (req, res) => {
  const user = getSessionUser(req);
  if (!user || user.role !== 'ADMIN') return res.status(403).json({ error: 'Forbidden: Administrator privileges required.' });
  const id = parseInt(req.params.id, 10);
  const ev = db.events.find(e => e.id === id);
  if (!ev) return res.status(404).json({ error: 'Event not found.' });
  ev.deleted = true;
  ev.manuallyEdited = true;
  ev.updatedAt = toKolkataIso(new Date());
  saveDb(db);
  res.json({ deleted: true, eventId: id, policy: 'Soft-deleted from public discovery. Student bookmarks and participation records remain intact.' });
});

app.get('/api/admin/participations', (req, res) => {
  const user = getSessionUser(req);
  if (!user || user.role !== 'ADMIN') return res.status(403).json({ error: 'Forbidden: Administrator privileges required.' });
  const list = db.participations
    .slice()
    .sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime())
    .map(toParticipationDto);
  res.json(list);
});

app.put('/api/admin/participations/:id/verify', (req, res) => {
  const user = getSessionUser(req);
  if (!user || user.role !== 'ADMIN') return res.status(403).json({ error: 'Forbidden: Administrator privileges required.' });
  const id = parseInt(req.params.id, 10);
  const p = db.participations.find(x => x.id === id);
  if (!p) return res.status(404).json({ error: 'Participation record not found.' });
  const ev = db.events.find(e => e.id === p.eventId)!;
  const { status, outcome, verifiedByAdmin, adminVerificationNote } = req.body || {};
  const comp = ev.eventType === 'HACKATHON' || ev.eventType === 'COMPETITION';
  const normOutcome = comp ? (outcome || 'PENDING') : 'NONE';
  p.status = status || p.status;
  p.outcome = normOutcome;
  p.credits = calcCredits(ev.eventType, p.status, p.outcome);
  p.verifiedByAdmin = Boolean(verifiedByAdmin);
  p.adminVerificationNote = adminVerificationNote ? String(adminVerificationNote).trim() : null;
  p.updatedAt = toKolkataIso(new Date());
  saveDb(db);
  res.json(toParticipationDto(p));
});

// Serve Spring Boot static resources directly (HTML5, Plain CSS3, Vanilla JS)
app.use(express.static(STATIC_DIR));
app.get('*', (_req, res) => {
  res.sendFile(path.join(STATIC_DIR, 'index.html'));
});

const PORT = 3000;
app.listen(PORT, '0.0.0.0', () => {
  console.log(`TECHPULSE Preview Server serving src/main/resources/static on http://0.0.0.0:${PORT}`);
});
