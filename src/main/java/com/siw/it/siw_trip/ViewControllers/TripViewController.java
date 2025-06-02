package com.siw.it.siw_trip.ViewControllers;

import com.siw.it.siw_trip.Model.Trip;
import com.siw.it.siw_trip.Model.TripStatus;
import com.siw.it.siw_trip.Model.User;
import com.siw.it.siw_trip.Service.TripService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;

import jakarta.servlet.http.HttpSession;
import java.beans.PropertyEditorSupport;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/trips")
public class TripViewController {

    private final TripService tripService;
    
    // Constants to avoid code duplication
    private static final String LOGGED_IN_USER = "loggedInUser";
    private static final String REDIRECT_LOGIN = "redirect:/login";
    private static final String REDIRECT_TRIPS = "redirect:/trips";
    private static final String REDIRECT_TRIPS_BASE = "redirect:/trips/";
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String SUCCESS_MESSAGE = "successMessage";
    private static final String TRIP_NOT_FOUND = "Trip not found";
    private static final String NOT_AUTHORIZED_VIEW = "You are not authorized to view this trip";
    private static final String NOT_AUTHORIZED_EDIT = "You are not authorized to edit this trip";

    public TripViewController(TripService tripService) {
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
     * Configure property editors for form binding
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
        binder.registerCustomEditor(TripStatus.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                if (text == null || text.trim().isEmpty()) {
                    setValue(null);
                } else {
                    try {
                        setValue(TripStatus.valueOf(text.trim().toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        setValue(TripStatus.PLANNING); // Default fallback
                    }
                }
            }
        });
    }

    /**
     * Display trips accessible to the current user
     */
    @GetMapping
    public String listTrips(Model model, HttpSession session) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }
        
        List<Trip> trips = tripService.findTripsForUser(loggedInUser);
        
        // Sort trips by start date
        trips.sort((t1, t2) -> t1.getStartDateTime().compareTo(t2.getStartDateTime()));
        
        model.addAttribute("trips", trips);
        return "trips/list";
    }

    /**
     * Show form for creating a new trip
     */
    @GetMapping("/new")
    public String showCreateTripForm(Model model, HttpSession session) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }
        
        model.addAttribute("trip", new Trip());
        model.addAttribute("tripStatuses", TripStatus.values());
        return "trips/create";
    }

    /**
     * Handle trip creation form submission
     */
    @PostMapping("/create")
    public String createTrip(@ModelAttribute Trip trip, 
                           HttpSession session, 
                           RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            // Set default values
            trip.setCreatedAt(LocalDateTime.now());
            trip.setUpdatedAt(LocalDateTime.now());
            if (trip.getStatus() == null) {
                trip.setStatus(TripStatus.PLANNING);
            }
            
            // Add the creator as both admin and participant
            trip.getAdmins().add(loggedInUser);
            trip.getParticipants().add(loggedInUser);

            // Save the trip
            Trip savedTrip = tripService.save(trip);
            
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, 
                "Trip '" + savedTrip.getName() + "' created successfully! You can now invite participants.");
            
            // Redirect to the invite page
            return REDIRECT_TRIPS_BASE + savedTrip.getId() + "/invite";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, 
                "Error creating trip: " + e.getMessage());
            return "redirect:/trips/new";
        }
    }

    /**
     * Display trip details
     */
    @GetMapping("/{id}")
    public String viewTrip(@PathVariable Long id, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }
        
        try {
            Trip trip = tripService.findByIdOrThrow(id);
            
            // Check if user is authorized to view this trip (participant or admin)
            if (!canViewTrip(trip, loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOT_AUTHORIZED_VIEW);
                return REDIRECT_TRIPS;
            }
            
            model.addAttribute("trip", trip);
            return "trips/view";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, TRIP_NOT_FOUND);
            return REDIRECT_TRIPS;
        }
    }

    /**
     * Show form for editing a trip
     */
    @GetMapping("/{id}/edit")
    public String showEditTripForm(@PathVariable Long id, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(id);
            
            // Check if user is authorized to edit (admin only)
            if (!canEditTrip(trip, loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOT_AUTHORIZED_EDIT);
                return REDIRECT_TRIPS_BASE + id;
            }
            
            model.addAttribute("trip", trip);
            model.addAttribute("tripStatuses", TripStatus.values());
            return "trips/edit";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, TRIP_NOT_FOUND);
            return REDIRECT_TRIPS;
        }
    }

    /**
     * Handle trip update form submission
     */
    @PostMapping("/{id}/update")
    public String updateTrip(@PathVariable Long id, @ModelAttribute Trip trip, 
                           HttpSession session, RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip existingTrip = tripService.findByIdOrThrow(id);
            
            // Check if user is authorized to edit
            if (!canEditTrip(existingTrip, loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOT_AUTHORIZED_EDIT);
                return REDIRECT_TRIPS_BASE + id;
            }

            // Update the trip fields
            existingTrip.setName(trip.getName());
            existingTrip.setDescription(trip.getDescription());
            existingTrip.setStartDateTime(trip.getStartDateTime());
            existingTrip.setEndDateTime(trip.getEndDateTime());
            existingTrip.setStatus(trip.getStatus());
            existingTrip.setNotes(trip.getNotes());
            existingTrip.setUpdatedAt(LocalDateTime.now());

            Trip updatedTrip = tripService.save(existingTrip);
            
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, 
                "Trip '" + updatedTrip.getName() + "' updated successfully!");
            return REDIRECT_TRIPS_BASE + id;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, TRIP_NOT_FOUND);
            return REDIRECT_TRIPS;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, 
                "Error updating trip: " + e.getMessage());
            return REDIRECT_TRIPS_BASE + id + "/edit";
        }
    }

    /**
     * Show participant invitation page
     */
    @GetMapping("/{id}/invite")
    public String showInviteForm(@PathVariable Long id, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(id);
            
            // Check if user is authorized to edit this trip (admin only)
            if (!canEditTrip(trip, loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOT_AUTHORIZED_EDIT);
                return REDIRECT_TRIPS_BASE + id;
            }

            model.addAttribute("trip", trip);
            return "trips/invite";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, TRIP_NOT_FOUND);
            return REDIRECT_TRIPS;
        }
    }
}
