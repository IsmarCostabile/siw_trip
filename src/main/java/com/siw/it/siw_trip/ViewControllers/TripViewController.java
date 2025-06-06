package com.siw.it.siw_trip.ViewControllers;

import com.siw.it.siw_trip.Model.Trip;
import com.siw.it.siw_trip.Model.TripStatus;
import com.siw.it.siw_trip.Model.User;
import com.siw.it.siw_trip.Service.TripService;
import com.siw.it.siw_trip.Service.UserService;
import com.siw.it.siw_trip.Service.InvitationService;
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
    private final UserService userService;
    private final InvitationService invitationService;
    
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

    public TripViewController(TripService tripService, UserService userService, InvitationService invitationService) {
        this.tripService = tripService;
        this.userService = userService;
        this.invitationService = invitationService;
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
     * Show form for editing a trip (also handles user search)
     */
    @GetMapping("/{id}/edit")
    public String showEditTripForm(@PathVariable Long id, 
                                 @RequestParam(value = "userQuery", required = false) String userQuery,
                                 @RequestParam(value = "search", required = false) String search,
                                 Model model, HttpSession session, RedirectAttributes redirectAttributes) {
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
            
            // Handle user search if query is provided
            if (search != null && userQuery != null && !userQuery.trim().isEmpty()) {
                List<User> searchResults = userService.findByNameContaining(userQuery.trim())
                    .stream()
                    .filter(user -> !trip.getParticipants().contains(user) && 
                                   !trip.getAdmins().contains(user) &&
                                   !invitationService.hasUserBeenInvited(user, trip))
                    .toList();
                model.addAttribute("searchResults", searchResults);
            }
            
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
    public String showInviteForm(@PathVariable Long id, 
                                @RequestParam(value = "userQuery", required = false) String userQuery,
                                @RequestParam(value = "search", required = false) String search,
                                Model model, HttpSession session, RedirectAttributes redirectAttributes) {
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
            
            // Handle user search if query is provided
            if (search != null && userQuery != null && !userQuery.trim().isEmpty()) {
                List<User> searchResults = userService.findByNameContaining(userQuery.trim())
                    .stream()
                    .filter(user -> !trip.getParticipants().contains(user) && 
                                   !trip.getAdmins().contains(user) &&
                                   !invitationService.hasUserBeenInvited(user, trip))
                    .toList();
                model.addAttribute("searchResults", searchResults);
            }
            
            return "trips/invite";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, TRIP_NOT_FOUND);
            return REDIRECT_TRIPS;
        }
    }

    /**
     * Send invitation to a user for a trip
     */
    @PostMapping("/{id}/participants/add")
    public String sendInvitation(@PathVariable Long id, 
                               @RequestParam Long userId,
                               @RequestParam(defaultValue = "false") boolean asAdmin,
                               HttpSession session, 
                               RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(id);
            
            // Check if user is authorized to edit this trip (admin only)
            if (!canEditTrip(trip, loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOT_AUTHORIZED_EDIT);
                return REDIRECT_TRIPS_BASE + id + "/edit";
            }

            User userToInvite = userService.findByIdOrThrow(userId);
            
            // Check if user is already a participant or admin
            if (trip.getParticipants().contains(userToInvite) || trip.getAdmins().contains(userToInvite)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "User is already a participant in this trip");
                return REDIRECT_TRIPS_BASE + id + "/edit";
            }

            // Create invitation
            String message = asAdmin ? 
                "You have been invited to join '" + trip.getName() + "' as an admin." :
                "You have been invited to join '" + trip.getName() + "' as a participant.";
            
            invitationService.createInvitation(userToInvite, trip, message, loggedInUser, asAdmin);
            
            String role = asAdmin ? "admin" : "participant";
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, 
                "Invitation sent to '" + userToInvite.getFullName() + "' as " + role + " successfully");

            return REDIRECT_TRIPS_BASE + id + "/edit";
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("already been invited")) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "User has already been invited to this trip");
            } else {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "User or trip not found");
            }
            return REDIRECT_TRIPS_BASE + id + "/edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Error sending invitation: " + e.getMessage());
            return REDIRECT_TRIPS_BASE + id + "/edit";
        }
    }

    /**
     * Remove a participant from a trip or cancel their invitation
     */
    @PostMapping("/{id}/participants/remove")
    public String removeParticipant(@PathVariable Long id, 
                                  @RequestParam Long userId,
                                  HttpSession session, 
                                  RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(id);
            
            // Check if user is authorized to edit this trip (admin only)
            if (!canEditTrip(trip, loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOT_AUTHORIZED_EDIT);
                return REDIRECT_TRIPS_BASE + id + "/edit";
            }

            User userToRemove = userService.findByIdOrThrow(userId);
            
            // Check if user is a participant/admin or has a pending invitation
            boolean isParticipant = trip.getParticipants().contains(userToRemove);
            boolean isAdmin = trip.getAdmins().contains(userToRemove);
            boolean hasInvitation = invitationService.hasUserBeenInvited(userToRemove, trip);
            
            if (!isParticipant && !isAdmin && !hasInvitation) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "User is not associated with this trip");
                return REDIRECT_TRIPS_BASE + id + "/edit";
            }
            
            // Prevent removing yourself if you're the only admin
            if (loggedInUser.equals(userToRemove) && trip.getAdmins().size() == 1 && trip.getAdmins().contains(loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Cannot remove yourself as you are the only admin");
                return REDIRECT_TRIPS_BASE + id + "/edit";
            }
            
            // If user has a pending invitation, cancel it
            if (hasInvitation) {
                invitationService.cancelInvitation(userToRemove, trip, loggedInUser);
                redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, 
                    "Invitation to '" + userToRemove.getFullName() + "' has been cancelled");
            } else {
                // Remove from participants/admins
                if (isAdmin) {
                    tripService.removeAdmin(id, userId);
                }
                if (isParticipant) {
                    tripService.removeParticipant(id, userId);
                }
                redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, 
                    "User '" + userToRemove.getFullName() + "' removed from trip successfully");
            }

            return REDIRECT_TRIPS_BASE + id + "/edit";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "User or trip not found");
            return REDIRECT_TRIPS_BASE + id + "/edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Error removing user: " + e.getMessage());
            return REDIRECT_TRIPS_BASE + id + "/edit";
        }
    }

    /**
     * Toggle admin status for a participant
     */
    @PostMapping("/{id}/participants/{participantId}/toggle-admin")
    public String toggleParticipantAdmin(@PathVariable Long id, 
                                       @PathVariable Long participantId,
                                       HttpSession session, 
                                       RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(id);
            
            // Check if user is authorized to edit this trip (admin only)
            if (!canEditTrip(trip, loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOT_AUTHORIZED_EDIT);
                return REDIRECT_TRIPS_BASE + id + "/edit";
            }

            // Toggle admin status using the existing service logic
            if (trip.getAdmins().stream().anyMatch(admin -> admin.getId().equals(participantId))) {
                // User is currently an admin, remove admin status
                tripService.removeAdmin(id, participantId);
                redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Admin status removed successfully");
            } else {
                // User is not an admin, add admin status
                tripService.addAdmin(id, participantId);
                redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Admin status granted successfully");
            }

            return REDIRECT_TRIPS_BASE + id + "/edit";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "User or trip not found");
            return REDIRECT_TRIPS_BASE + id + "/edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Error toggling admin status: " + e.getMessage());
            return REDIRECT_TRIPS_BASE + id + "/edit";
        }
    }

    /**
     * Delete a trip (admin only)
     */
    @PostMapping("/{id}/delete")
    public String deleteTrip(@PathVariable Long id, 
                           HttpSession session, 
                           RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(id);
            
            // Check if user is authorized to delete this trip (admin only)
            if (!canEditTrip(trip, loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOT_AUTHORIZED_EDIT);
                return REDIRECT_TRIPS_BASE + id;
            }

            // Store trip name for success message
            String tripName = trip.getName();
            
            // Delete the trip using service
            tripService.deleteById(id);
            
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, 
                "Trip '" + tripName + "' has been deleted successfully");

            return REDIRECT_TRIPS;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, TRIP_NOT_FOUND);
            return REDIRECT_TRIPS;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Error deleting trip: " + e.getMessage());
            return REDIRECT_TRIPS_BASE + id;
        }
    }

    /**
     * Leave a trip (participant can leave unless they're the only admin)
     */
    @PostMapping("/{id}/leave")
    public String leaveTrip(@PathVariable Long id, 
                          HttpSession session, 
                          RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(id);
            
            // Check if user is a participant or admin
            if (!canViewTrip(trip, loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOT_AUTHORIZED_VIEW);
                return REDIRECT_TRIPS;
            }

            // Prevent leaving if user is the only admin
            if (trip.getAdmins().size() == 1 && trip.getAdmins().contains(loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, 
                    "Cannot leave trip as you are the only admin. Please assign another admin first or delete the trip.");
                return REDIRECT_TRIPS_BASE + id;
            }

            // Remove user from the trip
            tripService.removeParticipant(id, loggedInUser.getId());
            
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, 
                "You have successfully left the trip '" + trip.getName() + "'");

            return REDIRECT_TRIPS;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, TRIP_NOT_FOUND);
            return REDIRECT_TRIPS;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Error leaving trip: " + e.getMessage());
            return REDIRECT_TRIPS_BASE + id;
        }
    }
}
