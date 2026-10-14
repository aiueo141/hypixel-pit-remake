package com.pitremake.special;

import com.pitremake.gem.GemItem;
import com.pitremake.util.NbtUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * 特殊アイテム (Philosopher's Cactus / Funky Feather)。ExtraAttributes のキーで判別する。
 * <pre>
 * Philosopher's Cactus : 右クリックで通常 5 色の Fresh Pants を 1 つ選んでもらえる (pit.wiki/Philosophers_Cactus)
 * Funky Feather        : ホットバーにあると死亡時にインベントリを守る。代わりに 1 つ消費される (pit.wiki/Funky_Feather)
 * </pre>
 */
public final class SpecialItems {

    private static final String CACTUS_KEY = "PhilosophersCactus";
    private static final String FEATHER_KEY = "FunkyFeather";
    private static final String VILE_KEY = "ChunkOfVile";

    private SpecialItems() {
    }

    public static ItemStack cactus(int amount) {
        return create(Material.CACTUS, amount, CACTUS_KEY, ChatColor.GREEN + "Philosopher's Cactus", Arrays.asList(
                ChatColor.YELLOW + "Special item",
                ChatColor.GRAY + "Right-click while holding this item to",
                ChatColor.GRAY + "summon Fresh Pants of your choice.",
                ChatColor.DARK_GRAY + "(Special pants excluded)"));
    }

    public static ItemStack feather(int amount) {
        return create(Material.FEATHER, amount, FEATHER_KEY, ChatColor.DARK_AQUA + "Funky Feather", Arrays.asList(
                ChatColor.YELLOW + "Special item",
                ChatColor.GRAY + "Protects your inventory but gets",
                ChatColor.GRAY + "consumed on death if in your",
                ChatColor.GRAY + "hotbar."));
    }

    /** Chunk of Vile (ユーザーのスクリーンショット: §5Chunk of Vile / Kept on death / §cHeretic artifact)。 */
    public static ItemStack vile(int amount) {
        return create(Material.COAL, amount, VILE_KEY, ChatColor.DARK_PURPLE + "Chunk of Vile", Arrays.asList(
                ChatColor.GRAY + "Kept on death",
                "",
                ChatColor.RED + "Heretic artifact"));
    }

    public static boolean isVile(ItemStack item) {
        return item != null && item.getType() == Material.COAL && NbtUtil.has(item, VILE_KEY);
    }

    public static boolean isCactus(ItemStack item) {
        return item != null && item.getType() == Material.CACTUS && NbtUtil.has(item, CACTUS_KEY);
    }

    public static boolean isFeather(ItemStack item) {
        return item != null && item.getType() == Material.FEATHER && NbtUtil.has(item, FEATHER_KEY);
    }

    /** 死亡してもインベントリに残る特殊アイテムか (Funky Feather / Philosopher's Cactus / Totally Legit Gem)。 */
    public static boolean isKeptOnDeath(ItemStack item) {
        return isFeather(item) || isCactus(item) || GemItem.isGem(item) || Uberdrop.is(item) || isVile(item);
    }

    private static ItemStack create(Material material, int amount, String key, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return NbtUtil.setInt(item, key, 1);
    }
}
