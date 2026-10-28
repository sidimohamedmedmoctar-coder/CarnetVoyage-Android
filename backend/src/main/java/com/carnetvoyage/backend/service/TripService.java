package com.carnetvoyage.backend.service;

import com.carnetvoyage.backend.dto.TripRequest;
import com.carnetvoyage.backend.dto.TripResponse;
import com.carnetvoyage.backend.entity.Trip;
import com.carnetvoyage.backend.entity.TripShare;
import com.carnetvoyage.backend.entity.User;
import com.carnetvoyage.backend.exception.ApiException;
import com.carnetvoyage.backend.repository.MemoryRepository;
import com.carnetvoyage.backend.repository.TripRepository;
import com.carnetvoyage.backend.repository.TripShareRepository;
import com.carnetvoyage.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final TripShareRepository tripShareRepository;
    private final MemoryRepository memoryRepository;
    private final SecurityUtils securityUtils;

    // === Lister mes voyages ===
    public List<TripResponse> getMyTrips() {
        User currentUser = securityUtils.getCurrentUser();
        return tripRepository.findByOwnerOrderByCreatedAtDesc(currentUser).stream()
                .map(trip -> TripResponse.fromEntity(trip, true))
                .collect(Collectors.toList());
    }

    // === Voyages partagés avec moi ===
    public List<TripResponse> getSharedTrips() {
        User currentUser = securityUtils.getCurrentUser();
        return tripShareRepository.findBySharedWith(currentUser).stream()
                .map(share -> TripResponse.fromEntity(share.getTrip(), false))
                .collect(Collectors.toList());
    }

    // === Détail d'un voyage ===
    public TripResponse getTripById(Long tripId) {
        User currentUser = securityUtils.getCurrentUser();
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ApiException("Voyage introuvable", HttpStatus.NOT_FOUND));

        boolean isOwner = trip.getOwner().getId().equals(currentUser.getId());
        boolean isShared = tripShareRepository.findByTripAndSharedWith(trip, currentUser).isPresent();

        if (!isOwner && !isShared) {
            throw new ApiException("Vous n'avez pas accès à ce voyage", HttpStatus.FORBIDDEN);
        }

        return TripResponse.fromEntity(trip, isOwner);
    }

    // === Créer un voyage ===
    public TripResponse createTrip(TripRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        Trip trip = Trip.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .owner(currentUser)
                .build();

        trip = tripRepository.save(trip);
        return TripResponse.fromEntity(trip, true);
    }

    // === Modifier un voyage ===
    public TripResponse updateTrip(Long tripId, TripRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ApiException("Voyage introuvable", HttpStatus.NOT_FOUND));

        if (!trip.getOwner().getId().equals(currentUser.getId())) {
            throw new ApiException("Seul le propriétaire peut modifier le voyage", HttpStatus.FORBIDDEN);
        }

        trip.setTitle(request.getTitle());
        trip.setDescription(request.getDescription());
        trip.setStartDate(request.getStartDate());
        trip.setEndDate(request.getEndDate());

        trip = tripRepository.save(trip);
        return TripResponse.fromEntity(trip, true);
    }

    // === Supprimer un voyage ===
    @Transactional
    public void deleteTrip(Long tripId) {
        User currentUser = securityUtils.getCurrentUser();
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ApiException("Voyage introuvable", HttpStatus.NOT_FOUND));

        if (!trip.getOwner().getId().equals(currentUser.getId())) {
            throw new ApiException("Seul le propriétaire peut supprimer le voyage", HttpStatus.FORBIDDEN);
        }

        // Supprimer d'abord les enfants pour respecter les contraintes FK
        tripShareRepository.deleteAll(tripShareRepository.findByTrip(trip));
        memoryRepository.deleteAll(memoryRepository.findByTripOrderByVisitedAtAsc(trip));

        tripRepository.delete(trip);
    }
}