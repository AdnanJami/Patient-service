package com.example.patient;

import com.example.patient.kafka.kafkaProducer;
import com.example.patient.model.Patient_class;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

// Port 1 is never a Kafka broker, so every send hits the outage path.
@SpringBootTest(properties = "spring.kafka.bootstrap-servers=localhost:1")
class KafkaProducerOutageTests {

    @Autowired
    private kafkaProducer kafkaProducer;

    @Test
    void unreachableKafkaDoesNotStallTheCaller() {
        Patient_class patient = new Patient_class();
        patient.setId(UUID.randomUUID());
        patient.setName("Outage");
        patient.setEmail("outage@example.com");

        long start = System.nanoTime();
        assertThatNoException().isThrownBy(() -> kafkaProducer.sendEvent(patient, kafkaProducer.PATIENT_CREATED));

        // Bounded by max.block.ms (2s) rather than the Kafka default of 60s.
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(5));
    }
}
