package com.pitremake.special;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SpecialItemListener implements Listener {

    private static final int HOTBAR_SIZE = 9;

    private final JavaPlugin plugin;
    /** 死亡時に残した特殊アイテム。リスポーン時に返す。 */
    private final Map<UUID, List<ItemStack>> keptItems = new HashMap<>();

    public SpecialItemListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Philosopher's Cactus を持って右クリック → 色選択画面。Mystic Well より先に処理する。 */
    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!SpecialItems.isCactus(event.getItem())) {
            return;
        }
        // サボテンを設置しないようにキャンセルする
        event.setCancelled(true);
        new CactusMenu(event.getPlayer()).open();
    }

    /** Mystic Repair Kit を持って右クリック → 直すミスティックを選ぶ画面。 */
    @EventHandler(priority = EventPriority.LOW)
    public void onRepairKit(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!SpecialItems.isRepairKit(event.getItem())) {
            return;
        }
        event.setCancelled(true);
        if (!new RepairKitMenu((com.pitremake.PitPlugin) plugin, event.getPlayer()).open()) {
            event.getPlayer().sendMessage("§cYou don't have any mystic items to repair!");
        }
    }

    /** Funky Feather と Mystic Repair Kit は捨てられない (Q キーでも、画面の外へ出しても)。 */
    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE
                && (SpecialItems.isFeather(item) || SpecialItems.isRepairKit(item))) {
            event.setCancelled(true);
        }
    }

    /** Uberdrop を持ってクリック (右でも左でも) → 開ける。 */
    @EventHandler(priority = EventPriority.LOW)
    public void onOpenUberdrop(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL || !Uberdrop.is(event.getItem())) {
            return;
        }
        event.setCancelled(true);
        Uberdrop.open((com.pitremake.PitPlugin) plugin, event.getPlayer());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof CactusMenu)) {
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot >= 0 && slot < event.getView().getTopInventory().getSize()) {
            ((CactusMenu) event.getView().getTopInventory().getHolder()).click(slot);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof CactusMenu) {
            event.setCancelled(true);
        }
    }

    /**
     * ホットバーに Funky Feather があれば 1 つ消費してインベントリを守る。
     * 無ければ通常どおりアイテムを落とすが、特殊アイテム (Feather / Cactus / Gem) は残してリスポーン時に返す。
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        if (event.getKeepInventory()) {
            // KillListener が持ち物をそのまま残して処理した (羽の消費もそちら)
            return;
        }
        Player player = event.getEntity();
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (SpecialItems.isFeather(item)) {
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                    inventory.setItem(slot, item);
                } else {
                    inventory.setItem(slot, null);
                }
                event.setKeepInventory(true);
                event.getDrops().clear();
                return;
            }
        }

        List<ItemStack> kept = new ArrayList<>();
        for (Iterator<ItemStack> it = event.getDrops().iterator(); it.hasNext(); ) {
            ItemStack drop = it.next();
            if (SpecialItems.isKeptOnDeath(drop)) {
                kept.add(drop);
                it.remove();
            }
        }
        if (!kept.isEmpty()) {
            keptItems.put(player.getUniqueId(), kept);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        List<ItemStack> kept = keptItems.remove(event.getPlayer().getUniqueId());
        if (kept == null) {
            return;
        }
        // リスポーン処理が終わってから渡す
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (ItemStack item : kept) {
                for (ItemStack leftover : player.getInventory().addItem(item).values()) {
                    player.getWorld().dropItem(player.getLocation(), leftover);
                }
            }
        });
    }
}
