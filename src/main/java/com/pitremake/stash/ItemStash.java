package com.pitremake.stash;

import com.pitremake.PitPlugin;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Item Stash (録画 2026-10-15_15-57-15.mcrc)。受け取れなかったアイテムを預かっておく場所 (plugins/Pit/item-stash/&lt;UUID&gt;.yml)。
 * <pre>
 * 入ったとき : §e§lITEM STASH! §7You have §e1 §7items stashed away! §7[§e§lCLICK§7]
 *              (クリックで /pickupstash、ホバー Click to collect your items!)。音は無し
 * /pickupstash: 持ち物に入れて §e§lCOLLECTED! §7You picked up §aall §7items from your item stash!
 * </pre>
 * 録画では /offer の出していた申し込みが入っていた (OFFERS STASHED!)。ここでは Spire の途中で抜けた・サーバーが落ちたときの持ち物を入れる。
 * 持ち物がいっぱいで全部入らないときのチャットは推測。
 */
public final class ItemStash implements Listener {

    private final PitPlugin plugin;
    private final File folder;

    public ItemStash(PitPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "item-stash");
    }

    public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getCommand("pickupstash").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player) {
                pickup((Player) sender);
            }
            return true;
        });
    }

    private File file(UUID player) {
        return new File(folder, player + ".yml");
    }

    public List<ItemStack> items(UUID player) {
        List<ItemStack> items = new ArrayList<>();
        File file = file(player);
        if (!file.exists()) {
            return items;
        }
        for (Object raw : YamlConfiguration.loadConfiguration(file).getList("items", new ArrayList<>())) {
            if (raw instanceof ItemStack && ((ItemStack) raw).getType() != Material.AIR) {
                items.add((ItemStack) raw);
            }
        }
        return items;
    }

    private void save(UUID player, List<ItemStack> items) {
        File file = file(player);
        if (items.isEmpty()) {
            if (file.exists() && !file.delete()) {
                plugin.getLogger().warning("Could not delete " + file);
            }
            return;
        }
        if (!folder.isDirectory() && !folder.mkdirs()) {
            plugin.getLogger().warning("Could not create " + folder);
        }
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("items", items);
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save the item stash of " + player, e);
        }
    }

    /** アイテムを預ける (オフラインでも)。 */
    public void add(UUID player, List<ItemStack> added) {
        List<ItemStack> items = items(player);
        for (ItemStack item : added) {
            if (item != null && item.getType() != Material.AIR) {
                items.add(item.clone());
            }
        }
        save(player, items);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // 録画: 入ってから少し後 (ほかのお知らせの後) に出る
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                notify(player);
            }
        }, 15);
    }

    /** 預かっているものがあれば ITEM STASH! を出す。 */
    public void notify(Player player) {
        int count = items(player.getUniqueId()).size();
        if (count == 0) {
            return;
        }
        TextComponent message = new TextComponent(TextComponent.fromLegacyText(
                "§e§lITEM STASH! §7You have §e" + count + " §7items stashed away! §7[§e§lCLICK§7]"));
        message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pickupstash"));
        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("Click to collect your items!").color(ChatColor.YELLOW).create()));
        player.spigot().sendMessage(message);
    }

    /** /pickupstash: 入るだけ持ち物に入れる。 */
    public void pickup(Player player) {
        List<ItemStack> items = items(player.getUniqueId());
        if (items.isEmpty()) {
            player.sendMessage("§c§lEMPTY! §7Your item stash is empty!");
            return;
        }
        List<ItemStack> left = new ArrayList<>();
        int picked = 0;
        for (ItemStack item : items) {
            HashMap<Integer, ItemStack> rest = player.getInventory().addItem(item.clone());
            if (rest.isEmpty()) {
                picked++;
            } else {
                left.addAll(rest.values());
            }
        }
        save(player.getUniqueId(), left);
        player.updateInventory();
        if (left.isEmpty()) {
            player.sendMessage("§e§lCOLLECTED! §7You picked up §aall §7items from your item stash!");
        } else if (picked > 0) {
            player.sendMessage("§e§lCOLLECTED! §7You picked up §e" + picked + " §7items from your item stash! §7(§e" + left.size()
                    + " §7left, inventory full)");
        } else {
            player.sendMessage("§c§lINVENTORY FULL! §7Make some room to collect your item stash!");
        }
    }
}
