package com.siw.it.siw_trip.Model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trip_days")
public class TripDay implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "day_number")
    private Integer dayNumber;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", referencedColumnName = "id")
    private Trip trip;

    @OneToMany(mappedBy = "tripDay", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("startTime ASC")
    private List<Visit> visits = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_visit_id", referencedColumnName = "id")
    private Visit destination;

    @Column(length = 1000)
    private String notes;

    // Default constructor
    public TripDay() {}

    // Constructor
    public TripDay(LocalDate date, Trip trip) {
        this.date = date;
        this.trip = trip;
    }

    // Constructor with day number
    public TripDay(LocalDate date, Integer dayNumber, Trip trip) {
        this.date = date;
        this.dayNumber = dayNumber;
        this.trip = trip;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(Integer dayNumber) {
        this.dayNumber = dayNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public List<Visit> getVisits() {
        return visits;
    }

    public void setVisits(List<Visit> visits) {
        this.visits = visits;
    }

    public Visit getDestination() {
        return destination;
    }

    public void setDestination(Visit destination) {
        this.destination = destination;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // Helper methods
    public void addVisit(Visit visit) {
        visits.add(visit);
        visit.setTripDay(this);
        // Visits will be ordered by start time automatically
    }

    public void removeVisit(Visit visit) {
        visits.remove(visit);
        visit.setTripDay(null);
        // No need to reorder since ordering is by start time
    }

    public String getDestinationName() {
        return destination != null && destination.getLocation() != null ? 
               destination.getLocation().getName() : "No destination set";
    }

    public String getDayTitle() {
        StringBuilder title = new StringBuilder();
        if (dayNumber != null) {
            title.append("Day ").append(dayNumber);
        }
        if (destination != null && destination.getLocation() != null) {
            if (title.length() > 0) {
                title.append(" - ");
            }
            title.append(destination.getLocation().getName());
        }
        return title.length() > 0 ? title.toString() : "Trip Day";
    }

    public int getTotalVisits() {
        return visits.size();
    }

    @Override
    public String toString() {
        return "TripDay{" +
                "id=" + id +
                ", date=" + date +
                ", dayNumber=" + dayNumber +
                ", destination=" + getDestinationName() +
                ", visits=" + visits.size() +
                '}';
    }
}
