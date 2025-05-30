package com.siw.it.siw_trip.Service;

import com.siw.it.siw_trip.Model.Invitation;
import com.siw.it.siw_trip.Model.Trip;
import com.siw.it.siw_trip.Model.User;
import com.siw.it.siw_trip.Repository.InvitationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final TripService tripService;

    public InvitationService(InvitationRepository invitationRepository, TripService tripService) {
        this.invitationRepository = invitationRepository;
        this.tripService = tripService;
    }

    /**
     * Create and send an invitation to a user for a trip
     */
    @Transactional
    public Invitation createInvitation(User user, Trip trip, String message, User invitedBy) {
        // Check if invitation already exists
        Optional<Invitation> existingInvitation = invitationRepository.findByUserAndTrip(user, trip);
        if (existingInvitation.isPresent()) {
            throw new IllegalArgumentException("User has already been invited to this trip");
        }

        Invitation invitation = new Invitation(user, trip, message, invitedBy, false);
        return invitationRepository.save(invitation);
    }

    /**
     * Create and send an invitation to a user for a trip with admin flag
     */
    @Transactional
    public Invitation createInvitation(User user, Trip trip, String message, User invitedBy, boolean isAdminInvitation) {
        // Check if invitation already exists
        Optional<Invitation> existingInvitation = invitationRepository.findByUserAndTrip(user, trip);
        if (existingInvitation.isPresent()) {
            throw new IllegalArgumentException("User has already been invited to this trip");
        }

        Invitation invitation = new Invitation(user, trip, message, invitedBy, isAdminInvitation);
        return invitationRepository.save(invitation);
    }

    /**
     * Accept an invitation
     */
    @Transactional
    public void acceptInvitation(Long invitationId, User user) {
        Invitation invitation = getInvitationById(invitationId);
        
        if (!invitation.getUser().equals(user)) {
            throw new IllegalArgumentException("User is not authorized to accept this invitation");
        }

        // Add user to trip participants
        Trip trip = invitation.getTrip();
        tripService.addParticipant(trip.getId(), user.getId());
        
        // If this was an admin invitation, make the user an admin
        if (invitation.isAdminInvitation()) {
            tripService.addAdmin(trip.getId(), user.getId());
        }
        
        // Delete the invitation from the database after acceptance
        invitationRepository.delete(invitation);
    }

    /**
     * Decline an invitation
     */
    @Transactional
    public void declineInvitation(Long invitationId, User user) {
        Invitation invitation = getInvitationById(invitationId);
        
        if (!invitation.getUser().equals(user)) {
            throw new IllegalArgumentException("User is not authorized to decline this invitation");
        }

        // Delete the invitation from the database after decline
        invitationRepository.delete(invitation);
    }

    /**
     * Get invitation by ID
     */
    public Invitation getInvitationById(Long id) {
        return invitationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found with id: " + id));
    }

    /**
     * Get all invitations for a user
     */
    public List<Invitation> getInvitationsForUser(User user) {
        return invitationRepository.findByUser(user);
    }

    /**
     * Get all invitations for a trip
     */
    public List<Invitation> getInvitationsForTrip(Trip trip) {
        return invitationRepository.findByTrip(trip);
    }

    /**
     * Get invitations sent by a user
     */
    public List<Invitation> getInvitationsSentBy(User user) {
        return invitationRepository.findByInvitedBy(user);
    }

    /**
     * Count invitations for a user
     */
    public long countInvitationsForUser(User user) {
        return invitationRepository.countInvitationsForUser(user);
    }

    /**
     * Check if user has been invited to a trip
     */
    public boolean hasUserBeenInvited(User user, Trip trip) {
        return invitationRepository.findByUserAndTrip(user, trip).isPresent();
    }

    /**
     * Delete an invitation (only if pending)
     */
    @Transactional
    public void deleteInvitation(Long invitationId, User requestingUser) {
        Invitation invitation = getInvitationById(invitationId);
        
        // Only the invitation recipient or the person who sent it can delete it
        if (!invitation.getUser().equals(requestingUser) && 
            !invitation.getInvitedBy().equals(requestingUser)) {
            throw new IllegalArgumentException("User is not authorized to delete this invitation");
        }

        invitationRepository.delete(invitation);
    }

    /**
     * Get all invitations for trips where the user is an admin
     */
    public List<Invitation> getInvitationsForTripsWhereUserIsAdmin(User user) {
        return invitationRepository.findInvitationsForTripsWhereUserIsAdmin(user);
    }
}
