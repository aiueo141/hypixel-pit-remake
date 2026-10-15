package com.pitremake.spire;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpireRulesTest {

    @Test
    void floorsFromTheRecording() {
        assertEquals("§4§lONE", SpireRules.name(1));
        assertEquals("§c-50%", SpireRules.rewardText(1));
        assertEquals("§aNORMAL", SpireRules.rewardText(3));
        assertEquals("§e+25%", SpireRules.rewardText(4));
        assertEquals(85, SpireRules.floorY(1));
        assertEquals(25, SpireRules.killSouls(9));
        assertEquals(0, SpireRules.deathSouls(1));
        // 範囲外は端の階
        assertEquals(SpireRules.floorY(9), SpireRules.floorY(12));
        assertEquals(SpireRules.floorY(1), SpireRules.floorY(0));
    }

    @Test
    void placementRewards() {
        assertArrayEquals(new int[]{2000, 2}, SpireRules.placementReward(3));
        assertArrayEquals(new int[]{500, 1}, SpireRules.placementReward(4));
        assertArrayEquals(new int[]{500, 1}, SpireRules.placementReward(20));
        assertArrayEquals(new int[]{100, 0}, SpireRules.placementReward(21));
    }

    @Test
    void time() {
        assertEquals("04:56", SpireRules.time(296));
        assertEquals("00:00", SpireRules.time(-3));
    }

    /** 塔のデータ: 3 つのフェーズがあり、どのブロックも 5 つの数字。 */
    @Test
    void structureData() throws Exception {
        InputStream in = getClass().getClassLoader().getResourceAsStream("spire/seasons.txt");
        assertNotNull(in);
        Map<String, Integer> counts = new HashMap<>();
        String phase = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("@")) {
                    phase = line.substring(1).trim();
                    continue;
                }
                assertEquals(5, line.trim().split(" ").length, line);
                counts.merge(phase, 1, Integer::sum);
            }
        }
        assertTrue(counts.get("build") > 20000);
        assertTrue(counts.get("tunnel") > 1000);
        assertTrue(counts.get("open") > 100);
    }
}
