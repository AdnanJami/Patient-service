package com.example.patient;

import com.example.patient.dto.PatientRequestDTO;
import com.example.patient.grpc.BillingServiceGrpcClient;
import com.example.patient.kafka.PatientEventOutbox;
import com.example.patient.model.OutboxEvent;
import com.example.patient.model.Patient_class;
import com.example.patient.repository.OutboxEventRepository;
import com.example.patient.repository.PatientRepository;
import com.example.patient.service.PatientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.IllegalTransactionStateException;
import patient.events.PatientEvent;

import java.util.List;
import java.util.UUID;

import static com.example.patient.kafka.kafkaProducer.PATIENT_CREATED;
import static com.example.patient.kafka.kafkaProducer.PATIENT_DELETED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(properties = "outbox.relay.enabled=false")
class PatientServiceOutboxTests {

    @Autowired
    private PatientService patientService;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PatientEventOutbox patientEventOutbox;

    @MockitoSpyBean
    private OutboxEventRepository outboxEventRepository;

    @MockitoBean
    private BillingServiceGrpcClient billingServiceGrpcClient;

    @BeforeEach
    void emptyOutbox() {
        outboxEventRepository.deleteAll();
    }

    @Test
    void creatingPatientStoresCreatedEvent() throws Exception {
        String email = uniqueEmail();

        UUID id = UUID.fromString(patientService.createPatient(request(email)).getId());

        List<OutboxEvent> events = outboxEventRepository.findAll();
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.getPatientId()).isEqualTo(id.toString());
            assertThat(event.getEventType()).isEqualTo(PATIENT_CREATED);
        });
        PatientEvent payload = PatientEvent.parseFrom(events.getFirst().getPayload());
        assertThat(payload.getPatientId()).isEqualTo(id.toString());
        assertThat(payload.getEmail()).isEqualTo(email);
        assertThat(payload.getEventType()).isEqualTo(PATIENT_CREATED);
    }

    @Test
    void failingToStoreEventAlsoRollsBackPatient() {
        String email = uniqueEmail();
        doThrow(new IllegalStateException("outbox unavailable")).when(outboxEventRepository).save(any());

        assertThatThrownBy(() -> patientService.createPatient(request(email)))
                .hasMessageContaining("outbox unavailable");

        assertThat(patientRepository.existsByEmail(email)).isFalse();
    }

    @Test
    void deletingPatientStoresDeletedEvent() {
        UUID id = UUID.fromString(patientService.createPatient(request(uniqueEmail())).getId());
        outboxEventRepository.deleteAll();

        patientService.deletePatient(id);

        assertThat(patientRepository.findById(id)).isEmpty();
        assertThat(outboxEventRepository.findAll()).singleElement().satisfies(event -> {
            assertThat(event.getPatientId()).isEqualTo(id.toString());
            assertThat(event.getEventType()).isEqualTo(PATIENT_DELETED);
        });
    }

    @Test
    void deletingUnknownPatientStoresNothing() {
        patientService.deletePatient(UUID.randomUUID());

        assertThat(outboxEventRepository.count()).isZero();
    }

    @Test
    void recordingEventOutsideTransactionIsRejected() {
        Patient_class patient = new Patient_class();
        patient.setId(UUID.randomUUID());
        patient.setName("No Transaction");
        patient.setEmail(uniqueEmail());

        assertThatThrownBy(() -> patientEventOutbox.record(patient, PATIENT_CREATED))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    private static String uniqueEmail() {
        return "outbox." + UUID.randomUUID() + "@example.com";
    }

    private static PatientRequestDTO request(String email) {
        PatientRequestDTO dto = new PatientRequestDTO();
        dto.setName("Outbox Test");
        dto.setEmail(email);
        dto.setAddress("1 Test St");
        dto.setDateOfBirth("1990-01-01");
        dto.setRegisteredDate("2026-09-30");
        return dto;
    }
}
