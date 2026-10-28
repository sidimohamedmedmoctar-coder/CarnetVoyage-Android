package com.carnetvoyage.backend.repository;

import com.carnetvoyage.backend.entity.Trip;
import com.carnetvoyage.backend.entity.TripShare;
import com.carnetvoyage.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripShareRepository extends JpaRepository<TripShare, Long> {

    // Tous les voyages partagés avec un utilisateur
    List<TripShare> findBySharedWith(User user);

    // Vérifier si un voyage est déjà partagé avec un user
    Optional<TripShare> findByTripAndSharedWith(Trip trip, User sharedWith);

    // Tous les partages d'un voyage donné
    List<TripShare> findByTrip(Trip trip);
}