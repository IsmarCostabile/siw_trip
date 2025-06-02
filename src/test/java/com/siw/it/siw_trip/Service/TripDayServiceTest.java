package com.siw.it.siw_trip.Service;

import com.siw.it.siw_trip.Model.*;
import com.siw.it.siw_trip.Repository.TripDayRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripDayServiceTest {

    @Mock
    private TripDayRepository tripDayRepository;

    @Mock
    private RouteService routeService;

    @InjectMocks
    private TripDayService tripDayService;

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
    void testAddVisitToEmptyTripDay() {
        // Setup
        when(tripDayRepository.findById(1L)).thenReturn(Optional.of(tripDay));
        when(tripDayRepository.save(any(TripDay.class))).thenReturn(tripDay);

        // Execute
        tripDayService.addVisit(1L, visitA);

        // Verify
        assertEquals(1, tripDay.getVisits().size());
        assertEquals(visitA, tripDay.getVisits().get(0));
        verify(tripDayRepository).save(tripDay);
        // No routes should be created for a single visit
        verify(routeService, never()).save(any(Route.class));
    }

    @Test
    void testAddSecondVisitCreatesRoute() {
        // Setup - trip day already has visitA
        tripDay.getVisits().add(visitA);
        when(tripDayRepository.findById(1L)).thenReturn(Optional.of(tripDay));
        when(tripDayRepository.save(any(TripDay.class))).thenReturn(tripDay);
        when(routeService.findByFromVisitAndToVisit(visitA, visitB)).thenReturn(Optional.empty());

        // Execute
        tripDayService.addVisit(1L, visitB);

        // Verify
        assertEquals(2, tripDay.getVisits().size());
        verify(routeService).save(argThat(route -> 
            route.getFromVisit().equals(visitA) && 
            route.getToVisit().equals(visitB) &&
            route.getTripDay().equals(tripDay)
        ));
    }

    @Test
    void testAddThirdVisitCreatesAdditionalRoute() {
        // Setup - trip day already has visitA and visitB
        tripDay.getVisits().add(visitA);
        tripDay.getVisits().add(visitB);
        when(tripDayRepository.findById(1L)).thenReturn(Optional.of(tripDay));
        when(tripDayRepository.save(any(TripDay.class))).thenReturn(tripDay);
        when(routeService.findByFromVisitAndToVisit(visitB, visitC)).thenReturn(Optional.empty());

        // Execute
        tripDayService.addVisit(1L, visitC);

        // Verify
        assertEquals(3, tripDay.getVisits().size());
        verify(routeService).save(argThat(route -> 
            route.getFromVisit().equals(visitB) && 
            route.getToVisit().equals(visitC) &&
            route.getTripDay().equals(tripDay)
        ));
    }

    @Test
    void testAddVisitInMiddleCreatesMultipleRoutes() {
        // Setup - trip day has visitA and visitC, inserting visitB in between
        tripDay.getVisits().add(visitA);
        tripDay.getVisits().add(visitC);
        
        when(tripDayRepository.findById(1L)).thenReturn(Optional.of(tripDay));
        when(tripDayRepository.save(any(TripDay.class))).thenReturn(tripDay);
        when(routeService.findByFromVisitAndToVisit(any(), any())).thenReturn(Optional.empty());

        // Execute - when visitB is added, it should be inserted and create routes
        tripDayService.addVisit(1L, visitB);

        // Verify
        assertEquals(3, tripDay.getVisits().size());
        // Should create routes A->B and B->C (assuming proper insertion logic)
        verify(routeService, atLeast(1)).save(any(Route.class));
    }

    @Test
    void testRemoveMiddleVisitReconnectsRoutes() {
        // Setup - trip day has visitA, visitB, visitC
        tripDay.getVisits().add(visitA);
        tripDay.getVisits().add(visitB);
        tripDay.getVisits().add(visitC);
        
        when(tripDayRepository.findById(1L)).thenReturn(Optional.of(tripDay));
        when(tripDayRepository.save(any(TripDay.class))).thenReturn(tripDay);
        when(routeService.findByFromVisitAndToVisit(visitA, visitC)).thenReturn(Optional.empty());

        // Execute
        tripDayService.removeVisit(1L, visitB.getId());

        // Verify
        assertEquals(2, tripDay.getVisits().size());
        assertFalse(tripDay.getVisits().contains(visitB));
        
        // Should delete routes involving visitB
        verify(routeService).deleteByVisit(visitB);
        
        // Should create new route A->C to reconnect
        verify(routeService).save(argThat(route -> 
            route.getFromVisit().equals(visitA) && 
            route.getToVisit().equals(visitC) &&
            route.getTripDay().equals(tripDay)
        ));
    }

    @Test
    void testRemoveFirstVisitCleansUpRoutes() {
        // Setup - trip day has visitA, visitB, visitC
        tripDay.getVisits().add(visitA);
        tripDay.getVisits().add(visitB);
        tripDay.getVisits().add(visitC);
        
        when(tripDayRepository.findById(1L)).thenReturn(Optional.of(tripDay));
        when(tripDayRepository.save(any(TripDay.class))).thenReturn(tripDay);
        when(routeService.findByFromVisitAndToVisit(visitB, visitC)).thenReturn(Optional.empty());

        // Execute
        tripDayService.removeVisit(1L, visitA.getId());

        // Verify
        assertEquals(2, tripDay.getVisits().size());
        assertFalse(tripDay.getVisits().contains(visitA));
        
        // Should delete routes involving visitA
        verify(routeService).deleteByVisit(visitA);
        
        // Should create route B->C to connect remaining visits
        verify(routeService).save(argThat(route -> 
            route.getFromVisit().equals(visitB) && 
            route.getToVisit().equals(visitC) &&
            route.getTripDay().equals(tripDay)
        ));
    }

    @Test
    void testRemoveLastVisitCleansUpRoutes() {
        // Setup - trip day has visitA, visitB, visitC
        tripDay.getVisits().add(visitA);
        tripDay.getVisits().add(visitB);
        tripDay.getVisits().add(visitC);
        
        when(tripDayRepository.findById(1L)).thenReturn(Optional.of(tripDay));
        when(tripDayRepository.save(any(TripDay.class))).thenReturn(tripDay);
        when(routeService.findByFromVisitAndToVisit(visitA, visitB)).thenReturn(Optional.empty());

        // Execute
        tripDayService.removeVisit(1L, visitC.getId());

        // Verify
        assertEquals(2, tripDay.getVisits().size());
        assertFalse(tripDay.getVisits().contains(visitC));
        
        // Should delete routes involving visitC
        verify(routeService).deleteByVisit(visitC);
        
        // Should create route A->B to connect remaining visits
        verify(routeService).save(argThat(route -> 
            route.getFromVisit().equals(visitA) && 
            route.getToVisit().equals(visitB) &&
            route.getTripDay().equals(tripDay)
        ));
    }

    @Test
    void testDoesNotCreateDuplicateRoutes() {
        // Setup - trip day already has visitA
        tripDay.getVisits().add(visitA);
        Route existingRoute = new Route(visitA, visitB, tripDay);
        
        when(tripDayRepository.findById(1L)).thenReturn(Optional.of(tripDay));
        when(tripDayRepository.save(any(TripDay.class))).thenReturn(tripDay);
        when(routeService.findByFromVisitAndToVisit(visitA, visitB)).thenReturn(Optional.of(existingRoute));

        // Execute
        tripDayService.addVisit(1L, visitB);

        // Verify
        assertEquals(2, tripDay.getVisits().size());
        // Should not create a new route since one already exists
        verify(routeService, never()).save(any(Route.class));
    }
}
