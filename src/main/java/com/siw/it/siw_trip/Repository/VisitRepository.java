package com.siw.it.siw_trip.Repository;

import com.siw.it.siw_trip.Model.Visit;
import com.siw.it.siw_trip.Model.TripDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VisitRepository extends JpaRepository<Visit, Long> {
    
    @Query("SELECT v FROM Visit v WHERE v.tripDay = :tripDay ORDER BY v.startTime ASC NULLS LAST")
    List<Visit> findByTripDayOrderByStartTimeAsc(@Param("tripDay") TripDay tripDay);
    
    List<Visit> findByNameContainingIgnoreCase(String name);
    
    @Query("SELECT v FROM Visit v WHERE v.tripDay.id = :tripDayId ORDER BY v.startTime ASC NULLS LAST")
    List<Visit> findByTripDayIdOrderByStartTime(@Param("tripDayId") Long tripDayId);
    
    @Query("SELECT v FROM Visit v WHERE v.startTime BETWEEN :start AND :end")
    List<Visit> findByStartTimeBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("SELECT v FROM Visit v WHERE v.location.id = :locationId")
    List<Visit> findByLocationId(@Param("locationId") Long locationId);
    
    @Query("SELECT v FROM Visit v JOIN v.tripDay td JOIN td.trip t WHERE t.id = :tripId")
    List<Visit> findByTripId(@Param("tripId") Long tripId);
}
