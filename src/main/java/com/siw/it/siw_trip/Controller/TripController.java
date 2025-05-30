package com.siw.it.siw_trip.Controller;

import com.siw.it.siw_trip.Model.Trip;
import com.siw.it.siw_trip.Model.TripDay;
import com.siw.it.siw_trip.Model.TripStatus;
import com.siw.it.siw_trip.Service.TripService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/trips")
@CrossOrigin(origins = "*")
public class TripController {

    @Autowired
    private TripService tripService;

    @GetMapping
    public ResponseEntity<List<Trip>> getAllTrips() {
        List<Trip> trips = tripService.findAll();
        return ResponseEntity.ok(trips);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Trip> getTripById(@PathVariable Long id) {
        try {
            Trip trip = tripService.findByIdOrThrow(id);
            return ResponseEntity.ok(trip);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search/name")
    public ResponseEntity<List<Trip>> searchTripsByName(@RequestParam String name) {
        List<Trip> trips = tripService.findByNameContaining(name);
        return ResponseEntity.ok(trips);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Trip>> getTripsByStatus(@PathVariable TripStatus status) {
        List<Trip> trips = tripService.findByStatus(status);
        return ResponseEntity.ok(trips);
    }

    @GetMapping("/participant/{userId}")
    public ResponseEntity<List<Trip>> getTripsByParticipant(@PathVariable Long userId) {
        List<Trip> trips = tripService.findByParticipantId(userId);
        return ResponseEntity.ok(trips);
    }

    @GetMapping("/admin/{userId}")
    public ResponseEntity<List<Trip>> getTripsByAdmin(@PathVariable Long userId) {
        List<Trip> trips = tripService.findByAdminId(userId);
        return ResponseEntity.ok(trips);
    }

    @GetMapping("/search/date-range")
    public ResponseEntity<List<Trip>> getTripsByDateRange(
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {
        List<Trip> trips = tripService.findByDateRange(startDate, endDate);
        return ResponseEntity.ok(trips);
    }

    @PostMapping
    public ResponseEntity<Trip> createTrip(@RequestBody Trip trip) {
        Trip savedTrip = tripService.save(trip);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedTrip);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Trip> updateTrip(@PathVariable Long id, @RequestBody Trip trip) {
        try {
            tripService.findByIdOrThrow(id); // Validate trip exists
            trip.setId(id);
            Trip updatedTrip = tripService.save(trip);
            return ResponseEntity.ok(updatedTrip);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrip(@PathVariable Long id) {
        try {
            tripService.findByIdOrThrow(id); // Validate trip exists
            tripService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/participants")
    public ResponseEntity<Trip> addParticipant(
            @PathVariable Long id,
            @RequestBody ParticipantRequest request) {
        try {
            Trip updatedTrip = tripService.addParticipant(id, request.getUserId());
            return ResponseEntity.ok(updatedTrip);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/participants/{userId}")
    public ResponseEntity<Trip> removeParticipant(
            @PathVariable Long id,
            @PathVariable Long userId) {
        try {
            Trip updatedTrip = tripService.removeParticipant(id, userId);
            return ResponseEntity.ok(updatedTrip);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/admins")
    public ResponseEntity<Trip> addAdmin(
            @PathVariable Long id,
            @RequestBody AdminRequest request) {
        try {
            Trip updatedTrip = tripService.addAdmin(id, request.getUserId());
            return ResponseEntity.ok(updatedTrip);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/admins/{userId}")
    public ResponseEntity<Trip> removeAdmin(
            @PathVariable Long id,
            @PathVariable Long userId) {
        try {
            Trip updatedTrip = tripService.removeAdmin(id, userId);
            return ResponseEntity.ok(updatedTrip);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/trip-days")
    public ResponseEntity<Trip> addTripDay(
            @PathVariable Long id,
            @RequestBody TripDay tripDay) {
        try {
            Trip updatedTrip = tripService.addTripDay(id, tripDay);
            return ResponseEntity.ok(updatedTrip);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/trip-days/{tripDayId}")
    public ResponseEntity<Trip> removeTripDay(
            @PathVariable Long id,
            @PathVariable Long tripDayId) {
        try {
            Trip updatedTrip = tripService.removeTripDay(id, tripDayId);
            return ResponseEntity.ok(updatedTrip);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Trip> updateTripStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request) {
        try {
            Trip updatedTrip = tripService.updateStatus(id, request.getStatus());
            return ResponseEntity.ok(updatedTrip);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Inner classes for request bodies
    public static class ParticipantRequest {
        private Long userId;
        
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
    }

    public static class AdminRequest {
        private Long userId;
        
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
    }

    public static class StatusUpdateRequest {
        private TripStatus status;
        
        public TripStatus getStatus() { return status; }
        public void setStatus(TripStatus status) { this.status = status; }
    }

    public static class BudgetUpdateRequest {
        private BigDecimal budget;
        
        public BigDecimal getBudget() { return budget; }
        public void setBudget(BigDecimal budget) { this.budget = budget; }
    }
}
