import db from "../config/db.js";
import {
    deleteFromCloudinary,
    extractPublicId
} from "../services/uploadService.js";
import { createNotification } from "../utils/notificationHelper.js";
import { isOnline } from "../utils/onlineUsers.js";

/**
 * Normalize a chat_messages row for JSON clients.
 * MySQL BOOLEAN/TINYINT comes out as 0/1; Android Gson fails to map that onto
 * a Java boolean, which breaks load/send even when the row was saved correctly.
 */
const normalizeMessage = (row) => {
    if (!row) return row;
    return {
        ...row,
        is_read: row.is_read === true || row.is_read === 1 || row.is_read === "1",
        is_edited: !!(row.edited_at),
        is_deleted: row.is_deleted === true || row.is_deleted === 1 || row.is_deleted === "1"
    };
};

/**
 * Best-effort Cloudinary cleanup for chat image/video URLs.
 */
const deleteChatMediaFromCloudinary = async (mediaUrl, messageType) => {
    if (!mediaUrl || typeof mediaUrl !== "string") return;
    if (!mediaUrl.includes("cloudinary.com")) return;

    const publicId = extractPublicId(mediaUrl);
    if (!publicId) return;

    const resourceType = messageType === "video" ? "video" : "image";
    try {
        await deleteFromCloudinary(publicId, resourceType);
    } catch (err) {
        console.warn("Cloudinary chat media delete failed:", err.message);
        // Fallback: try the other resource type once
        try {
            const fallback = resourceType === "video" ? "image" : "video";
            await deleteFromCloudinary(publicId, fallback);
        } catch (err2) {
            console.warn("Cloudinary fallback delete failed:", err2.message);
        }
    }
};

/**
 * Verify the authenticated user is a participant of the conversation.
 * Returns the conversation row or null.
 */
const getConversationForUser = async (conversationId, userId) => {
    const [rows] = await db.promise().query(
        `SELECT id, customer_id, cook_id
         FROM conversations
         WHERE id = ? AND (customer_id = ? OR cook_id = ?)`,
        [conversationId, userId, userId]
    );
    return rows.length > 0 ? rows[0] : null;
};

/**
 * GET /chat/conversations
 * List all conversations for the authenticated user with the other
 * participant's info, last message preview, and unread count.
 */
