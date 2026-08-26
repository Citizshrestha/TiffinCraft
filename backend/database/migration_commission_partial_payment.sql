-- ============================================================================
-- Edge case 3 — partial / installment commission payments.
--
-- PROBLEM: status was all-or-nothing (pending -> verified). If a cook paid
-- ₹100 against ₹145 due, the admin's only options were to verify (silently
-- writing off ₹45) or reject (pretending nothing was paid). Neither leaves a
-- record of the ₹45 still owed.
--
-- FIX: track money received separately from money owed. amount_due stays the
-- authoritative snapshot of what was billed and is never rewritten;
-- amount_paid accumulates across however many installments arrive. A
-- settlement only reaches 'verified' once amount_paid >= amount_due, so a
-- short payment stays 'pending' (and therefore still chaseable and still
-- counted as overdue) instead of vanishing. No new status values needed.
--
-- Idempotent: safe to run more than once.
-- ============================================================================

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'commission_settlements'
      AND column_name = 'amount_paid'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE commission_settlements
       ADD COLUMN amount_paid DECIMAL(10,2) NOT NULL DEFAULT 0 AFTER amount_due',
    'SELECT "amount_paid already exists" AS note');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Existing rows already marked verified were, by the old all-or-nothing rule,
-- paid in full — so backfill them to amount_due rather than leaving them at 0
-- and making every historical settlement look unpaid.
UPDATE commission_settlements
SET amount_paid = amount_due
WHERE status = 'verified' AND amount_paid = 0;

SELECT column_name, column_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'commission_settlements'
  AND column_name IN ('amount_due', 'amount_paid');
