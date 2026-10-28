package com.carnetvoyage.backend.repository;

import com.carnetvoyage.backend.entity.Trip;
import com.carnetvoyage.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    // Tous les voyages d'un utilisateur (triés du plus récent au plus ancien)
    List<Trip> findByOwnerOrderByCreatedAtDesc(User owner);
}