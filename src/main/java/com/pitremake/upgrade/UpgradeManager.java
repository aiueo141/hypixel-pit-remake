package com.pitremake.upgrade;

import com.pitremake.perk.Perk;
import com.pitremake.perk.PerkRules;
import com.pitremake.renown.RenownUpgrade;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * 選んでいるメガストリーク・解放したメガストリーク・Passive の Tier (plugins/Pit/upgrades.yml に保存) と、
 * 保存しない To the Moon の貯めた XP。
 */
public final class UpgradeManager {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Data> players = new HashMap<>();

    private static final class Data {
        Megastreak selected = Megastreak.OVERDRIVE;
        final Set<Megastreak> owned = EnumSet.noneOf(Megastreak.class);
        final Map<Passive, Integer> passives = new EnumMap<>(Passive.class);
        long moonXp;
        /** Items の村人の "Toggle Spawn Items" (初期装備の剣と弓をもらうか)。 */
        boolean spawnItems = true;
        final Perk[] perkSlots = new Perk[PerkRules.SLOTS];
        final Set<Perk> ownedPerks = EnumSet.noneOf(Perk.class);
        /** Renown Shop で解放したパーク (プレステージしても消えない)。 */
        final Set<Perk> renownPerks = EnumSet.noneOf(Perk.class);
        /** Renown Shop の Upgrades の Tier (プレステージしても消えない)。 */
        final Map<RenownUpgrade, Integer> renownUpgrades = new EnumMap<>(RenownUpgrade.class);
        /** Renown Shop の Shop Items / Killstreaks で解放したもの ("item:IRON_PACK" / "streak:HERMIT")。プレステージしても消えない。 */
        final Set<String> unlocks = new java.util.LinkedHashSet<>();
        /** Autobuy に印を付けた Items の村人のアイテム (ShopItem の名前)。 */
        final Set<String> autobuy = new java.util.LinkedHashSet<>();
        /** Uberdrop の "+1% Perma Mystic Chance" を開けた回数 (プレステージしても消えない)。 */
        int permaMystic;
        /** Fancy Hat の色 (RGB)。-1 ならそのままの革の色。 */
        int hatColor = -1;
        /** キルストリークの枠 (#1〜#3)。 */
        final Killstreak[] killstreaks = new Killstreak[3];
        /** ゴールドで買ったキルストリーク (プレステージで消える)。 */
        final Set<Killstreak> ownedKillstreaks = EnumSet.noneOf(Killstreak.class);
        /** Gold Stack / XP Stack で貯まった 1 キルあたりのボーナス (プレステージで消える)。 */
        double goldStack;
        double xpStack;
    }

    public Killstreak getKillstreak(UUID player, int slot) {
        Data data = players.get(player);
        return data == null ? null : data.killstreaks[slot];
    }

    public void setKillstreak(UUID player, int slot, Killstreak killstreak) {
        data(player).killstreaks[slot] = killstreak;
        save();
    }

    public boolean ownsKillstreak(UUID player, Killstreak killstreak) {
        Data data = players.get(player);
        return data != null && data.ownedKillstreaks.contains(killstreak);
    }

    public void buyKillstreak(UUID player, Killstreak killstreak) {
        data(player).ownedKillstreaks.add(killstreak);
        save();
    }

    public double getGoldStack(UUID player) {
        Data data = players.get(player);
        return data == null ? 0 : data.goldStack;
    }

    public double getXpStack(UUID player) {
        Data data = players.get(player);
        return data == null ? 0 : data.xpStack;
    }

    /** Gold Stack / XP Stack を増やす (max まで)。保存はしない (キルのたびに書かない)。 */
    public void addStacks(UUID player, double gold, double goldMax, double xp, double xpMax) {
        Data data = data(player);
        data.goldStack = Math.min(goldMax, data.goldStack + gold);
        data.xpStack = Math.min(xpMax, data.xpStack + xp);
    }

    /** Fancy Hat の色 (決めていなければ null)。 */
    public org.bukkit.Color getHatColor(UUID player) {
        Data data = players.get(player);
        return data == null || data.hatColor < 0 ? null : org.bukkit.Color.fromRGB(data.hatColor);
    }

