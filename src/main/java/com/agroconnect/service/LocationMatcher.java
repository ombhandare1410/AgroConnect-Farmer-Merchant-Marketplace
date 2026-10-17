package com.agroconnect.service;

import static com.agroconnect.service.DistrictProximity.norm;

/** Works out how close two places are. Lower rank = closer = shown first. */
public final class LocationMatcher {

    public static final String[] CODES = {"SAME_VILLAGE", "SAME_TALUKA", "SAME_DISTRICT",
                                          "NEARBY_DISTRICT", "SAME_STATE", "OTHER"};

    private LocationMatcher() { }

    /** pinA / pinB may be null when we don't know the PIN code of a place. */
    public static int rank(String stateA, String districtA, String talukaA, String villageA, String pinA,
                           String stateB, String districtB, String talukaB, String villageB, String pinB) {
        boolean sameDistrict = !norm(districtA).isEmpty() && norm(districtA).equals(norm(districtB));
        boolean samePin = pinA != null && pinB != null && !pinA.isBlank() && pinA.trim().equals(pinB.trim());
        if (samePin || (sameDistrict && !norm(villageA).isEmpty() && norm(villageA).equals(norm(villageB)))) return 0;
        if (sameDistrict && !norm(talukaA).isEmpty() && norm(talukaA).equals(norm(talukaB))) return 1;
        if (sameDistrict) return 2;
        if (DistrictProximity.areNearby(districtA, districtB)) return 3;
        if (!norm(stateA).isEmpty() && norm(stateA).equals(norm(stateB))) return 4;
        return 5;
    }

    public static String code(int rank) { return CODES[rank]; }

    /** Text filter used by the "Location" box: matches village, taluka or district. */
    public static boolean textMatches(String text, String village, String taluka, String district) {
        if (text == null || text.isBlank()) return true;
        String q = norm(text);
        return norm(village).contains(q) || norm(taluka).contains(q) || norm(district).contains(q);
    }
}
