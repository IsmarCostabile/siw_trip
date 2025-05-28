package com.siw.it.siw_trip.Model;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "users")
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Credentials credentials;

    // Default constructor
    public User() {}

    // Constructor
    public User(String email, String firstName, String lastName, UserRole role) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    // Helper methods to check roles
    public boolean isAdmin() {
        return this.role == UserRole.ADMIN;
    }

    public boolean isRegisteredUser() {
        return this.role == UserRole.USER;
    }

    public boolean isVisitor() {
        return this.role == UserRole.VISITOR;
    }

    // Helper method to get username from credentials
    public String getUsername() {
        return this.credentials != null ? this.credentials.getUsername() : null;
    }

    // Helper method to get full name
    public String getFullName() {
        return this.firstName + " " + this.lastName;
    }
}
