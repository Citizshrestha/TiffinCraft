package com.tiffincraft.app.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Formatting for the delivery dates the subscription endpoints return.
 *
 * Exists because the app used to print `next_delivery_date` straight into a
 * TextView, and the server was handing back a hydrated DATE column — so
 * customers were shown "2026-09-03T00:00:00.000Z". The server now formats date
 * columns in SQL, and this class is the other half of the fix: one place that
 * turns a 'YYYY-MM-DD' string into something readable, so no screen has to
 * re-invent it (and get the ISO-suffix case wrong again).
 *
 * Every method tolerates null, empty, and an unexpected shape — a date that
 * can't be parsed is returned as-is rather than throwing, because a slightly
 * ugly date on screen beats a crash on a screen the customer opened to check
 * their meals.
 *
 * NOTE ON TIMEZONES: these are calendar dates, not instants. They are parsed
 * and formatted in the device's default zone on purpose — 'YYYY-MM-DD' is
 * already the correct Nepal-Time date as the server computed it, so converting
 * it would shift it by a day. Nothing here should ever be used on a timestamp.
 */
public final class DeliveryDateUtils {

    private DeliveryDateUtils() { }

    /** "—" rather than an empty gap, so a missing date is visibly missing. */
    public static final String EMPTY = "—";

    // ---------------------------------------------------------------------
    // Month-grid helpers.
    //
    // java.time is native at this app's minSdk (26), so LocalDate/YearMonth are
    // used directly here rather than bending the SimpleDateFormat helpers above
    // into calendar arithmetic they were never meant for.
    // ---------------------------------------------------------------------

    /** ISO 'YYYY-MM' for a month, the key the calendar screen pages on. */
    public static String monthKey(java.time.YearMonth month) {
        return month.toString();
    }

    /** "September 2026" — the month grid's header. */
    public static String formatMonthLabel(java.time.YearMonth month) {
        return month.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US));
    }

    public static String firstOfMonth(java.time.YearMonth month) {
        return month.atDay(1).toString();
    }

    public static String lastOfMonth(java.time.YearMonth month) {
        return month.atEndOfMonth().toString();
    }

    /**
     * Sunday-first column index (0-6) for a month's first day.
     *
     * Sunday-first because the weekday header row on the grid is fixed to
     * Sun–Sat; DayOfWeek.getValue() is Monday=1, hence the modulo.
     */
    public static int firstWeekdayOffset(java.time.YearMonth month) {
        return month.atDay(1).getDayOfWeek().getValue() % 7;
    }

    /** Parses an ISO date to a LocalDate, or null if it isn't one. */
    public static java.time.LocalDate toLocalDate(String isoDate) {
        if (isoDate == null || isoDate.length() < 10) return null;
        try {
            return java.time.LocalDate.parse(isoDate.substring(0, 10));
        } catch (Exception e) {
            return null;
        }
    }

    /** The month an ISO date falls in, or null if it isn't parseable. */
    public static java.time.YearMonth monthOf(String isoDate) {
        java.time.LocalDate date = toLocalDate(isoDate);
        return date == null ? null : java.time.YearMonth.from(date);
    }

    private static Date parse(String isoDate) {
        if (isoDate == null || isoDate.length() < 10) return null;
        try {
            // substring(0,10) tolerates a full ISO timestamp arriving from an
            // endpoint that hasn't been converted to date-only output yet.
            return new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(isoDate.substring(0, 10));
        } catch (Exception e) {
            return null;
        }
    }

    private static String format(String isoDate, String pattern) {
        Date date = parse(isoDate);
        if (date == null) return isoDate == null || isoDate.isEmpty() ? EMPTY : isoDate;
        return new SimpleDateFormat(pattern, Locale.US).format(date);
    }

    /** "3 Sep 2026" — for inline sentences and list rows. */
    public static String formatShortDate(String isoDate) {
        return format(isoDate, "d MMM yyyy");
    }

    /** "Sep 3, 2026" — for headline positions like "Starts Sep 3, 2026". */
    public static String formatLongDate(String isoDate) {
        return format(isoDate, "MMM d, yyyy");
    }

    /** "Thu, 3 Sep" — the calendar row heading, where the weekday is the point. */
    public static String formatDayHeading(String isoDate) {
        return format(isoDate, "EEE, d MMM");
    }

    /**
     * "Today" / "Tomorrow" / "Yesterday" / "in 4 days" / "4 days ago", or null
     * when there's nothing useful to add.
     *
     * `todayNpt` is the server's idea of today in Nepal Time, NOT the device
     * clock. A phone whose clock is off, or a customer travelling, would
     * otherwise be told "Tomorrow" about a day the server considers today —
     * which is the day whose cutoff has already passed.
     */
    public static String describeRelative(String isoDate, String todayNpt) {
        Long diff = daysBetween(todayNpt, isoDate);
        if (diff == null) return null;
        if (diff == 0L) return "Today";
        if (diff == 1L) return "Tomorrow";
        if (diff == -1L) return "Yesterday";
        if (diff > 1L) return "in " + diff + " days";
        return (-diff) + " days ago";
    }

    /** Whole days from `fromIso` to `toIso`, or null if either can't be parsed. */
    public static Long daysBetween(String fromIso, String toIso) {
        Date from = parse(fromIso);
        Date to = parse(toIso);
        if (from == null || to == null) return null;

        // Both are already normalised to local midnight by the date-only parse,
        // so rounding absorbs any DST gap between the two.
        double days = (to.getTime() - from.getTime()) / (double) TimeUnit.DAYS.toMillis(1);
        return Math.round(days);
    }

    /**
     * "3h 12m" / "12m" / "less than a minute" — how long until a cutoff.
     *
     * Takes the server's `ms_until_cutoff` rather than computing from the device
     * clock, for the same reason as describeRelative: the deadline is the
     * server's, in Nepal Time.
     */
    public static String formatDuration(long millis) {
        if (millis <= 0) return "no time";
        long totalMinutes = millis / 60000L;
        if (totalMinutes < 1) return "less than a minute";
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        if (hours <= 0) return minutes + "m";
        if (hours >= 24) {
            long days = hours / 24;
            return days + (days == 1 ? " day" : " days");
        }
        return hours + "h " + minutes + "m";
    }

    /** Today's calendar date on the device, 'YYYY-MM-DD'. For date-picker bounds only. */
    public static String deviceToday() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    /** `days` after the device's today, 'YYYY-MM-DD'. For date-picker bounds only. */
    public static String deviceTodayPlus(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.getTime());
    }

    /** Milliseconds for `days` after the device's today, at local midnight. */
    public static long deviceMillisPlusDays(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar.getTimeInMillis();
    }
}
