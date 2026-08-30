-- Adds order payment-rejection columns so a cook can refuse a payment
-- screenshot with a reason instead of only being able to approve it.
--
-- Deliberately does NOT touch payment_screenshot_url: rejecting must not delete
-- the image, because it is the only evidence if the payment is disputed later.
--
-- Safe to run multiple times.

SET @dbname = DATABASE();
SET @tablename = 'orders';

SET @col = 'payment_rejection_reason';
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema = @dbname AND table_name = @tablename AND column_name = @col) = 0,
    'ALTER TABLE orders ADD COLUMN payment_rejection_reason VARCHAR(500) NULL COMMENT ''why the cook rejected the proof; shown to the customer so they can resubmit'' AFTER payment_verified_at',
    'SELECT 1'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'payment_rejected_at';
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema = @dbname AND table_name = @tablename AND column_name = @col) = 0,
    'ALTER TABLE orders ADD COLUMN payment_rejected_at TIMESTAMP NULL AFTER payment_rejection_reason',
    'SELECT 1'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = 'payment_proof_attempts';
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema = @dbname AND table_name = @tablename AND column_name = @col) = 0,
    'ALTER TABLE orders ADD COLUMN payment_proof_attempts INT NOT NULL DEFAULT 0 COMMENT ''how many proofs have been submitted; a rising count is a dispute signal'' AFTER payment_rejected_at',
    'SELECT 1'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Existing online orders that already have a screenshot have had exactly one
-- proof submitted; leaving them at 0 would make the cook's "Attempt N" warning
-- read wrong the first time they reject one.
UPDATE orders
   SET payment_proof_attempts = 1
 WHERE payment_proof_attempts = 0
   AND payment_screenshot_url IS NOT NULL
   AND payment_screenshot_url <> '';
