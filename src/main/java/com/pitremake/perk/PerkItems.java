package com.pitremake.perk;

import com.pitremake.util.NbtUtil;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NBTTagList;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * パークでもらうアイテム。
 * <pre>
 * Golden Head   : 録画 2026-10-14_09-32-22.mcrc の頭 (名前・説明・スキン)。Very Yummy の分 (+2 秒 / +0.5❤) は抜いた
 * Tasty Soup    : Soup の説明文
 * Olympus Potion: Olympus の説明文 (アイコンは "Renown Shop - Perks" と同じ 373:8203)
 * 丸石           : Mineman (名前・説明なし)
 * その他          : Mineman のピッケル・釣竿・溶岩バケツ・Safety First のヘルメット・キルの金リンゴ (バニラのまま)
 * </pre>
 * どれも ExtraAttributes の PitPerkItem で見分ける (Kept on death ではないので死ぬと無くなる。捨てられない)。
 */
public final class PerkItems {

    /** 並びは NBT の値なので、増やすときは最後に足す。 */
    public enum Type {
        GOLDEN_HEAD, TASTY_SOUP, OLYMPUS_POTION, MINEMAN_COBBLE,
        MINEMAN_PICKAXE, FISHING_ROD, LAVA_BUCKET, SAFETY_HELMET, GOLDEN_APPLE,
        FANCY_HAT, ASSIST_SOUP,
        /** Bounty Hunter の金のパンツ (印の番号がずれないよう最後に足す)。 */
        GOLDEN_PANTS
    }

    static final String KEY = "PitPerkItem";
    private static final String HEAD_ID = "c600ebdb-3db8-42be-8c28-811f6ce177eb";
    private static final String HEAD_TEXTURE = "eyJ0aW1lc3RhbXAiOjE0ODUwMjM0NDEyNzAsInByb2ZpbGVJZCI6ImRhNDk4YWM0ZTkzNzRlNWNiNjEyN2IzODA4NTU3OTgzIiwicHJvZmlsZU5hbWUiOiJOaXRyb2hvbGljXzIiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2Y5MzdlMWM0NWJiOGRhMjliMmM1NjRkZDlhN2RhNzgwZGQyZmU1NDQ2OGE1ZGZiNDExM2I0ZmY2NThmMDQzZTEifX19";

    private PerkItems() {
    }

    public static ItemStack goldenHead() {
        ItemStack item = named(new ItemStack(Material.SKULL_ITEM, 1, (short) 3), "§6Golden Head ", Arrays.asList(
                "§7Perk item",
                "§9Speed I (0:08)",
                "§9Regeneration II (0:05)",
                "§7§63❤ absorption!",
                "§71 second between eats"));
        return tag(withHeadTexture(item), Type.GOLDEN_HEAD);
    }

    /** 頭 (397:3) に Golden Head のスキンを付けたコピー (画面のアイコンにも使う)。 */
    public static ItemStack withHeadTexture(ItemStack item) {
        net.minecraft.server.v1_8_R3.ItemStack nms = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nms.hasTag() ? nms.getTag() : new NBTTagCompound();
        NBTTagCompound owner = new NBTTagCompound();
        owner.setString("Id", HEAD_ID);
        NBTTagCompound texture = new NBTTagCompound();
        texture.setString("Value", HEAD_TEXTURE);
        NBTTagList textures = new NBTTagList();
        textures.add(texture);
        NBTTagCompound properties = new NBTTagCompound();
        properties.set("textures", textures);
        owner.set("Properties", properties);
        tag.set("SkullOwner", owner);
        nms.setTag(tag);
        return CraftItemStack.asBukkitCopy(nms);
    }

    public static ItemStack tastySoup() {
        return tag(named(new ItemStack(Material.MUSHROOM_SOUP), "§aTasty Soup", Arrays.asList(
                "§7Perk item",
                "§9Speed I (0:07)",
                "§a1.5❤ Heal §7+ §61❤ Absorption",
                "§cNext melee hit +15% damage")), Type.TASTY_SOUP);
    }

