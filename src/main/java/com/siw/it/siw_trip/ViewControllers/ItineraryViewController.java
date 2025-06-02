package com.siw.it.siw_trip.ViewControllers;

import com.siw.it.siw_trip.Model.Trip;
import com.siw.it.siw_trip.Model.TripDay;
import com.siw.it.siw_trip.Model.Visit;
import com.siw.it.siw_trip.Model.User;
import com.siw.it.siw_trip.Model.Location;
import com.siw.it.siw_trip.Model.Route;
import com.siw.it.siw_trip.Model.TransportMode;
import com.siw.it.siw_trip.Model.dto.GooglePlaceDto;
import com.siw.it.siw_trip.Model.dto.PlaceSearchRequest;
import com.siw.it.siw_trip.Service.TripService;
import com.siw.it.siw_trip.Service.TripDayService;
import com.siw.it.siw_trip.Service.VisitService;
import com.siw.it.siw_trip.Service.LocationService;
import com.siw.it.siw_trip.Service.RouteService;
import com.siw.it.siw_trip.Service.GooglePlacesService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/itinerary")
public class ItineraryViewController {

    private final TripService tripService;
    
    // Constants to avoid code duplication
    private static final String LOGGED_IN_USER = "loggedInUser";
    private static final String REDIRECT_LOGIN = "redirect:/login";
    private static final String REDIRECT_TRIPS = "redirect:/trips";
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String TRIP_NOT_FOUND = "Trip not found";
    private static final String NOT_AUTHORIZED_VIEW = "You are not authorized to view this trip";
    private static final String TOTAL_DAYS = "totalDays";
    private static final String ALL_DAYS = "allDays";

    public ItineraryViewController(TripService tripService) {
        this.tripService = tripService;
    }

    /**
     * Helper method to check if user can view a trip (participant or admin)
     */
    private boolean canViewTrip(Trip trip, User user) {
        return trip.getParticipants().contains(user) || trip.getAdmins().contains(user);
    }

    /**
     * Helper method to check if user can edit a trip (admin only)
     */
    private boolean canEditTrip(Trip trip, User user) {
        return trip.getAdmins().contains(user);
    }

    /**
     * Display trip itinerary with day navigation
     */
    @GetMapping("/{tripId}")
    public String viewTripItinerary(@PathVariable Long tripId, 
                                  @RequestParam(defaultValue = "0") int startDay,
                                  Model model, 
                                  HttpSession session, 
                                  RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }
        
        try {
            Trip trip = tripService.findByIdOrThrow(tripId);
            
            // Check if user is authorized to view this trip (participant or admin)
            if (!canViewTrip(trip, loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOT_AUTHORIZED_VIEW);
                return REDIRECT_TRIPS;
            }
            
            // Show all days as columns instead of pagination
            List<com.siw.it.siw_trip.Model.TripDay> allDays = trip.getTripDays();
            int totalDays = allDays.size();
            
            // Add attributes to model
            model.addAttribute("trip", trip);
            model.addAttribute(TOTAL_DAYS, totalDays);
            model.addAttribute(ALL_DAYS, allDays);
            
            return "itinerary/itinerary";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, TRIP_NOT_FOUND);
            return REDIRECT_TRIPS;
        }
    }

    /**
     * Display trip itinerary with day navigation (admin view)
     */
    @GetMapping("/admin/{tripId}")
    public String viewTripItineraryAdmin(@PathVariable Long tripId, 
                                       @RequestParam(defaultValue = "0") int startDay,
                                       Model model, 
                                       HttpSession session, 
                                       RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }
        
        try {
            Trip trip = tripService.findByIdOrThrow(tripId);
            
            // Check if user is authorized to view this trip (admin only)
            if (!canEditTrip(trip, loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOT_AUTHORIZED_VIEW);
                return REDIRECT_TRIPS;
            }
            
            // Show all days as columns instead of pagination
            List<com.siw.it.siw_trip.Model.TripDay> allDays = trip.getTripDays();
            int totalDays = allDays.size();
            
            // Add attributes to model
            model.addAttribute("trip", trip);
            model.addAttribute(TOTAL_DAYS, totalDays);
            model.addAttribute(ALL_DAYS, allDays);
            
            return "itinerary/itineraryAdmin";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, TRIP_NOT_FOUND);
            return REDIRECT_TRIPS;
        }
    }
}

// Add controller for the new route pattern
@Controller
@RequestMapping("/trips")
class TripItineraryViewController {

    private final TripService tripService;
    private final TripDayService tripDayService;
    private final VisitService visitService;
    private final LocationService locationService;
    private final RouteService routeService;
    private final GooglePlacesService googlePlacesService;
    
