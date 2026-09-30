package com.example.billing_service.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "invoice")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private BillingAccount account;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status = InvoiceStatus.UNPAID;

    @Column(nullable = false)
    private LocalDate issuedDate = LocalDate.now();

    @Column(nullable = false)
    private LocalDate dueDate;

    private LocalDate paidDate;

    protected Invoice() {
    }

    public Invoice(BillingAccount account, String description, BigDecimal amount, LocalDate dueDate) {
        this.account = account;
        this.description = description;
        this.amount = amount;
        this.dueDate = dueDate;
    }

    public void markPaid() {
        this.status = InvoiceStatus.PAID;
        this.paidDate = LocalDate.now();
    }

    public UUID getId() {
        return id;
    }

    public BillingAccount getAccount() {
        return account;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public LocalDate getIssuedDate() {
        return issuedDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getPaidDate() {
        return paidDate;
    }
}
