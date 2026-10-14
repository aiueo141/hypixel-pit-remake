package com.pitremake.gold;

import com.pitremake.PitPlugin;
import com.pitremake.combat.MysticAccess;
import com.pitremake.renown.RenownUpgrade;
import com.pitremake.upgrade.Passive;
import com.pitremake.util.NbtUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Pit に落ちている金インゴット (録画 2026-10-14_20-58-24 / 21-06-08.mcrc)。
 * <pre>
 * 出現 : スポーン以外のいたるところに。何個かまとめて出て、少し上に跳ねて落ちる (速さ 0.2 上向き)
 * 拾う : 拾った人に §6§lGOLD PICKUP! §7from the ground §62.05g と random.orb (1.0 / 1.79)。アイテムは消える
 *        元は 2g (録画: Renown Gold Boost I で 2.05g)。Gold Boost (Passive) / Renown Gold Boost の % を足し算で掛ける。
 *        パンツの Pebble で +10 / 20 / 30g (III は 1❤ 回復も)
 * </pre>
 * 落ちている数・出る間隔は録画から大まかに (同時に最大 MAX_INGOTS 個、WAVE_TICKS ごとに WAVE_SIZE 個)。
 * Magnum Opus と Gold Nano-factory の金インゴットも同じもの。
 */
public final class GoldIngots implements Listener {

    private static final String KEY = "PitGoldIngot";
    private static final double BASE_GOLD = 2;
    private static final int MAX_INGOTS = 25;
    private static final long WAVE_TICKS = 60;
    private static final int WAVE_SIZE = 4;
    /** 出す場所を探す範囲 (Pit の中心からの距離)。 */
    private static final int RANGE = 60;
    private static final double PASSIVE_GOLD_BOOST = 0.10;
    private static final double RENOWN_GOLD_BOOST = 0.025;

    private final PitPlugin plugin;
    private final Set<UUID> spawned = new HashSet<>();
    private BukkitTask task;

    public GoldIngots(PitPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        // 前に残ったものを消す
        for (World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Item && isIngot(((Item) entity).getItemStack())) {
                    entity.remove();
                }
            }
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::wave, WAVE_TICKS, WAVE_TICKS);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        for (World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (spawned.contains(entity.getUniqueId())) {
                    entity.remove();
                }
            }
        }
        spawned.clear();
    }

    public static ItemStack ingot() {
        return NbtUtil.setInt(new ItemStack(Material.GOLD_INGOT), KEY, 1);
    }

    public static boolean isIngot(ItemStack item) {
        return item != null && item.getType() == Material.GOLD_INGOT && NbtUtil.has(item, KEY);
    }

    /** その場所から金インゴットを 1 つ落とす (velocity は跳ねる速さ)。 */
    public void drop(Location at, Vector velocity) {
        Item item = at.getWorld().dropItem(at, ingot());
        item.setVelocity(velocity);
        spawned.add(item.getUniqueId());
    }

    /** まわりにばらまく (Magnum Opus)。 */
    public void scatter(Location at, int count) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            drop(at.clone().add(0, 0.5, 0), new Vector(random.nextDouble(-0.35, 0.35), random.nextDouble(0.3, 0.6),
                    random.nextDouble(-0.35, 0.35)));
        }
    }

    private void wave() {
        World world = plugin.getPitSpawn().randomPoint().getWorld();
        Iterator<UUID> it = spawned.iterator();
        Set<UUID> alive = new HashSet<>();
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Item && entity.isValid()) {
                alive.add(entity.getUniqueId());
            }
        }
        while (it.hasNext()) {
            if (!alive.contains(it.next())) {
                it.remove();
            }
        }
        for (int i = 0; i < WAVE_SIZE && spawned.size() < MAX_INGOTS; i++) {
            Location at = randomGround(world);
            if (at != null) {
                // 録画: 少し上に跳ねて落ちる
                drop(at, new Vector(ThreadLocalRandom.current().nextDouble(-0.1, 0.1), 0.2,
                        ThreadLocalRandom.current().nextDouble(-0.1, 0.1)));
            }
        }
    }

    /** スポーン以外の、地面の上のランダムな場所 (見つからなければ null)。 */
    private Location randomGround(World world) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int top = plugin.getPitMap().getSpawnY() - 2;
        int bottom = plugin.getPitMap().getFloorY() - 4;
        for (int attempt = 0; attempt < 20; attempt++) {
            int x = random.nextInt(-RANGE, RANGE + 1);
            int z = random.nextInt(-RANGE, RANGE + 1);
            for (int y = top; y >= bottom; y--) {
                Block block = world.getBlockAt(x, y, z);
                Block above = block.getRelative(0, 1, 0);
                if (block.getType().isSolid() && above.getType() == Material.AIR && above.getRelative(0, 1, 0).getType() == Material.AIR) {
                    Location at = above.getLocation().add(0.5, 0.2, 0.5);
                    if (!plugin.getPitSpawn().isProtected(at)) {
                        return at;
                    }
                    break;
                }
            }
        }
        return null;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(PlayerPickupItemEvent event) {
        ItemStack stack = event.getItem().getItemStack();
        if (!isIngot(stack)) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (plugin.getBotManager().isBot(player)) {
            return;
        }
        spawned.remove(event.getItem().getUniqueId());
        event.getItem().remove();
        UUID id = player.getUniqueId();
        double boost = PASSIVE_GOLD_BOOST * plugin.getUpgradeManager().getTier(id, Passive.GOLD_BOOST)
                + RENOWN_GOLD_BOOST * plugin.getUpgradeManager().getRenownTier(id, RenownUpgrade.RENOWN_GOLD_BOOST);
        double gold = BASE_GOLD * (1 + boost) * stack.getAmount();
        Integer pebble = MysticAccess.isSuppressed(player) ? null : MysticAccess.pantsEnchants(player).get("increase_gold_pickup");
        if (pebble != null) {
            gold += 10 * Math.min(pebble, 3) * stack.getAmount();
            if (pebble >= 3 && !player.isDead()) {
                player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 2));
            }
        }
        plugin.getGoldManager().add(id, gold);
        player.sendMessage("§6§lGOLD PICKUP! §7from the ground §6" + GoldManager.formatExact(gold));
        player.playSound(player.getLocation(), "random.orb", 1.0f, 113 / 63f);
        plugin.getSidebar().update(player);
    }

    /** 確認用。 */
    public int count() {
        return spawned.size();
    }
}
