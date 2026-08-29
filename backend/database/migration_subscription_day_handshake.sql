-- ============================================================================
-- Per-day sent → received handshake
--
-- Until now a delivery day went straight from 'scheduled' to 'delivered', and
-- only the 06:00 cron ever wrote 'delivered' — inferred from the linked order's
-- status. Neither side of the transaction could say "I handed it over" or "I got
-- it", so a dispute about a single day had nothing in it but the cron's guess.
--
-- 'sent' is the intermediate state: the cook has handed the meal over and the
-- customer has not confirmed it yet.
--
--   scheduled --(cook marks sent)--> sent --(customer confirms)--> delivered
--
-- 'delivered' stays TERMINAL. The customer's confirmation is the only thing
-- that writes it from a live day; the reconcile pass writes it for a 'sent' day
-- the customer never got round to confirming, so an unconfirmed day settles
-- instead of hanging open forever.
--
-- APPENDED to the end of the ENUM, deliberately. MySQL/TiDB store an ENUM as the
-- ordinal of its value, so inserting 'sent' in the middle of the list — next to
-- 'scheduled', where it belongs logically — would silently renumber every
-- existing row. Display order is a client concern; storage order must not move.
--
-- Idempotent: MODIFY COLUMN restates the whole definition, so re-running it is a
-- no-op rather than an error.
--   mysql -u <user> -p <database> < migration_subscription_day_handshake.sql
-- ============================================================================

ALTER TABLE subscription_daily_log
    MODIFY COLUMN status
        ENUM('scheduled','customer_skipped','cook_unavailable','delivered','missed','sent')
        NOT NULL DEFAULT 'scheduled';

-- No backfill. A past day that was settled as 'delivered' or 'missed' by the
-- cron was never handed over under this handshake, and rewriting it as 'sent'
-- would claim a confirmation that never happened.
