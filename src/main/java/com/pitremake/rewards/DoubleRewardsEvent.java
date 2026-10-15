package com.pitremake.rewards;

import com.pitremake.PitPlugin;
import com.pitremake.spire.SpireRules;
import com.pitremake.util.EventBossBar;
import com.pitremake.util.PlayerPackets;
import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * 2X REWARDS (Minor Event)。録画 2026-10-15_17-07-13.mcrc (Summer Area) / 17-09-35.mcrc (Autumn Area)。
 * <pre>
 * ボスバー: §d§lMINOR EVENT! §2§l2X REWARDS! §7Go to §e§l§lSUMMER§7! §7Ending in §a02:49§7 (KOTH と同じ光と体力、4 分)
 * 文字    : 区域のパッドの上に 4 行 §d§lMINOR EVENT! / §e2x §bXP§d/§6gold§e! / §ein §e§lSummer Area / §a02:49 (毎秒)
 * 光      : 一番上の行の所に happyVillager (散らばり 4、40 個) を 12 tick ごと
 * </pre>
 * 倍率は /pit multireward X start で変えられる (自動は config の double-rewards.multiplier、録画は 2)。
 * 区域の中で倒すとキルの XP・ゴールドが X 倍 (アシストは自分が区域の中なら)。バウンティは 2 倍にならない (KOTH と同じとした)。
 * 今は区域は Mid だけ (ユーザーの指定): スポーンの下の真ん中 (半径 config の double-rewards.mid-radius) で、
 * 文字と光は Mid の床の真ん中の上。始まり・1 分ごと・終わりのチャットは録画に無いので KOTH と同じ形。
 */
public final class DoubleRewardsEvent {

    public static final int DURATION_MINUTES = 4;
    /** 自動で始まるときの倍率 (config の double-rewards.multiplier)。録画は 2。 */
    public static final double DEFAULT_MULTIPLIER = 2;
    private static final String AREA = "Mid";
    private static final String AREA_COLOR = "§c";
    private static final double[] LINE_Y = {3.188, 2.875, 2.594, 2.281};
    private static final int SPARKLE_TICKS = 12;
    private static final int[][] CHORD = {{36, 49}, {49, 61}, {61, 73}};

    private final PitPlugin plugin;
    private final List<ArmorStand> hologram = new ArrayList<>();
    private boolean running;
    /** 今の倍率 (/pit multireward X start で変えられる)。 */
    private double multiplier = DEFAULT_MULTIPLIER;
    private long endTime;
    private long nextEvent;
    private int lastAnnounced;
    private BukkitTask task;
    private BukkitTask sparkle;

