/**
 * Nepal-Time day arithmetic for the subscription delivery calendar.
 *
 * Why this file exists separately from nepaliTime.js: that module answers
 * "which calendar MONTH does this timestamp belong to" for commission periods.
 * This one answers "which calendar DAY is it, and is that day still editable" —
 * the questions the daily-delivery log asks. It re-exports the offset from
 * nepaliTime.js rather than restating +05:45, so there is exactly one place in
 * the codebase that defines Nepal's offset.
 *
 * The one rule that matters here: EVERY day boundary is a Nepal Time boundary.
 * The Node process and TiDB Cloud both run UTC, so `new Date()`, `CURDATE()`
 * and `toISOString()` are all off by up to 5h45m — between 00:00 and 05:45 NPT
 * they name YESTERDAY. A customer skipping "tomorrow" at 00:30 NPT would
 * otherwise skip the wrong day. Nothing in the delivery-calendar code path may
 * use them; use getNptToday() / SQL_NPT_TODAY instead.
 */

import db from "../config/db.js";
import { getNptNow, toNpt } from "./nepaliTime.js";

export { getNptNow, toNpt };

/** Fallback used when platform_settings is unreadable. 20 = 8:00 PM NPT. */
export const DEFAULT_CUTOFF_HOUR = 20;

/**
 * SQL expression for "today" in Nepal. Use this in place of CURDATE(), which on
 * a UTC server returns yesterday's date for the first 5h45m of every Nepali day.
 */
export const SQL_NPT_TODAY = `DATE(${toNpt("UTC_TIMESTAMP()")})`;

/**
 * Normalise anything the DB or a request body might hand us into a plain
 * 'YYYY-MM-DD' string, or null if it isn't a usable date.
 *
 * mysql2 hydrates DATE columns into JS Date objects at LOCAL midnight, so
 * `.toISOString()` on one shifts the date backwards on any server east of UTC
 * and forwards on any server west of it. The local getters below recover the
 * date the DB actually stored. This is also the fix for the raw
 * "2026-09-03T00:00:00.000Z" text the app was rendering: the API now emits
 * date-only strings.
 */
