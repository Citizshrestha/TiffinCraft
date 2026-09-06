import crypto from "crypto";
import db from "../config/db.js";
import { uploadToCloudinary } from "../services/uploadService.js";
import { fetchPlanWithItems } from "./subscriptionPlanController.js";
import { logDayEvent, applyDayStatus, DAY_STATUS, placeDayOrder } from "../utils/subscriptionDailyLog.js";
import {
    announceSubscriptionEvent, subscriptionCardMeta, CARD_TYPES
} from "../utils/subscriptionEvents.js";
import {
    dateOnly, getNptToday, addDays, daysBetween, SQL_NPT_TODAY
} from "../utils/nptTime.js";
import {
    getDurationDays, validateStartDate
} from "./subscriptionController.js";

/**
 * Subscription request-to-active flow.
 *
 *   requested  -> cook has not answered yet
 *   accepted   -> cook said yes; the customer now owes payment proof
 *   rejected   -> cook said no. TERMINAL, and deliberately distinct from
 *                 'cancelled': a cook declining is not the customer walking
 *                 away, and only the first leaves the customer free to
 *                 immediately request again on a different date.
 *   pending_verification -> proof uploaded, waiting on the cook
 *   verified/scheduled/active -> money confirmed; see verifySubscriptionProof
 *   completed  -> the calendar window ran out
 *
 * ── IMPLEMENTATION NOTE: payment verification is NOT cryptographic ─────────
 * Step 5 of this flow is a MANUAL, TRUST-BASED check. The cook looks at a JPEG
 * and taps Verify. Nothing here proves money moved: a cook can approve a forged
 * screenshot, approve a real screenshot for the wrong amount, or wrongly reject
 * a valid one. The three safeguards that DO exist are:
 *
 *   1. The image is stored permanently against this subscription row
 *      (payment_screenshot_url) and never overwritten silently — every
 *      submission is also written to subscription_payment_events, so a dispute
 *      can be reconstructed after the fact.
 *   2. payment_screenshot_hash is UNIQUE across the whole table, so the same
 *      image file cannot be submitted for two different subscriptions. This is
 *      the only mechanical fraud check in the flow.
 *   3. The cook has a Reject path, not just an approve path, and rejecting
 *      rolls the subscription back to 'accepted' so the customer can resubmit.
 *
 * Anything stronger (gateway confirmation) is the separate eSewa ePay flow in
 * subscriptionPaymentController.js. This path exists because most cooks here
 * are paid by direct QR transfer, which the platform cannot observe.
 */

/** Statuses that block a second request to the SAME cook. */
const BLOCKING_STATUSES = [
    "requested", "accepted", "pending_payment", "pending_verification",
    "verified", "scheduled", "active", "paused"
];

const describeStatus = (status) => ({
    requested: "waiting for the cook to accept it",
    accepted: "accepted — it just needs your payment proof",
    pending_payment: "waiting for your payment",
    pending_verification: "waiting for the cook to verify your payment",
    verified: "verified and about to start",
    scheduled: "scheduled to start on your chosen date",
    active: "active right now",
    paused: "paused"
}[status] || `currently "${status}"`);

/**
 * The customer-facing stage line. This is what drives the four-stage status
 * messaging the app shows, and it is computed here rather than in the app so
 * both sides can never describe the same row differently.
 */
export const stageFor = (sub) => {
    switch (sub.status) {
        case "requested":
            return { stage: "waiting_accept", headline: "Waiting for cook to accept", detail: "The cook has your request. They'll confirm whether the start date works for them." };
        case "accepted":
            return { stage: "awaiting_payment", headline: "Accepted — upload payment proof", detail: "Pay the cook, then upload a screenshot so they can confirm it." };
        case "rejected":
            return { stage: "rejected", headline: "Request declined", detail: sub.response_note || "The cook couldn't take this one on. You can request again with a different start date." };
        case "pending_payment":
            return { stage: "awaiting_payment", headline: "Payment pending", detail: "Pay the cook and upload your proof to continue." };
        case "pending_verification":
            return { stage: "verifying", headline: "Waiting for payment verification", detail: "The cook is checking your payment proof." };
        case "verified":
        case "scheduled":
            return { stage: "scheduled", headline: "Confirmed — starts soon", detail: `Deliveries begin on ${dateOnly(sub.start_date)}.` };
        case "active":
            return { stage: "active", headline: "Active", detail: sub.end_date ? `Runs until ${dateOnly(sub.end_date)}.` : "Deliveries are running." };
        case "paused":
            return { stage: "paused", headline: "Paused", detail: "Resume it whenever you're ready." };
        case "completed":
            return { stage: "completed", headline: "Completed", detail: "This subscription has finished its window." };
        case "cancelled":
            return { stage: "cancelled", headline: "Cancelled", detail: "This subscription was cancelled." };
        default:
            return { stage: sub.status, headline: sub.status, detail: "" };
    }
};

