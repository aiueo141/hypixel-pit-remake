package com.pitremake.prestige;

import com.pitremake.PitPlugin;
import com.pitremake.item.MysticItems;
import com.pitremake.perk.PerkItems;
import com.pitremake.renown.RenownEffects;
import com.pitremake.renown.RenownUpgrade;
import com.pitremake.shop.ShopItem;
import com.pitremake.stats.PitLevels;
import com.pitremake.stats.PrestigeRules;
import com.pitremake.stats.StatsManager;
import com.pitremake.util.ClickMenu;
import com.pitremake.util.Menus;
import com.pitremake.util.PlayerPackets;
import com.pitremake.util.RomanNumerals;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Prestige の村人の画面 (録画 2026-10-14_08-53-15.mcrc / 01-09-29.mcrc の題名・配置・説明文)。
 * <pre>
 * Prestige &amp; Renown (27) : 11 Prestige (ダイヤ) / 15 Renown shop (ビーコン)
 * Prestige? (5) (27)      : 5 秒数えてから 11 の "ARE YOU SURE?" を押せる / 15 Close
 * </pre>
 * プレステージ: レベル 1 (Fast Pass なら 50)・ゴールド 0・Passive とパークとメガストリークを戻す、
 * Items の村人のアイテムとパークのアイテムを消す (Mystic・Gem・Cactus などは残る)、Renown を足す。
 * 押せるようになった後の説明・プレステージしたときのアナウンスと演出は録画に無いので推測。
 */
public final class PrestigeMenu implements ClickMenu {

