import db from "../config/db.js";
import { createNotification } from "./notificationHelper.js";

/**
 * Subscription events — the ONE place a lifecycle change is announced.
 *
 * The spec requires the in-app notification, the FCM push and the chat message
 * to fire from the same backend action so they can never disagree with each
 * other. This module is that action: every controller in the request-to-active
 * flow calls `announceSubscriptionEvent` exactly once after its DB write, and
 * gets all three channels from it.
 *
 *   in-app row + FCM push  ->  createNotification (notificationHelper)
 *   chat card              ->  a real chat_messages row in the pair's thread
 *   live delivery          ->  the same Socket.IO events ChatActivity already
 *                              listens for, so a card appears without a refresh
 *
 * Ordering matters: this is called AFTER the state change is committed, never
 * before. The subscriptions row is the source of truth; an announcement that
 * fails must not be able to roll back a transition the customer already saw
 * succeed, so each channel is attempted independently and failures are logged
 * rather than thrown.
 */

/** Chat message_types that carry a structured card. Mirrors the DB ENUM. */
export const CARD_TYPES = {
    SUBSCRIPTION_REQUEST: "subscription_request",
    SUBSCRIPTION_UPDATE: "subscription_update",
    CUSTOM_MEAL_REQUEST: "custom_meal_request"
};

/**
 * Find or create the one conversation between this customer and cook.
 *
 * Relies on the UNIQUE key on (customer_id, cook_id) plus
 * `ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id)` — the same trick
 * getOrCreateConversation uses — so two simultaneous requests can't create two
 * threads for the same pair.
 */
export const ensureConversation = async (customerId, cookId) => {
    const [result] = await db.promise().query(
        `INSERT INTO conversations (customer_id, cook_id)
         VALUES (?, ?)
         ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id)`,
        [customerId, cookId]
    );
    return result.insertId;
};

/**
 * Insert the card as a normal chat message and push it out live.
 * `metadata` is the snapshot rendered inside the bubble; `referenceId` points at
 * the live row so the app can re-read current status instead of trusting the
 * snapshot (see attachLiveCardState in chatController).
 */
const postCard = async ({
    io, conversationId, senderId, recipientId, senderName,
    cardType, text, metadata, referenceId, referenceType
}) => {
    const [result] = await db.promise().query(
        `INSERT INTO chat_messages
            (conversation_id, sender_id, message_type, content, metadata, reference_id, reference_type)
         VALUES (?, ?, ?, ?, ?, ?, ?)`,
        [conversationId, senderId, cardType, text,
            metadata ? JSON.stringify(metadata) : null, referenceId, referenceType]
    );
    const messageId = result.insertId;

    await db.promise().query(
        `UPDATE conversations SET last_message_id = ?, last_message_at = NOW() WHERE id = ?`,
        [messageId, conversationId]
    );

    const [rows] = await db.promise().query("SELECT * FROM chat_messages WHERE id = ?", [messageId]);
    const saved = rows[0];
    if (!saved) return null;

    // `live_status` in the same shape attachLiveCardState adds on a normal fetch.
    // Read from the referenced row rather than passed in by the caller, so the
    // card the socket delivers and the card a reload delivers can't disagree —
    // and so the receiving app decides whether to show buttons the same way in
    // both paths. Without it, a card arriving live had no status at all and
    // rendered as though the subscription were gone.
    let liveStatus = null;
    if (referenceId) {
        const table = referenceType === "custom_meal_request" ? "custom_meal_requests" : "subscriptions";
        try {
            const [ref] = await db.promise().query(
                `SELECT status FROM ${table} WHERE id = ?`, [referenceId]
            );
            liveStatus = ref[0] ? ref[0].status : null;
        } catch (err) {
            console.warn("postCard live_status read failed:", err.message);
        }
    }

    // Same normalisation chatController applies: TINYINT 0/1 would otherwise
    // reach Android as a number and fail to map onto a Java boolean.
    const message = {
        ...saved,
        is_read: false,
        is_edited: false,
        is_deleted: false,
        live_status: liveStatus
    };

    if (io) {
        io.to(`chat_${conversationId}`).emit("newChatMessage", message);
        io.to(`user_${recipientId}`).emit("chatNotification", {
            conversation_id: conversationId,
            sender_name: senderName || "TiffinCraft",
            preview: text,
            message
        });
    }
    return message;
};

