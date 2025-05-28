package com.siw.it.siw_trip.Model;

public enum UserRole {
    VISITOR,    // Can only view books, authors, and reviews
    USER,       // Can view and add reviews (max one per book)
    ADMIN       // Can manage books, authors, and delete reviews
}

