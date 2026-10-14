package com.pitremake.prestige;

import com.pitremake.PitPlugin;
import com.pitremake.item.MysticItems;
import com.pitremake.item.PantsColor;
import com.pitremake.renown.RenownUpgrade;
import com.pitremake.special.SpecialItems;
import com.pitremake.util.ClickMenu;
import com.pitremake.util.Menus;
import com.pitremake.util.RomanNumerals;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Heresy の画面 (36)。録画 2026-10-14_20-50-01.mcrc の配置・説明文、数値は pit.wiki の Renown / Dark Pants。
 * <pre>
 * 11 Dark Pants   : Chunk of Vile 5 個 (Heresy II で 4、III で 3) と 2 Renown で Fresh Dark Pants (アイコンは欠けた金床)
 * 13 Rage Pants   : Chunk of Vile 17 個で Fresh Rage Pants (Pure Rage が必要)
 * 15 Heresy       : 次の Tier を買う (II: 革の防具の相手に +5% / Dark Pants 4 個、III: Dark Pants 3 個・Tier II まで強化可)
 * 31 戻る / 32 Night Quests (切り替えだけ。Night Quest は未実装)
 * </pre>
 * Hypixel では Dark Pants を作れるのは夜だけだが、ここでは昼夜を見ない。
 */
public final class HeresyMenu implements ClickMenu {

    private static final int DARK_SLOT = 11;
    private static final int RAGE_SLOT = 13;
    private static final int HERESY_SLOT = 15;
    private static final int BACK_SLOT = 31;
    private static final int NIGHT_QUESTS_SLOT = 32;
    private static final int DARK_RENOWN = 2;
    private static final int RAGE_VILE = 17;
    static final String NIGHT_QUESTS_OFF = "nightquests-off";

    private final PitPlugin plugin;
    private final Player player;
    private final Inventory inventory;

