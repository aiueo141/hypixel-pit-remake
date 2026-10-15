package com.pitremake.quest;

import com.pitremake.PitPlugin;
import com.pitremake.util.ClickMenu;
import com.pitremake.util.Menus;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * QUEST MASTER の NPC の画面 "Quests &amp; Contracts" (45)。録画 2026-10-15_10-32-13.mcrc の配置と文字。
 * <pre>
 *  4 説明 / 12・13 Daily Quest (Hunter / Contracted) / 14 Weekly Quest (Double Up)
 * 30 Novice Contract / 32 Big Time Contract (レベル 70 から) / 36 Achievements / 44 Auto-Accept Quests
 * </pre>
 * クエスト・Contract・実績はまだ無いので見た目だけ (クリックしても何も起きない)。Achievements は 0 / 97。
 */
public final class QuestMenu implements ClickMenu {

    private static final int BIG_TIME_LEVEL = 70;

    private final PitPlugin plugin;
    private final Player player;
    private final Inventory inventory;

    public QuestMenu(PitPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 45, "Quests & Contracts");
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
        inventory.setItem(4, Menus.item(Material.DIRT, 0, 1, "§aThe Pit Quests & Contracts", Arrays.asList(
                "§7View quests and contracts that you",
                "§7can complete by playing The Pit.")));
        inventory.setItem(12, quest("§aDaily Quest: Hunter", "§7§7Kill 25 Players", "2,200", "1,000", "3,300", true));
        inventory.setItem(13, quest("§aDaily Quest: Contracted", "§7§7Complete a Contract", "3,300", "1,000", "4,950", true));
        inventory.setItem(14, quest("§aWeekly Quest: Double Up", "§7§7Collect 10,000 Gold", "7,700", "10,000", "11,550", false));
        inventory.setItem(30, Menus.item(Material.ENCHANTED_BOOK, 0, 1, "§aNovice Contract", Arrays.asList(
                "§7Get a choice of 3 easy contracts.",
                "",
                "§7§8§oPay the request cost, then earn a",
                "§8§ogreater reward on completion.",
                "",
                "§7Daily contracts: §a8",
                "",
                "§7Cost: §6250g",
                "§eClick to request!")));
        List<String> bigTime = new ArrayList<>(Arrays.asList(
                "§7Get a choice of 3 hard contracts.",
                "",
                "§7§8§oPay the request cost, then earn a",
                "§8§ogreater reward on completion.",
                "",
                "§7Daily contracts: §a8",
                ""));
        if (plugin.getStatsManager().getLevel(player.getUniqueId()) < BIG_TIME_LEVEL) {
            bigTime.add("§7Required level: §c[§c§l" + BIG_TIME_LEVEL + "§c]");
            bigTime.add("§cToo low level!");
        } else {
            bigTime.add("§eClick to request!");
        }
        inventory.setItem(32, Menus.item(Material.ENCHANTED_BOOK, 0, 1, "§aBig Time Contract", bigTime));
        inventory.setItem(36, Menus.item(Material.DIAMOND, 0, 1, "§aThe Pit Achievements", Arrays.asList(
                "§7Unlocked: §b0§7/§b97 §8(0%§8)",
                "§7Points: §e0§7/§e1,080 §8(0%§8)",
                "",
                "§eClick to view achievements!")));
        inventory.setItem(44, Menus.item(Material.INK_SACK, 8, 1, "§cAuto-Accept Quests: §cOFF", Arrays.asList(
                "§7§7Click to automatically accept",
                "§7§7quests whenever you join a",
                "§7§7game lobby.",
                "§7",
                "§7§cRequires §bMVP§c+§b")));
    }

    private static ItemStack quest(String name, String goal, String hypixelXp, String gold,
                                   String eventXp, boolean daily) {
        return Menus.item(Material.PAPER, 0, 1, name, Arrays.asList(
                goal,
                "",
                "§7Rewards:",
                "§7§8+§3" + hypixelXp + "§7 §7Hypixel Experience",
                "§7§8+§6" + gold + "§7 §7Pit Gold",
                "§7§8+§5" + eventXp + "§7 §7Event Experience",
                "",
                daily ? "§7§8§oDaily Quests can be completed once every" : "§7§8§oWeekly Quests can be completed once every",
                daily ? "§8§oday." : "§8§oweek. Resets Thursday night.",
                "",
                "§eClick to start this quest."));
    }

    @Override
    public void click(int slot) {
        // クエスト・Contract はまだ無い
    }
}
