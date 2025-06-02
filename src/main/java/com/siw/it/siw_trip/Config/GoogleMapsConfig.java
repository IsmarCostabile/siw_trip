package com.siw.it.siw_trip.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class GoogleMapsConfig implements WebMvcConfigurer {

    @Value("${google.maps.api.key:}")
    private String googleMapsApiKey;

    public String getGoogleMapsApiKey() {
        return googleMapsApiKey;
    }
}
