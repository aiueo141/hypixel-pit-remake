package com.pitremake.combat;

import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityStatus;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityVelocity;
import net.minecraft.server.v1_8_R3.WorldServer;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleConsumer;

/**
 * プレイヤーが受けるダメージをまとめて計算する。
 * <pre>
 * 1. バニラの攻撃力 x0 (クリティカル 1.5 倍込み)
 * 2. x1 = x0 × E × L。E = max(0, 1 + 増加 - 減少) (Passive・パーク・Renown の +X% と Mystic の増減)、
 *    L = アカウントの補正・Gladiator・Marathon・Tough Skin・メガストリークなどの掛け算
 * 3. 剣でガード中は (1 + x1) / 2 → 防具 (Pit のパンツは Dark も含めて鉄相当 {@code ArmorListener}) → 耐性
 * 4. 1 回の上限 (Singularity)、確定ダメージ (Mirror / Hermit で無効)
 * </pre>
 * 式は逆算レポート「Hypixel Pit ダメージ計算ロジック」(-1 版、録画 2 本・近接 17,724 件) のもの。
 * 録画 (2026-10-14_09-32-22.mcrc など) の受けたダメージは、鉄の剣 7・ダイヤの剣 8 に防具 1 ポイント 4% 減で、
 * そこに少しの倍率 (Passive など) が掛かった値だった。
 */
public final class DamageCalculator implements Listener {

    private final List<DamageModifier> modifiers = new ArrayList<>();
    /** 今 Regularity の追撃中の攻撃者。 */
    private final Set<UUID> extraStrikes = new HashSet<>();
    /** 追撃の倍率と足す攻撃力 (攻撃者 → {倍率, 足す攻撃力})。 */
    private final java.util.Map<UUID, double[]> extraStrikeMultiplier = new java.util.HashMap<>();

    /** 最後のダメージが決まった後に攻撃を止められるもの (Escape Pod / Phoenix)。 */
    public interface HitInterceptor {
        /** true ならこの攻撃を無かったことにする。healthLeft は攻撃の後の体力。 */
        boolean intercept(DamageContext context, double healthLeft);
    }

    private HitInterceptor interceptor;

