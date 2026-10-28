package com.carnetvoyage.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TripRequest {

    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 100)
    private String title;

    private String description;

    private LocalDate startDate;

    private LocalDate endDate;
}