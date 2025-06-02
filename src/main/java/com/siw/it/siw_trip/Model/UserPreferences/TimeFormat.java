package com.siw.it.siw_trip.Model.UserPreferences;

public enum TimeFormat {
    HOUR_12("12-hour (AM/PM)", "h:mm a"),
    HOUR_24("24-hour", "HH:mm");

    private final String displayName;
    private final String pattern;

    TimeFormat(String displayName, String pattern) {
        this.displayName = displayName;
        this.pattern = pattern;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPattern() {
        return pattern;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
