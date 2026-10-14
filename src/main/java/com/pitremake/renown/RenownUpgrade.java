package com.pitremake.renown;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Renown Shop の Upgrades (録画 2026-10-14_08-53-15.mcrc の "Renown Shop - Upgrades" 2 ページの位置・アイコン・説明文)。
 * 必要 Renown・Tier の数・必要プレステージは pit.wiki の Renown。
 * <p>
 * available = false は契約・釣り・Heresy などの仕組みが無くて効果を作れないもの (見た目だけで買えない)。
 * Apollo は頼まれて見た目だけ。
 */
public enum RenownUpgrade {
    TENACITY("Tenacity", 0, 10, Material.MAGMA_CREAM, 1, new int[]{10, 50},
            lines("§7Heal §c0.5❤ §7on kill.")),
    AUTOBUY("Autobuy", 0, 11, Material.DIAMOND_CHESTPLATE, 1, new int[]{5},
            lines("§7Allows you to autobuy items from the", "§7shop when respawning.")),
    SCAM_ARTIST("Scam Artist", 0, 12, Material.PORK, 1, new int[]{10, 20, 30, 40, 50},
            lines("§7Item, contract and mystic well", "§7purchases are §6-5% §7the cost.")),
    RENOWN_GOLD_BOOST("Renown Gold Boost", 0, 13, Material.GOLD_NUGGET, 1, new int[]{5, 10, 10, 10, 10, 10, 10, 10, 10, 10},
            lines("§7Earn §6+2.5% gold (g) §7from kills and", "§7coin pickups.")),
    RENOWN_XP_BUMP("Renown XP Bump", 0, 14, Material.EXP_BOTTLE, 1, new int[]{5, 10, 10, 10, 10, 10, 20, 40, 80, 160},
            new int[]{1, 1, 1, 1, 2, 10, 13, 16, 19, 22}, true, lines("§7Earn §b+1 kill XP§7.")),
    MYSTICISM("Mysticism", 0, 15, Material.ENCHANTMENT_TABLE, 1, new int[]{10, 5, 5, 10, 5, 10, 10, 15, 20, 25},
            new int[]{1, 1, 1, 1, 1, 2, 3, 4, 5, 7}, true, lines("§7You find mystic items and pants", "§7more often than normal.")),
    FISH_MAXXING("Fish Maxxing", 0, 16, Material.COOKED_FISH, 2, new int[]{5, 5, 5, 5}, false,
            lines("§7Allows you to fish more times per day.")),
    CONTRACTOR("Contractor", 0, 19, Material.ENCHANTED_BOOK, 2, new int[]{5, 5, 10, 15, 15}, false,
            lines("§7Earn §b+1 §7daily contract limit§7.")),
    SUPERB_LAUNCH_TRAIL("Superb Launch Trail", 0, 20, Material.SPECKLED_MELON, 2, new int[]{10},
            lines("§7Everyone loves rainbows.", "", "§7§oThis is a cosmetic effect when using", "§7§othe slime launchers.")),
    EXTRA_PERK_SLOT("Extra Perk Slot", 0, 21, Material.DIAMOND_BLOCK, 3, new int[]{40},
            lines("§7Perk Slot #4 available at level 100.")),
    IMPATIENT("Impatient", 0, 22, Material.CARROT_ITEM, 3, new int[]{10},
            lines("§7Gain Speed II in spawn area.")),
    FISHING_CLUB("Fishing Club", 0, 23, Material.WATER_BUCKET, 3, new int[]{5, 5, 5, 5, 10}, false,
            lines("§7Can now actually find fish from", "§7fishing.")),
    EXTRA_HEARTS("Extra Hearts", 0, 24, Material.APPLE, 4, new int[]{20, 100},
            lines("§7Permanently gain an extra heart.")),
    EXTRA_KILLSTREAK("Extra Killstreak", 0, 25, Material.GOLD_BLOCK, 4, new int[]{50},
            lines("§7Unlock the extra killstreak slot.")),
    ARMORY("Armory", 0, 28, Material.ARROW, 4, new int[]{5, 5, 5, 5},
            lines("§7§f+8 arrows §7on spawn.")),
    FANCY_HAT("Fancy Hat", 0, 29, Material.LEATHER_HELMET, 5, new int[]{10},
            lines("§7Permanent leather helmet.", "§7Overridden by Safety First. Can be", "§7§ddyed§7!")),
    YUMMY("Yummy!", 0, 30, Material.GOLDEN_APPLE, 6, new int[]{15},
            lines("§7Golden apples and heads you eat", "§7reward §6+§66g§7.")),
    HERESY("Heresy", 0, 31, Material.COAL, 6, new int[]{10, 20, 50}, new int[]{6, 8, 11}, true,
            lines("§7You have trained in the profane", "§7arts of counter-mysticism.")),
    DIVINE_INTERVENTION("Divine Intervention", 0, 32, Material.QUARTZ_STAIRS, 6, new int[]{50, 50, 50},
            new int[]{6, 12, 18}, true, lines("§7§e+5% chance §7to keep your inventory", "§7on death.")),
    BLAZING_LAUNCH_TRAIL("Blazing Launch Trail", 0, 33, Material.BLAZE_POWDER, 6, new int[]{30},
            lines("§7One lit trail.", "", "§7§oThis is a cosmetic effect when using", "§7§othe slime launchers.")),
    APOLLO("Apollo", 0, 34, Material.ENDER_STONE, 7, new int[]{25}, false,
            lines("§7Launchers send you towards the", "§7nearest spawn you're looking at.")),

