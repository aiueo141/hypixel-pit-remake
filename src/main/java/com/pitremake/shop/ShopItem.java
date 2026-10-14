package com.pitremake.shop;

import com.pitremake.util.NbtUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Items の村人で買えるアイテム (録画 2026-10-14_08-53-15.mcrc の "Non-permanent items" の値段・説明)。
 * 最初に買えるのは 5 つで、残りは Renown Shop の Shop Items で解放する (pit.wiki の Item Shop。
 * Tactical Insertion と Bounty Solvent は作らない)。
 * 買ったアイテムは名前も説明も無いバニラのままで、Unbreakable (表示する) だけ付く。死ぬと無くなる (Pants Bundle 以外)。
 */
public enum ShopItem {
    DIAMOND_SWORD(11, Material.DIAMOND_SWORD, 0, 1, "Diamond Sword", 150, 0, 0,
            "§9+20% damage vs bountied"),
    OBSIDIAN(12, Material.OBSIDIAN, 0, 8, "Obsidian", 40, 0, 0,
            "§7Remains for 120 seconds."),
    GOLDEN_PICKAXE(39, Material.GOLD_PICKAXE, 0, 1, "Golden Pickaxe", 500, 0, 0,
            "§7Breaks a 5-high pillar of obsidian", "§7when 2-tapping it."),
    DIAMOND_CHESTPLATE(14, Material.DIAMOND_CHESTPLATE, 0, 1, "Diamond Chestplate", 500, 0, 0,
            "§7Auto-equips on buy!"),
    DIAMOND_BOOTS(15, Material.DIAMOND_BOOTS, 0, 1, "Diamond Boots", 300, 0, 0,
            "§7Auto-equips on buy!"),
    IRON_PACK(29, Material.IRON_CHESTPLATE, 0, 1, "Iron Pack", 100, 1, 5,
            "§7Iron chestplate, leggings and", "§7boots.", "", "§7Auto-equips on buy!"),
    OBSIDIAN_STACK(30, Material.OBSIDIAN, 0, 64, "Obsidian Stack", 240, 2, 5,
            "§7Remains for 120 seconds.", "§7§a25% cheaper §7than regular", "§7obsidian."),
    /** 値段はユーザーのスクリーンショット (350g。wiki は 750g)。攻撃力はダイヤの剣と同じ +7。 */
    COMBAT_SPADE(32, Material.DIAMOND_SPADE, 0, 1, "Combat Spade", 350, 3, 10,
            "§7Deals §9+1 damage §7per", "§bdiamond piece §7on enemy.", "", "§9+7 Attack Damage"),
    DIAMOND_LEGGINGS(31, Material.DIAMOND_LEGGINGS, 0, 1, "Diamond Leggings", 1_200, 3, 20,
            "§7Auto-equips on buy!"),
    /** 見た目はムーシュルームのスポーンエッグ (データ値 96)。クールダウン中はスケルトン (51)。 */
    FIRST_AID_EGG(38, Material.MONSTER_EGG, 96, 1, "First-Aid Egg", 200, 5, 15,
            "§7Heals §c2.5❤ §7and removes §eSpeed", "§7on use. §7(30s cooldown, §e-5s", "§7per kill during cooldown)",
            "", "§eRight-click to use!"),
    JUMP_BOOST(40, Material.POTION, 8203, 1, "Jump Boost IV", 150, 7, 20,
            "§9Jump Boost IV (0:30)", "§7Jump about 4.2 blocks high."),
    /** スクリーンショットに無い (場所は推測)。41 は作らない Tactical Insertion の場所。 */
    PANTS_BUNDLE(33, Material.MINECART, 0, 1, "Pants Bundle", 50, 12, 20,
            "§7Stores up to 10 §fFresh Pants§7.", "§7Kept on death.");

    /** ExtraAttributes のキー (値は ordinal + 1)。 */
    static final String KEY = "PitShopItem";
    public static final int SKELETON_EGG = 51;
    static final double SPADE_ATTACK_DAMAGE = 7;

    /** "Non-permanent items" の位置 (ユーザーのスクリーンショット)。 */
    private final int slot;
    private final Material material;
    private final short data;
    private final int amount;
    private final String displayName;
    private final int cost;
    private final int renownPrestige;
    private final int renownCost;
    private final List<String> description;

