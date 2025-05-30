package com.siw.it.siw_trip.Repository;

import com.siw.it.siw_trip.Model.Invitation;
import com.siw.it.siw_trip.Model.Trip;
import com.siw.it.siw_trip.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    // Find all invitations for a specific user
    List<Invitation> findByUser(User user);

    // Find all invitations for a specific trip
    List<Invitation> findByTrip(Trip trip);

    // Check if a user has already been invited to a trip
    Optional<Invitation> findByUserAndTrip(User user, Trip trip);

    // Find invitations sent by a specific user
    List<Invitation> findByInvitedBy(User invitedBy);

    // Count invitations for a user
    @Query("SELECT COUNT(i) FROM Invitation i WHERE i.user = :user")
    long countInvitationsForUser(@Param("user") User user);

    // Find all invitations for trips where the user is an admin
    @Query("SELECT i FROM Invitation i WHERE :user MEMBER OF i.trip.admins")
    List<Invitation> findInvitationsForTripsWhereUserIsAdmin(@Param("user") User user);
}
