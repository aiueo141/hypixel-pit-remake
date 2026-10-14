package com.pitremake.spawn;

import com.pitremake.PitPlugin;
import com.pitremake.map.PitMapType;
import com.pitremake.renown.RenownUpgrade;
import com.pitremake.util.PlayerPackets;
import net.minecraft.server.v1_8_R3.EntityArmorStand;
import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftArmorStand;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * スポーンの外周の打ち上げ (録画 Death.mcrc の 2 回の打ち上げ)。
 * <pre>
 * 1. スポーンの床の上で、中心から約 23 ブロック (Seasons の launcherRadius 25 より 2 内側) を外へ越える
 * 2. その場で hugeexplosion 1 個 + random.explode (0.5 / 1.79) + fireworks.launch (0.4 / 1.19)
 * 3. 見えないアーマースタンドに乗せて 81 tick で決まった弧を飛ぶ (録画の位置を tick ごとに表にしたもの)
 *    行き先は中心から見た向きを 45° 単位に丸めた方向の 108.2 ブロック先 (録画は (76.5, 97.7, -76.5) に 2 回とも着地)
 * 4. 着地で降ろして mob.bat.takeoff (0.2 / 0.78)
 * </pre>
 * ほかの向きの着地点は録画に無いので、途中でブロックに当たったらそこで降ろす。
 * 飛んでいる間は降りられない。Renown の打ち上げの軌跡 (Superb / Blazing / Bang) もここで出す。
 */
public final class Launcher implements Listener {

    /** 録画の打ち上げの 0〜81 tick: 出発点からの水平距離と高さ (出発点との差)。 */
    private static final double[][] PATH = {
            {0, 0}, {0.49, 1.67}, {0.97, 3.33}, {1.46, 5.0}, {2.12, 6.91}, {3.0, 8.65}, {3.87, 10.38}, {4.91, 11.94},
            {5.94, 13.53}, {7.25, 14.88}, {8.55, 16.22}, {9.88, 17.56}, {11.38, 18.66}, {12.84, 19.72}, {14.34, 20.81},
            {15.96, 21.62}, {17.58, 22.47}, {19.22, 23.31}, {20.91, 23.94}, {22.61, 24.56}, {24.3, 25.19}, {26.01, 25.59},
            {27.73, 26.03}, {29.46, 26.44}, {31.18, 26.69}, {32.91, 26.94}, {34.63, 27.16}, {36.33, 27.22}, {38.01, 27.31},
            {39.69, 27.38}, {41.3, 27.28}, {42.91, 27.19}, {44.52, 27.09}, {46.07, 26.91}, {47.62, 26.69}, {49.16, 26.47},
            {50.6, 26.16}, {52.04, 25.84}, {53.48, 25.53}, {54.82, 25.09}, {56.17, 24.66}, {57.5, 24.22}, {58.74, 23.72},
            {59.97, 23.25}, {61.18, 22.75}, {62.29, 22.19}, {63.42, 21.62}, {64.52, 21.03}, {65.56, 20.41}, {66.62, 19.78},
            {67.64, 19.16}, {68.63, 18.44}, {69.63, 17.69}, {70.6, 16.97}, {71.53, 16.12}, {72.46, 15.31}, {73.38, 14.47},
            {74.24, 13.56}, {75.14, 12.62}, {75.97, 11.72}, {76.76, 10.72}, {77.56, 9.69}, {78.36, 8.69}, {79.07, 7.56},
            {79.75, 6.44}, {80.43, 5.31}, {81.05, 4.09}, {81.67, 2.91}, {82.29, 1.69}, {82.79, 0.38}, {83.33, -0.94},
            {83.83, -2.28}, {84.26, -3.69}, {84.65, -5.09}, {85.06, -6.53}, {85.36, -8.06}, {85.67, -9.56}, {85.98, -11.09},
            {86.16, -12.72}, {86.33, -14.38}, {86.51, -16.0}, {86.56, -16.34}};
    /** 録画の出発点から着地点までの水平距離。 */
    private static final double RECORDED_DISTANCE = 86.56;
    /** 中心 (0, 0) から着地点までの距離 (録画の (76.5, -76.5))。 */
    static final double TARGET_DISTANCE = 76.5 * Math.sqrt(2);
    /** launcherRadius からどれだけ内側で打ち上げるか (録画では 2 回とも約 23 ブロック)。 */
    static final double TRIGGER_INSET = 2.0;
    private static final double SOUND_RANGE = 32;

