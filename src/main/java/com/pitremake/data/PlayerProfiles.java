package com.pitremake.data;

import com.pitremake.PitPlugin;
import com.pitremake.util.NbtUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * プレイヤーごとのファイル (plugins/Pit/players/&lt;UUID&gt;.yml)。デス・アシストなどの追加の記録と、
 * オフラインでも見られるよう持ち物・エンダーチェストの写し。
 * <pre>
 * name / first-join / last-seen / playtime (秒) / deaths / assists / highest-streak
 * inventory (36) / armor (4) / ender-chest (27〜54)  ← アイテムは NBT ごと Base64
 * </pre>
 * 写しはログアウト・1 分ごと・サーバー停止のときに取る。
 * 1 人 1 ファイルなので、人数が増えても全部を毎回書き直さない (将来 DB に移すときもこの単位で移せる)。
 */
public final class PlayerProfiles implements Listener {

    private static final long SNAPSHOT_TICKS = 20 * 60;

    /** 1 人分 (メモリ上)。 */
    public static final class Profile {
        public final UUID id;
        public String name = "";
        public long firstJoin;
        public long lastSeen;
        public long playtimeSeconds;
        public int deaths;
        public int assists;
        public int highestStreak;
        public List<ItemStack> inventory = Collections.emptyList();
        public List<ItemStack> armor = Collections.emptyList();
        public List<ItemStack> enderChest = Collections.emptyList();
        /** オンラインになった時刻 (プレイ時間の計算用。オフラインなら 0)。 */
        long sessionStart;

        Profile(UUID id) {
            this.id = id;
        }

        /** 今までのプレイ時間 (オンラインなら今のセッションも足す)。 */
        public long totalPlaytimeSeconds() {
            return playtimeSeconds + (sessionStart > 0 ? (System.currentTimeMillis() - sessionStart) / 1000 : 0);
        }
    }

    private final PitPlugin plugin;
    private final File folder;
    private final Map<UUID, Profile> profiles = new ConcurrentHashMap<>();
    /** 名前 (小文字) → UUID。 */
    private final Map<String, UUID> names = new ConcurrentHashMap<>();

