package com.siw.it.siw_trip.Model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateLocationFromPlaceRequest {
    @NotBlank(message = "Place ID is required")
    private String placeId;
    
    @NotNull(message = "Trip ID is required")
    private Long tripId;
    
    @NotNull(message = "Trip Day ID is required")
    private Long tripDayId;
    
    private String description; // Optional location description
    
    // Constructors
    public CreateLocationFromPlaceRequest() {}
    
    public CreateLocationFromPlaceRequest(String placeId, Long tripId, Long tripDayId) {
        this.placeId = placeId;
        this.tripId = tripId;
        this.tripDayId = tripDayId;
    }
    
    // Getters and Setters
    public String getPlaceId() { return placeId; }
    public void setPlaceId(String placeId) { this.placeId = placeId; }
    
    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }
    
    public Long getTripDayId() { return tripDayId; }
    public void setTripDayId(Long tripDayId) { this.tripDayId = tripDayId; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
