package com.pitremake.stats;

import com.pitremake.PitPlugin;
import com.pitremake.data.PitStat;
import com.pitremake.map.PitMapType;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * My pit stats の記録 ({@link PitStat}) を数える。Bot は数えない。ダメージは最終的なダメージ (HP)。
 * Launcher Launches・Golden Heads Eaten・Diamond Items Purchased は打ち上げ・Golden Head・ショップの処理から足す。
 */
public final class StatsTracker implements Listener {

    /** 床から何ブロック下を越えたら Jumps into Pit に数えるか。 */
    private static final double JUMP_DEPTH = 3;

    private final PitPlugin plugin;

    public StatsTracker(PitPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean counts(Entity entity) {
        return entity instanceof Player && !plugin.getBotManager().isBot((Player) entity);
    }

    private void add(Entity player, PitStat stat, double amount) {
        if (counts(player)) {
            plugin.getPlayerProfiles().add(player.getUniqueId(), stat, amount);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        double damage = event.getFinalDamage();
        Entity damager = event.getDamager();
        if (damager instanceof Player && event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            add(damager, PitStat.SWORD_HITS, 1);
            add(damager, PitStat.MELEE_DAMAGE_DEALT, damage);
            add(event.getEntity(), PitStat.MELEE_DAMAGE_TAKEN, damage);
        } else if (damager instanceof Arrow && ((Arrow) damager).getShooter() instanceof Player) {
            Player shooter = (Player) ((Arrow) damager).getShooter();
            add(shooter, PitStat.ARROW_HITS, 1);
            add(shooter, PitStat.BOW_DAMAGE_DEALT, damage);
            add(event.getEntity(), PitStat.BOW_DAMAGE_TAKEN, damage);
        }
    }

    /** Damage Taken は落下なども含めた全部 (録画: Melee + Bow より大きい)。 */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        add(event.getEntity(), PitStat.DAMAGE_TAKEN, event.getFinalDamage());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        add(event.getEntity(), PitStat.ARROWS_SHOT, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() == PlayerAnimationType.ARM_SWING) {
            add(event.getPlayer(), PitStat.LEFT_CLICKS, 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        add(event.getPlayer(), PitStat.CHAT_MESSAGES, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        add(event.getPlayer(), PitStat.BLOCKS_PLACED, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        add(event.getPlayer(), PitStat.BLOCKS_BROKEN, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() == Material.GOLDEN_APPLE) {
            add(event.getPlayer(), PitStat.GOLDEN_APPLES_EATEN, 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucket(PlayerBucketEmptyEvent event) {
        if (event.getBucket() == Material.LAVA_BUCKET) {
            add(event.getPlayer(), PitStat.LAVA_BUCKETS_EMPTIED, 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.FISHING) {
            add(event.getPlayer(), PitStat.FISHING_RODS_LAUNCHED, 1);
        }
    }

    /**
     * スポーンの床から下へ落ちたら Jumps into Pit: 中心から spawnRadius 以内で、床の {@link #JUMP_DEPTH} ブロック下を上から越えたとき。
     * 打ち上げ中は数えない。
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        PitMapType map = plugin.getPitMap();
        double line = map.getSpawnY() - JUMP_DEPTH;
        if (from.getY() < line || to.getY() >= line || player.isInsideVehicle()
                || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR
                || Math.hypot(from.getX(), from.getZ()) > map.getSpawnRadius()) {
            return;
        }
        add(player, PitStat.JUMPS_INTO_PIT, 1);
    }
}
