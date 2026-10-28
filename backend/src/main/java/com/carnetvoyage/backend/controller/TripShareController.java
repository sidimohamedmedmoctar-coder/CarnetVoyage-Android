package com.carnetvoyage.backend.controller;

import com.carnetvoyage.backend.dto.ShareRequest;
import com.carnetvoyage.backend.dto.ShareResponse;
import com.carnetvoyage.backend.service.TripShareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips/{tripId}/share")
@RequiredArgsConstructor
public class TripShareController {

    private final TripShareService tripShareService;

    @PostMapping
    public ResponseEntity<ShareResponse> shareTrip(@PathVariable Long tripId,
                                                   @Valid @RequestBody ShareRequest request) {
        return ResponseEntity.ok(tripShareService.shareTrip(tripId, request));
    }

    @GetMapping
    public ResponseEntity<List<ShareResponse>> getShares(@PathVariable Long tripId) {
        return ResponseEntity.ok(tripShareService.getSharesOfTrip(tripId));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> unshareTrip(@PathVariable Long tripId,
                                            @PathVariable Long userId) {
        tripShareService.unshareTrip(tripId, userId);
        return ResponseEntity.noContent().build();
    }
}