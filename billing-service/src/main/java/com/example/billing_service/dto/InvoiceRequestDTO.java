package com.example.billing_service.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceRequestDTO(
        @NotBlank(message = "Description is required")
        @Size(max = 200, message = "Description must be 200 characters or fewer")
        String description,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        @Digits(integer = 10, fraction = 2, message = "Amount can have at most 2 decimal places")
        BigDecimal amount,

        @NotNull(message = "Due date is required")
        @FutureOrPresent(message = "Due date can't be in the past")
        LocalDate dueDate
) {
}
