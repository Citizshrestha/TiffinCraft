import db from "../config/db.js";

/** Validates the items array shared by create/update — same rules as
 *  subscription plans: non-empty, each meal owned by this cook, positive
 *  integer quantity. Returns an error string or null. */
async function validateItems(items, cookId) {
    if (!Array.isArray(items) || items.length === 0) {
        return "At least one item is required.";
    }
    for (const item of items) {
        const qty = parseInt(item.quantity, 10);
        if (!item.meal_id || !Number.isInteger(qty) || qty <= 0) {
            return "Each item needs a meal_id and a positive integer quantity.";
        }
    }
    const mealIds = items.map(i => parseInt(i.meal_id, 10));
    const [owned] = await db.promise().query(
        `SELECT id FROM meals WHERE cook_id = ? AND id IN (${mealIds.map(() => "?").join(",")})`,
        [cookId, ...mealIds]
    );
    if (owned.length !== new Set(mealIds).size) {
        return "One or more items reference a meal that isn't yours.";
    }
    return null;
}

async function fetchComboWithItems(comboId) {
    const [combos] = await db.promise().query(
        `SELECT c.*, u.full_name AS cook_name, cp.kitchen_name
         FROM combo_deals c
         JOIN users u ON c.cook_id = u.id
         JOIN cook_profiles cp ON c.cook_id = cp.user_id
         WHERE c.id = ?`,
        [comboId]
    );
    if (combos.length === 0) return null;

    const [items] = await db.promise().query(
        `SELECT cdi.id, cdi.meal_id, cdi.quantity, m.name, m.description, m.price, m.image_url, m.is_available
         FROM combo_deal_items cdi
         JOIN meals m ON cdi.meal_id = m.id
         WHERE cdi.combo_id = ?
         ORDER BY cdi.id ASC`,
        [comboId]
    );

    const formattedItems = items.map(i => ({ ...i, price: parseFloat(i.price), is_available: !!i.is_available }));
    const individualTotal = formattedItems.reduce((sum, i) => sum + i.price * i.quantity, 0);
    const combo = combos[0];
    // Same reasoning as subscription plans' is_available — lets the customer
    // see "Currently unavailable" up front instead of a Buy Now that 400s.
    const allItemsAvailable = formattedItems.length > 0 && formattedItems.every(i => i.is_available);

    return {
        ...combo,
        is_active: !!combo.is_active,
        price: parseFloat(combo.price),
        items: formattedItems,
        // What the items would cost bought separately — lets the UI show
        // "Save ₹X" instead of just a flat price with no context.
        individual_total: individualTotal,
        savings: Math.max(0, individualTotal - parseFloat(combo.price)),
        is_available: !!combo.is_active && allItemsAvailable
    };
}

