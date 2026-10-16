package com.pitremake.event;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventScheduleTest {

    private static final long MINUTE = 60_000L;
    private static final long HOUR = 60 * MINUTE;

    @Test
    void nextIsOnTheClock() {
        long base = 1000 * HOUR;
        // 30 分ごと、毎時 15 分から: 12:00 → 12:15、12:15 ちょうど → 12:45、12:50 → 13:15
        assertEquals(base + 15 * MINUTE, EventSchedule.next(base, 30, 15, 0));
        assertEquals(base + 45 * MINUTE, EventSchedule.next(base + 15 * MINUTE, 30, 15, 0));
        assertEquals(base + HOUR + 15 * MINUTE, EventSchedule.next(base + 50 * MINUTE, 30, 15, 0));
        // タイムゾーンがずれていても、その時計の 15 分
        long zone = 9 * HOUR + 30 * MINUTE;
        long next = EventSchedule.next(base, 30, 15, zone);
        assertEquals(15, Math.floorMod((next + zone) / MINUTE, 30L));
    }

    @Test
    void findsOverlaps() {
        List<EventSchedule.Slot> slots = new ArrayList<>();
        slots.add(new EventSchedule.Slot(event("koth", 4), 0, 4 * MINUTE));
        slots.add(new EventSchedule.Slot(event("auction", 3), 3 * MINUTE, 6 * MINUTE));
        slots.add(new EventSchedule.Slot(event("spire", 8), 10 * MINUTE, 18 * MINUTE));
        assertEquals(1, EventSchedule.overlaps(slots).size());
    }

    /** 同梱の config.yml の時刻 (offset-minutes) は 1 日のうちで重ならない。 */
    @Test
    void defaultConfigDoesNotOverlap() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(
                new InputStreamReader(getClass().getResourceAsStream("/config.yml"), StandardCharsets.UTF_8));
        List<EventSchedule.Slot> slots = new ArrayList<>();
        String[] keys = {"spire", "koth", "double-rewards", "auction"};
        int[] lengths = {8, 4, 4, 3};
        for (int i = 0; i < keys.length; i++) {
            double interval = config.getDouble(keys[i] + ".interval-minutes");
            double offset = config.getDouble(keys[i] + ".offset-minutes");
            assertTrue(interval > 0, keys[i]);
            for (long time = EventSchedule.next(0, interval, offset, 0); time < 24 * HOUR; time = EventSchedule.next(time, interval, offset, 0)) {
                slots.add(new EventSchedule.Slot(event(keys[i], lengths[i]), time, time + lengths[i] * MINUTE));
            }
        }
        assertEquals(new ArrayList<String>(), EventSchedule.overlaps(slots));
    }

    private static PitEvent event(String key, int length) {
        return new PitEvent() {
            @Override
            public String configKey() {
                return key;
            }

            @Override
            public String displayName() {
                return key;
            }

            @Override
            public boolean isScheduled() {
                return true;
            }

            @Override
            public int lengthMinutes() {
                return length;
            }

            @Override
            public long nextStart() {
                return 0;
            }

            @Override
            public long endsAt() {
                return 0;
            }

            @Override
            public void scheduleNext() {
            }
        };
    }
}
