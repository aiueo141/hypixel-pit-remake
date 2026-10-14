package com.pitremake.upgrade;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * キルストリーク (メガストリーク以外)。録画 2026-10-14_20-50-01.mcrc の "Choose a killstreak" (54) の位置・アイコン・説明文、
 * 必要レベル・値段・Renown のバンドルは pit.wiki の Killstreaks。
 * <pre>
 * 行ごとに 1 / 10 / 19 / 28 / 37 が見出し (額縁 "3 Kills" など)、その右に並ぶ。49 が戻る
 * </pre>
 * アイコンはユーザーのスクリーンショット (全部解放した Choose a killstreak) から。
 */
public enum Killstreak {
    SECOND_GAPPLE("Second Gapple", 3, 2, Material.GOLDEN_APPLE, 0, 1, 10, 1_500, null, false,
            "§7Gain §b+5 XP§7, §6+§65g §7and an extra golden", "§7apple."),
    EXPLICIOUS("Explicious", 3, 3, Material.INK_SACK, 12, 1, 20, 3_000, null, false,
            "§7Gain §b+12 XP§7."),
    R_AND_R("R&R", 3, 4, Material.GOLDEN_CARROT, 0, 1, 40, 4_000, Megastreak.BEASTMODE, false,
            "§7Gain §9Resistance I §7and §cRegen II §7for", "§73s."),
    ARQUEBUSIER("Arquebusier", 3, 5, Material.ARROW, 0, 12, 50, 5_000, null, false,
            "§7Gain:", "§7■ §6+7g", "§7■ §f16 arrows §7(max 128)", "§7■ §eSpeed §7for 10 seconds"),
    KHANATE("Khanate", 3, 6, Material.GOLD_HELMET, 0, 1, 60, 6_000, Megastreak.HIGHLANDER, false,
            "§7Earn §6+§68g§7.", "", "§7Stack §c+4% damage §7vs bountied players", "§7up to §c+40%§7."),
    LEECH("Leech", 3, 7, Material.SPIDER_EYE, 0, 1, 70, 6_000, null, false,
            "§7Next hit heals for §c0.5❤ §7+ §c20% §7of its", "§7damage."),

    TOUGH_SKIN("Tough Skin", 5, 11, Material.LEATHER_CHESTPLATE, 0, 1, 30, 3_000, Megastreak.BEASTMODE, false,
            "§7Receive §9-3% §7damage.", "§7Stacks up to §9-24%§7."),
    FIGHT_OR_FLIGHT("Fight or Flight", 5, 12, Material.FIREBALL, 0, 1, 50, 5_000, null, false,
            "§7If below half §c❤§7:", "§7Gain §eSpeed I §7and §9Resistance I §7for 7 seconds.", "",
            "§7Otherwise:", "§7Deal §c+20% §7damage for 7 seconds."),
    PUNGENT("Pungent", 5, 13, Material.ROTTEN_FLESH, 0, 1, 50, 9_000, Megastreak.HERMIT, false,
            "§7Obtain a §cSmelly Bomb§7:", "§7Applies §9Slowness I §7to players within 3", "§7blocks for 5 seconds."),
    HEROS_HASTE("Hero's Haste", 5, 14, Material.ENCHANTED_BOOK, 0, 1, 100, 15_000, null, false,
            "§7Gain §eSpeed II §7for 5 seconds."),
    RUSH("Rush", 5, 15, Material.SUGAR, 0, 1, 110, 25_000, Megastreak.HIGHLANDER, false,
            "§7Gain §e1.5% Speed.", "§7Max §e+15% Speed."),

