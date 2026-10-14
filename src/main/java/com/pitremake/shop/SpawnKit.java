package com.pitremake.shop;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * スポーンしたときの初期装備 (録画 2026-10-14_07-53-32.mcrc の Hypixel の持ち物)。
 * <pre>
 * 防具 : 頭なし、胴・脚・足はチェーン 2 + 鉄 1 (死ぬたびに変わる)
 * 武器 : 鉄の剣・弓 (Items の村人の "Toggle Spawn Items" が ON のとき)
 * 矢   : 32 本
 * </pre>
 * どれも Unbreakable (表示する) で、名前・説明は無い (Kept on death ではないので死ぬと無くなり、スポーンでまたもらう)。
 * 持っているものはもらわない (足りない分だけ)。
 */
public final class SpawnKit {

    static final int ARROWS = 32;

    private SpawnKit() {
    }

    public static void give(Player player, boolean weapons) {
        give(player, weapons, 0);
    }

    /** extraArrows: Renown の Armory の分 (1 Tier +8 本)。 */
    public static void give(Player player, boolean weapons, int extraArrows) {
        PlayerInventory inventory = player.getInventory();
        List<Boolean> iron = new ArrayList<>(Arrays.asList(true, false, false));
        Collections.shuffle(iron, ThreadLocalRandom.current());
        if (inventory.getChestplate() == null) {
            inventory.setChestplate(unbreakable(iron.get(0) ? Material.IRON_CHESTPLATE : Material.CHAINMAIL_CHESTPLATE));
        }
        if (inventory.getLeggings() == null) {
            inventory.setLeggings(unbreakable(iron.get(1) ? Material.IRON_LEGGINGS : Material.CHAINMAIL_LEGGINGS));
        }
        if (inventory.getBoots() == null) {
            inventory.setBoots(unbreakable(iron.get(2) ? Material.IRON_BOOTS : Material.CHAINMAIL_BOOTS));
        }
        if (weapons) {
            giveIfMissing(inventory, Material.IRON_SWORD);
            giveIfMissing(inventory, Material.BOW);
        }
        int arrows = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == Material.ARROW) {
                arrows += item.getAmount();
            }
        }
        if (arrows < ARROWS + extraArrows) {
            inventory.addItem(new ItemStack(Material.ARROW, ARROWS + extraArrows - arrows));
        }
        player.updateInventory();
    }

    private static void giveIfMissing(PlayerInventory inventory, Material material) {
        if (!inventory.contains(material)) {
            inventory.addItem(unbreakable(material));
        }
    }

    static ItemStack unbreakable(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.spigot().setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }
}
