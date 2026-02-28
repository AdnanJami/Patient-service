package com.example.patient.dto;

import com.example.patient.dto.validators.CreatePatientValidationGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import javax.swing.*;

public class PatientRequestDTO {
    @NotBlank(message = "Name Required")
    @Size(max=100,message = "Too big")
    private String name;

    @NotBlank(message = "Email Required")
    @Size(message = "Email Should be valid")
    private String email;

    @NotBlank(message = "Address is required")
    private String address;
    @NotBlank(message = "Date of Birth required")
    private String dateOfBirth;

    @NotBlank(groups = CreatePatientValidationGroup.class,message = "Registered date is required")
    private String registeredDate;
    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRegisteredDate() {
        return registeredDate;
    }

    public void setRegisteredDate(String registeredDate) {
        this.registeredDate = registeredDate;
    }



}