    public DoubleRewardsEvent(PitPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        scheduleNext();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20, 20);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        if (running) {
            finish(false);
        }
    }

    public boolean isRunning() {
        return running;
    }

    private World world() {
        return plugin.getServer().getWorlds().get(0);
    }

    private void scheduleNext() {
        nextEvent = System.currentTimeMillis() + (long) (plugin.getConfig().getDouble("double-rewards.interval-minutes", 20) * 60_000);
    }

    private void tick() {
        long now = System.currentTimeMillis();
        if (!running) {
            if (plugin.getConfig().getBoolean("double-rewards.enabled", false) && now >= nextEvent) {
                if (plugin.isEventActive()) {
                    // 別のイベントの最中は終わるまで待つ
                    nextEvent = now + 60_000;
                } else if (realPlayers() < plugin.getConfig().getInt("double-rewards.min-players", 1)) {
                    scheduleNext();
                } else {
                    begin(plugin.getConfig().getDouble("double-rewards.multiplier", DEFAULT_MULTIPLIER));
                }
            }
            return;
        }
        long left = (endTime - now + 999) / 1000;
        if (left <= 0) {
            end();
            return;
        }
        if (hologram.size() == LINE_Y.length) {
            hologram.get(LINE_Y.length - 1).setCustomName("§a" + SpireRules.time(left));
        }
        int minutes = (int) ((left + 59) / 60);
        if (minutes < lastAnnounced) {
            lastAnnounced = minutes;
            broadcast(prefix() + "§7for §e" + minutes + (minutes == 1 ? " §7min" : " §7mins"));
        }
    }

    private int realPlayers() {
        int count = 0;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!plugin.getBotManager().isBot(player)) {
                count++;
            }
        }
        return count;
    }

    private String prefix() {
        return "§d§lMINOR EVENT! §2§l" + label(multiplier) + "X REWARDS §7in " + AREA_COLOR + "§l" + AREA + " ";
    }

    /** 倍率の表示 (2 → "2"、1.5 → "1.5")。 */
    public static String label(double multiplier) {
        return multiplier == Math.rint(multiplier) ? String.valueOf((long) multiplier)
                : String.valueOf(Math.round(multiplier * 100) / 100.0);
    }

    public double getMultiplier() {
        return multiplier;
    }

    /** /pit multireward X start と時間。 */
    public boolean begin(double multiplier) {
        if (running || plugin.isEventActive() || multiplier <= 0) {
            return false;
        }
        this.multiplier = multiplier;
        running = true;
        endTime = System.currentTimeMillis() + DURATION_MINUTES * 60_000L;
        lastAnnounced = DURATION_MINUTES;
        broadcast(prefix() + "§7for §e" + DURATION_MINUTES + " §7mins");
        String where = AREA.toUpperCase(java.util.Locale.ROOT);
        plugin.getEventBossBar().show(new EventBossBar.Content() {
            @Override
            public String text(int tick) {
                long left = Math.max(0, (endTime - System.currentTimeMillis() + 999) / 1000);
                return EventBossBar.shimmer("MINOR EVENT!", "§d", "§9", tick) + " §2§l" + label(multiplier) + "X REWARDS! §7Go to "
                        + AREA_COLOR + "§l§l"
                        + where + "§7! §7Ending in §a" + SpireRules.time(left) + "§7";
            }

            @Override
            public double fraction() {
                return (endTime - System.currentTimeMillis()) / (DURATION_MINUTES * 60_000.0);
            }
        });
        spawnHologram();
        Location top = textBase().add(0, LINE_Y[0], 0);
        sparkle = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(EnumParticle.VILLAGER_HAPPY, true,
                    (float) top.getX(), (float) top.getY(), (float) top.getZ(), 4, 4, 4, 0, 40);
            for (Player viewer : top.getWorld().getPlayers()) {
                if (viewer.getLocation().distanceSquared(top) <= 64 * 64) {
                    PlayerPackets.send(viewer, packet);
                }
            }
        }, 0, SPARKLE_TICKS);
        for (int i = 0; i < CHORD.length; i++) {
            int[] pitches = CHORD[i];
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> chord("note.harp", 0.8f, pitches), i * 3L);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> chord("note.bassattack", 0.72f, pitches), i * 3L + 1);
        }
        return true;
    }

    private void chord(String sound, float volume, int[] pitches) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            for (int pitch : pitches) {
                player.playSound(player.getLocation(), sound, volume, pitch / 63f);
            }
        }
    }

    /** /pit multireward end と時間切れ。 */
    public boolean end() {
        if (!running) {
            return false;
        }
        finish(true);
        return true;
    }

    private void finish(boolean announce) {
        if (announce) {
            broadcast(prefix() + "§7ended");
        }
        running = false;
        plugin.getEventBossBar().hide();
        if (sparkle != null) {
            sparkle.cancel();
            sparkle = null;
        }
        for (ArmorStand stand : hologram) {
            stand.remove();
        }
        hologram.clear();
        scheduleNext();
    }

    /** Mid の床の真ん中 (文字の高さはここからの差)。 */
    private Location textBase() {
        return new Location(world(), 0.5, plugin.getPitMap().getFloorY(), 0.5);
    }

    private void spawnHologram() {
        String[] lines = {"§d§lMINOR EVENT!", "§e" + label(multiplier) + "x §bXP§d/§6gold§e!", "§ein " + AREA_COLOR + "§l" + AREA + " Area",
                "§a" + SpireRules.time(DURATION_MINUTES * 60)};
        for (int i = 0; i < lines.length; i++) {
            Location at = textBase().add(0, LINE_Y[i], 0);
            at.getChunk().load();
            for (Entity entity : at.getWorld().getNearbyEntities(at, 0.2, 0.2, 0.2)) {
                if (entity instanceof ArmorStand && !((ArmorStand) entity).isVisible()) {
                    entity.remove();
                }
            }
            ArmorStand stand = (ArmorStand) world().spawnEntity(at, EntityType.ARMOR_STAND);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setBasePlate(false);
            stand.setMarker(true);
            stand.setCustomName(lines[i]);
            stand.setCustomNameVisible(true);
            hologram.add(stand);
        }
    }

    /** Mid の中か: 真ん中から半径 double-rewards.mid-radius (15) 以内で、スポーン (y105 より上) ではない。 */
    public boolean inArea(Player player) {
        if (!running || player == null) {
            return false;
        }
        Location at = player.getLocation();
        if (!at.getWorld().equals(world()) || plugin.getPitSpawn().isProtected(at)) {
            return false;
        }
        double radius = plugin.getConfig().getDouble("double-rewards.mid-radius", 15);
        return Math.hypot(at.getX() - 0.5, at.getZ() - 0.5) <= radius && at.getY() >= plugin.getPitMap().getFloorY() - 3;
    }

    /** キルの XP・ゴールドに掛ける数。 */
    public double killMultiplier(Player killer) {
        return inArea(killer) ? multiplier : 1;
    }

    private void broadcast(String message) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendMessage(message);
        }
        plugin.getServer().getConsoleSender().sendMessage(message);
    }
}
