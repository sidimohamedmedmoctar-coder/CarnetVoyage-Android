package com.carnetvoyage.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MemoryRequest {

    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 100)
    private String title;

    private String description;

    private String photoUrl;  // chemin retourné par /api/upload

    @NotNull(message = "La latitude est obligatoire")
    private Double latitude;

    @NotNull(message = "La longitude est obligatoire")
    private Double longitude;

    private LocalDateTime visitedAt;
}