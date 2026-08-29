import db from "../config/db.js";

/**
 * One-time script to normalize all email addresses in the database
 * Trims whitespace and converts to lowercase
 */
async function normalizeEmails() {
    try {
        console.log("Starting email normalization...");

        // Get all users with their current emails
        const [users] = await db.promise().query(
            "SELECT id, email FROM users"
        );

        console.log(`Found ${users.length} users to process`);

        let updatedCount = 0;
        let skippedCount = 0;

        for (const user of users) {
            const originalEmail = user.email;
            const normalizedEmail = originalEmail.trim().toLowerCase();

            if (originalEmail !== normalizedEmail) {
                console.log(`\nUser ID ${user.id}:`);
                console.log(`  Original: "${originalEmail}"`);
                console.log(`  Normalized: "${normalizedEmail}"`);

                // Check if normalized email conflicts with another user
                const [conflicts] = await db.promise().query(
                    "SELECT id, email FROM users WHERE email = ? AND id != ?",
                    [normalizedEmail, user.id]
                );

                if (conflicts.length > 0) {
                    console.log(`  ⚠️  CONFLICT: Email "${normalizedEmail}" already exists for user ID ${conflicts[0].id}`);
                    console.log(`  Skipping user ID ${user.id}`);
                    skippedCount++;
                    continue;
                }

                // Update the email
                await db.promise().query(
                    "UPDATE users SET email = ? WHERE id = ?",
                    [normalizedEmail, user.id]
                );

                console.log(`  ✅ Updated`);
                updatedCount++;
            }
        }

        console.log("\n" + "=".repeat(50));
        console.log(`Email normalization complete!`);
        console.log(`Total users: ${users.length}`);
        console.log(`Updated: ${updatedCount}`);
        console.log(`Skipped (conflicts): ${skippedCount}`);
        console.log(`Already normalized: ${users.length - updatedCount - skippedCount}`);
        console.log("=".repeat(50));

        process.exit(0);
    } catch (error) {
        console.error("Error normalizing emails:", error);
        process.exit(1);
    }
}

normalizeEmails();
