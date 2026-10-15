package com.pitremake.koth;

import com.pitremake.PitPlugin;
import com.pitremake.spire.SpireStructure;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * KOTH (Minor Event)。録画 2026-10-15_14-18-51.mcrc と pit.wiki/King_of_the_Hill。
 * <pre>
 * 開始   : §d§lMINOR EVENT! §b§lKOTH §7in §e§lSummer Area §7for §e4 §7mins と和音
 *          (note.harp 0.8 + 1 tick 後に note.bassattack 0.72、ピッチ 36/49 → 49/61 → 61/73 を 3 tick ごと)
 *          丘が出る: 石英 (y85) の上にダイヤ (y86) の円、真ん中はビーコン (下にダイヤ 3x3、上に水色のガラス)。
 *          ビーコンの光が通るよう、上空 (y195〜201) の飾りのブロックも消す
 * 1 分ごと: ... §7for §e3 §7mins / §e2 §7mins / §e1 §7min (音は無し)
 * 4 分後 : ... §7in §e§lSummer Area §7ended、丘を消して地形を戻す
 * </pre>
 * 丘のダイヤの上に立っている人のキルは XP・ゴールド ×4。アシストは自分と相手の両方が丘の上のときだけ ×4。
 * バウンティのゴールドは ×4 にならない (pit.wiki)。サイドバーの行は録画に無い。
 * 録画に出てきた丘は Summer Area だけなので、ほかの場所は resources/koth に足せば選ばれる。
 */
public final class KothEvent {

    public static final int DURATION_MINUTES = 4;
    public static final double MULTIPLIER = 4;
    private static final int[][] CHORD = {{36, 49}, {49, 61}, {61, 73}};

    private final PitPlugin plugin;
    private final SpireStructure structure;
    /** 丘を出す場所 (config の koth.positions。録画は Summer Area の (-61, 85, -67) = 丘の真ん中の真下の地面)。 */
    private final com.pitremake.map.EventPositions positions;
    /** 録画の丘のデータ (resources/koth のフェーズ)。 */
    private String hill;
    private boolean available;
    private String area;
    /** 丘の上の面 "x,z" と、その高さ。 */
    private final Set<String> top = new HashSet<>();
    private int topY;
    private long endTime;
    private long nextEvent;
    private int lastAnnounced;
    private BukkitTask task;

    public KothEvent(PitPlugin plugin) {
        this.plugin = plugin;
        this.structure = new SpireStructure(plugin, "koth", "koth-restore.yml");
        this.positions = new com.pitremake.map.EventPositions(plugin, "koth.positions", "koth/positions.yml",
                new com.pitremake.map.EventPositions.Position(HILL_X, HILL_Y, HILL_Z, "Summer Area"));
    }

    /** 録画の丘の真ん中の真下の地面 (丘の石英の段、y85)。 */
    private static final int HILL_X = -61;
    private static final int HILL_Y = 85;
    private static final int HILL_Z = -67;

    public com.pitremake.map.EventPositions getPositions() {
        return positions;
    }

