package com.pitremake.stats;

import com.pitremake.PitPlugin;
import com.pitremake.data.PitStat;
import com.pitremake.data.PlayerProfiles;
import com.pitremake.util.ClickMenu;
import com.pitremake.util.Menus;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

/**
 * STATS の NPC の画面 "My pit stats" (27)。録画 2026-10-15_10-32-13.mcrc の配置と文字。
 * <pre>
 * 10 Offensive Stats (鉄の剣) / 12 Defensive Stats (鉄のチェストプレート) / 14 Performance Stats (小麦) / 16 Miscellaneous Stats (黒曜石)
 * 割合は小数 3 桁まで (末尾の 0 は消す)。1 時間あたりはプレイ時間の時間数 (切り捨て) で割る (録画: 337,409 / 455 = 741.558)
 * </pre>
 * このリメイクに無いもの (Contracts、King's Quest、釣り、農業、Sewer) は 0。
 */
public final class PitStatsMenu implements ClickMenu {

    private static final int OFFENSIVE_SLOT = 10;
    private static final int DEFENSIVE_SLOT = 12;
    private static final int PERFORMANCE_SLOT = 14;
    private static final int MISC_SLOT = 16;
    private static final DecimalFormat RATIO = new DecimalFormat("#,##0.###", DecimalFormatSymbols.getInstance(Locale.US));

    static {
        RATIO.setRoundingMode(RoundingMode.HALF_EVEN);
    }

    private final PitPlugin plugin;
    private final Player player;
    private final Inventory inventory;

    public PitStatsMenu(PitPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 27, "My pit stats");
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        render();
        player.openInventory(inventory);
    }

    private void render() {
        UUID id = player.getUniqueId();
        StatsManager stats = plugin.getStatsManager();
        PlayerProfiles profiles = plugin.getPlayerProfiles();
        PlayerProfiles.Profile profile = profiles.get(id);
        long kills = stats.getKills(id);
        long assists = profile.assists;
        long deaths = profile.deaths;
        double melee = profiles.get(id, PitStat.MELEE_DAMAGE_DEALT);
        double bow = profiles.get(id, PitStat.BOW_DAMAGE_DEALT);
        double dealt = melee + bow;
        double taken = profiles.get(id, PitStat.DAMAGE_TAKEN);
        double arrowsShot = profiles.get(id, PitStat.ARROWS_SHOT);
        double arrowHits = profiles.get(id, PitStat.ARROW_HITS);
        long hours = profile.totalPlaytimeSeconds() / 3600;
        double gold = plugin.getGoldManager().getEarned(id);

        inventory.setItem(OFFENSIVE_SLOT, Menus.item(Material.IRON_SWORD, 0, 1, "§cOffensive Stats", Arrays.asList(
                stat("Kills", kills),
                stat("Assists", assists),
                stat("Sword Hits", profiles.get(id, PitStat.SWORD_HITS)),
                stat("Arrows Shot", arrowsShot),
                stat("Arrow Hits", arrowHits),
                "",
                stat("Damage Dealt", dealt),
                stat("Melee Damage Dealt", melee),
                stat("Bow Damage Dealt", bow),
                "",
                stat("Highest Streak", profile.highestStreak))));
        inventory.setItem(DEFENSIVE_SLOT, Menus.item(Material.IRON_CHESTPLATE, 0, 1, "§9Defensive Stats", Arrays.asList(
                stat("Deaths", deaths),
                "",
                stat("Damage Taken", taken),
                stat("Melee Damage Taken", profiles.get(id, PitStat.MELEE_DAMAGE_TAKEN)),
                stat("Bow Damage Taken", profiles.get(id, PitStat.BOW_DAMAGE_TAKEN)))));
        inventory.setItem(PERFORMANCE_SLOT, Menus.item(Material.WHEAT, 0, 1, "§ePerformance Stats", Arrays.asList(
                "§7XP: §b" + String.format(Locale.US, "%,d", stats.getLifetimeXp(id)),
                "",
                "§7§7K/D: §a" + ratio(kills, deaths),
                "§7§7K+A/D: §a" + ratio(kills + assists, deaths),
                "",
                "§7§7Damage dealt/taken: §a" + ratio(dealt, taken),
                "§7§7Arrows hit/shot: §a" + ratio(arrowHits, arrowsShot),
                "",
                stat("Hours played", hours),
                "§7§7Gold/hour: §a" + ratio(gold, hours),
                "§7§7K+A/hour: §a" + ratio(kills + assists, hours))));
        inventory.setItem(MISC_SLOT, Menus.item(Material.OBSIDIAN, 0, 1, "§dMiscellaneous Stats", Arrays.asList(
                stat("Left Clicks", profiles.get(id, PitStat.LEFT_CLICKS)),
                stat("Gold Earned", gold),
                stat("Diamond Items Purchased", profiles.get(id, PitStat.DIAMOND_ITEMS_PURCHASED)),
                stat("Chat Messages", profiles.get(id, PitStat.CHAT_MESSAGES)),
                "",
                stat("Blocks Placed", profiles.get(id, PitStat.BLOCKS_PLACED)),
                stat("Blocks Broken", profiles.get(id, PitStat.BLOCKS_BROKEN)),
                "",
                stat("Jumps into Pit", profiles.get(id, PitStat.JUMPS_INTO_PIT)),
                stat("Launcher Launches", profiles.get(id, PitStat.LAUNCHER_LAUNCHES)),
                "",
                stat("Golden Apples Eaten", profiles.get(id, PitStat.GOLDEN_APPLES_EATEN)),
                stat("Golden Heads Eaten", profiles.get(id, PitStat.GOLDEN_HEADS_EATEN)),
                "",
                stat("Lava Buckets Emptied", profiles.get(id, PitStat.LAVA_BUCKETS_EMPTIED)),
                stat("Fishing Rods Launched", profiles.get(id, PitStat.FISHING_RODS_LAUNCHED)),
                "",
                stat("Contracts Completed", 0),
                "",
                stat("Wheat Farmed", 0),
                stat("Gold from Farming", 0),
                stat("Gold from Selling Fish", 0),
                "",
                stat("King's Quest Completed", 0),
                stat("Sewer Treasures Found", 0),
                "",
                stat("Night Quest Completed", profile.nightQuests),
                "",
                stat("Fished Anything", 0),
                stat("Fished Fish", 0))));
    }

    /** 例: §7§7Kills: §a119,884 (小数は四捨五入)。 */
    static String stat(String name, double value) {
        return "§7§7" + name + ": §a" + String.format(Locale.US, "%,d", Math.round(value));
    }

    /** a / b を小数 3 桁まで (b が 0 なら a)。例: 0.865 / 0.23 / 10,329.127 */
    static String ratio(double a, double b) {
        synchronized (RATIO) {
            return RATIO.format(b == 0 ? a : a / b);
        }
    }

    @Override
    public void click(int slot) {
        // 見るだけ
    }
}
