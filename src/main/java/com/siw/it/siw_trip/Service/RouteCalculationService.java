package com.siw.it.siw_trip.Service;

import com.siw.it.siw_trip.Model.Route;
import com.siw.it.siw_trip.Model.Visit;
import com.siw.it.siw_trip.Model.Location;
import com.siw.it.siw_trip.Model.TransportMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class RouteCalculationService {

    private static final Logger logger = LoggerFactory.getLogger(RouteCalculationService.class);
    
    // Google Maps API URLs
    private static final String DIRECTIONS_API_URL = "https://maps.googleapis.com/maps/api/directions/json";
    private static final String DISTANCE_MATRIX_API_URL = "https://maps.googleapis.com/maps/api/distancematrix/json";
    
    // Constants for repeated literals
    private static final String STATUS_KEY = "status";
    private static final String VALUE_KEY = "value";
    private static final String OK_STATUS = "OK";
    private static final String DRIVING_MODE = "driving";
    
    @Value("${google.maps.api.key}")
    private String googleMapsApiKey;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public RouteCalculationService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Calculate route between two visits using Google Maps API
     * @param route The route object containing fromVisit, toVisit, transportMode, and timing preferences
     * @return Updated route with calculated data
     */
    public Route calculateRoute(Route route) {
        try {
            logger.info("Starting route calculation for route from {} to {}", 
                route.getFromVisit().getName(), route.getToVisit().getName());
                
            if (route.getFromVisit() == null || route.getToVisit() == null) {
                throw new IllegalArgumentException("Route must have both fromVisit and toVisit");
            }
            
            Location fromLocation = route.getFromVisit().getLocation();
            Location toLocation = route.getToVisit().getLocation();
            
            if (fromLocation == null || toLocation == null) {
                throw new IllegalArgumentException("Both visits must have associated locations");
            }
            
            if (!fromLocation.hasCoordinates() || !toLocation.hasCoordinates()) {
                throw new IllegalArgumentException("Both locations must have valid coordinates");
            }

            // Check if locations are the same
            if (isSameLocation(fromLocation, toLocation)) {
                logger.info("From and to locations are the same, setting zero-distance route");
                setZeroDistanceRoute(route);
                return route;
            }

            logger.info("All validation passed - calling Google Maps Directions API");
            
            // Call Google Maps Directions API
            String response = callDirectionsAPI(route);
            
            logger.info("Received response from Google Maps API - parsing...");
            
            // Parse the response and update the route
            parseDirectionsResponse(response, route);
            
            logger.info("Successfully calculated route from {} to {}", 
                fromLocation.getName(), toLocation.getName());
            
            return route;
            
        } catch (Exception e) {
            logger.error("Error calculating route: {}", e.getMessage(), e);
            // Set default values in case of error
            setDefaultRouteValues(route);
            return route;
        }
    }
    
    /**
     * Calculate route using Distance Matrix API for quick distance/duration estimates
     */
    public Route calculateQuickRoute(Route route) {
        try {
            if (route.getFromVisit() == null || route.getToVisit() == null) {
                throw new IllegalArgumentException("Route must have both fromVisit and toVisit");
            }
            
            Location fromLocation = route.getFromVisit().getLocation();
            Location toLocation = route.getToVisit().getLocation();
            
            if (fromLocation == null || toLocation == null) {
                throw new IllegalArgumentException("Both visits must have associated locations");
            }
            
            if (!fromLocation.hasCoordinates() || !toLocation.hasCoordinates()) {
                throw new IllegalArgumentException("Both locations must have valid coordinates");
            }
            
            // Call Google Maps Distance Matrix API
            String response = callDistanceMatrixAPI(route);
            
            // Parse the response and update the route
            parseDistanceMatrixResponse(response, route);
            
            logger.info("Successfully calculated quick route from {} to {}", 
                fromLocation.getName(), toLocation.getName());
            
            return route;
            
        } catch (Exception e) {
            logger.error("Error calculating quick route: {}", e.getMessage(), e);
            // Set default values in case of error
            setDefaultRouteValues(route);
            return route;
        }
    }
    
    private String callDirectionsAPI(Route route) {
        Location fromLocation = route.getFromVisit().getLocation();
        Location toLocation = route.getToVisit().getLocation();
        
        String origin = fromLocation.getLatitude() + "," + fromLocation.getLongitude();
        String destination = toLocation.getLatitude() + "," + toLocation.getLongitude();
        
        // Check if origin and destination are the same
        if (origin.equals(destination)) {
            logger.warn("Origin and destination are identical: {}. This might cause parsing issues.", origin);
        }
        
        // Build URL with proper parameter handling
        StringBuilder urlBuilder = new StringBuilder(DIRECTIONS_API_URL);
        urlBuilder.append("?origin=").append(origin);
        urlBuilder.append("&destination=").append(destination);
        urlBuilder.append("&mode=").append(mapTransportModeToGoogleMaps(route.getTransportMode()));
        urlBuilder.append("&key=").append(googleMapsApiKey);
        
        // Add departure or arrival time based on user preference
        addTimingParameters(urlBuilder, route);
        
        String url = urlBuilder.toString();
        
        logger.debug("Calling Google Maps Directions API: {}", url);
        
        String response = restTemplate.getForObject(url, String.class);
        logger.info("Google Maps API response received, length: {} characters", 
            response != null ? response.length() : 0);
        
        return response;
    }
    
    private String callDistanceMatrixAPI(Route route) {
        Location fromLocation = route.getFromVisit().getLocation();
        Location toLocation = route.getToVisit().getLocation();
        
        String origins = fromLocation.getLatitude() + "," + fromLocation.getLongitude();
        String destinations = toLocation.getLatitude() + "," + toLocation.getLongitude();
        
        // Build URL with proper parameter handling
        StringBuilder urlBuilder = new StringBuilder(DISTANCE_MATRIX_API_URL);
        urlBuilder.append("?origins=").append(origins);
        urlBuilder.append("&destinations=").append(destinations);
        urlBuilder.append("&mode=").append(mapTransportModeToGoogleMaps(route.getTransportMode()));
        urlBuilder.append("&key=").append(googleMapsApiKey);
        
        // Add departure or arrival time based on user preference
        addTimingParameters(urlBuilder, route);
        
        String url = urlBuilder.toString();
        
        logger.debug("Calling Google Maps Distance Matrix API: {}", url);
        
        return restTemplate.getForObject(url, String.class);
    }
    
    private void addTimingParameters(StringBuilder urlBuilder, Route route) {
        // Add timing parameters based on user preference and visit times
        if (route.getLeaveAtPreference() != null && route.getLeaveAtPreference()) {
            // User prefers "leave at" - use departure time
            LocalDateTime departureTime = getDepartureTime(route);
            if (departureTime != null) {
                long timestamp = departureTime.toEpochSecond(ZoneOffset.UTC);
                urlBuilder.append("&departure_time=").append(timestamp);
                logger.debug("Using departure time: {}", departureTime);
            }
        } else {
            // User prefers "arrive by" - use arrival time
            LocalDateTime arrivalTime = getArrivalTime(route);
            if (arrivalTime != null) {
                long timestamp = arrivalTime.toEpochSecond(ZoneOffset.UTC);
                urlBuilder.append("&arrival_time=").append(timestamp);
                logger.debug("Using arrival time: {}", arrivalTime);
            }
        }
    }
    
    private LocalDateTime getDepartureTime(Route route) {
        // Try to get departure time from fromVisit end time, or use current time
        Visit fromVisit = route.getFromVisit();
        if (fromVisit.getEndTime() != null) {
            return fromVisit.getEndTime();
        } else if (fromVisit.getStartTime() != null) {
            // Add estimated duration if available
            if (fromVisit.getEstimatedDurationMinutes() != null) {
                return fromVisit.getStartTime().plusMinutes(fromVisit.getEstimatedDurationMinutes());
            }
            return fromVisit.getStartTime();
        }
        return LocalDateTime.now();
    }
    
    private LocalDateTime getArrivalTime(Route route) {
        // Try to get arrival time from toVisit start time
        Visit toVisit = route.getToVisit();
        if (toVisit.getStartTime() != null) {
            return toVisit.getStartTime();
        }
        return null; // Let Google Maps decide
    }
    
    private String mapTransportModeToGoogleMaps(TransportMode transportMode) {
        if (transportMode == null) {
            return "walking"; // Default
        }
        
        switch (transportMode) {
            case WALKING:
                return "walking";
            case DRIVING, TAXI:
                return DRIVING_MODE;
            case PUBLIC_TRANSPORT:
                return "transit";
            case CYCLING:
                return "bicycling";
            case OTHER:
            default:
                return DRIVING_MODE;
        }
    }
    
    private void parseDirectionsResponse(String jsonResponse, Route route) {
        try {
            logger.debug("Parsing Google Maps response: {}", jsonResponse);
            JsonNode root = objectMapper.readTree(jsonResponse);
            
            if (!isValidApiResponse(root)) {
                logger.warn("Invalid API response, setting default values");
                setDefaultRouteValues(route);
                return;
            }
            
            JsonNode routes = root.path("routes");
            logger.debug("Found {} routes in response", routes.size());
            
            if (routes.isArray() && routes.size() > 0) {
                JsonNode firstRoute = routes.get(0);
                JsonNode legs = firstRoute.path("legs");
                logger.debug("Found {} legs in first route", legs.size());
                
                if (legs.isArray() && legs.size() > 0) {
                    JsonNode leg = legs.get(0);
                    logger.debug("Extracting data from first leg");
                    extractRouteData(leg, route);
                    
                    // Log the extracted values
                    logger.info("Extracted route data - Duration: {} minutes, Distance: {} km, Instructions: {}", 
                        route.getEstimatedDurationMinutes(), route.getDistanceKm(), 
                        route.getInstructions() != null ? route.getInstructions().substring(0, Math.min(100, route.getInstructions().length())) + "..." : "null");
                } else {
                    logger.warn("No legs found in route, setting default values");
                    setDefaultRouteValues(route);
                }
            } else {
                logger.warn("No routes found in response, setting default values");
                setDefaultRouteValues(route);
            }
            
        } catch (Exception e) {
            logger.error("Error parsing Google Maps Directions response: {}", e.getMessage(), e);
            setDefaultRouteValues(route);
        }
    }
    
    private boolean isValidApiResponse(JsonNode root) {
        String status = root.path(STATUS_KEY).asText();
        logger.debug("Google Maps API response status: {}", status);
        
        if (!OK_STATUS.equals(status)) {
            logger.warn("Google Maps API returned status: {} for route calculation", status);
            
            // Log error message if available
            String errorMessage = root.path("error_message").asText();
            if (!errorMessage.isEmpty()) {
                logger.warn("Google Maps API error message: {}", errorMessage);
            }
            
            return false;
        }
        return true;
    }
    
    private void extractRouteData(JsonNode leg, Route route) {
        extractDuration(leg, route);
        extractDistance(leg, route);
        extractInstructions(leg, route);
    }
    
    private void extractDuration(JsonNode leg, Route route) {
        JsonNode duration = leg.path("duration");
        logger.debug("Duration node: {}", duration);
        
        if (!duration.isMissingNode()) {
            int durationSeconds = duration.path(VALUE_KEY).asInt();
            int durationMinutes = durationSeconds / 60;
            route.setEstimatedDurationMinutes(durationMinutes);
            logger.debug("Extracted duration: {} seconds ({} minutes)", durationSeconds, durationMinutes);
        } else {
            logger.warn("Duration node is missing from API response");
        }
    }
    
    private void extractDistance(JsonNode leg, Route route) {
        JsonNode distance = leg.path("distance");
        logger.debug("Distance node: {}", distance);
        
        if (!distance.isMissingNode()) {
            int distanceMeters = distance.path(VALUE_KEY).asInt();
            double distanceKm = distanceMeters / 1000.0;
            route.setDistanceKm(distanceKm);
            logger.debug("Extracted distance: {} meters ({} km)", distanceMeters, distanceKm);
        } else {
            logger.warn("Distance node is missing from API response");
        }
    }
    
    private void extractInstructions(JsonNode leg, Route route) {
        StringBuilder instructions = new StringBuilder();
        JsonNode steps = leg.path("steps");
        logger.debug("Steps node: {}, is array: {}, size: {}", steps, steps.isArray(), steps.size());
        
        if (steps.isArray()) {
            for (JsonNode step : steps) {
                String instruction = step.path("html_instructions").asText();
                logger.debug("Raw instruction: {}", instruction);
                
                if (!instruction.isEmpty()) {
                    // Remove HTML tags for storage
                    instruction = instruction.replaceAll("<[^>]*>", "");
                    instructions.append(instruction).append(". ");
                }
            }
        } else {
            logger.warn("Steps node is not an array or is missing");
        }
        
        if (instructions.length() > 0) {
            String instructionText = limitInstructionLength(instructions.toString().trim());
            route.setInstructions(instructionText);
            logger.debug("Final instructions length: {} characters", instructionText.length());
        } else {
            logger.warn("No instructions extracted from API response");
        }
    }
    
    private String limitInstructionLength(String instructions) {
        // Limit instructions length to fit database constraint
        if (instructions.length() > 2000) {
            return instructions.substring(0, 1997) + "...";
        }
        return instructions;
    }
    
    private void parseDistanceMatrixResponse(String jsonResponse, Route route) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            
            if (!isValidApiResponse(root)) {
                setDefaultRouteValues(route);
                return;
            }
            
            JsonNode rows = root.path("rows");
            if (rows.isArray() && rows.size() > 0) {
                JsonNode elements = rows.get(0).path("elements");
                
                if (elements.isArray() && elements.size() > 0) {
                    JsonNode element = elements.get(0);
                    processDistanceMatrixElement(element, route);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error parsing Google Maps Distance Matrix response: {}", e.getMessage(), e);
            setDefaultRouteValues(route);
        }
    }
    
    private void processDistanceMatrixElement(JsonNode element, Route route) {
        // Check element status
        String elementStatus = element.path(STATUS_KEY).asText();
        if (!OK_STATUS.equals(elementStatus)) {
            logger.warn("Google Maps API element status: {} for distance matrix calculation", elementStatus);
            setDefaultRouteValues(route);
            return;
        }
        
        extractDuration(element, route);
        extractDistance(element, route);
        
        // Distance Matrix API doesn't provide detailed instructions
        route.setInstructions("Route calculated using Google Maps");
    }
    
    private void setDefaultRouteValues(Route route) {
        // Set reasonable default values when API call fails
        if (route.getEstimatedDurationMinutes() == null) {
            route.setEstimatedDurationMinutes(30); // 30 minutes default
        }
        if (route.getDistanceKm() == null) {
            route.setDistanceKm(5.0); // 5 km default
        }
        if (route.getInstructions() == null || route.getInstructions().isEmpty()) {
            route.setInstructions("Route calculation unavailable. Please check manually.");
        }
    }
    
    /**
     * Validate that a route can be calculated (locations have coordinates)
     */
    public boolean canCalculateRoute(Route route) {
        logger.info("Checking if route can be calculated...");
        
        if (route == null) {
            logger.warn("Route is null");
            return false;
        }
        
        if (route.getFromVisit() == null) {
            logger.warn("From visit is null");
            return false;
        }
        
        if (route.getToVisit() == null) {
            logger.warn("To visit is null");
            return false;
        }
        
        Location fromLocation = route.getFromVisit().getLocation();
        Location toLocation = route.getToVisit().getLocation();
        
        if (fromLocation == null) {
            logger.warn("From location is null for visit: {}", route.getFromVisit().getName());
            return false;
        }
        
        if (toLocation == null) {
            logger.warn("To location is null for visit: {}", route.getToVisit().getName());
            return false;
        }
        
        boolean fromHasCoordinates = fromLocation.hasCoordinates();
        boolean toHasCoordinates = toLocation.hasCoordinates();
        
        logger.info("From location '{}' has coordinates: {} (lat: {}, lng: {})", 
            fromLocation.getName(), fromHasCoordinates, 
            fromLocation.getLatitude(), fromLocation.getLongitude());
        logger.info("To location '{}' has coordinates: {} (lat: {}, lng: {})", 
            toLocation.getName(), toHasCoordinates, 
            toLocation.getLatitude(), toLocation.getLongitude());
        
        boolean canCalculate = fromHasCoordinates && toHasCoordinates;
        logger.info("Route calculation possible: {}", canCalculate);
        
        return canCalculate;
    }
    
    /**
     * Check if Google Maps API is available
     */
    public boolean isGoogleMapsApiAvailable() {
        return googleMapsApiKey != null && !googleMapsApiKey.trim().isEmpty();
    }
    
    /**
     * Check if two locations are the same (within a small tolerance)
     */
    private boolean isSameLocation(Location loc1, Location loc2) {
        double tolerance = 0.0001; // About 11 meters
        
        if (loc1.getLatitude() == null || loc1.getLongitude() == null ||
            loc2.getLatitude() == null || loc2.getLongitude() == null) {
            return false;
        }
        
        double latDiff = Math.abs(loc1.getLatitude() - loc2.getLatitude());
        double lngDiff = Math.abs(loc1.getLongitude() - loc2.getLongitude());
        
        return latDiff < tolerance && lngDiff < tolerance;
    }
    
    /**
     * Set route values for zero-distance routes (same location)
     */
    private void setZeroDistanceRoute(Route route) {
        route.setEstimatedDurationMinutes(0);
        route.setDistanceKm(0.0);
        route.setInstructions("You are already at your destination.");
    }
}
