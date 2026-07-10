package com.tiffincraft.app.utils;

import java.text.NumberFormat;
import java.util.Locale;

/** Rupee formatting shared by the earnings screens: ₹X,XX,XXX (INR grouping, no paise). */
public final class CurrencyUtils {

    private CurrencyUtils() { }

    public static String formatRupees(double amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        format.setMaximumFractionDigits(0);
        String formatted = format.format(amount);
        // Some devices render INR as "Rs." — normalize to the ₹ symbol
        return formatted.replace("Rs.", "₹").replace("Rs", "₹").replace("INR", "₹");
    }
}