/**
 * Announce one subscription lifecycle event on all three channels.
 *
 * @param {object}  o
 * @param {object}  o.io            Socket.IO server (req.app.get("io")), may be null
 * @param {number}  o.customerId    conversation participant
 * @param {number}  o.cookId        conversation participant
 * @param {number}  o.senderId      who the chat card is FROM (customerId or cookId)
 * @param {number}  o.recipientId   who gets the notification + push
 * @param {string}  o.senderName    display name for the push/socket preview
 * @param {string}  o.cardType      one of CARD_TYPES
 * @param {string}  o.cardText      plain-text fallback shown in the thread preview
 * @param {object}  o.metadata      card payload (see the *CardMeta builders below)
 * @param {number}  o.referenceId   subscriptions.id | custom_meal_requests.id
 * @param {string}  o.referenceType "subscription" | "custom_meal_request"
 * @param {string}  o.title         notification title
 * @param {string}  o.body          notification body
 * @param {string}  o.notifType     notifications.type
 * @param {object}  [o.pushData]    extra FCM data for deep-linking
 * @returns {Promise<{conversationId:number|null, messageId:number|null}>}
 */
export const announceSubscriptionEvent = async ({
    io, customerId, cookId, senderId, recipientId, senderName,
    cardType, cardText, metadata, referenceId, referenceType,
    title, body, notifType, pushData = {}
}) => {
    let conversationId = null;
    let messageId = null;

    // Channel 1+2 — in-app row and FCM push, both inside createNotification.
    // Attempted first because it is the channel the recipient is guaranteed to
    // see even with no chat history and no socket connection.
    try {
        await createNotification(recipientId, title, body, notifType, referenceId, referenceType, {
            pushData: {
                type: notifType,
                subscriptionId: String(referenceType === "subscription" ? referenceId : (metadata?.subscription_id ?? "")),
                referenceId: String(referenceId ?? ""),
                referenceType: referenceType || "",
                ...pushData
            }
        });
    } catch (err) {
        console.error("announceSubscriptionEvent: notification channel failed:", err.message);
    }

    // Channel 3 — the chat card.
    try {
        conversationId = await ensureConversation(customerId, cookId);
        const message = await postCard({
            io, conversationId, senderId, recipientId, senderName,
            cardType, text: cardText, metadata, referenceId, referenceType
        });
        messageId = message ? message.id : null;
    } catch (err) {
        console.error("announceSubscriptionEvent: chat channel failed:", err.message);
    }

    return { conversationId, messageId };
};

/**
 * Card payload for a subscription request / update.
 *
 * Only fields the bubble actually renders. `status` is a snapshot — the app
 * prefers the `live_*` fields chatController joins in, so an old card in the
 * scrollback never shows an Accept button for a request already answered.
 */
export const subscriptionCardMeta = ({
    subscriptionId, planName, duration, durationDays, amount,
    startDate, endDate, status, customerName, cookName, note, address
}) => ({
    subscription_id: subscriptionId,
    plan_name: planName || "Subscription plan",
    duration: duration || null,
    duration_days: durationDays ?? null,
    amount: amount ?? null,
    start_date: startDate || null,
    end_date: endDate || null,
    status: status || null,
    customer_name: customerName || null,
    cook_name: cookName || null,
    note: note || null,
    delivery_address: address || null
});

/** Card payload for a per-day custom meal request. */
export const customMealCardMeta = ({
    requestId, subscriptionId, deliveryDate, mealId, mealName, note, status, customerName
}) => ({
    request_id: requestId,
    subscription_id: subscriptionId,
    delivery_date: deliveryDate,
    meal_id: mealId ?? null,
    meal_name: mealName || "Customer's choice",
    note: note || null,
    status: status || "pending",
    customer_name: customerName || null
});

export default { announceSubscriptionEvent, ensureConversation, subscriptionCardMeta, customMealCardMeta, CARD_TYPES };
