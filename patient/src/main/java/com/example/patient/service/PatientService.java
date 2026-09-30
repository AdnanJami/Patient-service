package com.example.patient.service;

import com.example.patient.dto.PatientRequestDTO;
import com.example.patient.dto.PatientResponseDTO;
import com.example.patient.exception.EmailAlreadyExistException;
import com.example.patient.exception.PatientNotFoundException;
import com.example.patient.grpc.BillingServiceGrpcClient;
import com.example.patient.kafka.PatientEventOutbox;
import com.example.patient.mapper.PatientMapper;
import com.example.patient.model.Patient_class;
import com.example.patient.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.example.patient.kafka.kafkaProducer.PATIENT_CREATED;
import static com.example.patient.kafka.kafkaProducer.PATIENT_DELETED;

@Service
public class PatientService {
    private final PatientRepository patientRepository;
    private final BillingServiceGrpcClient billingServiceGrpcClient;
    private final PatientEventOutbox patientEventOutbox;
    private final TransactionTemplate transactionTemplate;

    public PatientService (PatientRepository patientRepository,
                           BillingServiceGrpcClient billingServiceGrpcClient,
                           PatientEventOutbox patientEventOutbox,
                           TransactionTemplate transactionTemplate){

        this.patientRepository = patientRepository;
        this.billingServiceGrpcClient = billingServiceGrpcClient;
        this.patientEventOutbox = patientEventOutbox;
        this.transactionTemplate = transactionTemplate;
    }
    public List<PatientResponseDTO> getPatient(){
        List<Patient_class> patients = patientRepository.findAll();
        return patients.stream().map(PatientMapper::toDTO).toList();
    }
    public PatientResponseDTO getPatientById(UUID id){
        Patient_class patient = patientRepository.findById(id).orElseThrow(()-> new PatientNotFoundException("Patient not found with Id:"+id));
        return PatientMapper.toDTO(patient);
    }

    public PatientResponseDTO createPatient (PatientRequestDTO  patientRequestDTO ){
        if(patientRepository.existsByEmail(patientRequestDTO.getEmail())){
            throw new EmailAlreadyExistException("A Patient Already exist with this email" + patientRequestDTO.getEmail());
        }
        // The patient and its PATIENT_CREATED event are stored together; OutboxRelay publishes the event.
        Patient_class newPatient = transactionTemplate.execute(status -> {
            Patient_class saved = patientRepository.save(PatientMapper.toModel(patientRequestDTO));
            patientEventOutbox.record(saved, PATIENT_CREATED);
            return saved;
        });
        // Outside the transaction so a slow billing-service doesn't hold a database connection.
        billingServiceGrpcClient.createBillingAccount(newPatient.getId().toString(), newPatient.getName(), newPatient.getEmail());
        return PatientMapper.toDTO(newPatient);
    }
    public PatientResponseDTO updatePatient (UUID id,PatientRequestDTO patientRequestDTO){
        Patient_class patient = patientRepository.findById(id).orElseThrow(()-> new PatientNotFoundException("Patient not found with Id:"+id));
        if(patientRepository.existsByEmailAndIdNot(patientRequestDTO.getEmail(),id)){
            throw new EmailAlreadyExistException("A Patient Already exist with this email" + patientRequestDTO.getEmail());
        }
        patient.setName(patientRequestDTO.getName());
        patient.setAddress(patientRequestDTO.getAddress());
        patient.setEmail(patientRequestDTO.getEmail());
        patient.setDateOfBirth(LocalDate.parse(patientRequestDTO.getDateOfBirth()));
        Patient_class updatedPatient = patientRepository.save(patient);
        return PatientMapper.toDTO(updatedPatient);

    }
    public PatientResponseDTO patchPatient(UUID id, PatientRequestDTO patientRequestDTO) {
        Patient_class patient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found with Id:" + id));

        // Only update fields that are provided (not null)
        if (patientRequestDTO.getName() != null) {
            patient.setName(patientRequestDTO.getName());
        }

        if (patientRequestDTO.getEmail() != null) {
            // Check if email already exists for a different patient
            if (patientRepository.existsByEmailAndIdNot(patientRequestDTO.getEmail(), id)) {
                throw new EmailAlreadyExistException("A Patient already exists with this email: " + patientRequestDTO.getEmail());
            }
            patient.setEmail(patientRequestDTO.getEmail());
        }

        if (patientRequestDTO.getAddress() != null) {
            patient.setAddress(patientRequestDTO.getAddress());
        }

        if (patientRequestDTO.getDateOfBirth() != null) {
            patient.setDateOfBirth(LocalDate.parse(patientRequestDTO.getDateOfBirth()));
        }

        Patient_class updatedPatient = patientRepository.save(patient);
        return PatientMapper.toDTO(updatedPatient);
    }

    @Transactional
    public void deletePatient(UUID id){
        // Deleting an unknown patient stays a no-op (204), and publishes nothing.
        patientRepository.findById(id).ifPresent(patient -> {
            patientRepository.delete(patient);
            patientEventOutbox.record(patient, PATIENT_DELETED);
        });
    }
}