    public void start() {
        available = structure.load(plugin.getPitMap().getName());
        // 丘の上 4 ブロックも空気で置く (別の場所に出したとき地形に埋まらないように)
        for (String name : structure.phaseNames()) {
            structure.fillAir(name, 4);
        }
        structure.recover();
        scheduleNext();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20, 20);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        if (area != null) {
            plugin.getPitSpawn().setFocus(null);
            plugin.getEventBossBar().hide();
            removeHologram();
            structure.restore(world());
            area = null;
        }
    }

    private World world() {
        return plugin.getServer().getWorlds().get(0);
    }

    private void scheduleNext() {
        nextEvent = System.currentTimeMillis() + (long) (plugin.getConfig().getDouble("koth.interval-minutes", 20) * 60_000);
    }

    public boolean isRunning() {
        return area != null;
    }

    private void tick() {
        long now = System.currentTimeMillis();
        if (area == null) {
            if (available && plugin.getConfig().getBoolean("koth.enabled", false) && now >= nextEvent) {
                if (plugin.isEventActive()) {
                    // 別のイベント (Auction / Spire) の最中は終わるまで待つ
                    nextEvent = now + 60_000;
                } else if (realPlayers() < plugin.getConfig().getInt("koth.min-players", 1)) {
                    scheduleNext();
                } else {
                    begin();
                }
            }
            return;
        }
        long left = (endTime - now + 999) / 1000;
        int minutes = (int) ((left + 59) / 60);
        if (left <= 0) {
            end();
            return;
        }
        updateHologram(left);
        if (minutes < lastAnnounced) {
            lastAnnounced = minutes;
            broadcast(prefix() + "§7for §e" + minutes + (minutes == 1 ? " §7min" : " §7mins"));
        }
    }

    private int realPlayers() {
        int count = 0;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!plugin.getBotManager().isBot(player)) {
                count++;
            }
        }
        return count;
    }

    private String prefix() {
        return "§d§lMINOR EVENT! §b§lKOTH §7in §e§l" + area + " ";
    }

    /** /pit koth start と時間。 */
    public boolean begin() {
        if (!available || area != null || plugin.isEventActive()) {
            return false;
        }
        com.pitremake.map.EventPositions.Position position = positions.random();
        if (position == null) {
            // 今のマップに丘の場所が無い
            return false;
        }
        List<String> hills = new ArrayList<>(structure.phaseNames());
        hill = hills.get(ThreadLocalRandom.current().nextInt(hills.size()));
        area = position.name;
        structure.setOffset(hill, position.x - HILL_X, position.y - HILL_Y, position.z - HILL_Z);
        // 上の面 = 丘の中 (上空の飾りより下) でいちばん高い段
        top.clear();
        topY = Integer.MIN_VALUE;
        int skyY = position.y + 60;
        for (int[] b : structure.blocks(hill)) {
            if (b[3] != 0 && b[1] < skyY) {
                topY = Math.max(topY, b[1]);
            }
        }
        for (int[] b : structure.blocks(hill)) {
            if (b[1] == topY && b[3] != 0) {
                top.add(b[0] + "," + b[2]);
            }
        }
        structure.place(world(), hill);
        endTime = System.currentTimeMillis() + DURATION_MINUTES * 60_000L;
        lastAnnounced = DURATION_MINUTES;
        broadcast(prefix() + "§7for §e" + DURATION_MINUTES + " §7mins");
        // ボスバー (録画: §d§lMINOR EVENT! §b§lKOTH! §7Go to §e§l§lSUMMER§7! §7Ending in §a04:00§7)
        String where = area.split(" ")[0].toUpperCase(java.util.Locale.ROOT);
        plugin.getEventBossBar().show(new com.pitremake.util.EventBossBar.Content() {
            @Override
            public String text(int tick) {
                long left = Math.max(0, (endTime - System.currentTimeMillis() + 999) / 1000);
                return com.pitremake.util.EventBossBar.shimmer("MINOR EVENT!", "§d", "§9", tick) + " §b§lKOTH! §7Go to §e§l§l"
                        + where + "§7! §7Ending in §a" + com.pitremake.spire.SpireRules.time(left) + "§7";
            }

            @Override
            public double fraction() {
                return (endTime - System.currentTimeMillis()) / (DURATION_MINUTES * 60_000.0);
            }
        });
        spawnHologram();
        // 復活するのは丘への直行便のパッドに一番近いスポーン地点だけ (プレイヤーも Bot も)。
        // 丘からのまっすぐの距離だと、パッドとは別の方向のスポーン地点になることがある (例: Spring Area の (-75, 12))
        double[] hill = hillCenter();
        double[] pad = plugin.getLauncher().padTop(hill[0], hill[2]);
        plugin.getPitSpawn().setFocus(pad != null ? pad : new double[]{hill[0], hill[2]});
        // 出ている Bot の半分はパッドから丘へ
        plugin.getBotManager().setRoute(plugin.getLauncher().kothRoute());
        for (int i = 0; i < CHORD.length; i++) {
            int[] pitches = CHORD[i];
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> chord("note.harp", 0.8f, pitches), i * 3L);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> chord("note.bassattack", 0.72f, pitches), i * 3L + 1);
        }
        return true;
    }

    private void chord(String sound, float volume, int[] pitches) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            for (int pitch : pitches) {
                player.playSound(player.getLocation(), sound, volume, pitch / 63f);
            }
        }
    }

    /** /pit koth end と時間切れ。 */
    public boolean end() {
        if (area == null) {
            return false;
        }
        broadcast(prefix() + "§7ended");
        plugin.getEventBossBar().hide();
        removeHologram();
        plugin.getBotManager().setRoute(null);
        plugin.getPitSpawn().setFocus(null);
        structure.restore(world());
        area = null;
        top.clear();
        scheduleNext();
        return true;
    }

    // ------------------------------------------------------------------ ランチャーパッドの文字

    /**
     * 丘に一番近いランチャーパッドの上の 4 行 (録画: Summer Area はパッド (-14, -14) の上、透明なマーカーのアーマースタンド)。
     * §d§lMINOR EVENT! (y117.188) / §bKING OF THE HILL (116.875) / §din §e§lSummer Area (116.594) / §a04:00 (116.281、毎秒)。
     * 高さはスポーンの高さ (114) からの差。
     */
    private static final double[] LINE_Y = {3.188, 2.875, 2.594, 2.281};

    private final List<ArmorStand> hologram = new ArrayList<>();

    private void spawnHologram() {
        removeHologram();
        double[] center = hillCenter();
        double[] pad = plugin.getLauncher().padTop(center[0], center[2]);
        if (pad == null) {
            return;
        }
        String[] lines = {"§d§lMINOR EVENT!", "§bKING OF THE HILL", "§din §e§l" + area,
                "§a" + com.pitremake.spire.SpireRules.time(DURATION_MINUTES * 60)};
        for (int i = 0; i < lines.length; i++) {
            Location at = new Location(world(), pad[0], plugin.getPitMap().getSpawnY() + LINE_Y[i], pad[1]);
            at.getChunk().load();
            // 前回 (止まったとき) に残った同じ場所の文字を消す
            for (Entity entity : at.getWorld().getNearbyEntities(at, 0.2, 0.2, 0.2)) {
                if (entity instanceof ArmorStand && !((ArmorStand) entity).isVisible()) {
                    entity.remove();
                }
            }
            ArmorStand stand = (ArmorStand) world().spawnEntity(at, EntityType.ARMOR_STAND);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setBasePlate(false);
            stand.setMarker(true);
            stand.setCustomName(lines[i]);
            stand.setCustomNameVisible(true);
            hologram.add(stand);
        }
    }

    private void updateHologram(long left) {
        if (hologram.size() == LINE_Y.length) {
            hologram.get(LINE_Y.length - 1).setCustomName("§a" + com.pitremake.spire.SpireRules.time(left));
        }
    }

    private void removeHologram() {
        for (ArmorStand stand : hologram) {
            stand.remove();
        }
        hologram.clear();
    }

    /** 丘のダイヤの上に立っているか (少しジャンプしていても)。 */
    public boolean onHill(Player player) {
        if (area == null || player == null) {
            return false;
        }
        Location at = player.getLocation();
        if (!at.getWorld().equals(world()) || at.getY() < topY + 1 || at.getY() > topY + 2.5) {
            return false;
        }
        return top.contains(at.getBlockX() + "," + at.getBlockZ());
    }

    /** 丘の上の面の真ん中 {x, 立つ高さ, z}。 */
    public double[] hillCenter() {
        double x = 0;
        double z = 0;
        for (String key : top) {
            String[] p = key.split(",");
            x += Integer.parseInt(p[0]) + 0.5;
            z += Integer.parseInt(p[1]) + 0.5;
        }
        int n = Math.max(1, top.size());
        return new double[]{x / n, topY + 1, z / n};
    }

    /** キルの XP・ゴールドに掛ける数 (バウンティは別)。 */
    public double killMultiplier(Player killer) {
        return onHill(killer) ? MULTIPLIER : 1;
    }

    /** アシストは自分と相手の両方が丘の上のときだけ。 */
    public double assistMultiplier(Player assister, Player victim) {
        return onHill(assister) && onHill(victim) ? MULTIPLIER : 1;
    }

    private void broadcast(String message) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendMessage(message);
        }
        plugin.getServer().getConsoleSender().sendMessage(message);
    }
}
