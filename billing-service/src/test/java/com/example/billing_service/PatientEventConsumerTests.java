package com.example.billing_service;

import com.example.billing_service.dto.AccountResponseDTO;
import com.example.billing_service.dto.InvoiceRequestDTO;
import com.example.billing_service.dto.InvoiceResponseDTO;
import com.example.billing_service.exception.AccountClosedException;
import com.example.billing_service.exception.NotFoundException;
import com.example.billing_service.model.InvoiceStatus;
import com.example.billing_service.kafka.PatientEventConsumer;
import com.example.billing_service.service.BillingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import patient.events.PatientEvent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// The listener is invoked directly; auto-startup is off so tests never touch a real broker.
@SpringBootTest(properties = {"spring.grpc.server.port=0", "spring.kafka.listener.auto-startup=false"})
class PatientEventConsumerTests {

    @Autowired
    private PatientEventConsumer consumer;

    @Autowired
    private BillingService billingService;

    private static byte[] event(String patientId, String type) {
        return PatientEvent.newBuilder()
                .setPatientId(patientId).setName("Kafka Patient").setEmail("kafka@example.com")
                .setEventType(type).build().toByteArray();
    }

    @Test
    void createdEventSetsUpAccountOnce() {
        UUID patientId = UUID.randomUUID();

        consumer.consume(event(patientId.toString(), "PATIENT_CREATED"));
        AccountResponseDTO account = billingService.getAccount(patientId);
        consumer.consume(event(patientId.toString(), "PATIENT_CREATED"));

        assertThat(account.name()).isEqualTo("Kafka Patient");
        assertThat(billingService.getAccount(patientId).id()).isEqualTo(account.id());
    }

    @Test
    void deletedEventClosesAccountAndKeepsInvoices() {
        UUID patientId = UUID.randomUUID();
        consumer.consume(event(patientId.toString(), "PATIENT_CREATED"));
        InvoiceResponseDTO unpaid = billingService.createInvoice(patientId,
                new InvoiceRequestDTO("Consultation", new BigDecimal("50.00"), LocalDate.now()));

        consumer.consume(event(patientId.toString(), "PATIENT_DELETED"));
        consumer.consume(event(patientId.toString(), "PATIENT_DELETED"));

        assertThat(billingService.getAccount(patientId).status()).isEqualTo("CLOSED");
        assertThat(billingService.getInvoicesForPatient(patientId))
                .singleElement()
                .satisfies(i -> assertThat(i.accountStatus()).isEqualTo("CLOSED"));
        assertThatThrownBy(() -> billingService.createInvoice(patientId,
                new InvoiceRequestDTO("Follow-up", BigDecimal.TEN, LocalDate.now())))
                .isInstanceOf(AccountClosedException.class);
        assertThat(billingService.payInvoice(unpaid.id()).status()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void deletedEventWithoutAccountIsIgnored() {
        assertThatNoException().isThrownBy(() ->
                consumer.consume(event(UUID.randomUUID().toString(), "PATIENT_DELETED")));
    }

    @Test
    void otherEventTypesAreIgnored() {
        UUID patientId = UUID.randomUUID();

        consumer.consume(event(patientId.toString(), "PATIENT_UPDATED"));

        assertThatThrownBy(() -> billingService.getAccount(patientId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void badEventsAreSkippedWithoutThrowing() {
        assertThatNoException().isThrownBy(() -> consumer.consume(new byte[]{1, 2, 3, 4}));
        assertThatNoException().isThrownBy(() -> consumer.consume(event("not-a-uuid", "PATIENT_CREATED")));
    }

    @Test
    void concurrentSetUpForSamePatientYieldsOneAccount() {
        UUID patientId = UUID.randomUUID();

        List<CompletableFuture<AccountResponseDTO>> calls = IntStream.range(0, 8)
                .mapToObj(i -> CompletableFuture.supplyAsync(
                        () -> billingService.getOrCreateAccount(patientId, "Race", "race@example.com")))
                .toList();

        assertThat(calls.stream().map(CompletableFuture::join).map(AccountResponseDTO::id).distinct())
                .hasSize(1);
    }
}