    public PlayerProfiles(PitPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "players");
    }

    public void start() {
        if (!folder.isDirectory() && !folder.mkdirs()) {
            plugin.getLogger().warning("Could not create " + folder);
        }
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    Profile profile = read(UUID.fromString(file.getName().replace(".yml", "")), file);
                    profiles.put(profile.id, profile);
                    if (!profile.name.isEmpty()) {
                        names.put(profile.name.toLowerCase(Locale.ROOT), profile.id);
                    }
                } catch (IllegalArgumentException ignored) {
                    // UUID でないファイル名は飛ばす
                }
            }
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            join(player);
        }
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::snapshotAll, SNAPSHOT_TICKS, SNAPSHOT_TICKS);
    }

    public void stop() {
        snapshotAll();
    }

    private Profile read(UUID id, File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        Profile profile = new Profile(id);
        profile.name = yaml.getString("name", "");
        profile.firstJoin = yaml.getLong("first-join");
        profile.lastSeen = yaml.getLong("last-seen");
        profile.playtimeSeconds = yaml.getLong("playtime");
        profile.deaths = yaml.getInt("deaths");
        profile.assists = yaml.getInt("assists");
        profile.highestStreak = yaml.getInt("highest-streak");
        profile.inventory = items(yaml.getStringList("inventory"));
        profile.armor = items(yaml.getStringList("armor"));
        profile.enderChest = items(yaml.getStringList("ender-chest"));
        return profile;
    }

    private static List<ItemStack> items(List<String> encoded) {
        List<ItemStack> items = new ArrayList<>();
        for (String data : encoded) {
            try {
                items.add(data.isEmpty() ? null : NbtUtil.fromBase64(data));
            } catch (IOException | RuntimeException e) {
                items.add(null);
            }
        }
        return items;
    }

    private static List<String> encode(Collection<ItemStack> items) {
        List<String> encoded = new ArrayList<>();
        for (ItemStack item : items) {
            try {
                encoded.add(item == null || item.getType() == org.bukkit.Material.AIR ? "" : NbtUtil.toBase64(item));
            } catch (IOException e) {
                encoded.add("");
            }
        }
        return encoded;
    }

    private void write(Profile profile) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("name", profile.name);
        yaml.set("first-join", profile.firstJoin);
        yaml.set("last-seen", profile.lastSeen);
        yaml.set("playtime", profile.totalPlaytimeSeconds());
        yaml.set("deaths", profile.deaths);
        yaml.set("assists", profile.assists);
        yaml.set("highest-streak", profile.highestStreak);
        yaml.set("inventory", encode(profile.inventory));
        yaml.set("armor", encode(profile.armor));
        yaml.set("ender-chest", encode(profile.enderChest));
        try {
            yaml.save(new File(folder, profile.id + ".yml"));
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save profile of " + profile.name, e);
        }
    }

    private Profile profile(UUID id) {
        return profiles.computeIfAbsent(id, Profile::new);
    }

    /** 名前か UUID から探す (無ければ null)。 */
    public Profile find(String nameOrId) {
        UUID id = names.get(nameOrId.toLowerCase(Locale.ROOT));
        if (id == null) {
            try {
                id = UUID.fromString(nameOrId);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return profiles.get(id);
    }

    /** 最後にいた時刻の新しい順。 */
    public List<Profile> recent(int limit) {
        List<Profile> list = new ArrayList<>(profiles.values());
        list.sort((a, b) -> Long.compare(b.lastSeen, a.lastSeen));
        return list.subList(0, Math.min(limit, list.size()));
    }

    /** 持ち物の写しを取る (メインスレッドで)。 */
    private void snapshot(Player player) {
        Profile profile = profile(player.getUniqueId());
        profile.name = player.getName();
        profile.lastSeen = System.currentTimeMillis();
        profile.inventory = new ArrayList<>(java.util.Arrays.asList(player.getInventory().getContents()));
        profile.armor = new ArrayList<>(java.util.Arrays.asList(player.getInventory().getArmorContents()));
        profile.enderChest = new ArrayList<>(java.util.Arrays.asList(plugin.getEnderChests().contents(player)));
    }

    private void snapshotAll() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            snapshot(player);
            write(profile(player.getUniqueId()));
        }
    }

    private void join(Player player) {
        Profile profile = profile(player.getUniqueId());
        if (profile.firstJoin == 0) {
            profile.firstJoin = System.currentTimeMillis();
        }
        profile.name = player.getName();
        profile.sessionStart = System.currentTimeMillis();
        names.put(player.getName().toLowerCase(Locale.ROOT), player.getUniqueId());
        snapshot(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getBotManager().isBot(event.getPlayer())) {
            join(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.getBotManager().isBot(player)) {
            return;
        }
        snapshot(player);
        Profile profile = profile(player.getUniqueId());
        profile.playtimeSeconds = profile.totalPlaytimeSeconds();
        profile.sessionStart = 0;
        write(profile);
    }

    // ---------------------------------------------------------------- 記録 (KillListener から)

    public void addDeath(UUID player) {
        profile(player).deaths++;
    }

    public void addAssist(UUID player) {
        profile(player).assists++;
    }

    public void updateStreak(UUID player, int streak) {
        Profile profile = profile(player);
        profile.highestStreak = Math.max(profile.highestStreak, streak);
    }

    /** オンラインの人 (と写しのある人) の UUID → 名前 (確認用)。 */
    public Map<UUID, String> knownNames() {
        Map<UUID, String> map = new HashMap<>();
        for (Profile profile : profiles.values()) {
            map.put(profile.id, profile.name);
        }
        return map;
    }
}
