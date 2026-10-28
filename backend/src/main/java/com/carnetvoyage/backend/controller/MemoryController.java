package com.carnetvoyage.backend.controller;

import com.carnetvoyage.backend.dto.MemoryRequest;
import com.carnetvoyage.backend.dto.MemoryResponse;
import com.carnetvoyage.backend.service.MemoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MemoryController {

    private final MemoryService memoryService;

    // Lister les souvenirs d'un voyage
    @GetMapping("/trips/{tripId}/memories")
    public ResponseEntity<List<MemoryResponse>> getMemories(@PathVariable Long tripId) {
        return ResponseEntity.ok(memoryService.getMemoriesByTrip(tripId));
    }

    // Ajouter un souvenir à un voyage
    @PostMapping("/trips/{tripId}/memories")
    public ResponseEntity<MemoryResponse> createMemory(@PathVariable Long tripId,
                                                       @Valid @RequestBody MemoryRequest request) {
        return ResponseEntity.ok(memoryService.createMemory(tripId, request));
    }

    // Modifier un souvenir
    @PutMapping("/memories/{id}")
    public ResponseEntity<MemoryResponse> updateMemory(@PathVariable Long id,
                                                       @Valid @RequestBody MemoryRequest request) {
        return ResponseEntity.ok(memoryService.updateMemory(id, request));
    }

    // Supprimer un souvenir
    @DeleteMapping("/memories/{id}")
    public ResponseEntity<Void> deleteMemory(@PathVariable Long id) {
        memoryService.deleteMemory(id);
        return ResponseEntity.noContent().build();
    }
}