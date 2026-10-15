package com.pitremake.special;

import com.pitremake.gem.GemItem;
import com.pitremake.util.NbtUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * 特殊アイテム (Philosopher's Cactus / Funky Feather)。ExtraAttributes のキーで判別する。
 * <pre>
 * Philosopher's Cactus : 右クリックで通常 5 色の Fresh Pants を 1 つ選んでもらえる (pit.wiki/Philosophers_Cactus)
 * Funky Feather        : ホットバーにあると死亡時にインベントリを守る。代わりに 1 つ消費される (pit.wiki/Funky_Feather)
 * Mystic Repair Kit    : 右クリックでミスティックを 1 つ選んで Lives を最大にする (pit.wiki/Mystic_Repair_Kit)
 * </pre>
 */
public final class SpecialItems {

    private static final String CACTUS_KEY = "PhilosophersCactus";
    private static final String FEATHER_KEY = "FunkyFeather";
    private static final String VILE_KEY = "ChunkOfVile";
    private static final String REPAIR_KIT_KEY = "MysticRepairKit";

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
    /**
     * Mystic Repair Kit (ユーザーのスクリーンショット、アイコンは pit.wiki のハサミ)。
     * 右クリックで持っているミスティックを 1 つ選んで Lives を最大にする。死んでも無くならない。
     */
    public static ItemStack repairKit(int amount) {
        ItemStack item = create(Material.SHEARS, amount, REPAIR_KIT_KEY, ChatColor.LIGHT_PURPLE + "Mystic Repair Kit", Arrays.asList(
                ChatColor.YELLOW + "Special item",
                ChatColor.RED + "Untradeable",
                ChatColor.GRAY + "Right-click while holding this",
                ChatColor.GRAY + "item to repair a mystic item."));
        ItemMeta meta = item.getItemMeta();
        meta.spigot().setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isRepairKit(ItemStack item) {
        return item != null && item.getType() == Material.SHEARS && NbtUtil.has(item, REPAIR_KIT_KEY);
    }

    public static boolean isKeptOnDeath(ItemStack item) {
        return isFeather(item) || isCactus(item) || GemItem.isGem(item) || Uberdrop.is(item) || isVile(item) || isRepairKit(item);
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
