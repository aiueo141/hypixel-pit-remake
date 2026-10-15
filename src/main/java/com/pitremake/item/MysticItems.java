package com.pitremake.item;

import com.pitremake.enchant.EnchantRegistry;
import com.pitremake.util.NbtUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class MysticItems {

    public static final String NONCE = "Nonce";

    /** Fresh Pants の防御ポイント (鉄のレギンスと同じ)。 */
    public static final int PANTS_ARMOR_POINTS = 5;

    /** Rage Pants の Nonce は常にこの値。 */
    public static final int RAGE_PANTS_NONCE = 9;
    /** Dark Pants の Nonce は常にこの値。 */
    public static final int DARK_PANTS_NONCE = 6;

    private static final double SWORD_DAMAGE = 6.5;

    private MysticItems() {
    }

    /** 毎回ランダムな Nonce を持つ Fresh Mystic Sword を作成する。 */
    public static ItemStack freshSword() {
        ItemStack item = new ItemStack(Material.GOLD_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Mystic Sword");
        meta.setLore(defaultLore());
        meta.spigot().setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
        item.setItemMeta(meta);

        item = NbtUtil.setAttribute(item, "generic.attackDamage", SWORD_DAMAGE);
        return withNonce(item);
    }

    /** 毎回ランダムな Nonce を持つ Fresh Mystic Bow を作成する。 */
    public static ItemStack freshBow() {
        ItemStack item = new ItemStack(Material.BOW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Mystic Bow");
        meta.setLore(defaultLore());
        meta.spigot().setUnbreakable(true);
        meta.addEnchant(Enchantment.LURE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE,ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);


        return withNonce(item);
    }

    /**
     * Fresh Pants を作成する。防御力は {@link #PANTS_ARMOR_POINTS}。
     * Nonce はランダムだが Nonce % 5 がこの色になるようにする ({@link PantsColor#fromNonce})。
     * そのため Tier III への強化で生贄に必要なパンツは、このパンツと同じ色になる。
     */
    public static ItemStack freshPants(PantsColor color) {
        ItemStack item = new ItemStack(Material.LEATHER_LEGGINGS);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setDisplayName(color.getChatColor() + "Fresh " + color.getDisplayName() + " Pants");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Kept on death",
                "",
                color.getChatColor() + "Used in the mystic well",
                color.getChatColor() + "Also, a fashion statement"
        ));
        meta.setColor(color.getColor());
        // パンツは Unbreakable を隠さない (Hypixel と同じく表示する)
        meta.spigot().setUnbreakable(true);
        item.setItemMeta(meta);

        int nonce = randomNonce();
        return NbtUtil.setInt(item, NONCE, nonce - Math.floorMod(nonce, 5) + color.nonceIndex());
    }

    /**
     * Rage Pants を作成する。Kept on death ではなく、Tier I への強化では Rage エンチャントだけが付く。
     * 防御力は通常のパンツと同じ {@link #PANTS_ARMOR_POINTS}。
     */
    public static ItemStack freshRagePants() {
        PantsColor color = PantsColor.RAGE;
        ItemStack item = new ItemStack(Material.LEATHER_LEGGINGS);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setDisplayName(color.getChatColor() + "Fresh Rage Pants");
        meta.setLore(defaultLore());
        meta.setColor(color.getColor());
        // パンツは Unbreakable を隠さない (Hypixel と同じく表示する)
        meta.spigot().setUnbreakable(true);
        item.setItemMeta(meta);

        MysticData data = MysticData.read(item);
        data.setNonce(RAGE_PANTS_NONCE);
        data.setRagePants(true);
        return data.write(item);
    }

    /**
     * Fresh Dark Pants を作成する。Nonce は常に {@link #DARK_PANTS_NONCE}。
     * Tier I で必ず Somber、Tier II で Dark エンチャントが 1 つ付く。防御力は革のレギンスのまま。
     */
    public static ItemStack freshDarkPants() {
        return freshSpecialPants(SpecialPants.DARK, "Edgy fashion", DARK_PANTS_NONCE);
    }

    /** Fresh Sewer Pants を作成する。Tier I までで、Sewer エンチャントが 1 つ付く。 */
    public static ItemStack freshSewerPants() {
        return freshSpecialPants(SpecialPants.SEWER, "Looks horrible and reeks", randomNonce());
    }

    /** Fresh Aqua Pants を作成する。Tier I までで、Club Rod と Aqua エンチャントが 1 つ付く。 */
    public static ItemStack freshAquaPants() {
        return freshSpecialPants(SpecialPants.AQUA, "Aqua obtained from fishing!", randomNonce());
    }

    /** 通常の Fresh Pants と同じ形の Lore (最後の 1 行だけパンツごとの一言) の特殊なパンツ。 */
    private static ItemStack freshSpecialPants(SpecialPants special, String flavor, int nonce) {
        PantsColor color = special.getColor();
        ItemStack item = new ItemStack(Material.LEATHER_LEGGINGS);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setDisplayName(color.getChatColor() + "Fresh " + color.getDisplayName() + " Pants");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Kept on death",
                "",
                color.getChatColor() + "Used in the mystic well",
                color.getChatColor() + flavor
        ));
        meta.setColor(color.getColor());
        // パンツは Unbreakable を隠さない (Hypixel と同じく表示する)
        meta.spigot().setUnbreakable(true);
        item.setItemMeta(meta);

        MysticData data = MysticData.read(item);
        data.setNonce(nonce);
        data.setSpecialPants(special);
        return data.write(item);
    }

    public static boolean isMysticPants(ItemStack item) {
        return item != null && item.getType() == Material.LEATHER_LEGGINGS && NbtUtil.has(item, NONCE);
    }

    public static boolean isMystic(ItemStack item) {
        return MysticType.of(item) != null && NbtUtil.has(item, NONCE);
    }

    /**
     * 強化済みアイテムの名前・Lore・見た目を {@link MysticData} に合わせて更新したコピーを返す。
     * Fresh のアイテムはそのまま返す。
     */
    public static ItemStack applyDisplay(ItemStack item, EnchantRegistry registry) {
        MysticType type = MysticType.of(item);
        MysticData data = MysticData.read(item);
        if (type == null || data.isFresh()) {
            return item;
        }
        ItemStack copy = item.clone();
        ItemMeta meta = copy.getItemMeta();
        PantsColor pantsColor = meta instanceof LeatherArmorMeta
                ? PantsColor.fromColor(((LeatherArmorMeta) meta).getColor()) : null;
        meta.setDisplayName(MysticLore.name(data, type, pantsColor, registry));
        meta.setLore(MysticLore.build(data, type, pantsColor, registry));
        // 強化後は攻撃力を表示する
        meta.removeItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        if (hasGlint(type, data.getTier())) {
            meta.addEnchant(Enchantment.LURE, 1, true);
        } else {
            meta.removeEnchant(Enchantment.LURE);
        }
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        // パンツは Unbreakable を表示し、剣と弓は隠す
        if (type == MysticType.PANTS) {
            meta.removeItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        } else {
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        }
        copy.setItemMeta(meta);
        return copy;
    }

    /** 弓は常に、剣は Tier III のみ光沢を付ける。パンツは付けない。 */
    private static boolean hasGlint(MysticType type, int tier) {
        switch (type) {
            case BOW:
                return true;
            case SWORD:
                return tier >= 3;
            default:
                return false;
        }
    }

    private static List<String> defaultLore() {
        return Arrays.asList(
                ChatColor.GRAY + "Kept on death",
                "",
                ChatColor.GRAY + "Used in the mystic well"
        );
    }

    /**
     * Nonce だけを振り直したコピーを返す。パンツの色 (Nonce % 5) は保つ。
     * Rage / Dark Pants は Nonce が固定なのでそのままコピーする。
     */
    public static ItemStack withNewNonce(ItemStack item) {
        MysticData data = MysticData.read(item);
        if (data.isRagePants() || data.isDarkPants()) {
            return item.clone();
        }
        int nonce = randomNonce();
        return NbtUtil.setInt(item.clone(), NONCE, nonce - Math.floorMod(nonce, 5) + Math.floorMod(data.getNonce(), 5));
    }

    private static ItemStack withNonce(ItemStack item) {
        return NbtUtil.setInt(item, NONCE, randomNonce());
    }

    private static int randomNonce() {
        return ThreadLocalRandom.current().nextInt(1_000_000, 10_000_000);
    }
}
