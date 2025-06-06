package com.siw.it.siw_trip.Repository;

import com.siw.it.siw_trip.Model.Route;
import com.siw.it.siw_trip.Model.Visit;
import com.siw.it.siw_trip.Model.TransportMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {
    
    Optional<Route> findByFromVisitAndToVisit(Visit fromVisit, Visit toVisit);
    
    List<Route> findByFromVisit(Visit fromVisit);
    
    List<Route> findByToVisit(Visit toVisit);
    
    List<Route> findByTransportMode(TransportMode transportMode);
    
    @Query("SELECT r FROM Route r WHERE r.fromVisit.id = :visitId OR r.toVisit.id = :visitId")
    List<Route> findByVisitId(@Param("visitId") Long visitId);
    
    @Query("SELECT r FROM Route r JOIN r.fromVisit.tripDay td JOIN td.trip t WHERE t.id = :tripId")
    List<Route> findByTripId(@Param("tripId") Long tripId);
    
    @Query("SELECT r FROM Route r WHERE r.estimatedDurationMinutes > :duration")
    List<Route> findByEstimatedDurationGreaterThan(@Param("duration") Integer duration);
    
    /**
     * Find all routes for a specific trip day by ID
     */
    @Query("SELECT r FROM Route r WHERE r.tripDay.id = :tripDayId")
    List<Route> findByTripDayId(@Param("tripDayId") Long tripDayId);
    
    /**
     * Find all routes involving a specific visit (either as source or destination)
     */
    @Query("SELECT r FROM Route r WHERE r.fromVisit = :visit OR r.toVisit = :visit")
    List<Route> findByVisit(@Param("visit") Visit visit);
    
    /**
     * Delete all routes involving a specific visit
     */
    @Modifying
    @Query("DELETE FROM Route r WHERE r.fromVisit = :visit OR r.toVisit = :visit")
    void deleteByVisit(@Param("visit") Visit visit);
}