    /** Fancy Hat の色を決める (null で元の革の色)。 */
    public void setHatColor(UUID player, org.bukkit.Color color) {
        data(player).hatColor = color == null ? -1 : color.asRGB();
        save();
    }

    public int getPermaMystic(UUID player) {
        Data data = players.get(player);
        return data == null ? 0 : data.permaMystic;
    }

    /** Perma Mystic Chance を 1 増やす (max まで)。 */
    public void addPermaMystic(UUID player, int max) {
        Data data = data(player);
        data.permaMystic = Math.min(max, data.permaMystic + 1);
        save();
    }

    /** Renown Shop の Shop Items / Killstreaks の解放 (key は "item:…" か "streak:…")。 */
    public boolean isUnlocked(UUID player, String key) {
        Data data = players.get(player);
        return data != null && data.unlocks.contains(key);
    }

    /** 解放の印の切り替え (Night Quests のオン・オフなど)。付いたら true。 */
    public boolean toggleFlag(UUID player, String key) {
        Data data = data(player);
        boolean on = data.unlocks.add(key) || !data.unlocks.remove(key);
        save();
        return on;
    }

    public void unlock(UUID player, String key) {
        data(player).unlocks.add(key);
        save();
    }

    /** Autobuy の印の切り替え。付いたら true。 */
    public boolean toggleAutobuy(UUID player, String item) {
        Data data = data(player);
        boolean on = data.autobuy.add(item) || !data.autobuy.remove(item);
        save();
        return on;
    }

    public boolean isAutobuy(UUID player, String item) {
        Data data = players.get(player);
        return data != null && data.autobuy.contains(item);
    }

    public java.util.Collection<String> autobuyItems(UUID player) {
        Data data = players.get(player);
        return data == null ? java.util.Collections.<String>emptyList() : new ArrayList<>(data.autobuy);
    }

    /** Passive の買える最後の Tier (One Upper で 6)。 */
    public int maxPassiveTier(UUID player) {
        return hasRenown(player, RenownUpgrade.ONE_UPPER) ? Passive.MAX_TIER : Passive.BASE_MAX_TIER;
    }

    public int getRenownTier(UUID player, RenownUpgrade upgrade) {
        Data data = players.get(player);
        Integer tier = data == null ? null : data.renownUpgrades.get(upgrade);
        return tier == null ? 0 : tier;
    }

    public boolean hasRenown(UUID player, RenownUpgrade upgrade) {
        return getRenownTier(player, upgrade) > 0;
    }

    public void setRenownTier(UUID player, RenownUpgrade upgrade, int tier) {
        data(player).renownUpgrades.put(upgrade, tier);
        save();
    }

    /** 買った Renown Upgrades の Tier の合計 (Renown Shop の "Unlocked: 28/89")。 */
    public int renownTierCount(UUID player) {
        Data data = players.get(player);
        int total = 0;
        if (data != null) {
            for (int tier : data.renownUpgrades.values()) {
                total += tier;
            }
        }
        return total;
    }

    /** その Perk Slot (0 から) に入っているパーク (空なら null)。 */
    public Perk getPerk(UUID player, int slot) {
        Data data = players.get(player);
        return data == null ? null : data.perkSlots[slot];
    }

    public void setPerk(UUID player, int slot, Perk perk) {
        data(player).perkSlots[slot] = perk;
        save();
    }

    /** どれかの Perk Slot に入っているか。 */
    public boolean hasPerk(UUID player, Perk perk) {
        Data data = players.get(player);
        if (data == null) {
            return false;
        }
        for (Perk selected : data.perkSlots) {
            if (selected == perk) {
                return true;
            }
        }
        return false;
    }

    public boolean ownsPerk(UUID player, Perk perk) {
        Data data = players.get(player);
        return data != null && data.ownedPerks.contains(perk);
    }

    public void buyPerk(UUID player, Perk perk) {
        data(player).ownedPerks.add(perk);
        save();
    }

