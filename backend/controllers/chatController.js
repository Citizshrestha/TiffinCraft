import db from "../config/db.js";

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
                lm.message_type AS last_message_type,
                lm.content      AS last_message_content,
                lm.sender_id    AS last_message_sender_id,
                (SELECT COUNT(*) FROM chat_messages m
                 WHERE m.conversation_id = c.id
                   AND m.is_read = FALSE
                   AND m.sender_id != ?) AS unread_count
             FROM conversations c
             JOIN users u
               ON u.id = IF(c.customer_id = ?, c.cook_id, c.customer_id)
             LEFT JOIN chat_messages lm ON lm.id = c.last_message_id
             WHERE c.customer_id = ? OR c.cook_id = ?
             ORDER BY c.last_message_at IS NULL, c.last_message_at DESC`,
            [userId, userId, userId, userId]
        );

        return res.status(200).json({
            success: true,
            conversations: rows
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
                    u.profile_image AS other_user_image, u.role AS other_user_role
             FROM conversations c
             JOIN users u ON u.id = ?
             WHERE c.customer_id = ? AND c.cook_id = ?`,
            [otherUserId, customerId, cookId]
        );

        return res.status(200).json({
            success: true,
            conversation: rows[0]
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

        let query =
            `SELECT id, conversation_id, sender_id, message_type, content,
                    call_duration_seconds, is_read, created_at
             FROM chat_messages
             WHERE conversation_id = ?`;
        const params = [conversationId];

        if (beforeId) {
            query += " AND id < ?";
            params.push(beforeId);
        }

        query += " ORDER BY id DESC LIMIT ?";
        params.push(limit);

        const [rows] = await db.promise().query(query, params);

        // Return in chronological order
        rows.reverse();

        return res.status(200).json({
            success: true,
            messages: rows,
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

        const allowedTypes = ["text", "call_ended", "call_declined", "call_missed"];
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

        const [rows] = await db.promise().query(
            `SELECT id, conversation_id, sender_id, message_type, content,
                    call_duration_seconds, is_read, created_at
             FROM chat_messages WHERE id = ?`,
            [messageId]
        );

        const message = rows[0];

        // Real-time delivery to the conversation room and recipient's user room
        const io = req.app.get("io");
        if (io) {
            const recipientId = conversation.customer_id === userId
                ? conversation.cook_id
                : conversation.customer_id;
            io.to(`chat_${conversationId}`).emit("newChatMessage", message);
            io.to(`user_${recipientId}`).emit("chatNotification", {
                conversation_id: conversationId,
                message
            });
        }

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

        let query;
        if (userRole === "customer") {
            query =
                `SELECT DISTINCT u.id, u.full_name, u.profile_image, u.role
                 FROM orders o
                 JOIN users u ON u.id = o.cook_id
                 WHERE o.customer_id = ? AND u.is_active = TRUE
                 ORDER BY u.full_name ASC`;
        } else if (userRole === "cook") {
            query =
                `SELECT DISTINCT u.id, u.full_name, u.profile_image, u.role
                 FROM orders o
                 JOIN users u ON u.id = o.customer_id
                 WHERE o.cook_id = ? AND u.is_active = TRUE
                 ORDER BY u.full_name ASC`;
        } else {
            return res.status(403).json({
                success: false,
                message: "Chat is only available for customers and cooks."
            });
        }

        const [rows] = await db.promise().query(query, [userId]);

        return res.status(200).json({
            success: true,
            contacts: rows
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
