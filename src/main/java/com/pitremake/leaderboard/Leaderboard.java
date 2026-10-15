package com.pitremake.leaderboard;

import com.pitremake.PitPlugin;
import com.pitremake.data.PlayerProfiles;
import com.pitremake.stats.StatsManager;
import com.pitremake.util.PlayerPackets;
import net.minecraft.server.v1_8_R3.DataWatcher;
import net.minecraft.server.v1_8_R3.EntityLiving;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityMetadata;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Rabbit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * スポーンのリーダーボード (録画 2026-10-15_10-32-13.mcrc)。
 * <pre>
 * 文字   : (16.188, 117.375〜112.031, 0.5) に透明なアーマースタンド 15 行 (マーカーではないので右クリックできる)
 *          TOP ACTIVE PLAYERS / Pit Level / 1.〜10. / You have (見る人ごと) / All-time best players! / Players who logged in this week
 *          右クリック → §a§lYEP! §7That's the leaderboards... と mob.chicken.plop (0.5 / 63)
 * ピッカー: (14.5, 114, 2.5) の白黒のウサギ §e§lLEADERBOARD PICKER (名前は見たときだけ)。右クリックで Leaderboard Picker
 * </pre>
 * 順位は今までに稼いだ XP ({@link StatsManager#getLifetimeXp})。この 1 週間にログインしたプレイヤーだけ (Bot は除く)。
 * 位置は config の leaderboard.position / leaderboard.picker。
 */
public final class Leaderboard implements Listener {

    /** 録画の各行の高さ。 */
    private static final double[] LINE_Y = {117.375, 117.0, 116.562, 116.188, 115.812, 115.469, 115.094, 114.719,
            114.344, 113.969, 113.594, 113.219, 112.781, 112.406, 112.031};
    private static final int FIRST_ENTRY_LINE = 2;
    private static final int YOU_HAVE_LINE = FIRST_ENTRY_LINE + LeaderboardRules.TOP;
    private static final int REFRESH_TICKS = 60 * 20;
    private static final int PERSONAL_TICKS = 20;
    private static final double PERSONAL_RANGE = 48;
    private static final double CLEANUP_RADIUS = 0.3;
    private static final String CLICK_SOUND = "mob.chicken.plop";

    private final PitPlugin plugin;
    private final List<ArmorStand> stands = new ArrayList<>();
    private Rabbit picker;
    private Location base;
    private Location pickerAt;
    private List<UUID> ranking = new ArrayList<>();
    private final Map<UUID, Long> lifetimeXp = new HashMap<>();
    private BukkitTask task;
    private int ticks;

    public Leaderboard(PitPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("leaderboard.enabled", true)) {
            return;
        }
        World world = plugin.getServer().getWorlds().get(0);
        base = parse(world, plugin.getConfig().getString("leaderboard.position", "16.188,117.375,0.5"));
        pickerAt = parse(world, plugin.getConfig().getString("leaderboard.picker", "14.5,114,2.5,111.1"));
        if (base == null || pickerAt == null) {
            plugin.getLogger().warning("Invalid leaderboard.position / leaderboard.picker in config.yml");
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        spawn();
        refresh();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, PERSONAL_TICKS, PERSONAL_TICKS);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        despawn();
    }

    /** "x,y,z" か "x,y,z,yaw"。 */
    private static Location parse(World world, String text) {
        String[] parts = text.split(",");
        try {
            return new Location(world, Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim()),
                    Double.parseDouble(parts[2].trim()), parts.length > 3 ? Float.parseFloat(parts[3].trim()) : 0, 0);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    private void spawn() {
        base.getChunk().load();
        pickerAt.getChunk().load();
        cleanup();
        for (double y : LINE_Y) {
            Location at = base.clone().add(0, y - LINE_Y[0], 0);
            ArmorStand stand = base.getWorld().spawn(at, ArmorStand.class);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setBasePlate(false);
            stand.setCustomNameVisible(false);
            stands.add(stand);
        }
        picker = pickerAt.getWorld().spawn(pickerAt, Rabbit.class);
        picker.setRabbitType(Rabbit.Type.BLACK_AND_WHITE);
        picker.setCustomName("§e§lLEADERBOARD PICKER");
        picker.setCustomNameVisible(false);
        picker.setRemoveWhenFarAway(false);
        EntityLiving handle = ((CraftLivingEntity) picker).getHandle();
        // 動かない・鳴かない (NpcManager の村人と同じく NBT で)
        NBTTagCompound tag = new NBTTagCompound();
        handle.c(tag);
        tag.setByte("NoAI", (byte) 1);
        tag.setByte("Silent", (byte) 1);
        handle.f(tag);
        handle.setLocation(pickerAt.getX(), pickerAt.getY(), pickerAt.getZ(), pickerAt.getYaw(), 0);
        handle.f(pickerAt.getYaw());
    }

    private void despawn() {
        for (ArmorStand stand : stands) {
            stand.remove();
        }
        stands.clear();
        if (picker != null) {
            picker.remove();
            picker = null;
        }
    }

    /** 前回消し損ねた文字とウサギを消す。 */
    private void cleanup() {
        double height = LINE_Y[0] - LINE_Y[LINE_Y.length - 1];
        for (Entity entity : base.getWorld().getNearbyEntities(base.clone().add(0, -height / 2, 0), CLEANUP_RADIUS,
                height / 2 + CLEANUP_RADIUS, CLEANUP_RADIUS)) {
            if (entity instanceof ArmorStand && !((ArmorStand) entity).isVisible()) {
                entity.remove();
            }
        }
        for (Entity entity : pickerAt.getWorld().getNearbyEntities(pickerAt, CLEANUP_RADIUS, 1, CLEANUP_RADIUS)) {
            if (entity instanceof Rabbit) {
                entity.remove();
            }
        }
    }

    private void tick() {
        ticks += PERSONAL_TICKS;
        boolean invalid = picker == null || !picker.isValid();
        for (ArmorStand stand : stands) {
            invalid |= !stand.isValid();
        }
        if (invalid && base.getWorld().isChunkLoaded(base.getBlockX() >> 4, base.getBlockZ() >> 4)) {
            // チャンクの読み込み直しなどで別のエンティティになっていたら出し直す
            despawn();
            spawn();
            refresh();
        } else if (picker != null && picker.getLocation().distanceSquared(pickerAt) > 0.01) {
            picker.teleport(pickerAt);
        }
        if (ticks >= REFRESH_TICKS) {
            ticks = 0;
            refresh();
        }
        showPersonalLines();
    }

    /** 順位を計算し直して文字を書き換える。 */
    public void refresh() {
        if (stands.isEmpty()) {
            return;
        }
        StatsManager stats = plugin.getStatsManager();
        PlayerProfiles profiles = plugin.getPlayerProfiles();
        long now = System.currentTimeMillis();
        lifetimeXp.clear();
        for (UUID id : stats.players()) {
            PlayerProfiles.Profile profile = profiles.find(id.toString());
            Player online = plugin.getServer().getPlayer(id);
            boolean bot = online != null && plugin.getBotManager().isBot(online);
            if (profile == null || profile.name.isEmpty() || bot
                    || (online == null && now - profile.lastSeen > LeaderboardRules.WEEK_MILLIS)) {
                continue;
            }
            lifetimeXp.put(id, stats.getLifetimeXp(id));
        }
        ranking = LeaderboardRules.rank(lifetimeXp);

        name(0, "§b§lTOP ACTIVE PLAYERS");
        name(1, "§7Pit Level");
        List<String> entries = entries();
        for (int i = 0; i < LeaderboardRules.TOP; i++) {
            name(FIRST_ENTRY_LINE + i, i < entries.size() ? entries.get(i) : "");
        }
        name(YOU_HAVE_LINE, "§7You have: §b0 XP");
        name(YOU_HAVE_LINE + 1, "§7All-time §ebest§7 players!");
        name(YOU_HAVE_LINE + 2, "§7§oPlayers who logged in this week");
    }

    /** 上位 10 人の行 (例: §e1. §8[§e§eL§8-§b§l120§8] §b名前§7 - §b11B XP)。 */
    List<String> entries() {
        List<String> lines = new ArrayList<>();
        StatsManager stats = plugin.getStatsManager();
        PlayerProfiles profiles = plugin.getPlayerProfiles();
        for (int i = 0; i < Math.min(LeaderboardRules.TOP, ranking.size()); i++) {
            UUID id = ranking.get(i);
            PlayerProfiles.Profile profile = profiles.find(id.toString());
            lines.add("§e" + (i + 1) + ". " + stats.chatTag(id) + " " + plugin.getRankManager().nameColor(id)
                    + (profile == null ? "?" : profile.name) + "§7 - §b" + LeaderboardRules.abbreviate(lifetimeXp.get(id)) + " XP");
        }
        return lines;
    }

    /** 順位 (1 から)。載っていなければ 0。 */
    int position(UUID player) {
        return ranking.indexOf(player) + 1;
    }

    private void name(int line, String text) {
        ArmorStand stand = stands.get(line);
        stand.setCustomName(text);
        stand.setCustomNameVisible(!text.isEmpty());
    }

    /** "You have" の行は見る人ごとの XP (名前だけをパケットで上書きする)。 */
    private void showPersonalLines() {
        if (stands.size() <= YOU_HAVE_LINE) {
            return;
        }
        ArmorStand stand = stands.get(YOU_HAVE_LINE);
        Location at = stand.getLocation();
        for (Player player : at.getWorld().getPlayers()) {
            if (plugin.getBotManager().isBot(player) || player.getLocation().distanceSquared(at) > PERSONAL_RANGE * PERSONAL_RANGE) {
                continue;
            }
            DataWatcher watcher = new DataWatcher(null);
            watcher.a(2, "§7You have: §b" + LeaderboardRules.abbreviate(plugin.getStatsManager().getLifetimeXp(player.getUniqueId())) + " XP");
            PlayerPackets.send(player, new PacketPlayOutEntityMetadata(stand.getEntityId(), watcher, true));
        }
    }

    private boolean isLine(Entity entity) {
        return entity instanceof ArmorStand && stands.contains(entity);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEntityEvent event) {
        Entity clicked = event.getRightClicked();
        if (picker != null && clicked.equals(picker)) {
            event.setCancelled(true);
            new LeaderboardPickerMenu(plugin, event.getPlayer(), this).open();
        } else if (isLine(clicked)) {
            event.setCancelled(true);
        }
    }

    /** 文字のアーマースタンドの右クリック (1.8 ではこちらが呼ばれる)。 */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractAt(PlayerInteractAtEntityEvent event) {
        if (isLine(event.getRightClicked())) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            player.sendMessage("§a§lYEP! §7That's the leaderboards...");
            player.playSound(player.getLocation(), CLICK_SOUND, 0.5f, 1.0f);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onManipulate(PlayerArmorStandManipulateEvent event) {
        if (isLine(event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent event) {
        if (isLine(event.getEntity()) || (picker != null && event.getEntity().equals(picker))) {
            event.setCancelled(true);
        }
    }
}