    // Constants to avoid code duplication
    private static final String LOGGED_IN_USER = "loggedInUser";
    private static final String REDIRECT_LOGIN = "redirect:/login";
    private static final String REDIRECT_TRIPS = "redirect:/trips";
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String SUCCESS_MESSAGE = "successMessage";
    private static final String TRIP_NOT_FOUND = "Trip not found";
    private static final String TRIPDAY_NOT_FOUND = "Trip day not found";
    private static final String VISIT_NOT_FOUND = "Visit not found";
    private static final String NOT_AUTHORIZED_VIEW = "You are not authorized to view this trip";
    private static final String NOT_AUTHORIZED_EDIT = "You are not authorized to edit this trip";
    private static final String TOTAL_DAYS = "totalDays";
    private static final String ALL_DAYS = "allDays";

    public TripItineraryViewController(TripService tripService, TripDayService tripDayService, 
                                      VisitService visitService, LocationService locationService, RouteService routeService, GooglePlacesService googlePlacesService) {
        this.tripService = tripService;
        this.tripDayService = tripDayService;
        this.visitService = visitService;
        this.locationService = locationService;
        this.routeService = routeService;
        this.googlePlacesService = googlePlacesService;
    }

    /**
     * Helper method to check if user can view a trip (participant or admin)
     */
    private boolean canViewTrip(Trip trip, User user) {
        return trip.getParticipants().contains(user) || trip.getAdmins().contains(user);
    }

    /**
     * Helper method to check if user can edit a trip (admin only)
     */
    private boolean canEditTrip(Trip trip, User user) {
        return trip.getAdmins().contains(user);
    }

