package com.siw.it.siw_trip.Controller;

import com.siw.it.siw_trip.Model.Location;
import com.siw.it.siw_trip.Model.Trip;
import com.siw.it.siw_trip.Model.TripDay;
import com.siw.it.siw_trip.Model.User;
import com.siw.it.siw_trip.Model.dto.GooglePlaceDto;
import com.siw.it.siw_trip.Model.dto.PlaceSearchRequest;
import com.siw.it.siw_trip.Model.dto.CreateLocationFromPlaceRequest;
import com.siw.it.siw_trip.Service.GooglePlacesService;
import com.siw.it.siw_trip.Service.LocationService;
import com.siw.it.siw_trip.Service.TripService;
import com.siw.it.siw_trip.Service.TripDayService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/places")
@CrossOrigin(origins = "*") // Configure as needed for your app
public class GooglePlacesController {
    
    private final GooglePlacesService googlePlacesService;
    private final LocationService locationService;
    private final TripService tripService;
    private final TripDayService tripDayService;
    
    @Autowired
    public GooglePlacesController(GooglePlacesService googlePlacesService, 
                                 LocationService locationService,
                                 TripService tripService,
                                 TripDayService tripDayService) {
        this.googlePlacesService = googlePlacesService;
        this.locationService = locationService;
        this.tripService = tripService;
        this.tripDayService = tripDayService;
    }
    
    /**
     * Search for places using text query
     * GET /api/places/search?q=eiffel tower&lat=48.8566&lng=2.3522&radius=5000&type=tourist_attraction&lang=en
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchPlaces(
            @RequestParam("q") String query,
            @RequestParam(value = "lat", required = false) Double latitude,
            @RequestParam(value = "lng", required = false) Double longitude,
            @RequestParam(value = "radius", required = false) Integer radius,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "lang", required = false) String language) {
        
        try {
            if (!googlePlacesService.isConfigured()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(createErrorResponse("Google Places API not configured"));
            }
            
            PlaceSearchRequest request = new PlaceSearchRequest(query, latitude, longitude, radius);
            request.setType(type);
            request.setLanguage(language != null ? language : "en");
            
            List<GooglePlaceDto> places = googlePlacesService.searchPlaces(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("places", places);
            response.put("count", places.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error searching places: " + e.getMessage()));
        }
    }
    
    /**
     * Get detailed information about a specific place
     * GET /api/places/{placeId}/details
     */
    @GetMapping("/{placeId}/details")
    public ResponseEntity<?> getPlaceDetails(@PathVariable String placeId) {
        try {
            if (!googlePlacesService.isConfigured()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(createErrorResponse("Google Places API not configured"));
            }
            
            GooglePlaceDto place = googlePlacesService.getPlaceDetails(placeId);
            
            if (place == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("place", place);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error getting place details: " + e.getMessage()));
        }
    }
    
    /**
     * Search for nearby places
     * GET /api/places/nearby?lat=48.8566&lng=2.3522&radius=1000&type=restaurant
     */
    @GetMapping("/nearby")
    public ResponseEntity<?> searchNearbyPlaces(
            @RequestParam("lat") Double latitude,
            @RequestParam("lng") Double longitude,
            @RequestParam(value = "radius", required = false, defaultValue = "5000") Integer radius,
            @RequestParam(value = "type", required = false) String type) {
        
        try {
            if (!googlePlacesService.isConfigured()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(createErrorResponse("Google Places API not configured"));
            }
            
            List<GooglePlaceDto> places = googlePlacesService.searchNearbyPlaces(latitude, longitude, radius, type);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("places", places);
            response.put("count", places.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error searching nearby places: " + e.getMessage()));
        }
    }
    
    /**
     * Create a location from a Google Place and redirect to visit creation
     * POST /api/places/create-location
     */
    @PostMapping("/create-location")
    public ResponseEntity<?> createLocationFromPlace(
            @Valid @RequestBody CreateLocationFromPlaceRequest request,
            HttpSession session) {
        
        try {
            // Check authentication
            User loggedInUser = (User) session.getAttribute("loggedInUser");
            if (loggedInUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Authentication required"));
            }
            
            // Validate trip access
            Trip trip = tripService.findById(request.getTripId()).orElse(null);
            if (trip == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (!trip.getAdmins().contains(loggedInUser)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("Not authorized to modify this trip"));
            }
            
            // Validate trip day
            TripDay tripDay = tripDayService.findById(request.getTripDayId()).orElse(null);
            if (tripDay == null || !tripDay.getTrip().getId().equals(trip.getId())) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Invalid trip day"));
            }
            
            // Get place details from Google Places API
            GooglePlaceDto place = googlePlacesService.getPlaceDetails(request.getPlaceId());
            if (place == null) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Place not found or invalid place ID"));
            }
            
            // Create location from place data
            Location location = new Location();
            location.setName(place.getName());
            location.setAddress(place.getFormattedAddress());
            location.setLatitude(place.getLatitude());
            location.setLongitude(place.getLongitude());
            location.setCity(place.getCity());
            location.setCountry(place.getCountry());
            location.setDescription(request.getDescription());
            
            // Save location
            Location savedLocation = locationService.save(location);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("location", savedLocation);
            response.put("message", "Location created successfully from Google Place");
            response.put("visitCreationUrl", "/trips/" + trip.getId() + "/days/" + tripDay.getId() + "/visits/new?locationId=" + savedLocation.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error creating location: " + e.getMessage()));
        }
    }
    
    /**
     * Check if Google Places API is configured
     * GET /api/places/status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getApiStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("configured", googlePlacesService.isConfigured());
        response.put("service", "Google Places API");
        
        return ResponseEntity.ok(response);
    }
    
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        return error;
    }
}