export const dateOnly = (value) => {
    if (!value) return null;
    if (value instanceof Date) {
        if (Number.isNaN(value.getTime())) return null;
        return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, "0")}-${String(value.getDate()).padStart(2, "0")}`;
    }
    const str = String(value).trim();
    const match = str.match(/^(\d{4})-(\d{2})-(\d{2})/);
    return match ? `${match[1]}-${match[2]}-${match[3]}` : null;
};

/**
 * Strict validator for client-supplied dates: exactly 'YYYY-MM-DD', and a date
 * that really exists. Deliberately stricter than dateOnly() — that one is for
 * normalising trusted DB values, this one guards request bodies.
 */
export const isValidDateString = (value) => {
    if (typeof value !== "string" || !/^\d{4}-\d{2}-\d{2}$/.test(value.trim())) return false;
    const [y, m, d] = value.trim().split("-").map(Number);
    // Rejects 2026-02-30 and friends, which Date would silently roll over.
    const probe = new Date(Date.UTC(y, m - 1, d));
    return probe.getUTCFullYear() === y && probe.getUTCMonth() === m - 1 && probe.getUTCDate() === d;
};

/** Today's calendar date in Nepal, as 'YYYY-MM-DD'. */
export const getNptToday = () => {
    const n = getNptNow(); // UTC fields of this Date == NPT wall-clock fields
    return `${n.getUTCFullYear()}-${String(n.getUTCMonth() + 1).padStart(2, "0")}-${String(n.getUTCDate()).padStart(2, "0")}`;
};

/** 'YYYY-MM-DD' + n days, in pure calendar arithmetic (no DST anywhere in NPT). */
export const addDays = (dateStr, n) => {
    const iso = dateOnly(dateStr);
    if (!iso) return null;
    const [y, m, d] = iso.split("-").map(Number);
    const shifted = new Date(Date.UTC(y, m - 1, d + n));
    return `${shifted.getUTCFullYear()}-${String(shifted.getUTCMonth() + 1).padStart(2, "0")}-${String(shifted.getUTCDate()).padStart(2, "0")}`;
};

/** Tomorrow in Nepal — the earliest date that is ever editable. */
export const getNptTomorrow = () => addDays(getNptToday(), 1);

/** Whole days from `fromDate` to `toDate` (negative if toDate is earlier). */
export const daysBetween = (fromDate, toDate) => {
    const a = dateOnly(fromDate);
    const b = dateOnly(toDate);
    if (!a || !b) return null;
    const [ay, am, ad] = a.split("-").map(Number);
    const [by, bm, bd] = b.split("-").map(Number);
    return Math.round((Date.UTC(by, bm - 1, bd) - Date.UTC(ay, am - 1, ad)) / 86400000);
};

/** Current NPT wall-clock as pseudo-epoch ms, comparable with cutoffMomentMs(). */
const nptNowMs = () => getNptNow().getTime();

/**
 * The instant a delivery date locks, as pseudo-epoch ms on the same NPT scale
 * as nptNowMs(): cutoffHour:00 NPT on the day BEFORE `dateStr`.
 *
 * Worth spelling out because one consequence is easy to miss and is exactly
 * what the business wants: TODAY is always already locked. Today's cutoff was
 * yesterday evening, which is unavoidably in the past, so no special-case
 * "can't change today" branch is needed anywhere — it falls out of this formula.
 */
export const cutoffMomentMs = (dateStr, cutoffHour = DEFAULT_CUTOFF_HOUR) => {
    const iso = dateOnly(dateStr);
    if (!iso) return null;
    const [y, m, d] = iso.split("-").map(Number);
    return Date.UTC(y, m - 1, d - 1, cutoffHour, 0, 0, 0);
};

/** True when `dateStr` can no longer be changed by anyone. */
export const isDateLocked = (dateStr, cutoffHour = DEFAULT_CUTOFF_HOUR) => {
    const moment = cutoffMomentMs(dateStr, cutoffHour);
    if (moment === null) return true; // unparseable date: fail closed
    return nptNowMs() >= moment;
};

/** ms until `dateStr` locks; 0 once it has. Drives the cook's countdown chip. */
export const msUntilCutoff = (dateStr, cutoffHour = DEFAULT_CUTOFF_HOUR) => {
    const moment = cutoffMomentMs(dateStr, cutoffHour);
    if (moment === null) return 0;
    return Math.max(0, moment - nptNowMs());
};

/** "8:00 PM" — for error messages and UI labels, never a bare 24h number. */
export const formatCutoffLabel = (cutoffHour = DEFAULT_CUTOFF_HOUR) => {
    const h = Number(cutoffHour);
    const suffix = h >= 12 ? "PM" : "AM";
    const display = h % 12 === 0 ? 12 : h % 12;
    return `${display}:00 ${suffix}`;
};

// The cutoff hour is read on nearly every calendar/skip request but changes
// approximately never, so it's cached in-process for a minute. A stale value
// can at worst mis-lock a day for <60s, and an admin change still lands
// promptly — versus one extra round trip on every single request.
let cutoffCache = { value: null, at: 0 };
const CUTOFF_TTL_MS = 60 * 1000;

/** Admin-configured cutoff hour (NPT) from platform_settings. Never throws. */
export const getCutoffHour = async () => {
    if (cutoffCache.value !== null && Date.now() - cutoffCache.at < CUTOFF_TTL_MS) {
        return cutoffCache.value;
    }
    try {
        const [rows] = await db.promise().query(
            "SELECT delivery_cutoff_hour FROM platform_settings WHERE id = 1"
        );
        const raw = rows.length > 0 ? Number(rows[0].delivery_cutoff_hour) : NaN;
        const hour = Number.isInteger(raw) && raw >= 0 && raw <= 23 ? raw : DEFAULT_CUTOFF_HOUR;
        cutoffCache = { value: hour, at: Date.now() };
        return hour;
    } catch (err) {
        // Missing column / unreachable DB must not make the whole calendar
        // 500 — degrade to the documented default and say so once.
        console.error("⚠️  Could not read delivery_cutoff_hour, using default 20:00 NPT:", err.message);
        return DEFAULT_CUTOFF_HOUR;
    }
};

/** Test/admin hook: drop the cached cutoff so the next read hits the DB. */
export const clearCutoffCache = () => {
    cutoffCache = { value: null, at: 0 };
};

export default {
    DEFAULT_CUTOFF_HOUR,
    SQL_NPT_TODAY,
    dateOnly,
    isValidDateString,
    getNptToday,
    getNptTomorrow,
    addDays,
    daysBetween,
    cutoffMomentMs,
    isDateLocked,
    msUntilCutoff,
    formatCutoffLabel,
    getCutoffHour,
    clearCutoffCache,
    getNptNow,
    toNpt
};
