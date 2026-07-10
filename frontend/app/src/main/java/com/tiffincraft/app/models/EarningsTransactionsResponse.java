package com.tiffincraft.app.models;

import java.util.List;

public class EarningsTransactionsResponse {
    private boolean success;
    private List<EarningsTransaction> transactions;
    private EarningsTransactionsPagination pagination;
    private String message;

    public EarningsTransactionsResponse() {
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<EarningsTransaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<EarningsTransaction> transactions) {
        this.transactions = transactions;
    }

    public EarningsTransactionsPagination getPagination() {
        return pagination;
    }

    public void setPagination(EarningsTransactionsPagination pagination) {
        this.pagination = pagination;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
