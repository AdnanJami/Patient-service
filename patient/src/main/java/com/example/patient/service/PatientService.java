package com.example.patient.service;

import com.example.patient.dto.PatientRequestDTO;
import com.example.patient.dto.PatientResponseDTO;
import com.example.patient.exception.EmailAlreadyExistException;
import com.example.patient.exception.PatientNotFoundException;
import com.example.patient.grpc.BillingServiceGrpcClient;
import com.example.patient.kafka.kafkaProducer;
import com.example.patient.mapper.PatientMapper;
import com.example.patient.model.Patient_class;
import com.example.patient.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PatientService {
    private final PatientRepository patientRepository;
    private final BillingServiceGrpcClient billingServiceGrpcClient;
    private final kafkaProducer kafkaProducer;

    public PatientService (PatientRepository patientRepository,
                           BillingServiceGrpcClient billingServiceGrpcClient,
                           kafkaProducer kafkaProducer){

        this.patientRepository = patientRepository;
        this.billingServiceGrpcClient = billingServiceGrpcClient;
        this.kafkaProducer = kafkaProducer;
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
        Patient_class newPatient = patientRepository.save(PatientMapper.toModel(patientRequestDTO));
        billingServiceGrpcClient.createBillingAccount(newPatient.getId().toString(), newPatient.getName(), newPatient.getEmail());
        kafkaProducer.sendEvent(newPatient);
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

    public void deletePatient(UUID id){
        patientRepository.deleteById(id);
    }
}
