package com.siw.it.siw_trip.Service;

import com.siw.it.siw_trip.Model.Route;
import com.siw.it.siw_trip.Model.Visit;
import com.siw.it.siw_trip.Model.TripDay;
import com.siw.it.siw_trip.Model.TransportMode;
import com.siw.it.siw_trip.Repository.RouteRepository;
import com.siw.it.siw_trip.Repository.TripDayRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@Service
public class RouteService {

    private static final Logger logger = LoggerFactory.getLogger(RouteService.class);

    private final RouteRepository routeRepository;
    private final RouteCalculationService routeCalculationService;
    private final VisitService visitService;
    private final TripDayRepository tripDayRepository;

    public RouteService(RouteRepository routeRepository, 
                       RouteCalculationService routeCalculationService,
                       VisitService visitService,
                       TripDayRepository tripDayRepository) {
        this.routeRepository = routeRepository;
        this.routeCalculationService = routeCalculationService;
        this.visitService = visitService;
        this.tripDayRepository = tripDayRepository;
    }

    public List<Route> findAll() {
        return routeRepository.findAll();
    }

    public Optional<Route> findById(Long id) {
        return routeRepository.findById(id);
    }

    public Route save(Route route) {
        return routeRepository.save(route);
    }

    public void deleteById(Long id) {
        routeRepository.deleteById(id);
    }

    public Optional<Route> findByFromVisitAndToVisit(Visit fromVisit, Visit toVisit) {
        return routeRepository.findByFromVisitAndToVisit(fromVisit, toVisit);
    }

    public List<Route> findByFromVisit(Visit fromVisit) {
        return routeRepository.findByFromVisit(fromVisit);
    }

    public List<Route> findByToVisit(Visit toVisit) {
        return routeRepository.findByToVisit(toVisit);
    }

    public List<Route> findByTransportMode(TransportMode transportMode) {
        return routeRepository.findByTransportMode(transportMode);
    }

    public List<Route> findByVisitId(Long visitId) {
        return routeRepository.findByVisitId(visitId);
    }

    public List<Route> findByTripId(Long tripId) {
        return routeRepository.findByTripId(tripId);
    }

    public List<Route> findByEstimatedDurationGreaterThan(Integer duration) {
        return routeRepository.findByEstimatedDurationGreaterThan(duration);
    }

    public boolean existsById(Long id) {
        return routeRepository.existsById(id);
    }
    
    public List<Route> findByTripDayId(Long tripDayId) {
        return routeRepository.findByTripDayId(tripDayId);
    }
    
    public List<Route> findByVisit(Visit visit) {
        return routeRepository.findByVisit(visit);
    }
    
    public void deleteByVisit(Visit visit) {
        routeRepository.deleteByVisit(visit);
    }

    /**
     * Save route and automatically calculate route details using Google Maps API
     */
    public Route saveAndCalculate(Route route) {
        logger.info("Saving route and calculating details for route from {} to {}", 
            route.getFromVisit().getName(), route.getToVisit().getName());
        
        try {
            // First save the route to get an ID
            Route savedRoute = routeRepository.save(route);
            
            // Debug logging for route calculation
            logger.info("Checking if route can be calculated...");
            logger.info("From visit: {} has location: {}", 
                savedRoute.getFromVisit().getName(), 
                savedRoute.getFromVisit().getLocation() != null);
            logger.info("To visit: {} has location: {}", 
                savedRoute.getToVisit().getName(), 
                savedRoute.getToVisit().getLocation() != null);
            
            if (savedRoute.getFromVisit().getLocation() != null) {
                logger.info("From location has coordinates: {}", 
                    savedRoute.getFromVisit().getLocation().hasCoordinates());
                if (savedRoute.getFromVisit().getLocation().hasCoordinates()) {
                    logger.info("From coordinates: {}, {}", 
                        savedRoute.getFromVisit().getLocation().getLatitude(),
                        savedRoute.getFromVisit().getLocation().getLongitude());
                }
            }
            
            if (savedRoute.getToVisit().getLocation() != null) {
                logger.info("To location has coordinates: {}", 
                    savedRoute.getToVisit().getLocation().hasCoordinates());
                if (savedRoute.getToVisit().getLocation().hasCoordinates()) {
                    logger.info("To coordinates: {}, {}", 
                        savedRoute.getToVisit().getLocation().getLatitude(),
                        savedRoute.getToVisit().getLocation().getLongitude());
                }
            }
            
            // Then calculate route details if possible
            if (routeCalculationService.canCalculateRoute(savedRoute)) {
                logger.info("Route calculation is possible - calling Google Maps API");
                savedRoute = routeCalculationService.calculateRoute(savedRoute);
                // Save again with calculated data
                savedRoute = routeRepository.save(savedRoute);
                logger.info("Successfully calculated and saved route details");
            } else {
                logger.warn("Cannot calculate route - missing coordinates or locations");
            }
            
            return savedRoute;
        } catch (Exception e) {
            logger.error("Error saving and calculating route: {}", e.getMessage(), e);
            // Still return the saved route even if calculation fails
            return routeRepository.save(route);
        }
    }

