import db from "../config/db.js";

// This small compatibility migration runs before the API starts. Some
// deployed databases predate subscription_plan_items.is_active, while newer
// controllers use it to preserve plan membership when meals are removed.
try {
    const [columns] = await db.promise().query(
        `SELECT COUNT(*) AS column_count
         FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'subscription_plan_items'
           AND COLUMN_NAME = 'is_active'`
    );

    if (Number(columns[0]?.column_count || 0) === 0) {
        await db.promise().query(
            `ALTER TABLE subscription_plan_items
             ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE
             COMMENT 'False while the cook temporarily removes this meal from subscription availability'
             AFTER quantity`
        );
        console.log("✅ Added subscription_plan_items.is_active");
    }

    const [indexes] = await db.promise().query(
        `SELECT COUNT(*) AS index_count
         FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'subscription_plan_items'
           AND INDEX_NAME = 'idx_item_plan_active'`
    );
    if (Number(indexes[0]?.index_count || 0) === 0) {
        await db.promise().query(
            "ALTER TABLE subscription_plan_items ADD INDEX idx_item_plan_active (plan_id, is_active)"
        );
        console.log("✅ Added subscription plan active-item index");
    }
} catch (error) {
    console.error("❌ Subscription plan item schema check failed:", error.message);
    process.exitCode = 1;
} finally {
    await db.promise().end();
}
