package com.siw.it.siw_trip.Controller;

import com.siw.it.siw_trip.Model.TripDay;
import com.siw.it.siw_trip.Model.Visit;
import com.siw.it.siw_trip.Service.TripDayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/trip-days")
@CrossOrigin(origins = "*")
public class TripDayController {

    @Autowired
    private TripDayService tripDayService;

    @GetMapping
    public ResponseEntity<List<TripDay>> getAllTripDays() {
        List<TripDay> tripDays = tripDayService.findAll();
        return ResponseEntity.ok(tripDays);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TripDay> getTripDayById(@PathVariable Long id) {
        Optional<TripDay> tripDay = tripDayService.findById(id);
        return tripDay.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/trip/{tripId}")
    public ResponseEntity<List<TripDay>> getTripDaysByTrip(@PathVariable Long tripId) {
        List<TripDay> tripDays = tripDayService.findByTripId(tripId);
        return ResponseEntity.ok(tripDays);
    }

    @GetMapping("/search/date-range")
    public ResponseEntity<List<TripDay>> getTripDaysByDateRange(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        List<TripDay> tripDays = tripDayService.findByDateBetween(startDate, endDate);
        return ResponseEntity.ok(tripDays);
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<List<TripDay>> getTripDaysByDate(@PathVariable LocalDate date) {
        List<TripDay> tripDays = tripDayService.findByDate(date);
        return ResponseEntity.ok(tripDays);
    }

    @PostMapping
    public ResponseEntity<TripDay> createTripDay(@RequestBody TripDay tripDay) {
        TripDay savedTripDay = tripDayService.save(tripDay);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedTripDay);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TripDay> updateTripDay(@PathVariable Long id, @RequestBody TripDay tripDay) {
        if (!tripDayService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        tripDay.setId(id);
        TripDay updatedTripDay = tripDayService.save(tripDay);
        return ResponseEntity.ok(updatedTripDay);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTripDay(@PathVariable Long id) {
        if (!tripDayService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        tripDayService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/visits")
    public ResponseEntity<TripDay> addVisitToTripDay(
            @PathVariable Long id,
            @RequestBody Visit visit) {
        Optional<TripDay> tripDayOpt = tripDayService.findById(id);
        if (!tripDayOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        TripDay updatedTripDay = tripDayService.addVisit(id, visit);
        return ResponseEntity.ok(updatedTripDay);
    }

    @DeleteMapping("/{id}/visits/{visitId}")
    public ResponseEntity<TripDay> removeVisitFromTripDay(
            @PathVariable Long id,
            @PathVariable Long visitId) {
        Optional<TripDay> tripDayOpt = tripDayService.findById(id);
        if (!tripDayOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        TripDay updatedTripDay = tripDayService.removeVisit(id, visitId);
        return ResponseEntity.ok(updatedTripDay);
    }

    @PutMapping("/{id}/destination-visit")
    public ResponseEntity<TripDay> updateDestinationVisit(
            @PathVariable Long id,
            @RequestBody DestinationVisitUpdateRequest request) {
        Optional<TripDay> tripDayOpt = tripDayService.findById(id);
        if (!tripDayOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        TripDay updatedTripDay = tripDayService.updateDestinationVisit(id, request.getDestinationVisitId());
        return ResponseEntity.ok(updatedTripDay);
    }

    // Inner class for request body
    public static class DestinationVisitUpdateRequest {
        private Long destinationVisitId;
        
        // Getters and setters
        public Long getDestinationVisitId() { return destinationVisitId; }
        public void setDestinationVisitId(Long destinationVisitId) { this.destinationVisitId = destinationVisitId; }
    }
}
