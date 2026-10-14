package com.pitremake.spawn;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PitSpawnTest {

    @Test
    void parsesRecordedSpawnPoints() {
        Location point = PitSpawn.parse(null, "11.5,114.6,-11.25,-135");
        assertEquals(11.5, point.getX());
        assertEquals(114.6, point.getY(), 1e-9);
        assertEquals(-11.25, point.getZ());
        assertEquals(-135f, point.getYaw());
        assertEquals(0f, PitSpawn.parse(null, "1,2,3").getYaw());
        assertNull(PitSpawn.parse(null, "1,2"));
        assertNull(PitSpawn.parse(null, "a,b,c"));
    }

    @Test
    void formatRoundTrips() {
        Location point = PitSpawn.parse(null, PitSpawn.format(new Location(null, 9.5, 114, 0.5, 90, 10)));
        assertEquals(9.5, point.getX());
        assertEquals(114, point.getY());
        assertEquals(90f, point.getYaw());
    }

    @Test
    void yawSnapsTo45Degrees() {
        assertEquals(90f, PitSpawn.snapYaw(87.3f));
        assertEquals(135f, PitSpawn.snapYaw(150f));
        assertEquals(-135f, PitSpawn.snapYaw(-130f));
        assertEquals(-135f, PitSpawn.snapYaw(230f));
        assertEquals(180f, PitSpawn.snapYaw(-179f));
        assertEquals(0f, PitSpawn.snapYaw(-361f));
    }

    @Test
    void protectionIsACylinderAboveMinY() {
        // スポーンの床 (Y=114) は攻撃不可、下の戦う場所 (Y=82) は攻撃できる
        assertTrue(PitSpawn.isInside(9.5, 114, 0.5, 0.5, 0.5, 25, 105));
        assertFalse(PitSpawn.isInside(9.5, 82, 0.5, 0.5, 0.5, 25, 105));
        assertFalse(PitSpawn.isInside(30, 114, 0.5, 0.5, 0.5, 25, 105));
    }
}
