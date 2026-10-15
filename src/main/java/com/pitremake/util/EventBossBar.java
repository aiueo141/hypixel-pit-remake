package com.pitremake.util;

import net.minecraft.server.v1_8_R3.EntityWither;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityTeleport;
import net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntityLiving;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * イベントのボスバー (1.8 は透明なウィザーの名前と体力)。録画 2026-10-15_13-57-51 (Spire) / 14-18-51 (KOTH)。
 * <pre>
 * ウィザー: 透明 (flags 32)、名前が見える、パケットだけ (各プレイヤーの前に毎 tick テレポート)
 * 名前    : §5§lMAJOR EVENT! §5§lSPIRE! §7Starting in §e03:00§7 のように、先頭の "MAJOR EVENT!" に光が走る
 *           (1 tick ごとに 1 文字進む白 §f と、後ろ 2 文字の色。14 コマ走って 4 コマ止まる = 18 tick で一周)
 * 体力    : 300 が満タン。1% 単位 (3 ずつ)。半分 (150) 以下は無敵時間 880、それより上は -1
 * </pre>
 */
public final class EventBossBar implements Listener {

    /** バーに出すもの。 */
    public interface Content {
        /** 光の位置 (tick) を含めた名前。 */
        String text(int tick);

        /** 0〜1 (バーの長さ)。 */
        double fraction();
    }

    private static final int CYCLE = 18;
    private static final int RUN = 14;
    private static final double DISTANCE = 25;
    private static final float MAX_HEALTH = 300;

    private final JavaPlugin plugin;
    private final Map<UUID, EntityWither> withers = new HashMap<>();
    private Content content;
    private int tick;
    private BukkitTask task;

    public EventBossBar(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::update, 1, 1);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        hide();
    }

    public void show(Content content) {
        this.content = content;
        tick = 0;
    }

    public void hide() {
        content = null;
        for (Map.Entry<UUID, EntityWither> entry : withers.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null) {
                PlayerPackets.send(player, new PacketPlayOutEntityDestroy(entry.getValue().getId()));
            }
        }
        withers.clear();
    }

    /** 光の走る見出し (録画: KOTH は §d に §9 の尾、Spire は §5 に §d の尾、頭は §f)。 */
    public static String shimmer(String text, String base, String trail, int tick) {
        int k = tick % CYCLE;
        if (k >= RUN) {
            return base + "§l" + text;
        }
        StringBuilder out = new StringBuilder();
        String current = null;
        for (int j = 0; j < text.length(); j++) {
            String color = j == k ? "§f" : j == k - 1 || j == k - 2 ? trail : base;
            if (!color.equals(current)) {
                out.append(color).append("§l");
                current = color;
            }
            out.append(text.charAt(j));
        }
        return out.toString();
    }

    /** 体力 (録画: 1% 単位で 3 ずつ、最低 1)。 */
    public static float health(double fraction) {
        long percent = Math.round(Math.max(0, Math.min(1, fraction)) * 100);
        return Math.max(1, percent * 3);
    }

    private void update() {
        if (content == null) {
            return;
        }
        String name = content.text(tick++);
        float health = health(content.fraction());
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Location eye = player.getEyeLocation();
            Vector ahead = eye.getDirection().multiply(DISTANCE);
            Location at = eye.add(ahead);
            EntityWither wither = withers.get(player.getUniqueId());
            if (wither == null) {
                wither = new EntityWither(((CraftWorld) player.getWorld()).getHandle());
                wither.setInvisible(true);
                wither.setCustomNameVisible(true);
                wither.setCustomName(name);
                wither.setHealth(health);
                wither.getDataWatcher().watch(20, health <= MAX_HEALTH / 2 ? 880 : -1);
                wither.setLocation(at.getX(), at.getY(), at.getZ(), 0, 0);
                withers.put(player.getUniqueId(), wither);
                PlayerPackets.send(player, new PacketPlayOutSpawnEntityLiving(wither));
                continue;
            }
            wither.setCustomName(name);
            wither.setHealth(health);
            wither.getDataWatcher().watch(20, health <= MAX_HEALTH / 2 ? 880 : -1);
            wither.setLocation(at.getX(), at.getY(), at.getZ(), 0, 0);
            PlayerPackets.send(player, new PacketPlayOutEntityTeleport(wither));
            PlayerPackets.send(player, new PacketPlayOutEntityMetadata(wither.getId(), wither.getDataWatcher(), true));
        }
    }

    /** 死んだ・ワールドが変わったときはクライアントからウィザーが消えるので出し直す。 */
    private void respawn(Player player) {
        EntityWither wither = withers.remove(player.getUniqueId());
        if (wither != null) {
            PlayerPackets.send(player, new PacketPlayOutEntityDestroy(wither.getId()));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        withers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        respawn(event.getPlayer());
    }

    @EventHandler
    public void onWorld(PlayerChangedWorldEvent event) {
        respawn(event.getPlayer());
    }
}
