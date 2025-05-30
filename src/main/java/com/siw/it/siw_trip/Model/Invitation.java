package com.siw.it.siw_trip.Model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "invitations")
public class Invitation implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user; // The user being invited

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", referencedColumnName = "id", nullable = false)
    private Trip trip; // The trip from which the invitation came

    @Column(length = 1000)
    private String message; // Invitation message

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by_user_id", referencedColumnName = "id")
    private User invitedBy; // Who sent the invitation

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "is_admin_invitation", nullable = false)
    private boolean isAdminInvitation = false;

    // Default constructor
    public Invitation() {
        this.createdAt = LocalDateTime.now();
    }

    // Constructor
    public Invitation(User user, Trip trip, String message, User invitedBy) {
        this();
        this.user = user;
        this.trip = trip;
        this.message = message;
        this.invitedBy = invitedBy;
    }

    // Constructor with admin invitation flag
    public Invitation(User user, Trip trip, String message, User invitedBy, boolean isAdminInvitation) {
        this(user, trip, message, invitedBy);
        this.isAdminInvitation = isAdminInvitation;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public User getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(User invitedBy) {
        this.invitedBy = invitedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }

    public boolean isAdminInvitation() {
        return isAdminInvitation;
    }

    public void setAdminInvitation(boolean adminInvitation) {
        this.isAdminInvitation = adminInvitation;
    }

    @Override
    public String toString() {
        return "Invitation{" +
                "id=" + id +
                ", user=" + (user != null ? user.getEmail() : "null") +
                ", trip=" + (trip != null ? trip.getName() : "null") +
                ", createdAt=" + createdAt +
                '}';
    }
}
