package com.siw.it.siw_trip.Service;

import com.siw.it.siw_trip.Model.TripDay;
import com.siw.it.siw_trip.Model.Location;
import com.siw.it.siw_trip.Model.Trip;
import com.siw.it.siw_trip.Repository.TripDayRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TripDayService {
    
    private static final Logger logger = LoggerFactory.getLogger(TripDayService.class);

    private final TripDayRepository tripDayRepository;
    private final RouteService routeService;
    private final LocationService locationService;
    
    public TripDayService(TripDayRepository tripDayRepository, RouteService routeService, LocationService locationService) {
        this.tripDayRepository = tripDayRepository;
        this.routeService = routeService;
        this.locationService = locationService;
    }

    public List<TripDay> findAll() {
        return tripDayRepository.findAll();
    }

    public Optional<TripDay> findById(Long id) {
        return tripDayRepository.findById(id);
    }

    public TripDay save(TripDay tripDay) {
        return tripDayRepository.save(tripDay);
    }

    public void deleteById(Long id) {
        tripDayRepository.deleteById(id);
    }

    public List<TripDay> findByTripOrderByDayNumber(Trip trip) {
        return tripDayRepository.findByTripOrderByDayNumber(trip);
    }

    public List<TripDay> findByTripIdOrderByDayNumber(Long tripId) {
        return tripDayRepository.findByTripIdOrderByDayNumber(tripId);
    }

    public List<TripDay> findByDate(LocalDate date) {
        return tripDayRepository.findByDate(date);
    }

    public List<TripDay> findByDateBetween(LocalDate startDate, LocalDate endDate) {
        return tripDayRepository.findByDateBetween(startDate, endDate);
    }

    public TripDay findByTripIdAndDayNumber(Long tripId, Integer dayNumber) {
        return tripDayRepository.findByTripIdAndDayNumber(tripId, dayNumber);
    }

    public boolean existsById(Long id) {
        return tripDayRepository.existsById(id);
    }

    // Alias method for backward compatibility with controller
    public List<TripDay> findByTripId(Long tripId) {
        return findByTripIdOrderByDayNumber(tripId);
    }

    /**
     * Add a visit to a trip day and create routes to connect it with adjacent visits
     */
    @Transactional
    public TripDay addVisit(Long tripDayId, com.siw.it.siw_trip.Model.Visit visit) {
        Optional<TripDay> tripDayOpt = findById(tripDayId);
        if (!tripDayOpt.isPresent()) {
            throw new IllegalArgumentException("TripDay not found with id: " + tripDayId);
        }

        TripDay tripDay = tripDayOpt.get();
        visit.setTripDay(tripDay);
        
        // Add visit to trip day
        tripDay.addVisit(visit);
        
        // Create routes for the new visit
        createRoutesForNewVisit(tripDay, visit);
        
        return save(tripDay);
    }
    
    /**
     * Creates routes when a new visit is added to maintain connectivity
     * If visit B is added between A and C, creates routes A→B and B→C
     */
    private void createRoutesForNewVisit(TripDay tripDay, com.siw.it.siw_trip.Model.Visit newVisit) {
        logger.info("Creating routes for new visit '{}' in trip day {}", newVisit.getName(), tripDay.getId());
        
        // Sort visits by start time to determine order
        List<com.siw.it.siw_trip.Model.Visit> sortedVisits = new ArrayList<>(tripDay.getVisits());
        sortedVisits.sort((v1, v2) -> {
            if (v1.getStartTime() == null && v2.getStartTime() == null) return 0;
            if (v1.getStartTime() == null) return 1;
            if (v2.getStartTime() == null) return -1;
            return v1.getStartTime().compareTo(v2.getStartTime());
        });
        
        int newVisitIndex = sortedVisits.indexOf(newVisit);
        
        // Create route from previous visit to new visit (A→B)
        if (newVisitIndex > 0) {
            com.siw.it.siw_trip.Model.Visit previousVisit = sortedVisits.get(newVisitIndex - 1);
            logger.info("Creating route from previous visit '{}' to new visit '{}'", 
                previousVisit.getName(), newVisit.getName());
            Optional<com.siw.it.siw_trip.Model.Route> existingRoute = routeService.findByFromVisitAndToVisit(previousVisit, newVisit);
            if (!existingRoute.isPresent()) {
                com.siw.it.siw_trip.Model.Route routeFromPrevious = new com.siw.it.siw_trip.Model.Route(previousVisit, newVisit, tripDay);
                logger.info("Calling routeService.saveAndCalculate() for route from '{}' to '{}'", 
                    previousVisit.getName(), newVisit.getName());
                routeService.saveAndCalculate(routeFromPrevious);
            } else {
                logger.info("Route from '{}' to '{}' already exists, skipping creation", 
                    previousVisit.getName(), newVisit.getName());
            }
        }
        
        // Create route from new visit to next visit (B→C)
        if (newVisitIndex < sortedVisits.size() - 1) {
            com.siw.it.siw_trip.Model.Visit nextVisit = sortedVisits.get(newVisitIndex + 1);
            logger.info("Creating route from new visit '{}' to next visit '{}'", 
                newVisit.getName(), nextVisit.getName());
            Optional<com.siw.it.siw_trip.Model.Route> existingRoute = routeService.findByFromVisitAndToVisit(newVisit, nextVisit);
            if (!existingRoute.isPresent()) {
                com.siw.it.siw_trip.Model.Route routeToNext = new com.siw.it.siw_trip.Model.Route(newVisit, nextVisit, tripDay);
                logger.info("Calling routeService.saveAndCalculate() for route from '{}' to '{}'", 
                    newVisit.getName(), nextVisit.getName());
                routeService.saveAndCalculate(routeToNext);
            } else {
                logger.info("Route from '{}' to '{}' already exists, skipping creation", 
                    newVisit.getName(), nextVisit.getName());
            }
        }
        
        // If there was a direct route between previous and next visit, remove it
        // because now we have previous→new→next instead of previous→next
        if (newVisitIndex > 0 && newVisitIndex < sortedVisits.size() - 1) {
            com.siw.it.siw_trip.Model.Visit previousVisit = sortedVisits.get(newVisitIndex - 1);
            com.siw.it.siw_trip.Model.Visit nextVisit = sortedVisits.get(newVisitIndex + 1);
            Optional<com.siw.it.siw_trip.Model.Route> existingRoute = routeService.findByFromVisitAndToVisit(previousVisit, nextVisit);
            if (existingRoute.isPresent()) {
                routeService.deleteById(existingRoute.get().getId());
            }
        }
    }

    /**
     * Remove a visit from a trip day and handle route cleanup
     */
    @Transactional
    public TripDay removeVisit(Long tripDayId, Long visitId) {
        Optional<TripDay> tripDayOpt = findById(tripDayId);
        if (!tripDayOpt.isPresent()) {
            throw new IllegalArgumentException("TripDay not found with id: " + tripDayId);
        }

        TripDay tripDay = tripDayOpt.get();
        // Find the visit by ID and remove it
        com.siw.it.siw_trip.Model.Visit visitToRemove = tripDay.getVisits().stream()
            .filter(visit -> visit.getId().equals(visitId))
            .findFirst()
            .orElse(null);
            
        if (visitToRemove != null) {
            // Remove all routes involving this visit
            routeService.deleteByVisit(visitToRemove);
            
            // Remove visit from trip day
            tripDay.removeVisit(visitToRemove);
            
            // Reconnect remaining visits if needed
            reconnectAfterVisitRemoval(tripDay);
        }
        
        return save(tripDay);
    }
    
    /**
     * Reconnects visits after a visit is removed
     * If B is removed from A→B→C, creates route A→C
     */
    private void reconnectAfterVisitRemoval(TripDay tripDay) {
        // Sort remaining visits by start time
        List<com.siw.it.siw_trip.Model.Visit> sortedVisits = new ArrayList<>(tripDay.getVisits());
        sortedVisits.sort((v1, v2) -> {
            if (v1.getStartTime() == null && v2.getStartTime() == null)
                return 0;
            if (v1.getStartTime() == null)
                return 1;
            if (v2.getStartTime() == null)
                return -1;
            return v1.getStartTime().compareTo(v2.getStartTime());
        });

        // Find visits that were connected through the removed visit
        // and create a direct connection between them
        for (int i = 0; i < sortedVisits.size() - 1; i++) {
            com.siw.it.siw_trip.Model.Visit currentVisit = sortedVisits.get(i);
            com.siw.it.siw_trip.Model.Visit nextVisit = sortedVisits.get(i + 1);

            // Check if there's no direct route between consecutive visits
            Optional<com.siw.it.siw_trip.Model.Route> existingRoute = routeService
                    .findByFromVisitAndToVisit(currentVisit, nextVisit);
            if (!existingRoute.isPresent()) {
                com.siw.it.siw_trip.Model.Route reconnectionRoute = new com.siw.it.siw_trip.Model.Route(currentVisit,
                        nextVisit, tripDay);
                routeService.saveAndCalculate(reconnectionRoute);
            }
        }
    }
    
    /**
     * Update the destination location for a trip day
     */
    @Transactional
    public TripDay updateDestinationLocation(Long tripDayId, Long locationId) {
        Optional<TripDay> tripDayOpt = findById(tripDayId);
        if (!tripDayOpt.isPresent()) {
            throw new IllegalArgumentException("TripDay not found with id: " + tripDayId);
        }

        TripDay tripDay = tripDayOpt.get();

        if (locationId != null) {
            Optional<Location> locationOpt = locationService.findById(locationId);
            if (!locationOpt.isPresent()) {
                throw new IllegalArgumentException("Location not found with id: " + locationId);
            }
            tripDay.setDestination(locationOpt.get());
        } else {
            tripDay.setDestination(null);
        }

        return save(tripDay);
    }
    
    @Transactional
        public TripDay removeDestination(Long tripDayId) {
        Optional<TripDay> tripDayOpt = findById(tripDayId);
        if (!tripDayOpt.isPresent()) {
            throw new IllegalArgumentException("TripDay not found with id: " + tripDayId);
        }
    
        TripDay tripDay = tripDayOpt.get();
        tripDay.setDestination(null);
        return save(tripDay);
    }
}
