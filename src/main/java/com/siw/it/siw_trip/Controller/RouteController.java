package com.siw.it.siw_trip.Controller;

import com.siw.it.siw_trip.Model.Route;
import com.siw.it.siw_trip.Model.TransportMode;
import com.siw.it.siw_trip.Service.RouteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/routes")
@CrossOrigin(origins = "*")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @PostMapping("/{id}/calculate")
    public ResponseEntity<Route> calculateRoute(@PathVariable Long id) {
        try {
            Optional<Route> routeOpt = routeService.findById(id);
            if (!routeOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Route calculatedRoute = routeService.calculateRouteDetails(routeOpt.get());
            return ResponseEntity.ok(calculatedRoute);
        } catch (Exception e) {
            // Log error and return the route as-is for now
            System.err.println("Error calculating route: " + e.getMessage());
            Optional<Route> routeOpt = routeService.findById(id);
            return routeOpt.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
        }
    }

    @GetMapping
    public ResponseEntity<List<Route>> getAllRoutes() {
        List<Route> routes = routeService.findAll();
        return ResponseEntity.ok(routes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Route> getRouteById(@PathVariable Long id) {
        Optional<Route> route = routeService.findById(id);
        return route.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/visit/{visitId}")
    public ResponseEntity<List<Route>> getRoutesByVisit(@PathVariable Long visitId) {
        List<Route> routes = routeService.findByVisitId(visitId);
        return ResponseEntity.ok(routes);
    }

    @GetMapping("/trip/{tripId}")
    public ResponseEntity<List<Route>> getRoutesByTrip(@PathVariable Long tripId) {
        List<Route> routes = routeService.findByTripId(tripId);
        return ResponseEntity.ok(routes);
    }

    @GetMapping("/transport-mode/{transportMode}")
    public ResponseEntity<List<Route>> getRoutesByTransportMode(@PathVariable TransportMode transportMode) {
        List<Route> routes = routeService.findByTransportMode(transportMode);
        return ResponseEntity.ok(routes);
    }

    @GetMapping("/search/duration")
    public ResponseEntity<List<Route>> getRoutesByMinDuration(@RequestParam Integer minDuration) {
        List<Route> routes = routeService.findByEstimatedDurationGreaterThan(minDuration);
        return ResponseEntity.ok(routes);
    }

    @GetMapping("/search/between-visits")
    public ResponseEntity<Route> getRouteBetweenVisits(
            @RequestParam Long fromVisitId,
            @RequestParam Long toVisitId) {
        // This would require additional service method implementation
        // For now, return not implemented
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @PostMapping
    public ResponseEntity<Route> createRoute(@RequestBody Route route) {
        Route savedRoute = routeService.saveAndCalculate(route);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedRoute);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Route> updateRoute(@PathVariable Long id, @RequestBody Route route) {
        if (!routeService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        route.setId(id);
        Route updatedRoute = routeService.saveAndCalculate(route);
        return ResponseEntity.ok(updatedRoute);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id) {
        if (!routeService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        routeService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/duration")
    public ResponseEntity<Route> updateRouteDuration(
            @PathVariable Long id,
            @RequestBody DurationUpdateRequest request) {
        Optional<Route> routeOpt = routeService.findById(id);
        if (!routeOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        Route route = routeOpt.get();
        route.setEstimatedDurationMinutes(request.getDuration());
        Route updatedRoute = routeService.save(route);
        return ResponseEntity.ok(updatedRoute);
    }

    @GetMapping("/trip-day/{tripDayId}")
    public ResponseEntity<List<Route>> getRoutesByTripDay(@PathVariable Long tripDayId) {
        List<Route> routes = routeService.findByTripDayId(tripDayId);
        return ResponseEntity.ok(routes);
    }

    @PostMapping("/between-visits")
    public ResponseEntity<?> createRouteBetweenVisits(@RequestBody CreateRouteRequest request) {
        try {
            // Validate request
            if (request.getFromVisitId() == null || request.getToVisitId() == null || request.getTripDayId() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "fromVisitId, toVisitId, and tripDayId are required"));
            }

            // Create route object using the service
            Route route = routeService.createRouteBetweenVisits(
                    request.getFromVisitId(), 
                    request.getToVisitId(), 
                    request.getTripDayId()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(route);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create route: " + e.getMessage()));
        }
    }

    @GetMapping("/calculation-status")
    public ResponseEntity<Boolean> getCalculationStatus() {
        boolean available = routeService.isRouteCalculationAvailable();
        return ResponseEntity.ok(available);
    }

    // Inner class for request body
    public static class DurationUpdateRequest {
        private Integer duration;
        
        // Getters and setters
        public Integer getDuration() { return duration; }
        public void setDuration(Integer duration) { this.duration = duration; }
    }

    public static class CreateRouteRequest {
        private Long fromVisitId;
        private Long toVisitId;
        private Long tripDayId;

        // Getters and setters
        public Long getFromVisitId() { return fromVisitId; }
        public void setFromVisitId(Long fromVisitId) { this.fromVisitId = fromVisitId; }

        public Long getToVisitId() { return toVisitId; }
        public void setToVisitId(Long toVisitId) { this.toVisitId = toVisitId; }

        public Long getTripDayId() { return tripDayId; }
        public void setTripDayId(Long tripDayId) { this.tripDayId = tripDayId; }
    }
}
