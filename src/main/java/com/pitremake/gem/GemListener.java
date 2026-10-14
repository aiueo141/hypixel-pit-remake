package com.pitremake.gem;

import com.pitremake.PitPlugin;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;

public final class GemListener implements Listener {

    private final PitPlugin plugin;

    public GemListener(PitPlugin plugin) {
        this.plugin = plugin;
    }

    /** Gem を持って右クリック → 使えるアイテムの一覧を開く。Mystic Well より先に処理する。 */
    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!GemItem.isGem(event.getItem())) {
            return;
        }
        event.setCancelled(true);
        if (!new GemSelectorMenu(plugin, event.getPlayer()).open()) {
            event.getPlayer().sendMessage(ChatColor.RED + "You don't have any items that can be upgraded with a gem!");
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof GemSelectorMenu) && !(holder instanceof GemUpgradeMenu)) {
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return;
        }
        if (holder instanceof GemSelectorMenu) {
            ((GemSelectorMenu) holder).click(slot);
        } else {
            ((GemUpgradeMenu) holder).click(slot);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof GemSelectorMenu || holder instanceof GemUpgradeMenu) {
            event.setCancelled(true);
        }
    }
}
