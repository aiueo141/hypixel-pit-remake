package com.pitremake.rank;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/** プレイヤーが選んだ見た目のランク・+ の色・MVP++ の色 (plugins/Pit/ranks.yml)。 */
public final class RankManager {

    /** MVP+ / MVP++ の + に選べる色 (/rankcolor と同じ 14 色)。最初は赤。 */
    public static final List<ChatColor> PLUS_COLORS = Collections.unmodifiableList(Arrays.asList(
            ChatColor.RED, ChatColor.GOLD, ChatColor.GREEN, ChatColor.YELLOW, ChatColor.LIGHT_PURPLE,
            ChatColor.WHITE, ChatColor.BLUE, ChatColor.DARK_GREEN, ChatColor.DARK_RED, ChatColor.DARK_AQUA,
            ChatColor.DARK_PURPLE, ChatColor.DARK_GRAY, ChatColor.BLACK, ChatColor.DARK_BLUE));

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Data> players = new HashMap<>();

    private static final class Data {
        Rank rank = Rank.NONE;
        ChatColor plus = ChatColor.RED;
        boolean aqua;
    }

    public RankManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "ranks.yml");
    }

    private Data data(UUID player) {
        Data data = players.get(player);
        if (data == null) {
            data = new Data();
            players.put(player, data);
        }
        return data;
    }

    public Rank getRank(UUID player) {
        Data data = players.get(player);
        return data == null ? Rank.NONE : data.rank;
    }

    public ChatColor getPlusColor(UUID player) {
        Data data = players.get(player);
        return data == null ? ChatColor.RED : data.plus;
    }

    public boolean isAqua(UUID player) {
        Data data = players.get(player);
        return data != null && data.aqua;
    }

    public void setRank(UUID player, Rank rank) {
        data(player).rank = rank;
        save();
    }

    public void setPlusColor(UUID player, ChatColor color) {
        data(player).plus = color;
        save();
    }

    public void setAqua(UUID player, boolean aqua) {
        data(player).aqua = aqua;
        save();
    }

    /** 名前の前のランク表示 (Non は "")。 */
    public String prefix(UUID player) {
        return getRank(player).prefix(getPlusColor(player), isAqua(player));
    }

    /** 名前の色。 */
    public String nameColor(UUID player) {
        return getRank(player).nameColor(isAqua(player));
    }

    public void load() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            Data data = new Data();
            Rank rank = Rank.fromName(section.getString("rank", ""));
            if (rank != null) {
                data.rank = rank;
            }
            try {
                data.plus = ChatColor.valueOf(section.getString("plus", "RED"));
            } catch (IllegalArgumentException e) {
                data.plus = ChatColor.RED;
            }
            data.aqua = section.getBoolean("aqua");
            players.put(UUID.fromString(key), data);
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Data> entry : players.entrySet()) {
            yaml.set(entry.getKey() + ".rank", entry.getValue().rank.name());
            yaml.set(entry.getKey() + ".plus", entry.getValue().plus.name());
            yaml.set(entry.getKey() + ".aqua", entry.getValue().aqua);
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save ranks.yml", e);
        }
    }
}
