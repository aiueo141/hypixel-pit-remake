package com.pitremake.map;

import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;

/**
 * Pit の地形の元。サーバーを起動するたびに、ワールドを読む前 (onLoad) にワールドの region を元で上書きする。
 * Spire や KOTH の途中で落ちて地形が変わったまま保存されても、次に起動すれば元に戻る。
 * <pre>
 * 元の場所: plugins/Pit/map-template/region/*.mca (/pit map save で作る) があればそれ、
 *           無ければプラグインの中の maps/&lt;マップ&gt;/region/*.mca (Seasons は録画から作ったマップ)
 * level.dat: ワールドにまだ無ければ maps/&lt;マップ&gt;/level.dat を置く (flat で空気だけ = マップの外は何も生成されない)
 * </pre>
 * なので新しいサーバーにプラグインを入れて起動するだけでマップができる。
 */
public final class MapTemplate {

    private final JavaPlugin plugin;
    private final File template;

    public MapTemplate(JavaPlugin plugin) {
        this.plugin = plugin;
        this.template = new File(plugin.getDataFolder(), "map-template/region");
    }

    /** server.properties の level-name (メインのワールドのフォルダ)。 */
    private File worldFolder() {
        File container = plugin.getServer().getWorldContainer();
        Properties properties = new Properties();
        try (InputStream in = new FileInputStream(new File(container, "server.properties"))) {
            properties.load(in);
        } catch (IOException e) {
            // 初めての起動などで無ければ既定の "world"
        }
        return new File(container, properties.getProperty("level-name", "world"));
    }

    /** config の map (onLoad ではまだ PitPlugin のマップが決まっていないので直接読む)。 */
    private String mapFolder() {
        return "maps/" + plugin.getConfig().getString("map", "Seasons").toLowerCase(Locale.ROOT);
    }

    /** onLoad から: 元でワールドの region を上書きし、level.dat が無ければ置く。 */
    public void restoreOnStartup() {
        if (!plugin.getConfig().getBoolean("map-template.restore-on-start", true)) {
            return;
        }
        File world = worldFolder();
        File region = new File(world, "region");
        if (!region.isDirectory() && !region.mkdirs()) {
            plugin.getLogger().warning("Could not create " + region);
            return;
        }
        try {
            int copied = copyRegions(region);
            if (copied == 0) {
                plugin.getLogger().info("No map for " + mapFolder() + " (plugins/Pit/map-template or inside the plugin).");
                return;
            }
            File levelDat = new File(world, "level.dat");
            if (!levelDat.exists()) {
                InputStream in = plugin.getResource(mapFolder() + "/level.dat");
                if (in != null) {
                    try (InputStream data = in) {
                        Files.copy(data, levelDat.toPath());
                    }
                    plugin.getLogger().info("Created " + levelDat + " (void world around the Pit map).");
                }
            }
            plugin.getLogger().info("Restored the Pit terrain from the map template (" + copied + " region files).");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to restore the map template", e);
        }
    }

    /** 元の region をワールドへコピーする。コピーした数。 */
    private int copyRegions(File region) throws IOException {
        File[] files = template.listFiles((dir, name) -> name.endsWith(".mca"));
        if (files != null && files.length > 0) {
            for (File file : files) {
                Files.copy(file.toPath(), new File(region, file.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return files.length;
        }
        // プラグインの中のマップ (index.txt にファイル名の一覧)
        int count = 0;
        for (String name : bundledRegions()) {
            InputStream in = plugin.getResource(mapFolder() + "/region/" + name);
            if (in == null) {
                continue;
            }
            try (InputStream data = in) {
                Files.copy(data, new File(region, name).toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            count++;
        }
        return count;
    }

    private List<String> bundledRegions() throws IOException {
        List<String> names = new ArrayList<>();
        InputStream in = plugin.getResource(mapFolder() + "/region/index.txt");
        if (in == null) {
            return names;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().endsWith(".mca")) {
                    names.add(line.trim());
                }
            }
        }
        return names;
    }

    /** /pit map save: 今のワールドを保存してから、その region を元としてコピーする。 */
    public int save(World world) throws IOException {
        world.save();
        File region = new File(world.getWorldFolder(), "region");
        File[] files = region.listFiles((dir, name) -> name.endsWith(".mca"));
        if (files == null) {
            return 0;
        }
        if (!template.isDirectory() && !template.mkdirs()) {
            throw new IOException("Could not create " + template);
        }
        for (File file : files) {
            Files.copy(file.toPath(), new File(template, file.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return files.length;
    }
}
