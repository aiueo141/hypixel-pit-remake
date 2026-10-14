package com.pitremake.gem;

import com.pitremake.util.NbtUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/** Totally Legit Gem のアイテム。ExtraAttributes.TotallyLegitGem = 1 で判別する。 */
public final class GemItem {

    private static final String KEY = "TotallyLegitGem";

    private GemItem() {
    }

    public static ItemStack create(int amount) {
        ItemStack item = new ItemStack(Material.EMERALD, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Totally Legit Gem");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Kept on death",
                ChatColor.GRAY + "Adds " + ChatColor.LIGHT_PURPLE + "1 tier" + ChatColor.GRAY + " to a mystic enchant.",
                ChatColor.DARK_GRAY + "Once per item!",
                "",
                ChatColor.YELLOW + "Hold and right-click to use!"));
        meta.addEnchant(Enchantment.LURE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return NbtUtil.setInt(item, KEY, 1);
    }

    public static boolean isGem(ItemStack item) {
        return item != null && item.getType() == Material.EMERALD && NbtUtil.has(item, KEY);
    }
}