    THE_WAY("The Way", 1, 10, Material.ACACIA_DOOR_ITEM, 8, new int[]{50},
            lines("§7No perk level requirements.")),
    VERY_YUMMY("Very Yummy!", 1, 11, Material.GOLDEN_CARROT, 8, new int[]{20},
            lines("§7Upgrade §6Golden Heads§7:", " §7Adds +2 seconds to §cRegen II", " §7Grants §6+0.5❤ absorption")),
    DO_YOUR_OWN_CARPENTRY("Do Your Own Carpentry", 1, 12, Material.WOOD_STEP, 8, new int[]{5}, false,
            lines("§7Increase odds for §5vile §7in contracts", "§7and gain §5+1 §7from them.")),
    SELF_CONFIDENCE("Self-Confidence", 1, 13, Material.PAINTING, 9, new int[]{50}, false,
            lines("§7Earn bonus §6gold§7:", " §7Top 5:  §6+§65,000g", " §7Top 10: §6+§62,500g", " §7Top 15: §6+§61,000g",
                    "§7on leaderboard major events.")),
    PURE_RAGE("Pure Rage", 1, 14, Material.LEATHER_LEGGINGS, 9, new int[]{35}, true,
            lines("§7Access §4Rage Pants§7, offering", "§7counter-measures to aggravate", "§7your victims.")),
    BANG_LAUNCH_TRAIL("Bang Launch Trail", 1, 15, Material.EXPLOSIVE_MINECART, 9, new int[]{50},
            lines("§7Go out with a bang!", "", "§7§oThis is a cosmetic effect when using", "§7§othe slime launchers.")),
    FAST_PASS("Fast Pass", 1, 16, Material.ACTIVATOR_RAIL, 10, new int[]{100},
            lines("§7Start at level 50 after prestige.")),
    COOL_CONCLAVE("Cool Conclave", 1, 19, Material.ICE, 10, new int[]{10},
            lines("§7Unlock the §b/cool §7command to list the", "§7high prestige players in your", "§7instance.")),
    INVENTORY_MANAGEMENT("Inventory Management", 1, 20, Material.ENDER_CHEST, 11, new int[]{50, 100, 150},
            new int[]{11, 15, 19}, true, lines("§7§5+1 row §7in ender chest.")),
    PROMOTION("Promotion!!", 1, 21, Material.SPRUCE_FENCE, 12, new int[]{30},
            lines("§7Upgrade the \"Assistant (to the)", "§7Streaker\" perk:", "", "§aAssistant Streaker",
                    "§7Earn §6+2g §7and §b+15% XP §7from kills and", "§7assists.", "",
                    "§7Gain §e+1 §7streak every §c4th kill§7.")),
    EXPERIENCE_INDUSTRIAL_COMPLEX("Experience-Industrial Complex", 1, 22, Material.DIAMOND_BARDING, 13, new int[]{80},
            lines("§7Streak §bXP §7bonus scales up to §c200 kills§7.", "§b+50 max XP §7per kill.")),
    RAW_NUMBERS("Raw Numbers", 1, 23, Material.MELON_SEEDS, 13, new int[]{40},
            lines("§7View the exact damage you deal in", "§7the action bar.")),
    ONE_UPPER("One Upper", 1, 24, Material.PISTON_BASE, 14, new int[]{40},
            lines("§7Can purchase §d+1 tier §7on passives.")),
    BOUNTY_CLOAK("Bounty Cloak", 1, 25, Material.GLOWSTONE_DUST, 15, new int[]{100},
            lines("§7Use the bounty effect in any Hypixel", "§7Lobby in the collectibles!")),
    CELEBRITY("Celebrity", 1, 28, Material.RAW_FISH, 25, new int[]{300},
            lines("§7Earn §6double gold §7from kills.")); // 説明文は wiki から (録画では Unknown upgrade)

