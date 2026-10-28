package com.carnetvoyage.backend.dto;

import com.carnetvoyage.backend.entity.TripShare;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareResponse {

    private Long id;
    private Long tripId;
    private Long sharedWithUserId;
    private String sharedWithUsername;
    private String sharedWithEmail;
    private LocalDateTime sharedAt;

    public static ShareResponse fromEntity(TripShare share) {
        return ShareResponse.builder()
                .id(share.getId())
                .tripId(share.getTrip().getId())
                .sharedWithUserId(share.getSharedWith().getId())
                .sharedWithUsername(share.getSharedWith().getUsername())
                .sharedWithEmail(share.getSharedWith().getEmail())
                .sharedAt(share.getSharedAt())
                .build();
    }
}