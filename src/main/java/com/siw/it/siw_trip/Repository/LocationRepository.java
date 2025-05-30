package com.siw.it.siw_trip.Repository;

import com.siw.it.siw_trip.Model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
    
    List<Location> findByNameContainingIgnoreCase(String name);
    
    List<Location> findByCityIgnoreCase(String city);
    
    List<Location> findByCountryIgnoreCase(String country);
    
    @Query("SELECT l FROM Location l WHERE l.name = :name AND l.city = :city")
    Optional<Location> findByNameAndCity(@Param("name") String name, @Param("city") String city);
    
    @Query("SELECT l FROM Location l WHERE l.latitude IS NOT NULL AND l.longitude IS NOT NULL")
    List<Location> findAllWithCoordinates();
    
    @Query("SELECT l FROM Location l WHERE " +
           "(:name IS NULL OR LOWER(l.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:city IS NULL OR LOWER(l.city) LIKE LOWER(CONCAT('%', :city, '%'))) AND " +
           "(:country IS NULL OR LOWER(l.country) LIKE LOWER(CONCAT('%', :country, '%')))")
    List<Location> searchLocations(@Param("name") String name, 
                                 @Param("city") String city, 
                                 @Param("country") String country);
}
