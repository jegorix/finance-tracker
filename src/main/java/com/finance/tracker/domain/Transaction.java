package com.finance.tracker.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Transaction {
    private final long id;
    private final BigDecimal amount;
    private final String category;
    private final String description;
    private final LocalDate date;

    public Transaction(long id, BigDecimal amount, String category, String description, LocalDate date) {
        this.id = id;
        this.amount = amount;
        this.category = category;
        this.description = description;
        this.date = date;
    }

    public long getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getDate() {
        return date;
    }
}