    /** Renown パークなら解放済みか (普通のパークは常に true)。 */
    public boolean isPerkUnlocked(UUID player, Perk perk) {
        if (!perk.isRenownPerk()) {
            return true;
        }
        Data data = players.get(player);
        return data != null && data.renownPerks.contains(perk);
    }

    public void unlockRenownPerk(UUID player, Perk perk) {
        data(player).renownPerks.add(perk);
        save();
    }

    public int renownPerkCount(UUID player) {
        Data data = players.get(player);
        return data == null ? 0 : data.renownPerks.size();
    }

    public boolean hasSpawnItems(UUID player) {
        Data data = players.get(player);
        return data == null || data.spawnItems;
    }

    public void setSpawnItems(UUID player, boolean enabled) {
        data(player).spawnItems = enabled;
        save();
    }

    /** プレステージしたとき: Passive・パーク・解放したメガストリークを消して Overdrive に戻す (Renown の解放は残す)。 */
    public void resetForPrestige(UUID player) {
        Data data = data(player);
        data.passives.clear();
        data.ownedPerks.clear();
        java.util.Arrays.fill(data.perkSlots, null);
        data.owned.clear();
        data.selected = Megastreak.OVERDRIVE;
        java.util.Arrays.fill(data.killstreaks, null);
        data.ownedKillstreaks.clear();
        data.goldStack = 0;
        data.xpStack = 0;
        data.moonXp = 0;
        save();
    }