    static final int PRESTIGE_SLOT = 11;
    static final int RENOWN_SLOT = 15;
    static final int CONFIRM_SLOT = 11;
    static final int CLOSE_SLOT = 15;
    private static final int COUNTDOWN_SECONDS = 5;
    private static final DecimalFormat NUMBER = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));

    private final PitPlugin plugin;
    private final Player player;
    private final boolean confirm;
    private final int secondsLeft;
    private final Inventory inventory;
    private BukkitTask countdown;

    public PrestigeMenu(PitPlugin plugin, Player player) {
        this(plugin, player, false, 0);
    }

    private PrestigeMenu(PitPlugin plugin, Player player, boolean confirm, int secondsLeft) {
        this.plugin = plugin;
        this.player = player;
        this.confirm = confirm;
        this.secondsLeft = secondsLeft;
        String title = !confirm ? "Prestige & Renown" : secondsLeft > 0 ? "Prestige? (" + secondsLeft + ")" : "Prestige?";
        this.inventory = Bukkit.createInventory(this, 27, title);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        render();
        player.openInventory(inventory);
        if (confirm && secondsLeft > 0) {
            // 1 秒ごとに題名の数字を減らす (題名は開き直さないと変わらない)
            countdown = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.getOpenInventory().getTopInventory().getHolder() == this) {
                    new PrestigeMenu(plugin, player, true, secondsLeft - 1).open();
                }
            }, 20);
        }
    }

    @Override
    public void close() {
        if (countdown != null) {
            countdown.cancel();
        }
    }

    private StatsManager stats() {
        return plugin.getStatsManager();
    }

    private void render() {
        UUID id = player.getUniqueId();
        int prestige = stats().getPrestige(id);
        if (confirm) {
            inventory.setItem(CONFIRM_SLOT, Menus.item(Material.STAINED_CLAY, 4, 1, "§aARE YOU SURE?", Arrays.asList(
                    "§7New prestige: §e" + RomanNumerals.of(prestige + 1),
                    "",
                    "§c§lRESETTING LEVEL!",
                    "§c§lRESETTING PERKS!",
                    "§c§lRESETTING UPGRADES!",
                    "§c§lRESETTING INVENTORY!",
                    "",
                    secondsLeft > 0 ? "§eWait and read!" : "§eClick to prestige!")));
            inventory.setItem(CLOSE_SLOT, Menus.item(Material.STAINED_CLAY, 14, 1, "§cClose", null));
            return;
        }
        inventory.setItem(PRESTIGE_SLOT, Menus.item(Material.DIAMOND, 0, 1, "§bPrestige", prestigeLore(prestige)));
        inventory.setItem(RENOWN_SLOT, Menus.item(Material.BEACON, 0, 1, "§eRenown shop", Arrays.asList(
                "§7Use §eRenown §7earned from §bPrestige §7to",
                "§7unlock unique upgrades!",
                "",
                "§7§7§oThese upgrades are safe from",
                "§7§oprestige reset.",
                "",
                "§7Renown: §e" + NUMBER.format(stats().getRenown(id)) + " Renown",
                "",
                "§eClick to browse!")));
    }

    private List<String> prestigeLore(int prestige) {
        UUID id = player.getUniqueId();
        List<String> lore = new ArrayList<>();
        String required = "§7Required Level: " + PrestigeRules.levelTag(prestige, PitLevels.MAX_LEVEL);
        if (prestige >= PrestigeRules.MAX_PRESTIGE) {
            lore.add("§7Current: §e" + RomanNumerals.of(prestige));
            lore.add("");
            lore.add("§aYou reached the highest prestige!");
            return lore;
        }
        if (stats().getLevel(id) < PitLevels.MAX_LEVEL) {
            if (prestige > 0) {
                lore.add("§7Current: §e" + RomanNumerals.of(prestige));
                lore.add("§b" + PrestigeRules.extraXpPercent(prestige) + " §7needed xp than normal!");
                lore.add("");
            }
            lore.add(required);
            lore.add("§7Level up to prestige!");
            return lore;
        }
        long goal = PrestigeRules.goldRequired(prestige);
        double grinded = plugin.getGoldManager().getGrinded(id);
        lore.add(required);
        lore.add("");
        lore.add("§7Cost:");
        lore.add("§c§l◾ Resets §blevel§c to 1");
        lore.add("§c§l◾ Resets §6gold§c to 0");
        lore.add("§c§l◾ Resets §cALL§a perks and upgrades");
        lore.add("§c§l◾ Resets §cyour inventory");
        lore.add("§c§l◾ Grinded §6" + NUMBER.format(Math.floor(grinded)) + "g§c/§6" + NUMBER.format(goal) + "g");
        lore.add("§7§oRenown upgrades are kept");
        lore.add("§7§oEnder chest is kept");
        lore.add("");
        lore.add("§7Reward: §e" + PrestigeRules.renownReward(prestige + 1) + " Renown");
        lore.add("");
        lore.add("§7New prestige: §e" + RomanNumerals.of(prestige + 1));
        lore.add("§b" + PrestigeRules.extraXpPercent(prestige + 1) + " §7needed xp than normal!");
        lore.add("");
        lore.add(grinded >= goal ? "§eClick to purchase!" : "§cGrind more gold to prestige!");
        return lore;
    }

    @Override
    public void click(int slot) {
        if (confirm) {
            if (slot == CLOSE_SLOT) {
                player.closeInventory();
            } else if (slot == CONFIRM_SLOT && secondsLeft <= 0) {
                player.closeInventory();
                if (canPrestige(true)) {
                    prestige();
                }
            }
            return;
        }
        if (slot == PRESTIGE_SLOT) {
            if (canPrestige(true)) {
                new PrestigeMenu(plugin, player, true, COUNTDOWN_SECONDS).open();
            }
        } else if (slot == RENOWN_SLOT) {
            new RenownShopMenu(plugin, player).open();
        }
    }

    private boolean canPrestige(boolean tell) {
        UUID id = player.getUniqueId();
        int prestige = stats().getPrestige(id);
        if (prestige >= PrestigeRules.MAX_PRESTIGE) {
            if (tell) {
                deny("§cYou already reached the highest prestige!");
            }
            return false;
        }
        if (stats().getLevel(id) < PitLevels.MAX_LEVEL) {
            if (tell) {
                // 録画のメッセージ
                deny("§cYou are too low level to prestige yet! Dream big and maybe someday!");
            }
            return false;
        }
        if (plugin.getGoldManager().getGrinded(id) < PrestigeRules.goldRequired(prestige)) {
            if (tell) {
                deny("§cYou haven't grinded enough gold to prestige yet!");
            }
            return false;
        }
        return true;
    }

    private void deny(String message) {
        player.sendMessage(message);
        player.playSound(player.getLocation(), "mob.villager.no", 1.0f, 1.0f);
    }

    /**
     * プレステージで消える持ち物: Items の村人で買ったもの・パークのアイテム・初期装備などの名前の無いバニラのアイテム。
     * Mystic・Gem・Cactus など名前の付いた特別なアイテムは残る (エンダーチェストもそのまま)。
     */
    static void removePrestigeItems(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (removedOnPrestige(inventory.getItem(slot))) {
                inventory.setItem(slot, null);
            }
        }
        ItemStack[] armor = inventory.getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            if (removedOnPrestige(armor[i])) {
                armor[i] = null;
            }
        }
        inventory.setArmorContents(armor);
        player.updateInventory();
    }

    private static boolean removedOnPrestige(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        if (ShopItem.of(item) == ShopItem.PANTS_BUNDLE) {
            // 中に Fresh Pants が入っているので残す
            return false;
        }
        if (ShopItem.of(item) != null || PerkItems.of(item) != null) {
            return true;
        }
        if (MysticItems.isMystic(item)) {
            return false;
        }
        return !item.hasItemMeta() || !item.getItemMeta().hasDisplayName();
    }

    /** プレステージする。 */
    private void prestige() {
        UUID id = player.getUniqueId();
        int reward = PrestigeRules.renownReward(stats().getPrestige(id) + 1);
        stats().prestige(id, reward);
        stats().resetStreak(id);
        plugin.getGoldManager().resetForPrestige(id);
        plugin.getUpgradeManager().resetForPrestige(id);
        if (plugin.getUpgradeManager().hasRenown(id, RenownUpgrade.FAST_PASS)) {
            // Fast Pass (Renown): レベル 50 から
            stats().setXp(id, PitLevels.totalXpFor(RenownEffects.FAST_PASS_LEVEL, stats().getPrestige(id)));
        }
        removePrestigeItems(player);
        plugin.giveSpawnKit(player);
        plugin.getMegastreakManager().refresh(player);
        player.setLevel(stats().getLevel(id));
        player.setExp(stats().getProgress(id));

        int prestige = stats().getPrestige(id);
        String roman = RomanNumerals.of(prestige);
        String message = "§e§lPRESTIGE! §7" + stats().chatTag(id) + " " + plugin.getRankManager().nameColor(id) + player.getName()
                + " §7unlocked prestige §e" + roman + "§7, gg!";
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(message);
        }
        Bukkit.getConsoleSender().sendMessage(message);
        PlayerPackets.title(player, "§e§lPRESTIGE!", "§7You unlocked prestige §e" + roman, 10, 60, 20);
        player.playSound(player.getLocation(), "random.levelup", 1.0f, 1.0f);
        player.sendMessage("§e+" + reward + " Renown");
        plugin.getSidebar().updateAll();
    }
}
