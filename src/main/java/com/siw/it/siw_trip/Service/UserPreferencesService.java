package com.siw.it.siw_trip.Service;

import com.siw.it.siw_trip.Model.User;
import com.siw.it.siw_trip.Model.UserPreferences.UserPreferences;
import com.siw.it.siw_trip.Repository.UserPreferencesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Service
@Transactional
public class UserPreferencesService {

    private static final Logger logger = LoggerFactory.getLogger(UserPreferencesService.class);

    private final UserPreferencesRepository userPreferencesRepository;
    private final UserService userService;

    public UserPreferencesService(UserPreferencesRepository userPreferencesRepository, UserService userService) {
        this.userPreferencesRepository = userPreferencesRepository;
        this.userService = userService;
    }

    /**
     * Find all user preferences
     */
    public Iterable<UserPreferences> findAll() {
        return userPreferencesRepository.findAll();
    }

    /**
     * Find user preferences by ID
     */
    public Optional<UserPreferences> findById(Long id) {
        return userPreferencesRepository.findById(id);
    }

    /**
     * Find user preferences by user
     */
    public Optional<UserPreferences> findByUser(User user) {
        return userPreferencesRepository.findByUser(user);
    }

    /**
     * Find user preferences by user ID
     */
    public Optional<UserPreferences> findByUserId(Long userId) {
        return userPreferencesRepository.findByUserId(userId);
    }

    /**
     * Get user preferences or create default ones if they don't exist
     */
    public UserPreferences getOrCreatePreferences(User user) {
        Optional<UserPreferences> existingPreferences = findByUser(user);
        if (existingPreferences.isPresent()) {
            return existingPreferences.get();
        }
        
        // Create default preferences
        UserPreferences preferences = new UserPreferences(user);
        return save(preferences);
    }

    /**
     * Get user preferences by user ID or create default ones if they don't exist
     */
    public UserPreferences getOrCreatePreferences(Long userId) {
        Optional<User> user = userService.findById(userId);
        if (user.isEmpty()) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
        return getOrCreatePreferences(user.get());
    }

    /**
     * Save user preferences
     */
    public UserPreferences save(UserPreferences preferences) {
        logger.info("Attempting to save UserPreferences for user ID: {}", 
                   preferences.getUser() != null ? preferences.getUser().getId() : "null");
        logger.debug("Preferences details - Language: {}, DistanceUnit: {}, TimeFormat: {}, TemperatureUnit: {}", 
                    preferences.getLanguage(), preferences.getDistanceUnit(), 
                    preferences.getTimeFormat(), preferences.getTemperatureUnit());
        
        UserPreferences savedPreferences = userPreferencesRepository.save(preferences);
        
        logger.info("Successfully saved UserPreferences with ID: {}", savedPreferences.getId());
        return savedPreferences;
    }

    /**
     * Update user preferences
     */
    public UserPreferences updatePreferences(Long userId, UserPreferences updatedPreferences) {
        logger.info("Starting updatePreferences for user ID: {}", userId);
        
        Optional<User> user = userService.findById(userId);
        if (user.isEmpty()) {
            logger.error("User not found with ID: {}", userId);
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }

        logger.info("Found user: {}", user.get().getEmail());
        UserPreferences existingPreferences = getOrCreatePreferences(user.get());
        logger.info("Retrieved existing preferences with ID: {}", existingPreferences.getId());
        
        // Log the values being updated
        logger.debug("Updating preferences - Old values: Language={}, Distance={}, Time={}, Temp={}", 
                    existingPreferences.getLanguage(), existingPreferences.getDistanceUnit(),
                    existingPreferences.getTimeFormat(), existingPreferences.getTemperatureUnit());
        logger.debug("Updating preferences - New values: Language={}, Distance={}, Time={}, Temp={}", 
                    updatedPreferences.getLanguage(), updatedPreferences.getDistanceUnit(),
                    updatedPreferences.getTimeFormat(), updatedPreferences.getTemperatureUnit());
        
        // Update all fields
        existingPreferences.setLanguage(updatedPreferences.getLanguage());
        existingPreferences.setDistanceUnit(updatedPreferences.getDistanceUnit());
        existingPreferences.setTimeFormat(updatedPreferences.getTimeFormat());
        existingPreferences.setTemperatureUnit(updatedPreferences.getTemperatureUnit());
        existingPreferences.setDateFormat(updatedPreferences.getDateFormat());
        existingPreferences.setCurrency(updatedPreferences.getCurrency());
        existingPreferences.setNotificationsEnabled(updatedPreferences.getNotificationsEnabled());
        existingPreferences.setEmailNotifications(updatedPreferences.getEmailNotifications());
        
        logger.info("About to save updated preferences");
        UserPreferences result = save(existingPreferences);
        logger.info("Finished updatePreferences for user ID: {}, result ID: {}", userId, result.getId());
        
        return result;
    }

    /**
     * Delete user preferences by ID
     */
    public void deleteById(Long id) {
        userPreferencesRepository.deleteById(id);
    }

    /**
     * Delete user preferences by user
     */
    public void deleteByUser(User user) {
        Optional<UserPreferences> preferences = findByUser(user);
        preferences.ifPresent(userPreferencesRepository::delete);
    }

    /**
     * Check if preferences exist for user
     */
    public boolean existsByUser(User user) {
        return userPreferencesRepository.existsByUser(user);
    }

    /**
     * Check if preferences exist for user ID
     */
    public boolean existsByUserId(Long userId) {
        return userPreferencesRepository.existsByUserId(userId);
    }

    /**
     * Reset preferences to default values
     */
    public UserPreferences resetToDefaults(Long userId) {
        Optional<User> user = userService.findById(userId);
        if (user.isEmpty()) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }

        UserPreferences preferences = getOrCreatePreferences(user.get());
        
        // Reset to default values
        preferences.setLanguage(com.siw.it.siw_trip.Model.UserPreferences.Language.ENGLISH);
        preferences.setDistanceUnit(com.siw.it.siw_trip.Model.UserPreferences.DistanceUnit.KILOMETERS);
        preferences.setTimeFormat(com.siw.it.siw_trip.Model.UserPreferences.TimeFormat.HOUR_24);
        preferences.setTemperatureUnit(com.siw.it.siw_trip.Model.UserPreferences.TemperatureUnit.CELSIUS);
        preferences.setDateFormat("yyyy-MM-dd");
        preferences.setCurrency("EUR");
        preferences.setNotificationsEnabled(true);
        preferences.setEmailNotifications(true);
        
        return save(preferences);
    }
}
