import db from '../config/db.js';

try {
    await db.promise().query(
        `ALTER TABLE subscription_daily_log
         MODIFY COLUMN status
             ENUM('scheduled','customer_skipped','cook_unavailable','delivered','missed','sent','cook_delivery_unavailable')
             NOT NULL DEFAULT 'scheduled'`
    );
    console.log('Single-delivery unavailable status migration applied.');
} finally {
    await db.promise().end();
}
