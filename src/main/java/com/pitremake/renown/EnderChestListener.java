package com.pitremake.renown;

import com.pitremake.PitPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Renown の Inventory Management: エンダーチェストが 1 Tier につき 1 行増える (3 行 → 最大 6 行)。
 * 最初の 3 行はバニラのエンダーチェストのまま、増えた行は plugins/Pit/enderchest.yml に保存する。
 */
public final class EnderChestListener implements Listener {

    private static final int BASE_SIZE = 27;

    private final PitPlugin plugin;
    private final File file;
    private final YamlConfiguration yaml;

    public EnderChestListener(PitPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "enderchest.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    private static final class Holder implements InventoryHolder {
        final UUID owner;
        Inventory inventory;

        Holder(UUID owner) {
            this.owner = owner;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    /** そのプレイヤーのエンダーチェストの大きさ (Inventory Management で 1 Tier ごとに +1 行)。 */
    public int size(Player player) {
        return size(player.getUniqueId());
    }

    /** オフラインのプレイヤーにも使える (/view)。 */
    public int size(UUID player) {
        int tier = plugin.getRenownEffects().tier(player, RenownUpgrade.INVENTORY_MANAGEMENT);
        return BASE_SIZE + 9 * Math.min(Math.max(tier, 0), 3);
    }

    /** 増えた行も含めたエンダーチェストの中身 (/view 用)。 */
    public ItemStack[] contents(Player player) {
        ItemStack[] all = new ItemStack[size(player)];
        ItemStack[] vanilla = player.getEnderChest().getContents();
        for (int i = 0; i < BASE_SIZE && i < vanilla.length; i++) {
            all[i] = vanilla[i];
        }
        String key = player.getUniqueId().toString();
        for (int i = BASE_SIZE; i < all.length; i++) {
            all[i] = yaml.getItemStack(key + "." + i);
        }
        return all;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onOpen(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null
                || event.getClickedBlock().getType() != Material.ENDER_CHEST || event.getPlayer().isSneaking()) {
            return;
        }
        Player player = event.getPlayer();
        int tier = plugin.getRenownEffects().tier(player.getUniqueId(), RenownUpgrade.INVENTORY_MANAGEMENT);
        if (tier <= 0) {
            return;
        }
        event.setCancelled(true);
        Holder holder = new Holder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, BASE_SIZE + 9 * Math.min(tier, 3), "Ender Chest");
        holder.inventory = inventory;
        ItemStack[] vanilla = player.getEnderChest().getContents();
        for (int i = 0; i < BASE_SIZE && i < vanilla.length; i++) {
            inventory.setItem(i, vanilla[i]);
        }
        String key = player.getUniqueId().toString();
        for (int i = BASE_SIZE; i < inventory.getSize(); i++) {
            inventory.setItem(i, yaml.getItemStack(key + "." + i));
        }
        player.openInventory(inventory);
        player.playSound(event.getClickedBlock().getLocation(), "random.chestopen", 0.5f, 1.0f);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder)) {
            return;
        }
        Holder holder = (Holder) event.getInventory().getHolder();
        Inventory inventory = event.getInventory();
        Player player = (Player) event.getPlayer();
        for (int i = 0; i < BASE_SIZE; i++) {
            player.getEnderChest().setItem(i, inventory.getItem(i));
        }
        String key = holder.owner.toString();
        for (int i = BASE_SIZE; i < inventory.getSize(); i++) {
            yaml.set(key + "." + i, inventory.getItem(i));
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save enderchest.yml", e);
        }
        player.playSound(player.getLocation(), "random.chestclosed", 0.5f, 1.0f);
    }
}
