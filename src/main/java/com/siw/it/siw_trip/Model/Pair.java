package com.siw.it.siw_trip.Model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Generic Pair class that can hold two variables of any type.
 * This class is useful for creating key-value pairs or for grouping two related objects.
 * 
 * @param <T> Type of the first element (must be Serializable)
 * @param <U> Type of the second element (must be Serializable)
 */
public class Pair<T extends Serializable, U extends Serializable> implements Serializable {
    private static final long serialVersionUID = 1L;

    private T first;
    private U second;

    // Default constructor
    public Pair() {}

    // Constructor
    public Pair(T first, U second) {
        this.first = first;
        this.second = second;
    }

    // Getters and Setters
    public T getFirst() {
        return first;
    }

    public void setFirst(T first) {
        this.first = first;
    }

    public U getSecond() {
        return second;
    }

    public void setSecond(U second) {
        this.second = second;
    }

    // Static factory method for convenience
    public static <T extends Serializable, U extends Serializable> Pair<T, U> of(T first, U second) {
        return new Pair<>(first, second);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Pair<?, ?> pair = (Pair<?, ?>) obj;
        return Objects.equals(first, pair.first) && Objects.equals(second, pair.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    @Override
    public String toString() {
        return "Pair{" +
                "first=" + first +
                ", second=" + second +
                '}';
    }
}
