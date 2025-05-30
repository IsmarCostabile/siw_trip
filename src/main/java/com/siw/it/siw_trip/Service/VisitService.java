package com.siw.it.siw_trip.Service;

import com.siw.it.siw_trip.Model.Visit;
import com.siw.it.siw_trip.Model.TripDay;
import com.siw.it.siw_trip.Repository.VisitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class VisitService {

    @Autowired
    private VisitRepository visitRepository;

    public List<Visit> findAll() {
        return visitRepository.findAll();
    }

    public Optional<Visit> findById(Long id) {
        return visitRepository.findById(id);
    }

    public Visit save(Visit visit) {
        return visitRepository.save(visit);
    }

    public void deleteById(Long id) {
        visitRepository.deleteById(id);
    }

    public List<Visit> findByTripDayOrderByStartTime(TripDay tripDay) {
        return visitRepository.findByTripDayOrderByStartTimeAsc(tripDay);
    }

    public List<Visit> findByTripDayIdOrderByStartTime(Long tripDayId) {
        return visitRepository.findByTripDayIdOrderByStartTime(tripDayId);
    }

    public List<Visit> findByNameContaining(String name) {
        return visitRepository.findByNameContainingIgnoreCase(name);
    }

    public List<Visit> findByStartTimeBetween(LocalDateTime start, LocalDateTime end) {
        return visitRepository.findByStartTimeBetween(start, end);
    }

    public List<Visit> findByLocationId(Long locationId) {
        return visitRepository.findByLocationId(locationId);
    }

    public List<Visit> findByTripId(Long tripId) {
        return visitRepository.findByTripId(tripId);
    }

    public boolean existsById(Long id) {
        return visitRepository.existsById(id);
    }

    // Alias method for backward compatibility with controller
    public List<Visit> findByTripDayId(Long tripDayId) {
        return findByTripDayIdOrderByStartTime(tripDayId);
    }

    /**
     * Update visit time while maintaining validation
     */
    public Visit updateVisitTime(Long visitId, LocalDateTime startTime, LocalDateTime endTime) {
        Optional<Visit> visitOpt = findById(visitId);
        if (!visitOpt.isPresent()) {
            throw new IllegalArgumentException("Visit not found with id: " + visitId);
        }

        Visit visit = visitOpt.get();
        visit.setStartTime(startTime);
        visit.setEndTime(endTime);

        return save(visit);
    }
}
