package com.carnetvoyage.backend.service;

import com.carnetvoyage.backend.dto.MemoryRequest;
import com.carnetvoyage.backend.dto.MemoryResponse;
import com.carnetvoyage.backend.entity.Memory;
import com.carnetvoyage.backend.entity.Trip;
import com.carnetvoyage.backend.entity.User;
import com.carnetvoyage.backend.exception.ApiException;
import com.carnetvoyage.backend.repository.MemoryRepository;
import com.carnetvoyage.backend.repository.TripRepository;
import com.carnetvoyage.backend.repository.TripShareRepository;
import com.carnetvoyage.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemoryService {

    private final MemoryRepository memoryRepository;
    private final TripRepository tripRepository;
    private final TripShareRepository tripShareRepository;
    private final SecurityUtils securityUtils;
    private final UploadService uploadService;

    // === Lister les souvenirs d'un voyage ===
    public List<MemoryResponse> getMemoriesByTrip(Long tripId) {
        User currentUser = securityUtils.getCurrentUser();
        Trip trip = findTripWithAccess(tripId, currentUser, false);

        return memoryRepository.findByTripOrderByVisitedAtAsc(trip).stream()
                .map(MemoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // === Créer un souvenir ===
    public MemoryResponse createMemory(Long tripId, MemoryRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        Trip trip = findTripWithAccess(tripId, currentUser, true);  // ownerOnly = true

        Memory memory = Memory.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .photoUrl(request.getPhotoUrl())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .visitedAt(request.getVisitedAt())
                .trip(trip)
                .build();

        memory = memoryRepository.save(memory);
        return MemoryResponse.fromEntity(memory);
    }

    // === Modifier un souvenir ===
    public MemoryResponse updateMemory(Long memoryId, MemoryRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        Memory memory = memoryRepository.findById(memoryId)
                .orElseThrow(() -> new ApiException("Souvenir introuvable", HttpStatus.NOT_FOUND));

        if (!memory.getTrip().getOwner().getId().equals(currentUser.getId())) {
            throw new ApiException("Seul le propriétaire peut modifier", HttpStatus.FORBIDDEN);
        }

        // Si la photo change, on supprime l'ancienne
        if (memory.getPhotoUrl() != null && !memory.getPhotoUrl().equals(request.getPhotoUrl())) {
            String oldFilename = extractFilename(memory.getPhotoUrl());
            uploadService.deleteFile(oldFilename);
        }

        memory.setTitle(request.getTitle());
        memory.setDescription(request.getDescription());
        memory.setPhotoUrl(request.getPhotoUrl());
        memory.setLatitude(request.getLatitude());
        memory.setLongitude(request.getLongitude());
        memory.setVisitedAt(request.getVisitedAt());

        memory = memoryRepository.save(memory);
        return MemoryResponse.fromEntity(memory);
    }

    // === Supprimer un souvenir ===
    public void deleteMemory(Long memoryId) {
        User currentUser = securityUtils.getCurrentUser();
        Memory memory = memoryRepository.findById(memoryId)
                .orElseThrow(() -> new ApiException("Souvenir introuvable", HttpStatus.NOT_FOUND));

        if (!memory.getTrip().getOwner().getId().equals(currentUser.getId())) {
            throw new ApiException("Seul le propriétaire peut supprimer", HttpStatus.FORBIDDEN);
        }

        // Supprimer la photo associée
        if (memory.getPhotoUrl() != null) {
            uploadService.deleteFile(extractFilename(memory.getPhotoUrl()));
        }

        memoryRepository.delete(memory);
    }

    // === Helper : vérifier les droits sur un voyage ===
    private Trip findTripWithAccess(Long tripId, User user, boolean ownerOnly) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ApiException("Voyage introuvable", HttpStatus.NOT_FOUND));

        boolean isOwner = trip.getOwner().getId().equals(user.getId());

        if (ownerOnly && !isOwner) {
            throw new ApiException("Action réservée au propriétaire", HttpStatus.FORBIDDEN);
        }

        if (!ownerOnly && !isOwner) {
            boolean isShared = tripShareRepository.findByTripAndSharedWith(trip, user).isPresent();
            if (!isShared) {
                throw new ApiException("Vous n'avez pas accès à ce voyage", HttpStatus.FORBIDDEN);
            }
        }
        return trip;
    }

    // Extrait "abc-123.jpg" de "/uploads/abc-123.jpg"
    private String extractFilename(String url) {
        if (url == null) return null;
        int idx = url.lastIndexOf('/');
        return idx >= 0 ? url.substring(idx + 1) : url;
    }
}