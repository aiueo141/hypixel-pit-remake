package com.pitremake.rank;

import com.pitremake.PitPlugin;
import com.pitremake.util.ClickMenu;
import com.pitremake.util.Menus;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Ranks の村人の画面 (見た目だけのランクを自分に付ける)。
 * <pre>
 * Ranks (54)      : 10〜15 Non〜MVP++ (誰でも) / 28〜36 staff などのランク (管理者だけ) / 48 + の色 / 50 MVP++ の色
 * Plus Color (36) : + の色 14 色 / 31 戻る
 * </pre>
 */
public final class RankMenu implements ClickMenu {

    private static final int[] PLAYER_SLOTS = {10, 11, 12, 13, 14, 15};
    private static final int[] ADMIN_SLOTS = {28, 29, 30, 31, 32, 33, 34, 35, 36};
    static final int PLUS_COLOR_SLOT = 48;
    static final int MVP_COLOR_SLOT = 50;
    private static final int[] COLOR_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
    static final int BACK_SLOT = 31;
    private static final String ADMIN_PERMISSION = "pit.admin";

    private final PitPlugin plugin;
    private final Player player;
    private final boolean colors;
    private final Inventory inventory;

    public RankMenu(PitPlugin plugin, Player player) {
        this(plugin, player, false);
    }

    private RankMenu(PitPlugin plugin, Player player, boolean colors) {
        this.plugin = plugin;
        this.player = player;
        this.colors = colors;
        this.inventory = Bukkit.createInventory(this, colors ? 36 : 54, colors ? "Plus Color" : "Ranks");
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        render();
        player.openInventory(inventory);
    }

    private RankManager ranks() {
        return plugin.getRankManager();
    }

    private void render() {
        inventory.clear();
        UUID id = player.getUniqueId();
        if (colors) {
            for (int i = 0; i < RankManager.PLUS_COLORS.size(); i++) {
                ChatColor color = RankManager.PLUS_COLORS.get(i);
                boolean selected = ranks().getPlusColor(id) == color;
                inventory.setItem(COLOR_SLOTS[i], Menus.item(Material.WOOL, woolData(color), 1,
                        color + name(color), Arrays.asList(
                                "§7Preview: " + ranks().getRank(id).prefix(color, ranks().isAqua(id)) + " " + player.getName(),
                                "",
                                selected ? "§aSelected!" : "§eClick to select!")));
            }
            inventory.setItem(BACK_SLOT, Menus.item(Material.ARROW, 0, 1, "§aGo Back", Arrays.asList("§7To Ranks")));
            return;
        }
        int playerIndex = 0;
        int adminIndex = 0;
        for (Rank rank : Rank.values()) {
            if (rank.isAdminOnly()) {
                inventory.setItem(ADMIN_SLOTS[adminIndex++], rankItem(rank));
            } else {
                inventory.setItem(PLAYER_SLOTS[playerIndex++], rankItem(rank));
            }
        }
        Rank current = ranks().getRank(id);
        inventory.setItem(PLUS_COLOR_SLOT, Menus.item(Material.WOOL, woolData(ranks().getPlusColor(id)), 1,
                "§ePlus Color", Arrays.asList(
                        "§7Change the color of the §c+ §7in",
                        "§bMVP+ §7and §6MVP++§7.",
                        "",
                        "§7Current: " + ranks().getPlusColor(id) + name(ranks().getPlusColor(id)),
                        "",
                        current.hasPlusColor() ? "§eClick to change!" : "§cRequires MVP+ or MVP++!")));
        boolean aqua = ranks().isAqua(id);
        inventory.setItem(MVP_COLOR_SLOT, Menus.item(Material.WOOL, aqua ? 3 : 1, 1, "§eMVP++ Color", Arrays.asList(
                "§7Switch the §6MVP++ §7tag and name",
                "§7between gold and aqua.",
                "",
                "§7Current: " + (aqua ? "§bAqua" : "§6Gold"),
                "",
                current == Rank.MVP_PLUS_PLUS ? "§eClick to switch!" : "§cRequires MVP++!")));
    }

