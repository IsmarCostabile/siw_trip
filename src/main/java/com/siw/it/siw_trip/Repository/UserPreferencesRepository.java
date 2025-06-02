package com.siw.it.siw_trip.Repository;

import com.siw.it.siw_trip.Model.User;
import com.siw.it.siw_trip.Model.UserPreferences.UserPreferences;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Long> {
    
    /**
     * Find user preferences by user
     */
    Optional<UserPreferences> findByUser(User user);
    
    /**
     * Find user preferences by user ID
     */
    Optional<UserPreferences> findByUserId(Long userId);
    
    /**
     * Check if preferences exist for user
     */
    boolean existsByUser(User user);
    
    /**
     * Check if preferences exist for user ID
     */
    boolean existsByUserId(Long userId);
}