// POST /api/combos — cook creates a combo deal
export const createCombo = async (req, res) => {
    try {
        const cookId = req.user.id;
        const { name, description, price, items } = req.body;

        if (!name || !name.trim()) {
            return res.status(400).json({ success: false, message: "Combo name is required." });
        }
        const priceNum = Number(price);
        if (!price || Number.isNaN(priceNum) || priceNum <= 0) {
            return res.status(400).json({ success: false, message: "price must be a positive number." });
        }
        const itemsError = await validateItems(items, cookId);
        if (itemsError) {
            return res.status(400).json({ success: false, message: itemsError });
        }

        const connection = await db.promise().getConnection();
        try {
            await connection.beginTransaction();

            const [result] = await connection.query(
                `INSERT INTO combo_deals (cook_id, name, description, price) VALUES (?, ?, ?, ?)`,
                [cookId, name.trim(), description || null, priceNum]
            );
            const comboId = result.insertId;

            for (const item of items) {
                await connection.query(
                    `INSERT INTO combo_deal_items (combo_id, meal_id, quantity) VALUES (?, ?, ?)`,
                    [comboId, item.meal_id, parseInt(item.quantity, 10)]
                );
            }

            await connection.commit();
            const combo = await fetchComboWithItems(comboId);
            return res.status(201).json({ success: true, message: "Combo deal created.", combo });
        } catch (err) {
            await connection.rollback();
            throw err;
        } finally {
            connection.release();
        }
    } catch (error) {
        console.error("createCombo error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

// GET /api/combos/my — cook's own combos (active + inactive), for management
export const getMyCombos = async (req, res) => {
    try {
        const cookId = req.user.id;
        const [combos] = await db.promise().query(
            `SELECT id FROM combo_deals WHERE cook_id = ? ORDER BY created_at DESC`,
            [cookId]
        );
        const full = await Promise.all(combos.map(c => fetchComboWithItems(c.id)));
        return res.status(200).json({ success: true, combos: full });
    } catch (error) {
        console.error("getMyCombos error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

// GET /api/combos/cook/:cookId — PUBLIC: active combos for a cook's profile page
export const getCombosByCook = async (req, res) => {
    try {
        const { cookId } = req.params;
        const [combos] = await db.promise().query(
            `SELECT id FROM combo_deals WHERE cook_id = ? AND is_active = TRUE ORDER BY created_at DESC`,
            [cookId]
        );
        const full = await Promise.all(combos.map(c => fetchComboWithItems(c.id)));
        return res.status(200).json({ success: true, combos: full });
    } catch (error) {
        console.error("getCombosByCook error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

// GET /api/combos/:id — single combo (edit prefill / detail view)
export const getComboById = async (req, res) => {
    try {
        const combo = await fetchComboWithItems(req.params.id);
        if (!combo) {
            return res.status(404).json({ success: false, message: "Combo not found." });
        }
        return res.status(200).json({ success: true, combo });
    } catch (error) {
        console.error("getComboById error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

// PUT /api/combos/:id — cook updates name/description/price/is_active and/or replaces items
export const updateCombo = async (req, res) => {
    try {
        const cookId = req.user.id;
        const { id } = req.params;
        const { name, description, price, is_active, items } = req.body;

        const [owned] = await db.promise().query(
            "SELECT id FROM combo_deals WHERE id = ? AND cook_id = ?",
            [id, cookId]
        );
        if (owned.length === 0) {
            return res.status(403).json({ success: false, message: "Combo not found or you don't have permission." });
        }

        if (price !== undefined && (Number.isNaN(Number(price)) || Number(price) <= 0)) {
            return res.status(400).json({ success: false, message: "price must be a positive number." });
        }
        if (items !== undefined) {
            const itemsError = await validateItems(items, cookId);
            if (itemsError) {
                return res.status(400).json({ success: false, message: itemsError });
            }
        }

        const updates = [];
        const values = [];
        if (name !== undefined) {
            if (!name.trim()) {
                return res.status(400).json({ success: false, message: "Combo name cannot be empty." });
            }
            updates.push("name = ?"); values.push(name.trim());
        }
        if (description !== undefined) { updates.push("description = ?"); values.push(description); }
        if (price !== undefined) { updates.push("price = ?"); values.push(Number(price)); }
        if (is_active !== undefined) { updates.push("is_active = ?"); values.push(!!is_active); }

        const connection = await db.promise().getConnection();
        try {
            await connection.beginTransaction();

            if (updates.length > 0) {
                values.push(id);
                await connection.query(`UPDATE combo_deals SET ${updates.join(", ")} WHERE id = ?`, values);
            }

            if (items !== undefined) {
                await connection.query("DELETE FROM combo_deal_items WHERE combo_id = ?", [id]);
                for (const item of items) {
                    await connection.query(
                        `INSERT INTO combo_deal_items (combo_id, meal_id, quantity) VALUES (?, ?, ?)`,
                        [id, item.meal_id, parseInt(item.quantity, 10)]
                    );
                }
            }

            await connection.commit();
            const combo = await fetchComboWithItems(id);
            return res.status(200).json({ success: true, message: "Combo updated.", combo });
        } catch (err) {
            await connection.rollback();
            throw err;
        } finally {
            connection.release();
        }
    } catch (error) {
        console.error("updateCombo error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

// DELETE /api/combos/:id — cook deletes a combo. No "active subscriber" block
// needed like plans have — combos aren't recurring, so past orders just keep
// a NULL combo_id (ON DELETE SET NULL) once deleted; nothing to protect.
export const deleteCombo = async (req, res) => {
    try {
        const cookId = req.user.id;
        const { id } = req.params;

        const [owned] = await db.promise().query(
            "SELECT id FROM combo_deals WHERE id = ? AND cook_id = ?",
            [id, cookId]
        );
        if (owned.length === 0) {
            return res.status(403).json({ success: false, message: "Combo not found or you don't have permission." });
        }

        await db.promise().query("DELETE FROM combo_deals WHERE id = ?", [id]);
        return res.status(200).json({ success: true, message: "Combo deleted." });
    } catch (error) {
        console.error("deleteCombo error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

/**
 * POST /api/combos/:id/order — customer buys a combo right now.
 * Creates a single real order at the combo's own fixed price (which can be
 * less than summing its items — that's the deal) rather than routing through
 * the cart, since cart checkout always sums live meal prices and has no
 * concept of a fixed bundle override. The order then behaves exactly like
 * any other order — same COD/online-QR payment flow, same cook fulfillment
 * lifecycle — this endpoint's only job is originating it correctly priced.
 * Body: { delivery_address, payment_method, special_instructions }
 */
export const buyCombo = async (req, res) => {
    const connection = await db.promise().getConnection();
    try {
        const customerId = req.user.id;
        const { id } = req.params;
        const { delivery_address, payment_method, special_instructions } = req.body;

        if (!delivery_address) {
            return res.status(400).json({ success: false, message: "delivery_address is required." });
        }
        const validPaymentMethods = ["online"];
        const selectedPaymentMethod = payment_method || "online";
        if (!validPaymentMethods.includes(selectedPaymentMethod)) {
            return res.status(400).json({ success: false, message: "payment_method must be 'online'." });
        }

        const [combos] = await connection.query(
            "SELECT * FROM combo_deals WHERE id = ?",
            [id]
        );
        if (combos.length === 0) {
            return res.status(404).json({ success: false, message: "Combo not found." });
        }
        const combo = combos[0];
        if (!combo.is_active) {
            return res.status(400).json({ success: false, message: "This combo is no longer available." });
        }

        const [items] = await connection.query(
            `SELECT cdi.meal_id, cdi.quantity, m.price, m.name, m.is_available
             FROM combo_deal_items cdi JOIN meals m ON cdi.meal_id = m.id
             WHERE cdi.combo_id = ?`,
            [id]
        );
        const unavailable = items.filter(i => !i.is_available);
        if (unavailable.length > 0) {
            return res.status(400).json({
                success: false,
                message: `Some items in this combo are currently unavailable: ${unavailable.map(i => i.name).join(", ")}`
            });
        }

        await connection.beginTransaction();

        const [orderResult] = await connection.query(
            `INSERT INTO orders (customer_id, cook_id, total_amount, delivery_address, status, payment_method, payment_status, special_instructions, combo_id)
             VALUES (?, ?, ?, ?, 'pending', ?, 'pending', ?, ?)`,
            [customerId, combo.cook_id, combo.price, delivery_address, selectedPaymentMethod, special_instructions || null, combo.id]
        );
        const orderId = orderResult.insertId;

        for (const item of items) {
            await connection.query(
                `INSERT INTO order_items (order_id, meal_id, quantity, price_at_time) VALUES (?, ?, ?, ?)`,
                [orderId, item.meal_id, item.quantity, item.price]
            );
        }

        await connection.commit();

        return res.status(201).json({
            success: true,
            message: "Payment is required before the combo order is sent to the cook.",
            orderId,
            payment_method: selectedPaymentMethod
        });
    } catch (error) {
        await connection.rollback();
        console.error("buyCombo error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    } finally {
        connection.release();
    }
};