    ShopItem(int slot, Material material, int data, int amount, String displayName, int cost, int renownPrestige, int renownCost,
             String... description) {
        this.slot = slot;
        this.material = material;
        this.data = (short) data;
        this.amount = amount;
        this.displayName = displayName;
        this.cost = cost;
        this.renownPrestige = renownPrestige;
        this.renownCost = renownCost;
        this.description = Collections.unmodifiableList(Arrays.asList(description));
    }

    public int getSlot() {
        return slot;
    }

    public Material getMaterial() {
        return material;
    }

    public short getData() {
        return data;
    }

    public int getAmount() {
        return amount;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getCost() {
        return cost;
    }

    public List<String> getDescription() {
        return description;
    }

    /** 最初から買えるか (それ以外は Renown Shop の Shop Items で解放)。 */
    public boolean isDefault() {
        return renownCost == 0;
    }

    public int getRenownPrestige() {
        return renownPrestige;
    }

    public int getRenownCost() {
        return renownCost;
    }

    /** UpgradeManager の解放の key。 */
    public String unlockKey() {
        return "item:" + name();
    }

    /** 着るもの (着ている間は 1 つしか買えない・Autobuy も 1 つだけ)。 */
    public boolean isArmor() {
        return this == DIAMOND_CHESTPLATE || this == DIAMOND_BOOTS || this == DIAMOND_LEGGINGS || this == IRON_PACK;
    }

    /** 持っていたらもう買わない (Autobuy) もの: 防具・武器・道具。 */
    public boolean isSingle() {
        return isArmor() || this == DIAMOND_SWORD || this == GOLDEN_PICKAXE || this == COMBAT_SPADE
                || this == FIRST_AID_EGG || this == PANTS_BUNDLE;
    }

    /** 買ったときに渡すアイテム (Iron Pack は 3 つ)。 */
    public List<ItemStack> createAll() {
        List<ItemStack> items = new ArrayList<>();
        if (this == IRON_PACK) {
            for (Material piece : new Material[]{Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS}) {
                items.add(tag(unbreakable(new ItemStack(piece))));
            }
            return items;
        }
        items.add(create());
        return items;
    }

    /** 買ったときに渡すアイテム (バニラのまま + Unbreakable)。黒曜石は耐久が無いのでそのまま。 */
    public ItemStack create() {
        ItemStack item = new ItemStack(material, amount, data);
        switch (this) {
            case OBSIDIAN:
            case OBSIDIAN_STACK:
                break;
            case FIRST_AID_EGG:
                item = named(item, "§cFirst-Aid Egg", getDescription());
                break;
            case JUMP_BOOST:
                PotionMeta potion = (PotionMeta) item.getItemMeta();
                potion.clearCustomEffects();
                potion.addCustomEffect(new PotionEffect(PotionEffectType.JUMP, 30 * 20, 3), true);
                potion.setDisplayName("§aJump Boost IV");
                item.setItemMeta(potion);
                break;
            case PANTS_BUNDLE:
                return PantsBundle.create();
            case COMBAT_SPADE:
                // ダイヤの剣と同じ攻撃力 (+7 Attack Damage と表示される)
                // 名前と説明はユーザーのスクリーンショット ("+7 Attack Damage" はバニラの攻撃力の表示)
                item = named(unbreakable(item), "§bCombat Spade", Arrays.asList("§7Deals §9+1 damage §7per", "§bdiamond piece §7on enemy."));
                item = NbtUtil.setAttribute(item, "generic.attackDamage", SPADE_ATTACK_DAMAGE);
                break;
            default:
                item = unbreakable(item);
                break;
        }
        return tag(item);
    }

    private ItemStack tag(ItemStack item) {
        return NbtUtil.setInt(item, KEY, ordinal() + 1);
    }

    private static ItemStack unbreakable(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.spigot().setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack named(ItemStack item, String name, List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /** そのアイテムがこのショップのものなら種類、違えば null。 */
    public static ShopItem of(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !NbtUtil.has(item, KEY)) {
            return null;
        }
        int index = NbtUtil.getInt(item, KEY) - 1;
        return index >= 0 && index < values().length ? values()[index] : null;
    }
}
