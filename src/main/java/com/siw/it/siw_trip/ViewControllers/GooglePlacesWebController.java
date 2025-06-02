package com.siw.it.siw_trip.ViewControllers;

import com.siw.it.siw_trip.Model.Location;
import com.siw.it.siw_trip.Model.Trip;
import com.siw.it.siw_trip.Model.TripDay;
import com.siw.it.siw_trip.Model.User;
import com.siw.it.siw_trip.Model.Visit;
import com.siw.it.siw_trip.Model.dto.GooglePlaceDto;
import com.siw.it.siw_trip.Model.dto.PlaceSearchRequest;

import com.siw.it.siw_trip.Service.GooglePlacesService;
import com.siw.it.siw_trip.Service.LocationService;
import com.siw.it.siw_trip.Service.TripService;
import com.siw.it.siw_trip.Service.TripDayService;



import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Web Controller for form-based interactions
@Controller
public class GooglePlacesWebController {
    
    private static final Logger logger = LoggerFactory.getLogger(GooglePlacesWebController.class);
    
    private final GooglePlacesService googlePlacesService;
    private final LocationService locationService;
    private final TripService tripService;
    private final TripDayService tripDayService;
    
    public GooglePlacesWebController(GooglePlacesService googlePlacesService, 
                                   LocationService locationService,
                                   TripService tripService,
                                   TripDayService tripDayService) {
        this.googlePlacesService = googlePlacesService;
        this.locationService = locationService;
        this.tripService = tripService;
        this.tripDayService = tripDayService;
    }
    
    /**
     * Handle place search form submission
     */
    @PostMapping("/trips/{tripId}/days/{tripDayId}/locations/search")
    public String searchPlaces(@PathVariable Long tripId,
                              @PathVariable Long tripDayId,
                              @RequestParam("q") String query,
                              @RequestParam(value = "lat", required = false) Double latitude,
                              @RequestParam(value = "lng", required = false) Double longitude,
                              @RequestParam(value = "radius", required = false) Integer radius,
                              @RequestParam(value = "type", required = false) String type,
                              Model model,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        
        logger.info("Starting search places request - tripId: {}, tripDayId: {}, query: '{}', lat: {}, lng: {}, radius: {}, type: {}", 
                   tripId, tripDayId, query, latitude, longitude, radius, type);
        
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            logger.warn("User not logged in, redirecting to login");
            return "redirect:/login";
        }
        
        logger.info("User authenticated: {}", loggedInUser.getUsername());
        
        try {
            logger.info("Looking up trip with ID: {}", tripId);
            Trip trip = tripService.findById(tripId).orElseThrow(() -> 
                new IllegalArgumentException("Trip not found"));
            
            logger.info("Found trip: {} (ID: {})", trip.getName(), trip.getId());
            
            if (!trip.getAdmins().contains(loggedInUser)) {
                logger.warn("User {} not authorized to modify trip {}", loggedInUser.getUsername(), tripId);
                redirectAttributes.addFlashAttribute("errorMessage", "Not authorized to modify this trip");
                return "redirect:/trips/" + tripId + "/itinerary";
            }
            
            logger.info("Looking up trip day with ID: {}", tripDayId);
            TripDay tripDay = tripDayService.findById(tripDayId).orElseThrow(() -> 
                new IllegalArgumentException("Trip day not found"));
            
            logger.info("Found trip day: {} for trip: {}", tripDay.getId(), tripDay.getTrip().getId());
            
            if (!tripDay.getTrip().getId().equals(tripId)) {
                logger.warn("Trip day {} does not belong to trip {}", tripDayId, tripId);
                redirectAttributes.addFlashAttribute("errorMessage", "Trip day does not belong to this trip");
                return "redirect:/trips/" + tripId + "/itinerary";
            }
            
            logger.info("Checking Google Places API configuration");
            if (!googlePlacesService.isConfigured()) {
                logger.warn("Google Places API not configured");
                redirectAttributes.addFlashAttribute("errorMessage", "Google Places API not configured");
                return "redirect:/trips/" + tripId + "/days/" + tripDayId + "/locations/new";
            }
            
            logger.info("Creating place search request");
            PlaceSearchRequest request = new PlaceSearchRequest(query, latitude, longitude, radius);
            request.setType(type);
            request.setLanguage("en");
            
            logger.info("Calling Google Places API with request: {}", request);
            List<GooglePlaceDto> places = googlePlacesService.searchPlaces(request);
            logger.info("Google Places API returned {} results", places != null ? places.size() : 0);
            
            // Get existing locations for the form
            logger.info("Retrieving existing locations for trip");
            List<Location> existingLocations = trip.getTripDays().stream()
                    .flatMap(day -> day.getVisits().stream())
                    .map(Visit::getLocation)
                    .distinct()
                    .toList();
            logger.info("Found {} existing locations", existingLocations.size());
            
            // Add search results to model
            logger.info("Adding attributes to model");
            model.addAttribute("trip", trip);
            model.addAttribute("tripDay", tripDay);
            model.addAttribute("location", new Location());
            model.addAttribute("existingLocations", existingLocations);
            model.addAttribute("googlePlacesService", googlePlacesService);
            model.addAttribute("searchResults", places);
            model.addAttribute("searchQuery", query);
            
            logger.info("Returning view: locations/create");
            return "locations/create";
            
        } catch (Exception e) {
            logger.error("Error in searchPlaces method", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error searching places: " + e.getMessage());
            return "redirect:/trips/" + tripId + "/days/" + tripDayId + "/locations/new";
        }
    }
    
    /**
     * Create location from selected Google Place
     */
    @PostMapping("/trips/{tripId}/days/{tripDayId}/locations/from-place")
    public String createLocationFromPlace(@PathVariable Long tripId,
                                        @PathVariable Long tripDayId,
                                        @RequestParam("placeId") String placeId,
                                        @RequestParam(value = "description", required = false) String description,
                                        HttpSession session,
                                        RedirectAttributes redirectAttributes) {
        
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }
        
        try {
            Trip trip = tripService.findById(tripId).orElseThrow(() -> 
                new IllegalArgumentException("Trip not found"));
            
            if (!trip.getAdmins().contains(loggedInUser)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Not authorized to modify this trip");
                return "redirect:/trips/" + tripId + "/itinerary";
            }
            
            TripDay tripDay = tripDayService.findById(tripDayId).orElseThrow(() -> 
                new IllegalArgumentException("Trip day not found"));
            
            if (!tripDay.getTrip().getId().equals(tripId)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Trip day does not belong to this trip");
                return "redirect:/trips/" + tripId + "/itinerary";
            }
            
            // Get place details from Google Places API
            GooglePlaceDto place = googlePlacesService.getPlaceDetails(placeId);
            if (place == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Place not found or invalid place ID");
                return "redirect:/trips/" + tripId + "/days/" + tripDayId + "/locations/new";
            }
            
            // Create location from place data
            Location location = new Location();
            location.setName(place.getName());
            location.setAddress(place.getFormattedAddress());
            location.setLatitude(place.getLatitude());
            location.setLongitude(place.getLongitude());
            location.setCity(place.getCity());
            location.setCountry(place.getCountry());
            location.setDescription(description);
            
            // Save location
            Location savedLocation = locationService.save(location);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Location '" + savedLocation.getName() + "' created successfully from Google Place");
            
            // Redirect to visit creation with the location ID
            return "redirect:/trips/" + tripId + "/days/" + tripDayId + "/visits/new?locationId=" + savedLocation.getId();
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating location: " + e.getMessage());
            return "redirect:/trips/" + tripId + "/days/" + tripDayId + "/locations/new";
        }
    }
}