-- V1: Create PostgreSQL ENUMs
-- All enum types used across the platform

CREATE TYPE user_role AS ENUM (
    'ADMIN',
    'DISTRICT_COORDINATOR',
    'NEIGHBORHOOD_COORDINATOR',
    'VOLUNTEER'
);

CREATE TYPE blood_type AS ENUM (
    'A_POSITIVE', 'A_NEGATIVE',
    'B_POSITIVE', 'B_NEGATIVE',
    'AB_POSITIVE', 'AB_NEGATIVE',
    'O_POSITIVE', 'O_NEGATIVE'
);

CREATE TYPE team_name AS ENUM (
    'SEARCH_RESCUE',
    'FOOD_WATER',
    'LOGISTICS',
    'EVACUATION',
    'COMMUNICATION',
    'PSYCHOSOCIAL'
);

CREATE TYPE document_type AS ENUM (
    'SEARCH_RESCUE_CERTIFICATE',
    'PSYCHOSOCIAL_GRADUATION_DOCUMENT'
);

CREATE TYPE document_status AS ENUM (
    'PENDING',
    'APPROVED',
    'REJECTED'
);

CREATE TYPE event_status AS ENUM (
    'OPEN',
    'CLOSED'
);

CREATE TYPE event_volunteer_status AS ENUM (
    'ASSIGNED',
    'COMPLETED',
    'WITHDRAWN'
);

CREATE TYPE simulation_email_status AS ENUM (
    'QUEUED',
    'PROCESSING',
    'COMPLETED',
    'PARTIAL_FAILURE'
);

CREATE TYPE notification_status AS ENUM (
    'QUEUED',
    'SENT',
    'FAILED',
    'BOUNCED'
);