const nameOf = async (userId) => {
    const [[row]] = await db.promise().query("SELECT full_name FROM users WHERE id = ?", [userId]);
    return row?.full_name || "Someone";
};

// ===========================================================================
// 1. Customer creates the request
// ===========================================================================

/**
 * POST /api/subscriptions/request — customer only.
 * Body: { plan_id, delivery_address, start_date, note? }
 *
 * Creates the row in 'requested' — NOT active, and no money asked for yet.
 * Announces it on all three channels in one call so the cook sees the same
 * thing in their bell, on their lock screen, and in the chat thread.
 */
export const createSubscriptionRequest = async (req, res) => {
    try {
        const customerId = req.user.id;
        const { plan_id, delivery_address, start_date, note } = req.body;

        if (!plan_id) {
            return res.status(400).json({ success: false, message: "plan_id is required." });
        }
        const address = typeof delivery_address === "string" ? delivery_address.trim() : "";
        if (!address) {
            return res.status(400).json({ success: false, message: "A delivery address is required." });
        }
        const startCheck = validateStartDate(start_date);
        if (!startCheck.ok) {
            return res.status(400).json({ success: false, message: startCheck.message });
        }
        const startDate = startCheck.startDate;
        const requestNote = typeof note === "string" ? note.trim().slice(0, 300) : null;

        const fullPlan = await fetchPlanWithItems(plan_id);
        if (!fullPlan) {
            return res.status(404).json({ success: false, message: "Subscription plan not found." });
        }
        if (!fullPlan.is_active) {
            return res.status(400).json({ success: false, message: "This plan is no longer offered." });
        }
        if (!fullPlan.is_available) {
            return res.status(400).json({
                success: false,
                message: "This plan includes an item the cook has marked unavailable. Please try again once it's back."
            });
        }

        const cookId = Number(fullPlan.cook_id);
        if (cookId === customerId) {
            return res.status(400).json({ success: false, message: "You can't subscribe to your own plan." });
        }

        // ── The "second plan before the first is verified" decision ──────────
        // ALLOWED across different cooks, BLOCKED with the same cook. A cook
        // can only reason about one running commitment per customer — two
        // overlapping plans from the same person means two meals a day from one
        // kitchen with one address, which is nearly always a double-tap or a
        // mistake rather than an intent. Different cooks are genuinely separate
        // commitments and are left alone. Enforced here for BOTH the request
        // and the payment stages, so the rule doesn't change halfway through.
        const [blocking] = await db.promise().query(
            `SELECT s.id, s.status, p.name AS plan_name
             FROM subscriptions s
             JOIN subscription_plans p ON s.plan_id = p.id
             WHERE s.customer_id = ? AND s.cook_id = ?
               AND s.status IN (?)
               AND (s.end_date IS NULL OR s.end_date >= ${SQL_NPT_TODAY})
             ORDER BY s.id DESC LIMIT 1`,
            [customerId, cookId, BLOCKING_STATUSES]
        );
        if (blocking.length > 0) {
            return res.status(409).json({
                success: false,
                message: `You already have a subscription with this cook ("${blocking[0].plan_name}") — it's ${describeStatus(blocking[0].status)}. Finish or cancel that one first.`,
                subscription_id: blocking[0].id
            });
        }

        const durationDays = getDurationDays(fullPlan.duration);
        const amount = Number(fullPlan.price_per_delivery) || null;

        // Each request is a distinct commitment. Reusing a cancelled/completed
        // ID attaches its old skips, custom meals and payment history to the new
        // purchase. Keep that history on the original row instead.
        const [created] = await db.promise().query(
            `INSERT INTO subscriptions
               (customer_id, cook_id, plan_id, delivery_address, start_date, next_delivery_date,
                status, payment_status, payment_method, request_note, requested_at)
             VALUES (?, ?, ?, ?, ?, ?, 'requested', 'pending', 'manual_qr', ?, NOW())`,
            [customerId, cookId, plan_id, address, startDate, startDate, requestNote]
        );
        const subscriptionId = created.insertId;

        await logDayEvent({
            subscriptionId, event: "requested", actor: "customer",
            detail: `Requested "${fullPlan.name}" (${durationDays} days) starting ${startDate}.`
        });

        const customerName = await nameOf(customerId);
        const { conversationId } = await announceSubscriptionEvent({
            io: req.app.get("io"),
            customerId, cookId,
            senderId: customerId, recipientId: cookId, senderName: customerName,
            cardType: CARD_TYPES.SUBSCRIPTION_REQUEST,
            cardText: `Subscription request: ${fullPlan.name} · ${durationDays} days from ${startDate}`,
            metadata: subscriptionCardMeta({
                subscriptionId, planName: fullPlan.name, duration: fullPlan.duration,
                durationDays, amount, startDate, status: "requested",
                customerName, note: requestNote, address
            }),
            referenceId: subscriptionId, referenceType: "subscription",
            title: "New subscription request",
            body: `${customerName} wants to start "${fullPlan.name}" on ${startDate}.`,
            notifType: "subscription_request"
        });

        return res.status(201).json({
            success: true,
            message: "Request sent. The cook will confirm whether that start date works.",
            subscription_id: subscriptionId,
            conversation_id: conversationId,
            status: "requested",
            start_date: startDate,
            duration_days: durationDays,
            amount
        });
    } catch (error) {
        console.error("createSubscriptionRequest error:", error);
        return res.status(500).json({ success: false, message: "Couldn't send the request. Please try again.", error: error.message });
    }
};