    private final PitPlugin plugin;
    private final Map<UUID, Flight> flights = new HashMap<>();
    private BukkitTask task;

    private static final class Flight {
        final Player player;
        final ArmorStand stand;
        final Location start;
        final Vector direction;
        final double distance;
        int tick;
        boolean landing;

        Flight(Player player, ArmorStand stand, Location start, Vector direction, double distance) {
            this.player = player;
            this.stand = stand;
            this.start = start;
            this.direction = direction;
            this.distance = distance;
        }
    }

    public Launcher(PitPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1, 1);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        for (Flight flight : new ArrayList<>(flights.values())) {
            land(flight);
        }
    }

    public boolean isFlying(Player player) {
        return flights.containsKey(player.getUniqueId());
    }

    /** 死んだときなど: 飛んでいたらすぐ降ろす。 */
    public void cancel(Player player) {
        Flight flight = flights.get(player.getUniqueId());
        if (flight != null) {
            land(flight);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (isFlying(player) || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR
                || player.isInsideVehicle() || plugin.getBotManager().isBot(player)) {
            return;
        }
        PitMapType map = plugin.getPitMap();
        Location to = event.getTo();
        if (!to.getWorld().equals(plugin.getServer().getWorlds().get(0))
                || to.getY() < map.getSpawnY() - 0.5 || to.getY() > map.getSpawnY() + 3) {
            return;
        }
        double trigger = map.getLauncherRadius() - TRIGGER_INSET;
        double from = Math.hypot(event.getFrom().getX(), event.getFrom().getZ());
        double now = Math.hypot(to.getX(), to.getZ());
        if (from < trigger && now >= trigger) {
            launch(player, event.getFrom());
        }
    }

    private void launch(Player player, Location from) {
        // 中心から見た向きを 45° 単位に丸め、その方向の決まった点へ飛ぶ
        double angle = Math.atan2(from.getZ(), from.getX());
        double snapped = Math.round(angle / (Math.PI / 4)) * (Math.PI / 4);
        Vector unit = new Vector(Math.cos(snapped), 0, Math.sin(snapped));
        Location target = new Location(from.getWorld(), unit.getX() * TARGET_DISTANCE, 0, unit.getZ() * TARGET_DISTANCE);
        Location start = from.clone();
        Vector horizontal = new Vector(target.getX() - start.getX(), 0, target.getZ() - start.getZ());
        double distance = horizontal.length();
        horizontal.normalize();

        World world = from.getWorld();
        ArmorStand stand = world.spawn(start, ArmorStand.class);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setSmall(true);
        stand.setPassenger(player);
        Flight flight = new Flight(player, stand, start, horizontal, distance);
        flights.put(player.getUniqueId(), flight);

        Location at = player.getLocation();
        PacketPlayOutWorldParticles explosion = new PacketPlayOutWorldParticles(EnumParticle.EXPLOSION_HUGE, true,
                (float) at.getX(), (float) at.getY(), (float) at.getZ(), 0, 0, 0, 1, 1);
        for (Player viewer : world.getPlayers()) {
            if (viewer.getLocation().distanceSquared(at) <= SOUND_RANGE * SOUND_RANGE) {
                PlayerPackets.send(viewer, explosion);
                viewer.playSound(at, "random.explode", 0.5f, 113 / 63f);
                viewer.playSound(at, "fireworks.launch", 0.4f, 75 / 63f);
            }
        }
    }

    private void tick() {
        for (Flight flight : new ArrayList<>(flights.values())) {
            if (!flight.player.isOnline() || flight.player.isDead() || flight.stand.isDead()) {
                land(flight);
                continue;
            }
            int next = flight.tick + 1;
            if (next >= PATH.length) {
                land(flight);
                continue;
            }
            Location position = position(flight, next);
            if (position.getBlock().getType().isSolid() || position.clone().add(0, 1, 0).getBlock().getType().isSolid()) {
                // 録画に無い向きで途中のブロックに当たったら、そこで降ろす
                land(flight);
                continue;
            }
            flight.tick = next;
            EntityArmorStand handle = ((CraftArmorStand) flight.stand).getHandle();
            handle.setPosition(position.getX(), position.getY(), position.getZ());
            trail(flight.player, position);
        }
    }

    private static Location position(Flight flight, int tick) {
        double along = PATH[tick][0] / RECORDED_DISTANCE * flight.distance;
        return flight.start.clone().add(flight.direction.clone().multiply(along)).add(0, PATH[tick][1], 0);
    }

    private void land(Flight flight) {
        flights.remove(flight.player.getUniqueId());
        flight.landing = true;
        Location at = flight.stand.getLocation();
        flight.stand.eject();
        flight.stand.remove();
        if (flight.player.isOnline() && !flight.player.isDead()) {
            at.setYaw(flight.player.getLocation().getYaw());
            at.setPitch(flight.player.getLocation().getPitch());
            flight.player.teleport(at);
            flight.player.setVelocity(new Vector());
            flight.player.setFallDistance(0);
            flight.player.playSound(at, "mob.bat.takeoff", 0.2f, 49 / 63f);
        }
    }

    /** 飛んでいる間は降りられない。 */
    @EventHandler(ignoreCancelled = true)
    public void onExit(VehicleExitEvent event) {
        if (event.getExited() instanceof Player) {
            Flight flight = flights.get(event.getExited().getUniqueId());
            if (flight != null && !flight.landing) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancel(event.getPlayer());
    }

    /** Renown の打ち上げの軌跡 (持っている中で一番上のもの): Bang = 爆発、Blazing = 炎、Superb = 虹色。 */
    private void trail(Player player, Location at) {
        UUID id = player.getUniqueId();
        Flight flight = flights.get(id);
        int tick = flight == null ? 0 : flight.tick;
        PacketPlayOutWorldParticles packet;
        if (plugin.getUpgradeManager().hasRenown(id, RenownUpgrade.BANG_LAUNCH_TRAIL)) {
            if (tick % 2 != 0) {
                return;
            }
            packet = particle(EnumParticle.EXPLOSION_LARGE, at, 0, 0, 0, 0, 1);
        } else if (plugin.getUpgradeManager().hasRenown(id, RenownUpgrade.BLAZING_LAUNCH_TRAIL)) {
            packet = particle(EnumParticle.FLAME, at, 0.1f, 0.1f, 0.1f, 0.01f, 3);
        } else if (plugin.getUpgradeManager().hasRenown(id, RenownUpgrade.SUPERB_LAUNCH_TRAIL)) {
            // 色付きのレッドストーン: 散らばりの値を RGB として使う (数 0)
            Color color = Color.getHSBColor((tick % 30) / 30f, 1, 1);
            packet = particle(EnumParticle.REDSTONE, at, Math.max(0.001f, color.getRed() / 255f), color.getGreen() / 255f,
                    color.getBlue() / 255f, 1, 0);
        } else {
            return;
        }
        for (Player viewer : at.getWorld().getPlayers()) {
            if (viewer.getLocation().distanceSquared(at) <= 64 * 64) {
                PlayerPackets.send(viewer, packet);
            }
        }
    }

    private static PacketPlayOutWorldParticles particle(EnumParticle type, Location at, float dx, float dy, float dz,
                                                        float speed, int count) {
        return new PacketPlayOutWorldParticles(type, true, (float) at.getX(), (float) at.getY(), (float) at.getZ(),
                dx, dy, dz, speed, count);
    }

    /** 確認用。 */
    static List<double[]> path() {
        List<double[]> list = new ArrayList<>();
        for (double[] point : PATH) {
            list.add(point.clone());
        }
        return list;
    }
}
