package com.pitremake.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/** ミスティックアイテムの種類。 */
public enum MysticType {
    SWORD(Material.GOLD_SWORD),
    BOW(Material.BOW),
    PANTS(Material.LEATHER_LEGGINGS);

    private final Material material;

    MysticType(Material material) {
        this.material = material;
    }

    public Material getMaterial() {
        return material;
    }

    /** アイテムの素材から種類を判定する。該当しなければ null。 */
    public static MysticType of(ItemStack item) {
        if (item == null) {
            return null;
        }
        for (MysticType type : values()) {
            if (type.material == item.getType()) {
                return type;
            }
        }
        return null;
    }
}
