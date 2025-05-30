package com.siw.it.siw_trip.ViewControllers;

import com.siw.it.siw_trip.Model.Trip;
import com.siw.it.siw_trip.Model.TripStatus;
import com.siw.it.siw_trip.Model.User;
import com.siw.it.siw_trip.Service.TripService;
import com.siw.it.siw_trip.Service.UserService;
import com.siw.it.siw_trip.Service.CredentialsService;
import com.siw.it.siw_trip.Service.InvitationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import jakarta.servlet.http.HttpSession;
import java.beans.PropertyEditorSupport;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/trips")
public class TripViewController {

    private final TripService tripService;
    private final UserService userService;
    private final CredentialsService credentialsService;
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

    public TripViewController(TripService tripService, UserService userService, CredentialsService credentialsService, InvitationService invitationService) {
        this.tripService = tripService;
        this.userService = userService;
        this.credentialsService = credentialsService;
        this.invitationService = invitationService;
    }

    /**
     * Helper method to check if user can view a trip (participant or admin)
     */
    private boolean canViewTrip(Trip trip, User user) {
        System.out.println("Checking if user can view trip: " + trip.getId() + " for user: " + user.getId());
        System.out.println("Logged-in user object: " + user.toString());
        
        // Debug: Print participants
        System.out.println("Trip participants:");
        for (User participant : trip.getParticipants()) {
            System.out.println("  - User ID: " + participant.getId() + ", Name: " + participant.getFirstName() + " " + participant.getLastName());
            System.out.println("  - Participant object: " + participant.toString());
            System.out.println("  - Equals check: " + participant.equals(user));
        }
        
        // Debug: Print admins
        System.out.println("Trip admins:");
        for (User admin : trip.getAdmins()) {
            System.out.println("  - User ID: " + admin.getId() + ", Name: " + admin.getFirstName() + " " + admin.getLastName());
            System.out.println("  - Admin object: " + admin.toString());
            System.out.println("  - Equals check: " + admin.equals(user));
        }
        
        boolean isParticipant = trip.getParticipants().contains(user);
        boolean isAdmin = trip.getAdmins().contains(user);
        
        System.out.println("User is participant: " + isParticipant);
        System.out.println("User is admin: " + isAdmin);
        
        return isParticipant || isAdmin;
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
    public String listTrips(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }
        
        // Get only trips where user is participant, admin, or creator
        List<Trip> trips = tripService.findTripsForUser(loggedInUser);
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
                "Trip '" + savedTrip.getName() + "' created successfully!");
            return REDIRECT_TRIPS_BASE + savedTrip.getId();
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
        System.out.println("DEBUG: viewTrip method called for trip ID: " + id);
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            System.out.println("DEBUG: No logged in user, redirecting to login");
            return REDIRECT_LOGIN;
        }
        
        try {
            System.out.println("DEBUG: Fetching trip with ID: " + id);
            Trip trip = tripService.findByIdOrThrow(id);
            System.out.println("DEBUG: Trip found: " + trip.getName() + ", checking authorization...");
            
            // Check if user is authorized to view this trip (participant, admin, or creator)
            if (!canViewTrip(trip, loggedInUser)) {
                System.out.println("DEBUG: User not authorized to view trip");
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOT_AUTHORIZED_VIEW);
                return REDIRECT_TRIPS;
            }
            
            System.out.println("DEBUG: User authorized, showing trip view");
            model.addAttribute("trip", trip);
            return "trips/view";
        } catch (IllegalArgumentException e) {
            System.out.println("DEBUG: Trip not found: " + e.getMessage());
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, TRIP_NOT_FOUND);
            return REDIRECT_TRIPS;
        }
    }

    /**
     * Show form for editing a trip
     */
    @GetMapping("/{id}/edit")
    public String showEditTripForm(@PathVariable Long id, 
                                 @RequestParam(required = false) String search,
                                 @RequestParam(required = false) String userQuery,
                                 Model model, 
                                 HttpSession session, 
                                 RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(id);
            
            // Check if user is authorized to edit (admin or creator)
            if (!canEditTrip(trip, loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, NOT_AUTHORIZED_EDIT);
                return REDIRECT_TRIPS_BASE + id;
            }
            
            model.addAttribute("trip", trip);
            model.addAttribute("tripStatuses", TripStatus.values());
            
            // Handle user search if requested
            if ("true".equals(search) && userQuery != null && !userQuery.trim().isEmpty()) {
                try {
                    // Search by username first
                    List<User> users = userService.findByNameContaining(userQuery.trim());
                    
                    // Also try to find by exact username
                    try {
                        Optional<User> userByUsernameOpt = credentialsService.findUserByUsername(userQuery.trim());
                        if (userByUsernameOpt.isPresent()) {
                            User userByUsername = userByUsernameOpt.get();
                            if (!users.contains(userByUsername)) {
                                users.add(userByUsername);
                            }
                        }
                    } catch (Exception e) {
                        // User not found by username, that's ok
                    }

                    // Filter out users already in the trip
                    users = users.stream()
                        .filter(user -> !trip.getParticipants().contains(user) && !trip.getAdmins().contains(user))
                        .limit(10)
                        .collect(Collectors.toList());
                    
                    model.addAttribute("searchResults", users);
                } catch (Exception e) {
                    // Search failed, continue without results
                }
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
     * Search for users by username or name for adding as participants
     */
    @GetMapping("/search-users")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> searchUsers(
            @RequestParam String query, 
            @RequestParam(required = false) Long tripId,
            HttpSession session) {
        
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // Search by username first
            List<User> users = userService.findByNameContaining(query);
            
            // Also try to find by exact username
            try {
                Optional<User> userByUsernameOpt = credentialsService.findUserByUsername(query);
                if (userByUsernameOpt.isPresent()) {
                    User userByUsername = userByUsernameOpt.get();
                    if (!users.contains(userByUsername)) {
                        users.add(userByUsername);
                    }
                }
            } catch (Exception e) {
                // User not found by username, that's ok
            }

            // If tripId is provided, filter out users already in the trip
            if (tripId != null) {
                try {
                    Trip trip = tripService.findByIdOrThrow(tripId);
                    users = users.stream()
                        .filter(user -> !trip.getParticipants().contains(user) && !trip.getAdmins().contains(user))
                        .collect(Collectors.toList());
                } catch (Exception e) {
                    // Trip not found, continue with all users
                }
            }

            // Convert to response format (limit to 10 results)
            List<Map<String, Object>> userResults = users.stream()
                .limit(10)
                .map(user -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", user.getId());
                    userMap.put("username", user.getCredentials().getUsername());
                    userMap.put("firstName", user.getFirstName());
                    userMap.put("lastName", user.getLastName());
                    userMap.put("displayName", user.getFirstName() + " " + user.getLastName() + " (@" + user.getCredentials().getUsername() + ")");
                    return userMap;
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(userResults);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Add a participant to a trip
     */
    @PostMapping("/{id}/participants/add")
    public String addParticipant(
            @PathVariable Long id,
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
            
            // Check if user is authorized to manage participants (admin only)
            if (!canEditTrip(trip, loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Not authorized to manage participants");
                return REDIRECT_TRIPS_BASE + id + "/edit";
            }

            User userToInvite = userService.findByIdOrThrow(userId);
            
            // Check if user is already a participant or admin
            if (trip.getParticipants().contains(userToInvite) || trip.getAdmins().contains(userToInvite)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "User is already part of this trip");
                return REDIRECT_TRIPS_BASE + id + "/edit";
            }

            // Check if there's already an invitation (pending or otherwise)
            if (invitationService.hasUserBeenInvited(userToInvite, trip)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "User already has an invitation for this trip");
                return REDIRECT_TRIPS_BASE + id + "/edit";
            }

            // Create invitation message
            String invitationMessage = asAdmin ? 
                "You have been invited to join '" + trip.getName() + "' as an administrator." :
                "You have been invited to join the trip '" + trip.getName() + "'.";

            // Create invitation instead of directly adding user
            invitationService.createInvitation(userToInvite, trip, invitationMessage, loggedInUser, asAdmin);
            
            String message = "Invitation sent successfully" + (asAdmin ? " for admin role" : "");
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, message);
            
            return REDIRECT_TRIPS_BASE + id + "/edit";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Trip or user not found");
            return REDIRECT_TRIPS_BASE + id + "/edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Error sending invitation: " + e.getMessage());
            return REDIRECT_TRIPS_BASE + id + "/edit";
        }
    }

    /**
     * Remove a participant from a trip
     */
    @PostMapping("/{id}/participants/remove")
    public String removeParticipant(
            @PathVariable Long id,
            @RequestParam Long userId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(id);
            
            // Check if user is authorized to manage participants (admin only)
            if (!canEditTrip(trip, loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Not authorized to manage participants");
                return REDIRECT_TRIPS_BASE + id + "/edit";
            }

            User userToRemove = userService.findByIdOrThrow(userId);
            
            // Check if user is in the trip before removing
            boolean wasParticipant = trip.getParticipants().contains(userToRemove);
            boolean wasAdmin = trip.getAdmins().contains(userToRemove);
            
            if (!wasParticipant && !wasAdmin) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "User is not part of this trip");
                return REDIRECT_TRIPS_BASE + id + "/edit";
            }
            
            // Remove user from both participants and admins
            trip.removeParticipant(userToRemove);
            trip.removeAdmin(userToRemove);
            
            trip.setUpdatedAt(LocalDateTime.now());
            tripService.save(trip);
            
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Participant removed successfully");
            
            return REDIRECT_TRIPS_BASE + id + "/edit";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Trip or user not found");
            return REDIRECT_TRIPS_BASE + id + "/edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Error removing participant: " + e.getMessage());
            return REDIRECT_TRIPS_BASE + id + "/edit";
        }
    }

    /**
     * Toggle admin status for a participant
     */
    @PostMapping("/{id}/participants/{userId}/toggle-admin")
    public String toggleAdminStatus(
            @PathVariable Long id,
            @PathVariable Long userId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        
        if (loggedInUser == null) {
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(id);
            
            // Check if user is authorized to manage participants (admin only)
            if (!canEditTrip(trip, loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Not authorized to manage participants");
                return REDIRECT_TRIPS_BASE + id + "/edit";
            }

            User targetUser = userService.findByIdOrThrow(userId);
            
            // User must be a participant first
            if (!trip.getParticipants().contains(targetUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "User is not a participant of this trip");
                return REDIRECT_TRIPS_BASE + id + "/edit";
            }

            boolean isCurrentlyAdmin = trip.getAdmins().contains(targetUser);
            String action;
            
            if (isCurrentlyAdmin) {
                // Remove admin status
                trip.getAdmins().remove(targetUser);
                action = "removed";
            } else {
                // Add admin status
                trip.getAdmins().add(targetUser);
                action = "granted";
            }
            
            trip.setUpdatedAt(LocalDateTime.now());
            tripService.save(trip);
            
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Admin privileges " + action + " successfully");
            
            return REDIRECT_TRIPS_BASE + id + "/edit";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Trip or user not found");
            return REDIRECT_TRIPS_BASE + id + "/edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Error updating admin status: " + e.getMessage());
            return REDIRECT_TRIPS_BASE + id + "/edit";
        }
    }
    
    /**
     * Delete a trip
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
            
            // Check if user is authorized to delete (admin only)
            if (!canEditTrip(trip, loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Not authorized to delete this trip");
                return REDIRECT_TRIPS_BASE + id;
            }
            
            // Delete the trip
            tripService.deleteById(id);
            
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Trip deleted successfully");
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
     * Allow a user to leave a trip
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
            
            // Check if user is a participant in the trip
            if (!trip.getParticipants().contains(loggedInUser)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "You are not a participant in this trip");
                return REDIRECT_TRIPS_BASE + id;
            }

            // Check if user is the only admin - prevent leaving if so
            boolean isAdmin = trip.getAdmins().contains(loggedInUser);
            if (isAdmin && trip.getAdmins().size() == 1) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Cannot leave trip: You are the only administrator. Please assign another admin before leaving.");
                return REDIRECT_TRIPS_BASE + id;
            }
            
            // Remove user from both participants and admins
            trip.removeParticipant(loggedInUser);
            if (isAdmin) {
                trip.removeAdmin(loggedInUser);
            }
            
            trip.setUpdatedAt(LocalDateTime.now());
            tripService.save(trip);
            
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "You have successfully left the trip '" + trip.getName() + "'");
            
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
