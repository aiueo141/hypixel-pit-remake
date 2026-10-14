package com.pitremake.prestige;

import com.pitremake.PitPlugin;
import com.pitremake.perk.Perk;
import com.pitremake.renown.RenownEffects;
import com.pitremake.renown.RenownUpgrade;
import com.pitremake.shop.ShopItem;
import com.pitremake.upgrade.Megastreak;
import com.pitremake.stats.PrestigeRules;
import com.pitremake.upgrade.UpgradeManager;
import com.pitremake.util.ClickMenu;
import com.pitremake.util.Menus;
import com.pitremake.util.RomanNumerals;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Renown shop (録画 2026-10-14_08-53-15.mcrc の題名・配置・説明文)。
 * <pre>
 * Renown Shop (27)                    : 10 Upgrades / 12 Perks / 14 Shop Items / 16 Killstreaks / 22 戻る
 * Renown Shop - Upgrades (45)         : 10〜34 / 40 戻る / 44 次のページ
 * (2/2) Renown Shop - Upgrades (45)   : 10〜28 / 36 前のページ
 * Renown Shop - Perks (45)            : 10〜25 パークの解放 / 40 戻る
 * </pre>
 * Renown Shop - Shop Items (45)      : Items の村人で買えるようになるもの (pit.wiki の Item Shop) / 40 戻る
 * Renown Shop - Killstreaks (45)     : メガストリークの解放 (このあと Upgrades の村人でゴールドで買う) / 40 戻る
 * 解放はプレステージしても消えない。
 * まだそのプレステージに届いていないものは "Unknown upgrade" / "Unknown perk" で隠す。
 * 効果を作れないもの (契約・釣り・Heresy など) と Apollo は見た目だけで買えない。
 * 買ったときのメッセージは録画に無いので推測。
 */
public final class RenownShopMenu implements ClickMenu {

    private enum Page {
        MAIN("Renown Shop", 27),
        UPGRADES("Renown Shop - Upgrades", 45),
        UPGRADES_2("(2/2) Renown Shop - Upgrades", 45),
        PERKS("Renown Shop - Perks", 45),
        SHOP_ITEMS("Renown Shop - Shop Items", 45),
        KILLSTREAKS("Renown Shop - Killstreaks", 45);

        final String title;
        final int size;

        Page(String title, int size) {
            this.title = title;
            this.size = size;
        }
    }

