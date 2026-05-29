# Implementation Plan
## Istanbul Disaster Coordination & Volunteer Management Platform — Backend

**Version:** 1.0
**Date:** 2026-03-05
**Stack:** Java 17 · Spring Boot 3.x · PostgreSQL · Flyway · JWT · Spring Security
**Master Spec References:** `docs/master-spec-part1.md` (Domain, Schema, RBAC) · `docs/master-spec-part2.md` (REST API, Risk Scoring, Simulation)

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Technology Stack & Dependencies](#2-technology-stack--dependencies)
3. [Module Structure](#3-module-structure)
4. [Phase 1 — Project Scaffold](#4-phase-1--project-scaffold)
5. [Phase 2 — Flyway Database Migrations](#5-phase-2--flyway-database-migrations)
6. [Phase 3 — Domain Layer](#6-phase-3--domain-layer)
7. [Phase 4 — Security Layer](#7-phase-4--security-layer)
8. [Phase 5 — DTOs](#8-phase-5--dtos)
9. [Phase 6 — Services](#9-phase-6--services)
10. [Phase 7 — Controllers](#10-phase-7--controllers)
11. [Phase 8 — Exception Handling & Configuration](#11-phase-8--exception-handling--configuration)
12. [Phase 9 — Documentation](#12-phase-9--documentation)
13. [Phase 10 — Verification Checklist](#13-phase-10--verification-checklist)

---

## 1. Project Overview

The platform is a Spring Boot REST API that coordinates disaster response volunteers across 10 Istanbul pilot districts. It implements a strict 4-level role hierarchy (ADMIN → DISTRICT_COORDINATOR → NEIGHBORHOOD_COORDINATOR → VOLUNTEER), risk-scored event management, team-based volunteer assignment with document gating, earthquake simulation email broadcasts, and a full audit trail.

**Key constraints:**
- Every entity uses `UUID` primary keys.
- All timestamps are `TIMESTAMPTZ` (timezone-aware).
- Passwords stored as BCrypt hashes (cost ≥ 12).
- JWT access tokens (1-hour TTL) + refresh tokens (30-day TTL stored as SHA-256 hashes).
- Risk score formula: `EventRisk = TeamCoefficient × Urgency × RequiredPeople × StatusCoefficient` (OPEN=1.0, CLOSED=0.2).

---

## 2. Technology Stack & Dependencies

| Dependency | Purpose |
|---|---|
| Spring Boot 3.x | Application framework |
| Spring Web (MVC) | REST controllers |
| Spring Data JPA + Hibernate | ORM + repository layer |
| Spring Security | Authentication / authorization |
| postgresql (JDBC driver) | Database connectivity |
| Flyway | Database migration versioning |
| jjwt (JJWT library) | JWT generation and validation |
| BCryptPasswordEncoder | Password hashing (Spring Security) |
| Jakarta Validation (Hibernate Validator) | Bean validation |
| springdoc-openapi (Swagger UI) | API documentation |
| Jackson | JSON serialization |
| Java Mail / Spring Mail | Email dispatch |
| Lombok | Boilerplate reduction |

---

## 3. Module Structure

```
backend/src/main/java/com/afet/koordinasyon/
├── AfetKoordinasyonApplication.java
├── domain/
│   ├── entity/           ← 14 JPA entities
│   └── enums/            ← 9 enum types
├── repository/           ← 14 Spring Data JPA repositories
├── security/
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   ├── JwtAuthenticationEntryPoint.java
│   ├── CustomUserDetailsService.java
│   ├── UserPrincipal.java
│   └── SecurityConfig.java
├── dto/
│   ├── request/          ← 13 inbound request DTOs
│   └── response/         ← 15+ outbound response DTOs
├── service/              ← 13 service classes
├── controller/           ← 10 REST controllers
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── BusinessRuleException.java
│   ├── ConflictException.java
│   ├── ResourceNotFoundException.java
│   └── ErrorResponse.java
└── config/
    └── SwaggerConfig.java

backend/src/main/resources/
├── application.yml
└── db/migration/
    ├── V1__create_enums.sql
    ├── V2__create_tables.sql
    ├── V3__create_indexes.sql
    ├── V4__create_triggers.sql
    └── V5__seed_data.sql
```

---

## 4. Phase 1 — Project Scaffold

**Status: ✅ Complete**

### Files

#### `pom.xml`
Maven build file with Java 17 target, Spring Boot 3.x parent, and all required dependencies (see §2). Key managed versions:
- `jjwt-api`, `jjwt-impl`, `jjwt-jackson`
- `springdoc-openapi-starter-webmvc-ui`
- `postgresql`
- `flyway-core`

#### `application.yml`
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/afet_koordinasyon
    username: ${DB_USERNAME:afet_user}
    password: ${DB_PASSWORD:afet_password}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  flyway:
    enabled: true
    locations: classpath:db/migration

app:
  jwt:
    secret: ${JWT_SECRET}       # ≥ 256-bit random string
    access-token-expiry: 3600   # seconds (1 hour)
    refresh-token-expiry: 2592000 # seconds (30 days)

server:
  port: 8080
```

#### `AfetKoordinasyonApplication.java`
Standard `@SpringBootApplication` entry point.

---

## 5. Phase 2 — Flyway Database Migrations

**Status: ✅ Complete**

All 5 migration scripts exist under `src/main/resources/db/migration/`.

### V1__create_enums.sql
Defines all PostgreSQL `ENUM` types before table creation:
- `user_role` → ADMIN, DISTRICT_COORDINATOR, NEIGHBORHOOD_COORDINATOR, VOLUNTEER
- `blood_type` → A_POSITIVE … O_NEGATIVE (8 values)
- `team_name` → SEARCH_RESCUE, FOOD_WATER, LOGISTICS, EVACUATION, COMMUNICATION, PSYCHOSOCIAL
- `document_type` → SEARCH_RESCUE_CERTIFICATE, PSYCHOSOCIAL_GRADUATION_DOCUMENT
- `document_status` → PENDING, APPROVED, REJECTED
- `event_status` → OPEN, CLOSED
- `event_volunteer_status` → ASSIGNED, COMPLETED, WITHDRAWN
- `simulation_email_status` → QUEUED, PROCESSING, COMPLETED, PARTIAL_FAILURE
- `notification_status` → QUEUED, SENT, FAILED, BOUNCED

### V2__create_tables.sql
Creates tables in dependency order (no circular FK at CREATE time):

1. `districts` — name, geojson_polygon (JSONB), risk_score, is_active
2. `neighborhoods` — FK → districts, coordinator_id (deferred FK)
3. `users` — FK → districts + neighborhoods; password_hash, role, is_active
4. `assembly_areas` — FK → neighborhoods
5. `teams` — 6 seeded teams; coefficient, requires_document
6. `team_memberships` — UNIQUE(user_id, team_id)
7. `events` — FK → neighborhoods + teams + users; urgency CHECK 1–5; endsAt ≥ startsAt CHECK
8. `event_volunteers` — UNIQUE(event_id, user_id)
9. `documents` — CHECK (status ≠ REJECTED OR rejection_reason IS NOT NULL)
10. `earthquake_simulations` — magnitude CHECK 0–10
11. `simulation_notification_logs` — FK → earthquake_simulations + users
12. `audit_logs` — INSERT-only; actor_id nullable (for system actions)
13. `email_verification_tokens` — token UNIQUE, 24-hour TTL
14. `refresh_tokens` — token_hash UNIQUE (SHA-256)

Circular FK resolution (post-table ALTER):
```sql
ALTER TABLE districts ADD CONSTRAINT fk_district_coordinator
  FOREIGN KEY (coordinator_id) REFERENCES users(id) ON DELETE SET NULL
  DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE neighborhoods ADD CONSTRAINT fk_neighborhood_coordinator
  FOREIGN KEY (coordinator_id) REFERENCES users(id) ON DELETE SET NULL
  DEFERRABLE INITIALLY DEFERRED;
```

### V3__create_indexes.sql
Indexes on all foreign keys and high-cardinality filter columns:
- `users`: email, phone, district_id, neighborhood_id, role, is_active
- `events`: neighborhood_id, team_id, status, created_by, urgency
- `documents`: user_id, status, document_type, reviewed_by
- `team_memberships`: user_id, team_id
- `simulation_notification_logs`: simulation_id, user_id, status
- `audit_logs`: actor_id, entity_id, action, created_at DESC
- `email_verification_tokens`: user_id, token, expires_at
- `refresh_tokens`: user_id, token_hash, expires_at

### V4__create_triggers.sql
Three triggers:

1. **`trg_validate_user_neighborhood`** — BEFORE INSERT OR UPDATE on `users`; ensures `neighborhood_id.district_id = users.district_id`.
2. **`trg_set_updated_at`** — BEFORE UPDATE on all mutable tables; sets `updated_at = NOW()`.
3. **`trg_event_closed_at`** — BEFORE UPDATE on `events`; when `status → CLOSED`, sets `closed_at = NOW()`.

### V5__seed_data.sql
Seeds immutable reference data:

**10 Districts:**
Pendik, Kartal, Tuzla, Kadıköy, Ataşehir, Bahçelievler, Beşiktaş, Bakırköy, Fatih, Avcılar

**6 Teams:**

| name | coefficient | requires_document |
|---|---|---|
| SEARCH_RESCUE | 5.00 | SEARCH_RESCUE_CERTIFICATE |
| FOOD_WATER | 3.00 | NULL |
| LOGISTICS | 2.00 | NULL |
| EVACUATION | 4.00 | NULL |
| COMMUNICATION | 2.00 | NULL |
| PSYCHOSOCIAL | 3.00 | PSYCHOSOCIAL_GRADUATION_DOCUMENT |

**Admin User:** A single ADMIN account with a pre-hashed password is seeded (email_verified = true, is_active = true).

---

## 6. Phase 3 — Domain Layer

**Status: ✅ Complete**

### 6.1 Enums (`domain/enums/`)

| File | Values |
|---|---|
| `UserRole.java` | ADMIN, DISTRICT_COORDINATOR, NEIGHBORHOOD_COORDINATOR, VOLUNTEER |
| `BloodType.java` | A_POSITIVE, A_NEGATIVE, B_POSITIVE, B_NEGATIVE, AB_POSITIVE, AB_NEGATIVE, O_POSITIVE, O_NEGATIVE |
| `TeamName.java` | SEARCH_RESCUE, FOOD_WATER, LOGISTICS, EVACUATION, COMMUNICATION, PSYCHOSOCIAL |
| `DocumentType.java` | SEARCH_RESCUE_CERTIFICATE, PSYCHOSOCIAL_GRADUATION_DOCUMENT |
| `DocumentStatus.java` | PENDING, APPROVED, REJECTED |
| `EventStatus.java` | OPEN, CLOSED |
| `EventVolunteerStatus.java` | ASSIGNED, COMPLETED, WITHDRAWN |
| `SimulationEmailStatus.java` | QUEUED, PROCESSING, COMPLETED, PARTIAL_FAILURE |
| `NotificationStatus.java` | QUEUED, SENT, FAILED, BOUNCED |

All enums are mapped to PostgreSQL native ENUMs via `@Enumerated(EnumType.STRING)`.

### 6.2 Entities (`domain/entity/`)

All entities use:
- `@Id @GeneratedValue(strategy = GenerationType.UUID)` for UUID PKs
- `@CreationTimestamp` / `@UpdateTimestamp` for audit timestamps
- Lombok `@Getter` / `@Setter` / `@Builder`

| Entity | Key relationships / notes |
|---|---|
| `User` | `@ManyToOne` District, Neighborhood; `role` enum |
| `District` | `@OneToOne(optional=true)` coordinator User; `geojsonPolygon` stored as String |
| `Neighborhood` | `@ManyToOne` District; `@OneToOne(optional=true)` coordinator User |
| `AssemblyArea` | `@ManyToOne` Neighborhood |
| `Team` | `requiresDocument` nullable enum |
| `TeamMembership` | `@ManyToOne` User, Team; `@Table(uniqueConstraints=...)` |
| `Event` | `@ManyToOne` Neighborhood, Team, User (createdBy); urgency CHECK in DB |
| `EventVolunteer` | `@ManyToOne` Event, User; unique constraint (event_id, user_id) |
| `Document` | `@ManyToOne` User (owner), User (reviewedBy); CHECK on rejectionReason |
| `EarthquakeSimulation` | `@ManyToOne` District, User (createdBy) |
| `SimulationNotificationLog` | `@ManyToOne` EarthquakeSimulation, User |
| `AuditLog` | `actor_id` nullable; no `@UpdateTimestamp` (immutable) |
| `EmailVerificationToken` | SHA-128 random token; `expiresAt`; `usedAt` nullable |
| `RefreshToken` | `tokenHash` (SHA-256); `revoked` boolean; `revokedAt` nullable |

### 6.3 Repositories (`repository/`)

All extend `JpaRepository<Entity, UUID>`. 14 repositories with custom query methods:

| Repository | Notable queries |
|---|---|
| `UserRepository` | `findByEmail`, `findByPhone`, `findByRoleAndDistrictId`, `findActiveVerifiedUsers` |
| `DistrictRepository` | `findByIsActiveTrue`, `findByCoordinatorId` |
| `NeighborhoodRepository` | `findByDistrictId`, `findByCoordinatorId` |
| `EventRepository` | `findByNeighborhood`, `findByStatus`, paged queries with filters |
| `DocumentRepository` | `findByUserIdAndDocumentTypeAndStatus` (latest approved), `findPendingByDistrictId` |
| `TeamMembershipRepository` | `findByUserIdAndTeamId`, `findActiveByUserId` |
| `EventVolunteerRepository` | `findByEventIdAndUserId`, `findByUserIdAndStatus` |
| `RefreshTokenRepository` | `findByTokenHash`, `revokeAllByUserId` |
| `EmailVerificationTokenRepository` | `findByTokenAndUsedAtIsNull` |
| `SimulationNotificationLogRepository` | `findBySimulationIdAndStatus`, count queries |
| `AuditLogRepository` | `findByActorId`, `findByEntityTypeAndEntityId` |

---

## 7. Phase 4 — Security Layer

**Status: ✅ Complete**

All 6 security classes are implemented under `security/`.

### `JwtTokenProvider.java`
- Generates access tokens (1-hour TTL) using HMAC-SHA256.
- JWT claims: `sub` (userId), `role`, `districtId`, `neighborhoodId`, `email`.
- `districtId` included for DC, NC, and VOLUNTEER roles.
- `neighborhoodId` included for NC and VOLUNTEER roles.
- Validates tokens: signature, expiry, and subject presence.
- `generateRefreshToken()` produces a cryptographically random 256-bit hex string; stored as SHA-256 hash.

### `JwtAuthenticationFilter.java`
- `OncePerRequestFilter` that extracts `Authorization: Bearer <token>`.
- Validates token via `JwtTokenProvider`, loads `UserDetails` from `CustomUserDetailsService`.
- Sets `UsernamePasswordAuthenticationToken` in `SecurityContextHolder`.

### `JwtAuthenticationEntryPoint.java`
- Implements `AuthenticationEntryPoint`; returns `401 UNAUTHORIZED` JSON on unauthenticated access.

### `CustomUserDetailsService.java`
- Implements `UserDetailsService`; loads user by email from `UserRepository`.
- Builds `UserPrincipal` with role as a `GrantedAuthority`.
- Throws `UsernameNotFoundException` if user not found or `is_active = false`.

### `UserPrincipal.java`
- Implements `UserDetails`; wraps the `User` entity.
- Exposes `getUserId()`, `getDistrictId()`, `getNeighborhoodId()` for scope checks in services.

### `SecurityConfig.java`
- Disables CSRF (stateless JWT API).
- Configures `sessionManagement` to `STATELESS`.
- Public endpoints (no auth): `POST /api/v1/auth/register`, `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/verify-email`.
- All other endpoints require authentication.
- Adds `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`.
- Registers `JwtAuthenticationEntryPoint` as the entry point.

---

## 8. Phase 5 — DTOs

**Status: 🔶 Partial — Request DTOs complete; Response DTOs pending**

### 8.1 Request DTOs (`dto/request/`) — ✅ Complete

| DTO | Fields |
|---|---|
| `RegisterRequest` | firstName, lastName, email, phone, bloodType, districtId, neighborhoodId, address, profession, password |
| `LoginRequest` | email, password |
| `RefreshTokenRequest` | refreshToken |
| `VerifyEmailRequest` | token |
| `UpdateProfileRequest` | firstName, lastName, phone, bloodType, districtId, neighborhoodId, address, profession |
| `ChangeEmailRequest` | newEmail, password |
| `ChangePasswordRequest` | currentPassword, newPassword |
| `AssignRoleRequest` | role, districtId (nullable, required for DC/NC assignment) |
| `AssignCoordinatorRequest` | userId |
| `CreateEventRequest` | title, description, neighborhoodId, teamId, urgency, requiredPeople, latitude, longitude, startsAt, endsAt |
| `UpdateEventRequest` | title, description, urgency, requiredPeople, latitude, longitude, startsAt, endsAt |
| `RejectDocumentRequest` | reason |
| `CreateSimulationRequest` | districtId, magnitude, notes |

All request DTOs use Jakarta Validation annotations (`@NotBlank`, `@NotNull`, `@Min`, `@Max`, `@Email`, `@Size`).

### 8.2 Response DTOs (`dto/response/`) — ❌ Pending

Create the following response DTOs as records or Lombok-annotated classes:

#### Auth

**`AuthResponse`**
```java
String accessToken, refreshToken, tokenType;
long expiresIn; // seconds
UserSummaryResponse user;
```

**`TokenRefreshResponse`**
```java
String accessToken, refreshToken, tokenType;
long expiresIn;
```

**`MessageResponse`**
```java
String message;
```

#### User

**`UserResponse`** (full profile — GET /users/me)
```java
UUID id; String firstName, lastName, email;
boolean emailVerified; String pendingEmail, phone;
String bloodType; // enum name
DistrictSummaryResponse district;
NeighborhoodSummaryResponse neighborhood;
String address, profession, role;
boolean isActive;
OffsetDateTime createdAt, updatedAt;
```

**`UserSummaryResponse`** (embedded in other responses)
```java
UUID id; String firstName, lastName, email; String role;
```

#### District / Neighborhood / Assembly

**`DistrictResponse`**
```java
UUID id; String name;
double riskScore; String riskColor; // GREEN / YELLOW / RED
UserSummaryResponse coordinator;
Object polygon; // raw GeoJSON
OffsetDateTime riskScoreUpdatedAt;
```

**`DistrictSummaryResponse`**
```java
UUID id; String name;
```

**`NeighborhoodResponse`**
```java
UUID id; String name; UUID districtId;
double riskScore; String riskColor;
UserSummaryResponse coordinator;
Object polygon;
List<AssemblyAreaResponse> assemblyAreas;
```

**`NeighborhoodSummaryResponse`**
```java
UUID id; String name; String districtName;
```

**`AssemblyAreaResponse`**
```java
UUID id; String name;
double latitude, longitude; int capacity;
String googleMapsUrl; // computed: "https://www.google.com/maps?q={lat},{lon}"
```

#### Team

**`TeamResponse`**
```java
UUID id; String name; double coefficient;
String requiresDocument; // nullable
UserSummaryResponse leader;
long activeMemberCount;
```

**`TeamMemberResponse`** (paginated list in GET /teams/{teamId}/members)
```java
UUID userId; String firstName, lastName;
OffsetDateTime joinedAt;
String district, neighborhood;
```

**`TeamJoinResponse`**
```java
UUID teamMembershipId, teamId; String teamName; OffsetDateTime joinedAt;
```

#### Event

**`EventResponse`** (full — GET /events/{eventId})
```java
UUID id; String title, description, status;
int urgency, requiredPeople;
double riskScore;
TeamSummaryResponse team;
NeighborhoodSummaryResponse neighborhood;
UserSummaryResponse createdBy;
Double latitude, longitude;
OffsetDateTime startsAt, endsAt, closedAt;
long assignedVolunteers;
OffsetDateTime createdAt, updatedAt;
```

**`EventSummaryResponse`** (paginated list)
```java
UUID id; String title, status;
int urgency, requiredPeople; double riskScore;
TeamSummaryResponse team;
NeighborhoodSummaryResponse neighborhood;
Double latitude, longitude;
OffsetDateTime startsAt, createdAt;
```

**`TeamSummaryResponse`**
```java
UUID id; String name; double coefficient;
```

**`EventCloseResponse`**
```java
UUID id; String status; OffsetDateTime closedAt; double riskScore;
```

**`EventVolunteerResponse`** (per-user volunteer record in event volunteers list)
```java
UUID userId; String firstName, lastName; String status; OffsetDateTime joinedAt;
```

**`EventJoinResponse`**
```java
UUID eventVolunteerId, eventId; String status; OffsetDateTime joinedAt;
```

**`UserEventResponse`** (in GET /users/me/events)
```java
UUID id; String title, status; int urgency;
TeamSummaryResponse team; NeighborhoodSummaryResponse neighborhood;
OffsetDateTime joinedAt; String volunteerStatus;
```

#### Document

**`DocumentResponse`**
```java
UUID id; String documentType, status, fileName;
long fileSizeBytes; String mimeType;
String rejectionReason;
UserSummaryResponse reviewedBy;
OffsetDateTime reviewedAt, createdAt;
```

**`DocumentDownloadResponse`**
```java
String presignedUrl; OffsetDateTime expiresAt;
```

**`PendingDocumentResponse`** (in GET /admin/documents/pending)
```java
UUID id; String documentType, status, fileName;
DocumentOwnerResponse owner;
OffsetDateTime createdAt;
```

**`DocumentOwnerResponse`**
```java
UUID id; String firstName, lastName; String district;
```

#### Simulation

**`SimulationCreatedResponse`**
```java
UUID simulationId, districtId; String districtName;
double magnitude; String emailStatus;
long totalUsersToNotify; OffsetDateTime triggeredAt;
```

**`SimulationDetailResponse`**
```java
UUID id; DistrictSummaryResponse district;
double magnitude; String notes, emailStatus;
long totalQueued, totalSent, totalFailed;
OffsetDateTime triggeredAt; UserSummaryResponse createdBy;
```

**`SimulationLogResponse`** (paginated notification log entry)
```java
UUID id, userId; String emailAddress, status;
int retryCount; String lastError; OffsetDateTime sentAt;
```

#### Map

**`MapDistrictResponse`** (GET /map/districts — includes GeoJSON Feature)
```java
UUID id; String name; double riskScore; String riskColor;
Object polygon; // GeoJSON Feature shape
```

**`MapNeighborhoodResponse`** (GET /map/districts/{id}/neighborhoods)
```java
UUID id; String name; double riskScore; String riskColor;
Object polygon; // GeoJSON Feature shape
```

#### Risk Color Helper
A `RiskColorUtil` utility (static) computes color from score:
```
score >= 70  → "RED"    (#E53935)
score >= 40  → "YELLOW" (#FDD835)
score < 40   → "GREEN"  (#43A047)
```

---

## 9. Phase 6 — Services

**Status: ❌ Pending**

All services live under `service/`. Each service is a `@Service` bean with constructor injection of required repositories and other services.

---

### `AuthService`

Handles all auth flows. Injects: `UserRepository`, `DistrictRepository`, `NeighborhoodRepository`, `RefreshTokenRepository`, `EmailVerificationTokenRepository`, `PasswordEncoder`, `JwtTokenProvider`, `EmailService`.

#### `register(RegisterRequest)`
1. Validate email and phone uniqueness (→ 409 CONFLICT if taken).
2. Validate `districtId` is active (→ 422 if not in active 10).
3. Validate `neighborhoodId.district_id = districtId` (→ 422 on mismatch).
4. Hash password with BCrypt.
5. Create and save `User` with `role=VOLUNTEER`, `emailVerified=false`.
6. Return `UserResponse`.

#### `login(LoginRequest)`
1. Load user by email (→ 401 if not found).
2. Verify password (→ 401 if mismatch).
3. Check `is_active = true` (→ 403 if suspended).
4. Generate JWT access token with full claims (userId, role, districtId, neighborhoodId, email).
5. Generate refresh token raw string; hash with SHA-256; store `RefreshToken` entity.
6. Return `AuthResponse`.

#### `refreshToken(RefreshTokenRequest)`
1. Hash incoming raw token → look up `RefreshToken` by hash.
2. Validate: not revoked, not expired (→ 401 on any failure).
3. Load user; validate `is_active`.
4. Revoke old refresh token (`revoked=true`, `revokedAt=NOW()`).
5. Issue new access token + new refresh token (rotation).
6. Return `TokenRefreshResponse`.

#### `logout(String rawRefreshToken)`
1. Hash token → find `RefreshToken`.
2. Set `revoked=true`. Return void (204).

#### `initiateEmailChange(UUID userId, ChangeEmailRequest)`
1. Validate password matches user's current hash (→ 401 if wrong).
2. Check new email not already registered (→ 409 if taken).
3. Set `user.pendingEmail = newEmail`.
4. Generate and save `EmailVerificationToken` (TTL = 24 hours).
5. Call `emailService.sendVerificationEmail(newEmail, token)`.
6. Return `MessageResponse`.

#### `verifyEmail(VerifyEmailRequest)`
1. Find `EmailVerificationToken` by token, `usedAt IS NULL` (→ 404 if not found).
2. Validate `expiresAt > NOW()` (→ 422 if expired).
3. Set `user.email = token.newEmail`, `user.emailVerified = true`, `user.pendingEmail = null`.
4. Mark `token.usedAt = NOW()`.
5. Return `MessageResponse`.

#### `changePassword(UUID userId, ChangePasswordRequest)`
1. Verify `currentPassword` matches (→ 401 if wrong).
2. Validate `newPassword` strength (min 8 chars, mixed case + digit).
3. Hash new password, update `user.passwordHash`.
4. Revoke all existing refresh tokens for this user (force re-login).
5. Return void (204).

---

### `UserService`

Injects: `UserRepository`, `DistrictRepository`, `NeighborhoodRepository`.

#### `getMyProfile(UUID userId)` → `UserResponse`
Load user → map to response.

#### `updateProfile(UUID userId, UpdateProfileRequest)` → `UserResponse`
1. Validate phone uniqueness if changed (→ 409).
2. Validate neighborhood belongs to district (→ 422).
3. Update allowed fields (firstName, lastName, phone, bloodType, districtId, neighborhoodId, address, profession).
4. **Note:** changing district/neighborhood does not affect coordinator assignments.

#### `listUsers(UserRole role, UUID districtId, Boolean isActive, Pageable)`
- ADMIN: no scope restriction.
- Filters applied via repository specification or named queries.

#### `getUserById(UUID userId)` → `UserResponse`
Load and return user profile; throw 404 if not found.

---

### `AdminUserService`

Injects: `UserRepository`, `DistrictRepository`, `NeighborhoodRepository`.

#### `assignRole(UUID targetUserId, AssignRoleRequest)` → `UserResponse`
Complex role transition logic:

| Target role | Validation |
|---|---|
| DISTRICT_COORDINATOR | User `email_verified=true`; user's `district_id = districtId`; no current DC already on that district (→ 422); user `is_active=true` |
| NEIGHBORHOOD_COORDINATOR | User `email_verified=true`; user's `neighborhood_id` must match a neighborhood in `districtId`; one NC per neighborhood |
| VOLUNTEER | If user was DC → clear `districts.coordinator_id`; if NC → clear `neighborhoods.coordinator_id` |
| ADMIN | Always rejected (→ 422 BUSINESS_RULE_VIOLATION) |

Steps:
1. Load target user (→ 404 if not found).
2. Run transition validations.
3. If assigning DC: clear any previous DC user's role back to VOLUNTEER; set `districts.coordinator_id = userId`.
4. If assigning NC: clear any previous NC; set `neighborhoods.coordinator_id = userId`.
5. Update `user.role`.
6. Return updated `UserResponse`.

#### `deactivateUser(UUID targetUserId)`
1. Load user (→ 404).
2. Reject if user is ADMIN (→ 422).
3. Set `is_active = false`.
4. If user is DC → clear `districts.coordinator_id`, revert role to VOLUNTEER.
5. Revoke all refresh tokens (force logout).

---

### `DistrictService`

Injects: `DistrictRepository`, `NeighborhoodRepository`.

#### `getAllDistricts()` → `List<DistrictResponse>`
Return all active districts with `riskColor` computed from `risk_score`.

#### `getDistrictById(UUID districtId)` → `DistrictResponse`
Throw 404 if not found or not active.

#### `getNeighborhoodsByDistrict(UUID districtId)` → `List<NeighborhoodResponse>`
Includes assemblyAreas list for each neighborhood.

#### `assignCoordinator(UUID districtId, UUID userId)` → `DistrictResponse`
Delegates to `AdminUserService.assignRole(userId, {role=DISTRICT_COORDINATOR, districtId})`.

---

### `NeighborhoodService`

Injects: `NeighborhoodRepository`, `AssemblyAreaRepository`, `DistrictRepository`.

#### `getNeighborhoodById(UUID neighborhoodId)` → `NeighborhoodResponse`
Includes assemblyAreas. Throw 404 if not found.

#### `getEventsByNeighborhood(UUID neighborhoodId, EventStatus status, UUID teamId, Pageable)` → `Page<EventSummaryResponse>`
Delegates filtering to `EventRepository`.

#### `assignCoordinator(UUID districtId, UUID neighborhoodId, UUID userId, UserPrincipal actor)` → `NeighborhoodResponse`
1. Scope check: if actor is DISTRICT_COORDINATOR, verify `actor.districtId == districtId` (→ 403 if mismatch).
2. Validate neighborhood belongs to district (→ 422).
3. Delegate role assignment to `AdminUserService`.

---

### `TeamService`

Injects: `TeamRepository`, `TeamMembershipRepository`, `DocumentRepository`, `EventVolunteerRepository`.

#### `getAllTeams()` → `List<TeamResponse>`
All 6 teams with active member counts.

#### `getTeamMembers(UUID teamId, Pageable)` → `Page<TeamMemberResponse>`
Returns paginated active team members.

#### `joinTeam(UUID teamId, UUID userId)` → `TeamJoinResponse`
1. Load team (→ 404 if not found).
2. Check `team.requiresDocument != null`:
   - If required, query `DocumentRepository` for at least one `APPROVED` document of that type owned by `userId` (→ 422 if none found).
3. Check no active membership already exists (→ 409 if duplicate).
4. Create `TeamMembership` with `is_active = true`.
5. Return `TeamJoinResponse`.

#### `leaveTeam(UUID teamId, UUID userId)` (204)
1. Find active membership (→ 404 if not found).
2. Check no open event with this team where user has `ASSIGNED` status (→ 422 if found — must withdraw from event first).
3. Set `membership.is_active = false`.

---

### `EventService`

Injects: `EventRepository`, `NeighborhoodRepository`, `TeamRepository`, `EventVolunteerRepository`, `TeamMembershipRepository`, `RiskCalculationService`, `AuditLogService`.

#### `listEvents(filters, pageable, UserPrincipal actor)` → `Page<EventSummaryResponse>`
Access control applied:
- VOLUNTEER: `status = OPEN` only, any neighborhood.
- NEIGHBORHOOD_COORDINATOR: own neighborhood only.
- DISTRICT_COORDINATOR: own district's neighborhoods only.
- ADMIN: no restriction.

#### `createEvent(CreateEventRequest, UserPrincipal actor)` → `EventResponse`
1. Scope check: NC → event must be in own neighborhood; DC → in own district.
2. Validate `endsAt >= startsAt` (→ 400).
3. Validate `teamId` exists (→ 404).
4. Compute initial `riskScore` = `team.coefficient × urgency × requiredPeople × 1.0`.
5. Save event with `status = OPEN`.
6. Trigger `riskCalculationService.recalculate(neighborhoodId)`.
7. Return `EventResponse`.

#### `getEventById(UUID eventId)` → `EventResponse`
Load event + count assigned volunteers.

#### `updateEvent(UUID eventId, UpdateEventRequest, UserPrincipal actor)` → `EventResponse`
1. Load event (→ 404).
2. Reject if `status = CLOSED` (→ 422 — immutable).
3. Scope check for DC/NC.
4. Update fields; recompute `riskScore`.
5. Trigger recalculation if urgency or requiredPeople changed.
6. Return updated `EventResponse`.

#### `closeEvent(UUID eventId, UserPrincipal actor)` → `EventCloseResponse`
1. Load event; reject if already CLOSED (→ 409).
2. Scope check.
3. Set `status = CLOSED` (trigger sets `closed_at = NOW()`).
4. Bulk update all `ASSIGNED` volunteers in this event → `COMPLETED`.
5. Trigger `riskCalculationService.recalculate(event.neighborhoodId)`.
6. Return `EventCloseResponse`.

#### `joinEvent(UUID eventId, UUID userId)` → `EventJoinResponse`
1. Load event; reject if `CLOSED` (→ 422).
2. Check `user is active member of event.team` via `TeamMembershipRepository` (→ 422 if not).
3. Check not already joined (→ 409).
4. Create `EventVolunteer` with `status = ASSIGNED`.
5. Return `EventJoinResponse`.

#### `leaveEvent(UUID eventId, UUID userId)` (204)
1. Find record (→ 404 if not ASSIGNED).
2. Reject if event is CLOSED (→ 422).
3. Set `status = WITHDRAWN`, `left_at = NOW()`.

#### `getEventVolunteers(UUID eventId, Pageable, UserPrincipal actor)` → `Page<EventVolunteerResponse>`
Scope check for DC/NC. Returns all volunteer records.

---

### `DocumentService`

Injects: `DocumentRepository`, `UserRepository`, `StorageService`, `AuditLogService`.

#### `getMyDocuments(UUID userId)` → `List<DocumentResponse>`
All documents owned by the user.

#### `uploadDocument(UUID userId, DocumentType type, MultipartFile file)` → `DocumentResponse`
1. Validate MIME type: `application/pdf`, `image/jpeg`, `image/png` only (→ 400 if invalid).
2. Validate file size ≤ 10 MB (→ 400 if exceeded).
3. Generate storage key: `documents/{userId}/{uuid}/{originalFileName}`.
4. Call `storageService.upload(storageKey, file)`.
5. Create and save `Document` with `status = PENDING`.
6. Return `DocumentResponse`.

#### `getDownloadUrl(UUID documentId, UserPrincipal actor)` → `DocumentDownloadResponse`
1. Load document (→ 404).
2. Access check: ADMIN or DC (own district) or VOLUNTEER owning the document (→ 403 otherwise).
3. Call `storageService.generatePresignedUrl(storageKey, 15 minutes)`.
4. Return `DocumentDownloadResponse` with URL and expiry.

#### `listPendingDocuments(UUID districtId, DocumentType type, Pageable, UserPrincipal actor)` → `Page<PendingDocumentResponse>`
- ADMIN: no district filter unless `districtId` provided.
- DC: always filtered to own district.
- Scope validated (→ 403 if NC or VOLUNTEER).

#### `approveDocument(UUID documentId, UserPrincipal actor)` → `DocumentResponse`
1. Load document (→ 404).
2. Scope check: DC must own same district as document's owner.
3. Reject if `status != PENDING` (→ 409).
4. Set `status = APPROVED`, `reviewedBy = actor.userId`, `reviewedAt = NOW()`.
5. Record audit log: `DOCUMENT_APPROVED`.
6. Return updated `DocumentResponse`.

#### `rejectDocument(UUID documentId, RejectDocumentRequest, UserPrincipal actor)` → `DocumentResponse`
1. Load document (→ 404).
2. Scope check.
3. Validate `reason` not blank (→ 400).
4. Reject if not PENDING (→ 409).
5. Set `status = REJECTED`, `rejectionReason`, `reviewedBy`, `reviewedAt`.
6. Record audit log: `DOCUMENT_REJECTED`.
7. Return updated `DocumentResponse`.

---

### `RiskCalculationService`

Injects: `EventRepository`, `NeighborhoodRepository`, `DistrictRepository`, `TeamRepository`.

#### `recalculate(UUID neighborhoodId)`
Implements the procedure from spec §2.4:

```
1. Fetch all events WHERE neighborhood_id = neighborhoodId
2. For each event:
     StatusCoefficient = (event.status == OPEN) ? 1.0 : 0.2
     eventRisk = team.coefficient × event.urgency × event.requiredPeople × StatusCoefficient
3. neighborhoodRisk = sum(all eventRisk)
4. UPDATE neighborhoods.risk_score = neighborhoodRisk, risk_score_updated_at = NOW()
5. districtId = neighborhood.district_id
6. districtRisk = SUM(neighborhood.risk_score) for all neighborhoods in district
7. UPDATE districts.risk_score = districtRisk, risk_score_updated_at = NOW()
```

This method is called synchronously after every event create, update (urgency/requiredPeople), and close.

---

### `SimulationService`

Injects: `EarthquakeSimulationRepository`, `SimulationNotificationLogRepository`, `UserRepository`, `AssemblyAreaRepository`, `DistrictRepository`, `EmailService`, `AuditLogService`.

#### `triggerSimulation(CreateSimulationRequest, UUID adminId)` → `SimulationCreatedResponse`
1. Validate district active (→ 404 if not found).
2. Validate magnitude 0 < x ≤ 10 (validated by bean validation, also checked here).
3. Count all users with `is_active=true AND email_verified=true` → `totalUsersToNotify`.
4. Create and save `EarthquakeSimulation` with `emailStatus = QUEUED`.
5. Submit job to `SimulationJobQueue` (async — non-blocking).
6. Return `SimulationCreatedResponse` immediately (202 Accepted).

#### `processSimulation(UUID simulationId)` — async worker
Called by `SimulationJobQueue`. Runs in a separate thread:

1. Update `emailStatus = PROCESSING`.
2. Fetch all active + verified users (batched to avoid OOM).
3. For each user:
   a. Fetch user's neighborhood's assembly areas.
   b. Build personalized email content (district name, magnitude, assembly areas with Google Maps links, disclaimer).
   c. Create `SimulationNotificationLog` with `status = QUEUED`.
4. For each queued log: dispatch email via `EmailService`. On success → `SENT`. On failure → `FAILED` + retry logic with exponential backoff (30s → 2m → 10m, max 3 retries).
5. After all dispatches: if `failedCount = 0` → `COMPLETED`; else → `PARTIAL_FAILURE`.

#### `listSimulations(UUID districtId, Pageable)` → `Page<SimulationDetailResponse>`
ADMIN scope. Optional district filter.

#### `getSimulationById(UUID simulationId)` → `SimulationDetailResponse`
Includes counts: totalQueued, totalSent, totalFailed.

#### `getSimulationLogs(UUID simulationId, NotificationStatus status, Pageable)` → `Page<SimulationLogResponse>`
Paginated notification log for a simulation, filterable by status.

---

### `StorageService` (interface + implementation)

**`StorageService` interface:**
```java
String upload(String key, MultipartFile file);
String generatePresignedUrl(String key, Duration ttl);
```

**`LocalStorageService`** (dev/test implementation):
- Saves files to `./uploads/` directory.
- `generatePresignedUrl` returns a local file-serving URL (e.g., `http://localhost:8080/files/{key}?token=...`).
- For production, replace with an `S3StorageService` using AWS SDK v2 or MinIO.

---

### `EmailService` (interface + implementation)

**`EmailService` interface:**
```java
void sendVerificationEmail(String toEmail, String token);
void sendSimulationNotification(String toEmail, SimulationEmailPayload payload);
```

**`DevEmailService`** (dev implementation):
- Logs email content to `SLF4J` logger at `INFO` level.
- Does not actually send emails.
- For production, replace with `SendGridEmailService` or `SesEmailService`.

---

### `SimulationJobQueue`

An in-memory async job queue using Spring's `@Async` + `TaskExecutor`:

```java
@Async("simulationExecutor")
public void submit(UUID simulationId) {
    simulationService.processSimulation(simulationId);
}
```

`SimulationExecutorConfig` configures a `ThreadPoolTaskExecutor` with:
- `corePoolSize = 2`
- `maxPoolSize = 5`
- `queueCapacity = 100`
- Thread name prefix: `simulation-worker-`

---

### `AuditLogService`

Injects: `AuditLogRepository`.

#### `log(UUID actorId, String actorRole, String action, String entityType, UUID entityId, Object oldValue, Object newValue, String ipAddress)`
Creates and saves an immutable `AuditLog` record. `oldValue` and `newValue` are serialized to JSON (Jackson `ObjectMapper`).

Called at:
- Document approval / rejection
- Role assignment changes
- User deactivation
- Earthquake simulation trigger

---

## 10. Phase 7 — Controllers

**Status: ❌ Pending**

All controllers annotated with `@RestController` + `@RequestMapping("/api/v1/...")`.

---

### `AuthController` — `/api/v1/auth`

| Method | Path | Handler | Auth |
|---|---|---|---|
| POST | `/register` | `register(@Valid @RequestBody RegisterRequest)` | None |
| POST | `/login` | `login(@Valid @RequestBody LoginRequest)` | None |
| POST | `/refresh` | `refresh(@Valid @RequestBody RefreshTokenRequest)` | None |
| POST | `/logout` | `logout(@Valid @RequestBody RefreshTokenRequest, @AuthenticationPrincipal)` | Bearer |
| POST | `/verify-email` | `verifyEmail(@Valid @RequestBody VerifyEmailRequest)` | None |

---

### `UserController` — `/api/v1/users`

| Method | Path | Handler | Auth |
|---|---|---|---|
| GET | `/me` | `getProfile(@AuthenticationPrincipal UserPrincipal)` | Any role |
| PATCH | `/me` | `updateProfile(@Valid @RequestBody, @AuthenticationPrincipal)` | Any role |
| POST | `/me/change-email` | `changeEmail(@Valid @RequestBody, @AuthenticationPrincipal)` | Any role |
| POST | `/me/change-password` | `changePassword(@Valid @RequestBody, @AuthenticationPrincipal)` | Any role |
| GET | `/me/events` | `getMyEvents(Pageable, @AuthenticationPrincipal)` | Any role |
| GET | `/me/documents` | `getMyDocuments(@AuthenticationPrincipal)` | Any role |
| POST | `/me/documents` | `uploadDocument(@RequestParam MultipartFile, @RequestParam DocumentType, @AuthenticationPrincipal)` | VOLUNTEER |

---

### `AdminController` — `/api/v1/admin`

| Method | Path | Handler | Auth |
|---|---|---|---|
| GET | `/users` | `listUsers(filters, Pageable)` | ADMIN |
| GET | `/users/{userId}` | `getUserById(@PathVariable UUID)` | ADMIN |
| PATCH | `/users/{userId}/role` | `assignRole(@PathVariable UUID, @Valid @RequestBody AssignRoleRequest)` | ADMIN |
| PATCH | `/users/{userId}/deactivate` | `deactivateUser(@PathVariable UUID)` | ADMIN |
| GET | `/documents/pending` | `getPendingDocuments(filters, Pageable, @AuthenticationPrincipal)` | ADMIN, DC |

---

### `DistrictController` — `/api/v1/districts`

| Method | Path | Handler | Auth |
|---|---|---|---|
| GET | `/` | `getAllDistricts()` | Any role |
| GET | `/{districtId}` | `getDistrict(@PathVariable UUID)` | Any role |
| GET | `/{districtId}/neighborhoods` | `getNeighborhoods(@PathVariable UUID)` | Any role |
| PATCH | `/admin/{districtId}/coordinator` | `assignCoordinator(@PathVariable UUID, @Valid @RequestBody AssignCoordinatorRequest)` | ADMIN |

---

### `NeighborhoodController` — `/api/v1/neighborhoods`

| Method | Path | Handler | Auth |
|---|---|---|---|
| GET | `/{neighborhoodId}` | `getNeighborhood(@PathVariable UUID)` | Any role |
| GET | `/{neighborhoodId}/events` | `getEvents(@PathVariable UUID, filters, Pageable)` | Any role |

**In `/api/v1/districts/{districtId}/neighborhoods`:**

| Method | Path | Handler | Auth |
|---|---|---|---|
| PATCH | `/{neighborhoodId}/coordinator` | `assignCoordinator(@PathVariable UUID districtId, @PathVariable UUID neighborhoodId, @RequestBody AssignCoordinatorRequest, @AuthenticationPrincipal)` | ADMIN, DC |

---

### `TeamController` — `/api/v1/teams`

| Method | Path | Handler | Auth |
|---|---|---|---|
| GET | `/` | `getAllTeams()` | Any role |
| GET | `/{teamId}/members` | `getMembers(@PathVariable UUID, Pageable)` | ADMIN, DC, NC |
| POST | `/{teamId}/join` | `joinTeam(@PathVariable UUID, @AuthenticationPrincipal)` | VOLUNTEER |
| DELETE | `/{teamId}/leave` | `leaveTeam(@PathVariable UUID, @AuthenticationPrincipal)` | VOLUNTEER |

---

### `EventController` — `/api/v1/events`

| Method | Path | Handler | Auth |
|---|---|---|---|
| GET | `/` | `listEvents(filters, Pageable, @AuthenticationPrincipal)` | Any role |
| POST | `/` | `createEvent(@Valid @RequestBody CreateEventRequest, @AuthenticationPrincipal)` | ADMIN, DC, NC |
| GET | `/{eventId}` | `getEvent(@PathVariable UUID)` | Any role |
| PATCH | `/{eventId}` | `updateEvent(@PathVariable UUID, @Valid @RequestBody UpdateEventRequest, @AuthenticationPrincipal)` | ADMIN, DC, NC |
| POST | `/{eventId}/close` | `closeEvent(@PathVariable UUID, @AuthenticationPrincipal)` | ADMIN, DC, NC |
| GET | `/{eventId}/volunteers` | `getVolunteers(@PathVariable UUID, Pageable, @AuthenticationPrincipal)` | ADMIN, DC, NC |
| POST | `/{eventId}/join` | `joinEvent(@PathVariable UUID, @AuthenticationPrincipal)` | VOLUNTEER |
| DELETE | `/{eventId}/leave` | `leaveEvent(@PathVariable UUID, @AuthenticationPrincipal)` | VOLUNTEER |

---

### `DocumentController` — `/api/v1/documents`

| Method | Path | Handler | Auth |
|---|---|---|---|
| GET | `/{documentId}/download` | `downloadDocument(@PathVariable UUID, @AuthenticationPrincipal)` | ADMIN, DC, VOLUNTEER (own) |
| PATCH | `/{documentId}/approve` | `approveDocument(@PathVariable UUID, @AuthenticationPrincipal)` | ADMIN, DC |
| PATCH | `/{documentId}/reject` | `rejectDocument(@PathVariable UUID, @Valid @RequestBody RejectDocumentRequest, @AuthenticationPrincipal)` | ADMIN, DC |

---

### `SimulationController` — `/api/v1/admin/simulations`

| Method | Path | Handler | Auth |
|---|---|---|---|
| POST | `/` | `triggerSimulation(@Valid @RequestBody CreateSimulationRequest, @AuthenticationPrincipal)` | ADMIN |
| GET | `/` | `listSimulations(filters, Pageable)` | ADMIN |
| GET | `/{simulationId}` | `getSimulation(@PathVariable UUID)` | ADMIN |
| GET | `/{simulationId}/logs` | `getLogs(@PathVariable UUID, filters, Pageable)` | ADMIN |

---

### `MapController` — `/api/v1/map`

| Method | Path | Handler | Auth |
|---|---|---|---|
| GET | `/districts` | `getMapDistricts()` | Any role |
| GET | `/districts/{districtId}/neighborhoods` | `getMapNeighborhoods(@PathVariable UUID)` | Any role |

Returns GeoJSON-enriched district and neighborhood data for map rendering.

---

## 11. Phase 8 — Exception Handling & Configuration

**Status: 🔶 Partial — Exception classes complete; SwaggerConfig pending**

### Exception Layer — ✅ Complete

All located under `exception/`:

**`ResourceNotFoundException`** (`extends RuntimeException`)
- Thrown on `404 NOT_FOUND`.
- Constructor: `ResourceNotFoundException(String entityType, UUID id)`.

**`BusinessRuleException`** (`extends RuntimeException`)
- Thrown on `422 BUSINESS_RULE_VIOLATION`.
- Constructor: `BusinessRuleException(String message)`.

**`ConflictException`** (`extends RuntimeException`)
- Thrown on `409 CONFLICT`.
- Constructor: `ConflictException(String message)`.

**`ErrorResponse`**
Standard error envelope (matches spec §1.0.1):
```java
int status; String error, message;
OffsetDateTime timestamp;
String path;
List<FieldError> details; // nullable
```

**`GlobalExceptionHandler`** (`@RestControllerAdvice`)
Handles:

| Exception | HTTP Status | Error Code |
|---|---|---|
| `MethodArgumentNotValidException` | 400 | `VALIDATION_ERROR` |
| `ConstraintViolationException` | 400 | `VALIDATION_ERROR` |
| `ResourceNotFoundException` | 404 | `NOT_FOUND` |
| `ConflictException` | 409 | `CONFLICT` |
| `BusinessRuleException` | 422 | `BUSINESS_RULE_VIOLATION` |
| `AccessDeniedException` | 403 | `FORBIDDEN` |
| `AuthenticationException` | 401 | `UNAUTHORIZED` |
| `Exception` (catch-all) | 500 | `INTERNAL_ERROR` |

### `SwaggerConfig.java` — ❌ Pending

```java
@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Afet Koordinasyon API")
                .version("1.0")
                .description("Istanbul Disaster Coordination & Volunteer Management Platform"))
            .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
            .components(new Components()
                .addSecuritySchemes("Bearer Authentication",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
```

Swagger UI available at: `http://localhost:8080/swagger-ui/index.html`

---

## 12. Phase 9 — Documentation

**Status: ❌ Pending**

### `docs/RUN_BACKEND.md`

Must contain:

1. **Prerequisites**: Java 17+, Maven 3.8+, Docker with Docker Compose.
2. **Start PostgreSQL**: `docker-compose up -d` — creates `afet_koordinasyon` database.
3. **Configure environment**: Set `JWT_SECRET` environment variable (min 32 chars).
4. **Run the application**: `cd backend && mvn spring-boot:run`.
5. **Flyway migrations**: Run automatically on startup — creates all tables, indexes, triggers, and seeds reference data.
6. **Swagger UI**: `http://localhost:8080/swagger-ui/index.html`.
7. **Smoke tests** (curl examples):

```bash
# 1. Register a volunteer
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Test","lastName":"User","email":"test@example.com","phone":"+905321234567","bloodType":"A_POSITIVE","districtId":"<district-uuid>","neighborhoodId":"<neighborhood-uuid>","address":"Test Mah. No:1","password":"TestP@ss123"}'

# 2. Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"TestP@ss123"}'

# 3. Get own profile (use token from step 2)
curl -X GET http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer <access_token>"

# 4. List all districts
curl -X GET http://localhost:8080/api/v1/districts \
  -H "Authorization: Bearer <access_token>"

# 5. List all teams
curl -X GET http://localhost:8080/api/v1/teams \
  -H "Authorization: Bearer <access_token>"
```

---

## 13. Phase 10 — Verification Checklist

**Status: ❌ Pending**

### Build Verification
- [ ] `mvn compile` — zero compilation errors.
- [ ] `mvn test` — all unit tests pass (if tests are written).

### Runtime Verification
- [ ] Application starts with Docker PostgreSQL running.
- [ ] Flyway migration output shows all 5 versions applied.
- [ ] Swagger UI loads at `/swagger-ui/index.html`.

### API Smoke Tests
- [ ] `POST /auth/register` → 201 for valid payload; 409 for duplicate email; 422 for invalid neighborhood–district combination.
- [ ] `POST /auth/login` → 200 with tokens; 401 for wrong password; 403 for deactivated account.
- [ ] `POST /auth/refresh` → 200 with new token pair; 401 for expired refresh token.
- [ ] `GET /districts` → 200 with 10 districts.
- [ ] `GET /teams` → 200 with 6 teams.
- [ ] `POST /teams/{teamId}/join` (SEARCH_RESCUE, no document) → 422 BUSINESS_RULE_VIOLATION.
- [ ] `POST /events` as ADMIN → 201; as VOLUNTEER → 403.
- [ ] `POST /events/{id}/close` (already closed) → 409 CONFLICT.
- [ ] `POST /users/me/documents` (invalid MIME) → 400 VALIDATION_ERROR.
- [ ] `PATCH /documents/{id}/approve` (wrong district scope) → 422 or 403.
- [ ] `POST /admin/simulations` → 202 Accepted; simulation queued.
- [ ] `GET /admin/simulations/{id}/logs` → paginated log entries.

### Security Verification
- [ ] Accessing any protected endpoint without token → 401.
- [ ] VOLUNTEER accessing `/admin/users` → 403.
- [ ] DC accessing another district's events → 403.
- [ ] NC attempting to approve document → 403.

### Risk Score Verification
- [ ] Create event (SEARCH_RESCUE, urgency=4, people=10) → `riskScore = 200.0`.
- [ ] Close same event → `riskScore = 40.0`; neighborhood and district risk updated accordingly.

---

*End of Implementation Plan*
*Next steps: Implement Phase 5 (Response DTOs) → Phase 6 (Services) → Phase 7 (Controllers) → Phase 8 (SwaggerConfig) → Phase 9 (RUN_BACKEND.md)*
