package com.pitremake.well;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Mystic Well の効果音が録画 (MysticWell.mcrc から書き出した well_roll_sounds.tsv) と一致するか。 */
class WellSoundsTest {

    @Test
    void rollSoundsMatchRecording() throws IOException {
        Map<String, List<String[]>> recorded = loadRecording();
        for (int tier = 1; tier <= 3; tier++) {
            for (int tick = 0; tick <= WellSounds.endTick(tier); tick++) {
                List<String[]> expected = recorded.getOrDefault(tier + ":" + tick, Collections.<String[]>emptyList());
                List<WellSounds.Sound> actual = WellSounds.rollSounds(tier, tick);
                String where = "tier " + tier + " tick " + tick;

                assertEquals(keys(expected), actualKeys(actual), where);
                // 音量 (録画には数か所 0.04 ずれがあるので少し余裕を持たせる)
                for (String[] sound : expected) {
                    float volume = Float.parseFloat(sound[3]);
                    boolean found = false;
                    for (WellSounds.Sound s : actual) {
                        if (s.name.equals(sound[2]) && s.pitchByte() == Integer.parseInt(sound[4])
                                && Math.abs(s.volume - volume) <= 0.05f) {
                            found = true;
                        }
                    }
                    assertTrue(found, where + " volume of " + sound[2]);
                }
            }
        }
    }

    @Test
    void rollLastsLongerForHigherTiers() {
        assertEquals(60, WellSounds.endTick(1));
        assertEquals(70, WellSounds.endTick(2));
        assertEquals(82, WellSounds.endTick(3));
    }

    private static List<String> keys(List<String[]> sounds) {
        List<String> keys = new ArrayList<>();
        for (String[] sound : sounds) {
            keys.add(sound[2] + "@" + sound[4]);
        }
        Collections.sort(keys);
        return keys;
    }

    private static List<String> actualKeys(List<WellSounds.Sound> sounds) {
        List<String> keys = new ArrayList<>();
        for (WellSounds.Sound sound : sounds) {
            keys.add(sound.name + "@" + sound.pitchByte());
        }
        Collections.sort(keys);
        return keys;
    }

    /** "tier:tick" → [tier, tick, name, volume, pitchByte] */
    private static Map<String, List<String[]>> loadRecording() throws IOException {
        Map<String, List<String[]>> sounds = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                WellSoundsTest.class.getResourceAsStream("/well_roll_sounds.tsv"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\t");
                sounds.computeIfAbsent(parts[0] + ":" + parts[1], key -> new ArrayList<>()).add(parts);
            }
        }
        return sounds;
    }
}
