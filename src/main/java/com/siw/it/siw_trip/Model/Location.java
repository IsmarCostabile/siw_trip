package com.siw.it.siw_trip.Model;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "locations")
public class Location implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    @Column
    private String country;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(length = 1000)
    private String description;

    // Default constructor
    public Location() {}

    // Constructor
    public Location(String name, String address, String city) {
        this.name = name;
        this.address = address;
        this.city = city;
    }

    // Constructor with coordinates
    public Location(String name, String address, String city, String country, Double latitude, Double longitude) {
        this.name = name;
        this.address = address;
        this.city = city;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Helper method to get full address
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(address);
        if (city != null && !city.isEmpty()) {
            sb.append(", ").append(city);
        }
        if (country != null && !country.isEmpty()) {
            sb.append(", ").append(country);
        }
        return sb.toString();
    }

    // Helper method to check if coordinates are available
    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }

    @Override
    public String toString() {
        return "Location{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", city='" + city + '\'' +
                ", country='" + country + '\'' +
                '}';
    }
}
