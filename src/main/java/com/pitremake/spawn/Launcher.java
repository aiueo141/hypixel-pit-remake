package com.pitremake.spawn;

import com.pitremake.PitPlugin;
import com.pitremake.data.PitStat;
import com.pitremake.map.PitMapType;
import com.pitremake.renown.RenownUpgrade;
import com.pitremake.util.PlayerPackets;
import net.minecraft.server.v1_8_R3.EntityArmorStand;
import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;

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
 * <p>
 * resources/launcher/&lt;マップ名&gt;.yml がある (Seasons) マップでは、録画 2026-10-15_10-32-13 / 10-34-42 の
 * 打ち上げ 135 回 (他のプレイヤーを含む) に合わせて、角の 4 つのパッド ({@link LauncherPads}) からだけ打ち上げる。
 * <pre>
 * 1. パッドの向きの範囲で、パッドごとの半径を外へ越える (北東 22.1 / 北西 16.7 / 南西 18.9 / 南東 24.1)
 * 2. パッドの着地点から重みでランダムに 1 つ選ぶ (録画の北西パッドは 4 か所にほぼ同じ回数ずつ飛んでいた。向き・キー入力・速さとは無関係)
 * 3. 着地点までの距離が一番近い録画の弧 (水平距離を合わせて伸縮) で飛ぶ。弧が終わっても地面に当たるまでは同じ速さで下り続ける
 * </pre>
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
    /** パッドの打ち上げで、録画の弧が終わってから地面に当たるまで下り続ける最大 tick 数。 */
    private static final int PAD_EXTRA_TICKS = 60;
    /** KOTH の直行便の着地点の広がり (録画の着地点は丘の真ん中から 6 ブロックくらいまで)。 */
    private static final double KOTH_LANDING_RADIUS = 5;
    /** 乗る合図を送り直す tick 数。 */
    public static final int ATTACH_RESEND_TICKS = 4;
    /** パッドの radius から文字の場所までの距離。 */
    private static final double PAD_TOP_INSET = 3.13;

    private final PitPlugin plugin;
    private final Map<UUID, Flight> flights = new HashMap<>();
    private final Random random = new Random();
    /** 今のマップのパッドのデータ。無ければ null (中心から半径を越えたらどの向きでも打ち上げる)。 */
    private LauncherPads pads;
    private BukkitTask task;

    private static final class Flight {
        final Player player;
        final ArmorStand stand;
        final Location start;
        final Vector direction;
        /** tick ごとの {水平距離, 高さ}。 */
        final double[][] path;
        /** path の水平距離に掛ける倍率。 */
        final double scale;
        /** path が終わってからも同じ速さで進む tick 数。 */
        final int extraTicks;
        int tick;
        boolean landing;

        Flight(Player player, ArmorStand stand, Location start, Vector direction, double[][] path, double scale, int extraTicks) {
            this.player = player;
            this.stand = stand;
            this.start = start;
            this.direction = direction;
            this.path = path;
            this.scale = scale;
            this.extraTicks = extraTicks;
        }
    }

    public Launcher(PitPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        String resource = "launcher/" + plugin.getPitMap().getName().toLowerCase(Locale.ROOT) + ".yml";
        try {
            pads = LauncherPads.load(plugin.getResource(resource));
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load " + resource, e);
        }
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
        Location from = event.getFrom();
        if (pads != null) {
            LauncherPads.Pad pad = pads.crossed(from.getX(), from.getZ(), to.getX(), to.getZ());
            if (pad != null) {
                double[] target = target(pad);
                double distance = Math.hypot(target[0] - from.getX(), target[2] - from.getZ());
                LauncherPads.Template template = pads.nearest(distance);
                launch(player, from, target[0], target[2], template.path, distance / template.distance(), PAD_EXTRA_TICKS);
            }
            return;
        }
        double trigger = map.getLauncherRadius() - TRIGGER_INSET;
        double fromRadius = Math.hypot(from.getX(), from.getZ());
        double now = Math.hypot(to.getX(), to.getZ());
        if (fromRadius < trigger && now >= trigger) {
            // 中心から見た向きを 45° 単位に丸め、その方向の決まった点へ飛ぶ
            double angle = Math.atan2(from.getZ(), from.getX());
            double snapped = Math.round(angle / (Math.PI / 4)) * (Math.PI / 4);
            double targetX = Math.cos(snapped) * TARGET_DISTANCE;
            double targetZ = Math.sin(snapped) * TARGET_DISTANCE;
            double distance = Math.hypot(targetX - from.getX(), targetZ - from.getZ());
            launch(player, from, targetX, targetZ, PATH, distance / RECORDED_DISTANCE, 0);
        }
    }

    /**
     * 着地点。KOTH の間は丘に一番近いパッドが丘への直行便になる
     * (録画 2026-10-15_14-18-51.mcrc: KOTH 中に NW のパッドから飛んだ人は全員 Summer Area の丘 (-61, -67) の周りに着地)。
     */
    private double[] target(LauncherPads.Pad pad) {
        if (pad == kothPad()) {
            double[] hill = plugin.getKothEvent().hillCenter();
            double angle = random.nextDouble() * Math.PI * 2;
            double r = Math.sqrt(random.nextDouble()) * KOTH_LANDING_RADIUS;
            return new double[]{hill[0] + Math.cos(angle) * r, hill[1], hill[2] + Math.sin(angle) * r};
        }
        return LauncherPads.pickTarget(pad, random);
    }

    /** KOTH 中なら丘に一番近いパッド、それ以外は null。 */
    private LauncherPads.Pad kothPad() {
        if (pads == null || plugin.getKothEvent() == null || !plugin.getKothEvent().isRunning()) {
            return null;
        }
        double[] hill = plugin.getKothEvent().hillCenter();
        return pads.facing(Math.toDegrees(Math.atan2(hill[2], hill[0])));
    }

    /**
     * (x, z) に向きが一番近いパッドの上 {x, z} (KOTH の文字の場所)。パッドが無ければ null。
     * 録画: 北西のパッド (radius 16.67、向き -135°) の文字は (-14, -14) = 中心から radius + 3.13。
     */
    public double[] padTop(double x, double z) {
        if (pads == null) {
            return null;
        }
        LauncherPads.Pad pad = pads.facing(Math.toDegrees(Math.atan2(z, x)));
        double angle = Math.toRadians((pad.angleMin + pad.angleMax) / 2);
        double r = pad.radius + PAD_TOP_INSET;
        return new double[]{Math.cos(angle) * r, Math.sin(angle) * r};
    }

    /** KOTH 中に Bot が使う丘への道 (KOTH でなければ null)。 */
    public com.pitremake.bot.BotRoute kothRoute() {
        LauncherPads.Pad pad = kothPad();
        if (pad == null) {
            return null;
        }
        double angle = Math.toRadians((pad.angleMin + pad.angleMax) / 2);
        double walk = pad.radius + 3;
        World world = plugin.getServer().getWorlds().get(0);
        Location destination = new Location(world, Math.cos(angle) * walk, plugin.getPitMap().getSpawnY(), Math.sin(angle) * walk);
        return new com.pitremake.bot.BotRoute() {
            @Override
            public Location destination() {
                return destination;
            }

            @Override
            public Flight launch(Location from, Location to) {
                if (!plugin.getKothEvent().isRunning() || pads.crossed(from.getX(), from.getZ(), to.getX(), to.getZ()) != pad) {
                    return null;
                }
                double[] target = target(pad);
                double distance = Math.hypot(target[0] - from.getX(), target[2] - from.getZ());
                LauncherPads.Template template = pads.nearest(distance);
                Vector direction = new Vector(target[0] - from.getX(), 0, target[2] - from.getZ()).normalize();
                Launcher.Flight flight = new Launcher.Flight(null, null, from.clone(), direction, template.path, distance / template.distance(),
                        PAD_EXTRA_TICKS);
                launchEffects(from, null);
                return tick -> {
                    if (tick >= flight.path.length + flight.extraTicks) {
                        return null;
                    }
                    Location position = position(flight, tick);
                    if (position.getBlock().getType().isSolid() || position.clone().add(0, 1, 0).getBlock().getType().isSolid()) {
                        return null;
                    }
                    return position;
                };
            }
        };
    }

    private void launch(Player player, Location from, double targetX, double targetZ, double[][] path, double scale,
                        int extraTicks) {
        Location start = from.clone();
        Vector horizontal = new Vector(targetX - start.getX(), 0, targetZ - start.getZ());
        horizontal.normalize();

        World world = from.getWorld();
        ArmorStand stand = world.spawn(start, ArmorStand.class);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setSmall(true);
        stand.setPassenger(player);
        Flight flight = new Flight(player, stand, start, horizontal, path, scale, extraTicks);
        flights.put(player.getUniqueId(), flight);
        plugin.getPlayerProfiles().add(player.getUniqueId(), PitStat.LAUNCHER_LAUNCHES, 1);
        launchEffects(player.getLocation(), player);
    }

    /** 打ち上げの爆発 (周りの人に) と音 (飛んだ本人だけ。Bot なら null で音なし)。 */
    /**
     * ほかの人にも台 (透明なアーマースタンド) に座っているように見せる。
     * 台を出した tick に送られる乗る合図は、まだ台を知らない人には無視されるので、最初の数 tick 送り直す。
     */
    public static void showRiding(org.bukkit.entity.Entity rider, org.bukkit.entity.Entity vehicle) {
        net.minecraft.server.v1_8_R3.PacketPlayOutAttachEntity attach = new net.minecraft.server.v1_8_R3.PacketPlayOutAttachEntity(0,
                ((org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity) rider).getHandle(),
                ((org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity) vehicle).getHandle());
        for (Player viewer : vehicle.getWorld().getPlayers()) {
            if (!viewer.equals(rider) && viewer.getLocation().distanceSquared(vehicle.getLocation()) <= 128 * 128) {
                PlayerPackets.send(viewer, attach);
            }
        }
    }

    private void launchEffects(Location at, Player rider) {
        World world = at.getWorld();
        if (rider != null) {
            rider.playSound(at, "random.explode", 0.5f, 113 / 63f);
            rider.playSound(at, "fireworks.launch", 0.4f, 75 / 63f);
        }
        // 爆発も本人だけに見える (Bot なら誰にも出さない)
        if (rider != null) {
            PlayerPackets.send(rider, new PacketPlayOutWorldParticles(EnumParticle.EXPLOSION_HUGE, true,
                    (float) at.getX(), (float) at.getY(), (float) at.getZ(), 0, 0, 0, 1, 1));
        }
    }

    private void tick() {
        for (Flight flight : new ArrayList<>(flights.values())) {
            if (!flight.player.isOnline() || flight.player.isDead() || flight.stand.isDead()) {
                land(flight);
                continue;
            }
            if (flight.tick > 0 && !flight.stand.equals(flight.player.getVehicle())) {
                // Shift で降りた: 飛ぶのはやめてその場で止まる (着地点へは運ばない、無敵も終わり)
                abort(flight);
                continue;
            }
            int next = flight.tick + 1;
            if (next >= flight.path.length + flight.extraTicks) {
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
            if (next <= ATTACH_RESEND_TICKS) {
                showRiding(flight.player, flight.stand);
            }
            trail(flight.player, position);
        }
    }

    private static Location position(Flight flight, int tick) {
        double[] point = point(flight.path, tick);
        double along = point[0] * flight.scale;
        return flight.start.clone().add(flight.direction.clone().multiply(along)).add(0, point[1], 0);
    }

    /** path の tick の {水平距離, 高さ}。path より後は最後の 1 tick と同じ速さで進み続ける。 */
    static double[] point(double[][] path, int tick) {
        int last = path.length - 1;
        if (tick <= last) {
            return path[tick];
        }
        double[] before = path[Math.max(0, last - 1)];
        double[] end = path[last];
        int extra = tick - last;
        return new double[]{end[0] + (end[0] - before[0]) * extra, end[1] + (end[1] - before[1]) * extra};
    }

    /** 途中で降りたとき: 台を消すだけで、テレポートはしない。 */
    private void abort(Flight flight) {
        flights.remove(flight.player.getUniqueId());
        flight.landing = true;
        flight.player.setFallDistance(0);
        flight.stand.remove();
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

    /** 台に座って飛んでいる間は無敵 (降りたら終わり)。Bot も同じ。 */
    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        Flight flight = flights.get(player.getUniqueId());
        if ((flight != null && !flight.landing) || plugin.getBotManager().isFlying(player)) {
            event.setCancelled(true);
        }
    }

    /** 飛んでいる途中に降りたら、その場で止まる (1.8 はアーマースタンドでこのイベントが出ないことがあるので tick でも見る)。 */
    @EventHandler(ignoreCancelled = true)
    public void onExit(VehicleExitEvent event) {
        if (event.getExited() instanceof Player) {
            Flight flight = flights.get(event.getExited().getUniqueId());
            if (flight != null && !flight.landing) {
                abort(flight);
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
        // 軌跡は飛んでいる本人だけに見える
        PlayerPackets.send(player, packet);
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