// ===========================================================================
// 2. Cook accepts or rejects
// ===========================================================================

/**
 * PUT /api/subscriptions/:id/respond — cook only.
 * Body: { action: 'accept' | 'reject', note? }
 *
 * The UPDATE is guarded on `status = 'requested'` so a double-tap (or the cook
 * answering from both the requests list and the chat card at once) loses with
 * affectedRows = 0 and gets a 409 instead of overwriting a decision.
 */
export const respondToSubscriptionRequest = async (req, res) => {
    try {
        const cookId = req.user.id;
        const { id } = req.params;
        const action = String(req.body.action || "").toLowerCase();
        const note = typeof req.body.note === "string" ? req.body.note.trim().slice(0, 300) : null;

        if (!["accept", "reject"].includes(action)) {
            return res.status(400).json({ success: false, message: "action must be 'accept' or 'reject'." });
        }

        const [subs] = await db.promise().query(
            `SELECT s.*, p.name AS plan_name, p.duration, p.price_per_delivery
             FROM subscriptions s JOIN subscription_plans p ON s.plan_id = p.id
             WHERE s.id = ?`,
            [id]
        );
        if (subs.length === 0) {
            return res.status(404).json({ success: false, message: "Subscription request not found." });
        }
        const sub = subs[0];
        // Ownership, not just role: a cook must not be able to answer another
        // cook's request by guessing an id.
        if (sub.cook_id !== cookId) {
            return res.status(403).json({ success: false, message: "This request isn't for your kitchen." });
        }
        if (sub.status !== "requested") {
            return res.status(409).json({
                success: false,
                message: `This request has already been answered — it's ${describeStatus(sub.status)}.`,
                status: sub.status
            });
        }

        const accepted = action === "accept";
        const newStatus = accepted ? "accepted" : "rejected";

        const [upd] = await db.promise().query(
            `UPDATE subscriptions
             SET status = ?, response_note = ?, responded_at = NOW()
             WHERE id = ? AND cook_id = ? AND status = 'requested'`,
            [newStatus, note, id, cookId]
        );
        if (upd.affectedRows === 0) {
            return res.status(409).json({ success: false, message: "That request was just answered from another screen. Pull to refresh." });
        }

        await logDayEvent({
            subscriptionId: id,
            event: accepted ? "request_accepted" : "request_rejected",
            actor: "cook",
            detail: note || (accepted ? "Cook accepted the request; awaiting payment proof." : "Cook rejected the request.")
        });

        const startDate = dateOnly(sub.start_date);
        const durationDays = getDurationDays(sub.duration);
        const amount = Number(sub.price_per_delivery) || null;
        const cookName = await nameOf(cookId);

        await announceSubscriptionEvent({
            io: req.app.get("io"),
            customerId: sub.customer_id, cookId,
            senderId: cookId, recipientId: sub.customer_id, senderName: cookName,
            cardType: CARD_TYPES.SUBSCRIPTION_UPDATE,
            cardText: accepted
                ? `Accepted your request for ${sub.plan_name} starting ${startDate}. Upload your payment proof to activate it.`
                : `Couldn't take on ${sub.plan_name} starting ${startDate}.${note ? " " + note : ""}`,
            metadata: subscriptionCardMeta({
                subscriptionId: Number(id), planName: sub.plan_name, duration: sub.duration,
                durationDays, amount, startDate, status: newStatus, cookName, note
            }),
            referenceId: Number(id), referenceType: "subscription",
            title: accepted ? "Subscription accepted" : "Subscription request declined",
            body: accepted
                ? `${cookName} accepted "${sub.plan_name}". Upload your payment proof to activate it.`
                : `${cookName} couldn't take on "${sub.plan_name}".${note ? " Reason: " + note : " You can request again with a different start date."}`,
            notifType: accepted ? "subscription_accepted" : "subscription_rejected"
        });

        return res.status(200).json({
            success: true,
            status: newStatus,
            message: accepted
                ? "Accepted. The customer has been asked to upload payment proof."
                : "Request declined and closed. The customer has been told."
        });
    } catch (error) {
        console.error("respondToSubscriptionRequest error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

// ===========================================================================
// 3. Customer submits payment proof
// ===========================================================================

/**
 * POST /api/subscriptions/:id/payment-proof — customer only, multipart (`proof`).
 *
 * One call rather than upload-then-post-the-URL: the SHA-256 is taken from the
 * bytes THIS server received, so the uniqueness check can't be defeated by a
 * client sending a hash that doesn't match the file. Order is deliberate —
 * hash and duplicate-check BEFORE the Cloudinary upload, so a rejected
 * duplicate never leaves an orphan asset behind.
 */
export const submitPaymentProof = async (req, res) => {
    try {
        const customerId = req.user.id;
        const { id } = req.params;

        if (!req.file || !req.file.buffer) {
            return res.status(400).json({ success: false, message: "A payment screenshot is required." });
        }

        const [subs] = await db.promise().query(
            `SELECT s.*, p.name AS plan_name, p.duration, p.price_per_delivery
             FROM subscriptions s JOIN subscription_plans p ON s.plan_id = p.id
             WHERE s.id = ?`,
            [id]
        );
        if (subs.length === 0) {
            return res.status(404).json({ success: false, message: "Subscription not found." });
        }
        const sub = subs[0];
        if (sub.customer_id !== customerId) {
            return res.status(403).json({ success: false, message: "This subscription does not belong to you." });
        }
        // Only from 'accepted' (or a rejected proof, which lands back there), plus
        // 'pending_verification' so a customer who uploaded the wrong image can
        // replace it while the cook still hasn't acted on it. Without that, the
        // first upload was final and the only remedy was the cook rejecting it.
        // 'requested' is explicitly refused: paying before the cook agrees is
        // exactly the situation this whole reordering exists to prevent.
        if (sub.status !== "accepted" && sub.status !== "pending_verification") {
            const hint = sub.status === "requested"
                ? "The cook hasn't accepted this request yet — don't pay until they do."
                : `This subscription is ${describeStatus(sub.status)}.`;
            return res.status(409).json({ success: false, message: `Can't submit payment proof right now. ${hint}` });
        }

        const hash = crypto.createHash("sha256").update(req.file.buffer).digest("hex");

        // Safeguard 2: the same image cannot pay for two subscriptions. Checked
        // explicitly so the customer gets a sentence instead of a 500 from the
        // UNIQUE index, but the index is still the real guarantee under a race.
        const [dupes] = await db.promise().query(
            "SELECT id, customer_id FROM subscriptions WHERE payment_screenshot_hash = ? AND id != ? LIMIT 1",
            [hash, id]
        );
        if (dupes.length > 0) {
            await logDayEvent({
                subscriptionId: id, event: "proof_duplicate_blocked", actor: "customer",
                detail: `Rejected a payment screenshot already used on subscription ${dupes[0].id}.`
            });
            return res.status(409).json({
                success: false,
                message: "This exact screenshot has already been used for another subscription. Please upload the receipt for this payment."
            });
        }

        const folder = `tiffincraft/subscription-proofs/${id}`;
        let uploaded;
        try {
            uploaded = await uploadToCloudinary(req.file.buffer, folder, {
                resource_type: "image",
                // No crop/fill: the cook has to read an amount and a timestamp off
                // this image, and a face-gravity square crop would cut them out.
                transformation: [{ width: 1600, height: 1600, crop: "limit" }, { quality: "auto:good" }]
            });
        } catch (err) {
            console.error("submitPaymentProof upload error:", err);
            return res.status(500).json({ success: false, message: "Couldn't upload that image. Please try again." });
        }

        const [upd] = await db.promise().query(
            `UPDATE subscriptions
             SET payment_screenshot_url = ?, payment_screenshot_hash = ?,
                 payment_status = 'submitted', status = 'pending_verification',
                 payment_submitted_at = NOW(), payment_rejection_reason = NULL,
                 payment_proof_attempts = payment_proof_attempts + 1
             WHERE id = ? AND customer_id = ? AND status IN ('accepted','pending_verification')`,
            [uploaded.secure_url, hash, id, customerId]
        );
        if (upd.affectedRows === 0) {
            return res.status(409).json({ success: false, message: "This subscription changed while uploading. Pull to refresh and try again." });
        }

        const amount = Number(sub.price_per_delivery) || null;
        await db.promise().query(
            `INSERT INTO subscription_payment_events (subscription_id, event, amount, detail)
             VALUES (?, 'proof_submitted', ?, ?)`,
            [id, amount, `Manual payment proof #${Number(sub.payment_proof_attempts) + 1} uploaded. sha256=${hash.slice(0, 16)}…`]
        );
        await logDayEvent({
            subscriptionId: id, event: "proof_submitted", actor: "customer",
            detail: `Payment proof uploaded; awaiting cook verification. Requested start ${dateOnly(sub.start_date)}.`
        });

        const customerName = await nameOf(customerId);
        await announceSubscriptionEvent({
            io: req.app.get("io"),
            customerId, cookId: sub.cook_id,
            senderId: customerId, recipientId: sub.cook_id, senderName: customerName,
            cardType: CARD_TYPES.SUBSCRIPTION_UPDATE,
            cardText: sub.status === "pending_verification"
                ? `Replaced the payment proof for ${sub.plan_name}. Please verify the new screenshot.`
                : `Uploaded payment proof for ${sub.plan_name}. Please verify it.`,
            metadata: subscriptionCardMeta({
                subscriptionId: Number(id), planName: sub.plan_name, duration: sub.duration,
                durationDays: getDurationDays(sub.duration), amount,
                startDate: dateOnly(sub.start_date), status: "pending_verification", customerName
            }),
            referenceId: Number(id), referenceType: "subscription",
            title: "Payment proof submitted",
            body: `${customerName} uploaded proof of payment for "${sub.plan_name}". Check it and verify or reject.`,
            notifType: "subscription_payment_submitted"
        });

        return res.status(200).json({
            success: true,
            message: "Payment proof submitted. The cook will verify it shortly.",
            status: "pending_verification",
            payment_screenshot_url: uploaded.secure_url
        });
    } catch (error) {
        console.error("submitPaymentProof error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

// ===========================================================================
// 4. Cook verifies or rejects the proof
// ===========================================================================

/**
 * PUT /api/subscriptions/:id/verify-proof — cook only.
 * Body: { action: 'verify' | 'reject', note? }
 *
 * ── Calendar-day model ────────────────────────────────────────────────────
 * end_date = start_date + (durationDays - 1). It is fixed HERE, once, and never
 * moves again. Skipping a day does not extend it: the subscription is a window
 * of calendar days, not a pool of meal credits. meals_total is still written
 * for reporting, but nothing schedules off meals_remaining any more.
 *
 * A start_date already in the past is CLAMPED to today rather than honoured —
 * otherwise the window would open on days that already went by and the cron
 * would owe deliveries nobody can cook.
 */
export const verifySubscriptionProof = async (req, res) => {
    try {
        const cookId = req.user.id;
        const { id } = req.params;
        const action = String(req.body.action || "").toLowerCase();
        const note = typeof req.body.note === "string" ? req.body.note.trim().slice(0, 300) : null;

        if (!["verify", "reject"].includes(action)) {
            return res.status(400).json({ success: false, message: "action must be 'verify' or 'reject'." });
        }

        const [subs] = await db.promise().query(
            `SELECT s.*, p.name AS plan_name, p.duration, p.price_per_delivery
             FROM subscriptions s JOIN subscription_plans p ON s.plan_id = p.id
             WHERE s.id = ?`,
            [id]
        );
        if (subs.length === 0) {
            return res.status(404).json({ success: false, message: "Subscription not found." });
        }
        const sub = subs[0];
        if (sub.cook_id !== cookId) {
            return res.status(403).json({ success: false, message: "This subscription isn't for your kitchen." });
        }
        if (sub.status !== "pending_verification" || sub.payment_status !== "submitted") {
            return res.status(409).json({
                success: false,
                message: `Nothing to verify — this subscription is ${describeStatus(sub.status)}.`,
                status: sub.status
            });
        }

        const cookName = await nameOf(cookId);
        const amount = Number(sub.price_per_delivery) || null;
        const durationDays = getDurationDays(sub.duration);

        // ── Reject path ──────────────────────────────────────────────────────
        // Rolls all the way back to 'accepted', which is a state the customer
        // can act from: they can upload a different screenshot immediately. The
        // hash is cleared too — otherwise a customer who mistakenly uploaded
        // the wrong receipt could never submit that image again on the correct
        // subscription. The URL is KEPT so the rejected image is still on
        // record for a dispute.
        if (action === "reject") {
            const [upd] = await db.promise().query(
                `UPDATE subscriptions
                 SET status = 'accepted', payment_status = 'rejected',
                     payment_screenshot_hash = NULL, payment_rejection_reason = ?
                 WHERE id = ? AND cook_id = ? AND status = 'pending_verification'`,
                [note || "The cook couldn't confirm this payment.", id, cookId]
            );
            if (upd.affectedRows === 0) {
                return res.status(409).json({ success: false, message: "This was just answered from another screen. Pull to refresh." });
            }

            await db.promise().query(
                `INSERT INTO subscription_payment_events (subscription_id, event, amount, detail)
                 VALUES (?, 'proof_rejected', ?, ?)`,
                [id, amount, `Cook ${cookId} rejected the payment proof. ${note || "No reason given."} Image kept at ${sub.payment_screenshot_url || "(none)"}`]
            );
            await logDayEvent({
                subscriptionId: id, event: "proof_rejected", actor: "cook",
                detail: note || "Cook rejected the payment proof; customer may resubmit."
            });

            await announceSubscriptionEvent({
                io: req.app.get("io"),
                customerId: sub.customer_id, cookId,
                senderId: cookId, recipientId: sub.customer_id, senderName: cookName,
                cardType: CARD_TYPES.SUBSCRIPTION_UPDATE,
                cardText: `Couldn't confirm your payment for ${sub.plan_name}.${note ? " " + note : ""} Please upload the correct screenshot.`,
                metadata: subscriptionCardMeta({
                    subscriptionId: Number(id), planName: sub.plan_name, duration: sub.duration,
                    durationDays, amount, startDate: dateOnly(sub.start_date),
                    status: "accepted", cookName, note
                }),
                referenceId: Number(id), referenceType: "subscription",
                title: "Payment proof rejected",
                body: `${cookName} couldn't confirm your payment for "${sub.plan_name}". ${note || "Please upload the correct screenshot."}`,
                notifType: "subscription_payment_rejected"
            });

            return res.status(200).json({
                success: true,
                status: "accepted",
                message: "Proof rejected. The customer can upload a new screenshot."
            });
        }

        // ── Verify path ──────────────────────────────────────────────────────
        const today = getNptToday();
        const requestedStart = dateOnly(sub.start_date);
        const clamped = !requestedStart || daysBetween(today, requestedStart) < 0;
        const effectiveStart = clamped ? today : requestedStart;
        const endDate = addDays(effectiveStart, durationDays - 1);
        const startsNow = effectiveStart === today;
        const newStatus = startsNow ? "active" : "scheduled";

        const [upd] = await db.promise().query(
            `UPDATE subscriptions
             SET status = ?, payment_status = 'verified', verified_by = ?, verified_at = NOW(),
                 start_date = ?, end_date = ?, next_delivery_date = ?,
                 meals_total = ?, meals_remaining = ?, payment_rejection_reason = NULL
             WHERE id = ? AND cook_id = ? AND status = 'pending_verification'`,
            [newStatus, cookId, effectiveStart, endDate, effectiveStart, durationDays, durationDays, id, cookId]
        );
        if (upd.affectedRows === 0) {
            return res.status(409).json({ success: false, message: "This was just answered from another screen. Pull to refresh." });
        }

        await db.promise().query(
            `INSERT INTO subscription_payment_events (subscription_id, event, amount, detail)
             VALUES (?, 'proof_verified', ?, ?)`,
            [id, amount, `Cook ${cookId} verified the payment proof manually. Window ${effectiveStart} → ${endDate} (${durationDays} calendar days).`]
        );
        await logDayEvent({
            subscriptionId: id, event: "proof_verified", actor: "cook",
            detail: `Payment verified. ${durationDays}-day window runs ${effectiveStart} → ${endDate}.${clamped ? " Requested start was in the past and was clamped to today." : ""}`
        });

        // Day one only. Every later day is created by the daily job.
        if (startsNow) {
            await applyDayStatus(db.promise(), {
                subscriptionId: Number(id), deliveryDate: today, status: DAY_STATUS.SCHEDULED,
                toggledBy: "system", reason: "First day of the subscription window."
            });
            // The 06:00 batch may already have run for today, so place day one's
            // order here rather than making the customer lose a paid day to the
            // cook's verification timing. Best-effort: a failure here must not
            // undo a verification the cook already confirmed — the daily job
            // retries the same date tomorrow morning.
            try {
                await placeDayOrder(db.promise(), {
                    id: Number(id),
                    plan_id: sub.plan_id,
                    customer_id: sub.customer_id,
                    cook_id: cookId,
                    delivery_address: sub.delivery_address,
                    price_per_delivery: sub.price_per_delivery
                }, today);
            } catch (err) {
                console.error(`verifySubscriptionProof: day-one order for ${id} failed:`, err.message);
            }
        }

        await announceSubscriptionEvent({
            io: req.app.get("io"),
            customerId: sub.customer_id, cookId,
            senderId: cookId, recipientId: sub.customer_id, senderName: cookName,
            cardType: CARD_TYPES.SUBSCRIPTION_UPDATE,
            cardText: startsNow
                ? `Payment confirmed — ${sub.plan_name} is active today through ${endDate}.`
                : `Payment confirmed — ${sub.plan_name} starts ${effectiveStart} and runs to ${endDate}.`,
            metadata: subscriptionCardMeta({
                subscriptionId: Number(id), planName: sub.plan_name, duration: sub.duration,
                durationDays, amount, startDate: effectiveStart, endDate,
                status: newStatus, cookName
            }),
            referenceId: Number(id), referenceType: "subscription",
            title: "Payment verified",
            body: startsNow
                ? `Your "${sub.plan_name}" subscription is active. It runs through ${endDate}.`
                : `Your "${sub.plan_name}" subscription is confirmed and starts ${effectiveStart}.`,
            notifType: "subscription_verified"
        });

        return res.status(200).json({
            success: true,
            status: newStatus,
            message: startsNow
                ? "Payment verified — this subscription is active and today's delivery is scheduled."
                : `Payment verified — deliveries start ${effectiveStart}. Nothing to do until then.`,
            subscription: {
                id: Number(id), status: newStatus,
                start_date: effectiveStart, end_date: endDate,
                duration_days: durationDays, start_date_clamped: clamped
            }
        });
    } catch (error) {
        console.error("verifySubscriptionProof error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

// ===========================================================================
// 5. Cook's request inbox
// ===========================================================================

/**
 * GET /api/subscriptions/cook/requests?filter=pending|proof|answered|all
 *
 * The cook's "Subscription Requests" screen. Deliberately a separate endpoint
 * from getCookSubscribers: that one is about people already being cooked for,
 * this one is about decisions waiting on the cook. `pending` (the default)
 * means BOTH kinds of decision — a request to answer and a proof to check —
 * because from the cook's side they are the same job: something is blocked on
 * them.
 */
export const getCookSubscriptionRequests = async (req, res) => {
    try {
        const cookId = req.user.id;
        const filter = String(req.query.filter || "pending").toLowerCase();

        const statusSets = {
            pending: ["requested", "pending_verification"],
            requested: ["requested"],
            proof: ["pending_verification"],
            answered: ["accepted", "rejected", "verified", "scheduled", "active"],
            all: ["requested", "accepted", "rejected", "pending_verification", "verified", "scheduled", "active"]
        };
        const statuses = statusSets[filter] || statusSets.pending;

        const [rows] = await db.promise().query(
            `SELECT s.id, s.status, s.payment_status, s.request_note, s.response_note,
                    s.payment_screenshot_url, s.payment_rejection_reason, s.payment_proof_attempts,
                    DATE_FORMAT(s.start_date, '%Y-%m-%d')        AS start_date,
                    DATE_FORMAT(s.end_date, '%Y-%m-%d')          AS end_date,
                    s.delivery_address, s.requested_at, s.responded_at, s.payment_submitted_at,
                    u.id AS customer_id, u.full_name AS customer_name,
                    u.phone AS customer_phone, u.profile_image AS customer_image,
                    p.id AS plan_id, p.name AS plan_name, p.duration, p.price_per_delivery AS amount,
                    c.id AS conversation_id
             FROM subscriptions s
             JOIN users u ON u.id = s.customer_id
             JOIN subscription_plans p ON p.id = s.plan_id
             LEFT JOIN conversations c ON c.customer_id = s.customer_id AND c.cook_id = s.cook_id
             WHERE s.cook_id = ? AND s.status IN (?)
             ORDER BY FIELD(s.status, 'pending_verification', 'requested') DESC, s.id DESC`,
            [cookId, statuses]
        );

        const requests = rows.map(r => ({
            ...r,
            amount: r.amount === null ? null : Number(r.amount),
            duration_days: getDurationDays(r.duration),
            needs_decision: r.status === "requested",
            needs_payment_check: r.status === "pending_verification",
            ...stageFor(r)
        }));

        // Counts are computed for the whole inbox, not the filtered slice, so the
        // chips can show totals while the list shows one filter.
        const [[counts]] = await db.promise().query(
            `SELECT
                SUM(status = 'requested')            AS requested,
                SUM(status = 'pending_verification') AS awaiting_proof_check,
                SUM(status = 'accepted')             AS awaiting_payment
             FROM subscriptions WHERE cook_id = ?`,
            [cookId]
        );

        return res.status(200).json({
            success: true,
            filter,
            counts: {
                requested: Number(counts?.requested || 0),
                awaiting_proof_check: Number(counts?.awaiting_proof_check || 0),
                awaiting_payment: Number(counts?.awaiting_payment || 0)
            },
            requests
        });
    } catch (error) {
        console.error("getCookSubscriptionRequests error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

/**
 * GET /api/subscriptions/:id/detail — customer or cook.
 * Everything either side needs for the status screen, in one round trip:
 * the stage line, the plan's meals, the proof image, and the audit trail.
 */
export const getSubscriptionDetail = async (req, res) => {
    try {
        const userId = req.user.id;
        const { id } = req.params;

        const [rows] = await db.promise().query(
            `SELECT s.id, s.customer_id, s.cook_id, s.plan_id, s.status, s.payment_status,
                    s.delivery_address, s.request_note, s.response_note,
                    s.payment_screenshot_url, s.payment_rejection_reason, s.payment_proof_attempts,
                    DATE_FORMAT(s.start_date, '%Y-%m-%d')        AS start_date,
                    DATE_FORMAT(s.end_date, '%Y-%m-%d')          AS end_date,
                    DATE_FORMAT(s.next_delivery_date, '%Y-%m-%d') AS next_delivery_date,
                    s.requested_at, s.responded_at, s.payment_submitted_at, s.verified_at,
                    p.name AS plan_name, p.duration, p.price_per_delivery AS amount, p.description,
                    cust.full_name AS customer_name, cust.phone AS customer_phone,
                    cook.full_name AS cook_name, cook.phone AS cook_phone,
                    cp.bank_details AS cook_bank_details,
                    c.id AS conversation_id
             FROM subscriptions s
             JOIN subscription_plans p ON p.id = s.plan_id
             JOIN users cust ON cust.id = s.customer_id
             JOIN users cook ON cook.id = s.cook_id
             LEFT JOIN cook_profiles cp ON cp.user_id = s.cook_id
             LEFT JOIN conversations c ON c.customer_id = s.customer_id AND c.cook_id = s.cook_id
             WHERE s.id = ?`,
            [id]
        );
        if (rows.length === 0) {
            return res.status(404).json({ success: false, message: "Subscription not found." });
        }
        const sub = rows[0];
        if (sub.customer_id !== userId && sub.cook_id !== userId) {
            return res.status(403).json({ success: false, message: "Access denied." });
        }

        const [items] = await db.promise().query(
            `SELECT m.id, m.name, m.image_url, spi.quantity
             FROM subscription_plan_items spi JOIN meals m ON m.id = spi.meal_id
             WHERE spi.plan_id = ?`,
            [sub.plan_id]
        );

        // One audit table for the whole lifecycle — logDayEvent and the money
        // events both write here, so a dispute reads as a single timeline.
        const [events] = await db.promise().query(
            `SELECT event, amount, detail, created_at
             FROM subscription_payment_events
             WHERE subscription_id = ? ORDER BY id DESC LIMIT 30`,
            [id]
        );

        const durationDays = getDurationDays(sub.duration);
        const today = getNptToday();
        // The window can have GROWN: every day skipped in advance pushes end_date
        // out by one. Report the real length, otherwise the screen shows "7 days"
        // over a window that now spans nine and the progress bar tops out early.
        const windowDays = sub.start_date && sub.end_date
            ? Math.max(durationDays, daysBetween(sub.start_date, sub.end_date) + 1)
            : durationDays;

        // The cook's eSewa QR, in the same field name and shape the order detail
        // and legacy subscription endpoints already expose, so all three manual-pay
        // screens consume one contract. bank_details is a JSON blob; tolerate rows
        // that are null (cook never set one up) or malformed.
        let cookEsewaQrUrl = null;
        try {
            if (sub.cook_bank_details) cookEsewaQrUrl = JSON.parse(sub.cook_bank_details).esewa_qr_url || null;
        } catch (_) { /* legacy/malformed JSON — no QR */ }
        const { cook_bank_details, ...subFields } = sub;

        return res.status(200).json({
            success: true,
            viewer: sub.cook_id === userId ? "cook" : "customer",
            today,
            subscription: {
                ...subFields,
                amount: sub.amount === null ? null : Number(sub.amount),
                duration_days: windowDays,
                plan_days: durationDays,
                days_elapsed: sub.start_date ? Math.max(0, Math.min(windowDays, daysBetween(sub.start_date, today) + 1)) : 0,
                cook_esewa_qr_url: cookEsewaQrUrl,
                ...stageFor(sub)
            },
            meals: items,
            events
        });
    } catch (error) {
        console.error("getSubscriptionDetail error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

export default {
    createSubscriptionRequest,
    respondToSubscriptionRequest,
    submitPaymentProof,
    verifySubscriptionProof,
    getCookSubscriptionRequests,
    getSubscriptionDetail,
    stageFor
};
