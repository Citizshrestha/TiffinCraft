import db from "../config/db.js";

const DURATIONS = ["weekly", "monthly"];

/** Validates the items array shared by create/update: non-empty, each meal owned
 *  by this cook, quantity a positive integer. Returns an error string or null. */
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

async function fetchPlanWithItems(planId) {
    const [plans] = await db.promise().query(
        `SELECT p.*, u.full_name AS cook_name, cp.kitchen_name
         FROM subscription_plans p
         JOIN users u ON p.cook_id = u.id
         JOIN cook_profiles cp ON p.cook_id = cp.user_id
         WHERE p.id = ?`,
        [planId]
    );
    if (plans.length === 0) return null;

    const [items] = await db.promise().query(
        `SELECT spi.id, spi.meal_id, spi.quantity, m.name, m.description, m.price, m.image_url, m.is_available
         FROM subscription_plan_items spi
         JOIN meals m ON spi.meal_id = m.id
         WHERE spi.plan_id = ?
         ORDER BY spi.id ASC`,
        [planId]
    );

    const formattedItems = items.map(i => ({ ...i, price: parseFloat(i.price), is_available: !!i.is_available }));
    const individualTotal = formattedItems.reduce((sum, i) => sum + i.price * i.quantity, 0);
    // The actual per-delivery charge. If the cook set an explicit price (the
    // whole point of a subscription — a real discount for committing, not
    // just "the same items, but recurring"), use it; otherwise fall back to
    // the old auto-summed behavior so plans created before this existed keep
    // working exactly as they did.
    const pricePerDelivery = plans[0].price_per_delivery !== null
        ? parseFloat(plans[0].price_per_delivery)
        : individualTotal;
    // Whether this plan can actually be subscribed to / delivered right now —
    // a plan can be "active" (cook hasn't paused it) yet still temporarily
    // unfulfillable because one of its meals is 86'd. Surfaced separately so
    // the UI can show "Currently unavailable" instead of a bare error after
    // the customer already tapped Subscribe.
    const allItemsAvailable = formattedItems.length > 0 && formattedItems.every(i => i.is_available);

    return {
        ...plans[0],
        is_active: !!plans[0].is_active,
        items: formattedItems,
        price_per_delivery: pricePerDelivery,
        // What the same items would cost ordered separately — lets the UI show
        // "Save ₹X vs ordering daily" instead of a bare price with no context,
        // same framing combo_deals already uses.
        individual_total: individualTotal,
        savings: Math.max(0, individualTotal - pricePerDelivery),
        // Whether the cook actually set a price, vs. it just being the
        // auto-summed fallback — savings alone can't tell these apart (a cook
        // could deliberately set a price >= the summed total), so the edit
        // form needs this explicitly to decide whether to prefill the field.
        has_custom_price: plans[0].price_per_delivery !== null,
        is_available: !!plans[0].is_active && allItemsAvailable
    };
}

