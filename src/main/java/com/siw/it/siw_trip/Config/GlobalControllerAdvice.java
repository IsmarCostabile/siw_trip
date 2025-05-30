package com.siw.it.siw_trip.Config;

import com.siw.it.siw_trip.Model.User;
import com.siw.it.siw_trip.Model.Trip;
import com.siw.it.siw_trip.Service.TripService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Collections;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final TripService tripService;
    private static final String LOGGED_IN_USER = "loggedInUser";

    @Autowired
    public GlobalControllerAdvice(TripService tripService) {
        this.tripService = tripService;
    }

    @ModelAttribute
    public void addUserTripsToModel(Model model, HttpSession session) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        
        if (loggedInUser != null) {
            try {
                // Get trips where the user is a participant
                List<Trip> userTrips = tripService.findByParticipantId(loggedInUser.getId());
                
                // Sort by updated date (most recent first) and limit to reasonable number for sidebar
                userTrips = userTrips.stream()
                    .sorted((t1, t2) -> t2.getUpdatedAt().compareTo(t1.getUpdatedAt()))
                    .limit(10) // Limit to 10 most recent trips for performance
                    .collect(java.util.stream.Collectors.toList());
                
                model.addAttribute("userTrips", userTrips);
            } catch (Exception e) {
                // In case of any error, just provide empty list
                model.addAttribute("userTrips", Collections.emptyList());
            }
        } else {
            model.addAttribute("userTrips", Collections.emptyList());
        }
    }
}
