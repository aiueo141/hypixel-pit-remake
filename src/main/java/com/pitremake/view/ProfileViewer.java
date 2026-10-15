package com.pitremake.view;

import com.pitremake.PitPlugin;
import com.pitremake.data.PlayerProfiles;
import com.pitremake.perk.Perk;
import com.pitremake.perk.PerkItems;
import com.pitremake.stats.PitLevels;
import com.pitremake.stats.StatsManager;
import com.pitremake.upgrade.Megastreak;
import com.pitremake.upgrade.Passive;
import com.pitremake.util.ClickMenu;
import com.pitremake.util.Menus;
import com.pitremake.util.RomanNumerals;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * /view の画面 (録画 2026-10-14_09-15-41.mcrc の "Profile Viewer" / "Inventory" / "Ender Chest" の配置)。見るだけ。
 * <pre>
 * Profile Viewer (45) : 0 / 9 / 18 / 27 防具 / 11 頭 / 13〜16 Perk Slot / 20 名前札 (ゴールド・XP・キル・時間・Renown)
 *                       23 Passives / 24 Killstreaks / 25 Inventory / 26 Ender Chest / 32 Send Trade Request / 36〜44 ホットバー
 * Inventory (36)      : インベントリ (0〜8 がホットバー)
 * Ender Chest (27)    : エンダーチェスト
 * </pre>
 * オフラインのプレイヤーは {@link PlayerProfiles} の写し (ログアウト・1 分ごとに取る持ち物・防具・エンダーチェスト) を見る。
 */
public final class ProfileViewer implements ClickMenu {

    enum Page {
        PROFILE("Profile Viewer", 45),
        INVENTORY("Inventory", 36),
        ENDER_CHEST("Ender Chest", 27);

        final String title;
        final int size;

        Page(String title, int size) {
            this.title = title;
            this.size = size;
        }
    }

