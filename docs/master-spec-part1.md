# MASTER SPEC — PART 1
## Istanbul Disaster Coordination & Volunteer Management Platform

**Version:** 1.0  
**Date:** 2026-03-03  
**Scope:** Domain Model · PostgreSQL Schema · RBAC Matrix · Coordinator Hierarchy  
**Status:** Authoritative contract — no implementation yet

---

## Table of Contents

1. [Full Domain Model](#1-full-domain-model)
2. [ERD-Level PostgreSQL Schema](#2-erd-level-postgresql-schema)
3. [RBAC Matrix](#3-rbac-matrix)
4. [Coordinator Hierarchy Rules & Constraints](#4-coordinator-hierarchy-rules--constraints)

---

## 1. Full Domain Model

### 1.1 Core Entities Overview

```
User ──< UserRole
User ──< Document
User ──< TeamMembership ──> Team
User ── District (home district)
User ── Neighborhood (home neighborhood)

District ──< Neighborhood
District ── DistrictCoordinator (User)

Neighborhood ── NeighborhoodCoordinator (User)
Neighborhood ──< AssemblyArea

Event ── Neighborhood
Event ── Team
Event ──< EventVolunteer ──> User
Event ── CreatedBy (User)

Team ── TeamLeader (User)
Team ──< TeamMembership ──> User

Document ── User
Document ── ReviewedBy (User, nullable)

EarthquakeSimulation ── District
EarthquakeSimulation ── CreatedBy (User, Admin only)
EarthquakeSimulation ──< SimulationNotificationLog ──> User
```

---

### 1.2 Entity Definitions

#### 1.2.1 User

Represents any authenticated person in the system. A single user holds exactly one role at a time.

| Field            | Type        | Notes                                              |
|------------------|-------------|---------------------------------------------------|
| id               | UUID        | Primary key                                        |
| first_name       | VARCHAR(100)| Required                                           |
| last_name        | VARCHAR(100)| Required                                           |
| email            | VARCHAR(255)| Unique, required, verified flag separate           |
| email_verified   | BOOLEAN     | Default false; must be true to act as coordinator  |
| pending_email    | VARCHAR(255)| Nullable; stores new email during change flow      |
| phone            | VARCHAR(20) | Required, unique                                   |
| blood_type       | ENUM        | A+, A-, B+, B-, AB+, AB-, O+, O-                  |
| district_id      | UUID FK     | Home district (one of 10 active)                   |
| neighborhood_id  | UUID FK     | Home neighborhood, must belong to district_id      |
| address          | TEXT        | Free-form address string                           |
| profession       | VARCHAR(255)| Free-form profession text                          |
| password_hash    | VARCHAR(255)| BCrypt hash                                        |
| role             | ENUM        | ADMIN, DISTRICT_COORDINATOR, NEIGHBORHOOD_COORDINATOR, VOLUNTEER |
| is_active        | BOOLEAN     | Soft-delete / account suspension flag, default true|
| created_at       | TIMESTAMPTZ | Auto-set on insert                                 |
| updated_at       | TIMESTAMPTZ | Auto-updated on any change                         |

**Business rules:**
- A user must belong to one of the 10 active Istanbul districts.
- `neighborhood_id` must be a neighborhood that belongs to `district_id`.
- Password stored only as BCrypt hash (cost factor ≥ 12).

---

#### 1.2.2 District

One of the 10 active pilot districts in Istanbul.

| Field                  | Type         | Notes                                         |
|------------------------|--------------|-----------------------------------------------|
| id                     | UUID         | Primary key                                   |
| name                   | VARCHAR(100) | Unique; e.g. "Pendik"                         |
| coordinator_id         | UUID FK      | FK → users.id; nullable (unassigned state)    |
| geojson_polygon        | JSONB        | GeoJSON Polygon geometry for map rendering    |
| risk_score             | NUMERIC(10,4)| Denormalized cache; recomputed on event change|
| risk_score_updated_at  | TIMESTAMPTZ  | Timestamp of last risk score computation      |
| is_active              | BOOLEAN      | Only active districts are shown on map        |
| created_at             | TIMESTAMPTZ  |                                               |
| updated_at             | TIMESTAMPTZ  |                                               |

**Seeded values (immutable during pilot):**
Pendik, Kartal, Tuzla, Kadıköy, Ataşehir, Bahçelievler, Beşiktaş, Bakırköy, Fatih, Avcılar

---

#### 1.2.3 Neighborhood

A sub-unit within a district. Multiple neighborhoods belong to one district.

| Field                  | Type         | Notes                                         |
|------------------------|--------------|-----------------------------------------------|
| id                     | UUID         | Primary key                                   |
| name                   | VARCHAR(150) | Required                                      |
| district_id            | UUID FK      | FK → districts.id; NOT NULL                   |
| coordinator_id         | UUID FK      | FK → users.id; nullable                       |
| geojson_polygon        | JSONB        | GeoJSON Polygon for map                       |
| risk_score             | NUMERIC(10,4)| Denormalized; sum of event risks              |
| risk_score_updated_at  | TIMESTAMPTZ  |                                               |
| created_at             | TIMESTAMPTZ  |                                               |
| updated_at             | TIMESTAMPTZ  |                                               |

---

#### 1.2.4 AssemblyArea

Designated gathering points for a neighborhood during disaster.

| Field           | Type          | Notes                                          |
|-----------------|---------------|------------------------------------------------|
| id              | UUID          | Primary key                                    |
| neighborhood_id | UUID FK       | FK → neighborhoods.id; NOT NULL                |
| name            | VARCHAR(200)  | e.g. "Kurtköy Meydanı Toplanma Alanı"         |
| latitude        | DECIMAL(10,7) | Required                                       |
| longitude       | DECIMAL(10,7) | Required                                       |
| capacity        | INTEGER       | Estimated person capacity                      |
| description     | TEXT          | Nullable                                       |
| created_at      | TIMESTAMPTZ   |                                                |

---

#### 1.2.5 Team

One of the 6 operational teams. Teams are system-defined and seeded.

| Field           | Type         | Notes                                                  |
|-----------------|--------------|--------------------------------------------------------|
| id              | UUID         | Primary key                                            |
| name            | ENUM         | SEARCH_RESCUE, FOOD_WATER, LOGISTICS, EVACUATION, COMMUNICATION, PSYCHOSOCIAL |
| coefficient     | NUMERIC(4,2) | Risk weight: SR=5, EVA=4, FW=3, LOG=2, COM=2, PSY=3   |
| requires_document | ENUM       | Nullable; SEARCH_RESCUE_CERTIFICATE or PSYCHOSOCIAL_GRADUATION_DOCUMENT |
| description     | TEXT         | Nullable                                               |
| leader_id       | UUID FK      | FK → users.id; nullable                                |
| created_at      | TIMESTAMPTZ  |                                                        |
| updated_at      | TIMESTAMPTZ  |                                                        |

**Team → Required Document mapping:**
- Search & Rescue → SearchRescueCertificate (must be APPROVED)
- Psychosocial Support → PsychosocialGraduationDocument (must be APPROVED)
- All other teams → no document required

---

#### 1.2.6 TeamMembership

Join table linking users to teams. A user may belong to multiple teams.

| Field      | Type        | Notes                                               |
|------------|-------------|-----------------------------------------------------|
| id         | UUID        | Primary key                                         |
| user_id    | UUID FK     | FK → users.id; NOT NULL                             |
| team_id    | UUID FK     | FK → teams.id; NOT NULL                             |
| joined_at  | TIMESTAMPTZ | Auto-set on insert                                  |
| is_active  | BOOLEAN     | Allows soft removal from team without hard delete   |

**Unique constraint:** (user_id, team_id)

---

#### 1.2.7 Event

A disaster response task assigned to a team in a neighborhood.

| Field            | Type          | Notes                                             |
|------------------|---------------|---------------------------------------------------|
| id               | UUID          | Primary key                                       |
| title            | VARCHAR(300)  | Required                                          |
| description      | TEXT          | Detailed description                              |
| neighborhood_id  | UUID FK       | FK → neighborhoods.id; NOT NULL                   |
| team_id          | UUID FK       | FK → teams.id; NOT NULL                           |
| created_by       | UUID FK       | FK → users.id; the coordinator who created it     |
| status           | ENUM          | OPEN, CLOSED                                      |
| urgency          | SMALLINT      | 1–5 inclusive                                     |
| required_people  | INTEGER       | Minimum volunteers needed                         |
| risk_score       | NUMERIC(10,4) | Computed: team_coeff × urgency × required_people × status_coeff |
| latitude         | DECIMAL(10,7) | Nullable; event location pin                      |
| longitude        | DECIMAL(10,7) | Nullable                                          |
| starts_at        | TIMESTAMPTZ   | Nullable; planned start                           |
| ends_at          | TIMESTAMPTZ   | Nullable; planned end                             |
| closed_at        | TIMESTAMPTZ   | Auto-set when status → CLOSED                     |
| created_at       | TIMESTAMPTZ   |                                                   |
| updated_at       | TIMESTAMPTZ   |                                                   |

**Risk computation:**
```
EventRisk = team.coefficient × urgency × required_people × StatusCoefficient
StatusCoefficient: OPEN = 1.0, CLOSED = 0.2
```

---

#### 1.2.8 EventVolunteer

Tracks which volunteers are assigned to which event.

| Field       | Type        | Notes                                              |
|-------------|-------------|----------------------------------------------------|
| id          | UUID        | Primary key                                        |
| event_id    | UUID FK     | FK → events.id; NOT NULL                           |
| user_id     | UUID FK     | FK → users.id; NOT NULL                            |
| joined_at   | TIMESTAMPTZ | Auto-set on insert                                 |
| left_at     | TIMESTAMPTZ | Nullable; set when volunteer exits event           |
| status      | ENUM        | ASSIGNED, COMPLETED, WITHDRAWN                     |

**Unique constraint:** (event_id, user_id)  
**Rule:** User must be a member of the event's team to be assigned.

---

#### 1.2.9 Document

A certification document uploaded by a volunteer for team join eligibility.

| Field           | Type         | Notes                                              |
|-----------------|--------------|----------------------------------------------------|
| id              | UUID         | Primary key                                        |
| user_id         | UUID FK      | FK → users.id; NOT NULL                            |
| document_type   | ENUM         | SEARCH_RESCUE_CERTIFICATE, PSYCHOSOCIAL_GRADUATION_DOCUMENT |
| status          | ENUM         | PENDING, APPROVED, REJECTED                        |
| storage_key     | VARCHAR(500) | S3-compatible object key; path to uploaded file    |
| file_name       | VARCHAR(300) | Original file name                                 |
| file_size_bytes | BIGINT       | File size for audit                                |
| mime_type       | VARCHAR(100) | e.g. application/pdf, image/jpeg                  |
| rejection_reason| TEXT         | Nullable; required if status = REJECTED            |
| reviewed_by     | UUID FK      | FK → users.id; nullable; Admin/DC who reviewed     |
| reviewed_at     | TIMESTAMPTZ  | Nullable                                           |
| created_at      | TIMESTAMPTZ  |                                                    |
| updated_at      | TIMESTAMPTZ  |                                                    |

**Business rules:**
- A user may have multiple documents of each type (re-upload after rejection).
- Only the latest APPROVED document per type is considered valid.
- Only ADMIN or DISTRICT_COORDINATOR may change status from PENDING.

---

#### 1.2.10 EarthquakeSimulation

Admin-triggered simulation broadcast to all users.

| Field        | Type          | Notes                                             |
|--------------|---------------|---------------------------------------------------|
| id           | UUID          | Primary key                                       |
| district_id  | UUID FK       | FK → districts.id; target district                |
| magnitude    | DECIMAL(3,1)  | Richter scale, e.g. 7.2                           |
| created_by   | UUID FK       | FK → users.id; must be ADMIN                      |
| notes        | TEXT          | Optional admin notes attached to simulation       |
| triggered_at | TIMESTAMPTZ   | Auto-set on insert                                |
| email_status | ENUM          | QUEUED, PROCESSING, COMPLETED, PARTIAL_FAILURE    |
| created_at   | TIMESTAMPTZ   |                                                   |

---

#### 1.2.11 SimulationNotificationLog

Audit record of each email sent during an earthquake simulation.

| Field           | Type         | Notes                                             |
|-----------------|--------------|---------------------------------------------------|
| id              | UUID         | Primary key                                       |
| simulation_id   | UUID FK      | FK → earthquake_simulations.id; NOT NULL          |
| user_id         | UUID FK      | FK → users.id; NOT NULL                           |
| email_address   | VARCHAR(255) | Snapshot of email at send time                    |
| status          | ENUM         | QUEUED, SENT, FAILED, BOUNCED                     |
| sent_at         | TIMESTAMPTZ  | Nullable                                          |
| retry_count     | SMALLINT     | Default 0                                         |
| last_error      | TEXT         | Nullable; last failure message                    |
| created_at      | TIMESTAMPTZ  |                                                   |
| updated_at      | TIMESTAMPTZ  |                                                   |

---

#### 1.2.12 AuditLog

Immutable system-wide audit trail.

| Field        | Type         | Notes                                               |
|--------------|--------------|-----------------------------------------------------|
| id           | UUID         | Primary key                                         |
| actor_id     | UUID         | FK → users.id nullable (system actions allowed)     |
| actor_role   | VARCHAR(50)  | Snapshot of role at event time                      |
| action       | VARCHAR(100) | e.g. DOCUMENT_APPROVED, EVENT_CLOSED               |
| entity_type  | VARCHAR(100) | e.g. Document, Event, User                          |
| entity_id    | UUID         | ID of the affected entity                           |
| old_value    | JSONB        | Nullable; previous state snapshot                   |
| new_value    | JSONB        | Nullable; new state snapshot                        |
| ip_address   | VARCHAR(45)  | Nullable; IPv4 or IPv6                             |
| created_at   | TIMESTAMPTZ  | Immutable; no updates allowed                       |

---

#### 1.2.13 EmailVerificationToken

Token for email change verification flow.

| Field      | Type         | Notes                                             |
|------------|--------------|---------------------------------------------------|
| id         | UUID         | Primary key                                       |
| user_id    | UUID FK      | FK → users.id; NOT NULL                           |
| token      | VARCHAR(128) | Cryptographically random; unique                  |
| new_email  | VARCHAR(255) | The email address being verified                  |
| expires_at | TIMESTAMPTZ  | Token TTL = 24 hours from creation                |
| used_at    | TIMESTAMPTZ  | Nullable; set on first use; one-time use only     |
| created_at | TIMESTAMPTZ  |                                                   |

---

#### 1.2.14 RefreshToken

JWT refresh token store for secure session management.

| Field       | Type         | Notes                                            |
|-------------|--------------|--------------------------------------------------|
| id          | UUID         | Primary key                                      |
| user_id     | UUID FK      | FK → users.id; NOT NULL                          |
| token_hash  | VARCHAR(255) | SHA-256 hash of the raw token; unique            |
| expires_at  | TIMESTAMPTZ  | Default: now() + 30 days                         |
| revoked     | BOOLEAN      | Default false                                    |
| revoked_at  | TIMESTAMPTZ  | Nullable                                         |
| user_agent  | TEXT         | Nullable; client info                            |
| ip_address  | VARCHAR(45)  | Nullable                                         |
| created_at  | TIMESTAMPTZ  |                                                  |

---

### 1.3 Entity Relationship Summary

```
districts (1) ──── (N) neighborhoods
districts (1) ──── (0..1) users [coordinator_id]

neighborhoods (1) ──── (N) assembly_areas
neighborhoods (1) ──── (0..1) users [coordinator_id]
neighborhoods (1) ──── (N) events

teams (1) ──── (N) team_memberships ──── (N) users
teams (1) ──── (N) events

events (1) ──── (N) event_volunteers ──── (N) users

users (1) ──── (N) documents
users (1) ──── (N) audit_logs [actor_id]
users (1) ──── (N) email_verification_tokens
users (1) ──── (N) refresh_tokens

earthquake_simulations (1) ──── (N) simulation_notification_logs
earthquake_simulations (N) ──── (1) districts
```

---

## 2. ERD-Level PostgreSQL Schema

> All tables use `UUID` primary keys generated via `gen_random_uuid()`.  
> All timestamps are `TIMESTAMPTZ` (time zone aware).  
> Schema name: `public` (default).

---

### 2.1 ENUMs

```sql
-- User roles
CREATE TYPE user_role AS ENUM (
    'ADMIN',
    'DISTRICT_COORDINATOR',
    'NEIGHBORHOOD_COORDINATOR',
    'VOLUNTEER'
);

-- Blood types
CREATE TYPE blood_type AS ENUM (
    'A_POSITIVE', 'A_NEGATIVE',
    'B_POSITIVE', 'B_NEGATIVE',
    'AB_POSITIVE', 'AB_NEGATIVE',
    'O_POSITIVE', 'O_NEGATIVE'
);

-- Team names
CREATE TYPE team_name AS ENUM (
    'SEARCH_RESCUE',
    'FOOD_WATER',
    'LOGISTICS',
    'EVACUATION',
    'COMMUNICATION',
    'PSYCHOSOCIAL'
);

-- Document types
CREATE TYPE document_type AS ENUM (
    'SEARCH_RESCUE_CERTIFICATE',
    'PSYCHOSOCIAL_GRADUATION_DOCUMENT'
);

-- Document status
CREATE TYPE document_status AS ENUM (
    'PENDING',
    'APPROVED',
    'REJECTED'
);

-- Event status
CREATE TYPE event_status AS ENUM (
    'OPEN',
    'CLOSED'
);

-- EventVolunteer status
CREATE TYPE event_volunteer_status AS ENUM (
    'ASSIGNED',
    'COMPLETED',
    'WITHDRAWN'
);

-- Simulation email status
CREATE TYPE simulation_email_status AS ENUM (
    'QUEUED',
    'PROCESSING',
    'COMPLETED',
    'PARTIAL_FAILURE'
);

-- Notification log status
CREATE TYPE notification_status AS ENUM (
    'QUEUED',
    'SENT',
    'FAILED',
    'BOUNCED'
);
```

---

### 2.2 Table Definitions

#### districts

```sql
CREATE TABLE districts (
    id                     UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name                   VARCHAR(100) NOT NULL UNIQUE,
    coordinator_id         UUID         REFERENCES users(id) ON DELETE SET NULL,
    geojson_polygon        JSONB,
    risk_score             NUMERIC(10,4) NOT NULL DEFAULT 0,
    risk_score_updated_at  TIMESTAMPTZ,
    is_active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_districts_is_active      ON districts(is_active);
CREATE INDEX idx_districts_coordinator_id ON districts(coordinator_id);
```

---

#### neighborhoods

```sql
CREATE TABLE neighborhoods (
    id                     UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    name                   VARCHAR(150)  NOT NULL,
    district_id            UUID          NOT NULL REFERENCES districts(id) ON DELETE RESTRICT,
    coordinator_id         UUID          REFERENCES users(id) ON DELETE SET NULL,
    geojson_polygon        JSONB,
    risk_score             NUMERIC(10,4) NOT NULL DEFAULT 0,
    risk_score_updated_at  TIMESTAMPTZ,
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT uq_neighborhood_name_district UNIQUE (name, district_id)
);

CREATE INDEX idx_neighborhoods_district_id    ON neighborhoods(district_id);
CREATE INDEX idx_neighborhoods_coordinator_id ON neighborhoods(coordinator_id);
```

---

#### users

```sql
CREATE TABLE users (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    pending_email   VARCHAR(255),
    phone           VARCHAR(20)  NOT NULL UNIQUE,
    blood_type      blood_type   NOT NULL,
    district_id     UUID         NOT NULL REFERENCES districts(id) ON DELETE RESTRICT,
    neighborhood_id UUID         NOT NULL REFERENCES neighborhoods(id) ON DELETE RESTRICT,
    address         TEXT         NOT NULL,
    profession      VARCHAR(255),
    password_hash   VARCHAR(255) NOT NULL,
    role            user_role    NOT NULL DEFAULT 'VOLUNTEER',
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_user_neighborhood_district
        FOREIGN KEY (neighborhood_id) REFERENCES neighborhoods(id)
);

CREATE INDEX idx_users_email           ON users(email);
CREATE INDEX idx_users_phone           ON users(phone);
CREATE INDEX idx_users_district_id     ON users(district_id);
CREATE INDEX idx_users_neighborhood_id ON users(neighborhood_id);
CREATE INDEX idx_users_role            ON users(role);
CREATE INDEX idx_users_is_active       ON users(is_active);
```

> **Note:** The application layer enforces that `neighborhood_id.district_id = users.district_id`. A database-level CHECK using a subquery is not standard SQL; this constraint is enforced via a trigger (see §2.3).

---

#### assembly_areas

```sql
CREATE TABLE assembly_areas (
    id               UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    neighborhood_id  UUID           NOT NULL REFERENCES neighborhoods(id) ON DELETE CASCADE,
    name             VARCHAR(200)   NOT NULL,
    latitude         DECIMAL(10,7)  NOT NULL,
    longitude        DECIMAL(10,7)  NOT NULL,
    capacity         INTEGER,
    description      TEXT,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_assembly_areas_neighborhood_id ON assembly_areas(neighborhood_id);
```

---

#### teams

```sql
CREATE TABLE teams (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name              team_name    NOT NULL UNIQUE,
    coefficient       NUMERIC(4,2) NOT NULL,
    requires_document document_type,
    description       TEXT,
    leader_id         UUID         REFERENCES users(id) ON DELETE SET NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Seed data (to be inserted at startup):
-- SEARCH_RESCUE      coefficient=5, requires_document=SEARCH_RESCUE_CERTIFICATE
-- FOOD_WATER         coefficient=3, requires_document=NULL
-- LOGISTICS          coefficient=2, requires_document=NULL
-- EVACUATION         coefficient=4, requires_document=NULL
-- COMMUNICATION      coefficient=2, requires_document=NULL
-- PSYCHOSOCIAL       coefficient=3, requires_document=PSYCHOSOCIAL_GRADUATION_DOCUMENT
```

---

#### team_memberships

```sql
CREATE TABLE team_memberships (
    id        UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id   UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    team_id   UUID        NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_active BOOLEAN     NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_team_membership UNIQUE (user_id, team_id)
);

CREATE INDEX idx_team_memberships_user_id ON team_memberships(user_id);
CREATE INDEX idx_team_memberships_team_id ON team_memberships(team_id);
```

---

#### events

```sql
CREATE TABLE events (
    id               UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    title            VARCHAR(300)   NOT NULL,
    description      TEXT,
    neighborhood_id  UUID           NOT NULL REFERENCES neighborhoods(id) ON DELETE RESTRICT,
    team_id          UUID           NOT NULL REFERENCES teams(id) ON DELETE RESTRICT,
    created_by       UUID           NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    status           event_status   NOT NULL DEFAULT 'OPEN',
    urgency          SMALLINT       NOT NULL CHECK (urgency BETWEEN 1 AND 5),
    required_people  INTEGER        NOT NULL CHECK (required_people > 0),
    risk_score       NUMERIC(10,4)  NOT NULL DEFAULT 0,
    latitude         DECIMAL(10,7),
    longitude        DECIMAL(10,7),
    starts_at        TIMESTAMPTZ,
    ends_at          TIMESTAMPTZ,
    closed_at        TIMESTAMPTZ,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT chk_event_dates CHECK (ends_at IS NULL OR starts_at IS NULL OR ends_at >= starts_at)
);

CREATE INDEX idx_events_neighborhood_id ON events(neighborhood_id);
CREATE INDEX idx_events_team_id         ON events(team_id);
CREATE INDEX idx_events_status          ON events(status);
CREATE INDEX idx_events_created_by      ON events(created_by);
CREATE INDEX idx_events_urgency         ON events(urgency);
```

---

#### event_volunteers

```sql
CREATE TABLE event_volunteers (
    id         UUID                   PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id   UUID                   NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    user_id    UUID                   NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at  TIMESTAMPTZ            NOT NULL DEFAULT now(),
    left_at    TIMESTAMPTZ,
    status     event_volunteer_status NOT NULL DEFAULT 'ASSIGNED',

    CONSTRAINT uq_event_volunteer UNIQUE (event_id, user_id)
);

CREATE INDEX idx_event_volunteers_event_id ON event_volunteers(event_id);
CREATE INDEX idx_event_volunteers_user_id  ON event_volunteers(user_id);
```

---

#### documents

```sql
CREATE TABLE documents (
    id               UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    document_type    document_type   NOT NULL,
    status           document_status NOT NULL DEFAULT 'PENDING',
    storage_key      VARCHAR(500)    NOT NULL,
    file_name        VARCHAR(300)    NOT NULL,
    file_size_bytes  BIGINT          NOT NULL,
    mime_type        VARCHAR(100)    NOT NULL,
    rejection_reason TEXT,
    reviewed_by      UUID            REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at      TIMESTAMPTZ,
    created_at       TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT chk_rejection_reason
        CHECK (status != 'REJECTED' OR rejection_reason IS NOT NULL)
);

CREATE INDEX idx_documents_user_id       ON documents(user_id);
CREATE INDEX idx_documents_status        ON documents(status);
CREATE INDEX idx_documents_document_type ON documents(document_type);
CREATE INDEX idx_documents_reviewed_by   ON documents(reviewed_by);
```

---

#### earthquake_simulations

```sql
CREATE TABLE earthquake_simulations (
    id           UUID                   PRIMARY KEY DEFAULT gen_random_uuid(),
    district_id  UUID                   NOT NULL REFERENCES districts(id) ON DELETE RESTRICT,
    magnitude    DECIMAL(3,1)           NOT NULL CHECK (magnitude > 0 AND magnitude <= 10),
    created_by   UUID                   NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    notes        TEXT,
    triggered_at TIMESTAMPTZ            NOT NULL DEFAULT now(),
    email_status simulation_email_status NOT NULL DEFAULT 'QUEUED',
    created_at   TIMESTAMPTZ            NOT NULL DEFAULT now()
);

CREATE INDEX idx_earthquake_simulations_district_id ON earthquake_simulations(district_id);
CREATE INDEX idx_earthquake_simulations_created_by  ON earthquake_simulations(created_by);
```

---

#### simulation_notification_logs

```sql
CREATE TABLE simulation_notification_logs (
    id              UUID                PRIMARY KEY DEFAULT gen_random_uuid(),
    simulation_id   UUID                NOT NULL REFERENCES earthquake_simulations(id) ON DELETE CASCADE,
    user_id         UUID                NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    email_address   VARCHAR(255)        NOT NULL,
    status          notification_status NOT NULL DEFAULT 'QUEUED',
    sent_at         TIMESTAMPTZ,
    retry_count     SMALLINT            NOT NULL DEFAULT 0,
    last_error      TEXT,
    created_at      TIMESTAMPTZ         NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ         NOT NULL DEFAULT now()
);

CREATE INDEX idx_sim_notif_simulation_id ON simulation_notification_logs(simulation_id);
CREATE INDEX idx_sim_notif_user_id       ON simulation_notification_logs(user_id);
CREATE INDEX idx_sim_notif_status        ON simulation_notification_logs(status);
```

---

#### audit_logs

```sql
CREATE TABLE audit_logs (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id     UUID         REFERENCES users(id) ON DELETE SET NULL,
    actor_role   VARCHAR(50),
    action       VARCHAR(100) NOT NULL,
    entity_type  VARCHAR(100) NOT NULL,
    entity_id    UUID,
    old_value    JSONB,
    new_value    JSONB,
    ip_address   VARCHAR(45),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_actor_id    ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_entity_id   ON audit_logs(entity_id);
CREATE INDEX idx_audit_logs_action      ON audit_logs(action);
CREATE INDEX idx_audit_logs_created_at  ON audit_logs(created_at DESC);
```

> **Immutability rule:** No UPDATE or DELETE is permitted on `audit_logs`. Enforced at the application layer and ideally via a PostgreSQL trigger or row-level security policy.

---

#### email_verification_tokens

```sql
CREATE TABLE email_verification_tokens (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(128) NOT NULL UNIQUE,
    new_email   VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_email_tokens_user_id    ON email_verification_tokens(user_id);
CREATE INDEX idx_email_tokens_token      ON email_verification_tokens(token);
CREATE INDEX idx_email_tokens_expires_at ON email_verification_tokens(expires_at);
```

---

#### refresh_tokens

```sql
CREATE TABLE refresh_tokens (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    revoked_at  TIMESTAMPTZ,
    user_agent  TEXT,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_user_id    ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
```

---

### 2.3 Triggers

#### Trigger: Enforce neighborhood belongs to user's district

```sql
CREATE OR REPLACE FUNCTION trg_validate_user_neighborhood()
RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM neighborhoods
        WHERE id = NEW.neighborhood_id
          AND district_id = NEW.district_id
    ) THEN
        RAISE EXCEPTION 'neighborhood_id % does not belong to district_id %',
            NEW.neighborhood_id, NEW.district_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_user_neighborhood_district_check
BEFORE INSERT OR UPDATE ON users
FOR EACH ROW EXECUTE FUNCTION trg_validate_user_neighborhood();
```

#### Trigger: Auto-set updated_at on all mutable tables

```sql
CREATE OR REPLACE FUNCTION trg_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply to: users, districts, neighborhoods, events, documents,
--           teams, team_memberships, earthquake_simulations,
--           simulation_notification_logs, refresh_tokens
```

#### Trigger: Auto-set closed_at when event status → CLOSED

```sql
CREATE OR REPLACE FUNCTION trg_event_closed_at()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'CLOSED' AND OLD.status != 'CLOSED' THEN
        NEW.closed_at = now();
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_event_closed_at_set
BEFORE UPDATE ON events
FOR EACH ROW EXECUTE FUNCTION trg_event_closed_at();
```

---

### 2.4 Foreign Key Dependency Order (Creation Order)

```
1. districts
2. neighborhoods  (→ districts)
3. users          (→ districts, neighborhoods)
4. assembly_areas (→ neighborhoods)
5. teams
6. team_memberships (→ users, teams)
7. events         (→ neighborhoods, teams, users)
8. event_volunteers (→ events, users)
9. documents      (→ users)
10. earthquake_simulations (→ districts, users)
11. simulation_notification_logs (→ earthquake_simulations, users)
12. audit_logs    (→ users)
13. email_verification_tokens (→ users)
14. refresh_tokens (→ users)
```

> **Circular dependency note:** `districts.coordinator_id → users.id` and `users.district_id → districts.id` create a circular FK. Resolution: `districts.coordinator_id` is added as a deferred FK or via ALTER TABLE after users table creation.

```sql
-- Add coordinator FK to districts after users table is created:
ALTER TABLE districts
    ADD CONSTRAINT fk_district_coordinator
    FOREIGN KEY (coordinator_id) REFERENCES users(id) ON DELETE SET NULL
    DEFERRABLE INITIALLY DEFERRED;

-- Same pattern for neighborhoods:
ALTER TABLE neighborhoods
    ADD CONSTRAINT fk_neighborhood_coordinator
    FOREIGN KEY (coordinator_id) REFERENCES users(id) ON DELETE SET NULL
    DEFERRABLE INITIALLY DEFERRED;
```

---

## 3. RBAC Matrix

### 3.1 Role Definitions

| Role                     | Code                      | Description                                                  |
|--------------------------|---------------------------|--------------------------------------------------------------|
| Admin                    | `ADMIN`                   | Full system access. Manages all districts, users, simulations|
| District Coordinator     | `DISTRICT_COORDINATOR`    | Manages one assigned district and its neighborhoods          |
| Neighborhood Coordinator | `NEIGHBORHOOD_COORDINATOR`| Manages one assigned neighborhood and its events             |
| Volunteer                | `VOLUNTEER`               | Joins teams, participates in events, manages own profile     |

---

### 3.2 Permission Matrix

> Legend: **✅ Full** | **🔶 Scoped** (own district/neighborhood/self only) | **❌ Denied**

#### User Management

| Permission                              | ADMIN | DISTRICT_COORD | NEIGHBORHOOD_COORD | VOLUNTEER |
|-----------------------------------------|-------|----------------|--------------------|-----------|
| List all users                          | ✅    | 🔶 own district | ❌                 | ❌        |
| View any user profile                   | ✅    | 🔶 own district | 🔶 own neighborhood| ❌        |
| View own profile                        | ✅    | ✅             | ✅                 | ✅        |
| Update own profile (phone/address/etc.) | ✅    | ✅             | ✅                 | ✅        |
| Change own email (with verification)    | ✅    | ✅             | ✅                 | ✅        |
| Change own password                     | ✅    | ✅             | ✅                 | ✅        |
| Assign DISTRICT_COORDINATOR role        | ✅    | ❌             | ❌                 | ❌        |
| Assign NEIGHBORHOOD_COORDINATOR role    | ✅    | 🔶 own district | ❌                | ❌        |
| Deactivate / suspend user               | ✅    | ❌             | ❌                 | ❌        |
| Delete user (soft)                      | ✅    | ❌             | ❌                 | ❌        |

---

#### District Management

| Permission                              | ADMIN | DISTRICT_COORD | NEIGHBORHOOD_COORD | VOLUNTEER |
|-----------------------------------------|-------|----------------|--------------------|-----------|
| View all districts (map data)           | ✅    | ✅             | ✅                 | ✅        |
| View district detail + risk score       | ✅    | ✅             | ✅                 | ✅        |
| Update district info (name, polygon)    | ✅    | ❌             | ❌                 | ❌        |
| Assign coordinator to district          | ✅    | ❌             | ❌                 | ❌        |
| View district risk breakdown            | ✅    | 🔶 own district | ❌                | ❌        |

---

#### Neighborhood Management

| Permission                              | ADMIN | DISTRICT_COORD | NEIGHBORHOOD_COORD | VOLUNTEER |
|-----------------------------------------|-------|----------------|--------------------|-----------|
| View neighborhoods in a district        | ✅    | ✅             | ✅                 | ✅        |
| View neighborhood detail + risk         | ✅    | ✅             | ✅                 | ✅        |
| Create neighborhood                     | ✅    | 🔶 own district | ❌                | ❌        |
| Update neighborhood (name, polygon)     | ✅    | 🔶 own district | ❌                | ❌        |
| Assign coordinator to neighborhood      | ✅    | 🔶 own district | ❌                | ❌        |
| View assembly areas                     | ✅    | ✅             | ✅                 | ✅        |
| Manage assembly areas                   | ✅    | 🔶 own district | 🔶 own neighborhood| ❌       |

---

#### Event Management

| Permission                              | ADMIN | DISTRICT_COORD | NEIGHBORHOOD_COORD | VOLUNTEER |
|-----------------------------------------|-------|----------------|--------------------|-----------|
| View all events                         | ✅    | 🔶 own district | 🔶 own neighborhood| ✅ (open) |
| Create event                            | ✅    | 🔶 own district | 🔶 own neighborhood| ❌        |
| Update event (title, urgency, etc.)     | ✅    | 🔶 own district | 🔶 own neighborhood| ❌        |
| Close event (set status=CLOSED)         | ✅    | 🔶 own district | 🔶 own neighborhood| ❌        |
| Delete event                            | ✅    | ❌             | ❌                 | ❌        |
| Join event as volunteer                 | ❌    | ❌             | ❌                 | ✅        |
| Withdraw from event                     | ❌    | ❌             | ❌                 | ✅        |
| View event volunteers list              | ✅    | 🔶 own district | 🔶 own neighborhood| ❌        |

---

#### Team Management

| Permission                              | ADMIN | DISTRICT_COORD | NEIGHBORHOOD_COORD | VOLUNTEER |
|-----------------------------------------|-------|----------------|--------------------|-----------|
| View all teams                          | ✅    | ✅             | ✅                 | ✅        |
| Join team (if eligible)                 | ❌    | ❌             | ❌                 | ✅        |
| Leave team                              | ❌    | ❌             | ❌                 | ✅        |
| Assign team leader                      | ✅    | ❌             | ❌                 | ❌        |
| View team members                       | ✅    | ✅             | ✅                 | ❌        |
| Remove member from team                 | ✅    | ❌             | ❌                 | ❌        |

---

#### Document Management

| Permission                              | ADMIN | DISTRICT_COORD | NEIGHBORHOOD_COORD | VOLUNTEER |
|-----------------------------------------|-------|----------------|--------------------|-----------|
| Upload own document                     | ❌    | ❌             | ❌                 | ✅        |
| View own documents                      | ✅    | ✅             | ✅                 | ✅        |
| View all PENDING documents              | ✅    | 🔶 own district | ❌                | ❌        |
| Approve document                        | ✅    | 🔶 own district | ❌                | ❌        |
| Reject document (with reason)           | ✅    | 🔶 own district | ❌                | ❌        |
| Download document file                  | ✅    | 🔶 own district | ❌                | 🔶 own    |

---

#### Earthquake Simulation

| Permission                              | ADMIN | DISTRICT_COORD | NEIGHBORHOOD_COORD | VOLUNTEER |
|-----------------------------------------|-------|----------------|--------------------|-----------|
| Trigger earthquake simulation           | ✅    | ❌             | ❌                 | ❌        |
| View simulation history                 | ✅    | 🔶 own district | ❌                | ❌        |
| View notification delivery logs         | ✅    | ❌             | ❌                 | ❌        |

---

#### Audit Log

| Permission                              | ADMIN | DISTRICT_COORD | NEIGHBORHOOD_COORD | VOLUNTEER |
|-----------------------------------------|-------|----------------|--------------------|-----------|
| View audit logs                         | ✅    | ❌             | ❌                 | ❌        |
| Export audit logs                       | ✅    | ❌             | ❌                 | ❌        |

---

### 3.3 JWT Claims Structure

JWT access token payload (relevant claims):

```json
{
  "sub": "<user-uuid>",
  "role": "DISTRICT_COORDINATOR",
  "districtId": "<district-uuid>",
  "neighborhoodId": "<neighborhood-uuid>",
  "email": "user@example.com",
  "iat": 1709000000,
  "exp": 1709003600
}
```

- **Access token TTL:** 1 hour  
- **Refresh token TTL:** 30 days  
- `districtId` is populated for DISTRICT_COORDINATOR and NEIGHBORHOOD_COORDINATOR and VOLUNTEER.  
- `neighborhoodId` is populated for NEIGHBORHOOD_COORDINATOR and VOLUNTEER.  
- Scoped permissions in the RBAC matrix are enforced by comparing JWT claims against entity ownership fields.

---

## 4. Coordinator Hierarchy Rules & Constraints

### 4.1 Hierarchy Overview

```
ADMIN
  └── DISTRICT_COORDINATOR (one per district)
        └── NEIGHBORHOOD_COORDINATOR (one per neighborhood)
              └── VOLUNTEER
```

This is a strict 4-level command chain. No lateral authority exists between coordinators of different districts or different neighborhoods within the same district.

---

### 4.2 District Coordinator Rules

#### Assignment Rules

| Rule # | Rule Description                                                                                                          |
|--------|---------------------------------------------------------------------------------------------------------------------------|
| DC-01  | Only ADMIN may assign or remove a District Coordinator.                                                                   |
| DC-02  | Each active district has **at most one** District Coordinator at any time (`districts.coordinator_id` is unique per row). |
| DC-03  | A user may be District Coordinator of **only one district** at a time.                                                    |
| DC-04  | When a user is assigned DISTRICT_COORDINATOR role, their `district_id` must match the district they are coordinating.    |
| DC-05  | A user must have `email_verified = TRUE` before being assigned DISTRICT_COORDINATOR.                                      |
| DC-06  | A user must have `is_active = TRUE` before being assigned any coordinator role.                                           |
| DC-07  | Removing a District Coordinator sets `districts.coordinator_id = NULL`; the user's role reverts to VOLUNTEER.             |
| DC-08  | If a District Coordinator is deactivated (`is_active = FALSE`), their coordinator assignment is automatically removed.    |

#### Operational Scope

| Rule # | Rule Description                                                                                                          |
|--------|---------------------------------------------------------------------------------------------------------------------------|
| DC-S01 | A District Coordinator may manage events, neighborhood coordinators, and documents **only within their assigned district**.|
| DC-S02 | A District Coordinator may assign Neighborhood Coordinators within their own district only.                               |
| DC-S03 | A District Coordinator can create and close events in any neighborhood within their district.                             |
| DC-S04 | A District Coordinator can approve/reject documents uploaded by users who belong to their district.                       |
| DC-S05 | District Coordinator cannot trigger earthquake simulations.                                                               |
| DC-S06 | District Coordinator cannot modify district-level GeoJSON polygon data.                                                  |

---

### 4.3 Neighborhood Coordinator Rules

#### Assignment Rules

| Rule # | Rule Description                                                                                                              |
|--------|-------------------------------------------------------------------------------------------------------------------------------|
| NC-01  | A Neighborhood Coordinator may be assigned by ADMIN or by the District Coordinator of the same district.                     |
| NC-02  | Each neighborhood has **at most one** Neighborhood Coordinator (`neighborhoods.coordinator_id` unique per row).               |
| NC-03  | A user may be Neighborhood Coordinator of **only one neighborhood** at a time.                                                |
| NC-04  | The assigned user's `neighborhood_id` must match the neighborhood they coordinate.                                            |
| NC-05  | The assigned user's `district_id` must match the district the neighborhood belongs to.                                        |
| NC-06  | A user must have `email_verified = TRUE` before being assigned NEIGHBORHOOD_COORDINATOR.                                      |
| NC-07  | Removing a Neighborhood Coordinator sets `neighborhoods.coordinator_id = NULL`; user's role reverts to VOLUNTEER.             |
| NC-08  | If the District Coordinator of the containing district is removed, Neighborhood Coordinators are **not** automatically removed.|

#### Operational Scope

| Rule # | Rule Description                                                                                                           |
|--------|----------------------------------------------------------------------------------------------------------------------------|
| NC-S01 | A Neighborhood Coordinator may manage events **only within their assigned neighborhood**.                                  |
| NC-S02 | A Neighborhood Coordinator may manage assembly areas for their neighborhood.                                               |
| NC-S03 | A Neighborhood Coordinator cannot approve or reject documents.                                                             |
| NC-S04 | A Neighborhood Coordinator cannot assign coordinators.                                                                     |
| NC-S05 | A Neighborhood Coordinator can view user profiles within their neighborhood only.                                          |

---

### 4.4 Volunteer Rules

| Rule # | Rule Description                                                                                                           |
|--------|----------------------------------------------------------------------------------------------------------------------------|
| V-01   | A Volunteer may join a team only if they hold an APPROVED document for that team's `requires_document` (where applicable). |
| V-02   | A Volunteer may join multiple teams simultaneously.                                                                        |
| V-03   | A Volunteer may participate in an event only if they are an active member of the event's assigned team.                    |
| V-04   | A Volunteer may not create, update, or close events.                                                                       |
| V-05   | A Volunteer may manage only their own profile, documents, and team memberships.                                            |

---

### 4.5 Role Transition Rules

| Transition                            | Permitted By      | Constraints                                                              |
|---------------------------------------|-------------------|--------------------------------------------------------------------------|
| VOLUNTEER → NEIGHBORHOOD_COORDINATOR  | ADMIN, DC (own district) | Must satisfy NC-04, NC-05, NC-06                               |
| VOLUNTEER → DISTRICT_COORDINATOR      | ADMIN only        | Must satisfy DC-04, DC-05                                                |
| NEIGHBORHOOD_COORDINATOR → VOLUNTEER  | ADMIN, DC (own district) | Clears `neighborhoods.coordinator_id`                          |
| DISTRICT_COORDINATOR → VOLUNTEER      | ADMIN only        | Clears `districts.coordinator_id`                                        |
| Any role → ADMIN                      | ❌ Not supported  | Admin accounts are seeded; no promotion path through the API             |
| ADMIN → Any lower role                | ❌ Not supported  | Admin role is protected; demotion not permitted                          |

---

### 4.6 Consistency Invariants

These invariants must hold at all times and are enforced at the application service layer (with database triggers as a secondary safeguard where possible):

| Invariant # | Description                                                                                      |
|-------------|--------------------------------------------------------------------------------------------------|
| INV-01      | `users.district_id` ∈ { active district IDs } at all times.                                     |
| INV-02      | `users.neighborhood_id.district_id = users.district_id` (enforced by trigger).                  |
| INV-03      | If `districts.coordinator_id = X`, then `users[X].role = DISTRICT_COORDINATOR`.                 |
| INV-04      | If `neighborhoods.coordinator_id = X`, then `users[X].role = NEIGHBORHOOD_COORDINATOR`.          |
| INV-05      | A user with role DISTRICT_COORDINATOR appears in exactly one `districts.coordinator_id` row.     |
| INV-06      | A user with role NEIGHBORHOOD_COORDINATOR appears in exactly one `neighborhoods.coordinator_id`. |
| INV-07      | Only ADMIN and DISTRICT_COORDINATOR may set `documents.status` from PENDING.                     |
| INV-08      | A REJECTED document must have a non-null, non-empty `rejection_reason`.                          |
| INV-09      | `team_memberships` for a restricted team require at least one APPROVED document of the correct type. |
| INV-10      | An event's `risk_score` is always recomputed when `urgency`, `required_people`, or `status` changes. |

---

*End of Master Spec — Part 1*  
*Next: Part 2 — REST API Contract, Risk Scoring Algorithm, Event Lifecycle Model*
