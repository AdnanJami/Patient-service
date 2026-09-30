package com.example.billing_service;

import com.example.billing_service.dto.AccountResponseDTO;
import com.example.billing_service.dto.InvoiceRequestDTO;
import com.example.billing_service.dto.InvoiceResponseDTO;
import com.example.billing_service.exception.InvoiceAlreadyPaidException;
import com.example.billing_service.exception.NotFoundException;
import com.example.billing_service.model.InvoiceStatus;
import com.example.billing_service.service.BillingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Runs against an in-memory H2 database; port 0 keeps the gRPC server off the Docker-mapped 9001.
@SpringBootTest(properties = {"spring.grpc.server.port=0", "spring.kafka.listener.auto-startup=false"})
class BillingServiceTests {

    @Autowired
    private BillingService billingService;

    @Test
    void getOrCreateAccountIsIdempotent() {
        UUID patientId = UUID.randomUUID();

        AccountResponseDTO first = billingService.getOrCreateAccount(patientId, "Jane", "jane@example.com");
        AccountResponseDTO second = billingService.getOrCreateAccount(patientId, "Jane", "jane@example.com");

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(first.status()).isEqualTo("ACTIVE");
    }

    @Test
    void createListAndPayInvoice() {
        UUID patientId = UUID.randomUUID();
        billingService.getOrCreateAccount(patientId, "John", "john@example.com");

        InvoiceResponseDTO invoice = billingService.createInvoice(patientId,
                new InvoiceRequestDTO("  Consultation  ", new BigDecimal("150.00"), LocalDate.now().plusDays(30)));

        assertThat(invoice.status()).isEqualTo(InvoiceStatus.UNPAID);
        assertThat(invoice.description()).isEqualTo("Consultation");
        assertThat(invoice.patientName()).isEqualTo("John");
        assertThat(billingService.getInvoicesForPatient(patientId)).extracting(InvoiceResponseDTO::id)
                .containsExactly(invoice.id());

        InvoiceResponseDTO paid = billingService.payInvoice(invoice.id());

        assertThat(paid.status()).isEqualTo(InvoiceStatus.PAID);
        assertThat(paid.paidDate()).isEqualTo(LocalDate.now());
        assertThat(billingService.getInvoices(InvoiceStatus.PAID)).extracting(InvoiceResponseDTO::id)
                .contains(invoice.id());
        assertThat(billingService.getInvoices(InvoiceStatus.UNPAID)).extracting(InvoiceResponseDTO::id)
                .doesNotContain(invoice.id());
    }

    @Test
    void payingTwiceIsRejected() {
        UUID patientId = UUID.randomUUID();
        billingService.getOrCreateAccount(patientId, "Ann", "ann@example.com");
        InvoiceResponseDTO invoice = billingService.createInvoice(patientId,
                new InvoiceRequestDTO("X-ray", new BigDecimal("80"), LocalDate.now()));
        billingService.payInvoice(invoice.id());

        assertThatThrownBy(() -> billingService.payInvoice(invoice.id()))
                .isInstanceOf(InvoiceAlreadyPaidException.class);
    }

    @Test
    void invoicesRequireAnAccount() {
        assertThatThrownBy(() -> billingService.createInvoice(UUID.randomUUID(),
                new InvoiceRequestDTO("Checkup", BigDecimal.TEN, LocalDate.now())))
                .isInstanceOf(NotFoundException.class);
    }
}
