package com.siw.it.siw_trip.ViewControllers;

import com.siw.it.siw_trip.Model.*;
import com.siw.it.siw_trip.Model.UserPreferences.DistanceUnit;
import com.siw.it.siw_trip.Model.UserPreferences.Language;
import com.siw.it.siw_trip.Model.UserPreferences.TemperatureUnit;
import com.siw.it.siw_trip.Model.UserPreferences.TimeFormat;
import com.siw.it.siw_trip.Model.UserPreferences.UserPreferences;
import com.siw.it.siw_trip.Service.UserPreferencesService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/preferences")
public class UserPreferencesController {

    private static final Logger logger = LoggerFactory.getLogger(UserPreferencesController.class);

    private static final String LOGGED_IN_USER = "loggedInUser";
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String SUCCESS_MESSAGE = "successMessage";
    private static final String REDIRECT_LOGIN = "redirect:/login";
    private static final String REDIRECT_PREFERENCES = "redirect:/preferences";
    
    // Model attribute constants
    private static final String LANGUAGES_ATTR = "languages";
    private static final String DISTANCE_UNITS_ATTR = "distanceUnits";
    private static final String TIME_FORMATS_ATTR = "timeFormats";
    private static final String TEMPERATURE_UNITS_ATTR = "temperatureUnits";

    private final UserPreferencesService userPreferencesService;

    public UserPreferencesController(UserPreferencesService userPreferencesService) {
        this.userPreferencesService = userPreferencesService;
    }

    /**
     * Show user preferences page
     */
    @GetMapping
    public String showPreferences(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Please log in to view preferences.");
            return REDIRECT_LOGIN;
        }

        try {
            UserPreferences preferences = userPreferencesService.getOrCreatePreferences(loggedInUser);
            
            model.addAttribute("preferences", preferences);
            model.addAttribute("user", loggedInUser);
            model.addAttribute(LANGUAGES_ATTR, Language.values());
            model.addAttribute(DISTANCE_UNITS_ATTR, DistanceUnit.values());
            model.addAttribute(TIME_FORMATS_ATTR, TimeFormat.values());
            model.addAttribute(TEMPERATURE_UNITS_ATTR, TemperatureUnit.values());
            
            return "preferences/view";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Error loading preferences: " + e.getMessage());
            return "redirect:/profile";
        }
    }

    /**
     * Show edit preferences form
     */
    @GetMapping("/edit")
    public String showEditPreferences(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Please log in to edit preferences.");
            return REDIRECT_LOGIN;
        }

        try {
            UserPreferences preferences = userPreferencesService.getOrCreatePreferences(loggedInUser);
            
            model.addAttribute("preferences", preferences);
            model.addAttribute("user", loggedInUser);
            model.addAttribute(LANGUAGES_ATTR, Language.values());
            model.addAttribute(DISTANCE_UNITS_ATTR, DistanceUnit.values());
            model.addAttribute(TIME_FORMATS_ATTR, TimeFormat.values());
            model.addAttribute(TEMPERATURE_UNITS_ATTR, TemperatureUnit.values());
            
            return "preferences/edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Error loading preferences for editing: " + e.getMessage());
            return REDIRECT_PREFERENCES;
        }
    }

    /**
     * Handle preferences update
     */
    @PostMapping("/update")
    public String updatePreferences(@ModelAttribute("preferences") UserPreferences preferences,
                                  Model model,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            logger.warn("User not logged in when trying to update preferences");
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Please log in to update preferences.");
            return REDIRECT_LOGIN;
        }

        logger.info("Updating preferences for user: {} (ID: {})", loggedInUser.getEmail(), loggedInUser.getId());
        logger.debug("Received preferences data - Language: {}, Distance: {}, Time: {}, Temp: {}", 
                    preferences.getLanguage(), preferences.getDistanceUnit(),
                    preferences.getTimeFormat(), preferences.getTemperatureUnit());

        // Set the user on the preferences object since it's not included in the form
        preferences.setUser(loggedInUser);

        // Basic validation for required enum fields
        if (preferences.getLanguage() == null || preferences.getDistanceUnit() == null || 
            preferences.getTimeFormat() == null || preferences.getTemperatureUnit() == null) {
            logger.error("Required preference fields are missing");
            model.addAttribute("user", loggedInUser);
            model.addAttribute(LANGUAGES_ATTR, Language.values());
            model.addAttribute(DISTANCE_UNITS_ATTR, DistanceUnit.values());
            model.addAttribute(TIME_FORMATS_ATTR, TimeFormat.values());
            model.addAttribute(TEMPERATURE_UNITS_ATTR, TemperatureUnit.values());
            return "preferences/edit";
        }

        try {
            logger.info("Calling userPreferencesService.updatePreferences");
            userPreferencesService.updatePreferences(loggedInUser.getId(), preferences);
            logger.info("Successfully updated preferences");
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Preferences updated successfully!");
            return REDIRECT_PREFERENCES;
        } catch (Exception e) {
            logger.error("Error updating preferences: ", e);
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Error updating preferences: " + e.getMessage());
            return "redirect:/preferences/edit";
        }
    }

    /**
     * Reset preferences to default values
     */
    @PostMapping("/reset")
    public String resetPreferences(HttpSession session, RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute(LOGGED_IN_USER);
        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Please log in to reset preferences.");
            return REDIRECT_LOGIN;
        }

        try {
            userPreferencesService.resetToDefaults(loggedInUser.getId());
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Preferences reset to default values!");
            return REDIRECT_PREFERENCES;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Error resetting preferences: " + e.getMessage());
            return REDIRECT_PREFERENCES;
        }
    }
}