    private static final int[] ARMOR_SLOTS = {27, 18, 9, 0};
    private static final int SKULL_SLOT = 11;
    private static final int[] PERK_SLOTS = {13, 14, 15, 16};
    private static final int STATS_SLOT = 20;
    private static final int PASSIVES_SLOT = 23;
    private static final int KILLSTREAKS_SLOT = 24;
    static final int INVENTORY_SLOT = 25;
    static final int ENDER_CHEST_SLOT = 26;
    static final int TRADE_SLOT = 32;
    private static final int HOTBAR_START = 36;
    private static final int TICKS_PER_HOUR = 20 * 60 * 60;
    private static final DecimalFormat NUMBER = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));

    private final PitPlugin plugin;
    private final Player viewer;
    private final UUID targetId;
    private final String targetName;
    private final Page page;
    private final Inventory inventory;

    public ProfileViewer(PitPlugin plugin, Player viewer, Player target) {
        this(plugin, viewer, target.getUniqueId(), target.getName(), Page.PROFILE);
    }

    /** オフラインのプレイヤー (記録の写しを見る)。 */
    public ProfileViewer(PitPlugin plugin, Player viewer, PlayerProfiles.Profile target) {
        this(plugin, viewer, target.id, target.name, Page.PROFILE);
    }

    private ProfileViewer(PitPlugin plugin, Player viewer, UUID targetId, String targetName, Page page) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.targetId = targetId;
        this.targetName = targetName;
        this.page = page;
        // エンダーチェストは Inventory Management で増えた行も出す
        int size = page == Page.ENDER_CHEST ? plugin.getEnderChests().size(targetId) : page.size;
        this.inventory = Bukkit.createInventory(this, size, page.title);
    }

    /** オンラインならそのプレイヤー、オフラインなら null。 */
    private Player online() {
        Player player = Bukkit.getPlayer(targetId);
        return player != null && player.isOnline() ? player : null;
    }

    private PlayerProfiles.Profile profile() {
        return plugin.getPlayerProfiles().get(targetId);
    }

    private ItemStack[] contents() {
        Player player = online();
        return player != null ? player.getInventory().getContents() : profile().inventory.toArray(new ItemStack[0]);
    }

    private ItemStack[] armorContents() {
        Player player = online();
        return player != null ? player.getInventory().getArmorContents() : profile().armor.toArray(new ItemStack[0]);
    }

    private ItemStack[] enderContents() {
        Player player = online();
        return player != null ? plugin.getEnderChests().contents(player) : profile().enderChest.toArray(new ItemStack[0]);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        render();
        viewer.openInventory(inventory);
    }

    private void render() {
        switch (page) {
            case INVENTORY:
                ItemStack[] contents = contents();
                for (int i = 0; i < contents.length && i < inventory.getSize(); i++) {
                    inventory.setItem(i, copy(contents[i]));
                }
                return;
            case ENDER_CHEST:
                ItemStack[] ender = enderContents();
                for (int i = 0; i < ender.length && i < inventory.getSize(); i++) {
                    inventory.setItem(i, copy(ender[i]));
                }
                return;
            default:
                renderProfile();
        }
    }

    private void renderProfile() {
        UUID id = targetId;
        StatsManager stats = plugin.getStatsManager();
        Player online = online();
        ItemStack[] armor = armorContents();
        for (int i = 0; i < armor.length && i < ARMOR_SLOTS.length; i++) {
            inventory.setItem(ARMOR_SLOTS[i], copy(armor[i]));
        }
        ItemStack[] contents = contents();
        for (int i = 0; i < 9 && i < contents.length; i++) {
            inventory.setItem(HOTBAR_START + i, copy(contents[i]));
        }

        String prefix = plugin.getRankManager().prefix(id);
        String nameColor = plugin.getRankManager().nameColor(id);
        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        skullMeta.setOwner(targetName);
        skullMeta.setDisplayName((prefix.isEmpty() ? "" : prefix + " ") + nameColor + targetName + "§f");
        skullMeta.setLore(Collections.singletonList("§7Last login: " + (online != null ? "§aOnline"
                : "§e" + ago(System.currentTimeMillis() - profile().lastSeen))));
        skull.setItemMeta(skullMeta);
        inventory.setItem(SKULL_SLOT, skull);

        for (int i = 0; i < PERK_SLOTS.length; i++) {
            // 録画: 入っているパークはそのアイコンに "§aPerk Slot #1" / "§eVampire"
            Perk perk = plugin.getUpgradeManager().getPerk(id, i);
            ItemStack icon = perk == null
                    ? Menus.item(Material.STAINED_GLASS_PANE, 8, 1, "§aPerk Slot #" + (i + 1), Collections.singletonList("§7None"))
                    : Menus.item(perk.getIcon(), perk.getData(), 1, "§aPerk Slot #" + (i + 1),
                    Collections.singletonList("§e" + perk.getDisplayName()));
            inventory.setItem(PERK_SLOTS[i], perk == Perk.GOLDEN_HEADS ? PerkItems.withHeadTexture(icon) : icon);
        }

        long hours = online != null ? online.getStatistic(Statistic.PLAY_ONE_TICK) / TICKS_PER_HOUR
                : profile().totalPlaytimeSeconds() / 3600;
        inventory.setItem(STATS_SLOT, Menus.item(Material.NAME_TAG, 0, 1,
                stats.chatTag(id) + " " + nameColor + targetName, Arrays.asList(
                        "§7Gold: §6§6" + NUMBER.format(Math.floor(plugin.getGoldManager().get(id))) + "g",
                        "§7Total XP: §b" + NUMBER.format(totalXp(id)) + " XP",
                        "",
                        "§7Kills: §c" + NUMBER.format(stats.getKills(id)),
                        "§7Hours played: §e" + NUMBER.format(hours),
                        "",
                        "§7Renown: §e" + NUMBER.format(stats.getRenown(id)))));

        List<String> passives = new ArrayList<>();
        for (Passive passive : Passive.values()) {
            int tier = plugin.getUpgradeManager().getTier(id, passive);
            passives.add("§7" + passive.getDisplayName() + ": " + (tier > 0 ? "§e" + RomanNumerals.of(tier) : "§cLocked"));
        }
        inventory.setItem(PASSIVES_SLOT, Menus.item(Material.CAKE, 0, 1, "§aPassives", passives));

        Megastreak megastreak = plugin.getUpgradeManager().getMegastreak(id);
        inventory.setItem(KILLSTREAKS_SLOT, Menus.item(megastreak.getIcon(), 0, 1, "§aKillstreaks",
                Collections.singletonList("§7Megastreak§7: §a" + megastreak.getDisplayName())));

        inventory.setItem(INVENTORY_SLOT, Menus.item(Material.CHEST, 0, 1, "§aInventory", Arrays.asList(
                "§7Check out this player's inventory.",
                "",
                "§eClick to open!")));
        inventory.setItem(ENDER_CHEST_SLOT, Menus.item(Material.ENDER_CHEST, 0, 1, "§5Ender Chest", Arrays.asList(
                "§7Snoop around more of this player's",
                "§7belongings.",
                "",
                "§eClick to open!")));
        inventory.setItem(TRADE_SLOT, Menus.item(Material.GOLD_INGOT, 0, 1, "§6Send Trade Request", Arrays.asList(
                "§7Big business.",
                "",
                "§eClick to send!")));
    }

    /** これまでのプレステージで稼いだ XP + 今の XP。 */
    private long totalXp(UUID id) {
        StatsManager stats = plugin.getStatsManager();
        long total = stats.getXp(id);
        for (int prestige = 0; prestige < stats.getPrestige(id); prestige++) {
            total += PitLevels.totalXpFor(PitLevels.MAX_LEVEL, prestige);
        }
        return total;
    }

    /** オフラインになってからの時間 (例: 5 minutes ago / 3 hours ago / 2 days ago)。 */
    static String ago(long millis) {
        long minutes = Math.max(0, millis / 60_000);
        if (minutes < 60) {
            return plural(minutes, "minute") + " ago";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return plural(hours, "hour") + " ago";
        }
        return plural(hours / 24, "day") + " ago";
    }

    private static String plural(long value, String unit) {
        return value + " " + unit + (value == 1 ? "" : "s");
    }

    private static ItemStack copy(ItemStack item) {
        return item == null || item.getType() == Material.AIR ? null : item.clone();
    }

    @Override
    public void click(int slot) {
        if (page != Page.PROFILE) {
            return;
        }
        if (slot == INVENTORY_SLOT) {
            new ProfileViewer(plugin, viewer, targetId, targetName, Page.INVENTORY).open();
        } else if (slot == ENDER_CHEST_SLOT) {
            new ProfileViewer(plugin, viewer, targetId, targetName, Page.ENDER_CHEST).open();
        } else if (slot == TRADE_SLOT) {
            viewer.closeInventory();
            plugin.getTradeManager().request(viewer, online());
        }
    }
}
