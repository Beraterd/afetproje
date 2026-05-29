# MASTER SPEC — PART 2
## Istanbul Disaster Coordination & Volunteer Management Platform

**Version:** 1.0  
**Date:** 2026-03-03  
**Scope:** REST API Contract · Risk Scoring Algorithm · Event Lifecycle · Earthquake Simulation Workflow  
**Depends on:** master-spec-part1.md (Domain Model, Schema, RBAC, Hierarchy)

---

## Table of Contents

1. [Complete REST API Contract](#1-complete-rest-api-contract)
2. [Risk Scoring Algorithm](#2-risk-scoring-algorithm)
3. [Event Lifecycle Model](#3-event-lifecycle-model)
4. [Earthquake Simulation Workflow](#4-earthquake-simulation-workflow)

---

## 1. Complete REST API Contract

### 1.0 Global Conventions

| Convention           | Value                                                              |
|----------------------|--------------------------------------------------------------------|
| Base URL             | `https://api.afetkoordinasyon.istanbul/api/v1`                    |
| Content-Type         | `application/json`                                                 |
| Authentication       | `Authorization: Bearer <access_token>` (JWT)                      |
| Pagination params    | `?page=0&size=20&sort=createdAt,desc`                              |
| Timestamp format     | ISO-8601 with timezone: `2026-03-03T14:00:00+03:00`               |
| UUID format          | RFC-4122 standard UUID v4                                          |
| Error envelope       | See §1.0.1                                                         |

#### 1.0.1 Standard Error Envelope

```json
{
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "urgency must be between 1 and 5",
  "timestamp": "2026-03-03T14:00:00+03:00",
  "path": "/api/v1/events",
  "details": [
    { "field": "urgency", "rejectedValue": 6, "message": "must be between 1 and 5" }
  ]
}
```

| HTTP Status | Error Code               | When Used                                      |
|-------------|--------------------------|------------------------------------------------|
| 400         | `VALIDATION_ERROR`       | Request body/param fails validation            |
| 401         | `UNAUTHORIZED`           | Missing or invalid JWT                         |
| 403         | `FORBIDDEN`              | Valid JWT but insufficient role/scope          |
| 404         | `NOT_FOUND`              | Entity not found                               |
| 409         | `CONFLICT`               | Duplicate unique field, state conflict         |
| 422         | `BUSINESS_RULE_VIOLATION`| Business rule enforced at service layer        |
| 500         | `INTERNAL_ERROR`         | Unexpected server error                        |

#### 1.0.2 Standard Paginated Response Wrapper

```json
{
  "content": [ /* array of items */ ],
  "page": 0,
  "size": 20,
  "totalElements": 142,
  "totalPages": 8,
  "last": false
}
```

---

### 1.1 Authentication Endpoints

---

#### POST /auth/register

Register a new user. Role defaults to VOLUNTEER. Email verification is NOT required to log in initially.

**Auth:** None

**Request:**
```json
{
  "firstName": "Ahmet",
  "lastName": "Yılmaz",
  "email": "ahmet@example.com",
  "phone": "+905321234567",
  "bloodType": "A_POSITIVE",
  "districtId": "d1a2b3c4-...",
  "neighborhoodId": "e5f6g7h8-...",
  "address": "Kurtköy Mah. Atatürk Cad. No:12",
  "profession": "Paramedic",
  "password": "SecureP@ss123"
}
```

**Response:** `201 Created`
```json
{
  "id": "a1b2c3d4-...",
  "firstName": "Ahmet",
  "lastName": "Yılmaz",
  "email": "ahmet@example.com",
  "role": "VOLUNTEER",
  "emailVerified": false,
  "createdAt": "2026-03-03T14:00:00+03:00"
}
```

**Error cases:**
- `409 CONFLICT` — email or phone already exists
- `400 VALIDATION_ERROR` — missing required fields, invalid blood type
- `422 BUSINESS_RULE_VIOLATION` — neighborhoodId does not belong to districtId, districtId not in active 10

---

#### POST /auth/login

**Auth:** None

**Request:**
```json
{
  "email": "ahmet@example.com",
  "password": "SecureP@ss123"
}
```

**Response:** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": "a1b2c3d4-...",
    "firstName": "Ahmet",
    "lastName": "Yılmaz",
    "email": "ahmet@example.com",
    "role": "VOLUNTEER"
  }
}
```

**Error cases:**
- `401 UNAUTHORIZED` — invalid credentials
- `403 FORBIDDEN` — account deactivated (`is_active = false`)

---

#### POST /auth/refresh

**Auth:** None (uses refresh token in body)

**Request:**
```json
{ "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4..." }
```

**Response:** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "bmV3UmVmcmVzaFRva2Vu...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

**Error cases:**
- `401 UNAUTHORIZED` — invalid, expired, or revoked refresh token

---

#### POST /auth/logout

Revokes the provided refresh token.

**Auth:** Bearer JWT

**Request:**
```json
{ "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4..." }
```

**Response:** `204 No Content`

---

### 1.2 User Profile Endpoints

---

#### GET /users/me

Returns the authenticated user's full profile.

**Auth:** Bearer JWT (any role)

**Response:** `200 OK`
```json
{
  "id": "a1b2c3d4-...",
  "firstName": "Ahmet",
  "lastName": "Yılmaz",
  "email": "ahmet@example.com",
  "emailVerified": true,
  "pendingEmail": null,
  "phone": "+905321234567",
  "bloodType": "A_POSITIVE",
  "district": { "id": "d1a2b3c4-...", "name": "Pendik" },
  "neighborhood": { "id": "e5f6g7h8-...", "name": "Kurtköy" },
  "address": "Kurtköy Mah. Atatürk Cad. No:12",
  "profession": "Paramedic",
  "role": "VOLUNTEER",
  "isActive": true,
  "createdAt": "2026-03-03T14:00:00+03:00",
  "updatedAt": "2026-03-03T14:00:00+03:00"
}
```

---

#### PATCH /users/me

Update own profile fields. Email and password changes have dedicated endpoints.

**Auth:** Bearer JWT (any role)

**Request:**
```json
{
  "firstName": "Ahmet",
  "lastName": "Yılmaz",
  "phone": "+905329876543",
  "bloodType": "B_POSITIVE",
  "districtId": "d1a2b3c4-...",
  "neighborhoodId": "e5f6g7h8-...",
  "address": "Yeni Mah. No:5",
  "profession": "Engineer"
}
```

**Response:** `200 OK` — updated user object (same shape as GET /users/me)

**Error cases:**
- `409 CONFLICT` — phone already in use by another user
- `422 BUSINESS_RULE_VIOLATION` — neighborhoodId does not belong to districtId

---

#### POST /users/me/change-email

Initiates email change. Sends a verification link to `newEmail`.

**Auth:** Bearer JWT (any role)

**Request:**
```json
{
  "newEmail": "ahmet.new@example.com",
  "password": "SecureP@ss123"
}
```

**Response:** `202 Accepted`
```json
{
  "message": "Verification email sent to ahmet.new@example.com. Link expires in 24 hours."
}
```

**Error cases:**
- `401 UNAUTHORIZED` — password incorrect
- `409 CONFLICT` — newEmail already registered

---

#### POST /auth/verify-email

Completes email change using the token from the verification link.

**Auth:** None

**Request:**
```json
{ "token": "a7f3e2d1c4b5..." }
```

**Response:** `200 OK`
```json
{ "message": "Email address updated successfully." }
```

**Error cases:**
- `400 VALIDATION_ERROR` — token missing
- `404 NOT_FOUND` — token not found
- `422 BUSINESS_RULE_VIOLATION` — token expired or already used

---

#### POST /users/me/change-password

**Auth:** Bearer JWT (any role)

**Request:**
```json
{
  "currentPassword": "OldP@ss123",
  "newPassword": "NewP@ss456"
}
```

**Response:** `204 No Content`

**Error cases:**
- `401 UNAUTHORIZED` — currentPassword incorrect
- `400 VALIDATION_ERROR` — newPassword too weak (min 8 chars, mix required)

---

#### GET /users/me/events

Lists events the authenticated user has joined.

**Auth:** Bearer JWT (any role)

**Response:** `200 OK` (paginated)
```json
{
  "content": [
    {
      "id": "ev111-...",
      "title": "Rescue Operation - Kurtköy",
      "status": "OPEN",
      "urgency": 4,
      "team": { "id": "t1-...", "name": "SEARCH_RESCUE" },
      "neighborhood": { "id": "n1-...", "name": "Kurtköy" },
      "joinedAt": "2026-03-02T10:00:00+03:00",
      "volunteerStatus": "ASSIGNED"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 3,
  "totalPages": 1,
  "last": true
}
```

---

### 1.3 Admin — User Management Endpoints

---

#### GET /admin/users

List all users with optional filters.

**Auth:** Bearer JWT — ADMIN

**Query params:** `?role=VOLUNTEER&districtId=...&isActive=true&page=0&size=20`

**Response:** `200 OK` (paginated list of user summary objects)

---

#### GET /admin/users/{userId}

**Auth:** Bearer JWT — ADMIN

**Response:** `200 OK` — full user profile object

**Error cases:** `404 NOT_FOUND`

---

#### PATCH /admin/users/{userId}/role

Assign or change a user's role.

**Auth:** Bearer JWT — ADMIN

**Request:**
```json
{
  "role": "DISTRICT_COORDINATOR",
  "districtId": "d1a2b3c4-..."
}
```

**Response:** `200 OK` — updated user profile

**Error cases:**
- `404 NOT_FOUND` — user not found
- `422 BUSINESS_RULE_VIOLATION` — districtId already has a coordinator; user email not verified; districtId mismatch; role transition not permitted

---

#### PATCH /admin/users/{userId}/deactivate

Soft-deactivates a user account.

**Auth:** Bearer JWT — ADMIN

**Response:** `204 No Content`

**Error cases:**
- `404 NOT_FOUND`
- `422 BUSINESS_RULE_VIOLATION` — cannot deactivate an ADMIN account

---

### 1.4 District Endpoints

---

#### GET /districts

Returns all 10 active districts with risk scores and map polygon.

**Auth:** Bearer JWT (any role)

**Response:** `200 OK`
```json
[
  {
    "id": "d1a2b3c4-...",
    "name": "Pendik",
    "riskScore": 87.5,
    "riskColor": "RED",
    "coordinator": { "id": "u1-...", "firstName": "Mehmet", "lastName": "Demir" },
    "polygon": { "type": "Polygon", "coordinates": [ [ [29.12, 40.87], [29.15, 40.87], [29.15, 40.90], [29.12, 40.90], [29.12, 40.87] ] ] },
    "riskScoreUpdatedAt": "2026-03-03T13:00:00+03:00"
  }
]
```

---

#### GET /districts/{districtId}

**Auth:** Bearer JWT (any role)

**Response:** `200 OK` — single district object (same shape as above)

**Error cases:** `404 NOT_FOUND`

---

#### GET /districts/{districtId}/neighborhoods

Returns neighborhoods within a district with risk scores.

**Auth:** Bearer JWT (any role)

**Response:** `200 OK`
```json
[
  {
    "id": "n1-...",
    "name": "Kurtköy",
    "riskScore": 32.0,
    "riskColor": "GREEN",
    "coordinator": { "id": "u2-...", "firstName": "Ayşe", "lastName": "Kaya" },
    "polygon": { "type": "Polygon", "coordinates": [ /* ... */ ] }
  }
]
```

---

#### PATCH /admin/districts/{districtId}/coordinator

Assign a District Coordinator.

**Auth:** Bearer JWT — ADMIN

**Request:**
```json
{ "userId": "u1a2b3c4-..." }
```

**Response:** `200 OK` — updated district object

**Error cases:**
- `422 BUSINESS_RULE_VIOLATION` — user not verified, user district mismatch, district already has coordinator, role transition blocked

---

### 1.5 Neighborhood Endpoints

---

#### GET /neighborhoods/{neighborhoodId}

**Auth:** Bearer JWT (any role)

**Response:** `200 OK`
```json
{
  "id": "n1-...",
  "name": "Kurtköy",
  "districtId": "d1-...",
  "riskScore": 32.0,
  "riskColor": "GREEN",
  "coordinator": { "id": "u2-...", "firstName": "Ayşe", "lastName": "Kaya" },
  "polygon": { "type": "Polygon", "coordinates": [ /* ... */ ] },
  "assemblyAreas": [
    {
      "id": "aa1-...",
      "name": "Kurtköy Meydanı Toplanma Alanı",
      "latitude": 40.916,
      "longitude": 29.186,
      "capacity": 1500,
      "googleMapsUrl": "https://www.google.com/maps?q=40.916,29.186"
    }
  ]
}
```

---

#### GET /neighborhoods/{neighborhoodId}/events

Returns events in a neighborhood.

**Auth:** Bearer JWT (any role)

**Query params:** `?status=OPEN&teamId=...&page=0&size=20`

**Response:** `200 OK` (paginated event list)

---

#### PATCH /districts/{districtId}/neighborhoods/{neighborhoodId}/coordinator

Assign Neighborhood Coordinator.

**Auth:** Bearer JWT — ADMIN or DISTRICT_COORDINATOR (own district)

**Request:**
```json
{ "userId": "u2a3b4c5-..." }
```

**Response:** `200 OK` — updated neighborhood object

**Error cases:**
- `403 FORBIDDEN` — District Coordinator trying to assign outside own district
- `422 BUSINESS_RULE_VIOLATION` — email not verified, neighborhood already has coordinator, user neighborhood mismatch

---

### 1.6 Team Endpoints

---

#### GET /teams

List all 6 teams.

**Auth:** Bearer JWT (any role)

**Response:** `200 OK`
```json
[
  {
    "id": "t1-...",
    "name": "SEARCH_RESCUE",
    "coefficient": 5.0,
    "requiresDocument": "SEARCH_RESCUE_CERTIFICATE",
    "leader": { "id": "u3-...", "firstName": "Ali", "lastName": "Çelik" },
    "activeMemberCount": 42
  }
]
```

---

#### GET /teams/{teamId}/members

**Auth:** Bearer JWT — ADMIN, DISTRICT_COORDINATOR, NEIGHBORHOOD_COORDINATOR

**Response:** `200 OK` (paginated)
```json
{
  "content": [
    {
      "userId": "u1-...",
      "firstName": "Ahmet",
      "lastName": "Yılmaz",
      "joinedAt": "2026-02-01T10:00:00+03:00",
      "district": "Pendik",
      "neighborhood": "Kurtköy"
    }
  ]
}
```

---

#### POST /teams/{teamId}/join

Volunteer joins a team. For restricted teams, validates approved document.

**Auth:** Bearer JWT — VOLUNTEER

**Request body:** (empty — user derived from JWT)

**Response:** `201 Created`
```json
{
  "teamMembershipId": "tm1-...",
  "teamId": "t1-...",
  "teamName": "SEARCH_RESCUE",
  "joinedAt": "2026-03-03T14:00:00+03:00"
}
```

**Error cases:**
- `422 BUSINESS_RULE_VIOLATION` — team requires document; no APPROVED document found for user
- `409 CONFLICT` — user already an active member of this team

---

#### DELETE /teams/{teamId}/leave

Volunteer leaves a team.

**Auth:** Bearer JWT — VOLUNTEER

**Response:** `204 No Content`

**Error cases:**
- `404 NOT_FOUND` — user is not a member of this team
- `422 BUSINESS_RULE_VIOLATION` — user is currently ASSIGNED to an open event belonging to this team (must withdraw from events first)

---

### 1.7 Event Endpoints

---

#### GET /events

List events. Volunteers see only OPEN events. Coordinators see their scope.

**Auth:** Bearer JWT (any role)

**Query params:** `?status=OPEN&neighborhoodId=...&teamId=...&urgency=4&page=0&size=20`

**Response:** `200 OK` (paginated)
```json
{
  "content": [
    {
      "id": "ev1-...",
      "title": "Rescue Operation - Kurtköy Block 4",
      "status": "OPEN",
      "urgency": 4,
      "requiredPeople": 10,
      "riskScore": 200.0,
      "team": { "id": "t1-...", "name": "SEARCH_RESCUE" },
      "neighborhood": { "id": "n1-...", "name": "Kurtköy", "districtName": "Pendik" },
      "latitude": 40.916,
      "longitude": 29.186,
      "startsAt": "2026-03-04T08:00:00+03:00",
      "createdAt": "2026-03-03T14:00:00+03:00"
    }
  ]
}
```

---

#### POST /events

Create a new event.

**Auth:** Bearer JWT — ADMIN, DISTRICT_COORDINATOR (own district), NEIGHBORHOOD_COORDINATOR (own neighborhood)

**Request:**
```json
{
  "title": "Rescue Operation - Kurtköy Block 4",
  "description": "Building collapse at Block 4. Immediate search and rescue needed.",
  "neighborhoodId": "n1-...",
  "teamId": "t1-...",
  "urgency": 4,
  "requiredPeople": 10,
  "latitude": 40.9163,
  "longitude": 29.1864,
  "startsAt": "2026-03-04T08:00:00+03:00",
  "endsAt": "2026-03-04T20:00:00+03:00"
}
```

**Response:** `201 Created`
```json
{
  "id": "ev1-...",
  "title": "Rescue Operation - Kurtköy Block 4",
  "status": "OPEN",
  "urgency": 4,
  "requiredPeople": 10,
  "riskScore": 200.0,
  "team": { "id": "t1-...", "name": "SEARCH_RESCUE" },
  "neighborhood": { "id": "n1-...", "name": "Kurtköy" },
  "createdBy": { "id": "u2-...", "firstName": "Ayşe", "lastName": "Kaya" },
  "createdAt": "2026-03-03T14:00:00+03:00"
}
```

**Error cases:**
- `403 FORBIDDEN` — coordinator accessing outside their scope
- `400 VALIDATION_ERROR` — urgency out of 1–5 range, requiredPeople < 1, endsAt before startsAt

---

#### GET /events/{eventId}

**Auth:** Bearer JWT (any role)

**Response:** `200 OK`
```json
{
  "id": "ev1-...",
  "title": "Rescue Operation - Kurtköy Block 4",
  "description": "Building collapse at Block 4.",
  "status": "OPEN",
  "urgency": 4,
  "requiredPeople": 10,
  "riskScore": 200.0,
  "team": { "id": "t1-...", "name": "SEARCH_RESCUE", "coefficient": 5.0 },
  "neighborhood": { "id": "n1-...", "name": "Kurtköy" },
  "createdBy": { "id": "u2-...", "firstName": "Ayşe", "lastName": "Kaya" },
  "latitude": 40.9163,
  "longitude": 29.1864,
  "startsAt": "2026-03-04T08:00:00+03:00",
  "endsAt": "2026-03-04T20:00:00+03:00",
  "closedAt": null,
  "assignedVolunteers": 7,
  "createdAt": "2026-03-03T14:00:00+03:00",
  "updatedAt": "2026-03-03T14:00:00+03:00"
}
```

---

#### PATCH /events/{eventId}

Update event metadata (not status).

**Auth:** Bearer JWT — ADMIN, DISTRICT_COORDINATOR (own district), NEIGHBORHOOD_COORDINATOR (own neighborhood)

**Request:**
```json
{
  "title": "Rescue Operation - Updated",
  "urgency": 5,
  "requiredPeople": 15,
  "description": "Updated: additional resources required."
}
```

**Response:** `200 OK` — updated event object

**Error cases:**
- `422 BUSINESS_RULE_VIOLATION` — event is CLOSED, no updates allowed on closed events
- `403 FORBIDDEN` — scope violation

---

#### POST /events/{eventId}/close

Closes an event. Triggers risk score recalculation.

**Auth:** Bearer JWT — ADMIN, DISTRICT_COORDINATOR (own district), NEIGHBORHOOD_COORDINATOR (own neighborhood)

**Request body:** (empty)

**Response:** `200 OK`
```json
{
  "id": "ev1-...",
  "status": "CLOSED",
  "closedAt": "2026-03-03T18:00:00+03:00",
  "riskScore": 40.0
}
```

**Error cases:**
- `409 CONFLICT` — event already CLOSED

---

#### GET /events/{eventId}/volunteers

List volunteers assigned to an event.

**Auth:** Bearer JWT — ADMIN, DISTRICT_COORDINATOR, NEIGHBORHOOD_COORDINATOR

**Response:** `200 OK` (paginated)
```json
{
  "content": [
    {
      "userId": "u1-...",
      "firstName": "Ahmet",
      "lastName": "Yılmaz",
      "status": "ASSIGNED",
      "joinedAt": "2026-03-03T14:30:00+03:00"
    }
  ]
}
```

---

#### POST /events/{eventId}/join

Volunteer joins an event.

**Auth:** Bearer JWT — VOLUNTEER

**Request body:** (empty)

**Response:** `201 Created`
```json
{
  "eventVolunteerId": "ev-vol-1-...",
  "eventId": "ev1-...",
  "status": "ASSIGNED",
  "joinedAt": "2026-03-03T14:30:00+03:00"
}
```

**Error cases:**
- `422 BUSINESS_RULE_VIOLATION` — volunteer not a member of the event's team; event is CLOSED
- `409 CONFLICT` — already joined this event

---

#### DELETE /events/{eventId}/leave

Volunteer withdraws from an event.

**Auth:** Bearer JWT — VOLUNTEER

**Response:** `204 No Content`

**Error cases:**
- `404 NOT_FOUND` — user not assigned to this event
- `422 BUSINESS_RULE_VIOLATION` — event already CLOSED

---

### 1.8 Document Endpoints

---

#### GET /users/me/documents

**Auth:** Bearer JWT (any role)

**Response:** `200 OK`
```json
[
  {
    "id": "doc1-...",
    "documentType": "SEARCH_RESCUE_CERTIFICATE",
    "status": "APPROVED",
    "fileName": "kurtarma_sertifikasi.pdf",
    "fileSizeBytes": 204800,
    "rejectionReason": null,
    "reviewedAt": "2026-02-15T10:00:00+03:00",
    "createdAt": "2026-02-10T09:00:00+03:00"
  }
]
```

---

#### POST /users/me/documents

Upload a document (multipart form upload).

**Auth:** Bearer JWT — VOLUNTEER

**Request:** `multipart/form-data`
```
documentType: SEARCH_RESCUE_CERTIFICATE
file: <binary>
```

**Response:** `201 Created`
```json
{
  "id": "doc2-...",
  "documentType": "SEARCH_RESCUE_CERTIFICATE",
  "status": "PENDING",
  "fileName": "kurtarma_sertifikasi.pdf",
  "fileSizeBytes": 204800,
  "mimeType": "application/pdf",
  "createdAt": "2026-03-03T14:00:00+03:00"
}
```

**Error cases:**
- `400 VALIDATION_ERROR` — no file provided, unsupported mime type (PDF and JPEG/PNG only), file too large (max 10 MB)

---

#### GET /documents/{documentId}/download

Returns a pre-signed URL to the file in S3-compatible storage (TTL 15 minutes).

**Auth:** Bearer JWT — ADMIN, DISTRICT_COORDINATOR; or VOLUNTEER (own document only)

**Response:** `200 OK`
```json
{
  "presignedUrl": "https://storage.example.com/documents/doc2.pdf?X-Amz-Signature=...",
  "expiresAt": "2026-03-03T14:15:00+03:00"
}
```

**Error cases:**
- `403 FORBIDDEN` — non-owner volunteer
- `404 NOT_FOUND`

---

#### GET /admin/documents/pending

List all documents awaiting review.

**Auth:** Bearer JWT — ADMIN, DISTRICT_COORDINATOR (own district)

**Query params:** `?documentType=SEARCH_RESCUE_CERTIFICATE&districtId=...&page=0&size=20`

**Response:** `200 OK` (paginated)
```json
{
  "content": [
    {
      "id": "doc2-...",
      "documentType": "SEARCH_RESCUE_CERTIFICATE",
      "status": "PENDING",
      "fileName": "kurtarma_sertifikasi.pdf",
      "owner": { "id": "u1-...", "firstName": "Ahmet", "lastName": "Yılmaz", "district": "Pendik" },
      "createdAt": "2026-03-03T14:00:00+03:00"
    }
  ]
}
```

---

#### PATCH /documents/{documentId}/approve

**Auth:** Bearer JWT — ADMIN, DISTRICT_COORDINATOR (own district)

**Request body:** (empty)

**Response:** `200 OK`
```json
{
  "id": "doc2-...",
  "status": "APPROVED",
  "reviewedAt": "2026-03-03T15:00:00+03:00",
  "reviewedBy": { "id": "u-admin-...", "firstName": "Sistem", "lastName": "Admin" }
}
```

**Error cases:**
- `409 CONFLICT` — document already APPROVED or REJECTED
- `422 BUSINESS_RULE_VIOLATION` — document owner not in reviewer's district scope

---

#### PATCH /documents/{documentId}/reject

**Auth:** Bearer JWT — ADMIN, DISTRICT_COORDINATOR (own district)

**Request:**
```json
{ "reason": "Certificate is expired. Issue date exceeds 2-year validity period." }
```

**Response:** `200 OK`
```json
{
  "id": "doc2-...",
  "status": "REJECTED",
  "rejectionReason": "Certificate is expired. Issue date exceeds 2-year validity period.",
  "reviewedAt": "2026-03-03T15:00:00+03:00"
}
```

**Error cases:**
- `400 VALIDATION_ERROR` — reason is blank or null
- `409 CONFLICT` — document already in terminal state

---

### 1.9 Earthquake Simulation Endpoints

---

#### POST /admin/simulations

Trigger an earthquake simulation. Admin only.

**Auth:** Bearer JWT — ADMIN

**Request:**
```json
{
  "districtId": "d1a2b3c4-...",
  "magnitude": 7.2,
  "notes": "Scenario: Marmara Fault Line rupture. Istanbul pilot simulation."
}
```

**Response:** `202 Accepted`
```json
{
  "simulationId": "sim1-...",
  "districtId": "d1a2b3c4-...",
  "districtName": "Pendik",
  "magnitude": 7.2,
  "emailStatus": "QUEUED",
  "totalUsersToNotify": 14823,
  "triggeredAt": "2026-03-03T16:00:00+03:00"
}
```

**Error cases:**
- `400 VALIDATION_ERROR` — magnitude out of valid range (0–10)
- `404 NOT_FOUND` — districtId not found or inactive

---

#### GET /admin/simulations

List all past simulations.

**Auth:** Bearer JWT — ADMIN

**Query params:** `?districtId=...&page=0&size=20`

**Response:** `200 OK` (paginated list of simulation summary objects)

---

#### GET /admin/simulations/{simulationId}

**Auth:** Bearer JWT — ADMIN

**Response:** `200 OK`
```json
{
  "id": "sim1-...",
  "district": { "id": "d1-...", "name": "Pendik" },
  "magnitude": 7.2,
  "notes": "Scenario: Marmara Fault Line rupture.",
  "emailStatus": "COMPLETED",
  "totalQueued": 14823,
  "totalSent": 14750,
  "totalFailed": 73,
  "triggeredAt": "2026-03-03T16:00:00+03:00",
  "createdBy": { "id": "u-admin-...", "firstName": "Sistem", "lastName": "Admin" }
}
```

---

#### GET /admin/simulations/{simulationId}/logs

Notification delivery log per user.

**Auth:** Bearer JWT — ADMIN

**Query params:** `?status=FAILED&page=0&size=20`

**Response:** `200 OK` (paginated)
```json
{
  "content": [
    {
      "id": "log1-...",
      "userId": "u1-...",
      "emailAddress": "ahmet@example.com",
      "status": "FAILED",
      "retryCount": 3,
      "lastError": "Invalid email address",
      "sentAt": null
    }
  ]
}
```

---

### 1.10 Map & Risk Endpoints

---

#### GET /map/districts

Returns all active districts with risk color classification for map rendering.

**Auth:** Bearer JWT (any role)

**Response:** `200 OK`
```json
[
  {
    "id": "d1-...",
    "name": "Pendik",
    "riskScore": 87.5,
    "riskColor": "RED",
    "polygon": {
      "type": "Feature",
      "properties": { "districtId": "d1-...", "name": "Pendik", "riskColor": "RED" },
      "geometry": { "type": "Polygon", "coordinates": [ [ [29.12, 40.87], [29.15, 40.87], [29.15, 40.90], [29.12, 40.90], [29.12, 40.87] ] ] }
    }
  }
]
```

---

#### GET /map/districts/{districtId}/neighborhoods

Returns neighborhood polygons for a given district for drill-down map view.

**Auth:** Bearer JWT (any role)

**Response:** `200 OK`
```json
[
  {
    "id": "n1-...",
    "name": "Kurtköy",
    "riskScore": 32.0,
    "riskColor": "GREEN",
    "polygon": {
      "type": "Feature",
      "properties": { "neighborhoodId": "n1-...", "name": "Kurtköy", "riskColor": "GREEN" },
      "geometry": { "type": "Polygon", "coordinates": [ /* ... */ ] }
    }
  }
]
```

---

## 2. Risk Scoring Algorithm

### 2.1 Core Formula

```
EventRisk = TeamCoefficient × Urgency × RequiredPeople × StatusCoefficient
```

#### Input Parameters

| Parameter         | Type           | Source           | Constraints        |
|-------------------|----------------|------------------|--------------------|
| TeamCoefficient   | NUMERIC(4,2)   | `teams.coefficient` | Fixed per team  |
| Urgency           | SMALLINT       | `events.urgency` | Integer 1–5        |
| RequiredPeople    | INTEGER        | `events.required_people` | > 0          |
| StatusCoefficient | DECIMAL        | Derived from `events.status` | See §2.2 |

#### Team Coefficients

| Team                | Coefficient |
|---------------------|-------------|
| SEARCH_RESCUE       | 5.0         |
| EVACUATION          | 4.0         |
| FOOD_WATER          | 3.0         |
| PSYCHOSOCIAL        | 3.0         |
| LOGISTICS           | 2.0         |
| COMMUNICATION       | 2.0         |

#### Status Coefficients

| Event Status | StatusCoefficient | Rationale                                              |
|--------------|-------------------|--------------------------------------------------------|
| OPEN         | 1.0               | Active threat; full risk weight applies                |
| CLOSED       | 0.2               | Resolved event; residual historical weight retained    |

#### Worked Example

```
Event: Search & Rescue, Urgency=4, Required=10, Status=OPEN
EventRisk = 5.0 × 4 × 10 × 1.0 = 200.0

Same event after closure:
EventRisk = 5.0 × 4 × 10 × 0.2 = 40.0
```

---

### 2.2 Aggregation Chain

```
NeighborhoodRisk = Σ EventRisk_i    (for all events in that neighborhood)
DistrictRisk     = Σ NeighborhoodRisk_j  (for all neighborhoods in that district)
```

Both values are stored as denormalized columns (`risk_score`) in `neighborhoods` and `districts` tables, updated on each recalculation trigger.

---

### 2.3 Risk Color Threshold Logic

Applied to both neighborhoods and districts using the same thresholds:

| Score Range       | Color      | Hex      | Meaning                   |
|-------------------|------------|----------|---------------------------|
| `score >= 70`     | **RED**    | `#E53935`| High risk — critical      |
| `40 <= score < 70`| **YELLOW** | `#FDD835`| Medium risk — caution     |
| `score < 40`      | **GREEN**  | `#43A047`| Low risk — stable         |

Color classification is computed at query time from the stored `risk_score` value; it is not persisted separately. The API returns both `riskScore` and `riskColor`.

---

### 2.4 Recalculation Triggers

Risk scores must be recalculated immediately when any of the following changes occur:

| Trigger Event                                      | Recalculate                                    |
|----------------------------------------------------|------------------------------------------------|
| New event created (`status=OPEN`)                  | Affected neighborhood + its district           |
| Event `urgency` updated                            | Affected neighborhood + its district           |
| Event `required_people` updated                    | Affected neighborhood + its district           |
| Event `status` changed to `CLOSED`                 | Affected neighborhood + its district           |
| Event deleted                                      | Affected neighborhood + its district           |
| Event moved to a different neighborhood (if allowed)| Both old and new neighborhood + districts      |

#### Recalculation Procedure (Service-Layer Logic)

```
PROCEDURE recalculateRisk(neighborhoodId):
  1. SELECT all events WHERE neighborhood_id = neighborhoodId
  2. FOR each event:
       eventRisk = team.coefficient × event.urgency × event.required_people
                   × (IF event.status = 'OPEN' THEN 1.0 ELSE 0.2)
  3. neighborhoodRisk = SUM(all eventRisk values)
  4. UPDATE neighborhoods SET risk_score = neighborhoodRisk,
                              risk_score_updated_at = NOW()
     WHERE id = neighborhoodId
  5. districtId = neighborhood.district_id
  6. SELECT SUM(risk_score) FROM neighborhoods WHERE district_id = districtId
  7. UPDATE districts SET risk_score = SUM, risk_score_updated_at = NOW()
     WHERE id = districtId
```

#### Caching Consideration

- Risk scores are stored denormalized; no secondary cache is required for normal reads.
- The `risk_score_updated_at` field allows consumers to detect staleness.
- For high-frequency map polling: add a Redis cache with a **30-second TTL** on the district list endpoint (`GET /map/districts`). Invalidate on any risk score update.

---

## 3. Event Lifecycle Model

### 3.1 States

| State    | Description                                                                 |
|----------|-----------------------------------------------------------------------------|
| `OPEN`   | Event is active. Volunteers may join. Risk weight = 1.0.                    |
| `CLOSED` | Event is resolved. No new volunteers. Risk weight = 0.2. Immutable metadata.|

Only two states exist. There is no DRAFT, PENDING, or CANCELLED state in this system.

---

### 3.2 State Transition Diagram

```
                    ┌──────────────────────────┐
                    │                          │
    [Create Event]  │          OPEN            │   [Close Event]
        ──────────► │  - Active                │ ─────────────────►  CLOSED
                    │  - StatusCoeff = 1.0     │                        │
                    │  - Volunteers can join   │                        │
                    │  - Editable              │                        │
                    │                          │              - StatusCoeff = 0.2
                    └──────────────────────────┘              - Read-only metadata
                                                              - Volunteers can't join
                                                              - closed_at auto-set
                                                              - Risk recalculated
                                                              - No reversal possible
```

**Transitions:**

| From   | To     | Trigger                              | Who Can Trigger                              |
|--------|--------|--------------------------------------|----------------------------------------------|
| *(new)*| OPEN   | `POST /events`                       | ADMIN, District Coordinator, Neighborhood Coordinator |
| OPEN   | CLOSED | `POST /events/{eventId}/close`       | ADMIN, District Coordinator (own), Neighborhood Coordinator (own) |
| CLOSED | OPEN   | *(not permitted)*                    | Nobody — closure is irreversible              |

---

### 3.3 State-Dependent Business Rules

#### OPEN state

- Metadata (title, description, urgency, requiredPeople, coordinates, times) is editable.
- Volunteers with appropriate team membership may join via `POST /events/{id}/join`.
- Volunteers may withdraw via `DELETE /events/{id}/leave`.
- Risk score uses `StatusCoefficient = 1.0`.
- Is included in neighborhood and district risk aggregation at full weight.

#### CLOSED state

- Metadata becomes **immutable** — no PATCH updates accepted.
- `closed_at` is auto-set to `NOW()` via database trigger.
- No new volunteer assignments accepted.
- Existing volunteer records are preserved; their statuses are set to `COMPLETED`.
- Risk score uses `StatusCoefficient = 0.2`.
- A recalculation of neighborhood and district risk is triggered immediately upon closure.
- The event remains visible in history queries with `?status=CLOSED` filter.

---

### 3.4 Volunteer Status Sub-Lifecycle (within an Event)

| Status      | Meaning                                                      |
|-------------|--------------------------------------------------------------|
| `ASSIGNED`  | Volunteer is actively joined to the OPEN event               |
| `COMPLETED` | Event closed while volunteer was ASSIGNED                    |
| `WITHDRAWN` | Volunteer explicitly left the event before closure           |

```
          ┌─────[join event]──────► ASSIGNED ──[event closed]──► COMPLETED
          │                             │
  (volunteer)                    [leave event]
                                        │
                                        ▼
                                   WITHDRAWN
```

No transitions are possible out of `COMPLETED` or `WITHDRAWN` — both are terminal states for the volunteer's participation record on that event.

---

## 4. Earthquake Simulation Workflow

### 4.1 Overview

The earthquake simulation is an admin-initiated broadcast scenario that sends emergency notification emails to all registered and active users in the system. It simulates a real earthquake event, informing users of the affected district, magnitude, and the nearest assembly areas for their neighborhood.

---

### 4.2 Trigger Flow

```
Step 1 — Admin initiates simulation
  ├─ Admin sends POST /admin/simulations
  │    { districtId, magnitude, notes }
  ├─ System validates: districtId is active, magnitude in range
  └─ System creates EarthquakeSimulation record (status = QUEUED)
       └─ Returns 202 Accepted with simulationId

Step 2 — Job is enqueued
  ├─ Application publishes a SimulationTriggeredEvent to the async queue
  │    (message payload: simulationId, districtId, magnitude)
  └─ HTTP response is returned immediately → non-blocking

Step 3 — Async worker picks up job
  ├─ Worker updates simulation: emailStatus = PROCESSING
  ├─ Worker fetches ALL active users (is_active = true, email_verified = true)
  │    → Query returns: userId, email, neighborhoodId
  ├─ For each user:
  │    a) Fetch user's neighborhood → fetch assembly areas for that neighborhood
  │    b) Build personalized email payload (see §4.3)
  │    c) Create SimulationNotificationLog record (status = QUEUED)
  │    d) Enqueue individual email send task
  └─ After all logs created: update simulation emailStatus = PROCESSING

Step 4 — Email dispatch (per-user)
  ├─ Send email via provider (SendGrid or AWS SES)
  ├─ On SUCCESS:
  │    └─ Update SimulationNotificationLog: status = SENT, sent_at = NOW()
  └─ On FAILURE:
       ├─ Update SimulationNotificationLog: status = FAILED, last_error = <error>
       ├─ Increment retry_count
       └─ Re-enqueue with exponential backoff (see §4.4)

Step 5 — Completion check
  ├─ After all per-user tasks resolve, worker evaluates:
  │    IF failed_count = 0  → emailStatus = COMPLETED
  │    ELSE                 → emailStatus = PARTIAL_FAILURE
  └─ Update EarthquakeSimulation.emailStatus accordingly
```

---

### 4.3 Email Content Contract

Each user receives a **personalized email** containing:

| Field                 | Value                                                                     |
|-----------------------|---------------------------------------------------------------------------|
| Subject               | `⚠️ DEPREM SİMÜLASYONU — Magnitude {magnitude} — {districtName}`         |
| Affected District     | Human-readable district name (e.g., "Pendik")                             |
| Magnitude             | Richter scale value (e.g., 7.2)                                           |
| User's Neighborhood   | User's registered neighborhood name                                       |
| Assembly Areas        | List of all assembly areas for user's neighborhood, each with:            |
| — Name                | e.g. "Kurtköy Meydanı Toplanma Alanı"                                    |
| — Capacity            | e.g. "1500 kişi"                                                         |
| — Google Maps link    | `https://www.google.com/maps?q={latitude},{longitude}`                    |
| Simulation timestamp  | Date and time the simulation was triggered                                |
| Disclaimer            | "Bu bir tatbikat mesajıdır. Gerçek bir acil durum değildir."             |

**Email recipient rules:**
- Send to ALL users where `is_active = true` **and** `email_verified = true`.
- Users with `is_active = false` or `email_verified = false` are excluded.
- Assembly areas shown are those of **the user's registered neighborhood** — not the affected district's neighborhoods.
- If a user's neighborhood has no assembly areas defined, include a fallback message: "Bölgenize özel toplanma alanı henüz tanımlanmamıştır. En yakın açık alana yönelin."

---

### 4.4 Retry & Failure Handling Rules

| Rule                | Specification                                                              |
|---------------------|----------------------------------------------------------------------------|
| Max retry count     | 3 attempts per email                                                       |
| Retry strategy      | Exponential backoff — delays: 30s → 2m → 10m                              |
| Retry eligibility   | Only `status = FAILED` and `retry_count < 3`                               |
| Permanent failure   | After 3 retries with no success, status remains `FAILED`, no further retry |
| Bounced emails      | Provider webhooks update status to `BOUNCED`; bounced emails are NOT retried|
| Partial failure     | Simulation marked `PARTIAL_FAILURE`; admin can view failed logs via API    |
| Manual retry        | (Future scope) Admin may trigger re-send for all FAILED logs in a simulation|

---

### 4.5 Notification Logging Rules

Every email dispatch attempt is captured in `simulation_notification_logs`:

| Event                           | Log Action                                                            |
|---------------------------------|-----------------------------------------------------------------------|
| Email job created               | Insert log with `status = QUEUED`                                     |
| Email sent successfully         | Update `status = SENT`, `sent_at = NOW()`                             |
| Email send fails (attempt)      | Update `status = FAILED`, increment `retry_count`, set `last_error`   |
| Email retried successfully      | Update `status = SENT`, `sent_at = NOW()`                             |
| Bounce webhook received         | Update `status = BOUNCED`                                             |
| Max retries exhausted           | `status` stays `FAILED`, `retry_count = 3`, `last_error` preserved    |

**Log immutability rule:** Individual log records may be updated (status transitions) but not deleted. They serve as a permanent audit trail for every notification attempt.

**Email snapshot rule:** `simulation_notification_logs.email_address` stores the email at the time of dispatch. If the user changes their email afterward, the log still shows the email address that was used — preserving the historical accuracy of who was notified at what address.

---

*End of Master Spec — Part 2*  
*Next: Part 3 — Email Notification Architecture, Document Approval Workflow, Map Polygon Data Contract, Caching Strategy, Audit Logging Strategy*
