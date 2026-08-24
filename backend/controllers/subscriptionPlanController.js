import db from "../config/db.js";

const DURATIONS = ["weekly", "2_weeks", "monthly", "1_week", "1_month"];

/** Validates the items array shared by create/update: non-empty, each meal owned
 *  by this cook, quantity a positive integer. Returns { error, menuTotal } — the
 *  total is what these items cost at full menu price, which the plan price has
 *  to undercut (see validatePlanPrice). */
async function validateItems(items, cookId) {
    if (!Array.isArray(items) || items.length === 0) {
        return { error: "At least one item is required.", menuTotal: 0 };
    }
    for (const item of items) {
        const qty = parseInt(item.quantity, 10);
        if (!item.meal_id || !Number.isInteger(qty) || qty <= 0) {
            return { error: "Each item needs a meal_id and a positive integer quantity.", menuTotal: 0 };
        }
    }
    const mealIds = items.map(i => parseInt(i.meal_id, 10));
    const [owned] = await db.promise().query(
        `SELECT id, price FROM meals WHERE cook_id = ? AND id IN (${mealIds.map(() => "?").join(",")})`,
        [cookId, ...mealIds]
    );
    if (owned.length !== new Set(mealIds).size) {
        return { error: "One or more items reference a meal that isn't yours.", menuTotal: 0 };
    }
    const priceById = new Map(owned.map(m => [m.id, parseFloat(m.price)]));
    const menuTotal = items.reduce(
        (sum, i) => sum + priceById.get(parseInt(i.meal_id, 10)) * parseInt(i.quantity, 10),
        0
    );
    return { error: null, menuTotal };
}

/**
 * A subscription is a single up-front payment, so its price has to be a real
 * discount on the items' combined menu price — otherwise the customer is paying
 * the same money for less flexibility. Enforced here and not only in the app,
 * because the app isn't a trust boundary.
 * Returns { error, price }.
 */
function validatePlanPrice(rawPrice, menuTotal) {
    if (rawPrice === undefined || rawPrice === null || rawPrice === "") {
        return { error: "A subscription price is required, and it must be lower than the items' combined menu price." };
    }
    const price = Number(rawPrice);
    if (Number.isNaN(price) || price <= 0) {
        return { error: "Subscription price must be a positive number." };
    }
    if (menuTotal > 0 && price >= menuTotal) {
        return {
            error: `Subscription price must be less than the items' combined menu price (₹${menuTotal.toFixed(0)}) — subscribers need a real discount.`
        };
    }
    return { error: null, price };
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
    // The plan's one-time subscription price. (The column is still named
    // price_per_delivery for schema-compatibility; it is now the single
    // up-front price for the plan, not a recurring per-delivery charge.)
    // Plans created before the price became mandatory can still have NULL here,
    // so fall back to the summed menu total for those instead of breaking them.
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
            return res.status(400).json({ success: false, message: "duration must be 'weekly' (1 week), '2_weeks' (2 weeks), or 'monthly' (1 month)." });
        }
        // Items first: the price rule is relative to what these items cost at
        // menu price, so there's nothing to check the price against until the
        // items are known-good.
        const { error: itemsError, menuTotal } = await validateItems(items, cookId);
        if (itemsError) {
            return res.status(400).json({ success: false, message: itemsError });
        }
        const { error: priceError, price: planPrice } = validatePlanPrice(price_per_delivery, menuTotal);
        if (priceError) {
            return res.status(400).json({ success: false, message: priceError });
        }

        const connection = await db.promise().getConnection();
        try {
            await connection.beginTransaction();

            const [result] = await connection.query(
                `INSERT INTO subscription_plans (cook_id, name, duration, description, price_per_delivery) VALUES (?, ?, ?, ?, ?)`,
                [cookId, name.trim(), duration, description || null, planPrice]
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
            "SELECT id, price_per_delivery FROM subscription_plans WHERE id = ? AND cook_id = ?",
            [id, cookId]
        );
        if (owned.length === 0) {
            return res.status(403).json({ success: false, message: "Plan not found or you don't have permission." });
        }

        if (duration !== undefined && !DURATIONS.includes(duration)) {
            return res.status(400).json({ success: false, message: "duration must be 'weekly' (1 week), '2_weeks' (2 weeks), or 'monthly' (1 month)." });
        }

        // The price rule is relative to the item set, so a change to either side
        // can break it. Resolve whichever side wasn't submitted from what's
        // already stored, then check the pair.
        let menuTotal = null;
        if (items !== undefined) {
            const validated = await validateItems(items, cookId);
            if (validated.error) {
                return res.status(400).json({ success: false, message: validated.error });
            }
            menuTotal = validated.menuTotal;
        }
        if (price_per_delivery !== undefined) {
            if (menuTotal === null) {
                const existing = await fetchPlanWithItems(id);
                menuTotal = existing ? existing.individual_total : 0;
            }
            const { error: priceError } = validatePlanPrice(price_per_delivery, menuTotal);
            if (priceError) {
                return res.status(400).json({ success: false, message: priceError });
            }
        } else if (menuTotal !== null && owned[0].price_per_delivery !== null) {
            // Items changed but the price didn't: swapping in cheaper meals can
            // leave the stored price at or above the new menu total, which would
            // silently turn the plan into a worse deal than ordering separately.
            const { error: priceError } = validatePlanPrice(owned[0].price_per_delivery, menuTotal);
            if (priceError) {
                return res.status(400).json({
                    success: false,
                    message: `${priceError} Lower the plan price in the same update as these items.`
                });
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
        // Clearing the price back to auto-summed is no longer allowed — a plan
        // priced at the menu total is not a subscription anyone benefits from,
        // and validatePlanPrice above already rejected "" / null.
        if (price_per_delivery !== undefined) {
            updates.push("price_per_delivery = ?");
            values.push(Number(price_per_delivery));
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
