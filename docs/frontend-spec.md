# Frontend Specification
## Istanbul Disaster Coordination & Volunteer Management Platform

**Version:** 1.0
**Date:** 2026-03-05
**Audience:** Frontend Engineers
**Depends on:** `docs/master-spec-part1.md` · `docs/master-spec-part2.md` · `docs/implementation-plan.md`

---

## Table of Contents

1. [Frontend Technology Stack](#1-frontend-technology-stack)
2. [Project Folder Structure](#2-project-folder-structure)
3. [Authentication Flow](#3-authentication-flow)
4. [Route Architecture](#4-route-architecture)
5. [Page Specifications](#5-page-specifications)
6. [Dashboard Design](#6-dashboard-design)
7. [Event UI Flow](#7-event-ui-flow)
8. [Document Upload & Approval](#8-document-upload--approval)
9. [Risk Map Interface](#9-risk-map-interface)
10. [Simulation Interface](#10-simulation-interface)
11. [API Client Architecture](#11-api-client-architecture)
12. [State Management Strategy](#12-state-management-strategy)
13. [UI Component Library](#13-ui-component-library)
14. [Frontend Environment Configuration](#14-frontend-environment-configuration)
15. [Development Phases](#15-development-phases)
16. [Frontend Verification Checklist](#16-frontend-verification-checklist)

---

## 1. Frontend Technology Stack

### 1.1 Core Framework

**React 18 + TypeScript**
React is the component model. TypeScript enforces compile-time type safety across all API responses, form inputs, and component props — critical when dealing with complex business rules like role-scoped data, risk scores, and document states. All types are derived from the backend's DTO contracts.

---

### 1.2 Build Tool

**Vite**
Vite provides sub-second HMR and a fast build pipeline optimized for modern ES modules. It natively supports TypeScript, environment variables (`.env.*` files), and path aliases. The production build outputs a minimal, tree-shaken bundle.

Configuration highlights:
```ts
// vite.config.ts
resolve: {
  alias: { '@': '/src' }
},
server: {
  proxy: { '/api': 'http://localhost:8080' }  // dev proxy to Spring Boot
}
```

---

### 1.3 Routing

**React Router v6**
React Router provides declarative, nested routing with `<Outlet>` for layout composition. Protected routes are wrapped in a `<ProtectedRoute>` component that reads the current user's role from the auth store and redirects to `/login` or `/unauthorized` as needed.

Key patterns used:
- `<BrowserRouter>` at the root
- Layout routes (`AppLayout`, `AuthLayout`) wrap page content
- `<Navigate>` for role-based redirects
- `useNavigate` + `useParams` in page components

---

### 1.4 Server State & Data Fetching

**TanStack Query (React Query) v5**
TanStack Query manages all asynchronous server state: fetching, caching, background refetching, and cache invalidation. It eliminates manual `useEffect` + `useState` patterns for API calls.

Key benefits in this system:
- Automatic stale-while-revalidate for district/neighborhood lists
- Optimistic updates for event join/leave actions
- Query invalidation after mutations (e.g., close event → invalidate event list)
- Shared query keys across components prevent redundant fetches

---

### 1.5 HTTP Client

**Axios**
Axios is used as the HTTP client, wrapped in a configured instance (`src/api/axiosInstance.ts`). It handles:
- Base URL injection from `VITE_API_BASE_URL`
- JWT access token attachment via request interceptor
- Automatic token refresh on 401 via response interceptor
- Standardized error parsing (backend `ErrorResponse` shape)

---

### 1.6 Forms

**React Hook Form + Zod**
React Hook Form provides performant, uncontrolled form handling. Zod defines schema-based validation that mirrors backend validation rules (e.g., urgency 1–5, password min 8 chars, valid UUID fields). The integration uses `@hookform/resolvers/zod`.

Example:
```ts
const schema = z.object({
  urgency: z.number().int().min(1).max(5),
  requiredPeople: z.number().int().min(1),
  endsAt: z.string().optional(),
}).refine(data => !data.endsAt || ..., { message: "endsAt must be after startsAt" });
```

---

### 1.7 Styling

**TailwindCSS v3**
Utility-first CSS provides consistent spacing, color, and responsive design tokens without writing custom CSS files. A custom Tailwind config extends the palette with the system's risk colors:

```js
// tailwind.config.js
theme: {
  extend: {
    colors: {
      risk: {
        red:    '#E53935',
        yellow: '#FDD835',
        green:  '#43A047',
      }
    }
  }
}
```

---

### 1.8 Maps

**Leaflet + react-leaflet**
Leaflet is a lightweight, open-source mapping library. It renders GeoJSON district and neighborhood polygons, applies risk-color fills, and places event markers with popups. `react-leaflet` provides React component wrappers.

Used specifically for:
- District polygon layer (`GET /map/districts`)
- Neighborhood drill-down (`GET /map/districts/{id}/neighborhoods`)
- Event pin markers (`GET /events` — latitude/longitude fields)
- Assembly area markers within the neighborhood detail view

`Mapbox GL JS` can replace Leaflet if vector tile rendering or custom basemaps are required.

---

### 1.9 Authentication

**JWT Access Token + Refresh Token (in-memory + HttpOnly cookie strategy)**
- Access token stored in **memory** (React context / Zustand store). Never in `localStorage` to prevent XSS token theft.
- Refresh token stored in an **HttpOnly cookie** (set by the backend) or, if the backend serves raw tokens in JSON, stored in `sessionStorage` with short TTL awareness.
- Axios interceptor silently refreshes the access token before retrying failed requests.

---

## 2. Project Folder Structure

```
src/
├── api/                        ← Axios instance + service functions per domain
│   ├── axiosInstance.ts        ← Configured Axios client with interceptors
│   ├── auth.api.ts             ← register, login, refresh, logout, verifyEmail
│   ├── users.api.ts            ← getMe, updateProfile, changeEmail, changePassword
│   ├── districts.api.ts        ← getDistricts, getDistrict, assignCoordinator
│   ├── neighborhoods.api.ts    ← getNeighborhood, assignCoordinator
│   ├── teams.api.ts            ← getTeams, getTeamMembers, joinTeam, leaveTeam
│   ├── events.api.ts           ← CRUD, join, leave, close, getVolunteers
│   ├── documents.api.ts        ← upload, list, download, approve, reject
│   ├── simulations.api.ts      ← trigger, list, getDetail, getLogs
│   └── map.api.ts              ← getMapDistricts, getMapNeighborhoods
│
├── components/                 ← Pure, reusable UI primitives (no business logic)
│   ├── ui/
│   │   ├── Button.tsx
│   │   ├── FormField.tsx
│   │   ├── Modal.tsx
│   │   ├── DataTable.tsx
│   │   ├── Toast.tsx
│   │   ├── ConfirmationDialog.tsx
│   │   ├── LoadingSpinner.tsx
│   │   ├── Badge.tsx           ← Risk color, event status, document status
│   │   ├── Pagination.tsx
│   │   └── EmptyState.tsx
│   └── shared/
│       ├── RoleGuard.tsx       ← Conditionally renders children based on role
│       ├── PageHeader.tsx
│       ├── SideNav.tsx
│       └── TopBar.tsx
│
├── features/                   ← Domain-specific feature modules
│   ├── auth/
│   │   ├── LoginForm.tsx
│   │   ├── RegisterForm.tsx
│   │   └── VerifyEmailBanner.tsx
│   ├── events/
│   │   ├── EventCard.tsx
│   │   ├── EventFilters.tsx
│   │   ├── EventStatusBadge.tsx
│   │   ├── CreateEventForm.tsx
│   │   └── VolunteerTable.tsx
│   ├── documents/
│   │   ├── DocumentUploadForm.tsx
│   │   ├── DocumentStatusBadge.tsx
│   │   └── PendingDocumentRow.tsx
│   ├── map/
│   │   ├── DistrictLayer.tsx
│   │   ├── NeighborhoodLayer.tsx
│   │   ├── EventMarker.tsx
│   │   └── RiskLegend.tsx
│   ├── simulations/
│   │   ├── SimulationTriggerForm.tsx
│   │   └── SimulationLogTable.tsx
│   └── admin/
│       ├── UserTable.tsx
│       ├── RoleAssignModal.tsx
│       └── DeactivateUserButton.tsx
│
├── pages/                      ← One file per route; composes features + components
│   ├── auth/
│   │   ├── LoginPage.tsx
│   │   ├── RegisterPage.tsx
│   │   └── VerifyEmailPage.tsx
│   ├── DashboardPage.tsx
│   ├── ProfilePage.tsx
│   ├── events/
│   │   ├── EventsPage.tsx
│   │   ├── EventDetailPage.tsx
│   │   └── CreateEventPage.tsx
│   ├── documents/
│   │   ├── DocumentsPage.tsx
│   │   └── DocumentApprovalPage.tsx
│   ├── MapPage.tsx
│   ├── simulations/
│   │   ├── SimulationTriggerPage.tsx
│   │   └── SimulationDetailPage.tsx
│   └── admin/
│       ├── UsersPage.tsx
│       └── DistrictsPage.tsx
│
├── hooks/                      ← Custom React hooks
│   ├── useAuth.ts              ← Auth state accessors (user, role, isAuthenticated)
│   ├── useCurrentUser.ts       ← GET /users/me via TanStack Query
│   ├── useRoleGuard.ts         ← Returns boolean for role checks
│   ├── useToast.ts             ← Imperative toast trigger
│   └── useMapData.ts           ← Combined district + neighborhood map data
│
├── store/                      ← Client state (non-server state)
│   └── authStore.ts            ← Zustand store: accessToken, user summary, setAuth, clear
│
├── utils/                      ← Pure utility functions
│   ├── riskColor.ts            ← score → "RED" | "YELLOW" | "GREEN" + hex
│   ├── formatDate.ts           ← ISO-8601 → Turkish locale display
│   ├── queryKeys.ts            ← Centralized TanStack Query key factories
│   └── errorParser.ts          ← Parses backend ErrorResponse → user message
│
├── types/                      ← TypeScript interfaces mirroring backend DTOs
│   ├── auth.types.ts
│   ├── user.types.ts
│   ├── district.types.ts
│   ├── neighborhood.types.ts
│   ├── team.types.ts
│   ├── event.types.ts
│   ├── document.types.ts
│   ├── simulation.types.ts
│   └── common.types.ts         ← PagedResponse<T>, ErrorResponse, etc.
│
├── layouts/                    ← Layout wrappers used by the router
│   ├── AppLayout.tsx           ← SideNav + TopBar + <Outlet>
│   └── AuthLayout.tsx          ← Centered card layout for login/register
│
├── router/
│   └── index.tsx               ← All route definitions
│
├── App.tsx
├── main.tsx
└── index.css                   ← Tailwind directives + global resets
```

---

## 3. Authentication Flow

### 3.1 Register

1. User fills `RegisterForm` (name, email, phone, bloodType, districtId, neighborhoodId, address, profession, password).
2. District dropdown is populated from `GET /districts` on page load.
3. Neighborhood dropdown is populated from `GET /districts/{id}/neighborhoods` reactively when districtId changes.
4. On submit: `POST /auth/register`. On 201: redirect to `/login` with a banner: "Registration successful. You can now log in."
5. On 409 (email/phone taken) → show field-level error.
6. On 422 (neighborhood–district mismatch) → global error toast.

---

### 3.2 Login

1. `POST /auth/login` with email + password.
2. On success: store `accessToken` in `authStore` (Zustand, memory only). Store `refreshToken` in `sessionStorage` (or rely on HttpOnly cookie if backend supports it).
3. Decode JWT claims (`sub`, `role`, `districtId`, `neighborhoodId`, `email`) and store in `authStore.user`.
4. Redirect to `/dashboard`.
5. On 401: "Invalid email or password."
6. On 403: "Your account has been deactivated. Contact your administrator."

---

### 3.3 Token Storage Strategy

| Token | Storage | Rationale |
|---|---|---|
| Access token | Zustand in-memory (`authStore`) | Never persisted; lost on page refresh (intentional — short TTL 1h) |
| Refresh token | `sessionStorage` key `rt` | Survives tab only; cleared on tab close |

On page refresh: if `rt` exists in `sessionStorage`, immediately call `POST /auth/refresh` to obtain a new access token before rendering the app. This is done in `main.tsx` before mounting React.

---

### 3.4 Axios Interceptor Design

**Request interceptor** — attaches Authorization header:
```ts
instance.interceptors.request.use(config => {
  const token = authStore.getState().accessToken;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
```

**Response interceptor** — handles 401 with silent refresh:
```ts
instance.interceptors.response.use(
  res => res,
  async error => {
    const original = error.config;
    if (error.response?.status === 401 && !original._retry) {
      original._retry = true;
      try {
        const rt = sessionStorage.getItem('rt');
        const { data } = await axios.post('/auth/refresh', { refreshToken: rt });
        authStore.getState().setAccessToken(data.accessToken);
        sessionStorage.setItem('rt', data.refreshToken);
        original.headers.Authorization = `Bearer ${data.accessToken}`;
        return instance(original);           // retry original request
      } catch {
        authStore.getState().clear();
        sessionStorage.removeItem('rt');
        window.location.href = '/login';     // force logout
      }
    }
    return Promise.reject(error);
  }
);
```

A **queue mechanism** prevents multiple simultaneous refresh calls when several requests 401 at once. Pending requests are queued and replayed after a single refresh succeeds.

---

### 3.5 Logout

1. Call `POST /auth/logout` with current refresh token (revokes server-side).
2. Clear `authStore` (access token + user).
3. Remove `rt` from `sessionStorage`.
4. Clear TanStack Query cache (`queryClient.clear()`).
5. Redirect to `/login`.

---

### 3.6 Email Verification

- After initiating an email change (`POST /users/me/change-email`): UI shows a green banner "Verification email sent to {newEmail}."
- The verification link opens `/verify-email?token=xxx`.
- `VerifyEmailPage` extracts the token from the query string and calls `POST /auth/verify-email`.
- On success: "Email updated successfully." Redirect to `/profile`.
- On 422 (expired/used token): "This link has expired. Please request a new verification email."

---

## 4. Route Architecture

### 4.1 Route Table

| Route | Page | Layout | Roles Allowed |
|---|---|---|---|
| `/login` | `LoginPage` | AuthLayout | Public (redirect if authenticated) |
| `/register` | `RegisterPage` | AuthLayout | Public (redirect if authenticated) |
| `/verify-email` | `VerifyEmailPage` | AuthLayout | Public |
| `/dashboard` | `DashboardPage` | AppLayout | All authenticated |
| `/profile` | `ProfilePage` | AppLayout | All authenticated |
| `/events` | `EventsPage` | AppLayout | All authenticated |
| `/events/create` | `CreateEventPage` | AppLayout | ADMIN, DC, NC |
| `/events/:id` | `EventDetailPage` | AppLayout | All authenticated |
| `/documents` | `DocumentsPage` | AppLayout | All authenticated |
| `/documents/approval` | `DocumentApprovalPage` | AppLayout | ADMIN, DC |
| `/map` | `MapPage` | AppLayout | All authenticated |
| `/simulations` | `SimulationTriggerPage` | AppLayout | ADMIN |
| `/simulations/:id` | `SimulationDetailPage` | AppLayout | ADMIN |
| `/admin/users` | `UsersPage` | AppLayout | ADMIN |
| `/admin/districts` | `DistrictsPage` | AppLayout | ADMIN |
| `/unauthorized` | `UnauthorizedPage` | AppLayout | All authenticated |
| `*` | `NotFoundPage` | AppLayout | All authenticated |

---

### 4.2 Protected Route Logic

```tsx
// router/ProtectedRoute.tsx
function ProtectedRoute({ allowedRoles }: { allowedRoles?: UserRole[] }) {
  const { user, accessToken } = useAuth();
  if (!accessToken) return <Navigate to="/login" replace />;
  if (allowedRoles && !allowedRoles.includes(user.role))
    return <Navigate to="/unauthorized" replace />;
  return <Outlet />;
}
```

---

### 4.3 Role-Based Navigation

The `SideNav` component conditionally renders menu items:

| Menu Item | ADMIN | DC | NC | VOLUNTEER |
|---|---|---|---|---|
| Dashboard | ✅ | ✅ | ✅ | ✅ |
| Events | ✅ | ✅ | ✅ | ✅ |
| My Documents | ✅ | ✅ | ✅ | ✅ |
| Document Approval | ✅ | ✅ | ❌ | ❌ |
| Risk Map | ✅ | ✅ | ✅ | ✅ |
| Simulations | ✅ | ❌ | ❌ | ❌ |
| User Management | ✅ | ❌ | ❌ | ❌ |
| District Management | ✅ | ❌ | ❌ | ❌ |

---

## 5. Page Specifications

### 5.1 Login Page

**Purpose:** Authenticate users and obtain JWT token pair.

**API endpoints:**
- `POST /auth/login`

**Form fields & validation:**

| Field | Type | Validation |
|---|---|---|
| email | text | Required, valid email format |
| password | text | Required, min 8 chars |

**User actions:** Submit login form, navigate to Register page.

**Error handling:**
- 401 → "Invalid email or password."
- 403 → "Account is deactivated."
- Network error → "Unable to connect. Check your connection."

**Loading state:** Button shows spinner + "Signing in…" during request.

---

### 5.2 Register Page

**Purpose:** Create a new VOLUNTEER account.

**API endpoints:**
- `GET /districts` (populate district dropdown)
- `GET /districts/{id}/neighborhoods` (populate neighborhood dropdown on district select)
- `POST /auth/register`

**Form fields & validation:**

| Field | Type | Validation |
|---|---|---|
| firstName | text | Required, max 100 chars |
| lastName | text | Required, max 100 chars |
| email | text | Required, valid email |
| phone | text | Required, format +90XXXXXXXXXX |
| bloodType | select | Required, one of 8 values |
| districtId | select | Required |
| neighborhoodId | select | Required; resets when districtId changes |
| address | textarea | Required |
| profession | text | Optional |
| password | password | Required, min 8 chars, mixed case + digit |
| confirmPassword | password | Must match password |

**User actions:** Submit registration, navigate to Login.

**Error handling:**
- 409 on email → field error: "Email already registered."
- 409 on phone → field error: "Phone number already registered."
- 422 → "Selected neighborhood does not belong to the selected district."

---

### 5.3 Dashboard Page

Detailed in [Section 6](#6-dashboard-design).

---

### 5.4 Profile Page

**Purpose:** View and update own profile; change email; change password.

**API endpoints:**
- `GET /users/me`
- `PATCH /users/me`
- `POST /users/me/change-email`
- `POST /users/me/change-password`

**Sections:**

**Profile Info tab:** Pre-filled form with all editable profile fields (same fields as register minus password). Submit → `PATCH /users/me`.

**Email tab:** Two fields: newEmail, currentPassword. Submit → `POST /users/me/change-email`. Shows "Verification email sent" info message on 202.

**Password tab:** Three fields: currentPassword, newPassword, confirmNewPassword. Submit → `POST /users/me/change-password`. On 204: success toast + force logout.

**Read-only display:** role badge, emailVerified status, account creation date.

**Error handling:** All field-level errors surfaced in-form. 401 on wrong password shown inline.

---

### 5.5 Events List Page

**Purpose:** Browse events according to role scope.

**API endpoints:**
- `GET /events` (with query params: status, neighborhoodId, teamId, urgency, page, size)

**Filters (shown in a filter bar):**
- Status: OPEN / CLOSED / All
- Team: multi-select from 6 teams
- Urgency: 1–5 slider or checkboxes
- Neighborhood: text search (for ADMIN/DC)

**Table columns:**

| Column | Type |
|---|---|
| Title | Link to `/events/:id` |
| Status | `EventStatusBadge` (green=OPEN, grey=CLOSED) |
| Urgency | Stars or numeric badge (1–5) |
| Team | Team name |
| Neighborhood | Name, district |
| Risk Score | Numeric + risk color dot |
| Starts At | Formatted date |
| Actions | "View" button; "Join" for VOLUNTEER (if OPEN) |

**Role differences:**
- VOLUNTEER: only sees OPEN events; no filter on status.
- NC: filtered to own neighborhood by default.
- DC: filtered to own district by default.
- ADMIN: no filter default.

**Loading state:** Skeleton rows (5 rows) during initial fetch.

**Empty state:** "No events found. Try adjusting your filters."

---

### 5.6 Event Detail Page

**Purpose:** Full event view with volunteer list (for coordinators).

**API endpoints:**
- `GET /events/:id`
- `GET /events/:id/volunteers` (ADMIN, DC, NC only)
- `POST /events/:id/join` (VOLUNTEER)
- `DELETE /events/:id/leave` (VOLUNTEER)
- `POST /events/:id/close` (ADMIN, DC, NC)
- `PATCH /events/:id` (ADMIN, DC, NC; edit mode)

**Sections:**
- **Header:** title, status badge, urgency, risk score.
- **Details card:** description, team, neighborhood, coordinates (with a small Leaflet mini-map pin if lat/lon exist), startsAt, endsAt, closedAt, requiredPeople, createdBy.
- **Action buttons (role-gated):**
  - VOLUNTEER + OPEN: "Join Event" (disabled if already joined; "Leave Event" if already in).
  - NC/DC/ADMIN + OPEN: "Edit", "Close Event".
  - Closed state: all action buttons hidden.
- **Volunteers panel** (DC/NC/ADMIN only): paginated table of assigned volunteers with status badges.

**Confirmation dialogs:**
- "Join Event" → ConfirmationDialog: "Confirm joining [team name] event?"
- "Close Event" → ConfirmationDialog: "Closing this event is irreversible. Proceed?"
- "Leave Event" → ConfirmationDialog: "Are you sure you want to withdraw?"

**Error handling:**
- 422 "Volunteer not a member of this team" → toast: "You must be a member of [team] to join this event."
- 409 "Already joined" → toast: "You are already assigned to this event."
- 409 "Already closed" → toast: "This event has already been closed."

---

### 5.7 Create Event Page

**Purpose:** Create a new disaster event.

**API endpoints:**
- `GET /districts` (for ADMIN to populate neighborhood picker)
- `GET /districts/{id}/neighborhoods`
- `GET /teams`
- `POST /events`

**Form fields & validation:**

| Field | Type | Validation |
|---|---|---|
| title | text | Required, max 300 chars |
| description | textarea | Optional |
| neighborhoodId | select | Required; scoped to own neighborhood (NC) or own district (DC) or all (ADMIN) |
| teamId | select | Required; shows all 6 teams |
| urgency | number (1–5) | Required; rendered as a slider or segmented control |
| requiredPeople | number | Required, min 1 |
| latitude | number | Optional, decimal |
| longitude | number | Optional, decimal |
| startsAt | datetime-local | Optional |
| endsAt | datetime-local | Optional; must be ≥ startsAt |

**Access:** ADMIN, DC, NC only. VOLUNTEER is redirected from this route.

**Post-submit:** Navigate to `/events/:id` of newly created event. Show success toast.

---

### 5.8 Documents List Page

**Purpose:** View own uploaded documents; upload new ones.

**API endpoints:**
- `GET /users/me/documents`
- `POST /users/me/documents` (multipart upload)
- `GET /documents/:id/download`

**Document table columns:**

| Column | |
|---|---|
| Type | SEARCH_RESCUE_CERTIFICATE / PSYCHOSOCIAL_GRADUATION_DOCUMENT |
| Status | `DocumentStatusBadge` (PENDING=yellow, APPROVED=green, REJECTED=red) |
| File name | Text |
| Uploaded | Formatted date |
| Actions | "Download" button |

**Upload section (VOLUNTEER only):**
- `documentType` select (2 options).
- File input — accepts `.pdf`, `.jpg`, `.jpeg`, `.png`; max 10 MB enforced client-side.
- Progress bar during upload.
- On 201: new document appears at top of list with PENDING status.

**Rejection reason:** If status is REJECTED, display the `rejectionReason` in a red callout below the row on expand/accordion.

---

### 5.9 Document Approval Page

**Purpose:** Review and approve or reject pending documents.

**API endpoints:**
- `GET /admin/documents/pending`
- `GET /documents/:id/download`
- `PATCH /documents/:id/approve`
- `PATCH /documents/:id/reject`

**Access:** ADMIN, DC.

**Filter bar:** documentType select, districtId select (ADMIN only).

**Table columns:**

| Column | |
|---|---|
| Document type | |
| Owner name | Links to user profile (ADMIN only) |
| Owner district | |
| File name | "Preview/Download" button opens presigned URL |
| Uploaded | Date |
| Actions | "Approve" (green) · "Reject" (red) buttons |

**Reject action:**
Opens a modal with a required textarea ("Rejection reason") and confirms before calling `PATCH /documents/:id/reject`.

**After approve/reject:** Row disappears from the list (query invalidated). Success toast shown.

---

### 5.10 Risk Map Page

Detailed in [Section 9](#9-risk-map-interface).

---

### 5.11 Simulation Trigger Page

Detailed in [Section 10](#10-simulation-interface).

---

### 5.12 User Management Page (Admin)

**Purpose:** View all users, change roles, deactivate accounts.

**API endpoints:**
- `GET /admin/users`
- `PATCH /admin/users/:id/role`
- `PATCH /admin/users/:id/deactivate`

**Filter bar:** role select, districtId select, isActive toggle.

**Table columns:**

| Column | |
|---|---|
| Name | |
| Email | |
| Role | Badge with color |
| District | |
| Status | Active / Inactive badge |
| Email Verified | ✅ / ❌ |
| Actions | "Edit Role" · "Deactivate" |

**Edit Role modal:**
- Role select: VOLUNTEER, NEIGHBORHOOD_COORDINATOR, DISTRICT_COORDINATOR.
- If NC or DC selected: district select appears.
- Validation: email must be verified, target must be active.
- On submit: `PATCH /admin/users/:id/role`. On success: row updates with new role badge.

**Deactivate:** ConfirmationDialog → `PATCH /admin/users/:id/deactivate`. On 204: user's status badge changes to Inactive.

---

### 5.13 District Management Page (Admin)

**Purpose:** View district list, assign district coordinators.

**API endpoints:**
- `GET /districts`
- `GET /admin/users?role=VOLUNTEER&districtId=...`
- `PATCH /admin/districts/:id/coordinator`

**Layout:** Card grid of 10 districts, each showing name, risk score with risk-color badge, and current coordinator (or "Unassigned").

**Assign coordinator:** Button on each card opens a modal:
- User search/select filtered to volunteers in that district.
- On confirm: `PATCH /admin/districts/:id/coordinator`.
- On success: card updates with new coordinator name.

---

## 6. Dashboard Design

### 6.1 Widget Definitions

**Widget: Active Events Count**
- Shows total count of OPEN events in the user's scope.
- Links to `/events?status=OPEN`.
- Visible to: all roles.

**Widget: Events Needing Volunteers**
- OPEN events where `assignedVolunteers < requiredPeople`.
- Shows count + shortlist of top 3, each a link to the event.
- Visible to: VOLUNTEER, NC, DC, ADMIN.

**Widget: My District Risk Level** (DC/NC)
- Shows the user's district name, current `riskScore`, and `riskColor` as a large colored badge (RED / YELLOW / GREEN).
- Links to `/map`.
- Visible to: DC, NC.

**Widget: Overall Risk Summary** (ADMIN)
- A mini bar chart or table of all 10 districts and their risk colors.
- Links to `/map`.
- Visible to: ADMIN only.

**Widget: My Team Memberships** (VOLUNTEER)
- Lists the teams the volunteer has joined.
- Each team links to the relevant events.
- Shows "Join a Team" CTA if no memberships exist.
- Visible to: VOLUNTEER only.

**Widget: Recent Simulations** (ADMIN)
- Lists the last 5 earthquake simulations with status badge (COMPLETED / PARTIAL_FAILURE).
- Links to `/simulations/:id`.
- Visible to: ADMIN only.

**Widget: Pending Document Reviews**
- Count of PENDING documents awaiting review.
- Links to `/documents/approval`.
- Visible to: ADMIN, DC.

**Widget: My Documents Status** (VOLUNTEER)
- Shows each uploaded document with its current status badge.
- Prompts "Upload document" CTA if no documents exist.
- Visible to: VOLUNTEER.

---

### 6.2 Role → Widget Matrix

| Widget | ADMIN | DC | NC | VOLUNTEER |
|---|---|---|---|---|
| Active Events Count | ✅ | ✅ | ✅ | ✅ |
| Events Needing Volunteers | ✅ | ✅ | ✅ | ✅ |
| District Risk Level | ❌ | ✅ | ✅ | ❌ |
| Overall Risk Summary | ✅ | ❌ | ❌ | ❌ |
| My Team Memberships | ❌ | ❌ | ❌ | ✅ |
| Recent Simulations | ✅ | ❌ | ❌ | ❌ |
| Pending Document Reviews | ✅ | ✅ | ❌ | ❌ |
| My Documents Status | ❌ | ❌ | ❌ | ✅ |

---

### 6.3 Dashboard Layout

- **2-column grid** on desktop (lg+): primary widget (large) on the left, secondary widgets stacked on the right.
- **Single column** on mobile.
- Widgets are cards with header, metric, and a CTA link.
- All data is fetched in parallel via TanStack Query on mount; each widget shows its own `LoadingSpinner` independently.

---

## 7. Event UI Flow

### 7.1 Create Event Flow

```
NC/DC/ADMIN clicks "Create Event"
  → Navigate to /events/create
  → Fill form:
       neighborhoodId (scoped to role)
       teamId (all 6 teams)
       title, description, urgency (1–5), requiredPeople
       optional: lat/lon, startsAt, endsAt
  → Submit → POST /events
  → On 201: Navigate to /events/:newId with success toast
  → Backend risk score computed: team.coeff × urgency × requiredPeople × 1.0
```

Role-specific UI differences:
- **NC:** `neighborhoodId` is pre-filled and read-only (own neighborhood).
- **DC:** `neighborhoodId` is a select showing only neighborhoods in their district.
- **ADMIN:** Full district + neighborhood cascade selects.

---

### 7.2 View Event Flow

```
User clicks event in list or enters /events/:id
  → GET /events/:id
  → Render event details
  → If ADMIN/DC/NC: also fetch GET /events/:id/volunteers (paginated table below)
  → Risk score, urgency, and team coefficient displayed in info card
```

---

### 7.3 Join Event Flow (VOLUNTEER only)

```
Volunteer views OPEN event detail
  → "Join Event" button visible (disabled if already joined)
  → Click → ConfirmationDialog: "Join [title]?"
  → Confirm → POST /events/:id/join
  → On 201: Button changes to "Leave Event". Success toast.
  → On 422 "not a team member": Toast: "You must join the [team] team first."
  → On 409 "already joined": Button already shows "Leave Event" (UI prevented duplicate click)
```

---

### 7.4 Leave Event Flow (VOLUNTEER only)

```
Volunteer views event they have joined
  → "Leave Event" button visible
  → Click → ConfirmationDialog: "Withdraw from [title]?"
  → Confirm → DELETE /events/:id/leave
  → On 204: Button reverts to "Join Event". Success toast.
  → On 422 "event closed": Toast: "You cannot withdraw from a closed event."
```

---

### 7.5 Close Event Flow (ADMIN / DC / NC)

```
Coordinator views OPEN event
  → "Close Event" button visible (not shown if already CLOSED)
  → Click → ConfirmationDialog:
       "Close this event? This action is irreversible.
        All assigned volunteers will be marked as COMPLETED."
  → Confirm → POST /events/:id/close
  → On 200: Status badge changes to CLOSED. closedAt timestamp displayed.
             Risk score recalculates (new value shown).
             Volunteer table: all ASSIGNED → COMPLETED.
             "Close Event" button disappears.
  → On 409: Toast: "Event is already closed."
```

---

### 7.6 Edit Event Flow (ADMIN / DC / NC)

```
Coordinator views OPEN event
  → Click "Edit" → form fields become editable inline (or modal form)
  → PATCH /events/:id
  → On 200: Info card refreshes. Risk score updates if urgency/requiredPeople changed.
  → On 422 "event is CLOSED": Toast: "Closed events cannot be edited."
```

---

## 8. Document Upload & Approval

### 8.1 Volunteer Upload Flow

**Step 1 — Select type:**
Volunteer selects `documentType` from a dropdown:
- "Arama-Kurtarma Sertifikası" → `SEARCH_RESCUE_CERTIFICATE`
- "Psikososyal Destek Mezuniyet Belgesi" → `PSYCHOSOCIAL_GRADUATION_DOCUMENT`

**Step 2 — Select file:**
- File input restricted to `.pdf`, `.jpg`, `.jpeg`, `.png`.
- Client-side size check: if `file.size > 10 * 1024 * 1024`, show error before upload: "File exceeds 10 MB limit."
- File name and size preview shown below input.

**Step 3 — Upload:**
- Submit → `POST /users/me/documents` (multipart/form-data).
- Progress bar tracks upload `onUploadProgress`.
- On 201: New document row added to table with `PENDING` status badge and yellow indicator.

**UI States:**

| State | UI |
|---|---|
| PENDING | Yellow badge "Awaiting Review" |
| APPROVED | Green badge "Approved" ✅ |
| REJECTED | Red badge "Rejected" + expandable rejection reason |

**Re-upload after rejection:** A fresh upload of the same document type is allowed. The previous REJECTED record remains visible in history.

---

### 8.2 Admin / DC Review Flow

**Step 1 — List pending documents:**
`GET /admin/documents/pending` renders a table of all PENDING documents.

**Step 2 — Preview document:**
"Preview" button → `GET /documents/:id/download` → open the `presignedUrl` in a new tab. The presigned URL expires in 15 minutes.

**Step 3 — Approve:**
- Click "Approve" (green button).
- No additional input required.
- `PATCH /documents/:id/approve`.
- On 200: Row removed from pending list. Success toast: "Document approved."

**Step 4 — Reject:**
- Click "Reject" (red button).
- Modal opens: textarea for rejection reason (required, cannot submit blank).
- "Confirm Rejection" → `PATCH /documents/:id/reject` with `{ reason }`.
- On 200: Row removed from pending list. Success toast: "Document rejected."
- On 400 (blank reason): Error shown inside modal.
- On 409 (already reviewed): Toast: "This document has already been reviewed."

**Scope enforcement:**
- DC sees only documents from users in their district.
- ADMIN sees all. District filter available to narrow list.

---

## 9. Risk Map Interface

### 9.1 Overview

The map page is the primary situational awareness view. It renders all 10 Istanbul pilot districts as GeoJSON polygons, color-coded by risk level. Clicking a district drills down to show its neighborhood polygons and event pin markers.

---

### 9.2 Map Initialization

```
MapPage mounts
  → GET /map/districts           → DistrictLayer (colored polygons)
  → GET /events?status=OPEN      → EventMarker (pins on map)
  → Leaflet map centered on Istanbul (lat: 41.015, lon: 28.979, zoom: 11)
```

The map always initializes with the district layer loaded. Neighborhood drill-down is on-demand when a user clicks a district.

---

### 9.3 District Layer (`DistrictLayer`)

- Source: `GET /map/districts`
- Each district is a GeoJSON `Feature` with a `Polygon` geometry.
- Fill color derived from `riskColor`:

| riskColor | Fill | Opacity |
|---|---|---|
| `RED` | `#E53935` | 0.55 |
| `YELLOW` | `#FDD835` | 0.45 |
| `GREEN` | `#43A047` | 0.35 |

- Hover: border thickens; tooltip shows `{districtName} — Risk: {riskScore}`.
- Click: fires `onDistrictClick(districtId)` → loads the neighborhood layer for that district.

---

### 9.4 Neighborhood Drill-Down (`NeighborhoodLayer`)

- Triggered when the user clicks a district polygon.
- Source: `GET /map/districts/{districtId}/neighborhoods` (lazy-fetched on click).
- Neighborhoods render as smaller polygons inside the selected district, also color-coded by `riskColor`.
- Hover tooltip: `{neighborhoodName} — Risk: {riskScore}`.
- Click: opens a right-side panel showing:
  - Neighborhood name, risk score, coordinator name.
  - List of OPEN events in that neighborhood (each a link to `/events/:id`).
  - Assembly areas with name, capacity, and Google Maps deeplinks (`https://www.google.com/maps?q={lat},{lon}`).

A "Back to all districts" button resets the view to the district layer.

---

### 9.5 Event Markers (`EventMarker`)

- Source: `GET /events?status=OPEN` — only events that have both `latitude` and `longitude`.
- Each marker is a pin color-coded by urgency:
  - Urgency 5 → red pin
  - Urgency 3–4 → orange pin
  - Urgency 1–2 → blue pin
- Click marker → Leaflet popup showing: event title, team, urgency, required people, and a "View Event" link to `/events/:id`.

---

### 9.6 Risk Legend (`RiskLegend`)

A fixed overlay in the bottom-right corner of the map:

```
● RED     ≥ 70   High Risk
● YELLOW  40–69  Medium Risk
● GREEN   < 40   Low Risk
```

---

### 9.7 Map Controls

| Control | Description |
|---|---|
| Zoom in/out | Standard Leaflet zoom controls |
| Layer toggle | Toggle district / neighborhood / event layers on/off |
| Refresh | Re-fetches `GET /map/districts` for latest risk scores |
| Fullscreen | Leaflet fullscreen plugin |

---

### 9.8 API Call Sequence

```
1. GET /map/districts                           → district polygons (on mount)
2. GET /events?status=OPEN                      → event markers (on mount, parallel)
3. GET /map/districts/:id/neighborhoods         → neighborhood polygons (on district click)
```

All map data is cached via TanStack Query with `staleTime: 30_000` ms. Caches are invalidated when events are created, updated, or closed.

---

## 10. Simulation Interface

### 10.1 Admin-Only Access

Both simulation pages are restricted to `ADMIN` role. Any other role navigating to `/simulations` is redirected to `/unauthorized`.

---

### 10.2 Simulation Trigger Page (`/simulations`)

**Purpose:** Trigger an earthquake simulation and view history.

**API endpoints used:**
- `GET /districts` — populate district select
- `POST /admin/simulations` — trigger simulation
- `GET /admin/simulations` — history list

**Form fields & validation:**

| Field | Type | Validation |
|---|---|---|
| districtId | select | Required; all 10 active districts |
| magnitude | number | Required; 0.1–10.0, step 0.1 |
| notes | textarea | Optional |

**Post-submit (202 Accepted):**
- UI shows: "Simulation queued. Notifying {totalUsersToNotify} users."
- "View Progress" link navigates to `/simulations/{simulationId}`.
- History table below the form auto-refreshes every 10 seconds while any row has `PROCESSING` status.

**Simulation history table columns:**

| Column | |
|---|---|
| District | Name |
| Magnitude | Numeric value |
| Status | QUEUED / PROCESSING / COMPLETED / PARTIAL_FAILURE badge |
| Users Notified | `sent / total` |
| Triggered At | Formatted timestamp |
| Actions | "View Logs" → `/simulations/:id` |

---

### 10.3 Simulation Detail Page (`/simulations/:id`)

**Purpose:** Monitor email delivery progress for a specific simulation.

**API endpoints used:**
- `GET /admin/simulations/:id`
- `GET /admin/simulations/:id/logs`

**Header:** district name, magnitude, triggered at, created by admin.

**Status display:**
- `QUEUED` → "Waiting to start…"
- `PROCESSING` → progress bar `(sent / total)%`; page auto-refreshes every 5 seconds.
- `COMPLETED` → "All emails delivered successfully."
- `PARTIAL_FAILURE` → "Some emails failed. See failure log below."

**Delivery stats cards (4 counters):**

| Metric | Value |
|---|---|
| Total Queued | `totalQueued` |
| Sent | `totalSent` (green) |
| Failed | `totalFailed` (red) |
| Success Rate | `(totalSent / totalQueued × 100).toFixed(1)%` |

**Notification log table:**

| Column | |
|---|---|
| Email Address | Snapshot at send time |
| Status | QUEUED / SENT / FAILED / BOUNCED badge |
| Retry Count | 0–3 |
| Last Error | Truncated; expandable on click |
| Sent At | Timestamp or "—" |

Filterable by status (FAILED, BOUNCED). Paginated (20/page).

---

## 11. API Client Architecture

### 11.1 Axios Instance

A single configured instance lives in `src/api/axiosInstance.ts`:

```ts
const instance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL + '/api/v1',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});
```

This is the only HTTP client used in the app. All service functions import from it.

---

### 11.2 Request Interceptor

Attaches the Bearer token from `authStore` to every outgoing request:

```ts
instance.interceptors.request.use(config => {
  const token = authStore.getState().accessToken;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
```

---

### 11.3 Response Interceptor — Token Refresh

A shared `refreshPromise` prevents parallel refresh calls when multiple requests 401 simultaneously:

```ts
let refreshPromise: Promise<string> | null = null;

instance.interceptors.response.use(
  res => res,
  async (error: AxiosError) => {
    const original = error.config as AxiosRequestConfig & { _retry?: boolean };
    if (error.response?.status === 401 && !original._retry) {
      original._retry = true;
      if (!refreshPromise) {
        refreshPromise = doTokenRefresh().finally(() => { refreshPromise = null; });
      }
      await refreshPromise;
      return instance(original); // retry with new token
    }
    return Promise.reject(parseApiError(error));
  }
);
```

`doTokenRefresh()` calls `POST /auth/refresh`, updates `authStore`, rotates `sessionStorage`. On failure, it clears state and redirects to `/login`.

---

### 11.4 Error Parsing

`parseApiError(error)` converts the backend `ErrorResponse` envelope into a typed frontend error:

```ts
interface ApiError {
  status: number;
  code: string;           // e.g. "VALIDATION_ERROR", "BUSINESS_RULE_VIOLATION"
  message: string;
  details?: FieldError[]; // field-level errors from 400 responses
}
```

Consumed by `useToast()` for global errors and by React Hook Form's `setError` for field-level errors.

---

### 11.5 API Service Organization

Each domain has a typed service file:

```ts
// api/events.api.ts
export const getEvents = (params: EventFilterParams) =>
  instance.get<PagedResponse<EventSummaryResponse>>('/events', { params }).then(r => r.data);

export const createEvent = (body: CreateEventRequest) =>
  instance.post<EventResponse>('/events', body).then(r => r.data);

export const closeEvent = (eventId: string) =>
  instance.post<EventCloseResponse>(`/events/${eventId}/close`).then(r => r.data);

export const joinEvent = (eventId: string) =>
  instance.post<EventJoinResponse>(`/events/${eventId}/join`).then(r => r.data);

export const leaveEvent = (eventId: string) =>
  instance.delete(`/events/${eventId}/leave`);
```

All functions return typed data directly (`.then(r => r.data)`). No component imports raw `AxiosResponse`.

---

### 11.6 Multipart Upload

Document uploads use `FormData` with upload progress tracking:

```ts
export const uploadDocument = (
  type: DocumentType,
  file: File,
  onProgress: (pct: number) => void
) => {
  const form = new FormData();
  form.append('documentType', type);
  form.append('file', file);
  return instance.post<DocumentResponse>('/users/me/documents', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: e =>
      onProgress(Math.round((e.loaded / (e.total ?? 1)) * 100)),
  }).then(r => r.data);
};
```

---

## 12. State Management Strategy

### 12.1 Two-Layer State Model

| Layer | Tool | What it stores |
|---|---|---|
| Server state | TanStack Query | All API data (events, users, districts, documents, simulations) |
| Client state | Zustand (`authStore`) | `accessToken`, decoded user summary (id, role, districtId, neighborhoodId, email) |

No Redux. No React Context for data. These two tools handle all state.

---

### 12.2 TanStack Query Configuration

```ts
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,        // 30s before background refetch
      retry: 1,
      refetchOnWindowFocus: true,
    },
  },
});
```

**Standard fetch pattern:**
```ts
const { data, isLoading, error } = useQuery({
  queryKey: queryKeys.events.list(filters),
  queryFn: () => getEvents(filters),
});
```

**Mutation with invalidation:**
```ts
const mutation = useMutation({
  mutationFn: (id: string) => closeEvent(id),
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: queryKeys.events.all });
    queryClient.invalidateQueries({ queryKey: queryKeys.districts.all });
    toast.success('Event closed.');
  },
});
```

---

### 12.3 Query Key Factory (`utils/queryKeys.ts`)

Centralized key factory prevents hardcoded strings and enables precise invalidation:

```ts
export const queryKeys = {
  events: {
    all: ['events'] as const,
    list: (f: EventFilterParams) => ['events', 'list', f] as const,
    detail: (id: string) => ['events', id] as const,
    volunteers: (id: string) => ['events', id, 'volunteers'] as const,
  },
  districts: {
    all: ['districts'] as const,
    detail: (id: string) => ['districts', id] as const,
    neighborhoods: (id: string) => ['districts', id, 'neighborhoods'] as const,
  },
  documents: {
    mine: () => ['documents', 'mine'] as const,
    pending: (f: PendingDocFilters) => ['documents', 'pending', f] as const,
  },
  simulations: {
    all: ['simulations'] as const,
    detail: (id: string) => ['simulations', id] as const,
    logs: (id: string, s?: string) => ['simulations', id, 'logs', s] as const,
  },
  teams: {
    all: ['teams'] as const,
    members: (id: string) => ['teams', id, 'members'] as const,
  },
  map: {
    districts: () => ['map', 'districts'] as const,
    neighborhoods: (districtId: string) => ['map', 'neighborhoods', districtId] as const,
  },
};
```

---

### 12.4 Cache Invalidation Rules

| User action | Caches invalidated |
|---|---|
| Create event | `events.all`, `districts.all` |
| Update event | `events.detail(id)`, `events.all`, `districts.all` |
| Close event | `events.detail(id)`, `events.all`, `districts.all` |
| Join / leave event | `events.detail(id)` |
| Upload document | `documents.mine` |
| Approve / reject document | `documents.pending`, `documents.mine` |
| Assign role | `users.list`, `districts.all` |
| Deactivate user | `users.list` |
| Trigger simulation | `simulations.all` |

---

### 12.5 Optimistic Updates

Applied to event join/leave for instant UI feedback:

```ts
useMutation({
  mutationFn: joinEvent,
  onMutate: async (eventId) => {
    await queryClient.cancelQueries({ queryKey: queryKeys.events.detail(eventId) });
    const prev = queryClient.getQueryData(queryKeys.events.detail(eventId));
    queryClient.setQueryData(queryKeys.events.detail(eventId), (old: EventResponse) => ({
      ...old, assignedVolunteers: old.assignedVolunteers + 1,
    }));
    return { prev };
  },
  onError: (_err, eventId, ctx) => {
    queryClient.setQueryData(queryKeys.events.detail(eventId), ctx?.prev);
  },
  onSettled: (_data, _err, eventId) => {
    queryClient.invalidateQueries({ queryKey: queryKeys.events.detail(eventId) });
  },
});
```

---

## 13. UI Component Library

All reusable primitives live in `src/components/ui/`. They are purely presentational — no API calls or business logic.

---

### `Button`

```tsx
interface ButtonProps {
  variant: 'primary' | 'secondary' | 'danger' | 'ghost';
  size?: 'sm' | 'md' | 'lg';
  loading?: boolean;
  disabled?: boolean;
  leftIcon?: ReactNode;
  type?: 'button' | 'submit' | 'reset';
  onClick?: () => void;
  children: ReactNode;
}
```

- `primary` — solid blue, white text (default).
- `secondary` — white bg, blue border.
- `danger` — red bg; used for Reject, Deactivate, Close Event.
- `ghost` — transparent; used for Cancel links.
- `loading=true` — renders `LoadingSpinner` inline; button disabled.

---

### `FormField`

Wraps label + input/select/textarea + error message in a consistent layout:

```tsx
interface FormFieldProps {
  label: string;
  error?: string;
  required?: boolean;
  hint?: string;
  children: ReactElement;
}
```

Error text renders in red below the input. Used with React Hook Form's `register` + `formState.errors`.

---

### `Modal`

```tsx
interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  size?: 'sm' | 'md' | 'lg';
  footer?: ReactNode;
  children: ReactNode;
}
```

- Mounted via `createPortal` to `document.body`.
- Backdrop click + Escape key trigger `onClose`.
- `footer` slot for action buttons.
- Focus trapped for accessibility.

---

### `DataTable`

Generic paginated table component:

```tsx
interface DataTableProps<T> {
  columns: ColumnDef<T>[];
  data: T[];
  isLoading?: boolean;
  emptyMessage?: string;
  pagination?: PaginationMeta;
  onPageChange?: (page: number) => void;
}
```

- Shows skeleton rows during loading (5 placeholder rows).
- Shows `EmptyState` when `data.length === 0`.
- `ColumnDef` supports a `render` function for custom cell content.

---

### `Toast`

Imperative toast system with a `useToast()` hook. Renders stacked notifications in the top-right corner.

```ts
const { toast } = useToast();
toast.success('Event created.');
toast.error('Something went wrong.');
toast.info('Verification email sent.');
toast.warning('This action is irreversible.');
```

- Auto-dismisses after 4 seconds.
- Clickable to dismiss manually.
- Maximum 5 simultaneous toasts; oldest is dismissed when limit is reached.

---

### `ConfirmationDialog`

A specialized modal for destructive actions:

```tsx
interface ConfirmationDialogProps {
  isOpen: boolean;
  title: string;
  message: string;
  confirmLabel?: string;       // default: "Confirm"
  confirmVariant?: 'primary' | 'danger';
  onConfirm: () => void;
  onCancel: () => void;
  isLoading?: boolean;
}
```

Used for: close event, leave event, reject document, deactivate user.

---

### `LoadingSpinner`

```tsx
interface LoadingSpinnerProps {
  size?: 'sm' | 'md' | 'lg';
  label?: string;  // accessible screen-reader label
}
```

Animated CSS spinner. Used inline in buttons, as centered page loaders, and inside dashboard widget cards.

---

### `Badge`

```tsx
interface BadgeProps {
  variant: 'success' | 'warning' | 'danger' | 'neutral' | 'info';
  children: ReactNode;
}
```

Color mapping:
| variant | Color | Used for |
|---|---|---|
| `success` | Green | OPEN, APPROVED, COMPLETED, Active |
| `warning` | Yellow | PENDING, PROCESSING |
| `danger` | Red | REJECTED, PARTIAL_FAILURE, CLOSED, Inactive |
| `neutral` | Gray | WITHDRAWN, QUEUED |
| `info` | Blue | Role labels, generic info |

---

### `Pagination`

```tsx
interface PaginationProps {
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}
```

Renders Prev / numbered pages / Next controls. Prev disabled on page 0; Next disabled on last page.

---

### `EmptyState`

```tsx
interface EmptyStateProps {
  icon?: ReactNode;
  title: string;
  description?: string;
  action?: { label: string; onClick: () => void };
}
```

Displayed in `DataTable` and list pages when the API returns no results.

---

### Design Consistency Rules

1. **Spacing:** Only Tailwind's default scale. No arbitrary values (`p-[13px]` is forbidden).
2. **Typography:** `Inter` font (Google Fonts). Use `text-sm` / `text-base` / `text-lg` / `text-xl` for hierarchy.
3. **Color:** Only the defined palette — blues for primary actions, reds for danger, and the three risk colors. No inline hex values in component files.
4. **Radius:** `rounded-md` for inputs and buttons; `rounded-lg` for cards and modals.
5. **Shadow:** `shadow-sm` for cards; `shadow-lg` for modals and dropdowns.
6. **Interactive states:** Every button/input/link must have `hover:`, `focus:`, and `disabled:` variants.

---

## 14. Frontend Environment Configuration

### 14.1 Environment Files

| File | Used by | Purpose |
|---|---|---|
| `.env` | All | Shared defaults (committed, no secrets) |
| `.env.development` | `vite dev` | Local dev overrides |
| `.env.production` | `vite build` | Production values |
| `.env.local` | Developer machine | Personal overrides (gitignored) |

---

### 14.2 Environment Variables

All variables must be prefixed with `VITE_` to be accessible in the browser bundle.

| Variable | Dev value | Prod value | Description |
|---|---|---|---|
| `VITE_API_BASE_URL` | `http://localhost:8080` | `https://api.afetkoordinasyon.istanbul` | Backend API base URL |
| `VITE_MAP_TILE_URL` | `https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png` | Same | Leaflet tile server |
| `VITE_MAP_ATTRIBUTION` | `© OpenStreetMap contributors` | Same | Leaflet attribution text |
| `VITE_APP_TITLE` | `Afet Koordinasyon [DEV]` | `Afet Koordinasyon Platformu` | Browser tab title |
| `VITE_ENABLE_DEVTOOLS` | `true` | `false` | React Query Devtools panel |
| `VITE_SESSION_STORAGE_KEY` | `rt` | `rt` | Key for refresh token in sessionStorage |

---

### 14.3 Dev Server Proxy

The Vite dev server proxies all `/api` traffic to the Spring Boot backend to avoid CORS issues during development:

```ts
// vite.config.ts
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
  },
},
```

This means `VITE_API_BASE_URL` in `.env.development` can be set to an empty string or kept as `http://localhost:8080` — both work with the proxy.

---

### 14.4 Production Build

```bash
vite build          # outputs to dist/
vite preview        # locally previews the production bundle
```

The `dist/` directory is a fully static bundle suitable for deployment to: Nginx, Vercel, Netlify, AWS S3 + CloudFront, or any static file host.

---

## 15. Development Phases

### Phase 1 — Project Scaffold

- Initialize project: `npm create vite@latest frontend -- --template react-ts`
- Install: TanStack Query, Axios, React Router, React Hook Form, Zod, Leaflet, react-leaflet, Zustand, Tailwind CSS, `@hookform/resolvers`.
- Configure Tailwind with risk color extension.
- Configure Vite (path aliases `@/`, dev proxy).
- Set up full `src/` folder structure.
- Create `axiosInstance.ts`, `authStore.ts`, `queryKeys.ts`, `errorParser.ts`.
- Create `.env.development` and `.env.production`.
- Implement `AppLayout` (SideNav + TopBar + Outlet) and `AuthLayout`.
- Define all routes in `router/index.tsx` with placeholder page stubs.
- Wire `ProtectedRoute` and role-based redirect logic.

**Exit criterion:** App boots, routing navigates between placeholder pages, auth store is functional.

---

### Phase 2 — Authentication Pages

- `LoginPage` + `LoginForm` (React Hook Form + Zod, error display).
- `RegisterPage` + `RegisterForm` (cascading district → neighborhood selects).
- `VerifyEmailPage` (reads token from query string, calls verify endpoint).
- Full Axios interceptor: request (attach token) + response (token refresh with queue).
- JWT decode on login → `authStore` populated.
- Page-refresh token recovery in `main.tsx`.
- Logout flow (revoke + clear + redirect).
- `ProfilePage` with three tabs: profile info, email change, password change.

**Exit criterion:** Full auth lifecycle works end-to-end against the real Spring Boot backend.

---

### Phase 3 — Dashboard

- `DashboardPage` with role-aware widget grid.
- All 8 widgets implemented with TanStack Query.
- Independent loading spinners per widget (no single blocking loader).
- `SideNav` with role-gated menu items.
- `TopBar` with user name, role badge, and logout button.

**Exit criterion:** Each role sees exactly the correct set of dashboard widgets.

---

### Phase 4 — Events UI

- `EventsPage` with filter bar, `DataTable`, skeleton loaders, pagination.
- `EventDetailPage`: full event info, mini-map pin (if lat/lon present), action buttons gated by role and event status.
- `CreateEventPage`: role-scoped neighborhood select, urgency slider, datetime validation.
- All confirmation dialogs wired.
- Optimistic updates for join/leave.
- Cache invalidation after all mutations.

**Exit criterion:** Full event lifecycle verified — create, view, join, leave, close, edit.

---

### Phase 5 — Documents UI

- `DocumentsPage`: own documents table, upload form with progress bar and client-side validation.
- `DocumentApprovalPage`: pending table, preview link, approve action, reject modal with required reason.
- All three document status badge states (PENDING / APPROVED / REJECTED).
- Rejection reason shown in expandable accordion row.

**Exit criterion:** Volunteers can upload; admins and DCs can approve and reject.

---

### Phase 6 — Map Interface

- `MapPage` with Leaflet initialized and centered on Istanbul.
- `DistrictLayer` with GeoJSON polygon rendering and risk-color fills.
- `NeighborhoodLayer` loaded on district click.
- `EventMarker` pins for OPEN events with urgency-color coding.
- `RiskLegend` overlay.
- Neighborhood side panel (assembly areas, events list).
- Layer toggle controls.

**Exit criterion:** Interactive risk map shows colored polygons, event pins, and neighborhood drill-down.

---

### Phase 7 — Simulation UI

- `SimulationTriggerPage` with form + history table + 10-second auto-refresh.
- `SimulationDetailPage` with delivery stats, progress bar (auto-refresh 5s), log table.
- All simulation and log status badges.

**Exit criterion:** Admin can trigger a simulation and watch delivery progress update live.

---

### Phase 8 — Admin Tools

- `UsersPage` with filters, role badges, edit-role modal, deactivate confirmation.
- `DistrictsPage` with 10-card grid, risk indicators, assign-coordinator modal.
- Full `422 BUSINESS_RULE_VIOLATION` error handling for all coordinator assignment rules.

**Exit criterion:** Admin can manage all users and assign all district coordinators.

---

## 16. Frontend Verification Checklist

Use this checklist to validate that the frontend integrates correctly with the backend.

---

### Authentication

- [ ] Register → 201; redirect to login with success banner.
- [ ] Register with duplicate email → field error "Email already registered."
- [ ] Register with mismatched neighborhood/district → toast 422 error.
- [ ] Login → 200; JWT decoded; user info in `authStore`; redirect to dashboard.
- [ ] Login with wrong password → 401 message displayed.
- [ ] Login with deactivated account → 403 message displayed.
- [ ] Page refresh → access token re-obtained via `POST /auth/refresh` silently.
- [ ] 1 hour passes → next request triggers silent interceptor refresh; user stays logged in.
- [ ] Expired refresh token → forced redirect to `/login`.
- [ ] Logout → tokens cleared, TanStack Query cache cleared, redirect to `/login`.
- [ ] Email change flow → 202 banner shown; `/verify-email?token=...` updates email.
- [ ] Password change → 204; all sessions revoked; re-login required.

---

### Role-Based Access

- [ ] VOLUNTEER → `/events/create` → redirect to `/unauthorized`.
- [ ] VOLUNTEER → `/admin/users` → redirect to `/unauthorized`.
- [ ] NC → `/simulations` → redirect to `/unauthorized`.
- [ ] SideNav shows correct items per role.
- [ ] `RoleGuard` components hide restricted UI elements at the component level.

---

### Events

- [ ] VOLUNTEER event list shows OPEN events only; status filter hidden.
- [ ] NC event list scoped to own neighborhood by default.
- [ ] DC event list scoped to own district by default.
- [ ] Create event as NC → neighborhoodId pre-filled and locked.
- [ ] Create event `endsAt < startsAt` → Zod validation error, no request sent.
- [ ] Create event → 201 → redirect to `/events/:id` with success toast.
- [ ] Join event (team member) → 201 → button switches to "Leave Event" immediately (optimistic).
- [ ] Join event (not a team member) → 422 → toast "Join the [team] team first."
- [ ] Leave event → 204 → button reverts to "Join Event."
- [ ] Close event → 200 → badge CLOSED, riskScore updated, volunteers show COMPLETED.
- [ ] Close already-closed event → 409 → toast message.
- [ ] Edit closed event → 422 → toast message.
- [ ] Volunteer panel visible only to ADMIN/DC/NC on event detail.

---

### Documents

- [ ] Upload `.pdf` → 201 → PENDING row appears at top of list.
- [ ] Upload file > 10 MB → client-side error, no HTTP request made.
- [ ] Upload `.exe` → client-side MIME error, no HTTP request made.
- [ ] Download own document → presigned URL opens in new tab.
- [ ] Admin/DC pending list shows PENDING documents.
- [ ] Approve → row removed; volunteer doc transitions to APPROVED.
- [ ] Reject with blank reason → modal validation error; submit blocked.
- [ ] Reject with reason → row removed; volunteer doc shows REJECTED + reason text.
- [ ] DC cannot see documents from a different district.

---

### Risk Map

- [ ] Map renders 10 district polygons on load.
- [ ] RED district fill is `#E53935`; GREEN is `#43A047`.
- [ ] Hover district → tooltip shows name and risk score.
- [ ] Click district → neighborhood polygons load inside it.
- [ ] Click neighborhood → side panel shows events and assembly areas.
- [ ] Assembly area Google Maps links open correct coordinates.
- [ ] OPEN events with coordinates appear as pins; click shows popup.
- [ ] Risk legend visible with correct color thresholds.

---

### Simulation

- [ ] Non-admin navigating to `/simulations` → redirect to `/unauthorized`.
- [ ] Trigger simulation → 202 → totalUsersToNotify shown; "View Progress" link appears.
- [ ] History table auto-refreshes while any row is PROCESSING.
- [ ] Detail page: PROCESSING → progress bar; auto-refreshes every 5 seconds.
- [ ] Detail page: COMPLETED → success message; delivery stats accurate.
- [ ] Log table filters by FAILED; pagination navigates correctly.

---

### Admin Tools

- [ ] User list loads with role/district/isActive filters functional.
- [ ] Edit role → DC selection shows district dropdown.
- [ ] Assign DC with unverified email → 422 error displayed.
- [ ] Assign DC already coordinating another district → 422 displayed.
- [ ] Deactivate user → confirmation dialog; user row shows Inactive badge.
- [ ] District cards show 10 districts with risk colors.
- [ ] Assign coordinator modal filtered to volunteers in that district.
- [ ] After coordinator assigned → district card shows new coordinator name.

---

### Performance & UX

- [ ] All list pages show skeleton rows during initial fetch.
- [ ] All empty states display message and CTA button.
- [ ] All form submissions show loading state on submit button.
- [ ] All destructive actions require confirmation before executing.
- [ ] Toast notifications appear for all success and error outcomes.
- [ ] Layout is functional on mobile (single-column; map is navigable).

---

*End of Frontend Specification*
*Related documents: `docs/master-spec-part1.md` · `docs/master-spec-part2.md` · `docs/implementation-plan.md`*
