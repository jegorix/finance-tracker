package com.finance.tracker.dto.request;
import java.math.BigDecimal;
import java.time.LocalDate;


public class TransactionRequest {

    private BigDecimal amount;
    private String category;
    private LocalDate date;

    public BigDecimal getAmount()
    {
        return amount;
    }

    public void setAmount(BigDecimal amount)
    {
        this.amount = amount;
    }

    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    public LocalDate getDate()
    {
        return date;
    }

    public void setDate(LocalDate date)
    {
        this.date = date;
    }

}
