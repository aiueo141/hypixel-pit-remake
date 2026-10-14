package com.pitremake.rank;

import com.pitremake.PitPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Pit のチャットの形 (録画 2026-10-14_07-53-32.mcrc / 08-53-15.mcrc)。
 * <pre>
 * ランクあり: §7[§f§l111§7] §a[VIP] bomb_trooper§f§f: メッセージ   (白)
 * ランクなし: §9[§e§eII§9-§d§l104§9] §7GraysonGoatz§7§7: メッセージ (灰色)
 * </pre>
 */
public final class PitChatListener implements Listener {

    private final PitPlugin plugin;

    public PitChatListener(PitPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        RankManager ranks = plugin.getRankManager();
        String prefix = ranks.prefix(player.getUniqueId());
        boolean ranked = ranks.getRank(player.getUniqueId()) != Rank.NONE;
        String format = plugin.getStatsManager().chatTag(player.getUniqueId()) + " "
                + (prefix.isEmpty() ? "" : prefix + " ")
                + ranks.nameColor(player.getUniqueId()) + "%1$s"
                + (ranked ? "§f§f: " : "§7§7: ") + "%2$s";
        event.setFormat(format.replace("%", "%%").replace("%%1$s", "%1$s").replace("%%2$s", "%2$s"));
    }
}
