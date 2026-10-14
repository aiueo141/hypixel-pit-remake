package com.pitremake.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/** 画面のボタン用のアイテム。 */
public final class Menus {

    private Menus() {
    }

    public static ItemStack item(Material material, int data, int amount, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, Math.max(1, amount), (short) data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_POTION_EFFECTS);
        item.setItemMeta(meta);
        return item;
    }
}
