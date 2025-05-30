package com.siw.it.siw_trip.Service;

import com.siw.it.siw_trip.Model.Route;
import com.siw.it.siw_trip.Model.Visit;
import com.siw.it.siw_trip.Model.TransportMode;
import com.siw.it.siw_trip.Repository.RouteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RouteService {

    @Autowired
    private RouteRepository routeRepository;

    public List<Route> findAll() {
        return routeRepository.findAll();
    }

    public Optional<Route> findById(Long id) {
        return routeRepository.findById(id);
    }

    public Route save(Route route) {
        return routeRepository.save(route);
    }

    public void deleteById(Long id) {
        routeRepository.deleteById(id);
    }

    public Optional<Route> findByFromVisitAndToVisit(Visit fromVisit, Visit toVisit) {
        return routeRepository.findByFromVisitAndToVisit(fromVisit, toVisit);
    }

    public List<Route> findByFromVisit(Visit fromVisit) {
        return routeRepository.findByFromVisit(fromVisit);
    }

    public List<Route> findByToVisit(Visit toVisit) {
        return routeRepository.findByToVisit(toVisit);
    }

    public List<Route> findByTransportMode(TransportMode transportMode) {
        return routeRepository.findByTransportMode(transportMode);
    }

    public List<Route> findByVisitId(Long visitId) {
        return routeRepository.findByVisitId(visitId);
    }

    public List<Route> findByTripId(Long tripId) {
        return routeRepository.findByTripId(tripId);
    }

    public List<Route> findByEstimatedDurationGreaterThan(Integer duration) {
        return routeRepository.findByEstimatedDurationGreaterThan(duration);
    }

    public boolean existsById(Long id) {
        return routeRepository.existsById(id);
    }
}
