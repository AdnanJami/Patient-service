package com.example.patient;

import com.example.patient.dto.PatientRequestDTO;
import com.example.patient.grpc.BillingServiceGrpcClient;
import com.example.patient.kafka.OutboxRelay;
import com.example.patient.repository.OutboxEventRepository;
import com.example.patient.service.PatientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

// Port 1 is never a Kafka broker, so every publish hits the outage path.
@SpringBootTest(properties = {"spring.kafka.bootstrap-servers=localhost:1", "outbox.relay.enabled=false"})
class KafkaOutageTests {

    @Autowired
    private PatientService patientService;

    @Autowired
    private OutboxRelay outboxRelay;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @MockitoBean
    private BillingServiceGrpcClient billingServiceGrpcClient;

    @Test
    void unreachableKafkaNeitherStallsRequestsNorLosesEvents() {
        outboxEventRepository.deleteAll();
        PatientRequestDTO request = new PatientRequestDTO();
        request.setName("Outage");
        request.setEmail("outage." + UUID.randomUUID() + "@example.com");
        request.setAddress("1 Test St");
        request.setDateOfBirth("1990-01-01");
        request.setRegisteredDate("2026-09-30");

        // Creating a patient doesn't touch Kafka at all.
        long start = System.nanoTime();
        String id = patientService.createPatient(request).getId();
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(1));

        // The relay gives up quickly (max.block.ms) and keeps the event for the next attempt.
        start = System.nanoTime();
        assertThatNoException().isThrownBy(outboxRelay::relay);
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(5));
        assertThat(outboxEventRepository.findAll()).singleElement()
                .satisfies(event -> assertThat(event.getPatientId()).isEqualTo(id));
    }
}
