package com.siw.it.siw_trip.Repository;

import com.siw.it.siw_trip.Model.Trip;
import com.siw.it.siw_trip.Model.User;
import com.siw.it.siw_trip.Model.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    
    List<Trip> findByStatus(TripStatus status);
    
    List<Trip> findByNameContainingIgnoreCase(String name);
    
    @Query("SELECT t FROM Trip t JOIN t.participants p WHERE p.id = :userId")
    List<Trip> findByParticipantId(@Param("userId") Long userId);
    
    @Query("SELECT t FROM Trip t JOIN t.admins a WHERE a.id = :userId")
    List<Trip> findByAdminId(@Param("userId") Long userId);
    
    @Query("SELECT t FROM Trip t WHERE t.startDateTime >= :startDate AND t.endDateTime <= :endDate")
    List<Trip> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT t FROM Trip t WHERE t.startDateTime >= :date")
    List<Trip> findUpcomingTrips(@Param("date") LocalDateTime date);
    
    @Query("SELECT t FROM Trip t WHERE t.endDateTime < :date")
    List<Trip> findPastTrips(@Param("date") LocalDateTime date);
    
    @Query("SELECT t FROM Trip t WHERE :user MEMBER OF t.participants OR :user MEMBER OF t.admins")
    List<Trip> findTripsForUser(@Param("user") User user);
    
    @Query("SELECT t FROM Trip t WHERE t.status = :status AND (:user MEMBER OF t.participants OR :user MEMBER OF t.admins)")
    List<Trip> findTripsForUserByStatus(@Param("user") User user, @Param("status") TripStatus status);
}
