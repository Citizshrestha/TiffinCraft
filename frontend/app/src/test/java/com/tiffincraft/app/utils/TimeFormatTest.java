package com.tiffincraft.app.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * The one thing that must not break: a UTC ISO timestamp is converted to the
 * device's zone, not read as if it were already local.
 */
public class TimeFormatTest {

    /** Renders the parsed instant in a fixed zone so the assertion is machine-independent. */
    private String utcIsoAsNpt(String raw) {
        SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.US);
        out.setTimeZone(TimeZone.getTimeZone("GMT+05:45"));
        return out.format(TimeFormat.parse(raw));
    }

    @Test
    public void utcIsoIsShiftedIntoLocalZone() {
        // 05:24 UTC is 11:09 AM in Nepal (+05:45) — the bug showed "5:24 AM".
        assertEquals("2026-08-29 11:09 AM", utcIsoAsNpt("2026-08-29T05:24:40.000Z"));
        assertEquals("2026-08-29 11:09 AM", utcIsoAsNpt("2026-08-29T05:24:40Z"));
    }

    @Test
    public void naiveTimestampIsTakenAsLocalTime() {
        SimpleDateFormat local = new SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.US);
        assertEquals("2026-08-29 5:24 AM", local.format(TimeFormat.parse("2026-08-29 05:24:40")));
    }

    @Test
    public void unparseableInputIsEchoedBackNotCrashed() {
        assertNull(TimeFormat.parse(null));
        assertNull(TimeFormat.parse(""));
        assertNull(TimeFormat.parse("not a date"));
        assertEquals("not a date", TimeFormat.relative("not a date"));
        assertEquals("—", TimeFormat.time(null));
        assertEquals("—", TimeFormat.dayOnly(null));
    }
}
