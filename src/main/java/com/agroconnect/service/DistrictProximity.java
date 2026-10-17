package com.agroconnect.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A small hand-made list of neighbouring Maharashtra districts (no maps API needed).
 * It is a starter list: add more pairs in the static block to improve "nearby" matching.
 * Districts not listed simply fall back to "same state".
 */
public final class DistrictProximity {

    private static final Map<String, Set<String>> NEIGHBOURS = new HashMap<>();

    static {
        link("nashik", "dhule", "jalgaon", "ahmednagar", "thane", "palghar", "aurangabad");
        link("pune", "ahmednagar", "solapur", "satara", "raigad", "thane");
        link("ahmednagar", "solapur", "beed", "aurangabad", "thane");
        link("solapur", "satara", "sangli", "osmanabad");
        link("satara", "sangli", "kolhapur", "ratnagiri", "raigad");
        link("sangli", "kolhapur", "ratnagiri");
        link("kolhapur", "ratnagiri", "sindhudurg");
        link("ratnagiri", "raigad", "sindhudurg");
        link("jalgaon", "dhule", "aurangabad", "buldhana");
        link("dhule", "nandurbar");
        link("thane", "palghar", "raigad");
    }

    private DistrictProximity() { }

    private static void link(String district, String... others) {
        for (String o : others) {
            NEIGHBOURS.computeIfAbsent(district, k -> new HashSet<>()).add(o);
            NEIGHBOURS.computeIfAbsent(o, k -> new HashSet<>()).add(district);   // works both ways
        }
    }

    public static boolean areNearby(String a, String b) {
        return NEIGHBOURS.getOrDefault(norm(a), Set.of()).contains(norm(b));
    }

    static String norm(String s) { return s == null ? "" : s.trim().toLowerCase(); }
}
