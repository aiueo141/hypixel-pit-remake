package com.pitremake.hologram;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * マップに浮かぶ文字 (config.yml の holograms.lines: "x,y,z|文字"、色は &amp;)。
 * 初期値は録画 2026-10-14_08-53-15.mcrc の Hypixel の位置 (Mystic Well、Ender Chest、スポーンの看板、解放される機能の一覧)。
 * 名前だけ見える透明なマーカーのアーマースタンドで、起動時に出して停止時に消す。
 */
public final class HologramManager {

    private static final double CLEANUP_RADIUS = 0.2;

    private final JavaPlugin plugin;
    private final List<ArmorStand> stands = new ArrayList<>();

    public HologramManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("holograms.enabled", true)) {
            return;
        }
        String worldName = plugin.getConfig().getString("spawn.world", "");
        World world = worldName.isEmpty() ? null : Bukkit.getWorld(worldName);
        if (world == null) {
            world = Bukkit.getWorlds().get(0);
        }
        // config.yml の holograms.lines があればそれ、無ければプラグインの中の今のマップの holograms
        List<String> lines = plugin.getConfig().isSet("holograms.lines") ? plugin.getConfig().getStringList("holograms.lines")
                : com.pitremake.map.MapData.of(plugin).getStringList("holograms");
        for (String raw : lines) {
            int bar = raw.indexOf('|');
            if (bar < 0) {
                continue;
            }
            String[] xyz = raw.substring(0, bar).split(",");
            if (xyz.length != 3) {
                continue;
            }
            try {
                Location at = new Location(world, Double.parseDouble(xyz[0].trim()), Double.parseDouble(xyz[1].trim()),
                        Double.parseDouble(xyz[2].trim()));
                spawn(at, ChatColor.translateAlternateColorCodes('&', raw.substring(bar + 1)));
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Skipping hologram in config.yml: " + raw);
            }
        }
    }

    public void stop() {
        for (ArmorStand stand : stands) {
            stand.remove();
        }
        stands.clear();
    }

    private void spawn(Location at, String text) {
        at.getChunk().load();
        // 前回消し損ねた同じ場所の文字は消す
        for (Entity entity : at.getWorld().getNearbyEntities(at, CLEANUP_RADIUS, CLEANUP_RADIUS, CLEANUP_RADIUS)) {
            if (entity instanceof ArmorStand && !((ArmorStand) entity).isVisible()) {
                entity.remove();
            }
        }
        ArmorStand stand = (ArmorStand) at.getWorld().spawnEntity(at, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setBasePlate(false);
        stand.setMarker(true);
        stand.setCustomName(text);
        stand.setCustomNameVisible(true);
        stands.add(stand);
    }
}
