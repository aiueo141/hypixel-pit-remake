package com.pitremake.listener;

import com.pitremake.PitPlugin;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerListener implements Listener {

    private final PitPlugin plugin;

    public PlayerListener(PitPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String message = plugin.getConfig().getString("messages.join", "");
        if (!message.isEmpty()) {
            event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&',
                    message.replace("{player}", event.getPlayer().getName())));
        }
    }
}
