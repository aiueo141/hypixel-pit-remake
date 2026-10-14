package com.pitremake.spawn;

import com.pitremake.PitPlugin;
import com.pitremake.view.ProfileViewer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * スポーンの中のルール (ユーザーの指定)。
 * <pre>
 * 右クリック : スポーンの中でプレイヤーを右クリックすると、その人の /view を開く
 * ストリーク : スポーンに 5 秒以上いるとストリークが 0 になる
 * </pre>
 * /spawn と /oof はスポーンの外でしか使えない (それぞれのコマンド)。
 */
public final class SpawnRules implements Listener {

    private static final int STREAK_RESET_SECONDS = 5;

    private final PitPlugin plugin;
    /** スポーンにいる秒数。 */
    private final Map<UUID, Integer> secondsInSpawn = new HashMap<>();

    public SpawnRules(PitPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::everySecond, 20, 20);
    }

    private void everySecond() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            if (player.isDead() || !plugin.getPitSpawn().isProtected(player.getLocation())) {
                secondsInSpawn.remove(id);
                continue;
            }
            int seconds = secondsInSpawn.merge(id, 1, Integer::sum);
            if (seconds >= STREAK_RESET_SECONDS && plugin.getStatsManager().getStreak(id) > 0) {
                plugin.getStatsManager().resetStreak(id);
                plugin.getKillstreakManager().reset(player);
                plugin.getMegastreakManager().refresh(player);
                plugin.getSidebar().update(player);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onRightClick(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player)) {
            return;
        }
        Player viewer = event.getPlayer();
        Player target = (Player) event.getRightClicked();
        if (plugin.getBotManager().isBot(target) || !plugin.getPitSpawn().isProtected(viewer.getLocation())) {
            return;
        }
        event.setCancelled(true);
        new ProfileViewer(plugin, viewer, target).open();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        secondsInSpawn.remove(event.getPlayer().getUniqueId());
    }
}