    /**
     * Calculate route details for an existing route
     */
    public Route calculateRouteDetails(Route route) {
        logger.info("Calculating route details for route ID: {}", route.getId());
        
        try {
            if (routeCalculationService.canCalculateRoute(route)) {
                Route calculatedRoute = routeCalculationService.calculateRoute(route);
                Route savedRoute = routeRepository.save(calculatedRoute);
                logger.info("Successfully calculated route details");
                return savedRoute;
            } else {
                logger.warn("Cannot calculate route - missing coordinates or locations");
                return route;
            }
        } catch (Exception e) {
            logger.error("Error calculating route details: {}", e.getMessage(), e);
            return route;
        }
    }

    /**
     * Calculate quick route details (using Distance Matrix API)
     */
    public Route calculateQuickRouteDetails(Route route) {
        logger.info("Calculating quick route details for route ID: {}", route.getId());
        
        try {
            if (routeCalculationService.canCalculateRoute(route)) {
                Route calculatedRoute = routeCalculationService.calculateQuickRoute(route);
                Route savedRoute = routeRepository.save(calculatedRoute);
                logger.info("Successfully calculated quick route details");
                return savedRoute;
            } else {
                logger.warn("Cannot calculate quick route - missing coordinates or locations");
                return route;
            }
        } catch (Exception e) {
            logger.error("Error calculating quick route details: {}", e.getMessage(), e);
            return route;
        }
    }

    /**
     * Recalculate all routes for a trip day
     */
    public void recalculateRoutesForTripDay(Long tripDayId) {
        logger.info("Recalculating all routes for trip day ID: {}", tripDayId);
        
        List<Route> routes = findByTripDayId(tripDayId);
        for (Route route : routes) {
            try {
                calculateRouteDetails(route);
            } catch (Exception e) {
                logger.error("Error recalculating route ID {}: {}", route.getId(), e.getMessage(), e);
            }
        }
        
        logger.info("Completed recalculating {} routes for trip day", routes.size());
    }

    /**
     * Check if route calculation service is available
     */
    public boolean isRouteCalculationAvailable() {
        return routeCalculationService != null;
    }

    /**
     * Create a route between two specific visits
     */
    public Route createRouteBetweenVisits(Long fromVisitId, Long toVisitId, Long tripDayId) {
        logger.info("Creating route between visits {} and {} for trip day {}", fromVisitId, toVisitId, tripDayId);
        
        try {
            // Fetch the visits
            Optional<Visit> fromVisitOpt = visitService.findById(fromVisitId);
            Optional<Visit> toVisitOpt = visitService.findById(toVisitId);
            Optional<TripDay> tripDayOpt = tripDayRepository.findById(tripDayId);
            
            if (!fromVisitOpt.isPresent()) {
                throw new IllegalArgumentException("From visit not found with id: " + fromVisitId);
            }
            if (!toVisitOpt.isPresent()) {
                throw new IllegalArgumentException("To visit not found with id: " + toVisitId);
            }
            if (!tripDayOpt.isPresent()) {
                throw new IllegalArgumentException("Trip day not found with id: " + tripDayId);
            }
            
            Visit fromVisit = fromVisitOpt.get();
            Visit toVisit = toVisitOpt.get();
            TripDay tripDay = tripDayOpt.get();
            
            // Check if a route already exists between these visits
            Optional<Route> existingRoute = findByFromVisitAndToVisit(fromVisit, toVisit);
            if (existingRoute.isPresent()) {
                logger.info("Route already exists between visits, recalculating it");
                return calculateRouteDetails(existingRoute.get());
            }
            
            // Create new route
            Route route = new Route();
            route.setFromVisit(fromVisit);
            route.setToVisit(toVisit);
            route.setTripDay(tripDay);
            route.setTransportMode(TransportMode.DRIVING); // Default transport mode
            
            // Save and calculate route details
            Route savedRoute = saveAndCalculate(route);
            logger.info("Successfully created route between visits {} and {}", fromVisitId, toVisitId);
            
            return savedRoute;
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid arguments for creating route between visits {} and {}: {}", fromVisitId, toVisitId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error creating route between visits {} and {}: {}", fromVisitId, toVisitId, e.getMessage(), e);
            throw new RuntimeException("Failed to create route between visits: " + e.getMessage(), e);
        }
    }
}
