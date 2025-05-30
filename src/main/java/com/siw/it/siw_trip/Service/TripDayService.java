package com.siw.it.siw_trip.Service;

import com.siw.it.siw_trip.Model.TripDay;
import com.siw.it.siw_trip.Model.Trip;
import com.siw.it.siw_trip.Repository.TripDayRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class TripDayService {

    @Autowired
    private TripDayRepository tripDayRepository;

    public List<TripDay> findAll() {
        return tripDayRepository.findAll();
    }

    public Optional<TripDay> findById(Long id) {
        return tripDayRepository.findById(id);
    }

    public TripDay save(TripDay tripDay) {
        return tripDayRepository.save(tripDay);
    }

    public void deleteById(Long id) {
        tripDayRepository.deleteById(id);
    }

    public List<TripDay> findByTripOrderByDayNumber(Trip trip) {
        return tripDayRepository.findByTripOrderByDayNumber(trip);
    }

    public List<TripDay> findByTripIdOrderByDayNumber(Long tripId) {
        return tripDayRepository.findByTripIdOrderByDayNumber(tripId);
    }

    public List<TripDay> findByDate(LocalDate date) {
        return tripDayRepository.findByDate(date);
    }

    public List<TripDay> findByDateBetween(LocalDate startDate, LocalDate endDate) {
        return tripDayRepository.findByDateBetween(startDate, endDate);
    }

    public TripDay findByTripIdAndDayNumber(Long tripId, Integer dayNumber) {
        return tripDayRepository.findByTripIdAndDayNumber(tripId, dayNumber);
    }

    public boolean existsById(Long id) {
        return tripDayRepository.existsById(id);
    }

    // Alias method for backward compatibility with controller
    public List<TripDay> findByTripId(Long tripId) {
        return findByTripIdOrderByDayNumber(tripId);
    }

    /**
     * Add a visit to a trip day
     */
    public TripDay addVisit(Long tripDayId, com.siw.it.siw_trip.Model.Visit visit) {
        Optional<TripDay> tripDayOpt = findById(tripDayId);
        if (!tripDayOpt.isPresent()) {
            throw new IllegalArgumentException("TripDay not found with id: " + tripDayId);
        }

        TripDay tripDay = tripDayOpt.get();
        visit.setTripDay(tripDay);
        tripDay.addVisit(visit);
        
        return save(tripDay);
    }

    /**
     * Remove a visit from a trip day
     */
    public TripDay removeVisit(Long tripDayId, Long visitId) {
        Optional<TripDay> tripDayOpt = findById(tripDayId);
        if (!tripDayOpt.isPresent()) {
            throw new IllegalArgumentException("TripDay not found with id: " + tripDayId);
        }

        TripDay tripDay = tripDayOpt.get();
        // Find the visit by ID and remove it
        com.siw.it.siw_trip.Model.Visit visitToRemove = tripDay.getVisits().stream()
            .filter(visit -> visit.getId().equals(visitId))
            .findFirst()
            .orElse(null);
            
        if (visitToRemove != null) {
            tripDay.removeVisit(visitToRemove);
        }
        
        return save(tripDay);
    }

    /**
     * Update the destination visit for a trip day
     */
    public TripDay updateDestinationVisit(Long tripDayId, Long destinationVisitId) {
        Optional<TripDay> tripDayOpt = findById(tripDayId);
        if (!tripDayOpt.isPresent()) {
            throw new IllegalArgumentException("TripDay not found with id: " + tripDayId);
        }

        TripDay tripDay = tripDayOpt.get();
        // Find the visit by ID in the trip day's visits
        com.siw.it.siw_trip.Model.Visit destinationVisit = tripDay.getVisits().stream()
            .filter(visit -> visit.getId().equals(destinationVisitId))
            .findFirst()
            .orElse(null);
            
        tripDay.setDestination(destinationVisit);
        
        return save(tripDay);
    }
}
