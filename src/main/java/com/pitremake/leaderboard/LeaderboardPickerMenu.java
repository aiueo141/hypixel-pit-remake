package com.pitremake.leaderboard;

import com.pitremake.PitPlugin;
import com.pitremake.util.ClickMenu;
import com.pitremake.util.Menus;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Leaderboard Picker (27)。録画 2026-10-15_10-32-13.mcrc の配置と文字。
 * <pre>
 * 11 Top Active Players (ダイヤ): 上位 10 人・自分の順位・自分の XP。クリックで §a§lPICKED! … と閉じる
 * 15 Event Leaderboards (紫の旗): 説明だけ (イベントはまだ無い)
 * </pre>
 */
public final class LeaderboardPickerMenu implements ClickMenu {

    private static final int PIT_LEVEL_SLOT = 11;
    private static final int EVENT_SLOT = 15;
    /** 紫の旗。 */
    private static final int PURPLE = 5;

    private final PitPlugin plugin;
    private final Player player;
    private final Leaderboard leaderboard;
    private final Inventory inventory;

    LeaderboardPickerMenu(PitPlugin plugin, Player player, Leaderboard leaderboard) {
        this.plugin = plugin;
        this.player = player;
        this.leaderboard = leaderboard;
        this.inventory = Bukkit.createInventory(this, 27, "Leaderboard Picker");
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        List<String> lore = new ArrayList<>(Arrays.asList(
                "§7Pit Level",
                "",
                "§7Players who have accumulated the most",
                "§7§bexperience§7.",
                ""));
        lore.addAll(leaderboard.entries());
        lore.add("");
        int position = leaderboard.position(player.getUniqueId());
        lore.add("§7Your position: §e" + (position > 0 ? "#" + String.format(Locale.US, "%,d", position) : "-"));
        lore.add("§7Your best: §e§b" + LeaderboardRules.abbreviate(plugin.getStatsManager().getLifetimeXp(player.getUniqueId())) + " XP");
        lore.add("");
        lore.add("§aSelected!");
        inventory.setItem(PIT_LEVEL_SLOT, Menus.item(Material.DIAMOND, 0, 1, "§bTop Active Players", lore));
        inventory.setItem(EVENT_SLOT, Menus.item(Material.BANNER, PURPLE, 1, "§5Event Leaderboards", Arrays.asList(
                "§7Uphold your competitive nature by",
                "§7scoring the highest across all players",
                "§7during pit events!",
                "",
                "§7§8Reset every 3 months, only includes",
                "§8players who logged in this week.",
                "",
                "§7Season: §6" + LeaderboardRules.season(LocalDate.now()),
                "",
                "§eClick to view leaderboards!")));
        player.openInventory(inventory);
    }

    @Override
    public void click(int slot) {
        if (slot == PIT_LEVEL_SLOT) {
            player.sendMessage("§a§lPICKED! §7Now showing leaderboards for §bPit Level§7!");
            player.closeInventory();
            leaderboard.refresh();
        }
    }
}
