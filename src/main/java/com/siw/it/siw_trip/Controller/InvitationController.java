package com.siw.it.siw_trip.Controller;

import com.siw.it.siw_trip.Model.Invitation;
import com.siw.it.siw_trip.Model.Trip;
import com.siw.it.siw_trip.Model.User;
import com.siw.it.siw_trip.Service.InvitationService;
import com.siw.it.siw_trip.Service.TripService;
import com.siw.it.siw_trip.Service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invitations")
@CrossOrigin(origins = "*")
public class InvitationController {

    private final InvitationService invitationService;
    private final UserService userService;
    private final TripService tripService;

    public InvitationController(InvitationService invitationService,
                              UserService userService,
                              TripService tripService) {
        this.invitationService = invitationService;
        this.userService = userService;
        this.tripService = tripService;
    }

    /**
     * Get all invitations for a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Invitation>> getInvitationsForUser(@PathVariable Long userId) {
        try {
            User user = userService.findByIdOrThrow(userId);
            List<Invitation> invitations = invitationService.getInvitationsForUser(user);
            return ResponseEntity.ok(invitations);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all invitations for a trip
     */
    @GetMapping("/trip/{tripId}")
    public ResponseEntity<List<Invitation>> getInvitationsForTrip(@PathVariable Long tripId) {
        try {
            Trip trip = tripService.findByIdOrThrow(tripId);
            List<Invitation> invitations = invitationService.getInvitationsForTrip(trip);
            return ResponseEntity.ok(invitations);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create a new invitation
     */
    @PostMapping
    public ResponseEntity<Invitation> createInvitation(@RequestBody InvitationRequest request) {
        try {
            User user = userService.findByIdOrThrow(request.getUserId());
            Trip trip = tripService.findByIdOrThrow(request.getTripId());
            User invitedBy = userService.findByIdOrThrow(request.getInvitedById());

            Invitation invitation = invitationService.createInvitation(user, trip, request.getMessage(), invitedBy);
            return ResponseEntity.status(HttpStatus.CREATED).body(invitation);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Accept an invitation
     */
    @PutMapping("/{invitationId}/accept")
    public ResponseEntity<Void> acceptInvitation(@PathVariable Long invitationId, @RequestBody UserRequest request) {
        try {
            User user = userService.findByIdOrThrow(request.getUserId());
            invitationService.acceptInvitation(invitationId, user);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Decline an invitation
     */
    @PutMapping("/{invitationId}/decline")
    public ResponseEntity<Void> declineInvitation(@PathVariable Long invitationId, @RequestBody UserRequest request) {
        try {
            User user = userService.findByIdOrThrow(request.getUserId());
            invitationService.declineInvitation(invitationId, user);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Get invitation by ID
     */
    @GetMapping("/{invitationId}")
    public ResponseEntity<Invitation> getInvitationById(@PathVariable Long invitationId) {
        try {
            Invitation invitation = invitationService.getInvitationById(invitationId);
            return ResponseEntity.ok(invitation);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Delete an invitation
     */
    @DeleteMapping("/{invitationId}")
    public ResponseEntity<Void> deleteInvitation(@PathVariable Long invitationId, @RequestBody UserRequest request) {
        try {
            User user = userService.findByIdOrThrow(request.getUserId());
            invitationService.deleteInvitation(invitationId, user);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Get count of invitations for a user
     */
    @GetMapping("/user/{userId}/count")
    public ResponseEntity<Long> countInvitationsForUser(@PathVariable Long userId) {
        try {
            User user = userService.findByIdOrThrow(userId);
            long count = invitationService.countInvitationsForUser(user);
            return ResponseEntity.ok(count);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get invitations sent by a user
     */
    @GetMapping("/sent/{userId}")
    public ResponseEntity<List<Invitation>> getInvitationsSentBy(@PathVariable Long userId) {
        try {
            User user = userService.findByIdOrThrow(userId);
            List<Invitation> invitations = invitationService.getInvitationsSentBy(user);
            return ResponseEntity.ok(invitations);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Request DTOs
    public static class InvitationRequest {
        private Long userId;
        private Long tripId;
        private String message;
        private Long invitedById;

        // Constructors
        public InvitationRequest() {}

        public InvitationRequest(Long userId, Long tripId, String message, Long invitedById) {
            this.userId = userId;
            this.tripId = tripId;
            this.message = message;
            this.invitedById = invitedById;
        }

        // Getters and Setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public Long getTripId() { return tripId; }
        public void setTripId(Long tripId) { this.tripId = tripId; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Long getInvitedById() { return invitedById; }
        public void setInvitedById(Long invitedById) { this.invitedById = invitedById; }
    }

    public static class UserRequest {
        private Long userId;

        public UserRequest() {}

        public UserRequest(Long userId) {
            this.userId = userId;
        }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
    }
}
