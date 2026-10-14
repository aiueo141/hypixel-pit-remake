package com.pitremake.well;

import com.pitremake.util.NbtUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * プレイヤーごとに Mystic Well に置かれているアイテム (中央のアイテムと、生贄のパンツ)。
 * GUI を閉じても井戸の中に残る。plugins/Pit/well.yml に NBT ごと保存する。
 * <pre>
 * items:      { uuid: base64 }
 * sacrifices: { uuid: base64 }
 * </pre>
 */
public final class MysticWellStorage {

    private static final String ITEMS = "items";
    private static final String SACRIFICES = "sacrifices";

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, ItemStack> items = new HashMap<>();
    private final Map<UUID, ItemStack> sacrifices = new HashMap<>();

    public MysticWellStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "well.yml");
    }

    public void load() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        loadSection(yaml.getConfigurationSection(ITEMS), items);
        loadSection(yaml.getConfigurationSection(SACRIFICES), sacrifices);
        // 旧形式 (トップレベルに uuid: base64) も読み込む
        for (String key : yaml.getKeys(false)) {
            if (yaml.isString(key)) {
                loadEntry(key, yaml.getString(key), items);
            }
        }
    }

    private void loadSection(ConfigurationSection section, Map<UUID, ItemStack> target) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            loadEntry(key, section.getString(key), target);
        }
    }

    private void loadEntry(String key, String data, Map<UUID, ItemStack> target) {
        try {
            target.put(UUID.fromString(key), NbtUtil.fromBase64(data));
        } catch (IOException | IllegalArgumentException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load Mystic Well item of " + key, e);
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        saveSection(yaml, ITEMS, items);
        saveSection(yaml, SACRIFICES, sacrifices);
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save well.yml", e);
        }
    }

    private void saveSection(YamlConfiguration yaml, String path, Map<UUID, ItemStack> source) {
        for (Map.Entry<UUID, ItemStack> entry : source.entrySet()) {
            try {
                yaml.set(path + "." + entry.getKey(), NbtUtil.toBase64(entry.getValue()));
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save Mystic Well item of " + entry.getKey(), e);
            }
        }
    }

    /** 中央に置かれているアイテム。無ければ null。 */
    public ItemStack get(UUID player) {
        return copy(items.get(player));
    }

    /** null を渡すと空にする。変更のたびにファイルへ保存する。 */
    public void set(UUID player, ItemStack item) {
        put(items, player, item);
    }

    /** 生贄として置かれているパンツ。無ければ null。 */
    public ItemStack getSacrifice(UUID player) {
        return copy(sacrifices.get(player));
    }

    /** null を渡すと空にする。変更のたびにファイルへ保存する。 */
    public void setSacrifice(UUID player, ItemStack item) {
        put(sacrifices, player, item);
    }

    private void put(Map<UUID, ItemStack> target, UUID player, ItemStack item) {
        if (item == null) {
            target.remove(player);
        } else {
            target.put(player, item.clone());
        }
        save();
    }

    private static ItemStack copy(ItemStack item) {
        return item == null ? null : item.clone();
    }
}
