package com.siw.it.siw_trip.ViewControllers;

import com.siw.it.siw_trip.Model.Trip;
import com.siw.it.siw_trip.Model.TripDay;
import com.siw.it.siw_trip.Model.Visit;
import com.siw.it.siw_trip.Model.User;
import com.siw.it.siw_trip.Model.Location;
import com.siw.it.siw_trip.Model.Route;
import com.siw.it.siw_trip.Model.TransportMode;
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

/**
 * Consolidated Itinerary Controller
 * Handles all itinerary-related operations including viewing, locations, visits, destinations, and routes
 * 
 * Consolidated from two separate controllers to provide a clean, organized structure
 * - Uses consistent `/trips/{tripId}` URL pattern for all operations
 * - Eliminates code duplication between controllers
 * - Organizes methods by functional areas
 */
@Controller
@RequestMapping("/trips")
public class ItineraryViewController {

    // ==================== DEPENDENCIES ====================
    
    private final TripService tripService;
    private final TripDayService tripDayService;
    private final VisitService visitService;
    private final LocationService locationService;
    private final RouteService routeService;
    private final GooglePlacesService googlePlacesService;

    // ==================== CONSTANTS ====================
    
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
    
    // URL patterns
    private static final String REDIRECT_TRIPS_PREFIX = "redirect:/trips/";
    private static final String ITINERARY_PATH = "/itinerary";
    private static final String DAYS_PATH = "/days/";
    
    // View names
    private static final String LOCATIONS_CREATE_VIEW = "locations/create";
    private static final String DESTINATIONS_SELECT_VIEW = "destinations/select";
    
    // Model attributes
    private static final String TRIP_DAY_ATTR = "tripDay";
    private static final String LOCATION_ATTR = "location";
    private static final String EXISTING_LOCATIONS_ATTR = "existingLocations";
    private static final String GOOGLE_PLACES_SERVICE_ATTR = "googlePlacesService";
    
    // Validation fields and messages
    private static final String LATITUDE_FIELD = "latitude";
    private static final String LONGITUDE_FIELD = "longitude";
    private static final String LATITUDE_VALIDATION_MSG = "Latitude must be between -90 and 90";
    private static final String LONGITUDE_VALIDATION_MSG = "Longitude must be between -180 and 180";
    
    // Error messages
    private static final String TRIP_DAY_MISMATCH = "Trip day does not belong to this trip";
    private static final String VISIT_MISMATCH = "Visit does not belong to this trip day";

    // ==================== CONSTRUCTOR ====================
    
    public ItineraryViewController(TripService tripService, TripDayService tripDayService,
                                  VisitService visitService, LocationService locationService,
                                  RouteService routeService, GooglePlacesService googlePlacesService) {
        this.tripService = tripService;
        this.tripDayService = tripDayService;
        this.visitService = visitService;
        this.locationService = locationService;
        this.routeService = routeService;
        this.googlePlacesService = googlePlacesService;
    }

    // ==================== AUTHORIZATION HELPERS ====================
    
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
     * Validates user session and retrieves logged-in user
     */
    private User validateUserSession(HttpSession session) {
        return (User) session.getAttribute(LOGGED_IN_USER);
    }

