/**
 * Edge case 5 fix: period-boundary arithmetic (which calendar month an order
 * belongs to) must use ONE consistent timezone everywhere, not whatever the
 * DB session or the Node process's OS timezone happens to be.
 *
 * Business timezone: Nepal Time, UTC+05:45 (NPT). Chosen because that's
 * where the business operates and where "the month closed" is a real-world
 * event cooks and the admin experience locally.
 *
 * Assumption (tag: Assumption — not verified against the live DB's
 * @@global.time_zone): delivered_at is written via SQL NOW() with no
 * explicit session time_zone set, so it is stored in whatever timezone the
 * DB connection defaults to. On TiDB Cloud and most managed MySQL that
 * default is UTC. All SQL below therefore explicitly converts delivered_at
 * from '+00:00' to '+05:45' with CONVERT_TZ before extracting MONTH/YEAR.
 * If the live DB is verified to already store local time, this conversion
 * must be removed — re-verify with `SELECT NOW(), UTC_TIMESTAMP();` before
 * relying on this in production.
 *
 * On the JS side (period boundaries used to query "the current month"),
 * use getNptNow()/getNptMonthYear() below instead of `new Date()` so the
 * result doesn't silently depend on the server process's OS/TZ env var.
 */

const NPT_OFFSET_MS = (5 * 60 + 45) * 60 * 1000; // +05:45

/** Current wall-clock time in Nepal, as a Date whose UTC fields equal NPT local fields. */
export const getNptNow = () => new Date(Date.now() + NPT_OFFSET_MS);

/** { month: 1-12, year } for "right now" in Nepal time. */
export const getNptMonthYear = () => {
    const d = getNptNow();
    return { month: d.getUTCMonth() + 1, year: d.getUTCFullYear() };
};

/**
 * SQL fragment to convert a UTC-stored timestamp column to NPT before
 * extracting date parts. Usage: `MONTH(${toNpt('o.delivered_at')})`.
 */
export const toNpt = (column) => `CONVERT_TZ(${column}, '+00:00', '+05:45')`;
