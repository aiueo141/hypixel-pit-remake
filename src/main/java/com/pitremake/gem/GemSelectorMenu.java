package com.pitremake.gem;

import com.pitremake.PitPlugin;
import com.pitremake.item.MysticData;
import com.pitremake.item.MysticItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Gem を右クリックしたときに開く、Gem を使えるアイテムの一覧 (Totally Legit Selector)。 */
public final class GemSelectorMenu implements InventoryHolder {

    /** プレイヤーのインベントリ上の位置 (0〜35) と、履いているパンツ ({@link #LEGGINGS})。 */
    static final int LEGGINGS = -1;

    private final PitPlugin plugin;
    private final Player player;
    private final Inventory inventory;
    /** メニューのスロット → プレイヤーのアイテムの位置。 */
    private final Map<Integer, Integer> targets = new HashMap<>();

    public GemSelectorMenu(PitPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 45, "Totally Legit Selector");
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /** Gem を使えるアイテムが 1 つも無ければ開かずに false を返す。 */
    public boolean open() {
        int menuSlot = 0;
        for (int position : positions()) {
            ItemStack item = getItem(player, position);
            if (!MysticItems.isMystic(item) || !GemRules.isEligible(MysticData.read(item), plugin.getEnchantRegistry())) {
                continue;
            }
            if (menuSlot >= inventory.getSize()) {
                break;
            }
            inventory.setItem(menuSlot, icon(item));
            targets.put(menuSlot, position);
            menuSlot++;
        }
        if (targets.isEmpty()) {
            return false;
        }
        player.openInventory(inventory);
        return true;
    }

    void click(int slot) {
        Integer position = targets.get(slot);
        if (position == null) {
            return;
        }
        ItemStack item = getItem(player, position);
        if (!MysticItems.isMystic(item) || !GemRules.isEligible(MysticData.read(item), plugin.getEnchantRegistry())) {
            player.closeInventory();
            return;
        }
        new GemUpgradeMenu(plugin, player, position, item).open();
    }

    /** アイテムの見た目に "Click to upgrade!" を足したもの。 */
    private static ItemStack icon(ItemStack item) {
        ItemStack icon = item.clone();
        ItemMeta meta = icon.getItemMeta();
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<String>();
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to upgrade!");
        meta.setLore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private static List<Integer> positions() {
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            positions.add(i);
        }
        positions.add(LEGGINGS);
        return positions;
    }

    static ItemStack getItem(Player player, int position) {
        return position == LEGGINGS ? player.getInventory().getLeggings() : player.getInventory().getItem(position);
    }

    static void setItem(Player player, int position, ItemStack item) {
        if (position == LEGGINGS) {
            player.getInventory().setLeggings(item);
        } else {
            player.getInventory().setItem(position, item);
        }
    }
}
