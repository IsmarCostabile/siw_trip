package com.siw.it.siw_trip.Controller;

import com.siw.it.siw_trip.Model.Location;
import com.siw.it.siw_trip.Service.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/locations")
@CrossOrigin(origins = "*")
public class LocationController {

    @Autowired
    private LocationService locationService;

    @GetMapping
    public ResponseEntity<List<Location>> getAllLocations() {
        List<Location> locations = locationService.findAll();
        return ResponseEntity.ok(locations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Location> getLocationById(@PathVariable Long id) {
        Optional<Location> location = locationService.findById(id);
        return location.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search/name")
    public ResponseEntity<List<Location>> searchLocationsByName(@RequestParam String name) {
        List<Location> locations = locationService.findByNameContaining(name);
        return ResponseEntity.ok(locations);
    }

    @GetMapping("/search/city")
    public ResponseEntity<List<Location>> getLocationsByCity(@RequestParam String city) {
        List<Location> locations = locationService.findByCity(city);
        return ResponseEntity.ok(locations);
    }

    @GetMapping("/search/country")
    public ResponseEntity<List<Location>> getLocationsByCountry(@RequestParam String country) {
        List<Location> locations = locationService.findByCountry(country);
        return ResponseEntity.ok(locations);
    }

    @GetMapping("/search/nearby")
    public ResponseEntity<List<Location>> getNearbyLocations(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam Double radius) {
        List<Location> locations = locationService.findNearbyLocations(latitude, longitude, radius);
        return ResponseEntity.ok(locations);
    }

    @PostMapping
    public ResponseEntity<Location> createLocation(@RequestBody Location location) {
        Location savedLocation = locationService.save(location);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedLocation);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Location> updateLocation(@PathVariable Long id, @RequestBody Location location) {
        if (!locationService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        location.setId(id);
        Location updatedLocation = locationService.save(location);
        return ResponseEntity.ok(updatedLocation);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLocation(@PathVariable Long id) {
        if (!locationService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        locationService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
