package com.carnetvoyage.backend.repository;

import com.carnetvoyage.backend.entity.Memory;
import com.carnetvoyage.backend.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemoryRepository extends JpaRepository<Memory, Long> {

    // Tous les souvenirs d'un voyage, triés chronologiquement
    List<Memory> findByTripOrderByVisitedAtAsc(Trip trip);
}