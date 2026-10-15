package com.pitremake.map;

import com.pitremake.PitPlugin;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * マップに紐づくもの (プラグインの中の maps/&lt;マップ&gt;.yml): npcs / spawn-points / holograms。
 * サーバー側 (npcs.yml、config.yml の spawn.points / holograms.lines) に書いてあればそちらが優先。
 */
public final class MapData {

    private MapData() {
    }

    /** 今のマップのデータ (無ければ空)。 */
    public static YamlConfiguration of(JavaPlugin plugin) {
        YamlConfiguration yaml = new YamlConfiguration();
        String map = plugin instanceof PitPlugin ? ((PitPlugin) plugin).getPitMap().getName() : "Seasons";
        String resource = "maps/" + map.toLowerCase(Locale.ROOT) + ".yml";
        InputStream in = plugin.getResource(resource);
        if (in == null) {
            return yaml;
        }
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            yaml.load(reader);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().warning("Failed to load " + resource + ": " + e);
        }
        return yaml;
    }
}