    FEAST("Feast", 7, 20, Material.MUTTON, 0, 1, 30, 4_000, null, false,
            "§7Obtain a §6AAA-Rated Steak:", "§7■ §c+20% damage", "§7■ §eSpeed I", "§7■ §9Resistance I", "§7Insta-eat (0:10)"),
    COUNTER_STRIKE("Counter-Strike", 7, 21, Material.IRON_BARDING, 0, 1, 40, 5_000, null, false,
            "§7Deal §c+15% damage §7and block §91❤", "§9§7per hit for 8s."),
    GOLD_NANO_FACTORY("Gold Nano-factory", 7, 22, Material.GOLD_NUGGET, 0, 1, 50, 6_000, Megastreak.HIGHLANDER, false,
            "§7Obtain a molecular assembler:", "§7Spawns §67 gold ingots.", "§7Grants §cRegen IV §7for 2 seconds."),
    TACTICAL_RETREAT("Tactical Retreat", 7, 23, Material.DOUBLE_PLANT, 2, 1, 50, 5_000, Megastreak.BEASTMODE, false,
            "§7§7Gain §cRegeneration IV §7and §cWeakness", "§cIV §7for 5 seconds."),
    GLASS_PICKAXE("Glass Pickaxe", 7, 24, Material.DIAMOND_PICKAXE, 0, 1, 60, 6_000, Megastreak.HERMIT, false,
            "§7Get a single-use weapon with §9+8.5", "§9Damage §7and §c+0.5❤ §7true damage."),
    ASSURED_STRIKE("Assured Strike", 7, 25, Material.COOKED_CHICKEN, 0, 1, 80, 10_000, null, false,
            "§7Your next melee hit deals §c+35%", "§cdamage §7and grants §eSpeed I §7for 20", "§7seconds."),

    AURA_OF_PROTECTION("Aura of Protection", 10, 29, Material.SLIME_BALL, 0, 1, 50, 8_000, Megastreak.HERMIT, false,
            "§7Gain an §aAura of Protection §7spell item.", "", "§aAura of Protection", "§9Resistance II §7(0:04)",
            "§eTrue Damage §7immunity §7(0:15)"),
    ICE_CUBE("Ice Cube", 10, 30, Material.PACKED_ICE, 0, 1, 60, 9_000, Megastreak.HERMIT, false,
            "§7Get an §bIce Cube §7item.", "", "§bIce Cube", "§7Single-Use on melee strike.", "§7Deals §c1❤ §7true damage to victim.",
            "§7Gain §b40 XP§7.", "§7Attacks slow enemies for 10 seconds."),
    SUPER_STREAKER("Super Streaker", 10, 31, Material.BREAD, 0, 1, 80, 20_000, Megastreak.TO_THE_MOON, false,
            "§7Gain §b+50 XP§7.", "", "§7Stack a buff of §b+5% XP §7from kills.", "§7(Up to §b+50%§7)"),
    GOLD_STACK("Gold Stack", 10, 32, Material.MOB_SPAWNER, 0, 1, 90, 25_000, Megastreak.TO_THE_MOON, false,
            "§7Permanently gain §6+0.1g §7per kill.", "§7Maximum: §6+{goldmax}g", "", "§8Bonus also applies when not selected.",
            "§8Resets on prestige."),
    XP_STACK("XP Stack", 10, 33, Material.MOB_SPAWNER, 0, 1, 90, 25_000, Megastreak.TO_THE_MOON, false,
            "§7Permanently gain §b+0.05 XP §7per kill.", "§7Maximum: §b+{xpmax} XP", "", "§8Bonus also applies when not selected.",
            "§8Resets on prestige."),

