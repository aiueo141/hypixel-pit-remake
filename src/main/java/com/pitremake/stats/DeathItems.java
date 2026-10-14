package com.pitremake.stats;

import com.pitremake.PitPlugin;
import com.pitremake.enchant.EnchantRegistry;
import com.pitremake.item.MysticData;
import com.pitremake.item.MysticItems;
import com.pitremake.item.RewardItems;
import com.pitremake.special.SpecialItems;
import com.pitremake.util.NbtUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.List;

/**
 * 死んだとき (HP 0 でスポーンへ戻るとき) の持ち物の処理。
 * <pre>
 * ホットバーに Funky Feather : 1 つ消費して、持ち物はそのまま (Lives も減らない)
 * Kept on death のアイテム    : そのまま (Feather / Cactus / Gem、Fresh のミスティック、Lore に "Kept on death")
 * Lives のあるアイテム        : Lives -1。0 になったら無くなる
 * それ以外                    : 無くなる
 * </pre>
 */
public final class DeathItems {

    private static final int HOTBAR_SIZE = 9;
    private static final String KEPT_ON_DEATH = "kept on death";

    private DeathItems() {
    }

    /** keepDiamondHelmet: Beastmode の「死んでもダイヤのヘルメットが残る」。 */
    public static void process(Player player, EnchantRegistry registry, boolean keepDiamondHelmet) {
        process(player, registry, keepDiamondHelmet, false);
    }

    /** keepLives: Divine Miracle で Lives のあるアイテムは Lives が減らない (Lives の無いものはいつも通り無くなる)。 */
    public static void process(Player player, EnchantRegistry registry, boolean keepDiamondHelmet, boolean keepLives) {
        PlayerInventory inventory = player.getInventory();
        if (consumeFeather(inventory)) {
            featherEffects(player);
            return;
        }
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            contents[slot] = keepLives && hasLives(contents[slot]) ? contents[slot] : afterDeath(contents[slot], registry);
        }
        inventory.setContents(contents);
        ItemStack[] armor = inventory.getArmorContents();
        for (int slot = 0; slot < armor.length; slot++) {
            if (keepDiamondHelmet && armor[slot] != null && armor[slot].getType() == Material.DIAMOND_HELMET) {
                continue;
            }
            armor[slot] = keepLives && hasLives(armor[slot]) ? armor[slot] : afterDeath(armor[slot], registry);
        }
        inventory.setArmorContents(armor);
        player.setItemOnCursor(null);
        player.updateInventory();
    }

    /**
     * 録画 (victim.mcrc): §3§lFUNKY FEATHER! §7inventory protected! と mob.bat.takeoff (0.75 / 2.0)。スポーンへ移した後に鳴らす
     * (game.neutral.hurt と mob.zombie.infect は羽が無くても鳴る死亡の音なので KillListener)。
     */
    private static void featherEffects(Player player) {
        player.sendMessage("§3§lFUNKY FEATHER! §7inventory protected!");
        PitPlugin plugin = PitPlugin.getInstance();
        if (plugin == null) {
            return;
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.playSound(player.getLocation(), "mob.bat.takeoff", 0.75f, 126 / 63f);
            }
        }, 1);
    }

    private static boolean consumeFeather(PlayerInventory inventory) {
        for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (SpecialItems.isFeather(item)) {
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                    inventory.setItem(slot, item);
                } else {
                    inventory.setItem(slot, null);
                }
                return true;
            }
        }
        return false;
    }

    private static boolean hasLives(ItemStack item) {
        return item != null && item.getType() != Material.AIR && NbtUtil.has(item, "Lives");
    }

    /** 死んだ後のアイテム (無くなるなら null)。 */
    static ItemStack afterDeath(ItemStack item, EnchantRegistry registry) {
        if (item == null || item.getType() == Material.AIR || isKeptOnDeath(item)) {
            return item;
        }
        if (!NbtUtil.has(item, "Lives")) {
            return null;
        }
        int lives = NbtUtil.getInt(item, "Lives") - 1;
        if (lives <= 0) {
            return null;
        }
        if (MysticItems.isMystic(item)) {
            MysticData data = MysticData.read(item);
            data.setLives(lives);
            return MysticItems.applyDisplay(data.write(item), registry);
        }
        return RewardItems.withLives(item, lives);
    }

    static boolean isKeptOnDeath(ItemStack item) {
        if (SpecialItems.isKeptOnDeath(item)) {
            return true;
        }
        if (MysticItems.isMystic(item) && MysticData.read(item).isFresh()) {
            return true;
        }
        if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
            List<String> lore = item.getItemMeta().getLore();
            for (String line : lore) {
                if (ChatColor.stripColor(line).toLowerCase().contains(KEPT_ON_DEATH)) {
                    return true;
                }
            }
        }
        return false;
    }
}