    /**
     * Validates trip access for viewing operations
     */
    private boolean validateViewAccess(Trip trip, User user, RedirectAttributes redirectAttributes) {
        if (!canViewTrip(trip, user)) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOT_AUTHORIZED_VIEW);
            return false;
        }
        return true;
    }

    /**
     * Validates trip access for editing operations
     */
    private boolean validateEditAccess(Trip trip, User user, RedirectAttributes redirectAttributes) {
        if (!canEditTrip(trip, user)) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOT_AUTHORIZED_EDIT);
            return false;
        }
        return true;
    }

    // ==================== ITINERARY VIEWING ====================/
    
    /**
     * Display trip itinerary with all days
     */
    @GetMapping("/{tripId}/itinerary")
    public String viewTripItinerary(@PathVariable Long tripId, 
                                  @RequestParam(defaultValue = "0") int startDay,
                                  Model model, 
                                  HttpSession session, 
                                  RedirectAttributes redirectAttributes) {
        User loggedInUser = validateUserSession(session);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(tripId);
            
            if (!validateViewAccess(trip, loggedInUser, redirectAttributes)) {
                return REDIRECT_TRIPS;
            }

            List<TripDay> allDays = trip.getTripDays();
            int totalDays = allDays.size();

            model.addAttribute("trip", trip);
            model.addAttribute(TOTAL_DAYS, totalDays);
            model.addAttribute(ALL_DAYS, allDays);

            return "itinerary/itinerary";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, TRIP_NOT_FOUND);
            return REDIRECT_TRIPS;
        }
    }

    // ==================== LOCATION MANAGEMENT ====================
    
    /**
     * Show create location form (Step 1 of adding a visit)
     */
    @GetMapping("/{tripId}/days/{tripDayId}/locations/new")
    public String showCreateLocationForm(@PathVariable Long tripId, 
                                       @PathVariable Long tripDayId,
                                       Model model, 
                                       HttpSession session, 
                                       RedirectAttributes redirectAttributes) {
        User loggedInUser = validateUserSession(session);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(tripId);
            
            if (!validateEditAccess(trip, loggedInUser, redirectAttributes)) {
                return "redirect:/trips/" + tripId + "/itinerary";
            }

            TripDay tripDay = tripDayService.findById(tripDayId)
                    .orElseThrow(() -> new IllegalArgumentException(TRIPDAY_NOT_FOUND));

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
        User loggedInUser = validateUserSession(session);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(tripId);
            
            if (!validateEditAccess(trip, loggedInUser, redirectAttributes)) {
                return "redirect:/trips/" + tripId + "/itinerary";
            }

            TripDay tripDay = tripDayService.findById(tripDayId)
                    .orElseThrow(() -> new IllegalArgumentException(TRIPDAY_NOT_FOUND));

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
     * Create location from Google Places search result
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
        User loggedInUser = validateUserSession(session);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(tripId);
            
            if (!validateEditAccess(trip, loggedInUser, redirectAttributes)) {
                return "redirect:/trips/" + tripId + "/itinerary";
            }

            TripDay tripDay = tripDayService.findById(tripDayId)
                    .orElseThrow(() -> new IllegalArgumentException(TRIPDAY_NOT_FOUND));

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

            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Location '" + name + "' created successfully from search!");
            return "redirect:/trips/" + tripId + "/days/" + tripDayId + "/visits/new?locationId=" + location.getId();

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
            return "redirect:/trips/" + tripId + "/itinerary";
        }
    }

    // ==================== VISIT MANAGEMENT ====================
    
    /**
     * Show create visit form (Step 2 of adding a visit)
     */
    @GetMapping("/{tripId}/days/{tripDayId}/visits/new")
    public String showCreateVisitForm(@PathVariable Long tripId, 
                                     @PathVariable Long tripDayId,
                                     @RequestParam(value = "locationId", required = false) Long locationId,
                                     Model model, 
                                     HttpSession session, 
                                     RedirectAttributes redirectAttributes) {
        User loggedInUser = validateUserSession(session);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(tripId);
            
            if (!validateEditAccess(trip, loggedInUser, redirectAttributes)) {
                return "redirect:/trips/" + tripId + "/itinerary";
            }

            TripDay tripDay = tripDayService.findById(tripDayId)
                    .orElseThrow(() -> new IllegalArgumentException(TRIPDAY_NOT_FOUND));

            if (!tripDay.getTrip().getId().equals(tripId)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Trip day does not belong to this trip");
                return "redirect:/trips/" + tripId + "/itinerary";
            }

            // Get the location if provided
            Location location = null;
            if (locationId != null) {
                location = locationService.findById(locationId)
                        .orElseThrow(() -> new IllegalArgumentException("Location not found"));
            }

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
     * Handle visit creation
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
        User loggedInUser = validateUserSession(session);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(tripId);
            
            if (!validateEditAccess(trip, loggedInUser, redirectAttributes)) {
                return "redirect:/trips/" + tripId + "/itinerary";
            }

            TripDay tripDay = tripDayService.findById(tripDayId)
                    .orElseThrow(() -> new IllegalArgumentException(TRIPDAY_NOT_FOUND));

            if (!tripDay.getTrip().getId().equals(tripId)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Trip day does not belong to this trip");
                return "redirect:/trips/" + tripId + "/itinerary";
            }

            Location location = locationService.findById(locationId)
                    .orElseThrow(() -> new IllegalArgumentException("Location not found"));

            if (bindingResult.hasErrors()) {
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
        User loggedInUser = validateUserSession(session);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(tripId);
            
            if (!validateEditAccess(trip, loggedInUser, redirectAttributes)) {
                return "redirect:/trips/" + tripId + "/itinerary";
            }

            TripDay tripDay = tripDayService.findById(tripDayId)
                    .orElseThrow(() -> new IllegalArgumentException(TRIPDAY_NOT_FOUND));

            Visit visit = visitService.findById(visitId)
                    .orElseThrow(() -> new IllegalArgumentException(VISIT_NOT_FOUND));

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
        User loggedInUser = validateUserSession(session);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(tripId);
            
            if (!validateEditAccess(trip, loggedInUser, redirectAttributes)) {
                return "redirect:/trips/" + tripId + "/itinerary";
            }

            TripDay tripDay = tripDayService.findById(tripDayId)
                    .orElseThrow(() -> new IllegalArgumentException(TRIPDAY_NOT_FOUND));

            Visit existingVisit = visitService.findById(visitId)
                    .orElseThrow(() -> new IllegalArgumentException(VISIT_NOT_FOUND));

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
                model.addAttribute("trip", trip);
                model.addAttribute("tripDay", tripDay);
                return "visits/edit";
            }

            // Update visit properties
            visit.setId(visitId);
            visit.setTripDay(tripDay);

            // Update location
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

            location = locationService.save(location);
            visit.setLocation(location);

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
        User loggedInUser = validateUserSession(session);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(tripId);
            
            if (!validateEditAccess(trip, loggedInUser, redirectAttributes)) {
                return "redirect:/trips/" + tripId + "/itinerary";
            }

            Visit visit = visitService.findById(visitId)
                    .orElseThrow(() -> new IllegalArgumentException(VISIT_NOT_FOUND));

            if (!visit.getTripDay().getId().equals(tripDayId)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Visit does not belong to this trip day");
                return "redirect:/trips/" + tripId + "/itinerary";
            }

            tripDayService.removeVisit(tripDayId, visitId);

            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Visit deleted successfully");
            return "redirect:/trips/" + tripId + "/itinerary";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
            return "redirect:/trips/" + tripId + "/itinerary";
        }
    }

    // ==================== ROUTE MANAGEMENT ====================
    
    /**
     * Create route between visits
     */
    @PostMapping("/{tripId}/routes/between-visits/form")
    public String createRouteBetweenVisitsForm(@PathVariable Long tripId,
                                              @RequestParam Long fromVisitId,
                                              @RequestParam Long toVisitId, 
                                              @RequestParam Long tripDayId,
                                              @RequestParam(required = false) TransportMode transportMode,
                                              RedirectAttributes redirectAttributes) {
        try {
            Route route = routeService.createRouteBetweenVisits(fromVisitId, toVisitId, tripDayId);
            
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
     * Recalculate individual route
     */
    @PostMapping("/{tripId}/routes/{routeId}/calculate/form")
    public String calculateRouteForm(@PathVariable Long tripId, 
                                    @PathVariable Long routeId, 
                                    @RequestParam(required = false) TransportMode transportMode,
                                    RedirectAttributes redirectAttributes) {
        try {
            Route route = routeService.findById(routeId)
                    .orElseThrow(() -> new IllegalArgumentException("Route not found"));
            
            if (transportMode != null) {
                route.setTransportMode(transportMode);
            }
            
            routeService.calculateRouteDetails(route);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Route recalculated successfully");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Failed to calculate route: " + e.getMessage());
        }
        
        return "redirect:/trips/" + tripId + "/itinerary";
    }

    /**
     * Recalculate all routes for a trip day
     */
    @PostMapping("/{tripId}/days/{tripDayId}/routes/recalculate/form")
    public String recalculateRoutesForTripDayForm(@PathVariable Long tripId, 
                                                 @PathVariable Long tripDayId, 
                                                 RedirectAttributes redirectAttributes) {
        try {
            routeService.recalculateRoutesForTripDay(tripDayId);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "All routes recalculated successfully");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
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
        User loggedInUser = validateUserSession(session);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(tripId);
            
            if (!validateEditAccess(trip, loggedInUser, redirectAttributes)) {
                return "redirect:/trips/" + tripId + "/itinerary";
            }

            TripDay tripDay = tripDayService.findById(tripDayId)
                    .orElseThrow(() -> new IllegalArgumentException(TRIPDAY_NOT_FOUND));

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
     * Set destination from existing location
     */
    @PostMapping("/{tripId}/days/{tripDayId}/destination/set")
    public String setDestinationFromExisting(@PathVariable Long tripId, 
                                           @PathVariable Long tripDayId,
                                           @RequestParam("locationId") Long locationId,
                                           HttpSession session, 
                                           RedirectAttributes redirectAttributes) {
        User loggedInUser = validateUserSession(session);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(tripId);
            
            if (!validateEditAccess(trip, loggedInUser, redirectAttributes)) {
                return "redirect:/trips/" + tripId + "/itinerary";
            }

            TripDay tripDay = tripDayService.findById(tripDayId)
                    .orElseThrow(() -> new IllegalArgumentException(TRIPDAY_NOT_FOUND));

            if (!tripDay.getTrip().getId().equals(tripId)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Trip day does not belong to this trip");
                return "redirect:/trips/" + tripId + "/itinerary";
            }

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
        User loggedInUser = validateUserSession(session);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(tripId);
            
            if (!validateEditAccess(trip, loggedInUser, redirectAttributes)) {
                return "redirect:/trips/" + tripId + "/itinerary";
            }

            TripDay tripDay = tripDayService.findById(tripDayId)
                    .orElseThrow(() -> new IllegalArgumentException(TRIPDAY_NOT_FOUND));

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

            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Destination '" + location.getName() + "' set successfully!");
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
        User loggedInUser = validateUserSession(session);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(tripId);
            
            if (!validateEditAccess(trip, loggedInUser, redirectAttributes)) {
                return "redirect:/trips/" + tripId + "/itinerary";
            }

            TripDay tripDay = tripDayService.findById(tripDayId)
                    .orElseThrow(() -> new IllegalArgumentException(TRIPDAY_NOT_FOUND));

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
        User loggedInUser = validateUserSession(session);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(tripId);
            
            if (!validateEditAccess(trip, loggedInUser, redirectAttributes)) {
                return "redirect:/trips/" + tripId + "/itinerary";
            }

            TripDay tripDay = tripDayService.findById(tripDayId)
                    .orElseThrow(() -> new IllegalArgumentException(TRIPDAY_NOT_FOUND));

            if (!tripDay.getTrip().getId().equals(tripId)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Trip day does not belong to this trip");
                return "redirect:/trips/" + tripId + "/itinerary";
            }

            tripDayService.removeDestination(tripDayId);

            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Destination removed successfully!");
            return "redirect:/trips/" + tripId + "/itinerary";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
            return "redirect:/trips/" + tripId + "/itinerary";
        }
    }
}