    /**
     * Display trip itinerary - alternative route pattern
     */
    @GetMapping("/{tripId}/itinerary")
    public String viewTripItinerary(@PathVariable Long tripId, 
                                  @RequestParam(defaultValue = "0") int startDay,
                                  Model model, 
                                  HttpSession session, 
                                  RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(tripId);
            
            // Check if user is authorized to view this trip (participant or admin)
            if (!canViewTrip(trip, loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOT_AUTHORIZED_VIEW);
                return REDIRECT_TRIPS;
            }
            
            // Show all days as columns instead of pagination
            List<com.siw.it.siw_trip.Model.TripDay> allDays = trip.getTripDays();
            int totalDays = allDays.size();
            
            // Add attributes to model
            model.addAttribute("trip", trip);
            model.addAttribute(TOTAL_DAYS, totalDays);
            model.addAttribute(ALL_DAYS, allDays);
            
            return "itinerary/itinerary";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, TRIP_NOT_FOUND);
            return REDIRECT_TRIPS;
        }
    }

    /**
     * Show create location form (Step 1 of adding a visit)
     */
    @GetMapping("/{tripId}/days/{tripDayId}/locations/new")
    public String showCreateLocationForm(@PathVariable Long tripId, 
                                       @PathVariable Long tripDayId,
                                       Model model, 
                                       HttpSession session, 
                                       RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(tripId);
            
            // Check if user is authorized to edit this trip (admin only)
            if (!canEditTrip(trip, loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOT_AUTHORIZED_EDIT);
                return "redirect:/trips/" + tripId + "/itinerary";
            }
            
            TripDay tripDay = tripDayService.findById(tripDayId)
                    .orElseThrow(() -> new IllegalArgumentException(TRIPDAY_NOT_FOUND));
            
            // Verify trip day belongs to this trip
            if (!tripDay.getTrip().getId().equals(tripId)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Trip day does not belong to this trip");
                return "redirect:/trips/" + tripId + "/itinerary";
            }
            
            // Get existing locations from all visits in this trip
            List<Location> existingLocations = trip.getTripDays().stream()
                    .flatMap(day -> day.getVisits().stream())
                    .map(Visit::getLocation)
                    .distinct()
                    .toList();
            
            // Create new location object for form binding
            Location location = new Location();
            
            model.addAttribute("trip", trip);
            model.addAttribute("tripDay", tripDay);
            model.addAttribute("location", location);
            model.addAttribute("existingLocations", existingLocations);
            model.addAttribute("googlePlacesService", googlePlacesService);
            
            return "locations/create";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
            return "redirect:/trips/" + tripId + "/itinerary";
        }
    }

    /**
     * Handle location creation (Step 1 of adding a visit)
     */
    @PostMapping("/{tripId}/days/{tripDayId}/locations")
    public String createLocation(@PathVariable Long tripId, 
                               @PathVariable Long tripDayId,
                               @Valid @ModelAttribute("location") Location location,
                               BindingResult bindingResult,
                               Model model, 
                               HttpSession session, 
                               RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(tripId);
            
            // Check if user is authorized to edit this trip (admin only)
            if (!canEditTrip(trip, loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOT_AUTHORIZED_EDIT);
                return "redirect:/trips/" + tripId + "/itinerary";
            }
            
            TripDay tripDay = tripDayService.findById(tripDayId)
                    .orElseThrow(() -> new IllegalArgumentException(TRIPDAY_NOT_FOUND));
            
            // Verify trip day belongs to this trip
            if (!tripDay.getTrip().getId().equals(tripId)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Trip day does not belong to this trip");
                return "redirect:/trips/" + tripId + "/itinerary";
            }
            
            // Validate coordinates
            if (location.getLatitude() != null && (location.getLatitude() < -90 || location.getLatitude() > 90)) {
                bindingResult.rejectValue("latitude", "error.latitude", "Latitude must be between -90 and 90");
            }
            if (location.getLongitude() != null && (location.getLongitude() < -180 || location.getLongitude() > 180)) {
                bindingResult.rejectValue("longitude", "error.longitude", "Longitude must be between -180 and 180");
            }
            
            if (bindingResult.hasErrors()) {
                // Get existing locations for the form
                List<Location> existingLocations = trip.getTripDays().stream()
                        .flatMap(day -> day.getVisits().stream())
                        .map(Visit::getLocation)
                        .distinct()
                        .toList();
                
                model.addAttribute("trip", trip);
                model.addAttribute("tripDay", tripDay);
                model.addAttribute("existingLocations", existingLocations);
                return "locations/create";
            }
            
            // Save location
            location = locationService.save(location);
            
            // Redirect to visit creation with the location ID
            return "redirect:/trips/" + tripId + "/days/" + tripDayId + "/visits/new?locationId=" + location.getId();
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
            return "redirect:/trips/" + tripId + "/itinerary";
        }
    }

    /**
     * Search for nearby locations based on trip day destination (for visit creation)
     */
    @PostMapping("/{tripId}/days/{tripDayId}/locations/search-nearby")
    public String searchNearbyLocations(@PathVariable Long tripId, 
                                       @PathVariable Long tripDayId,
                                       @RequestParam(value = "query", required = false) String query,
                                       @RequestParam(value = "radius", defaultValue = "5000") Double radius,
                                       Model model, 
                                       HttpSession session, 
                                       RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(tripId);
            
            // Check if user is authorized to edit this trip (admin only)
            if (!canEditTrip(trip, loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOT_AUTHORIZED_EDIT);
                return "redirect:/trips/" + tripId + "/itinerary";
            }
            
            TripDay tripDay = tripDayService.findById(tripDayId)
                    .orElseThrow(() -> new IllegalArgumentException(TRIPDAY_NOT_FOUND));
            
            // Verify trip day belongs to this trip
            if (!tripDay.getTrip().getId().equals(tripId)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Trip day does not belong to this trip");
                return "redirect:/trips/" + tripId + "/itinerary";
            }

            // Check if the trip day has a destination set
            Location destination = tripDay.getDestination();
            if (destination == null || destination.getLatitude() == null || destination.getLongitude() == null) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Please set a destination for this day first to search for nearby locations");
                return "redirect:/trips/" + tripId + "/days/" + tripDayId + "/locations/new";
            }

            // Search for nearby places using destination coordinates
            List<GooglePlaceDto> nearbyPlaces;
            if (query != null && !query.trim().isEmpty()) {
                // Search with query and location bias
                PlaceSearchRequest searchRequest = new PlaceSearchRequest(query.trim());
                nearbyPlaces = googlePlacesService.searchPlaces(searchRequest);
            } else {
                // Search for nearby places around destination
                nearbyPlaces = googlePlacesService.searchNearbyPlaces(
                    destination.getLatitude(), 
                    destination.getLongitude(), 
                    radius.intValue(), 
                    null // No specific type filter - get all types of places
                );
            }

            // Get existing locations for the form
            List<Location> existingLocations = trip.getTripDays().stream()
                    .flatMap(day -> day.getVisits().stream())
                    .map(Visit::getLocation)
                    .distinct()
                    .toList();
            
            // Create new location object for form binding
            Location location = new Location();
            
            model.addAttribute("trip", trip);
            model.addAttribute("tripDay", tripDay);
            model.addAttribute("location", location);
            model.addAttribute("existingLocations", existingLocations);
            model.addAttribute("destination", destination);
            model.addAttribute("nearbyPlaces", nearbyPlaces);
            model.addAttribute("searchQuery", query);
            model.addAttribute("searchRadius", radius);
            model.addAttribute("googlePlacesService", googlePlacesService);
            
            return "locations/create";
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
            return "redirect:/trips/" + tripId + "/itinerary";
        }
    }

    /**
     * Create location from Google Places search result (nearby search for visit creation)
     */
    @PostMapping("/{tripId}/days/{tripDayId}/locations/create-from-place")
    public String createLocationFromPlace(@PathVariable Long tripId, 
                                         @PathVariable Long tripDayId,
                                         @RequestParam("placeId") String placeId,
                                         @RequestParam("name") String name,
                                         @RequestParam("address") String address,
                                         @RequestParam(value = "latitude", required = false) Double latitude,
                                         @RequestParam(value = "longitude", required = false) Double longitude,
                                         HttpSession session, 
                                         RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(tripId);
            
            // Check if user is authorized to edit this trip (admin only)
            if (!canEditTrip(trip, loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOT_AUTHORIZED_EDIT);
                return "redirect:/trips/" + tripId + "/itinerary";
            }
            
            TripDay tripDay = tripDayService.findById(tripDayId)
                    .orElseThrow(() -> new IllegalArgumentException(TRIPDAY_NOT_FOUND));
            
            // Verify trip day belongs to this trip
            if (!tripDay.getTrip().getId().equals(tripId)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Trip day does not belong to this trip");
                return "redirect:/trips/" + tripId + "/itinerary";
            }

            // Create location from Google Places data
            Location location = new Location();
            location.setName(name);
            location.setAddress(address);
            location.setLatitude(latitude);
            location.setLongitude(longitude);
            
            // Extract city and country from address if possible
            String[] addressParts = address.split(",");
            if (addressParts.length >= 2) {
                location.setCity(addressParts[addressParts.length - 2].trim());
                location.setCountry(addressParts[addressParts.length - 1].trim());
            }
            
            // Save location and redirect to visit creation
            location = locationService.save(location);
            
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Location '" + name + "' created successfully from nearby search!");
            return "redirect:/trips/" + tripId + "/days/" + tripDayId + "/visits/new?locationId=" + location.getId();
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
            return "redirect:/trips/" + tripId + "/itinerary";
        }
    }

    /**
     * Show create visit form (Step 2 of adding a visit) - with location parameter required
     */
    @GetMapping("/{tripId}/days/{tripDayId}/visits/new")
    public String showCreateVisitFormWithLocation(@PathVariable Long tripId, 
                                                 @PathVariable Long tripDayId,
                                                 @RequestParam(value = "locationId", required = false) Long locationId,
                                                 Model model, 
                                                 HttpSession session, 
                                                 RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        // If no locationId is provided, redirect to location creation first
        if (locationId == null) {
            return "redirect:/trips/" + tripId + "/days/" + tripDayId + "/locations/new";
        }

        try {
            Trip trip = tripService.findByIdOrThrow(tripId);
            
            // Check if user is authorized to edit this trip (admin only)
            if (!canEditTrip(trip, loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOT_AUTHORIZED_EDIT);
                return "redirect:/trips/" + tripId + "/itinerary";
            }
            
            TripDay tripDay = tripDayService.findById(tripDayId)
                    .orElseThrow(() -> new IllegalArgumentException(TRIPDAY_NOT_FOUND));
            
            // Verify trip day belongs to this trip
            if (!tripDay.getTrip().getId().equals(tripId)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Trip day does not belong to this trip");
                return "redirect:/trips/" + tripId + "/itinerary";
            }
            
            // Get the location
            Location location = locationService.findById(locationId)
                    .orElseThrow(() -> new IllegalArgumentException("Location not found"));
            
            // Create new visit object for form binding
            Visit visit = new Visit();
            
            model.addAttribute("trip", trip);
            model.addAttribute("tripDay", tripDay);
            model.addAttribute("visit", visit);
            model.addAttribute("location", location);
            
            return "visits/create_with_location";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
            return "redirect:/trips/" + tripId + "/itinerary";
        }
    }

    /**
     * Handle visit creation - Updated for new two-step process
     */
    @PostMapping("/{tripId}/days/{tripDayId}/visits")
    public String createVisit(@PathVariable Long tripId, 
                            @PathVariable Long tripDayId,
                            @Valid @ModelAttribute("visit") Visit visit,
                            @RequestParam("locationId") Long locationId,
                            BindingResult bindingResult,
                            Model model, 
                            HttpSession session, 
                            RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(tripId);
            
            // Check if user is authorized to edit this trip (admin only)
            if (!canEditTrip(trip, loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOT_AUTHORIZED_EDIT);
                return "redirect:/trips/" + tripId + "/itinerary";
            }
            
            TripDay tripDay = tripDayService.findById(tripDayId)
                    .orElseThrow(() -> new IllegalArgumentException(TRIPDAY_NOT_FOUND));
            
            // Verify trip day belongs to this trip
            if (!tripDay.getTrip().getId().equals(tripId)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Trip day does not belong to this trip");
                return "redirect:/trips/" + tripId + "/itinerary";
            }
            
            // Get the location
            Location location = locationService.findById(locationId)
                    .orElseThrow(() -> new IllegalArgumentException("Location not found"));
            
            if (bindingResult.hasErrors()) {
                // Return to form with validation errors
                model.addAttribute("trip", trip);
                model.addAttribute("tripDay", tripDay);
                model.addAttribute("location", location);
                return "visits/create_with_location";
            }
            
            // Set trip day and location
            visit.setTripDay(tripDay);
            visit.setLocation(location);
            
            // Use TripDayService to add visit - this will also create routes
            tripDayService.addVisit(tripDay.getId(), visit);
            
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Visit created successfully");
            return "redirect:/trips/" + tripId + "/itinerary";
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
            return "redirect:/trips/" + tripId + "/itinerary";
        }
    }

    /**
     * Show edit visit form
     */
    @GetMapping("/{tripId}/days/{tripDayId}/visits/{visitId}/edit")
    public String showEditVisitForm(@PathVariable Long tripId, 
                                  @PathVariable Long tripDayId,
                                  @PathVariable Long visitId,
                                  Model model, 
                                  HttpSession session, 
                                  RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(tripId);
            
            // Check if user is authorized to edit this trip (admin only)
            if (!canEditTrip(trip, loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOT_AUTHORIZED_EDIT);
                return "redirect:/trips/" + tripId + "/itinerary";
            }
            
            TripDay tripDay = tripDayService.findById(tripDayId)
                    .orElseThrow(() -> new IllegalArgumentException(TRIPDAY_NOT_FOUND));
            
            Visit visit = visitService.findById(visitId)
                    .orElseThrow(() -> new IllegalArgumentException(VISIT_NOT_FOUND));
            
            // Verify visit belongs to this trip day
            if (!visit.getTripDay().getId().equals(tripDayId)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Visit does not belong to this trip day");
                return "redirect:/trips/" + tripId + "/itinerary";
            }
            
            model.addAttribute("trip", trip);
            model.addAttribute("tripDay", tripDay);
            model.addAttribute("visit", visit);
            
            return "visits/edit";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
            return "redirect:/trips/" + tripId + "/itinerary";
        }
    }

    /**
     * Handle visit update
     */
    @PostMapping("/{tripId}/days/{tripDayId}/visits/{visitId}")
    public String updateVisit(@PathVariable Long tripId, 
                            @PathVariable Long tripDayId,
                            @PathVariable Long visitId,
                            @Valid @ModelAttribute("visit") Visit visit,
                            @RequestParam("locationName") String locationName,
                            @RequestParam("locationAddress") String locationAddress,
                            @RequestParam("locationCity") String locationCity,
                            @RequestParam(value = "locationCountry", required = false) String locationCountry,
                            @RequestParam("latitude") Double latitude,
                            @RequestParam("longitude") Double longitude,
                            @RequestParam(value = "locationDescription", required = false) String locationDescription,
                            BindingResult bindingResult,
                            Model model, 
                            HttpSession session, 
                            RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(tripId);
            
            // Check if user is authorized to edit this trip (admin only)
            if (!canEditTrip(trip, loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOT_AUTHORIZED_EDIT);
                return "redirect:/trips/" + tripId + "/itinerary";
            }
            
            TripDay tripDay = tripDayService.findById(tripDayId)
                    .orElseThrow(() -> new IllegalArgumentException(TRIPDAY_NOT_FOUND));
            
            Visit existingVisit = visitService.findById(visitId)
                    .orElseThrow(() -> new IllegalArgumentException(VISIT_NOT_FOUND));
            
            // Verify visit belongs to this trip day    
            if (!existingVisit.getTripDay().getId().equals(tripDayId)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Visit does not belong to this trip day");
                return "redirect:/trips/" + tripId + "/itinerary";
            }
            
            // Validate coordinates
            if (latitude < -90 || latitude > 90) {
                bindingResult.reject("latitude", "Latitude must be between -90 and 90");
            }
            if (longitude < -180 || longitude > 180) {
                bindingResult.reject("longitude", "Longitude must be between -180 and 180");
            }
            
            if (bindingResult.hasErrors()) {
                // Return to form with validation errors
                model.addAttribute("trip", trip);
                model.addAttribute("tripDay", tripDay);
                return "visits/edit";
            }
            
            // Update visit properties
            visit.setId(visitId);
            visit.setTripDay(tripDay);
            
            // Update or create location
            Location location = existingVisit.getLocation();
            if (location == null) {
                location = new Location();
            }
            
            location.setName(locationName.trim());
            location.setAddress(locationAddress.trim());
            location.setCity(locationCity.trim());
            location.setCountry(locationCountry != null ? locationCountry.trim() : null);
            location.setLatitude(latitude);
            location.setLongitude(longitude);
            location.setDescription(locationDescription != null ? locationDescription.trim() : null);
            
            // Save location first
            location = locationService.save(location);
            
            visit.setLocation(location);
            
            // Save updated visit (no need for route recalculation on update unless location changed)
            visitService.save(visit);
            
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Visit updated successfully");
            return "redirect:/trips/" + tripId + "/itinerary";
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
            return "redirect:/trips/" + tripId + "/itinerary";
        }
    }

    /**
     * Handle visit deletion
     */
    @PostMapping("/{tripId}/days/{tripDayId}/visits/{visitId}/delete")
    public String deleteVisit(@PathVariable Long tripId, 
                            @PathVariable Long tripDayId,
                            @PathVariable Long visitId,
                            HttpSession session, 
                            RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(tripId);
            
            // Check if user is authorized to edit this trip (admin only)
            if (!canEditTrip(trip, loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOT_AUTHORIZED_EDIT);
                return "redirect:/trips/" + tripId + "/itinerary";
            }
            
            Visit visit = visitService.findById(visitId)
                    .orElseThrow(() -> new IllegalArgumentException(VISIT_NOT_FOUND));
            
            // Verify visit belongs to this trip day
            if (!visit.getTripDay().getId().equals(tripDayId)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Visit does not belong to this trip day");
                return "redirect:/trips/" + tripId + "/itinerary";
            }
            
            // Use TripDayService to remove visit - this will also handle route cleanup
            tripDayService.removeVisit(tripDayId, visitId);
            
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Visit deleted successfully");
            return "redirect:/trips/" + tripId + "/itinerary";
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
            return "redirect:/trips/" + tripId + "/itinerary";
        }
    }

    // Form-based route endpoints moved from RouteController

    /**
     * Form-based endpoint for creating routes between visits
     */
    @PostMapping("/{tripId}/routes/between-visits/form")
    public String createRouteBetweenVisitsForm(
            @PathVariable Long tripId,
            @RequestParam Long fromVisitId,
            @RequestParam Long toVisitId, 
            @RequestParam Long tripDayId,
            @RequestParam(required = false) TransportMode transportMode,
            RedirectAttributes redirectAttributes) {
        try {
            // Create route object using the service
            Route route = routeService.createRouteBetweenVisits(fromVisitId, toVisitId, tripDayId);
            
            // Set transport mode if provided
            if (transportMode != null) {
                route.setTransportMode(transportMode);
                routeService.calculateRouteDetails(route);
            }
            
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Route calculated successfully");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Failed to create route: " + e.getMessage());
        }
        
        return "redirect:/trips/" + tripId + "/itinerary";
    }

    /**
     * Form-based endpoint for recalculating individual routes  
     */
    @PostMapping("/{tripId}/routes/{routeId}/calculate/form")
    public String calculateRouteForm(
            @PathVariable Long tripId, 
            @PathVariable Long routeId, 
            @RequestParam(required = false) TransportMode transportMode,
            RedirectAttributes redirectAttributes) {
        try {
            Optional<Route> routeOpt = routeService.findById(routeId);
            if (!routeOpt.isPresent()) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Route not found");
                return "redirect:/trips/" + tripId + "/itinerary";
            }

            Route route = routeOpt.get();
            
            // Update transport mode if provided
            if (transportMode != null) {
                route.setTransportMode(transportMode);
                routeService.save(route);
            }
            
            routeService.calculateRouteDetails(route);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Route recalculated successfully");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Failed to recalculate route: " + e.getMessage());
        }
        
        return "redirect:/trips/" + tripId + "/itinerary";
    }

    /**
     * Form-based endpoint for recalculating all routes for a trip day
     */
    @PostMapping("/{tripId}/days/{tripDayId}/routes/recalculate/form")
    public String recalculateRoutesForTripDayForm(@PathVariable Long tripId, @PathVariable Long tripDayId, RedirectAttributes redirectAttributes) {
        try {
            routeService.recalculateRoutesForTripDay(tripDayId);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "All routes recalculated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Failed to recalculate routes: " + e.getMessage());
        }
        
        return "redirect:/trips/" + tripId + "/itinerary";
    }

    // ==================== DESTINATION MANAGEMENT ====================

    /**
     * Show destination selection form
     */
    @GetMapping("/{tripId}/days/{tripDayId}/destination/select")
    public String showSelectDestinationForm(@PathVariable Long tripId, 
                                          @PathVariable Long tripDayId,
                                          Model model, 
                                          HttpSession session, 
                                          RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(tripId);
            
            // Check if user is authorized to edit this trip (admin only)
            if (!canEditTrip(trip, loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOT_AUTHORIZED_EDIT);
                return "redirect:/trips/" + tripId + "/itinerary";
            }
            
            TripDay tripDay = tripDayService.findById(tripDayId)
                    .orElseThrow(() -> new IllegalArgumentException(TRIPDAY_NOT_FOUND));
            
            // Verify trip day belongs to this trip
            if (!tripDay.getTrip().getId().equals(tripId)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Trip day does not belong to this trip");
                return "redirect:/trips/" + tripId + "/itinerary";
            }

            // Get existing locations from the trip for quick selection
            List<Location> existingLocations = trip.getTripDays().stream()
                    .flatMap(day -> day.getVisits().stream())
                    .map(Visit::getLocation)
                    .distinct()
                    .toList();
            
            // Create new location object for form binding
            Location location = new Location();
            
            model.addAttribute("trip", trip);
            model.addAttribute("tripDay", tripDay);
            model.addAttribute("location", location);
            model.addAttribute("existingLocations", existingLocations);
            model.addAttribute("googlePlacesService", googlePlacesService);
            
            return "destinations/select";
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
            return "redirect:/trips/" + tripId + "/itinerary";
        }
    }

    /**
     * Handle destination search using Google Places API
     */
    @PostMapping("/{tripId}/days/{tripDayId}/destination/search")
    public String searchDestinations(@PathVariable Long tripId, 
                                   @PathVariable Long tripDayId,
                                   @RequestParam("q") String searchQuery,
                                   Model model, 
                                   HttpSession session, 
                                   RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(tripId);
            
            // Check if user is authorized to edit this trip (admin only)
            if (!canEditTrip(trip, loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOT_AUTHORIZED_EDIT);
                return "redirect:/trips/" + tripId + "/itinerary";
            }
            
            TripDay tripDay = tripDayService.findById(tripDayId)
                    .orElseThrow(() -> new IllegalArgumentException(TRIPDAY_NOT_FOUND));
            
            // Verify trip day belongs to this trip
            if (!tripDay.getTrip().getId().equals(tripId)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Trip day does not belong to this trip");
                return "redirect:/trips/" + tripId + "/itinerary";
            }

            // Perform search using Google Places API
            PlaceSearchRequest searchRequest = new PlaceSearchRequest(searchQuery);
            List<GooglePlaceDto> searchResults = 
                    googlePlacesService.searchPlaces(searchRequest);
            
            // Get existing locations for the form
            List<Location> existingLocations = trip.getTripDays().stream()
                    .flatMap(day -> day.getVisits().stream())
                    .map(Visit::getLocation)
                    .distinct()
                    .toList();
            
            // Create new location object for form binding
            Location location = new Location();
            
            model.addAttribute("trip", trip);
            model.addAttribute("tripDay", tripDay);
            model.addAttribute("location", location);
            model.addAttribute("existingLocations", existingLocations);
            model.addAttribute("searchResults", searchResults);
            model.addAttribute("searchQuery", searchQuery);
            model.addAttribute("googlePlacesService", googlePlacesService);
            
            return "destinations/select";
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
            return "redirect:/trips/" + tripId + "/itinerary";
        }
    }

    /**
     * Set destination from existing location
     */
    @PostMapping("/{tripId}/days/{tripDayId}/destination/set")
    public String setDestinationFromExisting(@PathVariable Long tripId, 
                                           @PathVariable Long tripDayId,
                                           @RequestParam("locationId") Long locationId,
                                           HttpSession session, 
                                           RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(tripId);
            
            // Check if user is authorized to edit this trip (admin only)
            if (!canEditTrip(trip, loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOT_AUTHORIZED_EDIT);
                return "redirect:/trips/" + tripId + "/itinerary";
            }
            
            TripDay tripDay = tripDayService.findById(tripDayId)
                    .orElseThrow(() -> new IllegalArgumentException(TRIPDAY_NOT_FOUND));
            
            // Verify trip day belongs to this trip
            if (!tripDay.getTrip().getId().equals(tripId)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Trip day does not belong to this trip");
                return "redirect:/trips/" + tripId + "/itinerary";
            }

            // Set the destination
            tripDayService.updateDestinationLocation(tripDayId, locationId);
            
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Destination set successfully!");
            return "redirect:/trips/" + tripId + "/itinerary";
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
            return "redirect:/trips/" + tripId + "/itinerary";
        }
    }

    /**
     * Create destination from Google Places search result
     */
    @PostMapping("/{tripId}/days/{tripDayId}/destination/create-from-place")
    public String createDestinationFromPlace(@PathVariable Long tripId, 
                                           @PathVariable Long tripDayId,
                                           @RequestParam("placeId") String placeId,
                                           @RequestParam("name") String name,
                                           @RequestParam("address") String address,
                                           @RequestParam(value = "latitude", required = false) Double latitude,
                                           @RequestParam(value = "longitude", required = false) Double longitude,
                                           HttpSession session, 
                                           RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(tripId);
            
            // Check if user is authorized to edit this trip (admin only)
            if (!canEditTrip(trip, loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOT_AUTHORIZED_EDIT);
                return "redirect:/trips/" + tripId + "/itinerary";
            }
            
            TripDay tripDay = tripDayService.findById(tripDayId)
                    .orElseThrow(() -> new IllegalArgumentException(TRIPDAY_NOT_FOUND));
            
            // Verify trip day belongs to this trip
            if (!tripDay.getTrip().getId().equals(tripId)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Trip day does not belong to this trip");
                return "redirect:/trips/" + tripId + "/itinerary";
            }

            // Create location from Google Places data
            Location location = new Location();
            location.setName(name);
            location.setAddress(address);
            location.setLatitude(latitude);
            location.setLongitude(longitude);
            
            // Extract city and country from address if possible
            String[] addressParts = address.split(",");
            if (addressParts.length >= 2) {
                location.setCity(addressParts[addressParts.length - 2].trim());
                location.setCountry(addressParts[addressParts.length - 1].trim());
            }
            
            // Save location and set as destination
            location = locationService.save(location);
            tripDayService.updateDestinationLocation(tripDayId, location.getId());
            
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Destination '" + name + "' set successfully!");
            return "redirect:/trips/" + tripId + "/itinerary";
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
            return "redirect:/trips/" + tripId + "/itinerary";
        }
    }

    /**
     * Create custom destination manually
     */
    @PostMapping("/{tripId}/days/{tripDayId}/destination/create")
    public String createDestination(@PathVariable Long tripId, 
                                  @PathVariable Long tripDayId,
                                  @Valid @ModelAttribute("location") Location location,
                                  BindingResult bindingResult,
                                  Model model, 
                                  HttpSession session, 
                                  RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(tripId);
            
            // Check if user is authorized to edit this trip (admin only)
            if (!canEditTrip(trip, loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOT_AUTHORIZED_EDIT);
                return "redirect:/trips/" + tripId + "/itinerary";
            }
            
            TripDay tripDay = tripDayService.findById(tripDayId)
                    .orElseThrow(() -> new IllegalArgumentException(TRIPDAY_NOT_FOUND));
            
            // Verify trip day belongs to this trip
            if (!tripDay.getTrip().getId().equals(tripId)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Trip day does not belong to this trip");
                return "redirect:/trips/" + tripId + "/itinerary";
            }
            
            // Validate coordinates
            if (location.getLatitude() != null && (location.getLatitude() < -90 || location.getLatitude() > 90)) {
                bindingResult.rejectValue("latitude", "error.latitude", "Latitude must be between -90 and 90");
            }
            if (location.getLongitude() != null && (location.getLongitude() < -180 || location.getLongitude() > 180)) {
                bindingResult.rejectValue("longitude", "error.longitude", "Longitude must be between -180 and 180");
            }
            
            if (bindingResult.hasErrors()) {
                // Get existing locations for the form
                List<Location> existingLocations = trip.getTripDays().stream()
                        .flatMap(day -> day.getVisits().stream())
                        .map(Visit::getLocation)
                        .distinct()
                        .toList();
                
                model.addAttribute("trip", trip);
                model.addAttribute("tripDay", tripDay);
                model.addAttribute("existingLocations", existingLocations);
                model.addAttribute("googlePlacesService", googlePlacesService);
                return "destinations/select";
            }
            
            // Save location and set as destination
            location = locationService.save(location);
            tripDayService.updateDestinationLocation(tripDayId, location.getId());
            
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Destination '" + location.getName() + "' set successfully!");
            return "redirect:/trips/" + tripId + "/itinerary";
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
            return "redirect:/trips/" + tripId + "/itinerary";
        }
    }

    /**
     * Remove destination from trip day
     */
    @PostMapping("/{tripId}/days/{tripDayId}/destination/remove")
    public String removeDestination(@PathVariable Long tripId, 
                                  @PathVariable Long tripDayId,
                                  HttpSession session, 
                                  RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(tripId);
            
            // Check if user is authorized to edit this trip (admin only)
            if (!canEditTrip(trip, loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOT_AUTHORIZED_EDIT);
                return "redirect:/trips/" + tripId + "/itinerary";
            }
            
            TripDay tripDay = tripDayService.findById(tripDayId)
                    .orElseThrow(() -> new IllegalArgumentException(TRIPDAY_NOT_FOUND));
            
            // Verify trip day belongs to this trip
            if (!tripDay.getTrip().getId().equals(tripId)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Trip day does not belong to this trip");
                return "redirect:/trips/" + tripId + "/itinerary";
            }

            // Remove the destination
            tripDayService.removeDestination(tripDayId);
            
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Destination removed successfully!");
            return "redirect:/trips/" + tripId + "/itinerary";
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
            return "redirect:/trips/" + tripId + "/itinerary";
        }
    }
}