    private org.bukkit.inventory.ItemStack rankItem(Rank rank) {
        UUID id = player.getUniqueId();
        String prefix = rank.prefix(ranks().getPlusColor(id), ranks().isAqua(id));
        String nameColor = rank.nameColor(ranks().isAqua(id));
        List<String> lore = new ArrayList<>();
        lore.add("§7Chat: " + (prefix.isEmpty() ? "" : prefix + " ") + nameColor + player.getName()
                + (rank == Rank.NONE ? "§7: Hello!" : "§f: Hello!"));
        lore.add("");
        if (rank == ranks().getRank(id)) {
            lore.add("§aSelected!");
        } else if (rank.isAdminOnly() && !player.hasPermission(ADMIN_PERMISSION)) {
            lore.add("§cAdmin only!");
        } else {
            lore.add("§eClick to select!");
        }
        return Menus.item(Material.NAME_TAG, 0, 1, prefix.isEmpty() ? "§7Non" : prefix, lore);
    }

    @Override
    public void click(int slot) {
        UUID id = player.getUniqueId();
        if (colors) {
            if (slot == BACK_SLOT) {
                new RankMenu(plugin, player).open();
                return;
            }
            for (int i = 0; i < COLOR_SLOTS.length && i < RankManager.PLUS_COLORS.size(); i++) {
                if (COLOR_SLOTS[i] == slot) {
                    ranks().setPlusColor(id, RankManager.PLUS_COLORS.get(i));
                    changed();
                    render();
                }
            }
            return;
        }
        if (slot == PLUS_COLOR_SLOT) {
            if (ranks().getRank(id).hasPlusColor()) {
                new RankMenu(plugin, player, true).open();
            } else {
                deny("§cYou need MVP+ or MVP++ to change the plus color!");
            }
            return;
        }
        if (slot == MVP_COLOR_SLOT) {
            if (ranks().getRank(id) == Rank.MVP_PLUS_PLUS) {
                ranks().setAqua(id, !ranks().isAqua(id));
                changed();
                render();
            } else {
                deny("§cYou need MVP++ to change this color!");
            }
            return;
        }
        Rank[] all = Rank.values();
        int playerIndex = 0;
        int adminIndex = 0;
        for (Rank rank : all) {
            int rankSlot = rank.isAdminOnly() ? ADMIN_SLOTS[adminIndex++] : PLAYER_SLOTS[playerIndex++];
            if (rankSlot != slot) {
                continue;
            }
            if (rank.isAdminOnly() && !player.hasPermission(ADMIN_PERMISSION)) {
                deny("§cThis rank is only for admins!");
                return;
            }
            ranks().setRank(id, rank);
            changed();
            String prefix = ranks().prefix(id);
            player.sendMessage("§aYour rank is now " + (prefix.isEmpty() ? "§7Non" : prefix) + "§a!");
            render();
            return;
        }
    }

    /** 選び直したら tab・頭の上の名前もすぐ変える。 */
    private void changed() {
        player.playSound(player.getLocation(), "note.pling", 1.0f, 2.0f);
        plugin.getSidebar().updateAll();
    }

    private void deny(String message) {
        player.sendMessage(message);
        player.playSound(player.getLocation(), "mob.villager.no", 1.0f, 1.0f);
    }

    private static String name(ChatColor color) {
        String[] words = color.name().toLowerCase().split("_");
        StringBuilder text = new StringBuilder();
        for (String word : words) {
            if (text.length() > 0) {
                text.append(' ');
            }
            text.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return text.toString();
    }

    /** 色に近い羊毛のデータ値。 */
    private static int woolData(ChatColor color) {
        switch (color) {
            case GOLD:
                return 1;
            case GREEN:
                return 5;
            case YELLOW:
                return 4;
            case LIGHT_PURPLE:
                return 6;
            case WHITE:
                return 0;
            case BLUE:
                return 3;
            case DARK_GREEN:
                return 13;
            case DARK_AQUA:
                return 9;
            case DARK_PURPLE:
                return 10;
            case DARK_GRAY:
                return 7;
            case BLACK:
                return 15;
            case DARK_BLUE:
                return 11;
            default:
                return 14;
        }
    }
}
