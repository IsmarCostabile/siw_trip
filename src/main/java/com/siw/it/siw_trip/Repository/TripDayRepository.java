package com.siw.it.siw_trip.Repository;

import com.siw.it.siw_trip.Model.TripDay;
import com.siw.it.siw_trip.Model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TripDayRepository extends JpaRepository<TripDay, Long> {
    
    List<TripDay> findByTripOrderByDayNumber(Trip trip);
    
    List<TripDay> findByDate(LocalDate date);
    
    @Query("SELECT td FROM TripDay td WHERE td.trip.id = :tripId ORDER BY td.dayNumber ASC")
    List<TripDay> findByTripIdOrderByDayNumber(@Param("tripId") Long tripId);
    
    @Query("SELECT td FROM TripDay td WHERE td.date BETWEEN :startDate AND :endDate")
    List<TripDay> findByDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT td FROM TripDay td WHERE td.trip.id = :tripId AND td.dayNumber = :dayNumber")
    TripDay findByTripIdAndDayNumber(@Param("tripId") Long tripId, @Param("dayNumber") Integer dayNumber);
}
