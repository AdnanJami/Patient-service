package com.example.billing_service.dto;

import com.example.billing_service.model.Invoice;
import com.example.billing_service.model.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceResponseDTO(
        UUID id,
        UUID patientId,
        String patientName,
        String description,
        BigDecimal amount,
        InvoiceStatus status,
        LocalDate issuedDate,
        LocalDate dueDate,
        LocalDate paidDate
) {
    public static InvoiceResponseDTO from(Invoice invoice) {
        return new InvoiceResponseDTO(invoice.getId(), invoice.getAccount().getPatientId(),
                invoice.getAccount().getName(), invoice.getDescription(), invoice.getAmount(),
                invoice.getStatus(), invoice.getIssuedDate(), invoice.getDueDate(), invoice.getPaidDate());
    }
}