export const getConversations = async (req, res) => {
    try {
        const userId = req.user.id;
        const search = typeof req.query.search === "string" ? req.query.search.trim() : "";
        const like = `%${search}%`;

        const [rows] = await db.promise().query(
            `SELECT
                c.id,
                c.customer_id,
                c.cook_id,
                c.last_message_at,
                u.id            AS other_user_id,
                u.full_name     AS other_user_name,
                u.profile_image AS other_user_image,
                u.role          AS other_user_role,
                u.phone         AS other_user_phone,
                lm.message_type AS last_message_type,
                lm.content      AS last_message_content,
                lm.sender_id    AS last_message_sender_id,
                lm.is_deleted   AS last_message_deleted,
                (SELECT COUNT(*) FROM chat_messages m
                 WHERE m.conversation_id = c.id
                   AND m.is_read = FALSE
                   AND m.sender_id != ?) AS unread_count
             FROM conversations c
             JOIN users u
               ON u.id = IF(c.customer_id = ?, c.cook_id, c.customer_id)
             LEFT JOIN chat_messages lm ON lm.id = c.last_message_id
             WHERE (c.customer_id = ? OR c.cook_id = ?)
               AND (? = '' OR u.full_name LIKE ?)
             ORDER BY c.last_message_at IS NULL, c.last_message_at DESC`,
            [userId, userId, userId, userId, search, like]
        );

        const conversations = rows.map(row => ({
            ...row,
            is_online: isOnline(row.other_user_id)
        }));

        return res.status(200).json({
            success: true,
            conversations
        });
    } catch (error) {
        console.error("getConversations error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

/**
 * POST /chat/conversations
 * Body: { other_user_id }
 * Get or create a conversation between the authenticated user and other_user_id.
 * One participant must be a customer and the other a cook.
 */
export const getOrCreateConversation = async (req, res) => {
    try {
        const userId = req.user.id;
        const userRole = req.user.role;
        const otherUserId = parseInt(req.body.other_user_id, 10);

        if (!otherUserId || otherUserId === userId) {
            return res.status(400).json({
                success: false,
                message: "A valid other_user_id is required."
            });
        }

        // Validate the other user exists and determine roles
        const [others] = await db.promise().query(
            "SELECT id, role FROM users WHERE id = ? AND is_active = TRUE",
            [otherUserId]
        );

        if (others.length === 0) {
            return res.status(404).json({
                success: false,
                message: "User not found."
            });
        }

        const otherRole = others[0].role;
        let customerId, cookId;

        if (userRole === "customer" && otherRole === "cook") {
            customerId = userId;
            cookId = otherUserId;
        } else if (userRole === "cook" && otherRole === "customer") {
            customerId = otherUserId;
            cookId = userId;
        } else {
            return res.status(400).json({
                success: false,
                message: "Conversations are only allowed between a customer and a cook."
            });
        }

        // Get existing or create new (atomic via unique key)
        await db.promise().query(
            `INSERT INTO conversations (customer_id, cook_id)
             VALUES (?, ?)
             ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id)`,
            [customerId, cookId]
        );

        const [rows] = await db.promise().query(
            `SELECT c.id, c.customer_id, c.cook_id, c.last_message_at,
                    u.id AS other_user_id, u.full_name AS other_user_name,
                    u.profile_image AS other_user_image, u.role AS other_user_role,
                    u.phone AS other_user_phone
             FROM conversations c
             JOIN users u ON u.id = ?
             WHERE c.customer_id = ? AND c.cook_id = ?`,
            [otherUserId, customerId, cookId]
        );

        return res.status(200).json({
            success: true,
            conversation: { ...rows[0], is_online: isOnline(otherUserId) }
        });
    } catch (error) {
        console.error("getOrCreateConversation error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

/**
 * GET /chat/conversations/:conversationId/messages?before_id=&limit=
 * Paginated messages, newest page first (use before_id to page back).
 */
export const getMessages = async (req, res) => {
    try {
        const userId = req.user.id;
        const conversationId = parseInt(req.params.conversationId, 10);
        const beforeId = parseInt(req.query.before_id, 10) || null;
        const limit = Math.min(parseInt(req.query.limit, 10) || 50, 100);

        const conversation = await getConversationForUser(conversationId, userId);
        if (!conversation) {
            return res.status(403).json({
                success: false,
                message: "Conversation not found or access denied."
            });
        }

        const baseSelect =
            `SELECT * FROM chat_messages
             WHERE conversation_id = ?`;
        const params = [conversationId];

        let whereExtra = "";
        if (beforeId) {
            whereExtra = " AND id < ?";
            params.push(beforeId);
        }

        const orderLimit = " ORDER BY id DESC LIMIT ?";
        params.push(limit);

        const [rows] = await db.promise().query(
            baseSelect + whereExtra + orderLimit,
            params
        );

        // Return in chronological order
        rows.reverse();

        return res.status(200).json({
            success: true,
            messages: rows.map(normalizeMessage),
            has_more: rows.length === limit
        });
    } catch (error) {
        console.error("getMessages error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

const fetchMessageById = async (messageId) => {
    const [rows] = await db.promise().query(
        `SELECT * FROM chat_messages WHERE id = ?`,
        [messageId]
    );
    return rows[0] || null;
};

/**
 * POST /chat/conversations/:conversationId/messages
 * Body: { message_type?, content?, call_duration_seconds? }
 * Persists a message and emits it over Socket.IO to the conversation room.
 */
export const sendMessage = async (req, res) => {
    try {
        const userId = req.user.id;
        const conversationId = parseInt(req.params.conversationId, 10);
        const messageType = req.body.message_type || "text";
        const content = typeof req.body.content === "string" ? req.body.content.trim() : null;
        const callDuration = Number.isInteger(req.body.call_duration_seconds)
            ? req.body.call_duration_seconds
            : null;

        const allowedTypes = ["text", "image", "video", "call_ended", "call_declined", "call_missed"];
        if (!allowedTypes.includes(messageType)) {
            return res.status(400).json({
                success: false,
                message: "Invalid message_type."
            });
        }

        if (messageType === "text" && (!content || content.length === 0)) {
            return res.status(400).json({
                success: false,
                message: "Message content is required."
            });
        }

        if ((messageType === "image" || messageType === "video") && (!content || content.length === 0)) {
            return res.status(400).json({
                success: false,
                message: "Media URL is required."
            });
        }

        if (content && content.length > 5000) {
            return res.status(400).json({
                success: false,
                message: "Message is too long (max 5000 characters)."
            });
        }

        const conversation = await getConversationForUser(conversationId, userId);
        if (!conversation) {
            return res.status(403).json({
                success: false,
                message: "Conversation not found or access denied."
            });
        }

        const [result] = await db.promise().query(
            `INSERT INTO chat_messages
                (conversation_id, sender_id, message_type, content, call_duration_seconds)
             VALUES (?, ?, ?, ?, ?)`,
            [conversationId, userId, messageType, content, callDuration]
        );

        const messageId = result.insertId;

        await db.promise().query(
            `UPDATE conversations
             SET last_message_id = ?, last_message_at = NOW()
             WHERE id = ?`,
            [messageId, conversationId]
        );

        const saved = await fetchMessageById(messageId);
        const message = normalizeMessage(saved);

        // Real-time delivery to the conversation room and recipient's user room
        const recipientId = conversation.customer_id === userId
            ? conversation.cook_id
            : conversation.customer_id;

        const [[sender]] = await db.promise().query(
            "SELECT full_name FROM users WHERE id = ?",
            [userId]
        );
        const senderName = sender ? sender.full_name : "Someone";
        const preview = messageType === "text"
            ? (content.length > 80 ? content.slice(0, 80) + "…" : content)
            : "Sent you an attachment";

        const io = req.app.get("io");
        if (io) {
            io.to(`chat_${conversationId}`).emit("newChatMessage", message);
            io.to(`user_${recipientId}`).emit("chatNotification", {
                conversation_id: conversationId,
                sender_name: senderName,
                preview,
                message
            });
        }

        // Persist a notification too, so it still shows up (bell icon, badge count)
        // if the recipient isn't connected to the socket when the message arrives.
        // The extra.pushData enables FCM deep-link to ChatActivity on Android.
        await createNotification(
            recipientId,
            `New message from ${senderName}`,
            preview,
            "chat_message",
            conversationId,
            "conversation",
            {
                pushData: {
                    type: "chat_message",
                    conversationId: String(conversationId),
                    senderName: senderName || "",
                    preview: preview || ""
                }
            }
        );

        return res.status(201).json({
            success: true,
            message: "Message sent.",
            data: message
        });
    } catch (error) {
        console.error("sendMessage error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

/**
 * PUT /chat/conversations/:conversationId/messages/:messageId
 * Body: { content }
 * Edit a text message you sent.
 */
export const editMessage = async (req, res) => {
    try {
        const userId = req.user.id;
        const conversationId = parseInt(req.params.conversationId, 10);
        const messageId = parseInt(req.params.messageId, 10);
        const content = typeof req.body.content === "string" ? req.body.content.trim() : "";

        if (!conversationId || !messageId) {
            return res.status(400).json({
                success: false,
                message: "Valid conversationId and messageId are required."
            });
        }

        if (!content) {
            return res.status(400).json({
                success: false,
                message: "Message content is required."
            });
        }

        if (content.length > 5000) {
            return res.status(400).json({
                success: false,
                message: "Message is too long (max 5000 characters)."
            });
        }

        const conversation = await getConversationForUser(conversationId, userId);
        if (!conversation) {
            return res.status(403).json({
                success: false,
                message: "Conversation not found or access denied."
            });
        }

        const row = await fetchMessageById(messageId);
        if (!row || row.conversation_id !== conversationId) {
            return res.status(404).json({
                success: false,
                message: "Message not found."
            });
        }

        if (row.sender_id !== userId) {
            return res.status(403).json({
                success: false,
                message: "You can only edit your own messages."
            });
        }

        if (row.is_deleted) {
            return res.status(400).json({
                success: false,
                message: "Cannot edit a deleted message."
            });
        }

        if (row.message_type !== "text") {
            return res.status(400).json({
                success: false,
                message: "Only text messages can be edited."
            });
        }

        if (row.content === content) {
            return res.status(200).json({
                success: true,
                message: "No changes.",
                data: normalizeMessage(row)
            });
        }

        try {
            await db.promise().query(
                `UPDATE chat_messages
                 SET content = ?, edited_at = NOW()
                 WHERE id = ? AND conversation_id = ? AND sender_id = ?`,
                [content, messageId, conversationId, userId]
            );
        } catch (err) {
            if (err && err.code === "ER_BAD_FIELD_ERROR") {
                await db.promise().query(
                    `UPDATE chat_messages
                     SET content = ?
                     WHERE id = ? AND conversation_id = ? AND sender_id = ?`,
                    [content, messageId, conversationId, userId]
                );
            } else {
                throw err;
            }
        }

        const saved = await fetchMessageById(messageId);
        const message = normalizeMessage(saved);
        // If schema has no edited_at, still flag as edited for clients
        if (!message.is_edited) {
            message.is_edited = true;
        }

        const io = req.app.get("io");
        if (io) {
            io.to(`chat_${conversationId}`).emit("chatMessageEdited", message);
        }

        return res.status(200).json({
            success: true,
            message: "Message updated.",
            data: message
        });
    } catch (error) {
        console.error("editMessage error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

/**
 * DELETE /chat/conversations/:conversationId/messages/:messageId
 * Delete a message (text/image/video) you sent. Removes Cloudinary media when applicable.
 */
export const deleteMessage = async (req, res) => {
    try {
        const userId = req.user.id;
        const conversationId = parseInt(req.params.conversationId, 10);
        const messageId = parseInt(req.params.messageId, 10);

        if (!conversationId || !messageId) {
            return res.status(400).json({
                success: false,
                message: "Valid conversationId and messageId are required."
            });
        }

        const conversation = await getConversationForUser(conversationId, userId);
        if (!conversation) {
            return res.status(403).json({
                success: false,
                message: "Conversation not found or access denied."
            });
        }

        const [existing] = await db.promise().query(
            `SELECT * FROM chat_messages
             WHERE id = ? AND conversation_id = ?`,
            [messageId, conversationId]
        );

        if (existing.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Message not found."
            });
        }

        const row = existing[0];
        if (row.sender_id !== userId) {
            return res.status(403).json({
                success: false,
                message: "You can only delete your own messages."
            });
        }

        if (row.is_deleted) {
            return res.status(200).json({
                success: true,
                message: "Message already deleted.",
                data: {
                    conversation_id: conversationId,
                    message_id: messageId
                }
            });
        }

        // Call log rows can also be deleted by the sender if desired;
        // primarily used for text/image/video.
        if (row.message_type === "image" || row.message_type === "video") {
            await deleteChatMediaFromCloudinary(row.content, row.message_type);
        }

        // Soft delete: keep the row so both participants see a
        // "This message was deleted" placeholder instead of a gap.
        await db.promise().query(
            `UPDATE chat_messages
             SET is_deleted = TRUE, content = NULL
             WHERE id = ? AND conversation_id = ? AND sender_id = ?`,
            [messageId, conversationId, userId]
        );

        const io = req.app.get("io");
        if (io) {
            io.to(`chat_${conversationId}`).emit("chatMessageDeleted", {
                conversation_id: conversationId,
                message_id: messageId
            });
        }

        return res.status(200).json({
            success: true,
            message: "Message deleted.",
            data: {
                conversation_id: conversationId,
                message_id: messageId
            }
        });
    } catch (error) {
        console.error("deleteMessage error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

/**
 * DELETE /chat/conversations/:conversationId/messages
 * Body: { message_ids: number[] }
 * Soft-delete multiple messages you sent in one request (bulk selection UI).
 */
export const deleteMessages = async (req, res) => {
    try {
        const userId = req.user.id;
        const conversationId = parseInt(req.params.conversationId, 10);
        const messageIds = Array.isArray(req.body.message_ids)
            ? req.body.message_ids
                  .map((id) => parseInt(id, 10))
                  .filter((id) => Number.isInteger(id) && id > 0)
            : [];

        if (!conversationId || messageIds.length === 0) {
            return res.status(400).json({
                success: false,
                message: "Valid conversationId and message_ids are required."
            });
        }

        const conversation = await getConversationForUser(conversationId, userId);
        if (!conversation) {
            return res.status(403).json({
                success: false,
                message: "Conversation not found or access denied."
            });
        }

        const [rows] = await db.promise().query(
            `SELECT id, message_type, content
             FROM chat_messages
             WHERE conversation_id = ? AND sender_id = ? AND is_deleted = FALSE AND id IN (?)`,
            [conversationId, userId, messageIds]
        );

        if (rows.length === 0) {
            return res.status(404).json({
                success: false,
                message: "No deletable messages found."
            });
        }

        const deletableIds = rows.map((r) => r.id);

        for (const row of rows) {
            if (row.message_type === "image" || row.message_type === "video") {
                await deleteChatMediaFromCloudinary(row.content, row.message_type);
            }
        }

        await db.promise().query(
            `UPDATE chat_messages
             SET is_deleted = TRUE, content = NULL
             WHERE conversation_id = ? AND sender_id = ? AND id IN (?)`,
            [conversationId, userId, deletableIds]
        );

        const io = req.app.get("io");
        if (io) {
            deletableIds.forEach((id) => {
                io.to(`chat_${conversationId}`).emit("chatMessageDeleted", {
                    conversation_id: conversationId,
                    message_id: id
                });
            });
        }

        return res.status(200).json({
            success: true,
            message: "Messages deleted.",
            data: {
                conversation_id: conversationId,
                deleted_ids: deletableIds
            }
        });
    } catch (error) {
        console.error("deleteMessages error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

/**
 * PUT /chat/conversations/:conversationId/read
 * Mark all messages from the other participant as read.
 */
export const markConversationRead = async (req, res) => {
    try {
        const userId = req.user.id;
        const conversationId = parseInt(req.params.conversationId, 10);

        const conversation = await getConversationForUser(conversationId, userId);
        if (!conversation) {
            return res.status(403).json({
                success: false,
                message: "Conversation not found or access denied."
            });
        }

        await db.promise().query(
            `UPDATE chat_messages
             SET is_read = TRUE
             WHERE conversation_id = ? AND sender_id != ? AND is_read = FALSE`,
            [conversationId, userId]
        );

        // Notify the other participant their messages were read
        const io = req.app.get("io");
        if (io) {
            io.to(`chat_${conversationId}`).emit("chatMessagesRead", {
                conversation_id: conversationId,
                reader_id: userId
            });
        }

        return res.status(200).json({
            success: true,
            message: "Conversation marked as read."
        });
    } catch (error) {
        console.error("markConversationRead error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

/**
 * GET /chat/unread-count
 * Total unread chat messages for the authenticated user (for badges).
 */
export const getUnreadCount = async (req, res) => {
    try {
        const userId = req.user.id;

        const [rows] = await db.promise().query(
            `SELECT COUNT(*) AS unread_count
             FROM chat_messages m
             JOIN conversations c ON c.id = m.conversation_id
             WHERE (c.customer_id = ? OR c.cook_id = ?)
               AND m.sender_id != ?
               AND m.is_read = FALSE`,
            [userId, userId, userId]
        );

        return res.status(200).json({
            success: true,
            unread_count: rows[0].unread_count
        });
    } catch (error) {
        console.error("getUnreadCount error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

/**
 * GET /chat/contacts
 * Users the authenticated user can start a chat with.
 * Customers see cooks they've ordered from; cooks see customers who ordered from them.
 */
export const getChatContacts = async (req, res) => {
    try {
        const userId = req.user.id;
        const userRole = req.user.role;
        const search = typeof req.query.search === "string" ? req.query.search.trim() : "";
        const like = `%${search}%`;

        let query;
        let params;

        if (userRole === "customer") {
            // A customer can start a chat with ANY active cook, not only cooks they
            // have already ordered from (that older rule left new customers with an
            // empty contact list and nobody to message).
            query =
                `SELECT u.id, u.full_name, u.profile_image, u.role, u.phone
                 FROM users u
                 WHERE u.role = 'cook' AND u.is_active = TRUE
                   AND (? = '' OR u.full_name LIKE ?)
                 ORDER BY u.full_name ASC
                 LIMIT 100`;
            params = [search, like];
        } else if (userRole === "cook") {
            // A cook sees customers who ordered from them or already have a
            // conversation open — union so neither source is missed.
            query =
                `SELECT DISTINCT u.id, u.full_name, u.profile_image, u.role, u.phone
                 FROM users u
                 WHERE u.role = 'customer' AND u.is_active = TRUE
                   AND (
                        u.id IN (SELECT o.customer_id FROM orders o WHERE o.cook_id = ?)
                     OR u.id IN (SELECT c.customer_id FROM conversations c WHERE c.cook_id = ?)
                   )
                   AND (? = '' OR u.full_name LIKE ?)
                 ORDER BY u.full_name ASC
                 LIMIT 100`;
            params = [userId, userId, search, like];
        } else {
            return res.status(403).json({
                success: false,
                message: "Chat is only available for customers and cooks."
            });
        }

        const [rows] = await db.promise().query(query, params);

        const contacts = rows.map(row => ({
            ...row,
            is_online: isOnline(row.id)
        }));

        return res.status(200).json({
            success: true,
            contacts
        });
    } catch (error) {
        console.error("getChatContacts error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};
