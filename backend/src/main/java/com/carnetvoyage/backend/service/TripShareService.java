package com.carnetvoyage.backend.service;

import com.carnetvoyage.backend.dto.ShareRequest;
import com.carnetvoyage.backend.dto.ShareResponse;
import com.carnetvoyage.backend.entity.Trip;
import com.carnetvoyage.backend.entity.TripShare;
import com.carnetvoyage.backend.entity.User;
import com.carnetvoyage.backend.exception.ApiException;
import com.carnetvoyage.backend.repository.TripRepository;
import com.carnetvoyage.backend.repository.TripShareRepository;
import com.carnetvoyage.backend.repository.UserRepository;
import com.carnetvoyage.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripShareService {

    private final TripShareRepository tripShareRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    // === Partager un voyage ===
    public ShareResponse shareTrip(Long tripId, ShareRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        // Vérifier que le voyage existe et appartient à l'utilisateur
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ApiException("Voyage introuvable", HttpStatus.NOT_FOUND));

        if (!trip.getOwner().getId().equals(currentUser.getId())) {
            throw new ApiException("Seul le propriétaire peut partager", HttpStatus.FORBIDDEN);
        }

        // Trouver le destinataire
        User targetUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException(
                        "Aucun utilisateur trouvé avec cet email", HttpStatus.NOT_FOUND));

        // Empêcher de partager avec soi-même
        if (targetUser.getId().equals(currentUser.getId())) {
            throw new ApiException("Vous ne pouvez pas partager avec vous-même", HttpStatus.BAD_REQUEST);
        }

        // Vérifier qu'il n'est pas déjà partagé avec cet utilisateur
        if (tripShareRepository.findByTripAndSharedWith(trip, targetUser).isPresent()) {
            throw new ApiException("Déjà partagé avec cet utilisateur", HttpStatus.CONFLICT);
        }

        // Créer le partage
        TripShare share = TripShare.builder()
                .trip(trip)
                .sharedWith(targetUser)
                .build();

        share = tripShareRepository.save(share);
        return ShareResponse.fromEntity(share);
    }

    // === Lister les partages d'un voyage ===
    public List<ShareResponse> getSharesOfTrip(Long tripId) {
        User currentUser = securityUtils.getCurrentUser();

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ApiException("Voyage introuvable", HttpStatus.NOT_FOUND));

        if (!trip.getOwner().getId().equals(currentUser.getId())) {
            throw new ApiException("Seul le propriétaire peut voir les partages", HttpStatus.FORBIDDEN);
        }

        return tripShareRepository.findByTrip(trip).stream()
                .map(ShareResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // === Retirer un partage ===
    public void unshareTrip(Long tripId, Long userId) {
        User currentUser = securityUtils.getCurrentUser();

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ApiException("Voyage introuvable", HttpStatus.NOT_FOUND));

        if (!trip.getOwner().getId().equals(currentUser.getId())) {
            throw new ApiException("Seul le propriétaire peut retirer un partage", HttpStatus.FORBIDDEN);
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("Utilisateur introuvable", HttpStatus.NOT_FOUND));

        TripShare share = tripShareRepository.findByTripAndSharedWith(trip, targetUser)
                .orElseThrow(() -> new ApiException("Ce voyage n'est pas partagé avec cet utilisateur",
                        HttpStatus.NOT_FOUND));

        tripShareRepository.delete(share);
    }
}