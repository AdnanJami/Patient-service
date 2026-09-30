package com.example.billing_service.repository;

import com.example.billing_service.model.BillingAccount;
import com.example.billing_service.model.Invoice;
import com.example.billing_service.model.InvoiceStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

// The account is fetched with each invoice because responses include the patient's name.
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    @EntityGraph(attributePaths = "account")
    List<Invoice> findByAccountOrderByIssuedDateDescDueDateDesc(BillingAccount account);

    @EntityGraph(attributePaths = "account")
    List<Invoice> findAllByOrderByIssuedDateDescDueDateDesc();

    @EntityGraph(attributePaths = "account")
    List<Invoice> findByStatusOrderByIssuedDateDescDueDateDesc(InvoiceStatus status);
}
