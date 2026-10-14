package com.pitremake.well;

import com.pitremake.PitPlugin;
import com.pitremake.item.MysticItems;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public final class MysticWellListener implements Listener {

    private final PitPlugin plugin;

    public MysticWellListener(PitPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * エンチャントテーブルを右クリックすると Mystic Well を開く (権限は不要)。
     * スポーン保護の範囲では OP 以外のクリックがキャンセル済みで届くので、キャンセルされていても開く。
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getClickedBlock().getType() != Material.ENCHANTMENT_TABLE) {
            return;
        }
        event.setCancelled(true);
        new MysticWellMenu(plugin, event.getPlayer(), event.getClickedBlock().getLocation()).open();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        MysticWellMenu menu = getMenu(event.getView().getTopInventory().getHolder());
        if (menu == null) {
            return;
        }
        event.setCancelled(true);
        if (event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT
                && event.getClick() != ClickType.SHIFT_LEFT && event.getClick() != ClickType.SHIFT_RIGHT) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot < 0) {
            return;
        }
        if (slot < event.getView().getTopInventory().getSize()) {
            if (slot == MysticWellMenu.CENTER) {
                menu.returnItem();
            } else if (slot == MysticWellMenu.SACRIFICE) {
                menu.returnSacrifice();
            } else if (menu.isTableSlot(slot)) {
                menu.tryUpgrade();
            }
            return;
        }

        // 自分のインベントリのミスティックアイテムをクリック → 生贄スロットか中央に置く
        ItemStack clicked = event.getCurrentItem();
        if (menu.canPlaceSacrifice(clicked)) {
            event.setCurrentItem(null);
            menu.placeSacrifice(clicked);
        } else if (MysticItems.isMystic(clicked) && menu.canPlace()) {
            event.setCurrentItem(null);
            menu.place(clicked);
        } else {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        plugin.getServer().getScheduler().runTask(plugin, player::updateInventory);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (getMenu(event.getView().getTopInventory().getHolder()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        MysticWellMenu menu = getMenu(event.getInventory().getHolder());
        if (menu != null) {
            menu.close();
        }
    }

    private static MysticWellMenu getMenu(InventoryHolder holder) {
        return holder instanceof MysticWellMenu ? (MysticWellMenu) holder : null;
    }
}