    public void setInterceptor(HitInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    /** その人が今 Regularity の追撃を当てているところか。 */
    public boolean isExtraStriking(UUID attacker) {
        return extraStrikes.contains(attacker);
    }

    public void register(DamageModifier modifier) {
        modifiers.add(modifier);
    }

    /**
     * Regularity の追撃 (この攻撃はコンボに数えず、もう一度は追撃しない)。
     * 追撃を通すために無敵時間を 0 にするが、終わったら最初の一撃のときの無敵時間と「最後のダメージ」に戻す
     * (戻さないと追撃で無敵時間が 20 tick に延び、次の普通の一撃が無敵時間に当たって通らない)。
     */
    /**
     * multiplier / flat: 追撃に使う倍率と、攻撃力に足す量 (EnchantEffects の Regularity が計算する)。
     * 追撃 = s × (x0 × E + F) × E × 防具 (ユーザーの調査 regularity.html: 録画 536 組)。
     */
    public void extraStrike(Player attacker, Player victim, double damage, double multiplier, double flat) {
        extraStrikeMultiplier.put(attacker.getUniqueId(), new double[]{multiplier, flat});
        int noDamageTicks = victim.getNoDamageTicks();
        double lastDamage = victim.getLastDamage();
        double healthBefore = victim.getHealth();
        EntityPlayer handle = ((CraftPlayer) victim).getHandle();
        float absorptionBefore = handle.getAbsorptionHearts();
        double motX = handle.motX;
        double motY = handle.motY;
        double motZ = handle.motZ;
        extraStrikes.add(attacker.getUniqueId());
        try {
            victim.setNoDamageTicks(0);
            victim.damage(damage, attacker);
            if (victim.getHealth() < healthBefore || handle.getAbsorptionHearts() < absorptionBefore) {
                showAsHit(handle, motX, motY, motZ);
            }
            debug(attacker, victim, String.format(java.util.Locale.US,
                    "§d[Regularity] §7%s HP %.2f → %.2f (absorption %.2f → %.2f, i-frames %d)", victim.getName(),
                    healthBefore, victim.getHealth(), absorptionBefore, handle.getAbsorptionHearts(), noDamageTicks));
        } finally {
            extraStrikes.remove(attacker.getUniqueId());
            extraStrikeMultiplier.remove(attacker.getUniqueId());
            if (!victim.isDead()) {
                victim.setNoDamageTicks(noDamageTicks);
                victim.setLastDamage(lastDamage);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player victim = (Player) event.getEntity();
        Player attacker = null;
        Arrow arrow = null;
        boolean melee = false;
        if (event instanceof EntityDamageByEntityEvent) {
            Entity damager = ((EntityDamageByEntityEvent) event).getDamager();
            if (damager instanceof Player) {
                attacker = (Player) damager;
                melee = event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK;
            } else if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof Player) {
                attacker = (Player) ((Projectile) damager).getShooter();
                arrow = damager instanceof Arrow ? (Arrow) damager : null;
            }
        }
        boolean extra = attacker != null && extraStrikes.contains(attacker.getUniqueId());
        DamageContext context = new DamageContext(event, victim, attacker, melee, arrow,
                melee && !extra && isCritical(attacker), extra);
        if (extra) {
            double[] forced = extraStrikeMultiplier.get(attacker.getUniqueId());
            if (forced != null) {
                context.forcedMultiplier = forced[0];
                context.forcedFlat = forced[1];
            }
        }
        for (DamageModifier modifier : modifiers) {
            modifier.modify(context);
        }
        apply(context);
    }

    /**
     * 追撃を普通に殴られたように見せる: 普通の攻撃 (EntityHuman.attack) と同じく、ノックバックの速度をすぐ送って
     * サーバー側の速度を戻し、被弾のアニメーション (Entity Status 2) を本人と周りに送る
     * (録画 victim.mcrc でも追撃のたびに速度のパケットが来ていた)。
     */
    private static void showAsHit(EntityPlayer handle, double motX, double motY, double motZ) {
        WorldServer world = (WorldServer) handle.world;
        if (handle.velocityChanged) {
            world.getTracker().sendPacketToEntity(handle, new PacketPlayOutEntityVelocity(handle));
            handle.velocityChanged = false;
            if (handle.playerConnection != null) {
                // 本物のプレイヤーは動きをクライアントが決めるので、普通の攻撃と同じくサーバー側の速度は元に戻す
                handle.motX = motX;
                handle.motY = motY;
                handle.motZ = motZ;
            }
        }
        world.getTracker().sendPacketToEntity(handle, new PacketPlayOutEntityStatus(handle, (byte) 2));
        // 録画 (2026-10-14_15-05-51.mcrc): 追撃でも game.player.hurt (1.0、音程はランダム) が鳴る → 「タタン」
        org.bukkit.Location at = handle.getBukkitEntity().getLocation();
        float pitch = (float) ((ThreadLocalRandom.current().nextDouble() - ThreadLocalRandom.current().nextDouble()) * 0.2 + 1.0);
        for (Player viewer : at.getWorld().getPlayers()) {
            if (viewer.getLocation().distanceSquared(at) <= 16 * 16) {
                viewer.playSound(at, "game.player.hurt", 1.0f, pitch);
            }
        }
    }

    /** /pit debug: 自分が与えた・受けたダメージの計算をチャットに出す人。 */
    private final Set<UUID> debugging = new HashSet<>();

    /** デバッグ表示を切り替える。オンになったら true。 */
    public boolean toggleDebug(Player player) {
        if (debugging.remove(player.getUniqueId())) {
            return false;
        }
        debugging.add(player.getUniqueId());
        return true;
    }

    /** 攻撃した人・攻撃された人のどちらかがデバッグ表示中なら、その人たちにメッセージを出す。 */
    public void debug(Player attacker, Player victim, String message) {
        for (Player player : new Player[]{attacker, victim}) {
            if (player != null && debugging.contains(player.getUniqueId())) {
                player.sendMessage("§8[DMG] §7" + message);
            }
        }
    }

    private void apply(DamageContext context) {
        EntityDamageEvent event = context.event;
        double multiplier = context.multiplier();
        double rawBefore = event.getDamage();
        if (multiplier != 1 || context.flat() != 0) {
            double base = event.getDamage();
            double target = Math.max(0, base + context.flat()) * multiplier;
            if (base > 0 && context.victim.isBlocking() && event.isApplicable(EntityDamageEvent.DamageModifier.BLOCKING)) {
                guard(event, context.victim, base, target);
            } else {
                scale(event, context.victim, base > 0 ? target / base : 0);
            }
            if (base <= 0) {
                event.setDamage(EntityDamageEvent.DamageModifier.BASE, target);
            }
        }
        if (context.kill) {
            event.setDamage(event.getDamage() + context.victim.getHealth() * 100);
            return;
        }
        double finalDamage = event.getFinalDamage();
        if (context.flatReduction > 0 && finalDamage > 0) {
            double reduced = Math.max(0, finalDamage - context.flatReduction);
            scale(event, context.victim, reduced / finalDamage);
            finalDamage = event.getFinalDamage();
        }
        if (finalDamage > context.maxFinalDamage && finalDamage > 0) {
            scale(event, context.victim, context.maxFinalDamage / finalDamage);
            finalDamage = event.getFinalDamage();
        }
        if (context.trueDamageImmune) {
            context.blockedTrueDamage = context.trueDamage;
        }
        double trueDamage = (context.trueDamageImmune ? 0 : context.trueDamage) + context.penaltyTrueDamage;
        // 落下など自分で減らした体力でも Escape Pod / Phoenix は発動するので、プレイヤー以外のダメージでも呼ぶ
        if (interceptor != null && interceptor.intercept(context, context.victim.getHealth() - finalDamage - trueDamage)) {
            event.setCancelled(true);
            return;
        }
        if (trueDamage > 0) {
            context.dealtTrueDamage = trueDamage;
            Player victim = context.victim;
            if (victim.getHealth() - finalDamage - trueDamage > 0) {
                victim.setHealth(victim.getHealth() - trueDamage);
            } else {
                // 確定ダメージで倒れる場合も、キルが攻撃した人のものになるよう同じ攻撃に足す
                event.setDamage(event.getDamage() + trueDamage * 100);
            }
        }
        if (context.attacker != null && (debugging.contains(context.victim.getUniqueId())
                || debugging.contains(context.attacker.getUniqueId()))) {
            debug(context.attacker, context.victim, String.format(java.util.Locale.US,
                    "%s%s→%s base %.3f ×%.3f (%s: +%.2f / -%.2f, L ×%.2f) = %.3f → final %.3f%s",
                    context.isExtraStrike() ? "§d[Regularity] §7" : "", context.attacker.getName(), context.victim.getName(),
                    rawBefore, multiplier, DamageContext.getStacking().name().toLowerCase(), context.increaseSum,
                    context.reductionSum, context.outerMultiplier(), event.getDamage(), finalDamage,
                    (context.dealtTrueDamage > 0 ? String.format(java.util.Locale.US, " +true %.2f", context.dealtTrueDamage) : "")
                            + (context.blockedTrueDamage > 0 ? String.format(java.util.Locale.US, " (blocked true %.2f)", context.blockedTrueDamage) : "")));
        }
        for (DoubleConsumer action : context.afterHit) {
            action.accept(finalDamage);
        }
    }

    /**
     * ダメージを丸ごと scale 倍にする。防具・耐性・エンチャントの軽減も同じ割合で縮める
     * (setDamage(double) は軽減を差で動かすので、ArmorListener の Fresh Pants の補正などが倍率と一緒に増えず、
     * 大きい一撃ほど防具が弱くなっていた)。衝撃吸収は持っている量で付け直す。
     */
    private static void scale(EntityDamageEvent event, Player victim, double scale) {
        if (scale == 1) {
            return;
        }
        double beforeAbsorption = 0;
        for (EntityDamageEvent.DamageModifier modifier : EntityDamageEvent.DamageModifier.values()) {
            if (modifier == EntityDamageEvent.DamageModifier.ABSORPTION || !event.isApplicable(modifier)) {
                continue;
            }
            double value = event.getDamage(modifier) * scale;
            event.setDamage(modifier, value);
            beforeAbsorption += value;
        }
        fitAbsorption(event, victim, beforeAbsorption);
    }

    /**
     * 剣でガード中の倍率: ガード (1 + x) / 2 は Pit の倍率を掛けた後の攻撃力 x に掛かる
     * (逆算レポート-1: C4talak → LaKbraa は (1 + 7 × 0.9381) / 2 × 0.56 = 2.119)。
     * 全部を同じ割合で縮める {@link #scale} だと (1 + x0) / 2 × 倍率 になってしまうので、ガードを付け直して
     * 防具・耐性はガードの後の値に比例させる。
     */
    private static void guard(EntityDamageEvent event, Player victim, double base, double target) {
        EntityDamageEvent.DamageModifier hardHat = EntityDamageEvent.DamageModifier.HARD_HAT;
        EntityDamageEvent.DamageModifier blocking = EntityDamageEvent.DamageModifier.BLOCKING;
        double oldHardHat = event.isApplicable(hardHat) ? event.getDamage(hardHat) : 0;
        double before = base + oldHardHat + event.getDamage(blocking);
        double newHardHat = oldHardHat * target / base;
        double incoming = target + newHardHat;
        double guarded = incoming > 0 ? (1 + incoming) * 0.5 : incoming;
        event.setDamage(EntityDamageEvent.DamageModifier.BASE, target);
        if (event.isApplicable(hardHat)) {
            event.setDamage(hardHat, newHardHat);
        }
        event.setDamage(blocking, guarded - incoming);
        double ratio = before > 0 ? guarded / before : 0;
        double beforeAbsorption = guarded;
        for (EntityDamageEvent.DamageModifier modifier : new EntityDamageEvent.DamageModifier[]{
                EntityDamageEvent.DamageModifier.ARMOR, EntityDamageEvent.DamageModifier.RESISTANCE,
                EntityDamageEvent.DamageModifier.MAGIC}) {
            if (event.isApplicable(modifier)) {
                double value = event.getDamage(modifier) * ratio;
                event.setDamage(modifier, value);
                beforeAbsorption += value;
            }
        }
        fitAbsorption(event, victim, beforeAbsorption);
    }

    /** 衝撃吸収は持っている量で付け直す。 */
    private static void fitAbsorption(EntityDamageEvent event, Player victim, double beforeAbsorption) {
        if (event.isApplicable(EntityDamageEvent.DamageModifier.ABSORPTION)) {
            double absorption = ((CraftPlayer) victim).getHandle().getAbsorptionHearts();
            event.setDamage(EntityDamageEvent.DamageModifier.ABSORPTION, -Math.min(absorption, Math.max(0, beforeAbsorption)));
        }
    }

    /** 1.8 のクリティカル: 落下中 (地面にいない)・水やはしごの中でない・盲目でない・乗り物に乗っていない。 */
    public static boolean isCritical(Player attacker) {
        return attacker.getFallDistance() > 0 && !attacker.isOnGround() && !attacker.getLocation().getBlock().isLiquid()
                && attacker.getLocation().getBlock().getType() != Material.LADDER
                && attacker.getLocation().getBlock().getType() != Material.VINE
                && !attacker.hasPotionEffect(PotionEffectType.BLINDNESS) && attacker.getVehicle() == null;
    }
}
