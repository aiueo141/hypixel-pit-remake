package com.pitremake.item;

import com.pitremake.util.NbtUtil;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 報酬の防具 (pit.wiki の各ページ)。エンチャントは固定で、Mystic Well には入れられない。
 * <pre>
 * Archangel Chestplate : ダイヤのチェストプレート、Lives 100、Guardian (被ダメージ -10%)、Angel Faction の報酬
 * Armageddon Boots     : 赤い革のブーツ、Lives 66、Evil Within (攻撃が Somber を無視)、Demon Faction の報酬
 * Golden Helmet        : 金のヘルメット (ダイヤと同じ防御力)、Lives 50、Royalty (キルの XP +10%)、King's Quest の報酬
 * </pre>
 * 表示 (名前・Lore) は Hypixel のアイテムの画像のまま。
 */
public final class RewardItems {

    /**
     * 録画 (2026-10-14_09-15-41.mcrc) の本物のアイテムと同じく、ミスティックと同じ形のデータを持つ
     * (Nonce・Lives・MaxLives・UpgradeTier 1・CustomEnchants)。種類はエンチャントのキーで見分ける。
     * guardian / royalty は録画のキー、evil_within は推測。
     */
    public enum Type {
        ARCHANGEL_CHESTPLATE(Material.DIAMOND_CHESTPLATE, "guardian"),
        ARMAGEDDON_BOOTS(Material.LEATHER_BOOTS, "evil_within"),
        GOLDEN_HELMET(Material.GOLD_HELMET, "royalty");

        final Material material;
        final String enchantKey;

        Type(Material material, String enchantKey) {
            this.material = material;
            this.enchantKey = enchantKey;
        }
    }

    static final int ARCHANGEL_LIVES = 100;
    static final int ARMAGEDDON_LIVES = 66;
    static final int GOLDEN_HELMET_LIVES = 50;
    /** Guardian: 受けるダメージ -10%。 */
    public static final double GUARDIAN_MULTIPLIER = 0.9;
    /** Royalty: キルの XP +10%。 */
    public static final double ROYALTY_XP = 0.10;
    /** Golden Helmet はダイヤのヘルメット (3) と同じ防御ポイント。金のヘルメットは 2。 */
    public static final int GOLDEN_HELMET_EXTRA_ARMOR = 1;
    private static final Color ARMAGEDDON_COLOR = Color.fromRGB(0xFF0000);

    private RewardItems() {
    }

    public static ItemStack archangelChestplate() {
        return create(Type.ARCHANGEL_CHESTPLATE, new ItemStack(Material.DIAMOND_CHESTPLATE), "§bArchangel Chestplate",
                ARCHANGEL_LIVES, Arrays.asList(
                        "§9Guardian",
                        "§7Receive §9-10% §7damage",
                        "",
                        "§bAngel Faction Reward"));
    }

    public static ItemStack armageddonBoots() {
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta meta = (LeatherArmorMeta) boots.getItemMeta();
        meta.setColor(ARMAGEDDON_COLOR);
        boots.setItemMeta(meta);
        return create(Type.ARMAGEDDON_BOOTS, boots, "§cArmageddon Boots", ARMAGEDDON_LIVES, Arrays.asList(
                "§9Evil Within",
                "§7Your hits ignore Somber",
                "",
                "§cDemon Faction Reward"));
    }

    public static ItemStack goldenHelmet() {
        return create(Type.GOLDEN_HELMET, new ItemStack(Material.GOLD_HELMET), "§6Golden Helmet", GOLDEN_HELMET_LIVES,
                Arrays.asList(
                        "§9Royalty",
                        "§7Earn §b+10% XP §7from kills",
                        "",
                        "§6As strong as diamond"));
    }

    public static boolean is(ItemStack item, Type type) {
        return item != null && item.getType() == type.material && MysticData.read(item).getEnchantLevel(type.enchantKey) > 0;
    }

    /** Lives を変えたコピー (Lore の 1 行目も書き換える)。 */
    public static ItemStack withLives(ItemStack item, int lives) {
        int maxLives = NbtUtil.getInt(item, MysticData.MAX_LIVES);
        ItemStack copy = NbtUtil.setInt(item.clone(), MysticData.LIVES, lives);
        ItemMeta meta = copy.getItemMeta();
        if (meta.hasLore() && !meta.getLore().isEmpty()) {
            List<String> lore = new ArrayList<>(meta.getLore());
            lore.set(0, livesLine(lives, maxLives));
            meta.setLore(lore);
            copy.setItemMeta(meta);
        }
        return copy;
    }

    /** 例: §7Lives: §a100§7/100 (少ないと赤)。 */
    static String livesLine(int lives, int maxLives) {
        return "§7Lives: " + MysticLore.livesColor(lives) + lives + "§7/" + maxLives;
    }

    /** 録画のとおり Unbreakable は隠さない。 */
    private static ItemStack create(Type type, ItemStack item, String name, int lives, List<String> enchantLore) {
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>();
        lore.add(livesLine(lives, lives));
        lore.add("");
        lore.addAll(enchantLore);
        meta.setLore(lore);
        meta.spigot().setUnbreakable(true);
        item.setItemMeta(meta);
        MysticData data = MysticData.read(item);
        data.setNonce(ThreadLocalRandom.current().nextInt());
        data.setMaxLives(lives);
        data.setLives(lives);
        data.setTier(1);
        data.setEnchant(type.enchantKey, 1);
        return data.write(item);
    }
}
