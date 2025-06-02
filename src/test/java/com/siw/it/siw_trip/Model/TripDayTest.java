package com.siw.it.siw_trip.Model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class TripDayTest {

    private TripDay tripDay;
    private Visit visitA;
    private Visit visitB;
    private Visit visitC;

    @BeforeEach
    void setUp() {
        tripDay = new TripDay();
        tripDay.setId(1L);
        tripDay.setDate(LocalDate.now());
        tripDay.setVisits(new ArrayList<>());
        tripDay.setRoutes(new ArrayList<>());

        visitA = new Visit();
        visitA.setId(1L);
        visitA.setName("Visit A");

        visitB = new Visit();
        visitB.setId(2L);
        visitB.setName("Visit B");

        visitC = new Visit();
        visitC.setId(3L);
        visitC.setName("Visit C");
    }

    @Test
    void testAddRoute() {
        Route route = new Route(visitA, visitB, tripDay);
        
        tripDay.addRoute(route);
        
        assertEquals(1, tripDay.getRoutes().size());
        assertTrue(tripDay.hasRoute(visitA, visitB));
        assertEquals(route, tripDay.getRoute(visitA, visitB));
    }

    @Test
    void testRemoveRoute() {
        Route route = new Route(visitA, visitB, tripDay);
        tripDay.addRoute(route);
        
        tripDay.removeRoute(route);
        
        assertEquals(0, tripDay.getRoutes().size());
        assertFalse(tripDay.hasRoute(visitA, visitB));
        assertNull(tripDay.getRoute(visitA, visitB));
    }

    @Test
    void testRoutesMapFunctionality() {
        Route routeAB = new Route(visitA, visitB, tripDay);
        Route routeBC = new Route(visitB, visitC, tripDay);
        
        tripDay.addRoute(routeAB);
        tripDay.addRoute(routeBC);
        
        // Test retrieval using Pair keys
        assertEquals(routeAB, tripDay.getRoute(visitA, visitB));
        assertEquals(routeBC, tripDay.getRoute(visitB, visitC));
        assertNull(tripDay.getRoute(visitA, visitC));
        
        // Test existence checks
        assertTrue(tripDay.hasRoute(visitA, visitB));
        assertTrue(tripDay.hasRoute(visitB, visitC));
        assertFalse(tripDay.hasRoute(visitA, visitC));
    }

    @Test
    void testMultipleRoutesWithSameVisits() {
        // This shouldn't happen in normal usage, but let's test the behavior
        Route route1 = new Route(visitA, visitB, tripDay);
        Route route2 = new Route(visitA, visitB, tripDay);
        route2.setId(2L); // Different route ID
        
        tripDay.addRoute(route1);
        tripDay.addRoute(route2);
        
        // Should only keep the last one in the map
        assertEquals(2, tripDay.getRoutes().size());
        assertEquals(route2, tripDay.getRoute(visitA, visitB));
    }

    @Test
    void testRoutesMapConsistencyAfterRemoval() {
        Route routeAB = new Route(visitA, visitB, tripDay);
        Route routeBC = new Route(visitB, visitC, tripDay);
        
        tripDay.addRoute(routeAB);
        tripDay.addRoute(routeBC);
        
        // Remove one route
        tripDay.removeRoute(routeAB);
        
        // Verify only the remaining route is accessible
        assertFalse(tripDay.hasRoute(visitA, visitB));
        assertTrue(tripDay.hasRoute(visitB, visitC));
        assertEquals(routeBC, tripDay.getRoute(visitB, visitC));
        assertEquals(1, tripDay.getRoutes().size());
    }

    @Test
    void testEmptyTripDayRoutes() {
        assertNull(tripDay.getRoute(visitA, visitB));
        assertFalse(tripDay.hasRoute(visitA, visitB));
        assertEquals(0, tripDay.getRoutes().size());
    }
}
