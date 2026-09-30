package com.example.billing_service.service;

import com.example.billing_service.dto.AccountResponseDTO;
import com.example.billing_service.dto.InvoiceRequestDTO;
import com.example.billing_service.dto.InvoiceResponseDTO;
import com.example.billing_service.exception.InvoiceAlreadyPaidException;
import com.example.billing_service.exception.NotFoundException;
import com.example.billing_service.model.BillingAccount;
import com.example.billing_service.model.Invoice;
import com.example.billing_service.model.InvoiceStatus;
import com.example.billing_service.repository.BillingAccountRepository;
import com.example.billing_service.repository.InvoiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BillingService {
    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final BillingAccountRepository accountRepository;
    private final InvoiceRepository invoiceRepository;

    public BillingService(BillingAccountRepository accountRepository, InvoiceRepository invoiceRepository) {
        this.accountRepository = accountRepository;
        this.invoiceRepository = invoiceRepository;
    }

    /** Returns the patient's account, creating it on first call. Safe to call repeatedly. */
    @Transactional
    public AccountResponseDTO getOrCreateAccount(UUID patientId, String name, String email) {
        BillingAccount account = accountRepository.findByPatientId(patientId).orElseGet(() -> {
            log.info("Creating billing account for patient {}", patientId);
            return accountRepository.save(new BillingAccount(patientId, name, email));
        });
        return AccountResponseDTO.from(account);
    }

    @Transactional(readOnly = true)
    public AccountResponseDTO getAccount(UUID patientId) {
        return AccountResponseDTO.from(findAccount(patientId));
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponseDTO> getInvoicesForPatient(UUID patientId) {
        return invoiceRepository.findByAccountOrderByIssuedDateDescDueDateDesc(findAccount(patientId))
                .stream().map(InvoiceResponseDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponseDTO> getInvoices(InvoiceStatus status) {
        List<Invoice> invoices = status == null
                ? invoiceRepository.findAllByOrderByIssuedDateDescDueDateDesc()
                : invoiceRepository.findByStatusOrderByIssuedDateDescDueDateDesc(status);
        return invoices.stream().map(InvoiceResponseDTO::from).toList();
    }

    @Transactional
    public InvoiceResponseDTO createInvoice(UUID patientId, InvoiceRequestDTO request) {
        BillingAccount account = findAccount(patientId);
        Invoice invoice = invoiceRepository.save(
                new Invoice(account, request.description().trim(), request.amount(), request.dueDate()));
        return InvoiceResponseDTO.from(invoice);
    }

    @Transactional
    public InvoiceResponseDTO payInvoice(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found"));
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new InvoiceAlreadyPaidException("Invoice is already paid");
        }
        invoice.markPaid();
        return InvoiceResponseDTO.from(invoice);
    }

    private BillingAccount findAccount(UUID patientId) {
        return accountRepository.findByPatientId(patientId)
                .orElseThrow(() -> new NotFoundException("Billing account not found"));
    }
}
