package com.siw.it.siw_trip.Model;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PairTest {

    @Test
    void testPairCreation() {
        Pair<String, Integer> pair = Pair.of("hello", 42);
        assertEquals("hello", pair.getFirst());
        assertEquals(42, pair.getSecond());
    }

    @Test
    void testPairEquality() {
        Pair<String, Integer> pair1 = Pair.of("hello", 42);
        Pair<String, Integer> pair2 = Pair.of("hello", 42);
        Pair<String, Integer> pair3 = Pair.of("world", 42);

        assertEquals(pair1, pair2);
        assertNotEquals(pair1, pair3);
        assertEquals(pair1.hashCode(), pair2.hashCode());
    }

    @Test
    void testPairAsMapKey() {
        Map<Pair<String, Integer>, String> map = new HashMap<>();
        
        Pair<String, Integer> key1 = Pair.of("hello", 42);
        Pair<String, Integer> key2 = Pair.of("hello", 42); // Same content as key1
        Pair<String, Integer> key3 = Pair.of("world", 43);

        map.put(key1, "value1");
        map.put(key3, "value3");

        // Should be able to retrieve using a different but equal pair
        assertEquals("value1", map.get(key2));
        assertEquals("value3", map.get(key3));
        assertEquals(2, map.size());
    }

    @Test
    void testPairWithNullValues() {
        Pair<String, Integer> pair1 = Pair.of(null, null);
        Pair<String, Integer> pair2 = Pair.of(null, null);
        Pair<String, Integer> pair3 = Pair.of("hello", null);

        assertEquals(pair1, pair2);
        assertNotEquals(pair1, pair3);
        assertEquals(pair1.hashCode(), pair2.hashCode());
    }

    @Test
    void testPairToString() {
        Pair<String, Integer> pair = Pair.of("hello", 42);
        assertEquals("Pair{first=hello, second=42}", pair.toString());
    }

    @Test
    void testPairWithVisitObjects() {
        Visit visit1 = new Visit();
        visit1.setId(1L);
        Visit visit2 = new Visit();
        visit2.setId(2L);

        Pair<Visit, Visit> pair = Pair.of(visit1, visit2);
        
        assertEquals(visit1, pair.getFirst());
        assertEquals(visit2, pair.getSecond());
        
        // Test with map
        Map<Pair<Visit, Visit>, String> routeMap = new HashMap<>();
        routeMap.put(pair, "route description");
        
        Pair<Visit, Visit> sameContentPair = Pair.of(visit1, visit2);
        assertEquals("route description", routeMap.get(sameContentPair));
    }
}