    /** Renown Shop - Upgrades のページ数。 */
    public static final int PAGES = 2;

    private final String displayName;
    private final int page;
    private final int slot;
    private final Material icon;
    private final int requiredPrestige;
    private final int[] costs;
    /** Tier ごとの必要プレステージ。 */
    private final int[] tierPrestige;
    private final boolean available;
    private final List<String> description;

    RenownUpgrade(String displayName, int page, int slot, Material icon, int requiredPrestige, int[] costs, List<String> description) {
        this(displayName, page, slot, icon, requiredPrestige, costs, null, true, description);
    }

    RenownUpgrade(String displayName, int page, int slot, Material icon, int requiredPrestige, int[] costs, boolean available,
                  List<String> description) {
        this(displayName, page, slot, icon, requiredPrestige, costs, null, available, description);
    }

    RenownUpgrade(String displayName, int page, int slot, Material icon, int requiredPrestige, int[] costs, int[] tierPrestige,
                  boolean available, List<String> description) {
        this.displayName = displayName;
        this.page = page;
        this.slot = slot;
        this.icon = icon;
        this.requiredPrestige = requiredPrestige;
        this.costs = costs;
        this.tierPrestige = tierPrestige;
        this.available = available;
        this.description = description;
    }

    private static List<String> lines(String... lines) {
        return Collections.unmodifiableList(Arrays.asList(lines));
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getPage() {
        return page;
    }

    public int getSlot() {
        return slot;
    }

    public Material getIcon() {
        return icon;
    }

    /** アイコンのデータ値 (Celebrity はフグ = 生魚の 3、ユーザーから)。 */
    public short getIconData() {
        return (short) (this == CELEBRITY ? 3 : 0);
    }

    public int getRequiredPrestige() {
        return requiredPrestige;
    }

    public int getMaxTier() {
        return costs.length;
    }

    public boolean isTiered() {
        return costs.length > 1;
    }

    /** tier (1 から) を買う Renown。 */
    public int cost(int tier) {
        return costs[tier - 1];
    }

    /** tier (1 から) を買うのに必要なプレステージ。 */
    public int requiredPrestige(int tier) {
        return tierPrestige == null ? requiredPrestige : Math.max(requiredPrestige, tierPrestige[tier - 1]);
    }

    /** 効果を作ってあって買えるか。 */
    public boolean isAvailable() {
        return available;
    }

    /** 説明 (Tier があるものは "Each tier:" の下)。 */
    public List<String> getDescription() {
        return description;
    }

    /** 全部の Tier の数 (Renown Shop の "Unlocked: 28/89" の分母)。 */
    public static int totalTiers() {
        int total = 0;
        for (RenownUpgrade upgrade : values()) {
            total += upgrade.getMaxTier();
        }
        return total;
    }

    public static RenownUpgrade fromName(String name) {
        for (RenownUpgrade upgrade : values()) {
            if (upgrade.name().equalsIgnoreCase(name)) {
                return upgrade;
            }
        }
        return null;
    }
}
