package com.siw.it.siw_trip.Repository;

import com.siw.it.siw_trip.Model.User;
import com.siw.it.siw_trip.Model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    List<User> findByRole(UserRole role);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u LEFT JOIN u.credentials c WHERE u.firstName LIKE %:name% OR u.lastName LIKE %:name% OR c.username LIKE %:name%")
    List<User> findByNameContaining(@Param("name") String name);
    

}
