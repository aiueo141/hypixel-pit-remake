package com.pitremake.stats;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * プレイヤーの XP・キル数 (plugins/Pit/stats.yml に保存) と、保存しないキルストリーク・戦闘中の判定。
 */
public final class StatsManager {

    /** 最後に殴った / 殴られてからこの時間は "Fighting" (実験的)。 */
    static final long COMBAT_MILLIS = 10_000;

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Long> xp = new HashMap<>();
    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, Double> streaks = new HashMap<>();
    private final Map<UUID, Long> lastCombat = new HashMap<>();
    private final Map<UUID, Integer> prestiges = new HashMap<>();
    private final Map<UUID, Integer> renown = new HashMap<>();

    public StatsManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
    }

    public void load() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            UUID id = UUID.fromString(key);
            xp.put(id, section.getLong("xp"));
            kills.put(id, section.getInt("kills"));
            if (section.getInt("prestige") > 0) {
                prestiges.put(id, section.getInt("prestige"));
            }
            if (section.getInt("renown") > 0) {
                renown.put(id, section.getInt("renown"));
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Long> entry : xp.entrySet()) {
            yaml.set(entry.getKey() + ".xp", entry.getValue());
            yaml.set(entry.getKey() + ".kills", getKills(entry.getKey()));
            yaml.set(entry.getKey() + ".prestige", getPrestige(entry.getKey()));
            yaml.set(entry.getKey() + ".renown", getRenown(entry.getKey()));
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save stats.yml", e);
        }
    }

    public long getXp(UUID player) {
        Long value = xp.get(player);
        return value == null ? 0 : value;
    }

    /**
     * 今までに稼いだ XP (リーダーボード・My pit stats の XP)。プレステージで XP は 0 に戻るので、
     * それまでのプレステージはレベル 120 まで貯めた分として足す。
     */
    public long getLifetimeXp(UUID player) {
        return PitLevels.lifetimeXp(getPrestige(player), getXp(player));
    }

    /** XP の記録があるプレイヤー。 */
    public Set<UUID> players() {
        return Collections.unmodifiableSet(new HashSet<>(xp.keySet()));
    }

    public int getLevel(UUID player) {
        return PitLevels.levelOf(getXp(player), getPrestige(player));
    }

    /** 次のレベルまでに足りない XP (プレステージの倍率込み)。 */
    public long getNeededXp(UUID player) {
        return PitLevels.neededXp(getXp(player), getPrestige(player));
    }

    public float getProgress(UUID player) {
        return PitLevels.progress(getXp(player), getPrestige(player));
    }

    public int getPrestige(UUID player) {
        Integer value = prestiges.get(player);
        return value == null ? 0 : value;
    }

    public int getRenown(UUID player) {
        Integer value = renown.get(player);
        return value == null ? 0 : value;
    }

    public void setRenown(UUID player, int amount) {
        renown.put(player, Math.max(0, amount));
        if (!xp.containsKey(player)) {
            // 保存は XP のあるプレイヤーだけなので、まだ無ければ 0 で作る
            xp.put(player, 0L);
        }
        save();
    }

    /** Renown を払う (足りなければ何もせず false)。 */
    public boolean takeRenown(UUID player, int amount) {
        if (getRenown(player) < amount) {
            return false;
        }
        setRenown(player, getRenown(player) - amount);
        return true;
    }

    /** チャットの名前の前 (例: §9[§e§eI§9-§6§l67§9])。 */
    public String chatTag(UUID player) {
        return PrestigeRules.chatTag(getPrestige(player), getLevel(player));
    }

    /** tab・サイドバー (例: §9[§6§l67§9])。 */
    public String levelTag(UUID player) {
        return PrestigeRules.levelTag(getPrestige(player), getLevel(player));
    }

    /** プレステージする: プレステージ +1、XP 0 (レベル 1)、Renown を足す。 */
    public void prestige(UUID player, int renownReward) {
        prestiges.put(player, Math.min(getPrestige(player) + 1, PrestigeRules.MAX_PRESTIGE));
        renown.put(player, getRenown(player) + renownReward);
        xp.put(player, 0L);
        save();
    }

    /** 確認用 (/pitprestige): プレステージを設定し、レベルはそのまま (新しいプレステージの必要 XP で数え直す)。 */
    public void setPrestige(UUID player, int prestige) {
        int level = getLevel(player);
        int value = Math.max(0, Math.min(prestige, PrestigeRules.MAX_PRESTIGE));
        if (value == 0) {
            prestiges.remove(player);
        } else {
            prestiges.put(player, value);
        }
        xp.put(player, PitLevels.totalXpFor(level, value));
        save();
    }

    /** 最大レベルを超える分は捨てる。 */
    public void setXp(UUID player, long amount) {
        long max = PitLevels.totalXpFor(PitLevels.MAX_LEVEL, getPrestige(player));
        xp.put(player, Math.max(0, Math.min(amount, max)));
        save();
    }

    public void addXp(UUID player, long amount) {
        setXp(player, getXp(player) + amount);
    }

    public int getKills(UUID player) {
        Integer value = kills.get(player);
        return value == null ? 0 : value;
    }

    /** キル数とストリークを 1 増やす (保存は XP と一緒に行う)。 */
    public void addKill(UUID player) {
        kills.put(player, getKills(player) + 1);
        addStreak(player, 1);
        xp.putIfAbsent(player, 0L);
    }

    /** ストリーク (整数部分。キルの数として使う)。 */
    public int getStreak(UUID player) {
        return (int) Math.floor(getExactStreak(player) + 1e-9);
    }

    /** アシストの参加分 (Assistant (to the) Streaker) も含めたストリーク。 */
    public double getExactStreak(UUID player) {
        Double value = streaks.get(player);
        return value == null ? 0 : value;
    }

    public void addStreak(UUID player, double amount) {
        streaks.put(player, getExactStreak(player) + amount);
    }

    /** 確認用 (/pitstreak)。 */
    public void setStreak(UUID player, int streak) {
        if (streak <= 0) {
            streaks.remove(player);
        } else {
            streaks.put(player, (double) streak);
        }
    }

    public void resetStreak(UUID player) {
        streaks.remove(player);
        lastCombat.remove(player);
    }

    public void tagCombat(UUID player) {
        lastCombat.put(player, System.currentTimeMillis());
    }

    /** 戦闘中の残りの秒数 (切り上げ。戦闘中でなければ 0)。 */
    public int combatSecondsLeft(UUID player, long extraMillis) {
        Long last = lastCombat.get(player);
        if (last == null) {
            return 0;
        }
        long left = COMBAT_MILLIS + extraMillis - (System.currentTimeMillis() - last);
        return left <= 0 ? 0 : (int) Math.ceil(left / 1000.0);
    }

    public boolean isFighting(UUID player) {
        return isFighting(player, 0);
    }

    /** extraMillis: 戦闘中の時間の延長 (バウンティ 1,000g ごとに 5 秒)。 */
    public boolean isFighting(UUID player, long extraMillis) {
        Long last = lastCombat.get(player);
        return last != null && System.currentTimeMillis() - last < COMBAT_MILLIS + extraMillis;
    }
}
