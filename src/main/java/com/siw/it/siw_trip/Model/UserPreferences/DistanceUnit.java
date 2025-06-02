package com.siw.it.siw_trip.Model.UserPreferences;

public enum DistanceUnit {
    KILOMETERS("Kilometers", "km"),
    MILES("Miles", "mi");

    private final String displayName;
    private final String abbreviation;

    DistanceUnit(String displayName, String abbreviation) {
        this.displayName = displayName;
        this.abbreviation = abbreviation;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
