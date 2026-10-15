package com.pitremake.stats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PitStatsMenuTest {

    @Test
    void numbersLikeTheRecording() {
        // 録画 2026-10-15_10-32-13.mcrc の My pit stats
        assertEquals("§7§7Kills: §a119,884", PitStatsMenu.stat("Kills", 119_884));
        assertEquals("0.865", PitStatsMenu.ratio(119_884, 138_564));
        assertEquals("2.435", PitStatsMenu.ratio(119_884 + 217_525, 138_564));
        assertEquals("0.736", PitStatsMenu.ratio(2_466_344, 3_348_749));
        assertEquals("0.23", PitStatsMenu.ratio(188, 819));
        assertEquals("10,329.127", PitStatsMenu.ratio(4_699_753, 455));
        assertEquals("741.558", PitStatsMenu.ratio(119_884 + 217_525, 455));
    }
}
