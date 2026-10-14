package com.pitremake.command;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PitNpcCommandTest {

    @Test
    void npcStandsInBlockCenterFacingThePlayer() {
        Location at = PitNpcCommand.placement(new Location(null, 12.3, 55.0, -1.8, 0, 30));
        assertEquals(12.5, at.getX());
        assertEquals(55.0, at.getY());
        assertEquals(-1.5, at.getZ());
        assertEquals(0f, at.getPitch());
        // 南 (0°) を向いているプレイヤー → NPC は北 (180°) を向く
        assertEquals(180f, Math.abs(at.getYaw()));
    }

    @Test
    void facingSnapsToRightAngles() {
        // 西 (90°) 寄りを向いている → NPC は東 (-90°)
        assertEquals(-90f, PitNpcCommand.placement(new Location(null, 0, 0, 0, 80, 0)).getYaw());
        // 東 (-90° = 270°) → NPC は西 (90°)
        assertEquals(90f, PitNpcCommand.placement(new Location(null, 0, 0, 0, 265, 0)).getYaw());
        assertEquals(0f, PitNpcCommand.placement(new Location(null, 0, 0, 0, -170, 0)).getYaw());
    }
}
