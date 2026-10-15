package com.pitremake.combat;

import com.pitremake.PitPlugin;
import com.pitremake.item.MysticData;
import com.pitremake.item.MysticItems;
import com.pitremake.item.MysticType;
import com.pitremake.util.PlayerPackets;
import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 弓・剣のブロックの特殊な挙動 (録画 Attacker.mcrc / victim.mcrc)。
 * <pre>
 * Volley       : 撃つと続けて矢が出る (I は 3 本、II は 4 本、III は 5 本)。ユーザーの指定で、全部 1 本目と 2 本目と同じ
 *                1 tick 間隔・1 本目と同じ速さとクリティカルで、その時に向いている方向へ
 *                (録画の III は 0 / 1 / 2 / 5 / 9 tick 目で 2 本目以降は速さ約 1.5 だった)
 * Mega Longbow : 弓を少しでも引いて離すと、最大まで引いた矢 (速さ 2.900・クリティカル) が出る (1 秒に 1 回)。
 *                音は実際の引き具合のバニラの弓の音 (録画: 0.2 秒で離すと音程 0.81、1 秒で 1.17)。
 *                Jump Boost II / III / IV (2 秒) は説明文どおり (録画の I では付かなかったが、ユーザーの判断で付ける)
 * Telebow      : しゃがんで撃つとテレポートの矢 (90 / 45 / 20 秒に 1 回、矢を当てるごとに -3 秒)。
 *                飛んでいる間は mobSpell の跡、着いたところへテレポートして mob.endermen.portal (0.4 / 1.75)
 * Sprint Drain : 矢を当てると Speed、II / III は相手に Slowness I (3 秒)
 * Faster than their shadow / Pullbow / Explosive : 録画 2026-10-14_18-32-08 / 18-32-44 / 18-34-09.mcrc (各メソッドの説明)
 * Devil Chicks : 矢が当たった場所にニワトリ 1 / 2 / 3 羽、1 秒後に爆発 (半径 3・1❤。数値は推測)。
 *                出すぎないよう、Volley の 2 本目以降からは出ず、1 人が出しているニワトリは同時に 3 羽まで
 * Bullet Time  : 剣でブロック中 (ブロックヒット中も) に当たった矢を消す (II / III は 1❤ / 1.5❤ 回復)。
 *                矢の場所に煙、頭の上に happyVillager 1 個、mob.wither.shoot (0.3 / 1.84)
 * </pre>
 */
public final class BowEffects implements Listener {

    private static final String TELEBOW_METADATA = "pitTelebow";
    private static final String CHICK_METADATA = "pitDevilChick";
    private static final long CHICK_FUSE_TICKS = 20;
    private static final double CHICK_RADIUS = 3;
    private static final double CHICK_DAMAGE = 2;
    /** Volley の矢の間隔 (tick)。 */
    private static final int VOLLEY_INTERVAL_TICKS = 1;
    private static final String VOLLEY_EXTRA_METADATA = "pitVolleyExtra";
    /** 1 人が同時に出せる Devil Chicks のニワトリの数。 */
    private static final int MAX_CHICKS = 3;
    private static final long MEGA_LONGBOW_COOLDOWN_MILLIS = 1_000;
    /** 録画の Mega Longbow の矢の速さ (毎回 2.900)。 */
    private static final double MEGA_LONGBOW_SPEED = 2.9;
    private static final long[] TELEBOW_COOLDOWN_MILLIS = {90_000, 45_000, 20_000};
    private static final long TELEBOW_HIT_REDUCTION_MILLIS = 3_000;

    private final PitPlugin plugin;
    private final Map<UUID, Long> megaLongbowUsed = new HashMap<>();
    /** 剣で最後に右クリックした時刻 (ブロックヒット中の Bullet Time 用)。 */
    private final Map<UUID, Long> blockStarted = new HashMap<>();
    private static final long BLOCK_GRACE_MILLIS = 250;
    /** Telebow が次に使える時刻。 */
    private final Map<UUID, Long> telebowReady = new HashMap<>();
    private static final String HIT_METADATA = "pitArrowHitPlayer";
    /** Faster than their shadow: 撃った人 → 外さずに当てた数。 */
    private final Map<UUID, Integer> shadowHits = new HashMap<>();
    /** Pullbow: (I は 撃った人:相手、II / III は 撃った人) → 最後に引き寄せた時刻。 */
    private final Map<String, Long> pullbowUsed = new HashMap<>();
    private static final long PULLBOW_COOLDOWN_MILLIS = 8_000;
    private static final double PULLBOW_PER_LEVEL = 0.06;
    private static final double PULLBOW_UP = 0.4;
    private static final double PULLBOW_NEARBY = 4;
    private final Map<UUID, Long> explosiveUsed = new HashMap<>();
    private static final double EXPLOSIVE_KNOCKBACK = 0.96;
    private static final double EXPLOSIVE_UP = 0.45;
    private static final double EXPLOSIVE_POP_RADIUS = 3;
    private static final double EXPLOSIVE_BOOM_RADIUS = 5;
    /** 撃った人 → 今いる Devil Chicks のニワトリの数。 */
    private final Map<UUID, Integer> aliveChicks = new HashMap<>();