// POST /api/subscription-plans — cook creates a plan
export const createPlan = async (req, res) => {
    try {
        const cookId = req.user.id;
        const { name, duration, description, items, price_per_delivery } = req.body;

        if (!name || !name.trim()) {
            return res.status(400).json({ success: false, message: "Plan name is required." });
        }
        if (!DURATIONS.includes(duration)) {
            return res.status(400).json({ success: false, message: "duration must be 'weekly' or 'monthly'." });
        }
        // Optional — a cook can leave this unset and let the price auto-sum
        // from item prices, but a real subscription only makes sense as a
        // deal, so the form strongly nudges toward setting it.
        let priceOverride = null;
        if (price_per_delivery !== undefined && price_per_delivery !== null && price_per_delivery !== "") {
            const priceNum = Number(price_per_delivery);
            if (Number.isNaN(priceNum) || priceNum <= 0) {
                return res.status(400).json({ success: false, message: "price_per_delivery must be a positive number." });
            }
            priceOverride = priceNum;
        }
        const itemsError = await validateItems(items, cookId);
        if (itemsError) {
            return res.status(400).json({ success: false, message: itemsError });
        }

        const connection = await db.promise().getConnection();
        try {
            await connection.beginTransaction();

            const [result] = await connection.query(
                `INSERT INTO subscription_plans (cook_id, name, duration, description, price_per_delivery) VALUES (?, ?, ?, ?, ?)`,
                [cookId, name.trim(), duration, description || null, priceOverride]
            );
            const planId = result.insertId;

            for (const item of items) {
                await connection.query(
                    `INSERT INTO subscription_plan_items (plan_id, meal_id, quantity) VALUES (?, ?, ?)`,
                    [planId, item.meal_id, parseInt(item.quantity, 10)]
                );
            }

            await connection.commit();
            const plan = await fetchPlanWithItems(planId);
            return res.status(201).json({ success: true, message: "Subscription plan created.", plan });
        } catch (err) {
            await connection.rollback();
            throw err;
        } finally {
            connection.release();
        }
    } catch (error) {
        console.error("createPlan error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

// GET /api/subscription-plans/my — cook's own plans (active + inactive), for management
export const getMyPlans = async (req, res) => {
    try {
        const cookId = req.user.id;
        const [plans] = await db.promise().query(
            `SELECT id FROM subscription_plans WHERE cook_id = ? ORDER BY created_at DESC`,
            [cookId]
        );
        const fullPlans = await Promise.all(plans.map(p => fetchPlanWithItems(p.id)));
        return res.status(200).json({ success: true, plans: fullPlans });
    } catch (error) {
        console.error("getMyPlans error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

// GET /api/subscription-plans/cook/:cookId — PUBLIC: active plans for a cook's profile page
export const getPlansByCook = async (req, res) => {
    try {
        const { cookId } = req.params;
        const [plans] = await db.promise().query(
            `SELECT id FROM subscription_plans WHERE cook_id = ? AND is_active = TRUE ORDER BY created_at DESC`,
            [cookId]
        );
        const fullPlans = await Promise.all(plans.map(p => fetchPlanWithItems(p.id)));
        return res.status(200).json({ success: true, plans: fullPlans });
    } catch (error) {
        console.error("getPlansByCook error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

// GET /api/subscription-plans/:id — single plan (used for edit prefill; public read otherwise harmless)
export const getPlanById = async (req, res) => {
    try {
        const plan = await fetchPlanWithItems(req.params.id);
        if (!plan) {
            return res.status(404).json({ success: false, message: "Plan not found." });
        }
        return res.status(200).json({ success: true, plan });
    } catch (error) {
        console.error("getPlanById error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

// PUT /api/subscription-plans/:id — cook updates name/duration/description/is_active and/or replaces items
export const updatePlan = async (req, res) => {
    try {
        const cookId = req.user.id;
        const { id } = req.params;
        const { name, duration, description, is_active, items, price_per_delivery } = req.body;

        const [owned] = await db.promise().query(
            "SELECT id FROM subscription_plans WHERE id = ? AND cook_id = ?",
            [id, cookId]
        );
        if (owned.length === 0) {
            return res.status(403).json({ success: false, message: "Plan not found or you don't have permission." });
        }

        if (duration !== undefined && !DURATIONS.includes(duration)) {
            return res.status(400).json({ success: false, message: "duration must be 'weekly' or 'monthly'." });
        }
        if (items !== undefined) {
            const itemsError = await validateItems(items, cookId);
            if (itemsError) {
                return res.status(400).json({ success: false, message: itemsError });
            }
        }
        if (price_per_delivery !== undefined && price_per_delivery !== null && price_per_delivery !== "") {
            const priceNum = Number(price_per_delivery);
            if (Number.isNaN(priceNum) || priceNum <= 0) {
                return res.status(400).json({ success: false, message: "price_per_delivery must be a positive number." });
            }
        }

        const updates = [];
        const values = [];
        if (name !== undefined) {
            if (!name.trim()) {
                return res.status(400).json({ success: false, message: "Plan name cannot be empty." });
            }
            updates.push("name = ?"); values.push(name.trim());
        }
        if (duration !== undefined) { updates.push("duration = ?"); values.push(duration); }
        if (description !== undefined) { updates.push("description = ?"); values.push(description); }
        if (is_active !== undefined) { updates.push("is_active = ?"); values.push(!!is_active); }
        // Sending "" or null explicitly clears the override back to auto-summed
        // pricing; omitting the field entirely (undefined) leaves it untouched —
        // same partial-update convention every other field here already uses.
        if (price_per_delivery !== undefined) {
            const cleared = price_per_delivery === null || price_per_delivery === "";
            updates.push("price_per_delivery = ?");
            values.push(cleared ? null : Number(price_per_delivery));
        }

        const connection = await db.promise().getConnection();
        try {
            await connection.beginTransaction();

            if (updates.length > 0) {
                values.push(id);
                await connection.query(`UPDATE subscription_plans SET ${updates.join(", ")} WHERE id = ?`, values);
            }

            if (items !== undefined) {
                await connection.query("DELETE FROM subscription_plan_items WHERE plan_id = ?", [id]);
                for (const item of items) {
                    await connection.query(
                        `INSERT INTO subscription_plan_items (plan_id, meal_id, quantity) VALUES (?, ?, ?)`,
                        [id, item.meal_id, parseInt(item.quantity, 10)]
                    );
                }
            }

            await connection.commit();
            const plan = await fetchPlanWithItems(id);
            return res.status(200).json({ success: true, message: "Plan updated.", plan });
        } catch (err) {
            await connection.rollback();
            throw err;
        } finally {
            connection.release();
        }
    } catch (error) {
        console.error("updatePlan error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

// DELETE /api/subscription-plans/:id — cook deletes a plan (blocked while it has live subscribers)
export const deletePlan = async (req, res) => {
    try {
        const cookId = req.user.id;
        const { id } = req.params;

        const [owned] = await db.promise().query(
            "SELECT id FROM subscription_plans WHERE id = ? AND cook_id = ?",
            [id, cookId]
        );
        if (owned.length === 0) {
            return res.status(403).json({ success: false, message: "Plan not found or you don't have permission." });
        }

        const [[{ activeCount }]] = await db.promise().query(
            "SELECT COUNT(*) AS activeCount FROM subscriptions WHERE plan_id = ? AND status != 'cancelled'",
            [id]
        );
        if (activeCount > 0) {
            return res.status(400).json({
                success: false,
                message: "This plan has active subscribers. Deactivate it instead of deleting, or wait until they cancel."
            });
        }

        await db.promise().query("DELETE FROM subscription_plans WHERE id = ?", [id]);
        return res.status(200).json({ success: true, message: "Plan deleted." });
    } catch (error) {
        console.error("deletePlan error:", error);
        return res.status(500).json({ success: false, message: "Server error.", error: error.message });
    }
};

export { fetchPlanWithItems };
