package com.pitremake.util;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/** {@link ClickMenu} の画面のクリック・ドラッグ・閉じる。 */
public final class ClickMenuListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof ClickMenu)) {
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        ClickMenu menu = (ClickMenu) event.getView().getTopInventory().getHolder();
        if (slot >= 0 && slot < event.getView().getTopInventory().getSize()) {
            menu.click(slot, event.isRightClick());
        } else if (slot >= 0 && event.getClickedInventory() == event.getView().getBottomInventory()) {
            menu.clickInventory(event.getSlot(), event.isRightClick());
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof ClickMenu) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof ClickMenu) {
            ((ClickMenu) event.getInventory().getHolder()).close();
        }
    }
}
