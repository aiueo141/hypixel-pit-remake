package com.pitremake.npc;

import net.minecraft.server.v1_8_R3.EntityLiving;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.logging.Level;

/**
 * 村人 NPC の設置・削除と保存。場所はプラグインの中の maps/&lt;マップ&gt;.yml、ゲーム内で変えると plugins/Pit/npcs.yml (こちらが優先)。
 * 村人と文字のアーマースタンドはサーバー起動時に出し、停止時に消す (ワールドに残さない)。
 * 村人は動かず (NoAI)、ダメージを受けず、押されても元の位置に戻る。右クリックしても取引画面は開かない。
 */
public final class NpcManager implements Listener {

    /** 押されて動いた村人を戻す間隔 (tick)。 */
    private static final int RETURN_INTERVAL_TICKS = 20;
    /** 起動時に、前回消し損ねた NPC とみなして消す範囲 (ブロック)。 */
    private static final double CLEANUP_RADIUS = 0.6;

    private final JavaPlugin plugin;
    private final File file;
    private final List<Npc> npcs = new ArrayList<>();
    private BukkitTask task;
    private BiConsumer<Player, PitNpcType> clickHandler;

    public NpcManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "npcs.yml");
    }

    private static final class Npc {
        final PitNpcType type;
        final Location location;
        Villager villager;
        ArmorStand title;
        ArmorStand subtitle;

        Npc(PitNpcType type, Location location) {
            this.type = type;
            this.location = location;
        }

        boolean owns(Entity entity) {
            return entity.equals(villager) || entity.equals(title) || entity.equals(subtitle);
        }
    }

    public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        load();
        for (Npc npc : npcs) {
            cleanup(npc.location);
            spawn(npc);
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::returnMovedVillagers,
                RETURN_INTERVAL_TICKS, RETURN_INTERVAL_TICKS);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        for (Npc npc : npcs) {
            despawn(npc);
        }
    }

    /** location に NPC を置いて保存する。 */
    public void create(PitNpcType type, Location location) {
        Npc npc = new Npc(type, location.clone());
        npcs.add(npc);
        spawn(npc);
        save();
    }

    /** location から radius ブロック以内で一番近い NPC を消して保存する。消したらその種類、無ければ null。 */
    public PitNpcType removeNearest(Location location, double radius) {
        Npc nearest = null;
        double best = radius * radius;
        for (Npc npc : npcs) {
            if (npc.location.getWorld().equals(location.getWorld())) {
                double distance = npc.location.distanceSquared(location);
                if (distance <= best) {
                    best = distance;
                    nearest = npc;
                }
            }
        }
        if (nearest == null) {
            return null;
        }
        despawn(nearest);
        npcs.remove(nearest);
        save();
        return nearest.type;
    }

    /** 例: "UPGRADES world (12.5, 55.0, 2.5)" */
    public List<String> describe() {
        List<String> lines = new ArrayList<>();
        for (Npc npc : npcs) {
            Location l = npc.location;
            lines.add(String.format("%s %s (%.1f, %.1f, %.1f)", npc.type, l.getWorld().getName(), l.getX(), l.getY(), l.getZ()));
        }
        return lines;
    }

    private void spawn(Npc npc) {
        Location at = npc.location;
        at.getChunk().load();
        Villager villager = (Villager) at.getWorld().spawnEntity(at, EntityType.VILLAGER);
        villager.setProfession(npc.type.getProfession());
        villager.setAdult();
        villager.setRemoveWhenFarAway(false);
        EntityLiving handle = ((CraftLivingEntity) villager).getHandle();
        // 動かない・鳴かない (1.8 には API が無いので NBT で設定する)
        NBTTagCompound tag = new NBTTagCompound();
        handle.c(tag);
        tag.setByte("NoAI", (byte) 1);
        tag.setByte("Silent", (byte) 1);
        handle.f(tag);
        handle.setLocation(at.getX(), at.getY(), at.getZ(), at.getYaw(), 0);
        handle.f(at.getYaw());
        npc.villager = villager;
        npc.title = hologram(at.clone().add(0, PitNpcType.TITLE_HEIGHT, 0), npc.type.getTitle());
        npc.subtitle = hologram(at.clone().add(0, PitNpcType.SUBTITLE_HEIGHT, 0), npc.type.getSubtitle());
    }

    /** 名前だけ見える透明なアーマースタンド (マーカー: 当たり判定なし)。 */
    private static ArmorStand hologram(Location at, String text) {
        ArmorStand stand = (ArmorStand) at.getWorld().spawnEntity(at, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setBasePlate(false);
        stand.setMarker(true);
        stand.setCustomName(text);
        stand.setCustomNameVisible(true);
        return stand;
    }

    private static void despawn(Npc npc) {
        for (Entity entity : new Entity[]{npc.villager, npc.title, npc.subtitle}) {
            if (entity != null) {
                entity.remove();
            }
        }
        npc.villager = null;
        npc.title = null;
        npc.subtitle = null;
    }

    /** サーバーが落ちたなどで前回消せなかった村人と文字を消す。 */
    private static void cleanup(Location location) {
        location.getChunk().load();
        for (Entity entity : location.getWorld().getNearbyEntities(location, CLEANUP_RADIUS, PitNpcType.TITLE_HEIGHT + 1, CLEANUP_RADIUS)) {
            if (entity instanceof Villager || (entity instanceof ArmorStand && !((ArmorStand) entity).isVisible())) {
                entity.remove();
            }
        }
    }

    private void returnMovedVillagers() {
        for (Npc npc : npcs) {
            Location l = npc.location;
            if (npc.villager == null || !l.getWorld().isChunkLoaded(l.getBlockX() >> 4, l.getBlockZ() >> 4)) {
                continue;
            }
            if (!npc.villager.isValid()) {
                // チャンクの読み込み直しなどで別のエンティティになっていたら出し直す
                despawn(npc);
                cleanup(npc.location);
                spawn(npc);
            } else if (npc.villager.getLocation().distanceSquared(npc.location) > 0.01) {
                npc.villager.teleport(npc.location);
            }
        }
    }

    private Npc find(Entity entity) {
        for (Npc npc : npcs) {
            if (npc.owns(entity)) {
                return npc;
            }
        }
        return null;
    }

    /** 右クリックしたときの処理 (画面を開くなど)。 */
    public void setClickHandler(BiConsumer<Player, PitNpcType> clickHandler) {
        this.clickHandler = clickHandler;
    }

    /** 右クリックしても取引画面は開かず、代わりに {@link #setClickHandler} の処理をする。 */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEntityEvent event) {
        Npc npc = find(event.getRightClicked());
        if (npc != null) {
            event.setCancelled(true);
            if (clickHandler != null) {
                clickHandler.accept(event.getPlayer(), npc.type);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractAt(PlayerInteractAtEntityEvent event) {
        if (find(event.getRightClicked()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent event) {
        if (find(event.getEntity()) != null) {
            event.setCancelled(true);
        }
    }

    /** npcs.yml (ゲーム内で置き直したとき) があればそれ、無ければプラグインの中の今のマップの npcs (maps/&lt;マップ&gt;.yml)。 */
    private void load() {
        YamlConfiguration yaml = file.exists() ? YamlConfiguration.loadConfiguration(file) : com.pitremake.map.MapData.of(plugin);
        for (Object raw : yaml.getList("npcs", new ArrayList<Object>())) {
            if (!(raw instanceof Map)) {
                continue;
            }
            ConfigurationSection section = new YamlConfiguration().createSection("npc", (Map<?, ?>) raw);
            PitNpcType type = PitNpcType.fromName(section.getString("type"));
            World world = section.contains("world") ? plugin.getServer().getWorld(section.getString("world"))
                    : plugin.getServer().getWorlds().get(0);
            if (type == null || world == null) {
                plugin.getLogger().warning("Skipping NPC in npcs.yml: " + raw);
                continue;
            }
            npcs.add(new Npc(type, new Location(world, section.getDouble("x"), section.getDouble("y"),
                    section.getDouble("z"), (float) section.getDouble("yaw"), 0)));
        }
    }

    private void save() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Npc npc : npcs) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("type", npc.type.name());
            entry.put("world", npc.location.getWorld().getName());
            entry.put("x", npc.location.getX());
            entry.put("y", npc.location.getY());
            entry.put("z", npc.location.getZ());
            entry.put("yaw", (double) npc.location.getYaw());
            list.add(entry);
        }
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("npcs", list);
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save npcs.yml", e);
        }
    }
}
