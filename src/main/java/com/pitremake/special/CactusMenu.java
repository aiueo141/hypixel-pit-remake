package com.pitremake.special;

import com.pitremake.item.MysticItems;
import com.pitremake.item.PantsColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.Arrays;
import java.util.List;

/** Philosopher's Cactus を右クリックしたときの色選択画面。通常 5 色を 11〜15 に並べる (特殊なパンツは選べない)。 */
public final class CactusMenu implements InventoryHolder {

    private static final int FIRST_SLOT = 11;

    private final Player player;
    private final Inventory inventory;
    private final List<PantsColor> colors = PantsColor.standardColors();

    public CactusMenu(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 27, "Philosopher's Cactus");
        for (int i = 0; i < colors.size(); i++) {
            inventory.setItem(FIRST_SLOT + i, icon(colors.get(i)));
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        player.openInventory(inventory);
    }

    void click(int slot) {
        int index = slot - FIRST_SLOT;
        if (index < 0 || index >= colors.size()) {
            return;
        }
        player.closeInventory();
        if (!consumeCactus()) {
            player.sendMessage(ChatColor.RED + "You don't have a Philosopher's Cactus!");
            return;
        }
        for (ItemStack leftover : player.getInventory().addItem(MysticItems.freshPants(colors.get(index))).values()) {
            player.getWorld().dropItem(player.getLocation(), leftover);
        }
        player.sendMessage(ChatColor.GREEN.toString() + ChatColor.BOLD + "THE CACTUS HAS SPOKEN!");
    }

    /** 手に持っている (無ければインベントリ内の) Philosopher's Cactus を 1 つ消費する。 */
    private boolean consumeCactus() {
        ItemStack[] contents = player.getInventory().getContents();
        int slot = SpecialItems.isCactus(player.getItemInHand()) ? player.getInventory().getHeldItemSlot() : -1;
        for (int i = 0; slot < 0 && i < contents.length; i++) {
            if (SpecialItems.isCactus(contents[i])) {
                slot = i;
            }
        }
        if (slot < 0) {
            return false;
        }
        ItemStack cactus = contents[slot];
        if (cactus.getAmount() > 1) {
            cactus.setAmount(cactus.getAmount() - 1);
            player.getInventory().setItem(slot, cactus);
        } else {
            player.getInventory().setItem(slot, null);
        }
        return true;
    }

    private static ItemStack icon(PantsColor color) {
        ItemStack icon = new ItemStack(Material.LEATHER_LEGGINGS);
        LeatherArmorMeta meta = (LeatherArmorMeta) icon.getItemMeta();
        meta.setDisplayName(color.getChatColor() + "Fresh " + color.getDisplayName() + " Pants");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Consume the " + ChatColor.GREEN + "Philosopher's Cactus" + ChatColor.GRAY + " to",
                ChatColor.GRAY + "obtain fresh pants of this color.",
                "",
                ChatColor.YELLOW + "Click to pet the cactus!"));
        meta.setColor(color.getColor());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        icon.setItemMeta(meta);
        return icon;
    }
}