    /** Soup でアシストしたときにもらうスープ (pit.wiki: Speed I 7 秒・再生 II 5 秒・1❤ 衝撃吸収・次の近接 +15%)。 */
    public static ItemStack assistSoup() {
        return tag(named(new ItemStack(Material.MUSHROOM_SOUP), "§bAssist Soup", Arrays.asList(
                "§7Perk item",
                "§9Speed I (0:07)",
                "§cRegeneration II (0:05) §7+ §61❤ Absorption",
                "§cNext melee hit +15% damage")), Type.ASSIST_SOUP);
    }

    /** Renown の Fancy Hat (革のヘルメット)。 */
    public static ItemStack fancyHat() {
        return tag(unbreakable(Material.LEATHER_HELMET), Type.FANCY_HAT);
    }

    /** 色を付けた Fancy Hat (color が null ならそのままの革の色)。 */
    public static ItemStack fancyHat(org.bukkit.Color color) {
        ItemStack item = unbreakable(Material.LEATHER_HELMET);
        if (color != null) {
            org.bukkit.inventory.meta.LeatherArmorMeta meta = (org.bukkit.inventory.meta.LeatherArmorMeta) item.getItemMeta();
            meta.setColor(color);
            item.setItemMeta(meta);
        }
        return tag(item, Type.FANCY_HAT);
    }

    public static ItemStack olympusPotion() {
        return tag(named(new ItemStack(Material.POTION, 1, (short) 8203), "§bOlympus Potion", Arrays.asList(
                "§7Perk item",
                "§9Speed I (0:24)",
                "§9Regeneration III (0:10)",
                "§9Resistance II (0:04)",
                "§bGain +27 XP!",
                "§7Can only hold 1")), Type.OLYMPUS_POTION);
    }

    public static ItemStack cobblestone(int amount) {
        return tag(new ItemStack(Material.COBBLESTONE, amount), Type.MINEMAN_COBBLE);
    }

    public static ItemStack minemanPickaxe() {
        ItemStack item = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.DIG_SPEED, 4, true);
        meta.spigot().setUnbreakable(true);
        item.setItemMeta(meta);
        return tag(item, Type.MINEMAN_PICKAXE);
    }

    public static ItemStack fishingRod() {
        return tag(unbreakable(Material.FISHING_ROD), Type.FISHING_ROD);
    }

    /** Lava Bucket (中身あり・空のバケツどちらも同じ印)。 */
    public static ItemStack lavaBucket(boolean full) {
        return tag(new ItemStack(full ? Material.LAVA_BUCKET : Material.BUCKET), Type.LAVA_BUCKET);
    }

    public static ItemStack safetyHelmet() {
        return tag(unbreakable(Material.CHAINMAIL_HELMET), Type.SAFETY_HELMET);
    }

    /**
     * Bounty Hunter の金のレギンス (録画 2026-10-15_13-52-12.mcrc の他のプレイヤーの持ち物: Unbreakable、§7Perk item)。
     * pit.wiki: これを履いていないと Bounty Hunter は何も効かない。
     */
    public static ItemStack goldenPants() {
        ItemStack item = unbreakable(Material.GOLD_LEGGINGS);
        ItemMeta meta = item.getItemMeta();
        meta.setLore(Arrays.asList("§7Perk item"));
        item.setItemMeta(meta);
        return tag(item, Type.GOLDEN_PANTS);
    }

    /** キルでもらう金リンゴ。 */
    public static ItemStack goldenApple() {
        return tag(new ItemStack(Material.GOLDEN_APPLE), Type.GOLDEN_APPLE);
    }

    private static ItemStack unbreakable(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.spigot().setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    /** パークのアイテムなら種類、違えば null。 */
    public static Type of(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !NbtUtil.has(item, KEY)) {
            return null;
        }
        int index = NbtUtil.getInt(item, KEY) - 1;
        return index >= 0 && index < Type.values().length ? Type.values()[index] : null;
    }

    private static ItemStack named(ItemStack item, String name, List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack tag(ItemStack item, Type type) {
        return NbtUtil.setInt(item, KEY, type.ordinal() + 1);
    }
}
