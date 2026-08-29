import db from "../config/db.js";

/**
 * POST /api/maintenance/normalize-emails
 * One-time endpoint to normalize all email addresses
 * Admin only - should be removed after migration
 */
export const normalizeEmails = async (req, res) => {
    const connection = await db.promise().getConnection();

    try {
        await connection.beginTransaction();

        // Get all users with their current emails
        const [users] = await connection.query("SELECT id, email FROM users");

        let updatedCount = 0;
        let skippedCount = 0;
        const conflicts = [];
        const updates = [];

        for (const user of users) {
            const originalEmail = user.email;
            const normalizedEmail = originalEmail.trim().toLowerCase();

            if (originalEmail !== normalizedEmail) {
                // Check if normalized email conflicts with another user
                const [conflictUsers] = await connection.query(
                    "SELECT id, email FROM users WHERE email = ? AND id != ?",
                    [normalizedEmail, user.id]
                );

                if (conflictUsers.length > 0) {
                    conflicts.push({
                        userId: user.id,
                        original: originalEmail,
                        normalized: normalizedEmail,
                        conflictsWith: conflictUsers[0].id
                    });
                    skippedCount++;
                    continue;
                }

                // Update the email
                await connection.query(
                    "UPDATE users SET email = ? WHERE id = ?",
                    [normalizedEmail, user.id]
                );

                updates.push({
                    userId: user.id,
                    from: originalEmail,
                    to: normalizedEmail
                });
                updatedCount++;
            }
        }

        await connection.commit();

        return res.status(200).json({
            success: true,
            message: "Email normalization complete",
            stats: {
                total: users.length,
                updated: updatedCount,
                skipped: skippedCount,
                alreadyNormalized: users.length - updatedCount - skippedCount
            },
            updates,
            conflicts
        });

    } catch (error) {
        await connection.rollback();
        console.error("normalizeEmails error:", error);
        return res.status(500).json({
            success: false,
            message: "Email normalization failed",
            error: error.message
        });
    } finally {
        connection.release();
    }
};
