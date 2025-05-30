package com.siw.it.siw_trip.Service;

import com.siw.it.siw_trip.Model.Location;
import com.siw.it.siw_trip.Repository.LocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LocationService {

    @Autowired
    private LocationRepository locationRepository;

    public List<Location> findAll() {
        return locationRepository.findAll();
    }

    public Optional<Location> findById(Long id) {
        return locationRepository.findById(id);
    }

    public Location save(Location location) {
        return locationRepository.save(location);
    }

    public void deleteById(Long id) {
        locationRepository.deleteById(id);
    }

    public List<Location> findByNameContaining(String name) {
        return locationRepository.findByNameContainingIgnoreCase(name);
    }

    public List<Location> findByCity(String city) {
        return locationRepository.findByCityIgnoreCase(city);
    }

    public List<Location> findByCountry(String country) {
        return locationRepository.findByCountryIgnoreCase(country);
    }

    public Optional<Location> findByNameAndCity(String name, String city) {
        return locationRepository.findByNameAndCity(name, city);
    }

    public List<Location> findAllWithCoordinates() {
        return locationRepository.findAllWithCoordinates();
    }

    /**
     * Find locations within a radius from a given point (basic implementation)
     * For production use, consider using a spatial database with proper GIS functions
     */
    public List<Location> findNearbyLocations(Double latitude, Double longitude, Double radius) {
        // Basic implementation - in production, use proper GIS queries
        return locationRepository.findAllWithCoordinates().stream()
            .filter(location -> {
                if (location.getLatitude() == null || location.getLongitude() == null) {
                    return false;
                }
                double distance = calculateDistance(latitude, longitude, 
                    location.getLatitude(), location.getLongitude());
                return distance <= radius;
            })
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Calculate distance between two points using Haversine formula
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // Distance in km
    }
}
