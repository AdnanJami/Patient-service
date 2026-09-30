package com.example.patient.kafka;

import com.example.patient.model.OutboxEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class kafkaProducer {
    public static final String PATIENT_CREATED = "PATIENT_CREATED";
    public static final String PATIENT_DELETED = "PATIENT_DELETED";

    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final KafkaTemplate<String,byte[]> kafkaTemplate;

    public kafkaProducer(KafkaTemplate<String,byte[]> kafkaTemplate){
        this.kafkaTemplate = kafkaTemplate;
    }

    // Blocks until Kafka acknowledges the event, and throws if it doesn't, so the caller only
    // removes an event from the outbox once it is really published.
    public void publish(OutboxEvent event) throws Exception {
        // Keyed by patient ID so all events for one patient stay in order on the same partition.
        kafkaTemplate.send("patient", event.getPatientId(), event.getPayload())
                .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

}
