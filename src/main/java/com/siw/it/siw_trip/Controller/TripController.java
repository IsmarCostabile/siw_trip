package com.siw.it.siw_trip.Controller;

import com.siw.it.siw_trip.Model.Trip;
import com.siw.it.siw_trip.Model.TripDay;
import com.siw.it.siw_trip.Model.TripStatus;
import com.siw.it.siw_trip.Model.User;
import com.siw.it.siw_trip.Service.TripService;
import com.siw.it.siw_trip.Service.UserService;
import com.siw.it.siw_trip.Service.CredentialsService;
import com.siw.it.siw_trip.Service.InvitationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@RestController
@RequestMapping("/api/trips")
@CrossOrigin(origins = "*")
public class TripController {
    
    private final TripService tripService;
    private final UserService userService;
    private final CredentialsService credentialsService;
    private final InvitationService invitationService;

    public TripController(
            TripService tripService, 
            UserService userService, 
            CredentialsService credentialsService,
            InvitationService invitationService) {
        this.tripService = tripService;
        this.userService = userService;
        this.credentialsService = credentialsService;
        this.invitationService = invitationService;
    }

    @GetMapping
    public ResponseEntity<List<Trip>> getAllTrips() {
        List<Trip> trips = tripService.findAll();
        return ResponseEntity.ok(trips);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Trip> getTripById(@PathVariable Long id) {
        try {
            Trip trip = tripService.findByIdOrThrow(id);
            return ResponseEntity.ok(trip);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search/name")
    public ResponseEntity<List<Trip>> searchTripsByName(@RequestParam String name) {
        List<Trip> trips = tripService.findByNameContaining(name);
        return ResponseEntity.ok(trips);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Trip>> getTripsByStatus(@PathVariable TripStatus status) {
        List<Trip> trips = tripService.findByStatus(status);
        return ResponseEntity.ok(trips);
    }

    @GetMapping("/participant/{userId}")
    public ResponseEntity<List<Trip>> getTripsByParticipant(@PathVariable Long userId) {
        List<Trip> trips = tripService.findByParticipantId(userId);
        return ResponseEntity.ok(trips);
    }

    @GetMapping("/admin/{userId}")
    public ResponseEntity<List<Trip>> getTripsByAdmin(@PathVariable Long userId) {
        List<Trip> trips = tripService.findByAdminId(userId);
        return ResponseEntity.ok(trips);
    }

    @GetMapping("/search/date-range")
    public ResponseEntity<List<Trip>> getTripsByDateRange(
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {
        List<Trip> trips = tripService.findByDateRange(startDate, endDate);
        return ResponseEntity.ok(trips);
    }

    @PostMapping
    public ResponseEntity<Trip> createTrip(@RequestBody Trip trip) {
        Trip savedTrip = tripService.save(trip);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedTrip);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Trip> updateTrip(@PathVariable Long id, @RequestBody Trip trip) {
        try {
            tripService.findByIdOrThrow(id); // Validate trip exists
            trip.setId(id);
            Trip updatedTrip = tripService.save(trip);
            return ResponseEntity.ok(updatedTrip);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrip(@PathVariable Long id) {
        try {
            tripService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/invite")
    public ResponseEntity<Void> inviteUser(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "false") boolean asAdmin,
            @RequestHeader("User-Id") Long inviterId) {
        try {
            Trip trip = tripService.findByIdOrThrow(id);
            User userToInvite = userService.findByIdOrThrow(userId);
            User inviter = userService.findByIdOrThrow(inviterId);
            
            // Check if user is already a participant or admin
            if (trip.getParticipants().contains(userToInvite) || trip.getAdmins().contains(userToInvite)) {
                return ResponseEntity.badRequest().build();
            }

            // Check if there's already an invitation
            if (invitationService.hasUserBeenInvited(userToInvite, trip)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }

            // Create invitation message
            String invitationMessage = asAdmin ? 
                "You have been invited to join '" + trip.getName() + "' as an administrator." :
                "You have been invited to join the trip '" + trip.getName() + "'.";

            // Create invitation
            invitationService.createInvitation(userToInvite, trip, invitationMessage, inviter, asAdmin);
            
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/participants/{userId}/toggle-admin")
    public ResponseEntity<Trip> toggleAdminStatus(
            @PathVariable Long id,
            @PathVariable Long userId) {
        try {
            Trip trip = tripService.findByIdOrThrow(id);
            User targetUser = userService.findByIdOrThrow(userId);
            
            // User must be a participant first
            if (!trip.getParticipants().contains(targetUser)) {
                return ResponseEntity.badRequest().build();
            }

            // Toggle admin status
            if (trip.getAdmins().contains(targetUser)) {
                trip.getAdmins().remove(targetUser);
            } else {
                trip.getAdmins().add(targetUser);
            }
            
            trip.setUpdatedAt(LocalDateTime.now());
            Trip updatedTrip = tripService.save(trip);
            
            return ResponseEntity.ok(updatedTrip);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<Trip> leaveTrip(
            @PathVariable Long id,
            @RequestHeader("User-Id") Long userId) {
        try {
            Trip trip = tripService.findByIdOrThrow(id);
            User user = userService.findByIdOrThrow(userId);
            
            // Check if user is a participant
            if (!trip.getParticipants().contains(user)) {
                return ResponseEntity.badRequest().build();
            }

            // Check if user is the only admin
            if (trip.getAdmins().contains(user) && trip.getAdmins().size() == 1) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(null); // Can't leave as only admin
            }
            
            trip.removeParticipant(user);
            trip.removeAdmin(user);
            
            trip.setUpdatedAt(LocalDateTime.now());
            Trip updatedTrip = tripService.save(trip);
            
            return ResponseEntity.ok(updatedTrip);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private List<User> searchUsersForTrip(String query, Long tripId) {
        List<User> users = userService.findByNameContaining(query);
        
        try {
            Optional<User> userByUsernameOpt = credentialsService.findUserByUsername(query);
            userByUsernameOpt.ifPresent(user -> {
                if (!users.contains(user)) {
                    users.add(user);
                }
            });
        } catch (Exception e) {
            // User not found by username, that's ok
        }

        if (tripId != null) {
            try {
                Trip trip = tripService.findByIdOrThrow(tripId);
                return users.stream()
                    .filter(user -> !trip.getParticipants().contains(user) && !trip.getAdmins().contains(user))
                    .toList();
            } catch (Exception e) {
                // Trip not found, continue with all users
            }
        }

        return users;
    }

    @GetMapping("/search-users")
    public ResponseEntity<List<Map<String, Object>>> searchUsers(
            @RequestParam String query, 
            @RequestParam(required = false) Long tripId) {
        try {
            List<User> users = searchUsersForTrip(query, tripId);

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
                .toList();

            return ResponseEntity.ok(userResults);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/participants/add")
    public ResponseEntity<Trip> addParticipant(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "false") boolean asAdmin) {
        try {
            Trip trip = tripService.findByIdOrThrow(id);
            User userToAdd = userService.findByIdOrThrow(userId);
            
            if (trip.getParticipants().contains(userToAdd) || trip.getAdmins().contains(userToAdd)) {
                return ResponseEntity.badRequest().build();
            }

            trip.getParticipants().add(userToAdd);
            if (asAdmin) {
                trip.getAdmins().add(userToAdd);
            }

            Trip updatedTrip = tripService.save(trip);
            return ResponseEntity.ok(updatedTrip);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/participants/{userId}")
    public ResponseEntity<Trip> removeParticipant(
            @PathVariable Long id,
            @PathVariable Long userId) {
        try {
            Trip trip = tripService.findByIdOrThrow(id);
            User userToRemove = userService.findByIdOrThrow(userId);
            
            if (!trip.getParticipants().contains(userToRemove)) {
                return ResponseEntity.badRequest().build();
            }

            trip.removeParticipant(userToRemove);
            trip.removeAdmin(userToRemove);

            Trip updatedTrip = tripService.save(trip);
            return ResponseEntity.ok(updatedTrip);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/admins")
    public ResponseEntity<Trip> addAdmin(
            @PathVariable Long id,
            @RequestBody AdminRequest request) {
        try {
            Trip updatedTrip = tripService.addAdmin(id, request.getUserId());
            return ResponseEntity.ok(updatedTrip);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/admins/{userId}")
    public ResponseEntity<Trip> removeAdmin(
            @PathVariable Long id,
            @PathVariable Long userId) {
        try {
            Trip updatedTrip = tripService.removeAdmin(id, userId);
            return ResponseEntity.ok(updatedTrip);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/trip-days")
    public ResponseEntity<Trip> addTripDay(
            @PathVariable Long id,
            @RequestBody TripDay tripDay) {
        try {
            Trip updatedTrip = tripService.addTripDay(id, tripDay);
            return ResponseEntity.ok(updatedTrip);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/trip-days/{tripDayId}")
    public ResponseEntity<Trip> removeTripDay(
            @PathVariable Long id,
            @PathVariable Long tripDayId) {
        try {
            Trip updatedTrip = tripService.removeTripDay(id, tripDayId);
            return ResponseEntity.ok(updatedTrip);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Trip> updateTripStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request) {
        try {
            Trip updatedTrip = tripService.updateStatus(id, request.getStatus());
            return ResponseEntity.ok(updatedTrip);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/participants/{userId}/remove")
    public ResponseEntity<Trip> removeParticipant(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestHeader("User-Id") Long requesterId) {
        try {
            Trip trip = tripService.findByIdOrThrow(id);
            User userToRemove = userService.findByIdOrThrow(userId);
            User requester = userService.findByIdOrThrow(requesterId);
            
            // Check if requester is an admin
            if (!trip.getAdmins().contains(requester)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // Check if user is a participant/admin
            boolean isParticipant = trip.getParticipants().contains(userToRemove);
            boolean isAdmin = trip.getAdmins().contains(userToRemove);
            boolean hasInvitation = invitationService.hasUserBeenInvited(userToRemove, trip);
            
            if (!isParticipant && !isAdmin && !hasInvitation) {
                return ResponseEntity.badRequest().build();
            }
            
            // Prevent removing the only admin
            if (trip.getAdmins().size() == 1 && trip.getAdmins().contains(userToRemove)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            
            // Handle invitation cancellation or participant removal
            if (hasInvitation) {
                invitationService.cancelInvitation(userToRemove, trip, requester);
            } else {
                if (isAdmin) {
                    tripService.removeAdmin(id, userId);
                }
                if (isParticipant) {
                    tripService.removeParticipant(id, userId);
                }
            }
            
            Trip updatedTrip = tripService.findByIdOrThrow(id);
            return ResponseEntity.ok(updatedTrip);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/participants/invite")
    public ResponseEntity<Map<String, String>> inviteParticipant(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "false") boolean asAdmin,
            @RequestHeader("User-Id") Long inviterId) {
        try {
            Trip trip = tripService.findByIdOrThrow(id);
            User userToInvite = userService.findByIdOrThrow(userId);
            User inviter = userService.findByIdOrThrow(inviterId);
            
            // Check if inviter is an admin
            if (!trip.getAdmins().contains(inviter)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Not authorized to send invitations");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }
            
            // Check if user is already a participant or admin
            if (trip.getParticipants().contains(userToInvite) || trip.getAdmins().contains(userToInvite)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User is already a participant in this trip");
                return ResponseEntity.badRequest().body(error);
            }

            // Check if there's already an invitation
            if (invitationService.hasUserBeenInvited(userToInvite, trip)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User has already been invited to this trip");
                return ResponseEntity.badRequest().body(error);
            }

            // Create invitation message
            String invitationMessage = asAdmin ? 
                "You have been invited to join '" + trip.getName() + "' as an administrator." :
                "You have been invited to join the trip '" + trip.getName() + "'.";

            // Create invitation
            invitationService.createInvitation(userToInvite, trip, invitationMessage, inviter, asAdmin);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Invitation sent successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "User or trip not found");
            return ResponseEntity.notFound().header("Content-Type", "application/json").build();
        }
    }

    @PostMapping("/{id}/manage-participant")
    public ResponseEntity<Trip> manageParticipant(
            @PathVariable Long id,
            @RequestBody ParticipantManagementRequest request,
            @RequestHeader("User-Id") Long managerId) {
        try {
            Trip trip = tripService.findByIdOrThrow(id);
            User manager = userService.findByIdOrThrow(managerId);
            User targetUser = userService.findByIdOrThrow(request.getUserId());
            
            // Check if manager is an admin
            if (!trip.getAdmins().contains(manager)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            switch (request.getAction().toLowerCase()) {
                case "promote":
                    if (trip.getParticipants().contains(targetUser) && !trip.getAdmins().contains(targetUser)) {
                        tripService.addAdmin(id, request.getUserId());
                    }
                    break;
                case "demote":
                    if (trip.getAdmins().contains(targetUser) && trip.getAdmins().size() > 1) {
                        tripService.removeAdmin(id, request.getUserId());
                    }
                    break;
                default:
                    return ResponseEntity.badRequest().build();
            }
            
            Trip updatedTrip = tripService.findByIdOrThrow(id);
            return ResponseEntity.ok(updatedTrip);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/user/{userId}/accessible")
    public ResponseEntity<List<Trip>> getAccessibleTripsForUser(@PathVariable Long userId) {
        try {
            User user = userService.findByIdOrThrow(userId);
            List<Trip> trips = tripService.findTripsForUser(user);
            return ResponseEntity.ok(trips);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/participants")
    public ResponseEntity<Map<String, Object>> getTripParticipants(@PathVariable Long id) {
        try {
            Trip trip = tripService.findByIdOrThrow(id);
            
            Map<String, Object> participants = new HashMap<>();
            participants.put("admins", trip.getAdmins());
            participants.put("participants", trip.getParticipants());
            participants.put("totalCount", trip.getParticipants().size());
            participants.put("adminCount", trip.getAdmins().size());
            
            return ResponseEntity.ok(participants);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<Map<String, Object>> getTripSummary(@PathVariable Long id) {
        try {
            Trip trip = tripService.findByIdOrThrow(id);
            
            Map<String, Object> summary = new HashMap<>();
            summary.put("id", trip.getId());
            summary.put("name", trip.getName());
            summary.put("status", trip.getStatus());
            summary.put("startDateTime", trip.getStartDateTime());
            summary.put("endDateTime", trip.getEndDateTime());
            summary.put("participantCount", trip.getParticipants().size());
            summary.put("adminCount", trip.getAdmins().size());
            summary.put("createdAt", trip.getCreatedAt());
            summary.put("updatedAt", trip.getUpdatedAt());
            
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Inner classes for request bodies
    public static class ParticipantRequest {
        private Long userId;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
    }

    public static class AdminRequest {
        private Long userId;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
    }

    public static class StatusUpdateRequest {
        private TripStatus status;

        public TripStatus getStatus() { return status; }
        public void setStatus(TripStatus status) { this.status = status; }
    }

    public static class BudgetUpdateRequest {
        private BigDecimal budget;

        public BigDecimal getBudget() { return budget; }
        public void setBudget(BigDecimal budget) { this.budget = budget; }
    }

    public static class ParticipantManagementRequest {
        private Long userId;
        private String action; // "promote" or "demote"

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }
    }
}
