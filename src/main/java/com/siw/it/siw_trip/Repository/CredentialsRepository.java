package com.siw.it.siw_trip.Repository;

import com.siw.it.siw_trip.Model.Credentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CredentialsRepository extends JpaRepository<Credentials, Long> {
    
    Optional<Credentials> findByUsername(String username);
    
    boolean existsByUsername(String username);
    
    @Query("SELECT c FROM Credentials c JOIN FETCH c.user WHERE c.username = :username")
    Optional<Credentials> findByUsernameWithUser(@Param("username") String username);
    
    List<Credentials> findByLastPasswordChangeBefore(LocalDateTime dateTime);
    
    Optional<Credentials> findByUserId(Long userId);
}
