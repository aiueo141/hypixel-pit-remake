package com.pitremake.spawn;

import com.pitremake.PitPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Pit のスポーン (config.yml の spawn)。
 * <pre>
 * 復活地点 : spawn.points からランダム (録画 2026-10-14_07-53-32.mcrc の Hypixel の復活地点が初期値)。
 *            空ならワールドのスポーン。spawn.teleport-on-join なら参加したときもそこへ移動する。
 *            死亡画面は出さずにすぐ復活させる
 * 攻撃不可 : spawn.protection の中心から半径 radius 以内、高さ min-y 以上にいるプレイヤーは
 *            攻撃できず、攻撃もされない (Hypixel のスポーンは Y=114、下の戦う場所は Y=82 くらい)
 * </pre>
 */
public final class PitSpawn implements Listener {

    private final PitPlugin plugin;

    public PitSpawn(PitPlugin plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration config() {
        return plugin.getConfig();
    }

    private World world() {
        String name = config().getString("spawn.world", "");
        World world = name.isEmpty() ? null : Bukkit.getWorld(name);
        return world != null ? world : Bukkit.getWorlds().get(0);
    }

    /** 復活地点の一覧 ("x,y,z,yaw")。 */
    public List<Location> points() {
        List<Location> points = new ArrayList<>();
        for (String raw : rawPoints()) {
            Location point = parse(world(), raw);
            if (point != null) {
                points.add(point);
            }
        }
        return points;
    }

    /** KOTH 中の丘の位置 {x, z} (無ければ null)。その間はここに一番近い復活地点だけを使う。 */
    private double[] focus;

    public void setFocus(double[] focus) {
        this.focus = focus;
    }

    /** ランダムな復活地点 (無ければワールドのスポーン)。KOTH 中は丘に一番近い 1 か所だけ。 */
    public Location randomPoint() {
        List<Location> points = points();
        if (points.isEmpty()) {
            return world().getSpawnLocation();
        }
        if (focus != null) {
            return nearest(points, focus[0], focus[1]).clone();
        }
        return points.get(ThreadLocalRandom.current().nextInt(points.size()));
    }

    static Location nearest(List<Location> points, double x, double z) {
        Location best = points.get(0);
        for (Location point : points) {
            if (Math.hypot(point.getX() - x, point.getZ() - z) < Math.hypot(best.getX() - x, best.getZ() - z)) {
                best = point;
            }
        }
        return best;
    }

    /** 向きは 45° 単位に揃える。 */
    /** config.yml の spawn.points (ゲーム内で足したとき) があればそれ、無ければプラグインの中の今のマップの spawn-points。 */
    private List<String> rawPoints() {
        if (config().isSet("spawn.points")) {
            return new ArrayList<>(config().getStringList("spawn.points"));
        }
        return new ArrayList<>(com.pitremake.map.MapData.of(plugin).getStringList("spawn-points"));
    }

    public void addPoint(Location location) {
        List<String> raw = rawPoints();
        Location point = location.clone();
        point.setYaw(snapYaw(location.getYaw()));
        raw.add(format(point));
        config().set("spawn.world", location.getWorld().getName());
        config().set("spawn.points", raw);
        plugin.saveConfig();
    }

    public void clearPoints() {
        config().set("spawn.points", new ArrayList<String>());
        plugin.saveConfig();
    }

    /** 攻撃不可の範囲を設定する (中心は location の x / z)。 */
    public void setProtection(Location center, double radius, double minY) {
        config().set("spawn.protection.center-x", center.getX());
        config().set("spawn.protection.center-z", center.getZ());
        config().set("spawn.protection.radius", radius);
        config().set("spawn.protection.min-y", minY);
        plugin.saveConfig();
    }

    public String describeProtection() {
        return String.format("center (%.1f, %.1f) radius %.1f min-y %.1f", centerX(), centerZ(), radius(), minY());
    }

    private double centerX() {
        return config().getDouble("spawn.protection.center-x", 0.5);
    }

    private double centerZ() {
        return config().getDouble("spawn.protection.center-z", 0.5);
    }

    private double radius() {
        return config().getDouble("spawn.protection.radius", 25);
    }

    private double minY() {
        return config().getDouble("spawn.protection.min-y", 105);
    }

    /** 攻撃不可の範囲の中心 (高さは location のまま)。Bot はスポーンにいる間ここへ向かう。 */
    public Location center(Location location) {
        return new Location(location.getWorld(), centerX(), location.getY(), centerZ());
    }

    /** 45° 単位、-180 より大きく 180 以下。 */
    static float snapYaw(float yaw) {
        float snapped = Math.round(yaw / 45f) * 45f;
        snapped = ((snapped % 360) + 360) % 360;
        return snapped > 180 ? snapped - 360 : snapped;
    }

    /** 攻撃不可の範囲の中か。 */
    public boolean isProtected(Location location) {
        if (!config().getBoolean("spawn.protection.enabled", true) || !location.getWorld().equals(world())) {
            return false;
        }
        return isInside(location.getX(), location.getY(), location.getZ(), centerX(), centerZ(), radius(), minY());
    }

    static boolean isInside(double x, double y, double z, double centerX, double centerZ, double radius, double minY) {
        double dx = x - centerX;
        double dz = z - centerZ;
        return y >= minY && dx * dx + dz * dz <= radius * radius;
    }

    /** 例: "9.5,114,0.5,90" */
    static Location parse(World world, String raw) {
        String[] parts = raw.split(",");
        if (parts.length < 3) {
            return null;
        }
        try {
            float yaw = parts.length > 3 ? Float.parseFloat(parts[3].trim()) : 0;
            return new Location(world, Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim()),
                    Double.parseDouble(parts[2].trim()), yaw, 0);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static String format(Location location) {
        return String.format(java.util.Locale.US, "%.2f,%.2f,%.2f,%.1f",
                location.getX(), location.getY(), location.getZ(), location.getYaw());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        event.setRespawnLocation(randomPoint());
    }

    /** 死亡画面を出さずにすぐ復活させる (Hypixel も死んだ瞬間にスポーンへ戻る)。Bot は自分で復活する。 */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (plugin.getBotManager().isBot(player)) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && player.isDead()) {
                player.spigot().respawn();
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (config().getBoolean("spawn.teleport-on-join", true)) {
            event.getPlayer().teleport(randomPoint());
        }
    }

    /** スポーンの中ではプレイヤーを攻撃できない (弓も)。中にいるプレイヤーも攻撃されない。 */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player attacker = attacker(event.getDamager());
        if (attacker == null || attacker.equals(event.getEntity())) {
            return;
        }
        if (isProtected(event.getEntity().getLocation()) || isProtected(attacker.getLocation())) {
            event.setCancelled(true);
        }
    }

    private static Player attacker(Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        }
        if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof Player) {
            return (Player) ((Projectile) damager).getShooter();
        }
        return null;
    }
}
