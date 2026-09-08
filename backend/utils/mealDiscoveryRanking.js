const PRIOR_RATING = 4.0;
const PRIOR_REVIEW_WEIGHT = 5;

const number = value => Number(value) || 0;
const clamp01 = value => Math.max(0, Math.min(1, value));
const normalized = (value, maximum) => maximum > 0 ? clamp01(number(value) / maximum) : 0;
const isCoordinate = value => value !== null && value !== undefined && value !== ""
    && Number.isFinite(Number(value));

export function distanceKm(latitudeA, longitudeA, latitudeB, longitudeB) {
    if (![latitudeA, longitudeA, latitudeB, longitudeB].every(isCoordinate)) return null;
    const values = [latitudeA, longitudeA, latitudeB, longitudeB].map(Number);
    const [latA, lngA, latB, lngB] = values;
    const radians = degrees => degrees * Math.PI / 180;
    const deltaLat = radians(latB - latA);
    const deltaLng = radians(lngB - lngA);
    const a = Math.sin(deltaLat / 2) ** 2
        + Math.cos(radians(latA)) * Math.cos(radians(latB)) * Math.sin(deltaLng / 2) ** 2;
    return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

export function legacyCategorySlugs(category, cuisineType) {
    const value = String(category || "").toLowerCase();
    const cuisine = String(cuisineType || "").toLowerCase();
    const slugs = [];
    if (value.includes("nepal") || cuisine.includes("nepal")) slugs.push("nepali");
    if (value.includes("fast food") || value.includes("fast_food")) slugs.push("fast_food");
    if (value.includes("healthy")) slugs.push("healthy");
    if (value.includes("snack")) slugs.push("snacks");
    if (value.includes("lunch")) slugs.push("lunch");
    if (value.includes("dessert")) slugs.push("desserts");
    if (value.includes("breakfast")) slugs.push("breakfast");
    if (value.includes("dinner")) slugs.push("dinner");
    if (value.includes("beverage")) slugs.push("beverages");
    return slugs;
}

function mealCategories(meal) {
    return Array.isArray(meal.category_slugs) && meal.category_slugs.length > 0
        ? meal.category_slugs
        : legacyCategorySlugs(meal.category, meal.cuisine_type);
}

function mealTimeCategory(now) {
    const hour = Number(new Intl.DateTimeFormat("en-GB", {
        timeZone: "Asia/Kathmandu",
        hour: "2-digit",
        hourCycle: "h23"
    }).format(now));
    if (hour >= 5 && hour < 10) return "breakfast";
    if (hour >= 10 && hour < 15) return "lunch";
    if (hour >= 15 && hour < 18) return "snacks";
    if (hour >= 18 && hour < 23) return "dinner";
    return null;
}

/** Keeps score order while preventing one cook from filling an entire row. */
export function diversifyByCook(sortedMeals, maxConsecutive = 2) {
    const remaining = [...sortedMeals];
    const result = [];
    let lastCookId = null;
    let consecutive = 0;

    while (remaining.length > 0) {
        let index = 0;
        if (lastCookId !== null && consecutive >= maxConsecutive) {
            const alternative = remaining.findIndex(meal => meal.cook_id !== lastCookId);
            if (alternative >= 0) index = alternative;
        }
        const [meal] = remaining.splice(index, 1);
        if (meal.cook_id === lastCookId) {
            consecutive += 1;
        } else {
            lastCookId = meal.cook_id;
            consecutive = 1;
        }
        result.push(meal);
    }
    return result;
}

export function rankMealDiscovery(meals, context = {}, now = new Date()) {
    const candidates = Array.isArray(meals) ? meals : [];
    const categoryPreferences = context.categoryPreferences || {};
    const cookAffinity = context.cookAffinity || {};
    const favoriteCookIds = new Set(context.favoriteCookIds || []);
    const hasLocation = [context.latitude, context.longitude].every(isCoordinate);
    const personalized = Object.values(categoryPreferences).some(number)
        || Object.values(cookAffinity).some(number)
        || favoriteCookIds.size > 0;

    const maxRecent = Math.max(0, ...candidates.map(meal => number(meal.recent_order_units)));
    const maxCompleted = Math.max(0, ...candidates.map(meal => number(meal.completed_order_units)));
    const maxFavorites = Math.max(0, ...candidates.map(meal => number(meal.cook_favorite_count)));
    const maxCategoryPreference = Math.max(0, ...Object.values(categoryPreferences).map(number));
    const maxCookAffinity = Math.max(0, ...Object.values(cookAffinity).map(number));
    const currentMealTime = mealTimeCategory(now);

    const scored = candidates.map(meal => {
        const reviewCount = number(meal.cook_review_count);
        const rating = number(meal.cook_rating);
        const adjustedRating = ((rating * reviewCount) + (PRIOR_RATING * PRIOR_REVIEW_WEIGHT))
            / (reviewCount + PRIOR_REVIEW_WEIGHT);
        const distance = hasLocation
            ? distanceKm(context.latitude, context.longitude, meal.latitude, meal.longitude)
            : null;
        const proximity = distance === null ? 0.5 : 1 / (1 + distance / 5);
        const preparation = number(meal.preparation_time) || 30;
        const operational = 1 / (1 + preparation / 30);
        const popularity =
            0.45 * normalized(meal.recent_order_units, maxRecent)
            + 0.20 * normalized(meal.completed_order_units, maxCompleted)
            + 0.15 * clamp01(adjustedRating / 5)
            + 0.10 * proximity
            + 0.05 * normalized(meal.cook_favorite_count, maxFavorites)
            + 0.05 * operational;

        const categories = mealCategories(meal);
        const categoryMatch = Math.max(0, ...categories.map(slug =>
            normalized(categoryPreferences[slug], maxCategoryPreference)));
        const affinity = normalized(cookAffinity[meal.cook_id], maxCookAffinity);
        const favorite = favoriteCookIds.has(meal.cook_id) ? 1 : 0;
        const timeMatch = currentMealTime && categories.includes(currentMealTime) ? 1 : 0;
        const mainMeal = categories.some(slug =>
            ["breakfast", "lunch", "dinner", "nepali", "healthy"].includes(slug)) ? 1 : 0;

        const recommendation = personalized
            ? 0.35 * categoryMatch + 0.20 * favorite + 0.10 * affinity
                + 0.20 * popularity + 0.10 * proximity + 0.05 * timeMatch
            : 0.55 * popularity + 0.20 * timeMatch + 0.15 * mainMeal + 0.10 * proximity;

        return {
            ...meal,
            is_favorite: favorite === 1,
            distance_km: distance === null ? null : Math.round(distance * 10) / 10,
            _popular_score: popularity,
            _recommendation_score: recommendation
        };
    });

    const stableSort = (scoreField) => [...scored].sort((a, b) =>
        b[scoreField] - a[scoreField]
        || number(b.recent_order_units) - number(a.recent_order_units)
        || number(b.completed_order_units) - number(a.completed_order_units)
        || number(a.id) - number(b.id));

    const rawPopular = stableSort("_popular_score");
    const bestSellerIds = new Set(rawPopular
        .filter(meal => number(meal.completed_order_units) > 0)
        .slice(0, 3)
        .map(meal => meal.id));
    const popular = diversifyByCook(rawPopular, 2);
    const popularTopIds = new Set(popular.slice(0, 3).map(meal => meal.id));

    // A small overlap penalty keeps the two home rows useful without hiding
    // strong matches when the catalogue is small or a category is selected.
    const rawRecommended = scored
        .map(meal => ({
            ...meal,
            _recommendation_score: meal._recommendation_score
                - (popularTopIds.has(meal.id) ? 0.08 : 0)
        }))
        .sort((a, b) =>
            b._recommendation_score - a._recommendation_score
            || b._popular_score - a._popular_score
            || number(a.id) - number(b.id));
    const recommended = diversifyByCook(rawRecommended, 1);

    const clean = meal => {
        const { _popular_score, _recommendation_score, ...publicMeal } = meal;
        return {
            ...publicMeal,
            is_bestseller: bestSellerIds.has(meal.id)
        };
    };

    return {
        personalized: Boolean(personalized),
        locationApplied: Boolean(hasLocation),
        popular: popular.map(clean),
        recommended: recommended.map(clean)
    };
}
