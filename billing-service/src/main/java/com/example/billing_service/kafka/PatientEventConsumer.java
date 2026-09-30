package com.example.billing_service.kafka;

import com.example.billing_service.service.BillingService;
import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import patient.events.PatientEvent;

import java.util.UUID;

/**
 * Keeps billing accounts in step with patients. On PATIENT_CREATED it sets up the account;
 * patient-service also asks for it over gRPC right away, and this is the durable path that catches
 * patients created while billing was down, because Kafka keeps the events until this consumer group
 * has read them. On PATIENT_DELETED it closes the account.
 */
@Component
public class PatientEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(PatientEventConsumer.class);

    static final String PATIENT_CREATED = "PATIENT_CREATED";
    static final String PATIENT_DELETED = "PATIENT_DELETED";

    private final BillingService billingService;

    public PatientEventConsumer(BillingService billingService) {
        this.billingService = billingService;
    }

    @KafkaListener(topics = "patient")
    public void consume(byte[] payload) {
        PatientEvent event;
        try {
            event = PatientEvent.parseFrom(payload);
        } catch (InvalidProtocolBufferException e) {
            log.error("Skipping unreadable patient event: {}", e.getMessage());
            return;
        }

        UUID patientId;
        try {
            patientId = UUID.fromString(event.getPatientId());
        } catch (IllegalArgumentException e) {
            log.warn("Skipping patient event with invalid patientId '{}'", event.getPatientId());
            return;
        }

        switch (event.getEventType()) {
            case PATIENT_CREATED -> billingService.getOrCreateAccount(patientId, event.getName(), event.getEmail());
            case PATIENT_DELETED -> billingService.closeAccount(patientId);
            default -> { }
        }
    }
}
