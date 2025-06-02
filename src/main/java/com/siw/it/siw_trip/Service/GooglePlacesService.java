package com.siw.it.siw_trip.Service;

import com.siw.it.siw_trip.Model.dto.GooglePlaceDto;
import com.siw.it.siw_trip.Model.dto.PlaceSearchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

@Service
public class GooglePlacesService {
    
    private static final Logger logger = Logger.getLogger(GooglePlacesService.class.getName());
    
    @Value("${google.maps.api.key:}")
    private String apiKey;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String PLACES_API_BASE_URL = "https://maps.googleapis.com/maps/api/place";
    private static final String TEXT_SEARCH_ENDPOINT = "/textsearch/json";
    private static final String DETAILS_ENDPOINT = "/details/json";
    private static final String NEARBY_SEARCH_ENDPOINT = "/nearbysearch/json";
    
    public GooglePlacesService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Search for places using text query
     */
    public List<GooglePlaceDto> searchPlaces(PlaceSearchRequest request) {
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warning("Google Maps API key not configured");
            return new ArrayList<>();
        }
        
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(PLACES_API_BASE_URL + TEXT_SEARCH_ENDPOINT)
                .queryParam("query", request.getQuery())
                .queryParam("key", apiKey);
            
            // Add optional parameters
            if (request.getLanguage() != null) {
                builder.queryParam("language", request.getLanguage());
            }
            
            if (request.getLatitude() != null && request.getLongitude() != null) {
                String location = request.getLatitude() + "," + request.getLongitude();
                builder.queryParam("location", location);
                
                if (request.getRadius() != null) {
                    builder.queryParam("radius", request.getRadius());
                }
            }
            
            if (request.getType() != null) {
                builder.queryParam("type", request.getType());
            }
            
            String url = builder.build().toUriString();
            logger.info("Making request to Google Places API: " + url.replaceAll("key=[^&]*", "key=***"));
            
            String response = restTemplate.getForObject(url, String.class);
            return parseSearchResponse(response);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error searching places: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get detailed information about a specific place
     */
    public GooglePlaceDto getPlaceDetails(String placeId) {
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warning("Google Maps API key not configured");
            return null;
        }
        
        try {
            String url = UriComponentsBuilder
                .fromHttpUrl(PLACES_API_BASE_URL + DETAILS_ENDPOINT)
                .queryParam("place_id", placeId)
                .queryParam("fields", "place_id,name,formatted_address,geometry,photos,rating,types,vicinity,address_components")
                .queryParam("key", apiKey)
                .build()
                .toUriString();
            
            logger.info("Getting place details for: " + placeId);
            
            String response = restTemplate.getForObject(url, String.class);
            return parseDetailsResponse(response);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting place details: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Search for nearby places
     */
    public List<GooglePlaceDto> searchNearbyPlaces(Double latitude, Double longitude, Integer radius, String type) {
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warning("Google Maps API key not configured");
            return new ArrayList<>();
        }
        
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(PLACES_API_BASE_URL + NEARBY_SEARCH_ENDPOINT)
                .queryParam("location", latitude + "," + longitude)
                .queryParam("radius", radius != null ? radius : 5000) // Default 5km
                .queryParam("key", apiKey);
            
            if (type != null) {
                builder.queryParam("type", type);
            }
            
            String url = builder.build().toUriString();
            logger.info("Searching nearby places at: " + latitude + "," + longitude);
            
            String response = restTemplate.getForObject(url, String.class);
            return parseSearchResponse(response);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error searching nearby places: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    private List<GooglePlaceDto> parseSearchResponse(String response) {
        List<GooglePlaceDto> places = new ArrayList<>();
        
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode results = root.get("results");
            
            if (results != null && results.isArray()) {
                for (JsonNode result : results) {
                    GooglePlaceDto place = parsePlace(result);
                    if (place != null) {
                        places.add(place);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error parsing search response: " + e.getMessage(), e);
        }
        
        return places;
    }
    
    private GooglePlaceDto parseDetailsResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode result = root.get("result");
            
            if (result != null) {
                return parsePlace(result);
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error parsing details response: " + e.getMessage(), e);
        }
        
        return null;
    }
    
    private GooglePlaceDto parsePlace(JsonNode placeNode) {
        try {
            GooglePlaceDto place = new GooglePlaceDto();
            
            // Basic information
            if (placeNode.has("place_id")) {
                place.setPlaceId(placeNode.get("place_id").asText());
            }
            
            if (placeNode.has("name")) {
                place.setName(placeNode.get("name").asText());
            }
            
            if (placeNode.has("formatted_address")) {
                place.setFormattedAddress(placeNode.get("formatted_address").asText());
            }
            
            if (placeNode.has("vicinity")) {
                place.setVicinity(placeNode.get("vicinity").asText());
            }
            
            // Geometry (coordinates)
            if (placeNode.has("geometry") && placeNode.get("geometry").has("location")) {
                JsonNode location = placeNode.get("geometry").get("location");
                if (location.has("lat")) {
                    place.setLatitude(location.get("lat").asDouble());
                }
                if (location.has("lng")) {
                    place.setLongitude(location.get("lng").asDouble());
                }
            }
            
            // Rating
            if (placeNode.has("rating")) {
                place.setRating(placeNode.get("rating").asDouble());
            }
            
            // Types
            if (placeNode.has("types") && placeNode.get("types").isArray()) {
                List<String> typesList = new ArrayList<>();
                for (JsonNode typeNode : placeNode.get("types")) {
                    typesList.add(typeNode.asText());
                }
                place.setTypes(typesList.toArray(new String[0]));
            }
            
            // Photo reference
            if (placeNode.has("photos") && placeNode.get("photos").isArray() && 
                placeNode.get("photos").size() > 0) {
                JsonNode firstPhoto = placeNode.get("photos").get(0);
                if (firstPhoto.has("photo_reference")) {
                    place.setPhotoReference(firstPhoto.get("photo_reference").asText());
                }
            }
            
            // Parse address components for city and country
            if (placeNode.has("address_components")) {
                parseAddressComponents(placeNode.get("address_components"), place);
            }
            
            return place;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error parsing place: " + e.getMessage(), e);
            return null;
        }
    }
    
    private void parseAddressComponents(JsonNode addressComponents, GooglePlaceDto place) {
        try {
            for (JsonNode component : addressComponents) {
                if (component.has("types") && component.get("types").isArray()) {
                    boolean isCity = false;
                    boolean isCountry = false;
                    
                    for (JsonNode type : component.get("types")) {
                        String typeStr = type.asText();
                        if ("locality".equals(typeStr) || "administrative_area_level_1".equals(typeStr)) {
                            isCity = true;
                        } else if ("country".equals(typeStr)) {
                            isCountry = true;
                        }
                    }
                    
                    if (isCity && component.has("long_name")) {
                        place.setCity(component.get("long_name").asText());
                    } else if (isCountry && component.has("long_name")) {
                        place.setCountry(component.get("long_name").asText());
                    }
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error parsing address components: " + e.getMessage(), e);
        }
    }
    
    /**
     * Check if Google Places API is configured
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty();
    }
}
