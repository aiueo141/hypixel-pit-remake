package com.pitremake.bot;

import com.pitremake.spawn.PitSpawn;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** Bot の出現・削除と、毎 tick の更新。 */
public final class BotManager implements Listener {

    private final JavaPlugin plugin;
    private final PitSpawn spawnArea;
    private final List<PitBot> bots = new ArrayList<>();
    private BukkitTask task;
    private int nextId = 1;
    private SkinPool skins;

    public BotManager(JavaPlugin plugin, PitSpawn spawnArea) {
        this.plugin = plugin;
        this.spawnArea = spawnArea;
    }

    public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1, 1);
        skins = new SkinPool(plugin);
        skins.load();
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        removeAll();
    }

    public void spawn(Location location, int count) {
        spawn(() -> location, count);
    }

    /** 1 体ごとに location から場所を決めて出す (Pit の復活地点からランダムなど)。 */
    public void spawn(Supplier<Location> location, int count) {
        for (int i = 0; i < count; i++) {
            java.util.Set<String> used = new java.util.HashSet<>();
            for (PitBot existing : bots) {
                used.add(existing.getName());
            }
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                used.add(online.getName());
            }
            nextId++;
            PitBot bot = new PitBot(BotNames.random(used), location.get(), spawnArea, skins == null ? null : skins.random());
            bot.spawn();
            bots.add(bot);
        }
    }

    /** HP 0 になった Bot を Pit の復活地点に出し直す。 */
    public void respawn(Player player) {
        for (PitBot bot : bots) {
            if (bot.is(player)) {
                bot.respawnAtPitSpawn();
                return;
            }
        }
    }

    /** UUID から Bot を探す (居なければ null)。 */
    public Player find(UUID id) {
        for (PitBot bot : bots) {
            if (bot.hasId(id)) {
                return bot.entity();
            }
        }
        return null;
    }

    public int removeAll() {
        int count = bots.size();
        for (PitBot bot : bots) {
            bot.despawn();
        }
        bots.clear();
        return count;
    }

    public int count() {
        return bots.size();
    }

    /** 各 Bot の位置と体力 (確認用)。 */
    public List<String> describe() {
        List<String> lines = new ArrayList<>();
        for (PitBot bot : bots) {
            lines.add(bot.describe());
        }
        return lines;
    }

    public boolean isBot(Player player) {
        for (PitBot bot : bots) {
            if (bot.is(player)) {
                return true;
            }
        }
        return false;
    }

    /** Bot の名前と Pit レベル (tab の表示用)。 */
    public Map<String, Integer> levels() {
        Map<String, Integer> levels = new LinkedHashMap<>();
        for (PitBot bot : bots) {
            levels.put(bot.getName(), bot.getLevel());
        }
        return levels;
    }

    /** Bot の名前とプレステージ (tab の表示用)。 */
    public Map<String, Integer> prestiges() {
        Map<String, Integer> prestiges = new LinkedHashMap<>();
        for (PitBot bot : bots) {
            prestiges.put(bot.getName(), bot.getPrestige());
        }
        return prestiges;
    }

    /** Bot ならそのプレステージ、Bot でなければ -1。 */
    public int prestigeOf(Player player) {
        for (PitBot bot : bots) {
            if (bot.is(player)) {
                return bot.getPrestige();
            }
        }
        return -1;
    }

    /** Bot ならその Pit レベル、Bot でなければ 0。 */
    public int levelOf(Player player) {
        for (PitBot bot : bots) {
            if (bot.is(player)) {
                return bot.getLevel();
            }
        }
        return 0;
    }

    private void tick() {
        for (PitBot bot : new ArrayList<>(bots)) {
            List<Player> others = new ArrayList<>();
            for (PitBot other : bots) {
                Player entity = other.entity();
                if (other != bot && entity != null) {
                    others.add(entity);
                }
            }
            try {
                bot.tick(others);
            } catch (RuntimeException e) {
                plugin.getLogger().warning("Bot " + bot.getName() + " crashed and was removed: " + e);
                bot.despawn();
                bots.remove(bot);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        for (PitBot bot : bots) {
            bot.showTo(event.getPlayer());
        }
    }

    /**
     * プレイヤーが殴られるとサーバーはノックバックをクライアントに送り、サーバー側の速度を元に戻す。
     * Bot にはクライアントが無いので、キャンセルしてサーバー側のノックバックをそのまま残す。
     */
    @EventHandler
    public void onBotVelocity(PlayerVelocityEvent event) {
        if (isBot(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /** Bot は何も落とさない。 */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBotDeath(PlayerDeathEvent event) {
        if (isBot(event.getEntity())) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }
}
