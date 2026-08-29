package com.tiffincraft.app.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Server timestamps → human strings, rendered in the device's timezone.
 *
 * The backend stores every timestamp in UTC (see backend/utils/nepaliTime.js) and
 * mysql2 serialises it as "2026-08-29T05:24:40.000Z". Screens that parsed that with
 * a pattern lacking 'Z' silently read the UTC wall clock as local time, which in
 * Nepal is 5h45m off — 05:24 instead of 11:09 AM. Parsing is centralised here so
 * that offset bug has one place to be wrong, not five.
 *
 * A naive "yyyy-MM-dd HH:mm:ss" (no zone marker) is taken as local time, which is
 * what the server means when it sends one.
 */
public final class TimeFormat {

    private TimeFormat() { }

    private static final String[] PATTERNS = {
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss"
    };

    /** Parses any timestamp shape the API emits; null when none matches. */
    public static Date parse(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        for (String pattern : PATTERNS) {
            SimpleDateFormat fmt = new SimpleDateFormat(pattern, Locale.US);
            fmt.setLenient(false);
            if (pattern.contains("'Z'")) fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
            try {
                Date date = fmt.parse(raw.trim());
                if (date != null) return date;
            } catch (ParseException ignored) { }
        }
        return null;
    }

    /** "Today, 11:09 AM" · "Yesterday, 9:40 AM" · "27 Aug 2026, 3:12 PM". */
    public static String relative(String raw) {
        Date date = parse(raw);
        if (date == null) return raw != null ? raw : "";

        SimpleDateFormat dayKey = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String day = dayKey.format(date);
        Calendar cal = Calendar.getInstance();
        String today = dayKey.format(cal.getTime());
        cal.add(Calendar.DAY_OF_YEAR, -1);
        String yesterday = dayKey.format(cal.getTime());

        String time = new SimpleDateFormat("h:mm a", Locale.US).format(date);
        if (day.equals(today)) return "Today, " + time;
        if (day.equals(yesterday)) return "Yesterday, " + time;
        return new SimpleDateFormat("d MMM yyyy, h:mm a", Locale.US).format(date);
    }

    /** "27 Aug 2026" — no time component. */
    public static String dayOnly(String raw) {
        Date date = parse(raw);
        if (date == null) return raw != null && !raw.isEmpty() ? raw : "—";
        return new SimpleDateFormat("d MMM, yyyy", Locale.US).format(date);
    }

    /** "11:09 AM". */
    public static String time(String raw) {
        Date date = parse(raw);
        if (date == null) return "—";
        return new SimpleDateFormat("h:mm a", Locale.US).format(date);
    }
}
