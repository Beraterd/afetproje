-- V57: Extend damage_assessment_photos with photo_type + uploaded_by,
--      and damage_assessment_assignments with assignment_status.

-- 1. photo_type: separates reporter photos from field-assignee photos
ALTER TABLE damage_assessment_photos
    ADD COLUMN IF NOT EXISTS photo_type VARCHAR(30) NOT NULL DEFAULT 'REPORTER_PHOTO';

-- 2. uploaded_by: nullable FK — existing rows may have no uploader recorded
ALTER TABLE damage_assessment_photos
    ADD COLUMN IF NOT EXISTS uploaded_by UUID REFERENCES users(id);

-- 3. assignment_status: tracks volunteer progress on a task
ALTER TABLE damage_assessment_assignments
    ADD COLUMN IF NOT EXISTS assignment_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';
