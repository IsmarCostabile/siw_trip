package com.siw.it.siw_trip.Controller;

import com.siw.it.siw_trip.Model.Route;
import com.siw.it.siw_trip.Model.TransportMode;
import com.siw.it.siw_trip.Service.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/routes")
@CrossOrigin(origins = "*")
public class RouteController {

    @Autowired
    private RouteService routeService;

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
        Route savedRoute = routeService.save(route);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedRoute);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Route> updateRoute(@PathVariable Long id, @RequestBody Route route) {
        if (!routeService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        route.setId(id);
        Route updatedRoute = routeService.save(route);
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

    // Inner class for request body
    public static class DurationUpdateRequest {
        private Integer duration;
        
        // Getters and setters
        public Integer getDuration() { return duration; }
        public void setDuration(Integer duration) { this.duration = duration; }
    }
}