    static final int UPGRADES_SLOT = 10;
    static final int PERKS_SLOT = 12;
    static final int SHOP_ITEMS_SLOT = 14;
    static final int KILLSTREAKS_SLOT = 16;
    static final int MAIN_BACK = 22;
    static final int PAGE_BACK = 40;
    static final int NEXT_PAGE = 44;
    static final int PREVIOUS_PAGE = 36;
    private static final DecimalFormat NUMBER = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));

    private final PitPlugin plugin;
    private final Player player;
    private final Page page;
    private final Inventory inventory;

    public RenownShopMenu(PitPlugin plugin, Player player) {
        this(plugin, player, Page.MAIN);
    }

    private RenownShopMenu(PitPlugin plugin, Player player, Page page) {
        this.plugin = plugin;
        this.player = player;
        this.page = page;
        this.inventory = Bukkit.createInventory(this, page.size, page.title);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        render();
        player.openInventory(inventory);
    }

    /** Renown Shop - Upgrades のそのページ (0 から) を開く (Heresy の戻る)。 */
    public void openUpgrades(int upgradePage) {
        new RenownShopMenu(plugin, player, upgradePage == 0 ? Page.UPGRADES : Page.UPGRADES_2).open();
    }

    private UUID id() {
        return player.getUniqueId();
    }

    private int renown() {
        return plugin.getStatsManager().getRenown(id());
    }

    private int prestige() {
        return plugin.getStatsManager().getPrestige(id());
    }

    private String renownLine() {
        return "§7Renown: §e" + NUMBER.format(renown()) + " Renown";
    }

    private String youHaveLine() {
        return "§7You have: §e" + NUMBER.format(renown()) + " Renown";
    }

    private void render() {
        inventory.clear();
        switch (page) {
            case MAIN:
                renderMain();
                break;
            case PERKS:
                for (Perk perk : renownPerks()) {
                    inventory.setItem(perk.getRenownSlot(), perkItem(perk));
                }
                inventory.setItem(PAGE_BACK, back("§7To Renown Shop"));
                break;
            case SHOP_ITEMS:
                List<ShopItem> shopItems = renownShopItems();
                for (int i = 0; i < shopItems.size(); i++) {
                    inventory.setItem(rowSlot(i, shopItems.size()), shopItemUnlock(shopItems.get(i)));
                }
                inventory.setItem(PAGE_BACK, back("§7To Renown Shop"));
                break;
            case KILLSTREAKS:
                List<Megastreak> streaks = renownMegastreaks();
                int bundles = streaks.size();
                for (int i = 0; i < streaks.size(); i++) {
                    inventory.setItem(rowSlot(i, bundles), megastreakUnlock(streaks.get(i)));
                }
                inventory.setItem(PAGE_BACK, back("§7To Renown Shop"));
                break;
            default:
                int upgradePage = page == Page.UPGRADES ? 0 : 1;
                for (RenownUpgrade upgrade : RenownUpgrade.values()) {
                    if (upgrade.getPage() == upgradePage) {
                        inventory.setItem(upgrade.getSlot(), upgradeItem(upgrade));
                    }
                }
                if (upgradePage == 0) {
                    inventory.setItem(PAGE_BACK, back("§7To Renown Shop"));
                    inventory.setItem(NEXT_PAGE, Menus.item(Material.ARROW, 0, 1, "§eLeft-click for next page!",
                            Collections.singletonList("§bRight-click for last page!")));
                } else {
                    inventory.setItem(PREVIOUS_PAGE, Menus.item(Material.ARROW, 0, 1, "§eLeft-click for previous page!",
                            Collections.singletonList("§bRight-click for first page!")));
                }
                break;
        }
    }

    private void renderMain() {
        UpgradeManager upgrades = plugin.getUpgradeManager();
        int unknownUpgrades = 0;
        for (RenownUpgrade upgrade : RenownUpgrade.values()) {
            if (prestige() < upgrade.getRequiredPrestige()) {
                unknownUpgrades++;
            }
        }
        List<String> upgradesLore = new ArrayList<>(Arrays.asList(
                "§7Variety of upgrades, buffs and",
                "§7special unlocks.",
                "",
                "§7Unlocked: §e" + upgrades.renownTierCount(id()) + "/" + RenownUpgrade.totalTiers()));
        if (unknownUpgrades > 0) {
            upgradesLore.add("§8Unknowns: " + unknownUpgrades);
        }
        upgradesLore.addAll(Arrays.asList("", renownLine(), "", "§eClick to browse!"));
        inventory.setItem(UPGRADES_SLOT, Menus.item(Material.EMERALD, 0, 1, "§e§aUpgrades", upgradesLore));
        inventory.setItem(PERKS_SLOT, Menus.item(Material.DIAMOND, 0, 1, "§e§bPerks", Arrays.asList(
                "§7Unlock new perks available for",
                "§7purchase at the upgrades npc.",
                "",
                "§7Unlocked: §e" + upgrades.renownPerkCount(id()) + "/" + renownPerks().size(),
                "",
                renownLine(),
                "",
                "§eClick to browse!")));
        inventory.setItem(SHOP_ITEMS_SLOT, Menus.item(Material.GOLD_BARDING, 0, 1, "§e§6Shop Items", Arrays.asList(
                "§7Get access to new items in the", "§7non-permanent items shop.", "",
                "§7Unlocked: §e" + countUnlocked(renownShopItemKeys()) + "/" + renownShopItems().size(), "",
                renownLine(), "", "§eClick to browse!")));
        inventory.setItem(KILLSTREAKS_SLOT, Menus.item(Material.BLAZE_POWDER, 0, 1, "§e§cKillstreaks", Arrays.asList(
                "§7Pick bundles of killstreaks to unlock", "§7in the upgrades npc.", "",
                "§7Unlocked: §e" + countUnlocked(renownMegastreakKeys()) + "/" + renownMegastreaks().size(), "",
                renownLine(), "", "§eClick to browse!")));
        inventory.setItem(MAIN_BACK, back("§7To Prestige & Renown"));
    }

    private static ItemStack back(String to) {
        return Menus.item(Material.ARROW, 0, 1, "§aGo Back", Collections.singletonList(to));
    }

    private static List<Perk> renownPerks() {
        List<Perk> perks = new ArrayList<>();
        for (Perk perk : Perk.values()) {
            if (perk.isRenownPerk()) {
                perks.add(perk);
            }
        }
        return perks;
    }

    /** Upgrades の 1 つ (録画の形: 1 段階のもの / Tier のあるもの (Current / Tier / Each tier) / 最後まで買ったもの)。 */
    private ItemStack upgradeItem(RenownUpgrade upgrade) {
        if (prestige() < upgrade.getRequiredPrestige()) {
            return Menus.item(Material.BEDROCK, 0, 1, "§cUnknown upgrade",
                    Collections.singletonList("§7Prestige: §e" + RomanNumerals.of(upgrade.getRequiredPrestige())));
        }
        int tier = plugin.getUpgradeManager().getRenownTier(id(), upgrade);
        List<String> lore = new ArrayList<>();
        if (upgrade == RenownUpgrade.MYSTICISM) {
            // 録画: Current: +150% chance / Tier: IX / Next tier: … +200% more often than normal.
            if (tier > 0) {
                lore.add("§7Current: §d+" + percent(RenownEffects.mysticismBonus(tier)) + "% chance");
                lore.add("§7Tier: §a" + RomanNumerals.of(tier));
                lore.add("");
            }
            if (tier < upgrade.getMaxTier()) {
                lore.add("§7Next tier:");
                lore.addAll(mysticismNextTier(tier + 1));
            } else {
                lore.addAll(upgrade.getDescription());
            }
        } else if (upgrade.isTiered()) {
            if (tier > 0) {
                String current = current(upgrade, tier);
                if (current != null) {
                    lore.add("§7Current: " + current);
                }
                lore.add("§7Tier: §a" + RomanNumerals.of(tier));
                lore.add("");
            }
            lore.add("§7Each tier:");
            lore.addAll(upgrade.getDescription());
        } else {
            lore.addAll(upgrade.getDescription());
        }
        lore.add("");
        String color;
        if (tier >= upgrade.getMaxTier()) {
            // 録画 2026-10-14_21-17-47.mcrc: 解放済みの Fancy Hat は "Click to customize!" だけ
            lore.add(upgrade == RenownUpgrade.FANCY_HAT ? "§aClick to customize!"
                    : upgrade.isTiered() ? "§aMax tier unlocked!" : "§aUnlocked!");
            color = "§a";
        } else if (!upgrade.isAvailable()) {
            lore.add("§7Cost: §e" + upgrade.cost(tier + 1) + " Renown");
            lore.add("");
            lore.add("§cNot available yet!");
            color = "§c";
        } else {
            int cost = upgrade.cost(tier + 1);
            lore.add((tier > 0 ? "§7§7Upgrade cost: §e" : "§7Cost: §e") + cost + " Renown");
            lore.add(youHaveLine());
            lore.add("");
            if (prestige() < upgrade.requiredPrestige(tier + 1)) {
                lore.add("§cRequires prestige " + RomanNumerals.of(upgrade.requiredPrestige(tier + 1)) + "!");
                color = tier > 0 ? "§a" : "§c";
            } else if (renown() < cost) {
                lore.add("§cNot enough renown!");
                color = tier > 0 ? "§a" : "§c";
            } else {
                lore.add(tier > 0 ? "§eClick to upgrade!" : "§eClick to purchase!");
                color = tier > 0 ? "§a" : "§e";
            }
        }
        int amount = upgrade == RenownUpgrade.ARMORY ? 8 : 1;
        return Menus.item(upgrade.getIcon(), upgrade.getIconData(), amount, color + upgrade.getDisplayName(), lore);
    }

    private static String percent(double fraction) {
        return new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.US)).format(fraction * 100);
    }

    /**
     * Mysticism の次の Tier の説明。確率が上がる Tier は録画の文 ("You find mystic items and pants +200% more often
     * than normal.")、それ以外 (I / IV / IX) は wiki の内容から作った文。
     */
    private static List<String> mysticismNextTier(int tier) {
        switch (tier) {
            case 1:
                return Arrays.asList("§7Mystic swords and bows can now", "§7drop from kills.");
            case 4:
                return Arrays.asList("§7Fresh §cP§6a§en§at§9s §7can now drop", "§7from kills.");
            case 9:
                return Arrays.asList("§7Fresh §cP§6a§en§at§9s §7can now be", "§7enchanted in the §dMystic Well§7.");
            default:
                return Arrays.asList("§7You find mystic items and pants",
                        "§7§d+" + percent(RenownEffects.mysticismBonus(tier)) + "% §7more often than normal.");
        }
    }

    /** "Current:" の行 (録画の Tenacity / Renown Gold Boost / Renown XP Bump の形)。 */
    private static String current(RenownUpgrade upgrade, int tier) {
        switch (upgrade) {
            case TENACITY:
                return "Heal §c" + String.format(Locale.US, "%.1f", 0.5 * tier) + "❤ §7on kill.";
            case SCAM_ARTIST:
                return "§6-" + 5 * tier + "% §7cost";
            case RENOWN_GOLD_BOOST:
                return "§6+" + new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.US)).format(2.5 * tier) + "% gold (g)";
            case RENOWN_XP_BUMP:
                return "§b+" + tier + " kill XP§7";
            case EXTRA_HEARTS:
                return "§c+" + tier + "❤ §7max health";
            case ARMORY:
                return "§f+" + 8 * tier + " arrows §7on spawn";
            case DIVINE_INTERVENTION:
                return "§e" + 5 * tier + "% chance";
            default:
                return null;
        }
    }

    private ItemStack perkItem(Perk perk) {
        if (prestige() < perk.getRequiredPrestige()) {
            return Menus.item(Material.BEDROCK, 0, 1, "§cUnknown perk",
                    Collections.singletonList("§7Prestige: §e" + RomanNumerals.of(perk.getRequiredPrestige())));
        }
        boolean unlocked = plugin.getUpgradeManager().isPerkUnlocked(id(), perk);
        List<String> lore = new ArrayList<>();
        lore.add("§7Required level: " + PrestigeRules.levelTag(perk.getRequiredPrestige(), perk.getRequiredLevel()));
        lore.add("§7Cost: §6" + NUMBER.format(perk.getCost()) + "g");
        lore.add("");
        lore.add("§e" + perk.getDisplayName());
        lore.addAll(perk.getDescription());
        lore.add("");
        lore.add("§7§7§oThis is a perk unlock. You still have");
        lore.add("§7§oto purchase the perk in the");
        lore.add("§7§oupgrades shop.");
        lore.add("");
        String color;
        if (unlocked) {
            lore.add("§aUnlocked!");
            color = "§a";
        } else {
            boolean affordable = renown() >= perk.getRenownCost();
            lore.add("§7Cost: §e" + perk.getRenownCost() + " Renown");
            lore.add(youHaveLine());
            lore.add("");
            lore.add(affordable ? "§eClick to purchase!" : "§cNot enough renown!");
            color = affordable ? "§e" : "§c";
        }
        ItemStack item = Menus.item(perk.getIcon(), perk.getData(), 1, color + "Perk unlock: " + perk.getDisplayName(), lore);
        if (perk.getIcon() == Material.LEATHER_BOOTS) {
            // 録画の Marathon は白い革のブーツ
            LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
            meta.setColor(Color.WHITE);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void click(int slot) {
        switch (page) {
            case MAIN:
                if (slot == UPGRADES_SLOT) {
                    new RenownShopMenu(plugin, player, Page.UPGRADES).open();
                } else if (slot == PERKS_SLOT) {
                    new RenownShopMenu(plugin, player, Page.PERKS).open();
                } else if (slot == MAIN_BACK) {
                    new PrestigeMenu(plugin, player).open();
                } else if (slot == SHOP_ITEMS_SLOT) {
                    new RenownShopMenu(plugin, player, Page.SHOP_ITEMS).open();
                } else if (slot == KILLSTREAKS_SLOT) {
                    new RenownShopMenu(plugin, player, Page.KILLSTREAKS).open();
                }
                return;
            case SHOP_ITEMS:
            case KILLSTREAKS:
                if (slot == PAGE_BACK) {
                    new RenownShopMenu(plugin, player, Page.MAIN).open();
                    return;
                }
                if (page == Page.SHOP_ITEMS) {
                    List<ShopItem> items = renownShopItems();
                    for (int i = 0; i < items.size(); i++) {
                        if (rowSlot(i, items.size()) == slot) {
                            ShopItem item = items.get(i);
                            unlock(item.unlockKey(), item.getDisplayName(), item.getRenownPrestige(), item.getRenownCost());
                        }
                    }
                } else {
                    List<Megastreak> streaks = renownMegastreaks();
                    int bundles = streaks.size();
                    for (int i = 0; i < streaks.size(); i++) {
                        if (rowSlot(i, bundles) == slot) {
                            Megastreak streak = streaks.get(i);
                            unlock(streak.unlockKey(), streak.getDisplayName(), streak.getRenownPrestige(), streak.getRenownCost());
                        }
                    }

                }
                return;
            case PERKS:
                if (slot == PAGE_BACK) {
                    new RenownShopMenu(plugin, player, Page.MAIN).open();
                    return;
                }
                for (Perk perk : renownPerks()) {
                    if (perk.getRenownSlot() == slot) {
                        buy(perk);
                    }
                }
                return;
            default:
                int upgradePage = page == Page.UPGRADES ? 0 : 1;
                if (upgradePage == 0 && slot == PAGE_BACK) {
                    new RenownShopMenu(plugin, player, Page.MAIN).open();
                    return;
                }
                if ((upgradePage == 0 && slot == NEXT_PAGE) || (upgradePage == 1 && slot == PREVIOUS_PAGE)) {
                    // ページは 2 つなので、右クリック (最後 / 最初のページ) も同じ
                    new RenownShopMenu(plugin, player, upgradePage == 0 ? Page.UPGRADES_2 : Page.UPGRADES).open();
                    return;
                }
                for (RenownUpgrade upgrade : RenownUpgrade.values()) {
                    if (upgrade.getPage() == upgradePage && upgrade.getSlot() == slot) {
                        if (upgrade == RenownUpgrade.HERESY && plugin.getUpgradeManager().hasRenown(id(), upgrade)) {
                            // 録画: 解放済みの Heresy をクリックすると Heresy の画面
                            new HeresyMenu(plugin, player).open();
                            return;
                        }
                        if (upgrade == RenownUpgrade.FANCY_HAT && plugin.getUpgradeManager().hasRenown(id(), upgrade)) {
                            // 解放済みの Fancy Hat は色を変える画面
                            new FancyHatMenu(plugin, player).open();
                            return;
                        }
                        buy(upgrade);
                    }
                }
        }
    }

    private void buy(RenownUpgrade upgrade) {
        UpgradeManager upgrades = plugin.getUpgradeManager();
        int tier = upgrades.getRenownTier(id(), upgrade) + 1;
        if (prestige() < upgrade.getRequiredPrestige()) {
            deny("§cYou need prestige " + RomanNumerals.of(upgrade.getRequiredPrestige()) + " to unlock this!");
            return;
        }
        if (tier > upgrade.getMaxTier()) {
            deny("§cYou already unlocked this upgrade!");
            return;
        }
        if (!upgrade.isAvailable()) {
            deny("§cThis upgrade is not available yet!");
            return;
        }
        if (prestige() < upgrade.requiredPrestige(tier)) {
            deny("§cYou need prestige " + RomanNumerals.of(upgrade.requiredPrestige(tier)) + " for the next tier!");
            return;
        }
        if (!plugin.getStatsManager().takeRenown(id(), upgrade.cost(tier))) {
            deny("§cYou don't have enough renown to afford this!");
            return;
        }
        upgrades.setRenownTier(id(), upgrade, tier);
        player.sendMessage("§a§lPURCHASE! §e" + upgrade.getDisplayName()
                + (upgrade.isTiered() ? " " + RomanNumerals.of(tier) : ""));
        player.playSound(player.getLocation(), "random.levelup", 0.55f, 2.0f);
        // 最大体力・持ち物などをすぐ反映する
        plugin.getMegastreakManager().refresh(player);
        new RenownShopMenu(plugin, player, upgrade.getPage() == 0 ? Page.UPGRADES : Page.UPGRADES_2).open();
    }

    private void buy(Perk perk) {
        UpgradeManager upgrades = plugin.getUpgradeManager();
        if (prestige() < perk.getRequiredPrestige()) {
            deny("§cYou need prestige " + RomanNumerals.of(perk.getRequiredPrestige()) + " to unlock this!");
            return;
        }
        if (upgrades.isPerkUnlocked(id(), perk)) {
            deny("§cYou already unlocked this perk!");
            return;
        }
        if (!plugin.getStatsManager().takeRenown(id(), perk.getRenownCost())) {
            deny("§cYou don't have enough renown to afford this!");
            return;
        }
        upgrades.unlockRenownPerk(id(), perk);
        player.sendMessage("§a§lPURCHASE! §ePerk unlock: " + perk.getDisplayName());
        player.playSound(player.getLocation(), "random.levelup", 0.55f, 2.0f);
        new RenownShopMenu(plugin, player, Page.PERKS).open();
    }

    // ---------------------------------------------------------------- Shop Items / Killstreaks

    private static List<ShopItem> renownShopItems() {
        List<ShopItem> items = new ArrayList<>();
        for (ShopItem item : ShopItem.values()) {
            if (!item.isDefault()) {
                items.add(item);
            }
        }
        return items;
    }

    private static List<String> renownShopItemKeys() {
        List<String> keys = new ArrayList<>();
        for (ShopItem item : renownShopItems()) {
            keys.add(item.unlockKey());
        }
        return keys;
    }

    private static List<Megastreak> renownMegastreaks() {
        List<Megastreak> streaks = new ArrayList<>();
        for (Megastreak streak : Megastreak.values()) {
            if (!streak.isDefault()) {
                streaks.add(streak);
            }
        }
        return streaks;
    }

    private static List<String> renownMegastreakKeys() {
        List<String> keys = new ArrayList<>();
        for (Megastreak streak : renownMegastreaks()) {
            keys.add(streak.unlockKey());
        }
        return keys;
    }

    private int countUnlocked(List<String> keys) {
        int count = 0;
        for (String key : keys) {
            if (plugin.getUpgradeManager().isUnlocked(id(), key)) {
                count++;
            }
        }
        return count;
    }

    /** 1 行 7 個まで真ん中に寄せた位置 (2 行目から)。 */
    private static int rowSlot(int index, int total) {
        int row = index / 7;
        int inRow = Math.min(7, total - row * 7);
        return 9 * (row + 1) + 1 + (7 - inRow) / 2 + index % 7;
    }

    private ItemStack shopItemUnlock(ShopItem item) {
        List<String> lore = new ArrayList<>();
        lore.add("§7Item cost: §6" + NUMBER.format(item.getCost()) + "g");
        lore.add("");
        lore.addAll(item.getDescription());
        lore.add("");
        lore.add("§7§oThis is an item unlock. You still");
        lore.add("§7§ohave to purchase the item in the");
        lore.add("§7§oitems shop.");
        return unlockItem(item.unlockKey(), item.getMaterial(), item.getData(), item.getAmount(), "Item unlock: " + item.getDisplayName(),
                item.getRenownPrestige(), item.getRenownCost(), lore);
    }

    private ItemStack megastreakUnlock(Megastreak streak) {
        List<String> lore = new ArrayList<>();
        lore.add("§7Required level: " + PrestigeRules.levelTag(prestige(), streak.getRequiredLevel()));
        lore.add("§7Cost: §6" + NUMBER.format(streak.getCost()) + "g");
        lore.add("");
        lore.addAll(streak.getLore());
        lore.add("");
        lore.addAll(bundleLines(streak.unlockKey()));
        lore.add("§7§oThis is a killstreak unlock. You");
        lore.add("§7§ostill have to purchase it in the");
        lore.add("§7§oupgrades shop.");
        return unlockItem(streak.unlockKey(), streak.getIcon(), 0, 1, "Killstreak unlock: " + streak.getDisplayName(),
                streak.getRenownPrestige(), streak.getRenownCost(), lore);
    }

    /** バンドルに入っているキルストリーク。 */
    private static List<String> bundleLines(String key) {
        List<String> lines = new ArrayList<>();
        for (com.pitremake.upgrade.Killstreak killstreak : com.pitremake.upgrade.Killstreak.values()) {
            if (key.equals(killstreak.bundleKey())) {
                if (lines.isEmpty()) {
                    lines.add("§7Killstreaks:");
                }
                lines.add("§7■ §a" + killstreak.getDisplayName());
            }
        }
        if (!lines.isEmpty()) {
            lines.add("");
        }
        return lines;
    }

    /** Shop Items / Killstreaks の 1 つ (Perks と同じ形)。届いていないプレステージは Unknown で隠す。 */
    private ItemStack unlockItem(String key, Material icon, int data, int amount, String name, int requiredPrestige, int cost,
                                 List<String> lore) {
        if (prestige() < requiredPrestige) {
            return Menus.item(Material.BEDROCK, 0, 1, "§cUnknown unlock",
                    Collections.singletonList("§7Prestige: §e" + RomanNumerals.of(requiredPrestige)));
        }
        lore.add("");
        String color;
        if (plugin.getUpgradeManager().isUnlocked(id(), key)) {
            lore.add("§aUnlocked!");
            color = "§a";
        } else {
            boolean affordable = renown() >= cost;
            lore.add("§7Cost: §e" + cost + " Renown");
            lore.add(youHaveLine());
            lore.add("");
            lore.add(affordable ? "§eClick to purchase!" : "§cNot enough renown!");
            color = affordable ? "§e" : "§c";
        }
        return Menus.item(icon, data, amount, color + name, lore);
    }

    private void unlock(String key, String name, int requiredPrestige, int cost) {
        UpgradeManager upgrades = plugin.getUpgradeManager();
        if (prestige() < requiredPrestige) {
            deny("§cYou need prestige " + RomanNumerals.of(requiredPrestige) + " to unlock this!");
            return;
        }
        if (upgrades.isUnlocked(id(), key)) {
            deny("§cYou already unlocked this!");
            return;
        }
        if (!plugin.getStatsManager().takeRenown(id(), cost)) {
            deny("§cYou don't have enough renown to afford this!");
            return;
        }
        upgrades.unlock(id(), key);
        player.sendMessage("§a§lPURCHASE! §e" + name);
        player.playSound(player.getLocation(), "random.levelup", 0.55f, 2.0f);
        new RenownShopMenu(plugin, player, page).open();
    }

    private void deny(String message) {
        player.sendMessage(message);
        player.playSound(player.getLocation(), "mob.villager.no", 1.0f, 1.0f);
    }
}
