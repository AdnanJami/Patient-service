package com.example.patient.kafka;

import com.example.patient.model.OutboxEvent;
import com.example.patient.model.Patient_class;
import com.example.patient.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import patient.events.PatientEvent;

@Service
public class PatientEventOutbox {
    private final OutboxEventRepository outboxEventRepository;

    public PatientEventOutbox(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    // MANDATORY: the event must be saved in the same transaction as the patient change it describes,
    // so either both are stored or neither is.
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Patient_class patient, String eventType) {
        PatientEvent event = PatientEvent.newBuilder()
                .setPatientId(patient.getId().toString())
                .setName(patient.getName())
                .setEmail(patient.getEmail())
                .setEventType(eventType)
                .build();
        outboxEventRepository.save(new OutboxEvent(event.getPatientId(), eventType, event.toByteArray()));
    }
}
