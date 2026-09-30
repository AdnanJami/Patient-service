package com.example.billing_service.controller;

import com.example.billing_service.dto.AccountRequestDTO;
import com.example.billing_service.dto.AccountResponseDTO;
import com.example.billing_service.dto.InvoiceRequestDTO;
import com.example.billing_service.dto.InvoiceResponseDTO;
import com.example.billing_service.model.InvoiceStatus;
import com.example.billing_service.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/billing")
@Tag(name = "Billing", description = "Billing accounts and invoices")
public class BillingController {
    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @PostMapping("/accounts")
    @Operation(summary = "Set up a billing account for a patient (returns the existing one if present)")
    public ResponseEntity<AccountResponseDTO> createAccount(@Valid @RequestBody AccountRequestDTO request) {
        return ResponseEntity.ok(billingService.getOrCreateAccount(request.patientId(), request.name(), request.email()));
    }

    @GetMapping("/accounts/{patientId}")
    @Operation(summary = "Get a patient's billing account")
    public ResponseEntity<AccountResponseDTO> getAccount(@PathVariable UUID patientId) {
        return ResponseEntity.ok(billingService.getAccount(patientId));
    }

    @GetMapping("/accounts/{patientId}/invoices")
    @Operation(summary = "List a patient's invoices, newest first")
    public ResponseEntity<List<InvoiceResponseDTO>> getPatientInvoices(@PathVariable UUID patientId) {
        return ResponseEntity.ok(billingService.getInvoicesForPatient(patientId));
    }

    @PostMapping("/accounts/{patientId}/invoices")
    @Operation(summary = "Create an invoice for a patient")
    public ResponseEntity<InvoiceResponseDTO> createInvoice(@PathVariable UUID patientId,
                                                            @Valid @RequestBody InvoiceRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(billingService.createInvoice(patientId, request));
    }

    @GetMapping("/invoices")
    @Operation(summary = "List all invoices, optionally filtered by status")
    public ResponseEntity<List<InvoiceResponseDTO>> getInvoices(@RequestParam(required = false) InvoiceStatus status) {
        return ResponseEntity.ok(billingService.getInvoices(status));
    }

    @PostMapping("/invoices/{invoiceId}/pay")
    @Operation(summary = "Mark an invoice as paid")
    public ResponseEntity<InvoiceResponseDTO> payInvoice(@PathVariable UUID invoiceId) {
        return ResponseEntity.ok(billingService.payInvoice(invoiceId));
    }
}
