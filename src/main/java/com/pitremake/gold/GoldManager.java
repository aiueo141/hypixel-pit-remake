package com.pitremake.gold;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/** プレイヤーの所持ゴールドと、そのプレステージ中に稼いだゴールド。plugins/Pit/gold.yml に保存する。 */
public final class GoldManager {

    private static final DecimalFormat FORMAT = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat EXACT_FORMAT = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat WHOLE_FORMAT = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));
    private static final double WHOLE_NUMBER_FROM = 10_000;

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Double> balances = new HashMap<>();
    /** このプレステージ中に稼いだゴールド (gold.yml の grinded)。 */
    private final Map<UUID, Double> grinded = new HashMap<>();
    private static final String GRINDED_SECTION = "grinded";

    public GoldManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "gold.yml");
    }

    public void load() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            if (key.equals(GRINDED_SECTION)) {
                for (String id : yaml.getConfigurationSection(key).getKeys(false)) {
                    grinded.put(UUID.fromString(id), yaml.getDouble(key + "." + id));
                }
                continue;
            }
            balances.put(UUID.fromString(key), yaml.getDouble(key));
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
            yaml.set(entry.getKey().toString(), entry.getValue());
        }
        for (Map.Entry<UUID, Double> entry : grinded.entrySet()) {
            yaml.set(GRINDED_SECTION + "." + entry.getKey(), entry.getValue());
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save gold.yml", e);
        }
    }

    public double get(UUID player) {
        Double balance = balances.get(player);
        return balance == null ? 0 : balance;
    }

    public void set(UUID player, double amount) {
        balances.put(player, Math.max(0, amount));
        save();
    }

    /** 稼いだゴールド (プレステージの条件の「稼いだ量」にも数える)。 */
    public void add(UUID player, double amount) {
        if (amount > 0) {
            Double total = grinded.get(player);
            grinded.put(player, (total == null ? 0 : total) + amount);
        }
        set(player, get(player) + amount);
    }

    /** このプレステージ中に稼いだゴールド。 */
    public double getGrinded(UUID player) {
        Double total = grinded.get(player);
        return total == null ? 0 : total;
    }

    /** プレステージしたとき: 所持金と稼いだ量を 0 に。 */
    public void resetForPrestige(UUID player) {
        grinded.remove(player);
        set(player, 0);
    }

    /** 足りていれば引いて true を返す。 */
    public boolean take(UUID player, double amount) {
        if (get(player) < amount) {
            return false;
        }
        set(player, get(player) - amount);
        return true;
    }

    /** 例: 1,000g */
    public static String format(double amount) {
        return FORMAT.format(amount) + "g";
    }

    /**
     * 小数点以下 2 桁まで出す (スコアボードやキルのメッセージ)。例: 23.70g。
     * 10,000g 以上は小数点以下を出さない (録画の Hypixel: 23.70g / 912,364,354g)。
     */
    public static String formatExact(double amount) {
        return amount >= WHOLE_NUMBER_FROM ? WHOLE_FORMAT.format(Math.floor(amount)) + "g" : EXACT_FORMAT.format(amount) + "g";
    }
}
