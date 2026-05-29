-- V31: Update verification_status values to new Turkish enum names
-- Old: PENDING, FIELD_VERIFIED, COORDINATOR_APPROVED, REJECTED, NEEDS_REVIEW
-- New: INCELEME_GEREKIYOR, SAHADA_DOGRULANDI, KOORDINATOR_ONAYLADI

UPDATE damage_assessments
SET verification_status = 'INCELEME_GEREKIYOR'
WHERE verification_status IN ('PENDING', 'NEEDS_REVIEW', 'REJECTED');

UPDATE damage_assessments
SET verification_status = 'SAHADA_DOGRULANDI'
WHERE verification_status = 'FIELD_VERIFIED';

UPDATE damage_assessments
SET verification_status = 'KOORDINATOR_ONAYLADI'
WHERE verification_status = 'COORDINATOR_APPROVED';
