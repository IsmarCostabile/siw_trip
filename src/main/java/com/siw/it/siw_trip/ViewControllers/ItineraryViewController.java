package com.siw.it.siw_trip.ViewControllers;

import com.siw.it.siw_trip.Model.Trip;
import com.siw.it.siw_trip.Model.TripDay;
import com.siw.it.siw_trip.Model.Visit;
import com.siw.it.siw_trip.Model.User;
import com.siw.it.siw_trip.Model.Location;
import com.siw.it.siw_trip.Service.TripService;
import com.siw.it.siw_trip.Service.TripDayService;
import com.siw.it.siw_trip.Service.VisitService;
import com.siw.it.siw_trip.Service.LocationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;

@Controller
@RequestMapping("/itinerary")
public class ItineraryViewController {

    private final TripService tripService;
    private final TripDayService tripDayService;
    private final VisitService visitService;
    private final LocationService locationService;
    
    // Constants to avoid code duplication
    private static final String LOGGED_IN_USER = "loggedInUser";
    private static final String REDIRECT_LOGIN = "redirect:/login";
    private static final String REDIRECT_TRIPS = "redirect:/trips";
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String TRIP_NOT_FOUND = "Trip not found";
    private static final String NOT_AUTHORIZED_VIEW = "You are not authorized to view this trip";

    public ItineraryViewController(TripService tripService, TripDayService tripDayService, 
                                  VisitService visitService, LocationService locationService) {
        this.tripService = tripService;
        this.tripDayService = tripDayService;
        this.visitService = visitService;
        this.locationService = locationService;
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
            model.addAttribute("totalDays", totalDays);
            model.addAttribute("allDays", allDays);
            
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
            model.addAttribute("totalDays", totalDays);
            model.addAttribute("allDays", allDays);
            
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

    public TripItineraryViewController(TripService tripService, TripDayService tripDayService, 
                                      VisitService visitService, LocationService locationService) {
        this.tripService = tripService;
        this.tripDayService = tripDayService;
        this.visitService = visitService;
        this.locationService = locationService;
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
            model.addAttribute("totalDays", totalDays);
            model.addAttribute("allDays", allDays);
            
            return "itinerary/itinerary";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, TRIP_NOT_FOUND);
            return REDIRECT_TRIPS;
        }
    }

    /**
     * Show create visit form
     */
    @GetMapping("/{tripId}/days/{tripDayId}/visits/new")
    public String showCreateVisitForm(@PathVariable Long tripId, 
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
            
            // Create new visit object for form binding
            Visit visit = new Visit();
            
            model.addAttribute("trip", trip);
            model.addAttribute("tripDay", tripDay);
            model.addAttribute("visit", visit);
            
            return "visits/create";
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
            
            // Verify trip day belongs to this trip
            if (!tripDay.getTrip().getId().equals(tripId)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Trip day does not belong to this trip");
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
                return "visits/create";
            }
            
            // Create new location with provided coordinates and information
            Location location = new Location();
            location.setName(locationName.trim());
            location.setAddress(locationAddress.trim());
            location.setCity(locationCity.trim());
            location.setCountry(locationCountry != null ? locationCountry.trim() : null);
            location.setLatitude(latitude);
            location.setLongitude(longitude);
            location.setDescription(locationDescription != null ? locationDescription.trim() : null);
            
            // Save location first
            location = locationService.save(location);
            
            // Time fields are automatically populated from the form via @ModelAttribute binding
            // since the create form uses datetime-local inputs with th:field binding
            
            // Set trip day, location
            visit.setTripDay(tripDay);
            visit.setLocation(location);
            
            // Save visit
            visitService.save(visit);
            
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
            
            // Update existing location or create new one
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
            
            // Time fields are automatically populated from the form via @ModelAttribute binding
            // since the edit form now uses datetime-local inputs with th:field binding
            
            // Update visit properties
            visit.setId(visitId);
            visit.setTripDay(tripDay);
            visit.setLocation(location);
            
            // Save updated visit
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
            
            // Delete visit
            visitService.deleteById(visitId);
            
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Visit deleted successfully");
            return "redirect:/trips/" + tripId + "/itinerary";
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
            return "redirect:/trips/" + tripId + "/itinerary";
        }
    }
}
