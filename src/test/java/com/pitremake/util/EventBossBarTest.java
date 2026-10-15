package com.pitremake.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventBossBarTest {

    /** 録画 2026-10-15_14-18-51 (KOTH) / 13-57-51 (Spire) のウィザーの名前。 */
    @Test
    void shimmerMatchesTheRecording() {
        assertEquals("§f§lM§d§lINOR EVENT!", EventBossBar.shimmer("MINOR EVENT!", "§d", "§9", 0));
        assertEquals("§d§lM§9§lIN§f§lO§d§lR EVENT!", EventBossBar.shimmer("MINOR EVENT!", "§d", "§9", 3));
        assertEquals("§d§lMINOR EVEN§9§lT!", EventBossBar.shimmer("MINOR EVENT!", "§d", "§9", 12));
        assertEquals("§d§lMINOR EVENT§9§l!", EventBossBar.shimmer("MINOR EVENT!", "§d", "§9", 13));
        assertEquals("§d§lMINOR EVENT!", EventBossBar.shimmer("MINOR EVENT!", "§d", "§9", 14));
        assertEquals("§f§lM§d§lINOR EVENT!", EventBossBar.shimmer("MINOR EVENT!", "§d", "§9", 18));
        assertEquals("§5§lMAJOR§d§l E§f§lV§5§lENT!", EventBossBar.shimmer("MAJOR EVENT!", "§5", "§d", 7));
    }

    @Test
    void healthInThreePointSteps() {
        assertEquals(300, EventBossBar.health(1), 0);
        assertEquals(297, EventBossBar.health(0.99), 0);
        assertEquals(1, EventBossBar.health(0), 0);
        assertEquals(150, EventBossBar.health(0.5), 0);
    }
}