    public BowEffects(PitPlugin plugin) {
        this.plugin = plugin;
    }

    private static Map<String, Integer> bowEnchants(ItemStack bow) {
        if (MysticType.of(bow) != MysticType.BOW || !MysticItems.isMystic(bow)) {
            return java.util.Collections.emptyMap();
        }
        return MysticData.read(bow).getEnchants();
    }

    // ---------------------------------------------------------------- Mega Longbow / Volley / Telebow

    /** スポーンの中では矢を撃てない (ユーザーの指定)。 */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onShootInSpawn(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player && plugin.getPitSpawn().isProtected(event.getEntity().getLocation())) {
            event.setCancelled(true);
            ((Player) event.getEntity()).updateInventory();
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getProjectile() instanceof Arrow)) {
            return;
        }
        Player shooter = (Player) event.getEntity();
        Arrow arrow = (Arrow) event.getProjectile();
        // Somber を履いていると Mystic の弓のエンチャント (Mega Longbow / Volley / Telebow …) は効かない
        if (MysticAccess.isSuppressed(shooter) || MysticAccess.mysticItemsBlocked(shooter)) {
            return;
        }
        Map<String, Integer> enchants = bowEnchants(event.getBow());
        if (enchants.containsKey("instant_shot")) {
            // Mega Longbow: 少しでも引いて離せば、最大まで引いた矢 (録画: 速さ 2.900・クリティカル) が出る。1 秒に 1 回
            long now = System.currentTimeMillis();
            Long last = megaLongbowUsed.get(shooter.getUniqueId());
            if (last != null && now - last < MEGA_LONGBOW_COOLDOWN_MILLIS) {
                event.setCancelled(true);
                return;
            }
            megaLongbowUsed.put(shooter.getUniqueId(), now);
            Vector velocity = arrow.getVelocity();
            if (velocity.lengthSquared() > 0) {
                arrow.setVelocity(velocity.normalize().multiply(MEGA_LONGBOW_SPEED));
            }
            arrow.setCritical(true);
            // 説明文どおり Jump Boost II / III / IV (2 秒)。録画の I では付いていなかったが、レベルで違うのはここだけなので付ける
            int level = Math.min(enchants.get("instant_shot"), 3);
            shooter.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 40, level), true);
        }
        Integer telebow = enchants.get("telebow");
        if (telebow != null && shooter.isSneaking()) {
            long now = System.currentTimeMillis();
            Long ready = telebowReady.get(shooter.getUniqueId());
            if (ready == null || now >= ready) {
                telebowReady.put(shooter.getUniqueId(), now + TELEBOW_COOLDOWN_MILLIS[Math.min(telebow, 3) - 1]);
                arrow.setMetadata(TELEBOW_METADATA, new FixedMetadataValue(plugin, shooter.getUniqueId()));
                trail(arrow);
                return;
            }
            shooter.sendMessage("§cTelebow is on cooldown! §7(" + (int) Math.ceil((ready - now) / 1000.0) + "s)");
        }
        Integer volley = enchants.get("volley");
        if (volley != null) {
            int extra = Math.min(volley, 3) + 1;
            ItemStack bow = event.getBow();
            // 1 本目と同じ速さ・クリティカル (Mega Longbow で変えた後の値)
            double speed = arrow.getVelocity().length();
            boolean critical = arrow.isCritical();
            for (int i = 1; i <= extra; i++) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (!shooter.isOnline() || shooter.isDead()) {
                        return;
                    }
                    // その時に向いている方向へ (バニラと同じ少しのばらつき)
                    Arrow next = shooter.launchProjectile(Arrow.class);
                    Vector velocity = next.getVelocity();
                    if (velocity.lengthSquared() > 0) {
                        next.setVelocity(velocity.normalize().multiply(speed));
                    }
                    next.setCritical(critical);
                    next.setMetadata(VOLLEY_EXTRA_METADATA, new FixedMetadataValue(plugin, true));
                    plugin.getEnchantEffects().tagArrow(next, bowEnchants(bow));
                    float pitch = (43 + ThreadLocalRandom.current().nextInt(16)) / 63f;
                    EnchantEffects.worldSound(shooter.getLocation(), "random.bow", 1.0f, pitch);
                }, (long) i * VOLLEY_INTERVAL_TICKS);
            }
        }
    }

    /** Telebow の矢の跡 (録画: 1 tick ごとに mobSpell 2 個、散らばり 1、速さ 1。ユーザーの指定で矢に寄せて散らばり 0.15)。 */
    private static final float TRAIL_SPREAD = 0.15f;

    private void trail(Arrow arrow) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!arrow.isValid() || arrow.isOnGround()) {
                    cancel();
                    return;
                }
                Location at = arrow.getLocation();
                PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(EnumParticle.SPELL_MOB, true,
                        (float) at.getX(), (float) at.getY(), (float) at.getZ(), TRAIL_SPREAD, TRAIL_SPREAD, TRAIL_SPREAD, 1, 2);
                for (Player viewer : at.getWorld().getPlayers()) {
                    if (viewer.getLocation().distanceSquared(at) <= 64 * 64) {
                        PlayerPackets.send(viewer, packet);
                    }
                }
            }
        }.runTaskTimer(plugin, 1, 1);
    }

    @EventHandler
    public void onLand(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Arrow && event.getEntity().getShooter() instanceof Player) {
            Arrow landed = (Arrow) event.getEntity();
            Player owner = (Player) landed.getShooter();
            if (plugin.getPitSpawn().isProtected(landed.getLocation())) {
                // スポーンの中に撃っても Explosive / Devil Chicks / Telebow は何も起きない
                if (landed.hasMetadata(TELEBOW_METADATA)) {
                    landed.remove();
                }
                return;
            }
            Map<String, Integer> landedEnchants = plugin.getEnchantEffects().arrowEnchants(landed);
            Integer explosive = landedEnchants.get("explosive_bow");
            if (explosive != null) {
                explosive(landed, owner, explosive);
            }
            if (landedEnchants.containsKey("bow_combo_speed")) {
                // 当たった矢はこの後のダメージのイベントで印が付く。付かなければ外れ → 連続が途切れる
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (!landed.hasMetadata(HIT_METADATA)) {
                        shadowHits.remove(owner.getUniqueId());
                    }
                });
            }
            Integer chicks = landedEnchants.get("explosive_chickens");
            if (chicks != null && !event.getEntity().hasMetadata(VOLLEY_EXTRA_METADATA)) {
                devilChicks((Arrow) event.getEntity(), (Player) event.getEntity().getShooter(), chicks);
            }
        }
        if (!(event.getEntity() instanceof Arrow) || !event.getEntity().hasMetadata(TELEBOW_METADATA)) {
            return;
        }
        Arrow arrow = (Arrow) event.getEntity();
        if (!(arrow.getShooter() instanceof Player)) {
            return;
        }
        Player shooter = (Player) arrow.getShooter();
        Location at = arrow.getLocation();
        arrow.remove();
        if (!shooter.isOnline() || shooter.isDead()) {
            return;
        }
        at.setYaw(shooter.getLocation().getYaw());
        at.setPitch(shooter.getLocation().getPitch());
        shooter.setFallDistance(0);
        shooter.teleport(at);
        EnchantEffects.worldSound(at, "mob.endermen.portal", 0.4f, 110 / 63f);
    }

    // ---------------------------------------------------------------- Devil Chicks!

    /**
     * Devil Chicks!: 矢が当たった (刺さった) 場所にニワトリが 1 / 2 / 3 羽出て、少しして爆発する。
     * 録画が無く wiki にも数値が無いので推測: 1 秒後から 0.2 秒おきに爆発、半径 3 ブロックの撃った人以外に 1❤ (防具で減る) と吹き飛ばし。
     */
    private void devilChicks(Arrow arrow, Player shooter, int level) {
        UUID owner = shooter.getUniqueId();
        int alive = aliveChicks.containsKey(owner) ? aliveChicks.get(owner) : 0;
        int count = Math.min(Math.min(level, 3), MAX_CHICKS - alive);
        if (count <= 0) {
            return;
        }
        aliveChicks.put(owner, alive + count);
        Location at = arrow.getLocation().add(0, 0.3, 0);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            org.bukkit.entity.Chicken chicken = at.getWorld().spawn(at, org.bukkit.entity.Chicken.class);
            chicken.setMetadata(CHICK_METADATA, new FixedMetadataValue(plugin, shooter.getUniqueId()));
            chicken.setVelocity(new Vector(random.nextDouble(-0.25, 0.25), 0.35, random.nextDouble(-0.25, 0.25)));
            EnchantEffects.worldSound(at, "mob.chicken.say", 1.0f, 1.0f);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> explodeChick(chicken, shooter), CHICK_FUSE_TICKS + 4L * i);
        }
    }

    private void explodeChick(org.bukkit.entity.Chicken chicken, Player shooter) {
        aliveChicks.computeIfPresent(shooter.getUniqueId(), (id, n) -> n <= 1 ? null : n - 1);
        if (!chicken.isValid()) {
            return;
        }
        Location at = chicken.getLocation();
        PacketPlayOutWorldParticles boom = new PacketPlayOutWorldParticles(EnumParticle.EXPLOSION_LARGE, true,
                (float) at.getX(), (float) at.getY() + 0.3f, (float) at.getZ(), 0, 0, 0, 0, 1);
        for (Player viewer : at.getWorld().getPlayers()) {
            if (viewer.getLocation().distanceSquared(at) <= 64 * 64) {
                PlayerPackets.send(viewer, boom);
            }
        }
        EnchantEffects.worldSound(at, "random.explode", 0.6f, 1.4f);
        for (org.bukkit.entity.Entity entity : chicken.getNearbyEntities(CHICK_RADIUS, CHICK_RADIUS, CHICK_RADIUS)) {
            if (!(entity instanceof Player) || entity.equals(shooter) || ((Player) entity).isDead()
                    || entity.getLocation().distanceSquared(at) > CHICK_RADIUS * CHICK_RADIUS
                    || plugin.getPitSpawn().isProtected(entity.getLocation())) {
                continue;
            }
            Player target = (Player) entity;
            target.damage(CHICK_DAMAGE, chicken);
            Vector push = target.getLocation().toVector().subtract(at.toVector()).setY(0);
            if (push.lengthSquared() > 0) {
                push.normalize().multiply(0.5);
            }
            target.setVelocity(target.getVelocity().add(push.setY(0.35)));
        }
        chicken.remove();
    }

    /** Devil Chicks のニワトリは攻撃されない。 */
    @EventHandler(ignoreCancelled = true)
    public void onChickDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event.getEntity().hasMetadata(CHICK_METADATA)) {
            event.setCancelled(true);
        }
    }

    // ---------------------------------------------------------------- 当たったとき

    /** Bullet Time (ダメージの計算より前に矢を消す)。 */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onArrowHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Arrow)) {
            return;
        }
        Player victim = (Player) event.getEntity();
        Arrow arrow = (Arrow) event.getDamager();
        ItemStack held = victim.getItemInHand();
        Player arrowOwner = arrow.getShooter() instanceof Player ? (Player) arrow.getShooter() : null;
        if (isBlocking(victim) && MysticType.of(held) == MysticType.SWORD && MysticItems.isMystic(held)
                && !MysticAccess.disabled(victim, arrowOwner) && !MysticAccess.mysticItemsBlocked(victim)) {
            Integer bulletTime = MysticData.read(held).getEnchants().get("blocking_cancels_projectiles");
            if (bulletTime != null) {
                event.setCancelled(true);
                // 消した矢の場所に煙 (ユーザーの指定)
                Location at = arrow.getLocation();
                PacketPlayOutWorldParticles smoke = new PacketPlayOutWorldParticles(EnumParticle.SMOKE_NORMAL, true,
                        (float) at.getX(), (float) at.getY(), (float) at.getZ(), 0.15f, 0.15f, 0.15f, 0.02f, 12);
                for (Player viewer : at.getWorld().getPlayers()) {
                    if (viewer.getLocation().distanceSquared(at) <= 64 * 64) {
                        PlayerPackets.send(viewer, smoke);
                    }
                }
                arrow.remove();
                if (bulletTime >= 2) {
                    victim.setHealth(Math.min(victim.getMaxHealth(), victim.getHealth() + (bulletTime >= 3 ? 3 : 2)));
                }
                Location head = victim.getLocation().add(0, 2.3, 0);
                PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(EnumParticle.VILLAGER_HAPPY, true,
                        (float) head.getX(), (float) head.getY(), (float) head.getZ(), 0, 0, 0, 1, 1);
                for (Player viewer : head.getWorld().getPlayers()) {
                    if (viewer.getLocation().distanceSquared(head) <= 64 * 64) {
                        PlayerPackets.send(viewer, packet);
                    }
                }
                EnchantEffects.worldSound(victim.getLocation(), "mob.wither.shoot", 0.3f, 116 / 63f);
                return;
            }
        }
        if (!(arrow.getShooter() instanceof Player)) {
            return;
        }
        Player shooter = (Player) arrow.getShooter();
        if (MysticAccess.disabled(shooter, victim)) {
            return;
        }
        // Telebow: 矢を当てるごとに -3 秒
        Long ready = telebowReady.get(shooter.getUniqueId());
        if (ready != null) {
            telebowReady.put(shooter.getUniqueId(), ready - TELEBOW_HIT_REDUCTION_MILLIS);
        }
        arrow.setMetadata(HIT_METADATA, new FixedMetadataValue(plugin, true));
        Map<String, Integer> hitEnchants = plugin.getEnchantEffects().arrowEnchants(arrow);
        Integer shadow = hitEnchants.get("bow_combo_speed");
        if (shadow != null) {
            fasterThanTheirShadow(shooter, shadow);
        }
        Integer pullbow = hitEnchants.get("pullbow");
        if (pullbow != null && !victim.equals(shooter)) {
            pullbow(shooter, victim, pullbow);
        }
        Integer sprintDrain = hitEnchants.get("bow_slow");
        if (sprintDrain != null) {
            int[] seconds = {5, 5, 7};
            shooter.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, seconds[Math.min(sprintDrain, 3) - 1] * 20,
                    sprintDrain >= 3 ? 1 : 0), true);
            if (sprintDrain >= 2) {
                victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 0), true);
            }
        }
    }

    /**
     * ガード中か。ブロックヒット (殴る瞬間にガードが外れる) でも Bullet Time が効くよう、
     * 剣で右クリックしてから BLOCK_GRACE_MILLIS の間はガード中とみなす。
     */
    private boolean isBlocking(Player player) {
        if (player.isBlocking()) {
            return true;
        }
        Long last = blockStarted.get(player.getUniqueId());
        return last != null && System.currentTimeMillis() - last <= BLOCK_GRACE_MILLIS;
    }

    @EventHandler
    public void onRightClick(org.bukkit.event.player.PlayerInteractEvent event) {
        org.bukkit.event.block.Action action = event.getAction();
        if ((action == org.bukkit.event.block.Action.RIGHT_CLICK_AIR || action == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)
                && event.getItem() != null && event.getItem().getType().name().endsWith("_SWORD")) {
            blockStarted.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
        }
    }

    // ---------------------------------------------------------------- Faster than their shadow / Pullbow / Explosive

    /**
     * Faster than their shadow (録画 2026-10-14_18-32-08.mcrc の III): 外さずに 3 / 2 / 2 本当てると
     * 自分に Speed II / III / IV (4 秒)、mob.bat.takeoff (2.0 / 2.0)。外すと数え直し。
     */
    private void fasterThanTheirShadow(Player shooter, int level) {
        int needed = level == 1 ? 3 : 2;
        int hits = shadowHits.merge(shooter.getUniqueId(), 1, Integer::sum);
        if (hits >= needed) {
            shadowHits.remove(shooter.getUniqueId());
            shooter.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 80, Math.min(level, 3)), true);
            EnchantEffects.worldSound(shooter.getLocation(), "mob.bat.takeoff", 2.0f, 126 / 63f);
        }
    }

    /**
     * Pullbow (録画 2026-10-14_18-32-44.mcrc): 当てた相手を自分の方へ引き寄せる (普通のノックバックの代わり)。
     * 横の速さ = (自分 - 相手) × 0.06 / 0.12 / 0.18 (Lv)、上 0.4。相手の場所で mob.bat.takeoff (1.0 / 1.22)。
     * I は相手ごとに 8 秒、II / III は 8 秒に 1 回で近くのプレイヤーも引き寄せる (近くの範囲は推測)。
     */
    private void pullbow(Player shooter, Player victim, int level) {
        long now = System.currentTimeMillis();
        String key = level == 1 ? shooter.getUniqueId() + ":" + victim.getUniqueId() : shooter.getUniqueId().toString();
        Long last = pullbowUsed.get(key);
        if (last != null && now - last < PULLBOW_COOLDOWN_MILLIS) {
            return;
        }
        pullbowUsed.put(key, now);
        double strength = PULLBOW_PER_LEVEL * Math.min(level, 3);
        java.util.List<Player> pulled = new java.util.ArrayList<>();
        pulled.add(victim);
        if (level >= 2) {
            for (org.bukkit.entity.Entity near : victim.getNearbyEntities(PULLBOW_NEARBY, PULLBOW_NEARBY, PULLBOW_NEARBY)) {
                if (near instanceof Player && !near.equals(shooter) && !plugin.getPitSpawn().isProtected(near.getLocation())) {
                    pulled.add((Player) near);
                }
            }
        }
        // 普通のノックバックはこのイベントの後に付くので、次の tick に上書きする
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (Player target : pulled) {
                if (!target.isValid() || target.isDead()) {
                    continue;
                }
                Vector pull = shooter.getLocation().toVector().subtract(target.getLocation().toVector()).multiply(strength);
                target.setVelocity(new Vector(pull.getX(), PULLBOW_UP, pull.getZ()));
                EnchantEffects.worldSound(target.getLocation(), "mob.bat.takeoff", 1.0f, 77 / 63f);
            }
        });
    }

    /**
     * Explosive (録画 2026-10-14_18-34-09.mcrc): 矢が着いたところで爆発する (ダメージなし、5 / 3 / 5 秒に 1 回)。
     * II / III は hugeexplosion と random.explode (0.75 / 1.0)、I (POP!) は largeexplode と (0.75 / 2.0)。
     * 近くの撃った人以外を横 0.96・上 0.45 で吹き飛ばす (録画ではどのレベルでも同じ。範囲は推測)。
     */
    private void explosive(Arrow arrow, Player shooter, int level) {
        long now = System.currentTimeMillis();
        Long last = explosiveUsed.get(shooter.getUniqueId());
        long cooldown = level == 2 ? 3_000 : 5_000;
        if (last != null && now - last < cooldown) {
            return;
        }
        explosiveUsed.put(shooter.getUniqueId(), now);
        Location at = arrow.getLocation();
        boolean pop = level == 1;
        PacketPlayOutWorldParticles boom = new PacketPlayOutWorldParticles(pop ? EnumParticle.EXPLOSION_LARGE : EnumParticle.EXPLOSION_HUGE,
                true, (float) at.getX(), (float) at.getY(), (float) at.getZ(), 0, 0, 0, 1, 1);
        for (Player viewer : at.getWorld().getPlayers()) {
            if (viewer.getLocation().distanceSquared(at) <= 64 * 64) {
                PlayerPackets.send(viewer, boom);
            }
        }
        EnchantEffects.worldSound(at, "random.explode", 0.75f, pop ? 2.0f : 1.0f);
        double radius = pop ? EXPLOSIVE_POP_RADIUS : EXPLOSIVE_BOOM_RADIUS;
        java.util.List<Player> targets = new java.util.ArrayList<>();
        for (org.bukkit.entity.Entity near : arrow.getNearbyEntities(radius, radius, radius)) {
            if (near instanceof Player && !near.equals(shooter) && near.getLocation().distanceSquared(at) <= radius * radius
                    && !plugin.getPitSpawn().isProtected(near.getLocation())) {
                targets.add((Player) near);
            }
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (Player target : targets) {
                if (!target.isValid() || target.isDead()) {
                    continue;
                }
                Vector away = target.getLocation().toVector().subtract(at.toVector()).setY(0);
                if (away.lengthSquared() < 1e-4) {
                    away = new Vector(ThreadLocalRandom.current().nextDouble(-1, 1), 0, ThreadLocalRandom.current().nextDouble(-1, 1));
                }
                away.normalize().multiply(EXPLOSIVE_KNOCKBACK);
                target.setVelocity(new Vector(away.getX(), EXPLOSIVE_UP, away.getZ()));
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        shadowHits.remove(event.getPlayer().getUniqueId());
        explosiveUsed.remove(event.getPlayer().getUniqueId());
        megaLongbowUsed.remove(event.getPlayer().getUniqueId());
        blockStarted.remove(event.getPlayer().getUniqueId());
    }
}
