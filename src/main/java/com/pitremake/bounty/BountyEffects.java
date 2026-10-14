package com.pitremake.bounty;

import com.pitremake.util.PlayerPackets;
import net.minecraft.server.v1_8_R3.EntityArmorStand;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityTeleport;
import net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntityLiving;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * バウンティの付いたプレイヤーの周りに金色の額 (§6§l1800g) が湧いて昇っていく演出。
 * 録画 (2026-10-14_00-39-15.mcrc) と同じく、名前だけ見える透明なアーマースタンドを
 * 0.2 秒ごとに足元〜腰のあたりに出し、毎 tick 上に動かして 0.5 秒で消す。
 * アーマースタンドはパケットだけの存在 (サーバーのワールドには追加しない)。本人以外の近くのプレイヤーに見える。
 */
public final class BountyEffects {

    private static final int SPAWN_INTERVAL_TICKS = 4;
    private static final int LIFETIME_TICKS = 10;
    private static final double RISE_PER_TICK = 0.22;
    /** 出る位置: 横は ±SPREAD、縦は足元から MIN_Y〜MAX_Y。 */
    private static final double SPREAD = 0.8;
    private static final double MIN_Y = -0.9;
    private static final double MAX_Y = 0.1;
    private static final double VIEW_DISTANCE = 48;

    private final JavaPlugin plugin;
    private final BountyManager bounties;
    private final List<Hologram> holograms = new ArrayList<>();
    private BukkitTask task;
    private int tick;

    public BountyEffects(JavaPlugin plugin, BountyManager bounties) {
        this.plugin = plugin;
        this.bounties = bounties;
    }

    public void start() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1, 1);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        for (Hologram hologram : holograms) {
            hologram.destroy();
        }
        holograms.clear();
    }

    private void tick() {
        if (++tick % SPAWN_INTERVAL_TICKS == 0) {
            for (Map.Entry<UUID, Integer> entry : bounties.all().entrySet()) {
                Player owner = Bukkit.getPlayer(entry.getKey());
                if (owner != null && !owner.isDead()) {
                    spawn(owner, "§6§l" + entry.getValue() + "g");
                }
            }
        }
        for (Iterator<Hologram> it = holograms.iterator(); it.hasNext(); ) {
            Hologram hologram = it.next();
            if (++hologram.age >= LIFETIME_TICKS) {
                hologram.destroy();
                it.remove();
            } else {
                hologram.rise();
            }
        }
    }

    private void spawn(Player owner, String text) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Location at = owner.getLocation().add(
                (random.nextDouble() * 2 - 1) * SPREAD,
                MIN_Y + random.nextDouble() * (MAX_Y - MIN_Y),
                (random.nextDouble() * 2 - 1) * SPREAD);
        List<Player> viewers = new ArrayList<>();
        for (Player player : owner.getWorld().getPlayers()) {
            // バウンティが付いている本人には見えない
            if (!player.equals(owner) && player.getLocation().distanceSquared(at) <= VIEW_DISTANCE * VIEW_DISTANCE) {
                viewers.add(player);
            }
        }
        if (viewers.isEmpty()) {
            return;
        }
        EntityArmorStand stand = new EntityArmorStand(((CraftWorld) at.getWorld()).getHandle(), at.getX(), at.getY(), at.getZ());
        stand.setInvisible(true);
        stand.setGravity(false);
        // マーカー (当たり判定なし、名前が足元の位置に出る)
        stand.n(true);
        stand.setCustomName(text);
        stand.setCustomNameVisible(true);
        Hologram hologram = new Hologram(stand, viewers);
        for (Player viewer : viewers) {
            PlayerPackets.send(viewer, new PacketPlayOutSpawnEntityLiving(stand));
        }
        holograms.add(hologram);
    }

    private static final class Hologram {
        final EntityArmorStand stand;
        final List<Player> viewers;
        int age;

        Hologram(EntityArmorStand stand, List<Player> viewers) {
            this.stand = stand;
            this.viewers = viewers;
        }

        void rise() {
            stand.setPosition(stand.locX, stand.locY + RISE_PER_TICK, stand.locZ);
            PacketPlayOutEntityTeleport packet = new PacketPlayOutEntityTeleport(stand);
            for (Player viewer : viewers) {
                if (viewer.isOnline()) {
                    PlayerPackets.send(viewer, packet);
                }
            }
        }

        void destroy() {
            PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(stand.getId());
            for (Player viewer : viewers) {
                if (viewer.isOnline()) {
                    PlayerPackets.send(viewer, packet);
                }
            }
        }
    }
}
