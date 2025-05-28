package com.siw.it.siw_trip.Model;

public enum TransportMode {
    WALKING("Walking"),
    DRIVING("Driving"),
    PUBLIC_TRANSPORT("Public Transport"),
    CYCLING("Cycling"),
    TAXI("Taxi"),
    OTHER("Other");

    private final String displayName;

    TransportMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