    public UpgradeManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "upgrades.yml");
    }

    private Data data(UUID player) {
        Data data = players.get(player);
        if (data == null) {
            data = new Data();
            players.put(player, data);
        }
        return data;
    }

    public Megastreak getMegastreak(UUID player) {
        Data data = players.get(player);
        return data == null ? Megastreak.OVERDRIVE : data.selected;
    }

    public void selectMegastreak(UUID player, Megastreak megastreak) {
        data(player).selected = megastreak;
        save();
    }

    public boolean owns(UUID player, Megastreak megastreak) {
        Data data = players.get(player);
        return megastreak.isDefault() || (data != null && data.owned.contains(megastreak));
    }

    public void unlock(UUID player, Megastreak megastreak) {
        data(player).owned.add(megastreak);
        save();
    }

    public int getTier(UUID player, Passive passive) {
        Data data = players.get(player);
        Integer tier = data == null ? null : data.passives.get(passive);
        return tier == null ? 0 : tier;
    }

    public void setTier(UUID player, Passive passive, int tier) {
        data(player).passives.put(passive, tier);
        save();
    }

    public long getMoonXp(UUID player) {
        Data data = players.get(player);
        return data == null ? 0 : data.moonXp;
    }

    public void addMoonXp(UUID player, long xp) {
        data(player).moonXp += xp;
    }

    /** 貯めた XP を返して 0 にする。 */
    public long takeMoonXp(UUID player) {
        Data data = players.get(player);
        if (data == null) {
            return 0;
        }
        long xp = data.moonXp;
        data.moonXp = 0;
        return xp;
    }

    public void load() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            Data data = new Data();
            Megastreak selected = Megastreak.fromName(section.getString("megastreak", ""));
            if (selected != null) {
                data.selected = selected;
            }
            for (String name : section.getStringList("owned")) {
                Megastreak megastreak = Megastreak.fromName(name);
                if (megastreak != null) {
                    data.owned.add(megastreak);
                }
            }
            data.spawnItems = section.getBoolean("spawn-items", true);
            List<String> slots = section.getStringList("perk-slots");
            for (int i = 0; i < slots.size() && i < PerkRules.SLOTS; i++) {
                data.perkSlots[i] = Perk.fromName(slots.get(i));
            }
            for (String name : section.getStringList("perks")) {
                Perk perk = Perk.fromName(name);
                if (perk != null) {
                    data.ownedPerks.add(perk);
                }
            }
            for (String name : section.getStringList("renown-perks")) {
                Perk perk = Perk.fromName(name);
                if (perk != null) {
                    data.renownPerks.add(perk);
                }
            }
            data.unlocks.addAll(section.getStringList("unlocks"));
            data.autobuy.addAll(section.getStringList("autobuy"));
            data.permaMystic = section.getInt("perma-mystic");
            data.hatColor = section.getInt("hat-color", -1);
            List<String> killstreakSlots = section.getStringList("killstreaks");
            for (int i = 0; i < killstreakSlots.size() && i < data.killstreaks.length; i++) {
                data.killstreaks[i] = Killstreak.fromName(killstreakSlots.get(i));
            }
            for (String name : section.getStringList("owned-killstreaks")) {
                Killstreak killstreak = Killstreak.fromName(name);
                if (killstreak != null) {
                    data.ownedKillstreaks.add(killstreak);
                }
            }
            data.goldStack = section.getDouble("gold-stack");
            data.xpStack = section.getDouble("xp-stack");
            ConfigurationSection renown = section.getConfigurationSection("renown-upgrades");
            if (renown != null) {
                for (String name : renown.getKeys(false)) {
                    RenownUpgrade upgrade = RenownUpgrade.fromName(name);
                    if (upgrade != null && renown.getInt(name) > 0) {
                        data.renownUpgrades.put(upgrade, Math.min(renown.getInt(name), upgrade.getMaxTier()));
                    }
                }
            }
            ConfigurationSection passives = section.getConfigurationSection("passives");
            if (passives != null) {
                for (Passive passive : Passive.values()) {
                    int tier = passives.getInt(passive.name());
                    if (tier > 0) {
                        data.passives.put(passive, Math.min(tier, Passive.MAX_TIER));
                    }
                }
            }
            players.put(UUID.fromString(key), data);
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Data> entry : players.entrySet()) {
            String key = entry.getKey().toString();
            Data data = entry.getValue();
            yaml.set(key + ".megastreak", data.selected.name());
            List<String> owned = new ArrayList<>();
            for (Megastreak megastreak : data.owned) {
                owned.add(megastreak.name());
            }
            yaml.set(key + ".owned", owned);
            yaml.set(key + ".spawn-items", data.spawnItems);
            List<String> slots = new ArrayList<>();
            for (Perk perk : data.perkSlots) {
                slots.add(perk == null ? "" : perk.name());
            }
            yaml.set(key + ".perk-slots", slots);
            yaml.set(key + ".perks", names(data.ownedPerks));
            yaml.set(key + ".renown-perks", names(data.renownPerks));
            yaml.set(key + ".unlocks", new ArrayList<>(data.unlocks));
            yaml.set(key + ".autobuy", new ArrayList<>(data.autobuy));
            yaml.set(key + ".perma-mystic", data.permaMystic);
            yaml.set(key + ".hat-color", data.hatColor);
            List<String> killstreakSlots = new ArrayList<>();
            for (Killstreak killstreak : data.killstreaks) {
                killstreakSlots.add(killstreak == null ? "" : killstreak.name());
            }
            yaml.set(key + ".killstreaks", killstreakSlots);
            List<String> ownedKillstreaks = new ArrayList<>();
            for (Killstreak killstreak : data.ownedKillstreaks) {
                ownedKillstreaks.add(killstreak.name());
            }
            yaml.set(key + ".owned-killstreaks", ownedKillstreaks);
            yaml.set(key + ".gold-stack", data.goldStack);
            yaml.set(key + ".xp-stack", data.xpStack);
            for (Map.Entry<RenownUpgrade, Integer> renown : data.renownUpgrades.entrySet()) {
                yaml.set(key + ".renown-upgrades." + renown.getKey().name(), renown.getValue());
            }
            for (Map.Entry<Passive, Integer> passive : data.passives.entrySet()) {
                yaml.set(key + ".passives." + passive.getKey().name(), passive.getValue());
            }
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save upgrades.yml", e);
        }
    }

    private static List<String> names(Set<Perk> perks) {
        List<String> names = new ArrayList<>();
        for (Perk perk : perks) {
            names.add(perk.name());
        }
        return names;
    }
}
