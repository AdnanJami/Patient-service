package com.example.patient.controller;

import com.example.patient.dto.PatientRequestDTO;
import com.example.patient.dto.PatientResponseDTO;
import com.example.patient.dto.validators.CreatePatientValidationGroup;
import com.example.patient.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/patients")
@Tag(name = "Patient",description = "API for managing Patient Service")
public class PatientController {
    private final PatientService patientService;
    public PatientController(PatientService patientService){
        this.patientService = patientService;
    }
    @GetMapping
    @Operation(summary = "GET ALL PATIENT INFORMATION")
    public ResponseEntity<List<PatientResponseDTO>> getPatients(){
        List<PatientResponseDTO> patients = patientService.getPatient();
        return ResponseEntity.ok().body(patients);
    }
    @GetMapping("/{id}")
    @Operation(summary = "get patient by id")
    public ResponseEntity<PatientResponseDTO> getPatient(@PathVariable UUID id){
        PatientResponseDTO patient = patientService.getPatientById(id);
        return ResponseEntity.ok().body(patient);
    }
    @PostMapping
    @Operation(summary = "create new patient")
    public ResponseEntity<PatientResponseDTO> createPatient(@Validated({Default.class, CreatePatientValidationGroup.class}) @RequestBody PatientRequestDTO patientRequestDTO){
        PatientResponseDTO patientResponseDTO = patientService.createPatient(patientRequestDTO);
        return ResponseEntity.ok().body(patientResponseDTO);
    }
    @PutMapping("/{id}")
    @Operation(summary = "put patient operation")
    public ResponseEntity<PatientResponseDTO> updatePatient(@PathVariable UUID id,
                                                           @Validated({Default.class}) @RequestBody PatientRequestDTO patientRequestDTO){
        PatientResponseDTO patientResponseDTO = patientService.updatePatient(id,patientRequestDTO);
        return ResponseEntity.ok().body(patientResponseDTO);
    }
    @PatchMapping("/{id}")
    @Operation(summary = "Patch patient information")
    public ResponseEntity<PatientResponseDTO> patchPatient(
            @PathVariable UUID id,
            @RequestBody PatientRequestDTO patientRequestDTO) {
        PatientResponseDTO patientResponseDTO = patientService.patchPatient(id, patientRequestDTO);
        return ResponseEntity.ok().body(patientResponseDTO);
    }
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete patient")
    public ResponseEntity<Void> deletePatient(@PathVariable UUID id){
        patientService.deletePatient(id);
        return ResponseEntity.noContent().build();
    }
}
