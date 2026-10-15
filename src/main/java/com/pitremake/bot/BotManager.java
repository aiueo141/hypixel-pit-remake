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
    /** KOTH 中の丘への道 (無ければ null)。半分の Bot (routed) がこれを使う。 */
    private BotRoute route;
    /** Spire の間は Bot を出さない。 */
    private boolean paused;
    private int autoTicks;

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
            if (route != null) {
                // KOTH 中に増えた Bot も半分くらいは丘へ
                long routed = bots.stream().filter(PitBot::isRouted).count();
                bot.setRouted(routed < bots.size() / 2);
            }
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

    /**
     * スポーンの外で一定時間 (config の bots.idle-oof-seconds、初期値 10 秒) 相手が見つからない Bot は /oof
     * (死んだ扱い: 少し前に殴った人がいればその人のキル。スポーンに戻る)。Spire の塔の中の Bot はしない。
     */
    private void idleOof(PitBot bot) {
        if (!(plugin instanceof com.pitremake.PitPlugin)) {
            return;
        }
        com.pitremake.PitPlugin pit = (com.pitremake.PitPlugin) plugin;
        int seconds = plugin.getConfig().getInt("bots.idle-oof-seconds", 10);
        Player entity = bot.entity();
        if (seconds <= 0 || entity == null || !bot.idleFor(seconds * 20) || bot.isFlying()
                || (pit.getSpireEvent() != null && pit.getSpireEvent().inSpire(entity))) {
            return;
        }
        // 死んだ扱いでスポーンに出し直される (BotManager.respawn → 数え直しもそこで)
        pit.getKillListener().oof(entity);
    }

    /** KOTH の始まり (route) と終わり (null)。出ている Bot の半分をランダムに丘へ向かわせる。 */
    public void setRoute(BotRoute route) {
        this.route = route;
        List<PitBot> shuffled = new ArrayList<>(bots);
        java.util.Collections.shuffle(shuffled);
        for (int i = 0; i < shuffled.size(); i++) {
            shuffled.get(i).setRouted(route != null && i < shuffled.size() / 2);
        }
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

    /** ランチャーパッドで飛んでいる途中の Bot か (その間は無敵)。 */
    public boolean isFlying(Player player) {
        for (PitBot bot : bots) {
            if (bot.is(player)) {
                return bot.isFlying();
            }
        }
        return false;
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
                Player entity = bot.entity();
                if (entity != null && plugin instanceof com.pitremake.PitPlugin
                        && ((com.pitremake.PitPlugin) plugin).getSpireEvent() != null) {
                    // Spire の通路に落ちた Bot は床のエンドポータルに触る前に塔へ
                    ((com.pitremake.PitPlugin) plugin).getSpireEvent().catchFallingBot(entity);
                }
                bot.tick(others, bot.isRouted() ? route : null);
                idleOof(bot);
            } catch (RuntimeException e) {
                plugin.getLogger().warning("Bot " + bot.getName() + " crashed and was removed: " + e);
                bot.despawn();
                bots.remove(bot);
            }
        }
        if (++autoTicks >= AUTO_INTERVAL_TICKS) {
            autoTicks = 0;
            autoPopulate();
        }
    }

    private static final int AUTO_INTERVAL_TICKS = 20;
    /** 1 秒に増やす数 (一度にたくさん出すと重いので)。 */
    private static final int AUTO_SPAWN_PER_SECOND = 2;

    /** 今いるべき Bot の数: config の bots.base (10) + 本物のプレイヤー 1 人ごとに bots.per-player (5)。Spire の間は 0。 */
    public int targetCount() {
        if (paused || !plugin.getConfig().getBoolean("bots.auto", true)) {
            return -1;
        }
        int players = 0;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!isBot(player)) {
                players++;
            }
        }
        return plugin.getConfig().getInt("bots.base", 10) + plugin.getConfig().getInt("bots.per-player", 5) * players;
    }

    /** 1 秒ごと: 足りなければスポーンの復活地点に出し、多すぎれば消す。 */
    private void autoPopulate() {
        if (paused) {
            if (!bots.isEmpty()) {
                removeAll();
            }
            return;
        }
        int target = targetCount();
        if (target < 0) {
            return;
        }
        if (bots.size() < target) {
            spawn(spawnArea::randomPoint, Math.min(AUTO_SPAWN_PER_SECOND, target - bots.size()));
        } else if (bots.size() > target) {
            removeSome(bots.size() - target);
        }
    }

    /** 後から出した Bot から count 体消す (飛んでいる途中の Bot は後回し)。 */
    public int removeSome(int count) {
        int removed = 0;
        for (int i = bots.size() - 1; i >= 0 && removed < count; i--) {
            PitBot bot = bots.get(i);
            if (bot.isFlying()) {
                continue;
            }
            bot.despawn();
            bots.remove(i);
            removed++;
        }
        return removed;
    }

    /** Spire の始まり (true: 全部消す) と終わり (false: また出す)。 */
    public void setPaused(boolean paused) {
        this.paused = paused;
        if (paused) {
            removeAll();
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
