package com.agroconnect.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/** Pure logic tests for the location ranking used in discovery (lower rank = closer = shown first). */
class LocationMatcherTest {

    // rank(stateA, districtA, talukaA, villageA, pinA,   stateB, districtB, talukaB, villageB, pinB)
    private int rank(String d1, String t1, String v1, String pin1, String d2, String t2, String v2, String pin2) {
        return LocationMatcher.rank("Maharashtra", d1, t1, v1, pin1, "Maharashtra", d2, t2, v2, pin2);
    }

    @Test
    void sameVillageInSameDistrictIsClosest() {
        assertThat(rank("Nashik", "Niphad", "Pimpalgaon", null, "nashik", "NIPHAD", " pimpalgaon ", null)).isEqualTo(0);
    }

    @Test
    void samePinCodeCountsAsSamePlace() {
        assertThat(rank("Nashik", "Niphad", "A", "422001", "Nashik", "Sinnar", "B", "422001")).isEqualTo(0);
    }

    @Test
    void sameVillageNameInDifferentDistrictIsNotClose() {
        assertThat(rank("Nashik", "X", "Rampur", null, "Pune", "Y", "Rampur", null)).isGreaterThan(2);
    }

    @Test
    void sameTalukaThenSameDistrict() {
        assertThat(rank("Nashik", "Niphad", "A", null, "Nashik", "Niphad", "B", null)).isEqualTo(1);
        assertThat(rank("Nashik", "Niphad", "A", null, "Nashik", "Sinnar", "B", null)).isEqualTo(2);
    }

    @Test
    void nearbyDistrictThenSameStateThenOther() {
        assertThat(rank("Nashik", null, "A", null, "Dhule", null, "B", null)).isEqualTo(3);
        assertThat(rank("Nashik", null, "A", null, "Nagpur", null, "B", null)).isEqualTo(4);
        assertThat(LocationMatcher.rank("Maharashtra", "Nashik", null, "A", null,
                "Gujarat", "Surat", null, "B", null)).isEqualTo(5);
    }

    @Test
    void nearbyDistrictsWorkBothWays() {
        assertThat(DistrictProximity.areNearby("Pune", "Satara")).isTrue();
        assertThat(DistrictProximity.areNearby("Satara", "Pune")).isTrue();
        assertThat(DistrictProximity.areNearby("Nashik", "Kolhapur")).isFalse();
    }

    @Test
    void locationTextFilterMatchesVillageTalukaOrDistrict() {
        assertThat(LocationMatcher.textMatches("nash", "Pimpalgaon", "Niphad", "Nashik")).isTrue();
        assertThat(LocationMatcher.textMatches("sinn", "Pimpalgaon", "Sinnar", "Nashik")).isTrue();
        assertThat(LocationMatcher.textMatches("pune", "Pimpalgaon", "Niphad", "Nashik")).isFalse();
        assertThat(LocationMatcher.textMatches("", "A", "B", "C")).isTrue();
    }
}
