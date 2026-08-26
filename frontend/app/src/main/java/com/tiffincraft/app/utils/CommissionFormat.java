package com.tiffincraft.app.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Date helpers shared by every cook-facing commission surface
 * (CommissionSettlementActivity and the CommissionBanner on home/earnings).
 *
 * These live here rather than on one screen because a due date that renders as
 * "16 Sep 2026" in the banner and "15 Sep 2026" in the settlement screen would
 * be worse than showing nothing — the cook would not know which to trust.
 */
public final class CommissionFormat {

    /** Nepal Time. The backend bills on NPT calendar months (utils/nepaliTime.js). */
    private static final String NPT = "GMT+05:45";

    public static final String[] MONTH_NAMES = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };

    private CommissionFormat() {
    }

    /**
     * "Today" in Nepal Time as yyyy-MM-dd, matching the backend's NPT period
     * convention rather than trusting the device clock's own timezone — a cook
     * travelling abroad must not see their due date shift. SimpleDateFormat with
     * an explicit offset works on every supported API level; java.time would
     * need desugaring.
     */
    public static String todayNptIso() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone(NPT));
        return f.format(new Date());
    }

    /** "16 Sep 2026" from an ISO yyyy-MM-dd, or null if absent/unparseable. */
    public static String formatDueDate(String iso) {
        if (iso == null || iso.length() < 10) return null;
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            in.setTimeZone(TimeZone.getTimeZone(NPT));
            Date d = in.parse(iso.substring(0, 10));
            if (d == null) return null;
            SimpleDateFormat out = new SimpleDateFormat("d MMM yyyy", Locale.US);
            out.setTimeZone(TimeZone.getTimeZone(NPT));
            return out.format(d);
        } catch (Exception e) {
            return null;
        }
    }

    /** "August" for 8, or the supplied fallback when the month is out of range. */
    public static String monthName(int month, String fallback) {
        return month >= 1 && month <= 12 ? MONTH_NAMES[month - 1] : fallback;
    }
}
