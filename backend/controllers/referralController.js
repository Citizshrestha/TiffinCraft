import db from "../config/db.js";
import { createNotification } from "../utils/notificationHelper.js";

const REFERRER_REWARD = 100.00;
const REFERRED_REWARD = 50.00;
const CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I — avoids read-aloud mixups

const generateCode = () => {
    let code = "TC";
    for (let i = 0; i < 6; i++) {
        code += CODE_CHARS[Math.floor(Math.random() * CODE_CHARS.length)];
    }
    return code;
};

/** Returns the user's referral code, creating one on first access. */
const ensureReferralCode = async (userId) => {
    const [[user]] = await db.promise().query(
        "SELECT referral_code FROM users WHERE id = ?", [userId]
    );
    if (!user) return null;
    if (user.referral_code) return user.referral_code;

    // Retry on the (unlikely) unique-collision
    for (let attempt = 0; attempt < 5; attempt++) {
        const code = generateCode();
        try {
            await db.promise().query(
                "UPDATE users SET referral_code = ? WHERE id = ?", [code, userId]
            );
            return code;
        } catch (err) {
            if (err.code !== "ER_DUP_ENTRY") throw err;
        }
    }
    throw new Error("Could not generate a unique referral code.");
};

// GET /api/referrals/my
// The user's code, credit balance, and everyone they've referred.
export const getMyReferralInfo = async (req, res) => {
    try {
        const userId = req.user.id;

        const code = await ensureReferralCode(userId);
        if (!code) {
            return res.status(404).json({ success: false, message: "User not found." });
        }

        const [[user]] = await db.promise().query(
            "SELECT referral_credits FROM users WHERE id = ?", [userId]
        );

        const [referrals] = await db.promise().query(
            `SELECT r.referrer_reward AS reward, r.created_at, u.full_name
             FROM referrals r
             JOIN users u ON r.referred_id = u.id
             WHERE r.referrer_id = ?
             ORDER BY r.created_at DESC`,
            [userId]
        );

        const [[applied]] = await db.promise().query(
            "SELECT id FROM referrals WHERE referred_id = ?", [userId]
        );

        return res.status(200).json({
            success: true,
            code,
            credits: parseFloat(user.referral_credits) || 0,
            total_referred: referrals.length,
            has_applied: !!applied,
            referrer_reward: REFERRER_REWARD,
            referred_reward: REFERRED_REWARD,
            referrals: referrals.map(r => ({
                full_name: r.full_name,
                reward: parseFloat(r.reward) || 0,
                created_at: r.created_at
            }))
        });

    } catch (error) {
        console.error("getMyReferralInfo error:", error);
        return res.status(500).json({ success: false, message: "Server error." });
    }
};

// POST /api/referrals/apply
// Body: { code } — credits both parties and records the referral.
export const applyReferralCode = async (req, res) => {
    try {
        const userId = req.user.id;
        const code = typeof req.body.code === "string" ? req.body.code.trim().toUpperCase() : "";

        if (!code) {
            return res.status(400).json({ success: false, message: "Referral code is required." });
        }

        const [[owner]] = await db.promise().query(
            "SELECT id, full_name FROM users WHERE referral_code = ?", [code]
        );
        if (!owner) {
            return res.status(404).json({ success: false, message: "Invalid referral code." });
        }
        if (owner.id === userId) {
            return res.status(400).json({ success: false, message: "You cannot use your own referral code." });
        }

        const [[already]] = await db.promise().query(
            "SELECT id FROM referrals WHERE referred_id = ?", [userId]
        );
        if (already) {
            return res.status(400).json({ success: false, message: "You have already used a referral code." });
        }

        await db.promise().query(
            `INSERT INTO referrals (referrer_id, referred_id, code_used, referrer_reward, referred_reward)
             VALUES (?, ?, ?, ?, ?)`,
            [owner.id, userId, code, REFERRER_REWARD, REFERRED_REWARD]
        );

        await db.promise().query(
            "UPDATE users SET referral_credits = referral_credits + ? WHERE id = ?",
            [REFERRER_REWARD, owner.id]
        );
        await db.promise().query(
            "UPDATE users SET referral_credits = referral_credits + ? WHERE id = ?",
            [REFERRED_REWARD, userId]
        );

        const [[me]] = await db.promise().query(
            "SELECT full_name, referral_credits FROM users WHERE id = ?", [userId]
        );

        // Tell the referrer they just earned credits (bell + real-time if connected)
        await createNotification(
            owner.id,
            "You earned referral credits! 🎉",
            `${me.full_name} joined with your code — ₹${REFERRER_REWARD.toFixed(0)} credits added.`,
            "referral_reward",
            userId,
            "referral"
        );
        const io = req.app.get("io");
        if (io) {
            io.to(`user_${owner.id}`).emit("chatNotification", {
                conversation_id: 0,
                sender_name: "TiffinCraft Rewards",
                preview: `${me.full_name} joined with your code — ₹${REFERRER_REWARD.toFixed(0)} credits added!`
            });
        }

        return res.status(200).json({
            success: true,
            message: `Code applied! ₹${REFERRED_REWARD.toFixed(0)} credits added to your account.`,
            credits: parseFloat(me.referral_credits) || 0
        });

    } catch (error) {
        if (error.code === "ER_DUP_ENTRY") {
            return res.status(400).json({ success: false, message: "You have already used a referral code." });
        }
        console.error("applyReferralCode error:", error);
        return res.status(500).json({ success: false, message: "Server error." });
    }
};
