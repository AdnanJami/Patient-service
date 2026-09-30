package com.example.patient.model;

import jakarta.persistence.*;

import java.time.Instant;

// A patient event waiting to be published to Kafka. Written in the same transaction as the
// patient change, so the event can't be lost if Kafka is down; OutboxRelay publishes and removes it.
@Entity
public class OutboxEvent {

    // Increasing ID gives the publish order.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String patientId;

    @Column(nullable = false)
    private String eventType;

    // Serialized PatientEvent protobuf, exactly as it will be sent.
    @Column(nullable = false, length = 65535)
    private byte[] payload;

    @Column(nullable = false)
    private Instant createdAt;

    protected OutboxEvent() {
    }

    public OutboxEvent(String patientId, String eventType, byte[] payload) {
        this.patientId = patientId;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getEventType() {
        return eventType;
    }

    public byte[] getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
