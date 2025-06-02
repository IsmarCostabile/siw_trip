package com.siw.it.siw_trip.Model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @JoinColumn(name = "destination_id", referencedColumnName = "id")
    private Location destination;

    @Column(length = 1000)
    private String notes;

    @OneToMany(mappedBy = "tripDay", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Route> routes = new ArrayList<>();

    // Map to store routes by pair of visits for quick lookup
    @Transient
    private Map<Pair<Visit, Visit>, Route> routesMap = new HashMap<>();

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

    public Location getDestination() {
        return destination;
    }

    public void setDestination(Location destination) {
        this.destination = destination;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<Route> getRoutes() {
        return routes;
    }

    public void setRoutes(List<Route> routes) {
        this.routes = routes;
        // Rebuild the routes map when routes are set
        rebuildRoutesMap();
    }

    public Map<Pair<Visit, Visit>, Route> getRoutesMap() {
        if (routesMap.isEmpty() && !routes.isEmpty()) {
            rebuildRoutesMap();
        }
        return routesMap;
    }

    // Helper methods
    public void addVisit(Visit visit) {
        visits.add(visit);
        visit.setTripDay(this);
        // Visits will be ordered by start time automatically
        
        // Note: Route creation is now handled by TripDayService to ensure proper calculation
    }
    
    /**
     * Creates routes when a new visit is added to maintain connectivity
     * If visit B is added between A and C, creates routes A→B and B→C
     */
    private void createRoutesForNewVisit(Visit newVisit) {
        // Sort visits by start time to determine order
        List<Visit> sortedVisits = new ArrayList<>(visits);
        sortedVisits.sort((v1, v2) -> {
            if (v1.getStartTime() == null && v2.getStartTime() == null) return 0;
            if (v1.getStartTime() == null) return 1;
            if (v2.getStartTime() == null) return -1;
            return v1.getStartTime().compareTo(v2.getStartTime());
        });
        
        int newVisitIndex = sortedVisits.indexOf(newVisit);
        
        // Create route from previous visit to new visit (A→B)
        if (newVisitIndex > 0) {
            Visit previousVisit = sortedVisits.get(newVisitIndex - 1);
            Route routeFromPrevious = new Route(previousVisit, newVisit, this);
            addRoute(routeFromPrevious);
        }
        
        // Create route from new visit to next visit (B→C)
        if (newVisitIndex < sortedVisits.size() - 1) {
            Visit nextVisit = sortedVisits.get(newVisitIndex + 1);
            Route routeToNext = new Route(newVisit, nextVisit, this);
            addRoute(routeToNext);
        }
        
        // If there was a direct route between previous and next visit, remove it
        // because now we have previous→new→next instead of previous→next
        if (newVisitIndex > 0 && newVisitIndex < sortedVisits.size() - 1) {
            Visit previousVisit = sortedVisits.get(newVisitIndex - 1);
            Visit nextVisit = sortedVisits.get(newVisitIndex + 1);
            removeRoute(previousVisit, nextVisit);
        }
    }

    public void removeVisit(Visit visit) {
        visits.remove(visit);
        visit.setTripDay(null);
        
        // Remove all routes that involve this visit
        removeRoutesForVisit(visit);
        
        // Reconnect remaining visits if needed
        reconnectAfterVisitRemoval();
    }
    
    /**
     * Removes all routes that involve the specified visit
     */
    private void removeRoutesForVisit(Visit visit) {
        List<Route> routesToRemove = new ArrayList<>();
        for (Route route : routes) {
            if (route.getFromVisit().equals(visit) || route.getToVisit().equals(visit)) {
                routesToRemove.add(route);
            }
        }
        
        for (Route route : routesToRemove) {
            removeRoute(route);
        }
    }
    
    /**
     * Reconnects visits after a visit is removed
     * If B is removed from A→B→C, creates route A→C
     */
    private void reconnectAfterVisitRemoval() {
        // Sort remaining visits by start time
        List<Visit> sortedVisits = new ArrayList<>(visits);
        sortedVisits.sort((v1, v2) -> {
            if (v1.getStartTime() == null && v2.getStartTime() == null) return 0;
            if (v1.getStartTime() == null) return 1;
            if (v2.getStartTime() == null) return -1;
            return v1.getStartTime().compareTo(v2.getStartTime());
        });
        
        // Find visits that were connected through the removed visit
        // and create a direct connection between them
        for (int i = 0; i < sortedVisits.size() - 1; i++) {
            Visit currentVisit = sortedVisits.get(i);
            Visit nextVisit = sortedVisits.get(i + 1);
            
            // Check if there's no direct route between consecutive visits
            if (getRoute(currentVisit, nextVisit) == null) {
                Route reconnectionRoute = new Route(currentVisit, nextVisit, this);
                addRoute(reconnectionRoute);
            }
        }
    }
    
    /**
     * Removes a route between two specific visits
     */
    public void removeRoute(Visit fromVisit, Visit toVisit) {
        Pair<Visit, Visit> routeKey = Pair.of(fromVisit, toVisit);
        Route route = routesMap.get(routeKey);
        if (route != null) {
            removeRoute(route);
        }
    }

    // Route management methods
    public void addRoute(Route route) {
        routes.add(route);
        route.setTripDay(this);
        // Update the routes map
        if (route.getFromVisit() != null && route.getToVisit() != null) {
            routesMap.put(Pair.of(route.getFromVisit(), route.getToVisit()), route);
        }
    }

    public void removeRoute(Route route) {
        routes.remove(route);
        route.setTripDay(null);
        // Remove from the routes map
        if (route.getFromVisit() != null && route.getToVisit() != null) {
            routesMap.remove(Pair.of(route.getFromVisit(), route.getToVisit()));
        }
    }

    public Route getRoute(Visit fromVisit, Visit toVisit) {
        // Ensure routes map is initialized
        if (routesMap.isEmpty() && !routes.isEmpty()) {
            rebuildRoutesMap();
        }
        return routesMap.get(Pair.of(fromVisit, toVisit));
    }

    public boolean hasRoute(Visit fromVisit, Visit toVisit) {
        // Ensure routes map is initialized
        if (routesMap.isEmpty() && !routes.isEmpty()) {
            rebuildRoutesMap();
        }
        return routesMap.containsKey(Pair.of(fromVisit, toVisit));
    }

    private void rebuildRoutesMap() {
        routesMap.clear();
        for (Route route : routes) {
            if (route.getFromVisit() != null && route.getToVisit() != null) {
                routesMap.put(Pair.of(route.getFromVisit(), route.getToVisit()), route);
            }
        }
    }

    public String getDestinationName() {
        return destination != null ? 
               destination.getName() : "No destination set";
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
