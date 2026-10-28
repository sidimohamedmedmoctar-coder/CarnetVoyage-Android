package com.carnetvoyage.backend.dto;

import com.carnetvoyage.backend.entity.Trip;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripResponse {

    private Long id;
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;

    // Infos sur le propriétaire (utile pour les voyages partagés)
    private Long ownerId;
    private String ownerUsername;

    // Indique si l'utilisateur actuel est le propriétaire
    private boolean isOwner;

    // Helper pour convertir une entité en DTO
    public static TripResponse fromEntity(Trip trip, boolean isOwner) {
        return TripResponse.builder()
                .id(trip.getId())
                .title(trip.getTitle())
                .description(trip.getDescription())
                .startDate(trip.getStartDate())
                .endDate(trip.getEndDate())
                .createdAt(trip.getCreatedAt())
                .ownerId(trip.getOwner().getId())
                .ownerUsername(trip.getOwner().getUsername())
                .isOwner(isOwner)
                .build();
    }
}