package com.pitremake.leaderboard;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LeaderboardRulesTest {

    @Test
    void abbreviatesLikeTheRecording() {
        // 録画: My pit stats の XP 5,171,145 → "You have: 5.1M XP"、プレステージ L の 120 → "11B XP"
        assertEquals("5.1M", LeaderboardRules.abbreviate(5_171_145));
        assertEquals("11B", LeaderboardRules.abbreviate(11_480_000_000L));
        assertEquals("1K", LeaderboardRules.abbreviate(1_050));
        assertEquals("250K", LeaderboardRules.abbreviate(250_999));
        assertEquals("999", LeaderboardRules.abbreviate(999));
        assertEquals("0", LeaderboardRules.abbreviate(0));
    }

    @Test
    void ranksByXp() {
        UUID a = new UUID(0, 1);
        UUID b = new UUID(0, 2);
        UUID c = new UUID(0, 3);
        Map<UUID, Long> xp = new HashMap<>();
        xp.put(a, 10L);
        xp.put(b, 300L);
        xp.put(c, 20L);
        assertEquals(Arrays.asList(b, c, a), LeaderboardRules.rank(xp));
    }

    @Test
    void seasonOfTheRecording() {
        assertEquals("Fall 2026", LeaderboardRules.season(LocalDate.of(2026, 10, 15)));
        assertEquals("Spring 2027", LeaderboardRules.season(LocalDate.of(2027, 4, 1)));
    }
}
