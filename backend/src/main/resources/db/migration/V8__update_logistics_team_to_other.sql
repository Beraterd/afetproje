-- V8: Update LOGISTICS team to OTHER (must run after V7 adds OTHER to enum)

UPDATE teams
SET    name        = 'OTHER',
       description = 'Diğer Ekip',
       coefficient = 1.0,
       updated_at  = now()
WHERE  name = 'LOGISTICS';
