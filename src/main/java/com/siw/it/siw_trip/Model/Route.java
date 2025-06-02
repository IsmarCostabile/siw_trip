package com.siw.it.siw_trip.Model;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "routes")
public class Route implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_visit_id", referencedColumnName = "id", nullable = false)
    private Visit fromVisit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_visit_id", referencedColumnName = "id", nullable = false)
    private Visit toVisit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_day_id", referencedColumnName = "id", nullable = false)
    private TripDay tripDay;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Column(name = "distance_km")
    private Double distanceKm;

    @Column(length = 2000)
    private String instructions;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_mode")
    private TransportMode transportMode;

    @Column(length = 1000)
    private String notes;

    @Column(name = "leave_at_preference", nullable = false)
    private Boolean leaveAtPreference = true; // true for "leave at", false for "arrive by"

    // Constants
    private static final String UNKNOWN_LOCATION = "Unknown";

    // Default constructor
    public Route() {}

    // Constructor
    public Route(Visit fromVisit, Visit toVisit, TripDay tripDay) {
        this.fromVisit = fromVisit;
        this.toVisit = toVisit;
        this.tripDay = tripDay;
        this.leaveAtPreference = true; // Default to "leave at"
        this.transportMode = TransportMode.DRIVING; // Default transport mode
    }

    // Constructor with details
    public Route(Visit fromVisit, Visit toVisit, TripDay tripDay, String instructions, Integer estimatedDurationMinutes) {
        this.fromVisit = fromVisit;
        this.toVisit = toVisit;
        this.tripDay = tripDay;
        this.instructions = instructions;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.leaveAtPreference = true; // Default to "leave at"
        this.transportMode = TransportMode.DRIVING; // Default transport mode
    }

    // Constructor with time preference
    public Route(Visit fromVisit, Visit toVisit, TripDay tripDay, Boolean leaveAtPreference) {
        this.fromVisit = fromVisit;
        this.toVisit = toVisit;
        this.tripDay = tripDay;
        this.leaveAtPreference = leaveAtPreference;
        this.transportMode = TransportMode.DRIVING; // Default transport mode
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Visit getFromVisit() {
        return fromVisit;
    }

    public void setFromVisit(Visit fromVisit) {
        this.fromVisit = fromVisit;
    }

    public Visit getToVisit() {
        return toVisit;
    }

    public void setToVisit(Visit toVisit) {
        this.toVisit = toVisit;
    }

    public TripDay getTripDay() {
        return tripDay;
    }

    public void setTripDay(TripDay tripDay) {
        this.tripDay = tripDay;
    }

    public Integer getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public TransportMode getTransportMode() {
        return transportMode;
    }

    public void setTransportMode(TransportMode transportMode) {
        this.transportMode = transportMode;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Boolean getLeaveAtPreference() {
        return leaveAtPreference;
    }

    public void setLeaveAtPreference(Boolean leaveAtPreference) {
        this.leaveAtPreference = leaveAtPreference;
    }

    // Helper methods
    public String getRouteDescription() {
        String from = fromVisit != null && fromVisit.getLocation() != null ? fromVisit.getLocation().getName() : UNKNOWN_LOCATION;
        String to = toVisit != null && toVisit.getLocation() != null ? toVisit.getLocation().getName() : UNKNOWN_LOCATION;
        return from + " → " + to;
    }

    public String getEstimatedDurationFormatted() {
        if (estimatedDurationMinutes == null) {
            return UNKNOWN_LOCATION;
        }
        int hours = estimatedDurationMinutes / 60;
        int minutes = estimatedDurationMinutes % 60;
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }

    public String getTimingPreferenceDescription() {
        return leaveAtPreference != null && leaveAtPreference ? "Leave at" : "Arrive by";
    }

    @Override
    public String toString() {
        return "Route{" +
                "id=" + id +
                ", from=" + (fromVisit != null ? fromVisit.getName() : "null") +
                ", to=" + (toVisit != null ? toVisit.getName() : "null") +
                ", duration=" + estimatedDurationMinutes +
                ", mode=" + transportMode +
                ", timing=" + getTimingPreferenceDescription() +
                '}';
    }
}
