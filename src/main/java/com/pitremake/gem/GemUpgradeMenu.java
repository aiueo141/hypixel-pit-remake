package com.pitremake.gem;

import com.pitremake.PitPlugin;
import com.pitremake.enchant.EnchantRarity;
import com.pitremake.enchant.PitEnchant;
import com.pitremake.item.MysticData;
import com.pitremake.item.MysticItems;
import com.pitremake.item.MysticType;
import com.pitremake.util.RomanNumerals;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 選んだアイテムのどのエンチャントに Gem を使うか選ぶ画面 (Totally Legit Gem)。
 * エンチャントごとにアイテムと同じ種類のアイコン (Upgrade Slot 1〜3) を 11 / 13 / 15 に置き、31 に戻るボタン。
 */
public final class GemUpgradeMenu implements InventoryHolder {

    private static final int[] UPGRADE_SLOTS = {11, 13, 15, 20, 22, 24};
    static final int BACK = 31;
    /** Gem を使ったときの効果音。 */
    private static final String UPGRADE_SOUND = "dig.glass";

    private final PitPlugin plugin;
    private final Player player;
    private final int position;
    /** 開いた時点のアイテム。強化するときに同じアイテムのままか確認する。 */
    private final ItemStack snapshot;
    private final List<String> enchantKeys = new ArrayList<>();
    private final Inventory inventory;

    GemUpgradeMenu(PitPlugin plugin, Player player, int position, ItemStack item) {
        this.plugin = plugin;
        this.player = player;
        this.position = position;
        this.snapshot = item.clone();
        this.inventory = Bukkit.createInventory(this, 36, "Totally Legit Gem");
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    void open() {
        MysticData data = MysticData.read(snapshot);
        for (Map.Entry<String, Integer> entry : data.getEnchants().entrySet()) {
            if (enchantKeys.size() >= UPGRADE_SLOTS.length) {
                break;
            }
            PitEnchant enchant = plugin.getEnchantRegistry().get(entry.getKey());
            if (enchant == null) {
                continue;
            }
            inventory.setItem(UPGRADE_SLOTS[enchantKeys.size()], upgradeIcon(enchantKeys.size() + 1, enchant, entry.getValue()));
            enchantKeys.add(entry.getKey());
        }
        inventory.setItem(BACK, backButton());
        player.openInventory(inventory);
    }

    void click(int slot) {
        if (slot == BACK) {
            if (!new GemSelectorMenu(plugin, player).open()) {
                player.closeInventory();
            }
            return;
        }
        int index = indexOf(slot);
        if (index < 0 || index >= enchantKeys.size()) {
            return;
        }
        upgrade(enchantKeys.get(index));
    }

    private void upgrade(String key) {
        ItemStack current = GemSelectorMenu.getItem(player, position);
        if (current == null || !current.isSimilar(snapshot)) {
            player.sendMessage(ChatColor.RED + "That item has changed!");
            player.closeInventory();
            return;
        }
        MysticData data = MysticData.read(current);
        PitEnchant enchant = plugin.getEnchantRegistry().get(key);
        if (!GemRules.isEligible(data, plugin.getEnchantRegistry()) || !GemRules.canUpgrade(enchant, data.getEnchantLevel(key))) {
            return;
        }
        if (!consumeGem()) {
            player.sendMessage(ChatColor.RED + "You don't have a Totally Legit Gem!");
            player.closeInventory();
            return;
        }

        // Gem で上げたエンチャントは Lore の一番下に移す (付け直すと末尾になる)
        int level = data.getEnchantLevel(key) + 1;
        data.removeEnchant(key);
        data.setEnchant(key, level);
        data.setGemmed(true);
        ItemStack upgraded = MysticItems.applyDisplay(data.write(current), plugin.getEnchantRegistry());
        GemSelectorMenu.setItem(player, position, upgraded);
        player.closeInventory();
        // 録画 (gem.mcrc) ではチャットは出ず、ガラスの割れる音 (音程 2.0) だけ。称号が変わってもアナウンスしない
        player.playSound(player.getLocation(), UPGRADE_SOUND, 1.0f, 2.0f);
    }

    /** 手に持っている Gem (無ければインベントリ内の Gem) を 1 つ消費する。 */
    private boolean consumeGem() {
        ItemStack[] contents = player.getInventory().getContents();
        int slot = GemItem.isGem(player.getItemInHand()) ? player.getInventory().getHeldItemSlot() : -1;
        for (int i = 0; slot < 0 && i < contents.length; i++) {
            if (GemItem.isGem(contents[i])) {
                slot = i;
            }
        }
        if (slot < 0) {
            return false;
        }
        ItemStack gem = contents[slot];
        if (gem.getAmount() > 1) {
            gem.setAmount(gem.getAmount() - 1);
            player.getInventory().setItem(slot, gem);
        } else {
            player.getInventory().setItem(slot, null);
        }
        return true;
    }

    private ItemStack upgradeIcon(int number, PitEnchant enchant, int level) {
        String state;
        if (enchant.getRarity() == EnchantRarity.RARE) {
            state = ChatColor.RED + "RARE! enchants can't be upgraded!";
        } else if (level >= enchant.getMaxLevel()) {
            state = ChatColor.RED + "Maxed out!";
        } else {
            state = ChatColor.YELLOW + "Click to upgrade!";
        }
        // アイテムの種類と同じ見た目 (剣 = 金の剣、弓 = 弓、パンツ = 色の無い革のズボン)
        MysticType type = MysticType.of(snapshot);
        ItemStack icon = new ItemStack(type == null ? Material.GOLD_SWORD : type.getMaterial());
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Upgrade Slot " + number);
        meta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "Upgrade: " + ChatColor.BLUE + enchant.getName(),
                ChatColor.GRAY + "Tier: " + ChatColor.YELLOW + RomanNumerals.of(level),
                "",
                ChatColor.GRAY + "Cost: " + ChatColor.GREEN + "1 Totally Legit Gem",
                "",
                state));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        icon.setItemMeta(meta);
        return icon;
    }

    private static ItemStack backButton() {
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Go Back");
        meta.setLore(Collections.singletonList(ChatColor.GRAY + "To Totally Legit Selector"));
        arrow.setItemMeta(meta);
        return arrow;
    }

    private static int indexOf(int slot) {
        for (int i = 0; i < UPGRADE_SLOTS.length; i++) {
            if (UPGRADE_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
    }
}