    MONSTER("Monster", 25, 38, Material.APPLE, 0, 1, 40, 10_000, Megastreak.BEASTMODE, false,
            "§7Gain an extra max §c❤ §7(max 2)."),
    SPONGESTEVE("Spongesteve", 25, 39, Material.SPONGE, 0, 1, 70, 12_000, null, false,
            "§7Gain §615❤ Absorption§7."),
    APOSTLE_TO_RNGESUS("Apostle to RNGesus", 25, 40, Material.SNOW, 0, 1, 100, 50_000, null, false,
            "§7Roll a number between 1-100:", "§e■ §727 or 43: §e+1 Renown", "§e■ §742: §6+10❤ absorption", "§e■ §750: §bDiamond Helmet",
            "§e■ §713 or 66: §5+3 Chunk of Vile", "§e■ §777 or 88: §d+1 Mystic Drop", "§e■ §799: Smited for §c4❤",
            "§e■ §7Multiple of 2: §cRegeneration I §7(0:20)", "§e■ §7Multiple of 3: §eSpeed I §7(0:15)",
            "§e■ §7Multiple of 4: §cRegeneration II §7(0:10)", "§e■ §7Multiple of 7: Gain between §6500g§7-§65,000g",
            "§e■ §7Multiple of 8: Gain between §b500§7-§b5,000 XP", "§e■ §7Multiple of 12: §6+4❤ absorption",
            "§e■ §7Multiple of 13: §c+25% damage §7(0:30)");

    /** Leech / Assured Strike / Apostle to RNGesus は Magnum Opus のバンドル (Magnum Opus は未実装なので解放の key だけ使う)。 */
    public static final String MAGNUM_OPUS_KEY = "streak:MAGNUM_OPUS";

    private final String displayName;
    private final int every;
    private final int slot;
    private final Material icon;
    private final short data;
    private final int amount;
    private final int requiredLevel;
    private final int cost;
    /** Renown Shop の Killstreaks のバンドル (null なら最初から。Magnum Opus は MAGNUM_OPUS_KEY)。 */
    private final Megastreak bundle;
    private final boolean iconGuessed;
    private final List<String> description;

    Killstreak(String displayName, int every, int slot, Material icon, int data, int amount, int requiredLevel, int cost,
               Megastreak bundle, boolean iconGuessed, String... description) {
        this.displayName = displayName;
        this.every = every;
        this.slot = slot;
        this.icon = icon;
        this.data = (short) data;
        this.amount = amount;
        this.requiredLevel = requiredLevel;
        this.cost = cost;
        this.bundle = bundle;
        this.iconGuessed = iconGuessed;
        this.description = Collections.unmodifiableList(Arrays.asList(description));
    }

    public String getDisplayName() {
        return displayName;
    }

    /** 何キルごとに発動するか。 */
    public int getEvery() {
        return every;
    }

    public int getSlot() {
        return slot;
    }

    public Material getIcon() {
        return icon;
    }

    public short getData() {
        return data;
    }

    public int getAmount() {
        return amount;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public int getCost() {
        return cost;
    }

    public boolean isIconGuessed() {
        return iconGuessed;
    }

    public List<String> getDescription() {
        return description;
    }

    /** Renown Shop の解放の key (最初から使えるものは null)。 */
    public String bundleKey() {
        if (this == LEECH || this == ASSURED_STRIKE || this == APOSTLE_TO_RNGESUS) {
            return MAGNUM_OPUS_KEY;
        }
        return bundle == null ? null : bundle.unlockKey();
    }

    /** バンドルの名前 (録画: §cBundle: §9Hermit)。 */
    public String bundleName() {
        if (bundleKey() == null) {
            return null;
        }
        if (bundle == null) {
            return "§eMagnum Opus";
        }
        switch (bundle) {
            case BEASTMODE:
                return "§aBeastmode";
            case HERMIT:
                return "§9Hermit";
            case HIGHLANDER:
                return "§6Highlander";
            case TO_THE_MOON:
                return "§bTo the Moon";
            default:
                return "§f" + bundle.getDisplayName();
        }
    }

    /** "Choose a killstreak" の見出しの位置と文字 (額縁)。 */
    public static final int[][] HEADERS = {{1, 3}, {10, 5}, {19, 7}, {28, 10}, {37, 25}};

    public static Killstreak fromName(String name) {
        for (Killstreak killstreak : values()) {
            if (killstreak.name().equalsIgnoreCase(name)) {
                return killstreak;
            }
        }
        return null;
    }
}
