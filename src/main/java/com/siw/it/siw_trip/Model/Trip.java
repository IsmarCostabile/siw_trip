package com.siw.it.siw_trip.Model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "trips")
public class Trip implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(name = "start_date_time", nullable = false)
    private LocalDateTime startDateTime;

    @Column(name = "end_date_time", nullable = false)
    private LocalDateTime endDateTime;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "trip_participants",
        joinColumns = @JoinColumn(name = "trip_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> participants = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "trip_admins",
        joinColumns = @JoinColumn(name = "trip_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> admins = new HashSet<>();

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("dayNumber ASC")
    private List<TripDay> tripDays = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", referencedColumnName = "id")
    private User createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TripStatus status;

    @Column(name = "estimated_budget")
    private Double estimatedBudget;

    @Column(length = 1000)
    private String notes;

    // Default constructor
    public Trip() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = TripStatus.PLANNING;
    }

    // Constructor
    public Trip(String name, LocalDateTime startDateTime, LocalDateTime endDateTime, User createdBy) {
        this();
        this.name = name;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.createdBy = createdBy;
        // Creator is automatically an admin
        this.admins.add(createdBy);
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
        this.updatedAt = LocalDateTime.now();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(LocalDateTime endDateTime) {
        this.endDateTime = endDateTime;
        this.updatedAt = LocalDateTime.now();
    }

    public Set<User> getParticipants() {
        return participants;
    }

    public void setParticipants(Set<User> participants) {
        this.participants = participants;
        this.updatedAt = LocalDateTime.now();
    }

    public Set<User> getAdmins() {
        return admins;
    }

    public void setAdmins(Set<User> admins) {
        this.admins = admins;
        this.updatedAt = LocalDateTime.now();
    }

    public List<TripDay> getTripDays() {
        return tripDays;
    }

    public void setTripDays(List<TripDay> tripDays) {
        this.tripDays = tripDays;
        this.updatedAt = LocalDateTime.now();
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public TripStatus getStatus() {
        return status;
    }

    public void setStatus(TripStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public Double getEstimatedBudget() {
        return estimatedBudget;
    }

    public void setEstimatedBudget(Double estimatedBudget) {
        this.estimatedBudget = estimatedBudget;
        this.updatedAt = LocalDateTime.now();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
        this.updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public void addParticipant(User user) {
        participants.add(user);
        this.updatedAt = LocalDateTime.now();
    }

    public void removeParticipant(User user) {
        participants.remove(user);
        // If user was an admin, remove from admins as well
        if (admins.contains(user) && !user.equals(createdBy)) {
            admins.remove(user);
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void addAdmin(User user) {
        admins.add(user);
        // Admin should also be a participant
        participants.add(user);
        this.updatedAt = LocalDateTime.now();
    }

    public void removeAdmin(User user) {
        // Cannot remove the creator from admins
        if (!user.equals(createdBy)) {
            admins.remove(user);
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void addTripDay(TripDay tripDay) {
        tripDays.add(tripDay);
        tripDay.setTrip(this);
        if (tripDay.getDayNumber() == null) {
            tripDay.setDayNumber(tripDays.size());
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void removeTripDay(TripDay tripDay) {
        tripDays.remove(tripDay);
        tripDay.setTrip(null);
        // Reorder remaining days
        for (int i = 0; i < tripDays.size(); i++) {
            tripDays.get(i).setDayNumber(i + 1);
        }
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isUserAdmin(User user) {
        return admins.contains(user);
    }

    public boolean isUserParticipant(User user) {
        return participants.contains(user);
    }

    public boolean isUserCreator(User user) {
        return user.equals(createdBy);
    }

    public long getDurationDays() {
        if (startDateTime != null && endDateTime != null) {
            return java.time.Duration.between(startDateTime.toLocalDate().atStartOfDay(), 
                                            endDateTime.toLocalDate().atStartOfDay()).toDays() + 1;
        }
        return 0;
    }

    public int getTotalParticipants() {
        return participants.size();
    }

    public int getTotalAdmins() {
        return admins.size();
    }

    public int getTotalDays() {
        return tripDays.size();
    }

    @Override
    public String toString() {
        return "Trip{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", startDateTime=" + startDateTime +
                ", endDateTime=" + endDateTime +
                ", status=" + status +
                ", participants=" + participants.size() +
                ", tripDays=" + tripDays.size() +
                '}';
    }
}
