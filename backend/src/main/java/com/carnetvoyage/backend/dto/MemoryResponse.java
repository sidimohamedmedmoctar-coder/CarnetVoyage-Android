package com.carnetvoyage.backend.dto;

import com.carnetvoyage.backend.entity.Memory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryResponse {

    private Long id;
    private String title;
    private String description;
    private String photoUrl;
    private Double latitude;
    private Double longitude;
    private LocalDateTime visitedAt;
    private LocalDateTime createdAt;
    private Long tripId;

    public static MemoryResponse fromEntity(Memory memory) {
        return MemoryResponse.builder()
                .id(memory.getId())
                .title(memory.getTitle())
                .description(memory.getDescription())
                .photoUrl(memory.getPhotoUrl())
                .latitude(memory.getLatitude())
                .longitude(memory.getLongitude())
                .visitedAt(memory.getVisitedAt())
                .createdAt(memory.getCreatedAt())
                .tripId(memory.getTrip().getId())
                .build();
    }
}