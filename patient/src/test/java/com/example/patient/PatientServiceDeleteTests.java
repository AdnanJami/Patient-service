package com.example.patient;

import com.example.patient.kafka.kafkaProducer;
import com.example.patient.model.Patient_class;
import com.example.patient.repository.PatientRepository;
import com.example.patient.service.PatientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.UUID;

import static com.example.patient.kafka.kafkaProducer.PATIENT_DELETED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
class PatientServiceDeleteTests {

    @Autowired
    private PatientService patientService;

    @Autowired
    private PatientRepository patientRepository;

    @MockitoBean
    private kafkaProducer kafkaProducer;

    @Test
    void deletingPatientPublishesDeletedEvent() {
        Patient_class patient = new Patient_class();
        patient.setName("To Delete");
        patient.setEmail("delete." + UUID.randomUUID() + "@example.com");
        patient.setAddress("1 Test St");
        patient.setDateOfBirth(LocalDate.of(1990, 1, 1));
        patient.setRegisteredDate(LocalDate.now());
        UUID id = patientRepository.save(patient).getId();

        patientService.deletePatient(id);

        assertThat(patientRepository.findById(id)).isEmpty();
        verify(kafkaProducer).sendEvent(argThat(p -> p.getId().equals(id)), eq(PATIENT_DELETED));
    }

    @Test
    void deletingUnknownPatientPublishesNothing() {
        patientService.deletePatient(UUID.randomUUID());

        verify(kafkaProducer, never()).sendEvent(any(), anyString());
    }
}
