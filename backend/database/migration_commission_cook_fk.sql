-- ============================================================================
-- Edge case 6 — stop cook deletion from erasing the commission money trail.
--
-- PROBLEM (verified against the live schema): the FK
-- commission_settlements_ibfk_1 (cook_id -> users.id) is ON DELETE CASCADE.
-- Deleting a cook therefore silently destroys every record of commission they
-- owed or paid. This is reachable on two real paths:
--   * adminController.js  — admin deletes a user (hard DELETE FROM users)
--   * authController.js   — the COOK deletes their own account, and the code
--                           comment explicitly relies on CASCADE
-- So a cook who owes money can erase the debt themselves.
--
-- FIX: SET NULL, not RESTRICT. Self-service account deletion is a Google Play
-- requirement, so RESTRICT would either break compliance or leave the user
-- with an un-deletable account. SET NULL keeps deletion working while the
-- settlement row survives — and the cook's identity survives with it via the
-- denormalised snapshot columns added below, because once cook_id is NULL the
-- JOIN to users can no longer tell you whose debt it was.
--
-- Idempotent: safe to run more than once.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. Denormalised identity, so a settlement is still attributable after the
--    user row is gone.
-- ---------------------------------------------------------------------------
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'commission_settlements'
      AND column_name = 'cook_name_snapshot'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE commission_settlements ADD COLUMN cook_name_snapshot VARCHAR(255) NULL AFTER cook_id',
    'SELECT "cook_name_snapshot already exists" AS note');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'commission_settlements'
      AND column_name = 'cook_phone_snapshot'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE commission_settlements ADD COLUMN cook_phone_snapshot VARCHAR(30) NULL AFTER cook_name_snapshot',
    'SELECT "cook_phone_snapshot already exists" AS note');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'commission_settlements'
      AND column_name = 'kitchen_name_snapshot'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE commission_settlements ADD COLUMN kitchen_name_snapshot VARCHAR(255) NULL AFTER cook_phone_snapshot',
    'SELECT "kitchen_name_snapshot already exists" AS note');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Backfill existing rows from the users/cook_profiles they still point at.
UPDATE commission_settlements s
JOIN users u ON u.id = s.cook_id
LEFT JOIN cook_profiles cp ON cp.user_id = s.cook_id
SET s.cook_name_snapshot    = COALESCE(s.cook_name_snapshot, u.full_name),
    s.cook_phone_snapshot   = COALESCE(s.cook_phone_snapshot, u.phone),
    s.kitchen_name_snapshot = COALESCE(s.kitchen_name_snapshot, cp.kitchen_name)
WHERE s.cook_name_snapshot IS NULL
   OR s.cook_phone_snapshot IS NULL
   OR s.kitchen_name_snapshot IS NULL;

-- ---------------------------------------------------------------------------
-- 2. cook_id must be nullable before a FK can SET NULL on it.
-- ---------------------------------------------------------------------------
SET @is_nullable = (
    SELECT is_nullable FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'commission_settlements'
      AND column_name = 'cook_id'
);
SET @sql = IF(@is_nullable = 'NO',
    'ALTER TABLE commission_settlements MODIFY COLUMN cook_id INT NULL',
    'SELECT "cook_id already nullable" AS note');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 3. Swap CASCADE for SET NULL. Looked up by rule rather than by hardcoded
--    name so this still works if the constraint was auto-named differently.
-- ---------------------------------------------------------------------------
SET @fk_name = (
    SELECT rc.constraint_name
    FROM information_schema.referential_constraints rc
    JOIN information_schema.key_column_usage k
      ON k.constraint_name = rc.constraint_name
     AND k.constraint_schema = rc.constraint_schema
    WHERE rc.constraint_schema = DATABASE()
      AND rc.table_name = 'commission_settlements'
      AND k.column_name = 'cook_id'
      AND rc.delete_rule = 'CASCADE'
    LIMIT 1
);

SET @sql = IF(@fk_name IS NOT NULL,
    CONCAT('ALTER TABLE commission_settlements DROP FOREIGN KEY ', @fk_name),
    'SELECT "no CASCADE FK on cook_id — already fixed" AS note');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@fk_name IS NOT NULL,
    'ALTER TABLE commission_settlements
       ADD CONSTRAINT fk_settlement_cook
       FOREIGN KEY (cook_id) REFERENCES users(id) ON DELETE SET NULL',
    'SELECT "FK not re-added — nothing was dropped" AS note');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 4. Verify.
-- ---------------------------------------------------------------------------
SELECT rc.constraint_name, k.column_name, rc.delete_rule
FROM information_schema.referential_constraints rc
JOIN information_schema.key_column_usage k
  ON k.constraint_name = rc.constraint_name
 AND k.constraint_schema = rc.constraint_schema
WHERE rc.constraint_schema = DATABASE()
  AND rc.table_name = 'commission_settlements';
