package com.siw.it.siw_trip.Model.UserPreferences;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

import com.siw.it.siw_trip.Model.User;

@Entity
@Table(name = "user_preferences")
public class UserPreferences implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, unique = true)
    @NotNull
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Language language = Language.ENGLISH;

    @Enumerated(EnumType.STRING)
    @Column(name = "distance_unit", nullable = false)
    private DistanceUnit distanceUnit = DistanceUnit.KILOMETERS;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_format", nullable = false)
    private TimeFormat timeFormat = TimeFormat.HOUR_24;

    @Enumerated(EnumType.STRING)
    @Column(name = "temperature_unit", nullable = false)
    private TemperatureUnit temperatureUnit = TemperatureUnit.CELSIUS;

    @Column(name = "date_format")
    private String dateFormat = "yyyy-MM-dd";

    @Column(name = "currency", length = 3)
    private String currency = "EUR";

    @Column(name = "notifications_enabled")
    private Boolean notificationsEnabled = true;

    @Column(name = "email_notifications")
    private Boolean emailNotifications = true;

    // Default constructor
    public UserPreferences() {}

    // Constructor with user
    public UserPreferences(User user) {
        this.user = user;
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

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public DistanceUnit getDistanceUnit() {
        return distanceUnit;
    }

    public void setDistanceUnit(DistanceUnit distanceUnit) {
        this.distanceUnit = distanceUnit;
    }

    public TimeFormat getTimeFormat() {
        return timeFormat;
    }

    public void setTimeFormat(TimeFormat timeFormat) {
        this.timeFormat = timeFormat;
    }

    public TemperatureUnit getTemperatureUnit() {
        return temperatureUnit;
    }

    public void setTemperatureUnit(TemperatureUnit temperatureUnit) {
        this.temperatureUnit = temperatureUnit;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Boolean getNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(Boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public Boolean getEmailNotifications() {
        return emailNotifications;
    }

    public void setEmailNotifications(Boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }

    @Override
    public String toString() {
        return "UserPreferences{" +
                "id=" + id +
                ", language=" + language +
                ", distanceUnit=" + distanceUnit +
                ", timeFormat=" + timeFormat +
                ", temperatureUnit=" + temperatureUnit +
                ", dateFormat='" + dateFormat + '\'' +
                ", currency='" + currency + '\'' +
                '}';
    }
}
