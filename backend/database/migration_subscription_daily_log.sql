-- ============================================================================
-- subscription_daily_log — one row per subscription per delivery date
--
-- An active subscription used to be a single on/off flag plus a `skipped_dates`
-- JSON array, which meant:
--   * no way to record WHY a day didn't happen (customer skipped vs cook closed
--     vs genuinely missed),
--   * no way to record WHO changed it or when,
--   * and JSON_CONTAINS/append-in-app-code races on concurrent writes.
--
-- This table replaces that: every day of an active subscription is its own
-- record with its own status, actor and reason.
--
-- Concurrency: UNIQUE KEY (subscription_id, delivery_date) makes the write
-- atomic. Callers use INSERT IGNORE (+ affectedRows to tell created-from-existing)
-- followed by a guarded UPDATE, rather than SELECT-then-INSERT which can
-- interleave. This is deliberate — see the "concurrent skip + cook-unavailable"
-- edge case: the constraint decides the winner, not application timing.
--
-- Idempotent (CREATE TABLE IF NOT EXISTS).
--   mysql -u <user> -p <database> < migration_subscription_daily_log.sql
-- ============================================================================

CREATE TABLE IF NOT EXISTS subscription_daily_log (
    id INT PRIMARY KEY AUTO_INCREMENT,
    subscription_id INT NOT NULL,
    delivery_date DATE NOT NULL COMMENT 'the NPT calendar date this row is about, never a timestamp',

    -- scheduled        : a meal is expected and nothing has overridden it
    -- customer_skipped : customer opted out of this one day (no credit charged)
    -- cook_unavailable : cook closed for this date (bulk, no credit charged)
    -- delivered        : the order was actually fulfilled — terminal, never overwritten
    -- missed           : the day passed with no delivery and no explicit skip
    status ENUM('scheduled','customer_skipped','cook_unavailable','delivered','missed')
        NOT NULL DEFAULT 'scheduled',

    toggled_by ENUM('customer','cook','system') NULL COMMENT 'actor for dispute resolution; system = cron',
    reason TEXT NULL,
    credit_deducted BOOLEAN NOT NULL DEFAULT FALSE
        COMMENT 'TRUE only when this day consumed one of the paid meals',

    -- The order the cron actually placed for this day, if any. `orders` has no
    -- subscription_id column (subscription orders are only recognisable by
    -- their special_instructions text), so without this there is no way to ask
    -- "did the meal we scheduled for the 3rd actually get delivered?" — which
    -- is exactly what the next morning's reconcile pass needs in order to
    -- settle the day as 'delivered' rather than 'missed'.
    order_id INT NULL,

    toggled_at TIMESTAMP NULL COMMENT 'when the status last changed (UTC)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_sdl_sub_date (subscription_id, delivery_date),
    INDEX idx_sdl_date_status (delivery_date, status),
    CONSTRAINT fk_sdl_subscription FOREIGN KEY (subscription_id)
        REFERENCES subscriptions(id) ON DELETE CASCADE,
    -- SET NULL, not CASCADE: if an order row is ever removed, the day still
    -- happened and its status must survive.
    CONSTRAINT fk_sdl_order FOREIGN KEY (order_id)
        REFERENCES orders(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Backfill note: intentionally none. Existing `skipped_dates` entries are all in
-- the past for the handful of legacy rows, and inventing 'scheduled' rows for
-- past dates would make the cron think it already handled days it never saw.
-- Legacy subscriptions simply start logging from their next delivery onward.