    public HeresyMenu(PitPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 36, "Heresy");
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        render();
        player.openInventory(inventory);
    }

    private UUID id() {
        return player.getUniqueId();
    }

    private int tier() {
        return plugin.getUpgradeManager().getRenownTier(id(), RenownUpgrade.HERESY);
    }

    private int renown() {
        return plugin.getStatsManager().getRenown(id());
    }

    /** Dark Pants に要る Chunk of Vile (Heresy I = 5、II = 4、III = 3)。 */
    static int darkVileCost(int heresyTier) {
        return Math.max(3, 6 - Math.max(1, heresyTier));
    }

    private int vile() {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (SpecialItems.isVile(item)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void render() {
        inventory.clear();
        int vile = vile();
        int darkCost = darkVileCost(tier());
        List<String> dark = new ArrayList<>(Arrays.asList(
                "§7Assemble §5Chunks of Vile §7and §eRenown",
                "§e§7to create pants who are really",
                "§7strong against §dMysticism§7.",
                "",
                "§7Costs: §5" + darkCost + " Chunks of Vile",
                "§7Also costs: §e" + DARK_RENOWN + " Renown",
                "",
                "§7Creating: §5Fresh Dark Pants",
                "",
                "§7You have: §e" + renown() + " Renown",
                ""));
        dark.add(vile < darkCost ? "§5Missing Chunks of Vile!" : renown() < DARK_RENOWN ? "§cNot enough renown!" : "§eClick to create!");
        inventory.setItem(DARK_SLOT, Menus.item(Material.ANVIL, 2, 1, "§5Dark Pants", dark));

        boolean pureRage = plugin.getUpgradeManager().hasRenown(id(), RenownUpgrade.PURE_RAGE);
        List<String> rage = new ArrayList<>(Arrays.asList(
                "§7Use §5Chunks of Vile §7to create pants",
                "§7which offer strong",
                "§7counter-enchants.",
                "",
                "§7Costs: §5" + RAGE_VILE + " Chunks of Vile",
                "",
                "§7Creating: §cFresh Rage Pants",
                ""));
        rage.add(!pureRage ? "§cRequires Pure Rage!" : vile < RAGE_VILE ? "§5Missing Chunks of Vile!" : "§eClick to create!");
        ItemStack rageIcon = Menus.item(Material.LEATHER_LEGGINGS, 0, 1, "§cRage Pants", rage);
        LeatherArmorMeta meta = (LeatherArmorMeta) rageIcon.getItemMeta();
        meta.setColor(PantsColor.RAGE.getColor());
        rageIcon.setItemMeta(meta);
        inventory.setItem(RAGE_SLOT, rageIcon);

        inventory.setItem(HERESY_SLOT, heresyItem());
        inventory.setItem(BACK_SLOT, Menus.item(Material.ARROW, 0, 1, "§aGo Back",
                Collections.singletonList("§7To Renown Shop - Upgrades")));
        boolean nightQuests = !plugin.getUpgradeManager().isUnlocked(id(), NIGHT_QUESTS_OFF);
        inventory.setItem(NIGHT_QUESTS_SLOT, Menus.item(Material.LAPIS_BLOCK, 0, 1, "§9Night Quests", Arrays.asList(
                "§7Find §5Chunks of Vile §7by doing",
                "§7§9Night Quests§7.",
                "",
                "§7You may disable the night",
                "§7quests if you desire.",
                "",
                "§7Enabled: " + (nightQuests ? "§aON" : "§cOFF"),
                "",
                "§7§7Note: You can also earn",
                "§7§5chunks §7from contracts.",
                "",
                "§eClick to toggle!")));
    }

    /** 録画: Tier / Next tier の効果 / Dark Pants の値段 / Upgrade cost / You have。 */
    private ItemStack heresyItem() {
        RenownUpgrade heresy = RenownUpgrade.HERESY;
        int tier = tier();
        List<String> lore = new ArrayList<>();
        lore.add("§7Tier: §a" + RomanNumerals.of(tier));
        lore.add("");
        String color;
        if (tier >= heresy.getMaxTier()) {
            lore.add("§aMax tier unlocked!");
            color = "§a";
        } else {
            int next = tier + 1;
            lore.add("§7Next tier:");
            if (next == 2) {
                lore.add("§7Deal §c+5% damage §7against players");
                lore.add("§7wearing leather armor.");
                lore.add("");
            } else {
                lore.add("§7Enchant §5Dark Pants §7to §eTier II");
                lore.add("§7in the §dMystic Well§7.");
                lore.add("");
            }
            lore.add("§7§d+ §7Dark Pants Cost:");
            lore.add("§7§5" + darkVileCost(next) + " Chunks of Vile!");
            lore.add("");
            int cost = heresy.cost(next);
            lore.add("§7§7Upgrade cost: §e" + cost + " Renown");
            lore.add("§7You have: §e" + renown() + " Renown");
            lore.add("");
            int prestige = plugin.getStatsManager().getPrestige(id());
            if (prestige < heresy.requiredPrestige(next)) {
                lore.add("§cRequires prestige " + RomanNumerals.of(heresy.requiredPrestige(next)) + "!");
                color = "§c";
            } else if (renown() < cost) {
                lore.add("§cNot enough renown!");
                color = "§c";
            } else {
                lore.add("§eClick to upgrade!");
                color = "§e";
            }
        }
        return Menus.item(Material.COAL, 0, 1, color + "Heresy", lore);
    }

    @Override
    public void click(int slot) {
        switch (slot) {
            case DARK_SLOT:
                createDark();
                break;
            case RAGE_SLOT:
                createRage();
                break;
            case HERESY_SLOT:
                upgrade();
                break;
            case BACK_SLOT:
                new RenownShopMenu(plugin, player).openUpgrades(RenownUpgrade.HERESY.getPage());
                break;
            case NIGHT_QUESTS_SLOT:
                plugin.getUpgradeManager().toggleFlag(id(), NIGHT_QUESTS_OFF);
                player.playSound(player.getLocation(), "random.click", 1.0f, 1.0f);
                render();
                break;
            default:
                break;
        }
    }

    private void createDark() {
        int cost = darkVileCost(tier());
        if (vile() < cost) {
            deny("§cYou don't have enough Chunks of Vile!");
            return;
        }
        if (!plugin.getStatsManager().takeRenown(id(), DARK_RENOWN)) {
            deny("§cYou don't have enough renown to afford this!");
            return;
        }
        takeVile(cost);
        give(MysticItems.freshDarkPants());
        player.sendMessage("§5§lHERESY! §7Created §5Fresh Dark Pants§7!");
        player.playSound(player.getLocation(), "mob.wither.spawn", 0.5f, 1.5f);
        render();
    }

    private void createRage() {
        if (!plugin.getUpgradeManager().hasRenown(id(), RenownUpgrade.PURE_RAGE)) {
            deny("§cUnlock Pure Rage in the Renown shop first!");
            return;
        }
        if (vile() < RAGE_VILE) {
            deny("§cYou don't have enough Chunks of Vile!");
            return;
        }
        takeVile(RAGE_VILE);
        give(MysticItems.freshRagePants());
        player.sendMessage("§5§lHERESY! §7Created §cFresh Rage Pants§7!");
        player.playSound(player.getLocation(), "mob.wither.spawn", 0.5f, 1.5f);
        render();
    }

    private void upgrade() {
        RenownUpgrade heresy = RenownUpgrade.HERESY;
        int next = tier() + 1;
        if (next > heresy.getMaxTier()) {
            deny("§cYou already unlocked this upgrade!");
            return;
        }
        if (plugin.getStatsManager().getPrestige(id()) < heresy.requiredPrestige(next)) {
            deny("§cYou need prestige " + RomanNumerals.of(heresy.requiredPrestige(next)) + " for the next tier!");
            return;
        }
        if (!plugin.getStatsManager().takeRenown(id(), heresy.cost(next))) {
            deny("§cYou don't have enough renown to afford this!");
            return;
        }
        plugin.getUpgradeManager().setRenownTier(id(), heresy, next);
        player.sendMessage("§a§lPURCHASE! §eHeresy " + RomanNumerals.of(next));
        player.playSound(player.getLocation(), "random.levelup", 0.55f, 2.0f);
        render();
    }

    private void takeVile(int amount) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && amount > 0; i++) {
            if (!SpecialItems.isVile(contents[i])) {
                continue;
            }
            int take = Math.min(amount, contents[i].getAmount());
            amount -= take;
            if (take >= contents[i].getAmount()) {
                player.getInventory().setItem(i, null);
            } else {
                contents[i].setAmount(contents[i].getAmount() - take);
                player.getInventory().setItem(i, contents[i]);
            }
        }
    }

    private void give(ItemStack item) {
        for (ItemStack leftover : player.getInventory().addItem(item).values()) {
            player.getWorld().dropItem(player.getLocation(), leftover);
        }
    }

    private void deny(String message) {
        player.sendMessage(message);
        player.playSound(player.getLocation(), "mob.villager.no", 1.0f, 1.0f);
    }
}
