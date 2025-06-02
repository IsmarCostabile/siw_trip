package com.siw.it.siw_trip.Model.dto;

public class PlaceSearchRequest {
    private String query;
    private Double latitude;
    private Double longitude;
    private Integer radius; // in meters
    private String type; // e.g., "tourist_attraction", "restaurant", etc.
    private String language; // e.g., "en", "it"
    
    // Constructors
    public PlaceSearchRequest() {}
    
    public PlaceSearchRequest(String query) {
        this.query = query;
    }
    
    public PlaceSearchRequest(String query, Double latitude, Double longitude, Integer radius) {
        this.query = query;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
    }
    
    // Getters and Setters
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    
    public Integer getRadius() { return radius; }
    public void setRadius(Integer radius) { this.radius = radius; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
