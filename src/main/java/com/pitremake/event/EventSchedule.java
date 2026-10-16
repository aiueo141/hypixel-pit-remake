package com.pitremake.event;

import org.bukkit.configuration.Configuration;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 * 自動イベントの時刻を時計に合わせて決める: config の &lt;key&gt;.interval-minutes ごと、毎時 offset-minutes 分から。
 * 例: interval 30 / offset 15 → 毎時 15 分と 45 分。前のイベントが終わった時刻からは数えないので、ずれて重なることはない。
 * Spire は offset の時刻に予告 (3 分) が始まる。
 */
public final class EventSchedule {

    private static final long MINUTE = 60_000L;
    /** 重なりのチェックで見る長さ (1 日)。 */
    private static final long CHECK_SPAN = 24 * 60 * MINUTE;

    private final Plugin plugin;
    private final List<PitEvent> events = new ArrayList<>();

    public EventSchedule(Plugin plugin) {
        this.plugin = plugin;
    }

    private Configuration config() {
        return plugin.getConfig();
    }

    public void add(PitEvent event) {
        events.add(event);
    }

    public List<PitEvent> getEvents() {
        return events;
    }

    /** key のイベントが now の後に初めて始まる時刻。 */
    public long next(String key, double defaultInterval, long now) {
        return next(now, interval(key, defaultInterval), config().getDouble(key + ".offset-minutes", 0), zoneOffset(now));
    }

    private double interval(String key, double defaultInterval) {
        return config().getDouble(key + ".interval-minutes", defaultInterval);
    }

    private static long zoneOffset(long now) {
        return TimeZone.getDefault().getOffset(now);
    }

    /** (サーバーの時計で) offset 分 + interval 分の倍数 の時刻のうち、now より後の最初のもの。 */
    static long next(long now, double intervalMinutes, double offsetMinutes, long zoneOffset) {
        long interval = Math.max(MINUTE, (long) (intervalMinutes * MINUTE));
        long offset = (long) (offsetMinutes * MINUTE);
        long local = now + zoneOffset - offset;
        long slot = Math.floorDiv(local, interval) * interval + interval;
        return slot - zoneOffset + offset;
    }

    /** 自動で始まるイベントどうしで、1 日のうちに時間が重なるもの (「KOTH 12:15 / AUCTION 12:15」の形)。 */
    public List<String> overlaps(long now) {
        List<Slot> slots = new ArrayList<>();
        long zone = zoneOffset(now);
        for (PitEvent event : events) {
            if (!event.isScheduled()) {
                continue;
            }
            double interval = interval(event.configKey(), 60);
            double offset = config().getDouble(event.configKey() + ".offset-minutes", 0);
            for (long time = next(now, interval, offset, zone); time < now + CHECK_SPAN; time = next(time, interval, offset, zone)) {
                slots.add(new Slot(event, time, time + event.lengthMinutes() * MINUTE));
            }
        }
        return overlaps(slots);
    }

    static final class Slot {
        final PitEvent event;
        final long start;
        final long end;

        Slot(PitEvent event, long start, long end) {
            this.event = event;
            this.start = start;
            this.end = end;
        }
    }

    static List<String> overlaps(List<Slot> slots) {
        slots.sort((a, b) -> Long.compare(a.start, b.start));
        List<String> result = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int i = 0; i < slots.size(); i++) {
            Slot a = slots.get(i);
            for (int j = i + 1; j < slots.size() && slots.get(j).start < a.end; j++) {
                Slot b = slots.get(j);
                String line = a.event.configKey() + " " + minuteOfHour(a.start) + " / " + b.event.configKey() + " " + minuteOfHour(b.start);
                if (seen.add(line)) {
                    result.add(line);
                }
            }
        }
        return result;
    }

    private static String minuteOfHour(long time) {
        long local = time + zoneOffset(time);
        return String.format(":%02d", Math.floorMod(local / MINUTE, 60L));
    }
}
