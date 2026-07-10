package com.tiffincraft.app.models;

import java.util.List;

public class EarningsSummary {
    private double thisWeekTotal;
    private double thisMonthTotal;
    private int thisMonthOrderCount;
    private double todayTotal;
    private List<EarningsTransaction> recentTransactions;
    private List<WeeklyBreakdown> weeklyBreakdown;
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

    public int getThisMonthOrderCount() {
        return thisMonthOrderCount;
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

    public List<MonthlyBreakdown> getMonthlyBreakdown() {
        return monthlyBreakdown;
    }

    public void setMonthlyBreakdown(List<MonthlyBreakdown> monthlyBreakdown) {
        this.monthlyBreakdown = monthlyBreakdown;
    }
}
