package com.tiffincraft.app.models;

import java.util.List;

public class EarningsSummary {
    private double thisWeekTotal;
    private double thisMonthTotal;
    private double thisMonthCommission;
    private double thisMonthNetTotal;
    private int thisMonthOrderCount;
    /** Slices of thisMonthTotal by revenue stream — they sum to the gross total. */
    private double thisMonthSubscriptionTotal;
    private double thisMonthComboTotal;
    private double thisMonthDirectTotal;
    private double todayTotal;
    private List<EarningsTransaction> recentTransactions;
    private List<WeeklyBreakdown> weeklyBreakdown;
    private List<WeeklyBreakdown> dailyBreakdown;
    private List<MonthlyBreakdown> monthlyBreakdown;

    public EarningsSummary() {
    }

    public double getThisWeekTotal() {
        return thisWeekTotal;
    }

    public void setThisWeekTotal(double thisWeekTotal) {
        this.thisWeekTotal = thisWeekTotal;
    }

    public double getThisMonthTotal() {
        return thisMonthTotal;
    }

    public void setThisMonthTotal(double thisMonthTotal) {
        this.thisMonthTotal = thisMonthTotal;
    }

    public double getThisMonthCommission() {
        return thisMonthCommission;
    }

    public void setThisMonthCommission(double thisMonthCommission) {
        this.thisMonthCommission = thisMonthCommission;
    }

    /** What the cook actually keeps this month — gross minus platform commission. */
    public double getThisMonthNetTotal() {
        return thisMonthNetTotal;
    }

    public void setThisMonthNetTotal(double thisMonthNetTotal) {
        this.thisMonthNetTotal = thisMonthNetTotal;
    }

    public int getThisMonthOrderCount() {
        return thisMonthOrderCount;
    }

    public double getThisMonthSubscriptionTotal() {
        return thisMonthSubscriptionTotal;
    }

    public double getThisMonthComboTotal() {
        return thisMonthComboTotal;
    }

    /** One-off orders: gross minus the subscription and combo slices. */
    public double getThisMonthDirectTotal() {
        return thisMonthDirectTotal;
    }

    public void setThisMonthOrderCount(int thisMonthOrderCount) {
        this.thisMonthOrderCount = thisMonthOrderCount;
    }

    public double getTodayTotal() {
        return todayTotal;
    }

    public void setTodayTotal(double todayTotal) {
        this.todayTotal = todayTotal;
    }

    public List<EarningsTransaction> getRecentTransactions() {
        return recentTransactions;
    }

    public void setRecentTransactions(List<EarningsTransaction> recentTransactions) {
        this.recentTransactions = recentTransactions;
    }

    public List<WeeklyBreakdown> getWeeklyBreakdown() {
        return weeklyBreakdown;
    }

    public void setWeeklyBreakdown(List<WeeklyBreakdown> weeklyBreakdown) {
        this.weeklyBreakdown = weeklyBreakdown;
    }

    public List<WeeklyBreakdown> getDailyBreakdown() {
        return dailyBreakdown;
    }

    public void setDailyBreakdown(List<WeeklyBreakdown> dailyBreakdown) {
        this.dailyBreakdown = dailyBreakdown;
    }

    public List<MonthlyBreakdown> getMonthlyBreakdown() {
        return monthlyBreakdown;
    }

    public void setMonthlyBreakdown(List<MonthlyBreakdown> monthlyBreakdown) {
        this.monthlyBreakdown = monthlyBreakdown;
    }
}
