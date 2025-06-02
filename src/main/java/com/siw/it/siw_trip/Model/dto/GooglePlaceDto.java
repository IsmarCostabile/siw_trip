package com.siw.it.siw_trip.Model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class GooglePlaceDto {
    @JsonProperty("place_id")
    private String placeId;
    
    private String name;
    private String formattedAddress;
    private Double latitude;
    private Double longitude;
    private String city;
    private String country;
    private String photoReference;
    private Double rating;
    private String[] types;
    private String vicinity;
    
    // Additional Google Places API fields
    @JsonProperty("price_level")
    private Integer priceLevel;
    
    @JsonProperty("user_ratings_total")
    private Integer userRatingsTotal;
    
    @JsonProperty("business_status")
    private String businessStatus;
    
    private String website;
    
    @JsonProperty("formatted_phone_number")
    private String formattedPhoneNumber;
    
    @JsonProperty("international_phone_number")
    private String internationalPhoneNumber;
    
    @JsonProperty("opening_hours")
    private OpeningHours openingHours;
    
    @JsonProperty("editorial_summary")
    private EditorialSummary editorialSummary;
    
    // Service boolean flags
    @JsonProperty("delivery")
    private Boolean delivery;
    
    @JsonProperty("dine_in")
    private Boolean dineIn;
    
    @JsonProperty("takeout")
    private Boolean takeout;
    
    @JsonProperty("reservable")
    private Boolean reservable;
    
    @JsonProperty("serves_beer")
    private Boolean servesBeer;
    
    @JsonProperty("serves_wine")
    private Boolean servesWine;
    
    @JsonProperty("wheelchair_accessible_entrance")
    private Boolean wheelchairAccessibleEntrance;
    
    // Nested classes for complex fields
    public static class OpeningHours {
        @JsonProperty("open_now")
        private Boolean openNow;
        
        @JsonProperty("weekday_text")
        private List<String> weekdayText;
        
        // Getters and setters
        public Boolean getOpenNow() { return openNow; }
        public void setOpenNow(Boolean openNow) { this.openNow = openNow; }
        
        public List<String> getWeekdayText() { return weekdayText; }
        public void setWeekdayText(List<String> weekdayText) { this.weekdayText = weekdayText; }
    }
    
    public static class EditorialSummary {
        private String overview;
        
        public String getOverview() { return overview; }
        public void setOverview(String overview) { this.overview = overview; }
    }
    
    // Constructors
    public GooglePlaceDto() {}
    
    public GooglePlaceDto(String placeId, String name, String formattedAddress, 
                         Double latitude, Double longitude) {
        this.placeId = placeId;
        this.name = name;
        this.formattedAddress = formattedAddress;
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    // Getters and Setters
    public String getPlaceId() { return placeId; }
    public void setPlaceId(String placeId) { this.placeId = placeId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getFormattedAddress() { return formattedAddress; }
    public void setFormattedAddress(String formattedAddress) { this.formattedAddress = formattedAddress; }
    
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    
    public String getPhotoReference() { return photoReference; }
    public void setPhotoReference(String photoReference) { this.photoReference = photoReference; }
    
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    
    public String[] getTypes() { return types; }
    public void setTypes(String[] types) { this.types = types; }
    
    public String getVicinity() { return vicinity; }
    public void setVicinity(String vicinity) { this.vicinity = vicinity; }
    
    // Getters and setters for new fields
    public Integer getPriceLevel() { return priceLevel; }
    public void setPriceLevel(Integer priceLevel) { this.priceLevel = priceLevel; }
    
    public Integer getUserRatingsTotal() { return userRatingsTotal; }
    public void setUserRatingsTotal(Integer userRatingsTotal) { this.userRatingsTotal = userRatingsTotal; }
    
    public String getBusinessStatus() { return businessStatus; }
    public void setBusinessStatus(String businessStatus) { this.businessStatus = businessStatus; }
    
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    
    public String getFormattedPhoneNumber() { return formattedPhoneNumber; }
    public void setFormattedPhoneNumber(String formattedPhoneNumber) { this.formattedPhoneNumber = formattedPhoneNumber; }
    
    public String getInternationalPhoneNumber() { return internationalPhoneNumber; }
    public void setInternationalPhoneNumber(String internationalPhoneNumber) { this.internationalPhoneNumber = internationalPhoneNumber; }
    
    public OpeningHours getOpeningHours() { return openingHours; }
    public void setOpeningHours(OpeningHours openingHours) { this.openingHours = openingHours; }
    
    public EditorialSummary getEditorialSummary() { return editorialSummary; }
    public void setEditorialSummary(EditorialSummary editorialSummary) { this.editorialSummary = editorialSummary; }
    
    public Boolean getDelivery() { return delivery; }
    public void setDelivery(Boolean delivery) { this.delivery = delivery; }
    
    public Boolean getDineIn() { return dineIn; }
    public void setDineIn(Boolean dineIn) { this.dineIn = dineIn; }
    
    public Boolean getTakeout() { return takeout; }
    public void setTakeout(Boolean takeout) { this.takeout = takeout; }
    
    public Boolean getReservable() { return reservable; }
    public void setReservable(Boolean reservable) { this.reservable = reservable; }
    
    public Boolean getServesBeer() { return servesBeer; }
    public void setServesBeer(Boolean servesBeer) { this.servesBeer = servesBeer; }
    
    public Boolean getServesWine() { return servesWine; }
    public void setServesWine(Boolean servesWine) { this.servesWine = servesWine; }
    
    public Boolean getWheelchairAccessibleEntrance() { return wheelchairAccessibleEntrance; }
    public void setWheelchairAccessibleEntrance(Boolean wheelchairAccessibleEntrance) { this.wheelchairAccessibleEntrance = wheelchairAccessibleEntrance; }
    
    // Helper method to get price level as dollar signs
    public String getPriceLevelDisplay() {
        if (priceLevel == null) return null;
        
        int level = priceLevel;
        if (level < 0) level = 0;
        if (level > 4) level = 4;
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < level; i++) {
            result.append("$");
        }
        return result.toString();
    }
    
    /**
     * Convert this DTO to a Location entity
     */
    public com.siw.it.siw_trip.Model.Location toLocation() {
        com.siw.it.siw_trip.Model.Location location = new com.siw.it.siw_trip.Model.Location();
        location.setName(this.name);
        location.setAddress(this.formattedAddress);
        location.setCity(this.city);
        location.setCountry(this.country);
        location.setLatitude(this.latitude);
        location.setLongitude(this.longitude);
        return location;
    }
}
