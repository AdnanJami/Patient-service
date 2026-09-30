package com.example.billing_service.dto;

import com.example.billing_service.model.BillingAccount;

import java.time.LocalDateTime;
import java.util.UUID;

public record AccountResponseDTO(
        UUID id,
        UUID patientId,
        String name,
        String email,
        String status,
        LocalDateTime createdAt
) {
    public static AccountResponseDTO from(BillingAccount account) {
        return new AccountResponseDTO(account.getId(), account.getPatientId(), account.getName(),
                account.getEmail(), account.getStatus(), account.getCreatedAt());
    }
}
