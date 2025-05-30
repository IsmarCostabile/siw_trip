package com.siw.it.siw_trip.ViewControllers;

import com.siw.it.siw_trip.Model.Invitation;
import com.siw.it.siw_trip.Model.Trip;
import com.siw.it.siw_trip.Model.User;
import com.siw.it.siw_trip.Service.InvitationService;
import com.siw.it.siw_trip.Service.TripService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/invitations")
public class InvitationViewController {

    private static final String LOGGED_IN_USER = "loggedInUser";
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String SUCCESS_MESSAGE = "successMessage";
    private static final String REDIRECT_LOGIN = "redirect:/login";
    private static final String REDIRECT_INVITATIONS = "redirect:/invitations";

    private final InvitationService invitationService;
    private final TripService tripService;

    public InvitationViewController(InvitationService invitationService, 
                                   TripService tripService) {
        this.invitationService = invitationService;
        this.tripService = tripService;
    }

    /**
     * Show all invitations for the current user
     */
    @GetMapping
    public String showInvitations(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Please log in to view your invitations.");
            return REDIRECT_LOGIN;
        }

        try {
            // Get all invitations for the user
            List<Invitation> pendingInvitations = invitationService.getInvitationsForUser(loggedInUser);
            List<Invitation> allInvitations = invitationService.getInvitationsForUser(loggedInUser);

            model.addAttribute("pendingInvitations", pendingInvitations);
            model.addAttribute("allInvitations", allInvitations);
            model.addAttribute("user", loggedInUser);

            return "invitations/list";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Error loading invitations. Please try again.");
            return "redirect:/";
        }
    }

    /**
     * Accept an invitation
     */
    @PostMapping("/{invitationId}/accept")
    public String acceptInvitation(@PathVariable Long invitationId, 
                                 HttpSession session, 
                                 RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Please log in to accept invitations.");
            return REDIRECT_LOGIN;
        }

        try {
            // Get invitation details before accepting (since it will be deleted)
            Invitation invitation = invitationService.getInvitationById(invitationId);
            String tripName = invitation.getTrip().getName();
            
            invitationService.acceptInvitation(invitationId, loggedInUser);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, 
                "Invitation accepted! You have joined the trip: " + tripName);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Error accepting invitation. Please try again.");
        }

        return REDIRECT_INVITATIONS;
    }

    /**
     * Decline an invitation
     */
    @PostMapping("/{invitationId}/decline")
    public String declineInvitation(@PathVariable Long invitationId, 
                                  HttpSession session, 
                                  RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Please log in to decline invitations.");
            return REDIRECT_LOGIN;
        }

        try {
            // Get invitation details before declining (since it will be deleted)
            Invitation invitation = invitationService.getInvitationById(invitationId);
            String tripName = invitation.getTrip().getName();
            
            invitationService.declineInvitation(invitationId, loggedInUser);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, 
                "Invitation to trip '" + tripName + "' has been declined.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Error declining invitation. Please try again.");
        }

        return REDIRECT_INVITATIONS;
    }

    /**
     * View a trip from an invitation (allows invited users to see trip details before joining)
     */
    @GetMapping("/trip/{tripId}")
    public String viewTripFromInvitation(@PathVariable Long tripId, 
                                       HttpSession session, 
                                       Model model, 
                                       RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Please log in to view trip details.");
            return REDIRECT_LOGIN;
        }

        try {
            Trip trip = tripService.findByIdOrThrow(tripId);
            
            // Check if user has a pending invitation to this trip
            Optional<Invitation> invitationOpt = invitationService.getInvitationsForUser(loggedInUser)
                .stream()
                .filter(inv -> inv.getTrip().getId().equals(tripId))
                .findFirst();

            if (invitationOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "You don't have access to view this trip.");
                return REDIRECT_INVITATIONS;
            }

            Invitation invitation = invitationOpt.get();
            
            // Check if user is already a participant or admin (can view normally)
            boolean isParticipant = trip.getParticipants().contains(loggedInUser);
            boolean isAdmin = trip.getAdmins().contains(loggedInUser);
            
            if (isParticipant || isAdmin) {
                return "redirect:/trips/" + tripId;
            }

            model.addAttribute("trip", trip);
            model.addAttribute("invitation", invitation);
            model.addAttribute("user", loggedInUser);
            model.addAttribute("isInviteView", true);

            return "trips/view";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Trip not found.");
            return REDIRECT_INVITATIONS;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Error loading trip details. Please try again.");
            return REDIRECT_INVITATIONS;
        }
    }

    /**
     * Delete/cancel a pending invitation (for the invitee)
     */
    @PostMapping("/{invitationId}/delete")
    public String deleteInvitation(@PathVariable Long invitationId, 
                                 HttpSession session, 
                                 RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Please log in to delete invitations.");
            return REDIRECT_LOGIN;
        }

        try {
            invitationService.deleteInvitation(invitationId, loggedInUser);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Invitation deleted successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Error deleting invitation. Please try again.");
        }

        return REDIRECT_INVITATIONS;
    }
}
