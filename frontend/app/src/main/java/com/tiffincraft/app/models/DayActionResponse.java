package com.tiffincraft.app.models;

import com.google.gson.annotations.SerializedName;

/**
 * Shared response for the four day-level write endpoints:
 *   POST   /api/subscriptions/{id}/skip-day
 *   POST   /api/cook/daily-availability
 *   DELETE /api/cook/daily-availability/{date}
 *
 * One model rather than three because all of them answer the same question —
 * "what happened to that date, and who did it affect" — and the fields each
 * omits simply arrive null/0.
 *
 * `message` is always shown to the user verbatim. The server writes these
 * deliberately ("Left unchanged: 1 had already been delivered to — no extra day
 * was added for those."), and paraphrasing them on the client would lose the
 * distinction between "too late" and "the kitchen was already closed anyway",
 * which call for completely different reactions.
 */
public class DayActionResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    /**
     * Machine-readable outcome on the skip path: "already_skipped" or
     * "cook_unavailable" when the request succeeded but changed nothing, and
     * "not_applied" on a 409. Null on a plain success.
     */
    @SerializedName("code")
    private String code;

    @SerializedName("date")
    private String date;

    @SerializedName("day")
    private Day day;

    // ── cook closure (POST /cook/daily-availability) ────────────────────────

    /** True when the date was already marked closed, so nothing new happened. */
    @SerializedName("already_marked")
    private boolean alreadyMarked;

    @SerializedName("affected_subscriptions")
    private int affectedSubscriptions;

    /** Days left alone because the meal had already been handed over. */
    @SerializedName("skipped_already_delivered")
    private int skippedAlreadyDelivered;

    /** Days left alone because the customer had already skipped them — these
     *  must NOT be compensated a second time. */
    @SerializedName("skipped_already_settled")
    private int skippedAlreadySettled;

    // ── reopen (DELETE /cook/daily-availability/{date}) ─────────────────────

    @SerializedName("restored_subscriptions")
    private int restoredSubscriptions;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getCode() { return code; }
    public String getDate() { return date; }
    public Day getDay() { return day; }
    public boolean isAlreadyMarked() { return alreadyMarked; }
    public int getAffectedSubscriptions() { return affectedSubscriptions; }
    public int getSkippedAlreadyDelivered() { return skippedAlreadyDelivered; }
    public int getSkippedAlreadySettled() { return skippedAlreadySettled; }
    public int getRestoredSubscriptions() { return restoredSubscriptions; }

    /** True when the request succeeded but the day was already in that state. */
    public boolean isNoOp() {
        return "already_skipped".equals(code) || "cook_unavailable".equals(code);
    }

    public static class Day {
        @SerializedName("date")
        private String date;

        @SerializedName("status")
        private String status;

        @SerializedName("credit_deducted")
        private boolean creditDeducted;

        public String getDate() { return date; }
        public String getStatus() { return status; }
        public boolean isCreditDeducted() { return creditDeducted; }
    }
}
