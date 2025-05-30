package com.siw.it.siw_trip.Service;

import com.siw.it.siw_trip.Model.Trip;
import com.siw.it.siw_trip.Model.TripDay;
import com.siw.it.siw_trip.Model.User;
import com.siw.it.siw_trip.Repository.TripDayRepository;
import com.siw.it.siw_trip.Model.TripStatus;
import com.siw.it.siw_trip.Repository.TripRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class TripService {

    private final TripDayRepository tripDayRepository;

    @Autowired
    private TripRepository tripRepository;

    TripService(TripDayRepository tripDayRepository) {
        this.tripDayRepository = tripDayRepository;
    }

    public List<Trip> findAll() {
        return tripRepository.findAll();
    }

    public Optional<Trip> findById(Long id) {
        return tripRepository.findById(id);
    }

    /**
     * Find trip by ID and throw exception if not found
     * This method simplifies controller code by handling Optional extraction
     */
    public Trip findByIdOrThrow(Long id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found with id: " + id));
    }

    public Trip save(Trip trip) {
        boolean isNewTrip = trip.getId() == null;
        Trip savedTrip = tripRepository.save(trip);
        
        // Automatically generate trip days for new trips that have start and end dates
        if (isNewTrip && savedTrip.getStartDateTime() != null && savedTrip.getEndDateTime() != null) {
            // Clear any existing trip days and generate new ones
            savedTrip.getTripDays().clear();
            
            LocalDate startDate = savedTrip.getStartDateTime().toLocalDate();
            LocalDate endDate = savedTrip.getEndDateTime().toLocalDate();
            
            int dayNumber = 1;
            LocalDate currentDate = startDate;
            
            while (!currentDate.isAfter(endDate)) {
                TripDay tripDay = new TripDay(currentDate, dayNumber, savedTrip);
                savedTrip.addTripDay(tripDay);
                
                currentDate = currentDate.plusDays(1);
                dayNumber++;
            }
            
            // Save again with the trip days
            savedTrip = tripRepository.save(savedTrip);
        }
        
        return savedTrip;
    }

    public void deleteById(Long id) {
        tripRepository.deleteById(id);
    }

    public List<Trip> findByStatus(TripStatus status) {
        return tripRepository.findByStatus(status);
    }

    public List<Trip> findByNameContaining(String name) {
        return tripRepository.findByNameContainingIgnoreCase(name);
    }

    public List<Trip> findByParticipantId(Long userId) {
        return tripRepository.findByParticipantId(userId);
    }

    public List<Trip> findByAdminId(Long userId) {
        return tripRepository.findByAdminId(userId);
    }

    public List<Trip> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return tripRepository.findByDateRange(startDate, endDate);
    }

    public List<Trip> findUpcomingTrips() {
        return tripRepository.findUpcomingTrips(LocalDateTime.now());
    }

    public List<Trip> findPastTrips() {
        return tripRepository.findPastTrips(LocalDateTime.now());
    }

    public List<Trip> findTripsForUser(User user) {
        return tripRepository.findTripsForUser(user);
    }

    public List<Trip> findTripsForUserByStatus(User user, TripStatus status) {
        return tripRepository.findTripsForUserByStatus(user, status);
    }

    @Transactional
    public Trip addParticipantToTrip(Long tripId, User participant) {
        Optional<Trip> tripOpt = findById(tripId);
        if (tripOpt.isPresent()) {
            Trip trip = tripOpt.get();
            trip.addParticipant(participant);
            return save(trip);
        }
        throw new RuntimeException("Trip not found with id: " + tripId);
    }

    @Transactional
    public Trip removeParticipantFromTrip(Long tripId, User participant) {
        Optional<Trip> tripOpt = findById(tripId);
        if (tripOpt.isPresent()) {
            Trip trip = tripOpt.get();
            trip.removeParticipant(participant);
            return save(trip);
        }
        throw new RuntimeException("Trip not found with id: " + tripId);
    }

    @Transactional
    public Trip addAdminToTrip(Long tripId, User admin) {
        Optional<Trip> tripOpt = findById(tripId);
        if (tripOpt.isPresent()) {
            Trip trip = tripOpt.get();
            trip.addAdmin(admin);
            return save(trip);
        }
        throw new RuntimeException("Trip not found with id: " + tripId);
    }

    @Transactional
    public Trip removeAdminFromTrip(Long tripId, User admin) {
        Optional<Trip> tripOpt = findById(tripId);
        if (tripOpt.isPresent()) {
            Trip trip = tripOpt.get();
            trip.removeAdmin(admin);
            return save(trip);
        }
        throw new RuntimeException("Trip not found with id: " + tripId);
    }

    public boolean existsById(Long id) {
        return tripRepository.existsById(id);
    }

    public boolean isUserAuthorizedForTrip(Long tripId, User user) {
        Optional<Trip> tripOpt = findById(tripId);
        if (tripOpt.isPresent()) {
            Trip trip = tripOpt.get();
            return trip.isUserParticipant(user) || trip.isUserAdmin(user);
        }
        return false;
    }

    public boolean isUserAdminForTrip(Long tripId, User user) {
        Optional<Trip> tripOpt = findById(tripId);
        if (tripOpt.isPresent()) {
            Trip trip = tripOpt.get();
            return trip.isUserAdmin(user);
        }
        return false;
    }

    // Additional methods needed by TripController
    @Autowired
    private com.siw.it.siw_trip.Service.UserService userService;

    public Trip addParticipant(Long tripId, Long userId) {
        Optional<User> userOpt = userService.findById(userId);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("User not found with id: " + userId);
        }
        return addParticipantToTrip(tripId, userOpt.get());
    }

    public Trip removeParticipant(Long tripId, Long userId) {
        Optional<User> userOpt = userService.findById(userId);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("User not found with id: " + userId);
        }
        return removeParticipantFromTrip(tripId, userOpt.get());
    }

    public Trip addAdmin(Long tripId, Long userId) {
        Optional<User> userOpt = userService.findById(userId);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("User not found with id: " + userId);
        }
        return addAdminToTrip(tripId, userOpt.get());
    }

    public Trip removeAdmin(Long tripId, Long userId) {
        Optional<User> userOpt = userService.findById(userId);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("User not found with id: " + userId);
        }
        return removeAdminFromTrip(tripId, userOpt.get());
    }

    public Trip addTripDay(Long tripId, TripDay tripDay) {
        Optional<Trip> tripOpt = findById(tripId);
        if (!tripOpt.isPresent()) {
            throw new IllegalArgumentException("Trip not found with id: " + tripId);
        }

        Trip trip = tripOpt.get();
        tripDay.setTrip(trip);
        tripDay.setDayNumber(trip.getTripDays().size() + 1);
        trip.addTripDay(tripDay);

        return save(trip);
    }

    public Trip removeTripDay(Long tripId, Long tripDayId) {
        Optional<Trip> tripOpt = findById(tripId);
        if (!tripOpt.isPresent()) {
            throw new IllegalArgumentException("Trip not found with id: " + tripId);
        }

        Trip trip = tripOpt.get();
        trip.removeTripDay(tripDayRepository.findById(tripDayId)
                .orElseThrow(() -> new IllegalArgumentException("TripDay not found with id: " + tripDayId)));

        return save(trip);
    }

    public Trip updateStatus(Long tripId, TripStatus status) {
        Optional<Trip> tripOpt = findById(tripId);
        if (!tripOpt.isPresent()) {
            throw new IllegalArgumentException("Trip not found with id: " + tripId);
        }

        Trip trip = tripOpt.get();
        trip.setStatus(status);

        return save(trip);
    }

    /**
     * Automatically generate trip days for a trip based on its start and end dates
     * This method creates a TripDay entity for each day between start and end dates (inclusive)
     */
    @Transactional
    public Trip generateTripDays(Trip trip) {
        if (trip.getStartDateTime() == null || trip.getEndDateTime() == null) {
            throw new IllegalArgumentException("Trip must have start and end dates to generate trip days");
        }

        // Clear existing trip days if any
        trip.getTripDays().clear();

        LocalDate startDate = trip.getStartDateTime().toLocalDate();
        LocalDate endDate = trip.getEndDateTime().toLocalDate();
        
        int dayNumber = 1;
        LocalDate currentDate = startDate;
        
        while (!currentDate.isAfter(endDate)) {
            TripDay tripDay = new TripDay(currentDate, dayNumber, trip);
            trip.addTripDay(tripDay);
            
            currentDate = currentDate.plusDays(1);
            dayNumber++;
        }

        return save(trip);
    }
}
