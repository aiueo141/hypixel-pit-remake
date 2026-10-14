package com.pitremake.upgrade;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Passive (pit.wiki の Passives)。Upgrades の村人の "Permanent upgrades" 画面の 28〜34 で買う (位置と見た目は録画のまま)。
 * Tier VI は Renown の One Upper が必要 (値段と必要レベルは wiki)。Build Battler は置いたブロックが消えるまでの時間を延ばす。
 */
public enum Passive {
    XP_BOOST("XP Boost", Material.INK_SACK, 12, 28,
            new int[]{500, 2_500, 5_000, 10_000, 25_000, 50_000}, new int[]{10, 30, 50, 80, 100, 100},
            10, "§7Earn §b+{v}% XP §7from all sources."),
    GOLD_BOOST("Gold Boost", Material.INK_SACK, 14, 29,
            new int[]{1_000, 2_500, 10_000, 25_000, 40_000, 100_000}, new int[]{20, 35, 50, 70, 90, 90},
            10, "§7Earn §6+{v}% gold (g) §7from kills and", "§7coin pickups."),
    MELEE_DAMAGE("Melee Damage", Material.INK_SACK, 1, 30,
            new int[]{450, 1_050, 1_500, 2_250, 3_000, 10_000}, new int[]{30, 50, 60, 70, 80, 90},
            1, "§7Deal §c+{v}% §7melee damage."),
    BOW_DAMAGE("Bow Damage", Material.INK_SACK, 11, 31,
            new int[]{450, 1_050, 1_500, 2_250, 3_000, 5_000}, new int[]{30, 50, 60, 70, 80, 90},
            3, "§7Deal §c+{v}% §7bow damage."),
    DAMAGE_REDUCTION("Damage Reduction", Material.INK_SACK, 6, 32,
            new int[]{450, 1_050, 1_500, 2_250, 3_000, 10_000}, new int[]{30, 50, 60, 70, 80, 90},
            1, "§7Receive §9-{v}% §7damage."),
    BUILD_BATTLER("Build Battler", Material.INK_SACK, 15, 33,
            new int[]{750, 1_750, 2_750, 3_750, 5_000, 7_500}, new int[]{40, 50, 55, 60, 65, 70},
            60, "§7Your blocks stay §a+{v}% §7longer."),
    EL_GATO("El Gato", Material.CAKE, 0, 34,
            new int[]{1_000, 2_000, 3_000, 4_000, 5_000, 6_000}, new int[]{50, 60, 70, 80, 90, 100}, 0);

    /** 普通に買える Tier。 */
    public static final int BASE_MAX_TIER = 5;
    /** One Upper (Renown) で買える Tier。 */
    public static final int MAX_TIER = 6;

    private final String displayName;
    private final Material icon;
    private final short data;
    private final int slot;
    private final int[] costs;
    private final int[] levels;
    /** 説明文の {v} に入る 1 Tier あたりの値 (% など)。 */
    private final int step;
    private final List<String> description;

    Passive(String displayName, Material icon, int data, int slot, int[] costs, int[] levels, int step, String... description) {
        this.displayName = displayName;
        this.icon = icon;
        this.data = (short) data;
        this.slot = slot;
        this.costs = costs;
        this.levels = levels;
        this.step = step;
        this.description = Collections.unmodifiableList(Arrays.asList(description));
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public short getData() {
        return data;
    }

    public int getSlot() {
        return slot;
    }

    /** tier (1〜5) を買う値段。 */
    public int cost(int tier) {
        return costs[tier - 1];
    }

    /** tier (1〜5) を買うのに必要なレベル。 */
    public int requiredLevel(int tier) {
        return levels[tier - 1];
    }

    /** 1 Tier 分の説明 ("Each tier:" の下)。 */
    public List<String> getDescription() {
        return description(1);
    }

    /** その Tier までの合計の説明 (例: Gold Boost V なら +50%)。El Gato は別に作る。 */
    public List<String> description(int tier) {
        List<String> lines = new ArrayList<>();
        for (String line : description) {
            lines.add(line.replace("{v}", String.valueOf(step * tier)));
        }
        return lines;
    }
}
