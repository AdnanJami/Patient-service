package com.example.patient.kafka;

import com.example.patient.model.Patient_class;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

@Service
public class kafkaProducer {
    public static final String PATIENT_CREATED = "PATIENT_CREATED";
    public static final String PATIENT_DELETED = "PATIENT_DELETED";

    private static final Logger log = LoggerFactory.getLogger(kafkaProducer.class);
    private final KafkaTemplate<String,byte[]> kafkaTemplate;

    public kafkaProducer(KafkaTemplate<String,byte[]> kafkaTemplate){
        this.kafkaTemplate = kafkaTemplate;
    }
    public void sendEvent(Patient_class patient, String eventType){
        PatientEvent event = PatientEvent.newBuilder()
                .setPatientId(patient.getId().toString())
                .setName(patient.getName())
                .setEmail(patient.getEmail())
                .setEventType(eventType)
                .build();
        try {
            // Keyed by patient ID so all events for one patient stay in order on the same partition.
            kafkaTemplate.send("patient", event.getPatientId(), event.toByteArray());

        }catch (Exception e){
            log.error("Error sending {} event : {}",eventType,event);
        }
    }

}
