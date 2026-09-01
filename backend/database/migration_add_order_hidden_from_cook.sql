-- Adds hidden_from_cook so a cook "deleting" an order only removes the card
-- from their list, never the money from earnings (earnings SUMs total_amount
-- straight off the orders table, so a hard DELETE was subtracting the order's
-- amount from every earnings total/breakdown/transaction list too).
--
-- Safe to run multiple times.

SET @dbname = DATABASE();
SET @tablename = 'orders';

SET @col = 'hidden_from_cook';
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema = @dbname AND table_name = @tablename AND column_name = @col) = 0,
    'ALTER TABLE orders ADD COLUMN hidden_from_cook BOOLEAN NOT NULL DEFAULT FALSE AFTER status',
    'SELECT 1'
));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
