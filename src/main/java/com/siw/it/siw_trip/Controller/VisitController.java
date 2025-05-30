package com.siw.it.siw_trip.Controller;

import com.siw.it.siw_trip.Model.Visit;
import com.siw.it.siw_trip.Service.VisitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/visits")
@CrossOrigin(origins = "*")
public class VisitController {

    @Autowired
    private VisitService visitService;

    @GetMapping
    public ResponseEntity<List<Visit>> getAllVisits() {
        List<Visit> visits = visitService.findAll();
        return ResponseEntity.ok(visits);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Visit> getVisitById(@PathVariable Long id) {
        Optional<Visit> visit = visitService.findById(id);
        return visit.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/trip-day/{tripDayId}")
    public ResponseEntity<List<Visit>> getVisitsByTripDay(@PathVariable Long tripDayId) {
        List<Visit> visits = visitService.findByTripDayId(tripDayId);
        return ResponseEntity.ok(visits);
    }

    @GetMapping("/location/{locationId}")
    public ResponseEntity<List<Visit>> getVisitsByLocation(@PathVariable Long locationId) {
        List<Visit> visits = visitService.findByLocationId(locationId);
        return ResponseEntity.ok(visits);
    }

    @GetMapping("/trip/{tripId}")
    public ResponseEntity<List<Visit>> getVisitsByTrip(@PathVariable Long tripId) {
        List<Visit> visits = visitService.findByTripId(tripId);
        return ResponseEntity.ok(visits);
    }

    @GetMapping("/search/date-range")
    public ResponseEntity<List<Visit>> getVisitsByDateRange(
            @RequestParam LocalDateTime startTime,
            @RequestParam LocalDateTime endTime) {
        List<Visit> visits = visitService.findByStartTimeBetween(startTime, endTime);
        return ResponseEntity.ok(visits);
    }

    @GetMapping("/search/name")
    public ResponseEntity<List<Visit>> searchVisitsByName(@RequestParam String name) {
        List<Visit> visits = visitService.findByNameContaining(name);
        return ResponseEntity.ok(visits);
    }

    @GetMapping("/trip-day/{tripDayId}/ordered")
    public ResponseEntity<List<Visit>> getVisitsByTripDayOrdered(@PathVariable Long tripDayId) {
        List<Visit> visits = visitService.findByTripDayIdOrderByStartTime(tripDayId);
        return ResponseEntity.ok(visits);
    }

    @GetMapping("/{id}/exists")
    public ResponseEntity<Boolean> checkVisitExists(@PathVariable Long id) {
        boolean exists = visitService.existsById(id);
        return ResponseEntity.ok(exists);
    }

    @PatchMapping("/{id}/duration")
    public ResponseEntity<Visit> updateEstimatedDuration(
            @PathVariable Long id, 
            @RequestParam Integer estimatedDurationMinutes) {
        Optional<Visit> visitOpt = visitService.findById(id);
        if (!visitOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        Visit visit = visitOpt.get();
        visit.setEstimatedDurationMinutes(estimatedDurationMinutes);
        Visit updatedVisit = visitService.save(visit);
        return ResponseEntity.ok(updatedVisit);
    }

    @PatchMapping("/{id}/notes")
    public ResponseEntity<Visit> updateVisitNotes(
            @PathVariable Long id, 
            @RequestBody String notes) {
        Optional<Visit> visitOpt = visitService.findById(id);
        if (!visitOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        Visit visit = visitOpt.get();
        visit.setNotes(notes);
        Visit updatedVisit = visitService.save(visit);
        return ResponseEntity.ok(updatedVisit);
    }

    @PostMapping
    public ResponseEntity<Visit> createVisit(@RequestBody Visit visit) {
        Visit savedVisit = visitService.save(visit);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedVisit);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<Visit>> createMultipleVisits(@RequestBody List<Visit> visits) {
        List<Visit> savedVisits = visits.stream()
                .map(visitService::save)
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(savedVisits);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Visit> updateVisit(@PathVariable Long id, @RequestBody Visit visit) {
        if (!visitService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        visit.setId(id);
        Visit updatedVisit = visitService.save(visit);
        return ResponseEntity.ok(updatedVisit);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVisit(@PathVariable Long id) {
        if (!visitService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        visitService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/batch")
    public ResponseEntity<Void> deleteMultipleVisits(@RequestBody List<Long> visitIds) {
        for (Long id : visitIds) {
            if (visitService.existsById(id)) {
                visitService.deleteById(id);
            }
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getVisitCount() {
        long count = visitService.findAll().size();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/trip/{tripId}/count")
    public ResponseEntity<Long> getVisitCountByTrip(@PathVariable Long tripId) {
        long count = visitService.findByTripId(tripId).size();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/trip-day/{tripDayId}/count")
    public ResponseEntity<Long> getVisitCountByTripDay(@PathVariable Long tripDayId) {
        long count = visitService.findByTripDayId(tripDayId).size();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/{id}/duration/actual")
    public ResponseEntity<Long> getActualVisitDuration(@PathVariable Long id) {
        Optional<Visit> visitOpt = visitService.findById(id);
        if (!visitOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        Visit visit = visitOpt.get();
        long actualDuration = visit.getActualDurationMinutes();
        return ResponseEntity.ok(actualDuration);
    }

    @GetMapping("/{id}/scheduled")
    public ResponseEntity<Boolean> isVisitScheduled(@PathVariable Long id) {
        Optional<Visit> visitOpt = visitService.findById(id);
        if (!visitOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        Visit visit = visitOpt.get();
        boolean isScheduled = visit.hasScheduledTime();
        return ResponseEntity.ok(isScheduled);
    }

    @PostMapping("/{id}/update-time")
    public ResponseEntity<Visit> updateVisitTime(
            @PathVariable Long id,
            @RequestBody VisitTimeUpdateRequest request) {
        Optional<Visit> visitOpt = visitService.findById(id);
        if (!visitOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        Visit updatedVisit = visitService.updateVisitTime(id, request.getStartTime(), request.getEndTime());
        return ResponseEntity.ok(updatedVisit);
    }

    // Inner class for request body
    public static class VisitTimeUpdateRequest {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        
        // Getters and setters
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    }
}
