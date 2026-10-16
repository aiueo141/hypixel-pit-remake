package com.pitremake.permission;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * /pitpermission で op 以外のプレイヤーに付けるコマンドの権限 (plugins/Pit/permissions.yml に保存、入ったときに付ける)。
 * 付けられるのは地形・サーバーの設定を壊さないものだけ。/pit reload・map・backup・damage、/pitnpc、/pitspawn、
 * Spire / KOTH の場所、/pitpermission は op だけ (pit.admin)。
 */
public final class PitPermissions implements Listener {

    /** /pitpermission での名前 → 権限。 */
    public static final Map<String, String> GRANTS;

    static {
        Map<String, String> grants = new LinkedHashMap<>();
        grants.put("renown", "pit.cmd.renown");
        grants.put("prestige", "pit.cmd.prestige");
        grants.put("level", "pit.cmd.level");
        grants.put("gold", "pit.cmd.gold");
        grants.put("streak", "pit.cmd.streak");
        grants.put("bounty", "pit.cmd.bounty");
        grants.put("fresh", "pit.cmd.fresh");
        grants.put("menchant", "pit.cmd.menchant");
        grants.put("fillmystic", "pit.cmd.fillmystic");
        grants.put("instantupgrade", "pit.cmd.instantupgrade");
        grants.put("event", "pit.cmd.event");
        grants.put("bot", "pit.cmd.bot");
        grants.put("rank", "pit.rank.admin");
        grants.put("supporter", "pit.supporter");
        GRANTS = Collections.unmodifiableMap(grants);
    }

    private final JavaPlugin plugin;
    private final File file;
    /** プレイヤー → 付けた名前 (GRANTS のキー)。 */
    private final Map<UUID, Set<String>> granted = new HashMap<>();
    private final Map<UUID, String> names = new HashMap<>();
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();

    public PitPermissions(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "permissions.yml");
    }

    public void start() {
        load();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            apply(player);
        }
    }

    public void stop() {
        for (PermissionAttachment attachment : attachments.values()) {
            attachment.remove();
        }
        attachments.clear();
    }

    private void load() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String key : players.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                Set<String> set = new TreeSet<>();
                for (String name : players.getStringList(key + ".permissions")) {
                    if (GRANTS.containsKey(name)) {
                        set.add(name);
                    }
                }
                if (!set.isEmpty()) {
                    granted.put(id, set);
                    names.put(id, players.getString(key + ".name", key));
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unknown player in permissions.yml: " + key);
            }
        }
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Set<String>> entry : granted.entrySet()) {
            String key = "players." + entry.getKey();
            yaml.set(key + ".name", names.get(entry.getKey()));
            yaml.set(key + ".permissions", new ArrayList<>(entry.getValue()));
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save " + file + ": " + e);
        }
    }

    public Set<String> get(UUID player) {
        Set<String> set = granted.get(player);
        return set == null ? Collections.<String>emptySet() : Collections.unmodifiableSet(set);
    }

    /** 権限を持っているプレイヤー (UUID → 名前)。 */
    public Map<UUID, String> players() {
        return Collections.unmodifiableMap(names);
    }

    /** 付ける (true) / 外す (false)。変わった名前を返す。 */
    public List<String> change(UUID player, String name, Iterable<String> grants, boolean add) {
        Set<String> set = granted.computeIfAbsent(player, id -> new TreeSet<>());
        List<String> changed = new ArrayList<>();
        for (String grant : grants) {
            if (add ? set.add(grant) : set.remove(grant)) {
                changed.add(grant);
            }
        }
        if (set.isEmpty()) {
            granted.remove(player);
            names.remove(player);
        } else {
            names.put(player, name);
        }
        save();
        Player online = plugin.getServer().getPlayer(player);
        if (online != null) {
            apply(online);
        }
        return changed;
    }

    private void apply(Player player) {
        PermissionAttachment old = attachments.remove(player.getUniqueId());
        if (old != null) {
            old.remove();
        }
        Set<String> set = granted.get(player.getUniqueId());
        if (set != null) {
            PermissionAttachment attachment = player.addAttachment(plugin);
            for (String name : set) {
                attachment.setPermission(GRANTS.get(name), true);
            }
            attachments.put(player.getUniqueId(), attachment);
            // 名前が変わっていたら直す
            if (!player.getName().equals(names.get(player.getUniqueId()))) {
                names.put(player.getUniqueId(), player.getName());
                save();
            }
        }
        player.recalculatePermissions();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        apply(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        PermissionAttachment attachment = attachments.remove(event.getPlayer().getUniqueId());
        if (attachment != null) {
            attachment.remove();
        }
    }
}
