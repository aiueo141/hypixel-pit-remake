package com.pitremake.combat;

import com.pitremake.PitPlugin;
import com.pitremake.enchant.PitEnchant;
import com.pitremake.item.MysticData;
import com.pitremake.item.MysticItems;
import com.pitremake.item.MysticType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import com.pitremake.stats.PrestigeRules;
import com.pitremake.util.PlayerPackets;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ミスティックエンチャントの戦闘の効果。数値は説明文から読む ({@link EnchantNumbers})。
 * <pre>
 * 剣 (持っている人)  : Sharp / Beat the Spammers / Bounty Reaper / Diamond Stomp / Fancy Raider / King Buster / Punisher /
 *                     Pain Focus / Shark / Combo: Damage / Grasshopper / Gold and Boosted / Revengeance / Billionaire /
 *                     Berserker / Lifesteal / Executioner / Gamble! / Combo: Perun's Wrath / Healer /
 *                     Combo: Swift / Combo: Heal / Combo: Stun / Crush / Hemorrhage
 *                     (音・表示は録画 Attacker.mcrc / victim.mcrc: Gamble = note.pling、Billionaire = random.orb、
 *                      Executioner = EXECUTED! と mob.villager.death、Combo = mob.guardian.hit)
 * 弓・ブロック        : Volley / Mega Longbow / Telebow / Sprint Drain / Bullet Time は {@link BowEffects}
 * 弓 (撃った弓)      : Fletching / First Shot / Jumpspammer / Sniper / Spammer and Proud / Arrow Armory / Chipping /
 *                     True Shot / Lucky Shot / Mixed Combat
 * パンツ (受ける人)  : Protection / Ring Armor / Diamond Allergy / David and Goliath / Billy / Cricket / McSwimmer /
 *                     "Not" Gladiator / Solitude / Critically Funky / Mirror / Singularity / Prick
 * パンツ (攻撃する人): Regularity (最終ダメージが少ないと 0.1 秒後にもう一度殴る)
 * 報酬の防具         : Guardian (被ダメージ -10%)
 * </pre>
 * 増加・減少はすべて {@link DamageContext} で掛け算される。
 */
public final class EnchantEffects implements DamageModifier, Listener {

    private static final String BOW_METADATA = "pitBowEnchants";
    private static final String ARMORY_METADATA = "pitArrowArmory";
    private static final double LOW_HP_SHARK = 12;
    private static final double SHARK_RANGE = 12;
    private static final double SOLITUDE_RANGE = 7;
    private static final double GLADIATOR_RANGE = 12;
    private static final int GLADIATOR_MAX = 10;
    private static final double SNIPER_DISTANCE = 24;
    private static final double SPAMMER_DISTANCE = 8;
    private static final int PERUN_EVERY = 5;
    private static final double LIFESTEAL_MAX = 3;
    private static final double EXECUTE_BELOW = 2;
    private static final long HEALER_COOLDOWN_MILLIS = 1_000;
    /**
     * 録画では追撃が最初の一撃のちょうど 0.100 秒 (2 tick) 後 (2026-10-14_15-05-51.mcrc: 1.717 → 1.818)。
     * 攻撃のパケットは tick の終わりに処理され、予約したタスクは tick の始めに動くので、2 tick 後に予約すると
     * 実際には約 1 tick (0.05 秒) 後になって、2 回の被弾の音とアニメーションが重なって 1 回に聞こえる。
     * そのため 3 tick 後に予約して、実際の間隔を 2 tick にする。
     */
    private static final int REGULARITY_DELAY_TICKS = 3;
    /** Combo: Stun は同じ相手に 8 秒に 1 回 (I / II)。 */
    private static final long STUN_COOLDOWN_MILLIS = 8_000;
    private static final long CRUSH_COOLDOWN_MILLIS = 2_000;
    private static final Pattern COST = Pattern.compile("cost (\\d+)g");

    private final PitPlugin plugin;
    /** 近接で殴った回数 (コンボ用)。 */
    private final Map<UUID, Integer> strikes = new HashMap<>();
    /** 最後に殴った時刻 (コンボは前の一撃から 3.5 秒以内でないと途切れる)。 */
    private final Map<UUID, Long> strikeTimes = new HashMap<>();
    private final Map<UUID, Integer> venomStrikes = new HashMap<>();
    private final Map<UUID, Long> venomTimes = new HashMap<>();
    /** コンボが途切れるまでの時間 (ユーザーの指定: 3.5 秒)。 */
    private static final long COMBO_TIMEOUT_MILLIS = 3_500;
    /** Combo: Venom で毒にした相手 / 自分のエンチャントが止まる時間。 */
    private static final long VENOM_VICTIM_MILLIS = 12_000;
    private static final long VENOM_SELF_MILLIS = 24_000;
    /** First Shot: 撃った人 → この人生で矢を当てた相手。 */
    private final Map<UUID, Set<UUID>> shotTargets = new HashMap<>();
    /** Mixed Combat: 撃った人 → 次の近接が強くなる相手と増加量。 */
    private final Map<UUID, Map<UUID, Double>> mixedCombat = new HashMap<>();
    /** Critically Funky II / III: 次の攻撃の増加量。 */
    private final Map<UUID, Double> funkyEmpower = new HashMap<>();
    /** Revengeance: 最後に自分を倒した人。 */
    private final Map<UUID, UUID> lastKiller = new HashMap<>();
    private final Map<UUID, Long> healerUsed = new HashMap<>();
    private final Map<UUID, Long> stunned = new HashMap<>();
    private final Map<UUID, Long> crushUsed = new HashMap<>();
    private final Map<UUID, Long> bleedUsed = new HashMap<>();

    public EnchantEffects(PitPlugin plugin) {
        this.plugin = plugin;
    }

    private PitEnchant enchant(String key) {
        return plugin.getEnchantRegistry().get(key);
    }

    /** 死んだとき (HP 0): First Shot・Mixed Combat を消し、Revengeance の相手を覚える。 */
    public void onDeath(Player victim, Player killer) {
        UUID id = victim.getUniqueId();
        shotTargets.remove(id);
        mixedCombat.remove(id);
        for (Set<UUID> targets : shotTargets.values()) {
            targets.remove(id);
        }
        strikes.remove(id);
        strikeTimes.remove(id);
        venomStrikes.remove(id);
        venomTimes.remove(id);
        MysticAccess.clear(victim);
        if (killer != null && !killer.equals(victim)) {
            lastKiller.put(id, killer.getUniqueId());
        }
    }

    /** 撃った弓のエンチャントを矢に覚えさせる (矢が当たったときに使う)。Arrow Armory は矢を 2 本多く使う。 */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getProjectile() instanceof Arrow)
                || MysticType.of(event.getBow()) != MysticType.BOW || !MysticItems.isMystic(event.getBow())) {
            return;
        }
        Player shooter = (Player) event.getEntity();
        Map<String, Integer> enchants = MysticData.read(event.getBow()).getEnchants();
        if (enchants.isEmpty() || MysticAccess.isSuppressed(shooter)) {
            return;
        }
        event.getProjectile().setMetadata(BOW_METADATA, new FixedMetadataValue(plugin, new HashMap<>(enchants)));
        if (enchants.containsKey("damage_per_arrow") && shooter.getInventory().containsAtLeast(new ItemStack(Material.ARROW), 2)) {
            shooter.getInventory().removeItem(new ItemStack(Material.ARROW, 2));
            event.getProjectile().setMetadata(ARMORY_METADATA, new FixedMetadataValue(plugin, true));
        }
    }

    @Override
    public void modify(DamageContext context) {
        Player victim = context.victim;
        // Somber / Venom: エンチャントが効かない側はここで止める (MysticAccess)
        boolean victimOff = MysticAccess.disabled(victim, context.byPlayer() ? context.attacker : null);
        Map<String, Integer> pants = victimOff ? Collections.<String, Integer>emptyMap() : pantsEnchants(victim);
        // Guardian / Protection はどのダメージにも効く
        if (!victimOff && hasEnchant(victim.getInventory().getChestplate(), "guardian")) {
            context.reduce(0.10);
        }
        context.reduce(percent(pants, "damage_reduction", 0));
        Integer mirror = pants.get("immune_true_damage");
        if (mirror != null) {
            // Mirror: 相手からの確定ダメージ (Gamble / Perun / Chipping / True Shot など) を受けない。II / III は 25% / 50% を跳ね返す。
            // 自分の Gamble の跳ね返り・メガストリークの代償は防げない (別の経路)
            context.immuneToTrueDamage();
            double reflect = EnchantNumbers.percent(enchant("immune_true_damage"), mirror, 0);
            if (reflect > 0 && context.byPlayer()) {
                Player source = context.attacker;
                context.afterHit(dealt -> trueDamage(source, context.getBlockedTrueDamage() * reflect));
            }
        }
        if (pants.containsKey("singularity")) {
            context.capFinalDamage(EnchantNumbers.hearts(enchant("singularity"), pants.get("singularity"), 0));
        }
        if (!context.byPlayer()) {
            return;
        }
        Player attacker = context.attacker;
        defense(context, pants, attacker);
        // Combo: Venom は自分が Venom の毒で止まっている間も発動できる (ユーザーの指定) ので、止める判定より前
        if (context.melee && !context.isExtraStrike()) {
            venomCombo(attacker, victim);
        }
        if (MysticAccess.disabled(attacker, victim)) {
            return;
        }
        if (context.melee) {
            melee(context, attacker);
        } else if (context.arrow != null) {
            arrow(context, attacker);
        }
    }

    private void defense(DamageContext context, Map<String, Integer> pants, Player attacker) {
        Player victim = context.victim;
        if (context.arrow != null) {
            context.reduce(percent(pants, "less_damage_from_arrows", 0));
        }
        if (context.melee && attacker.getItemInHand() != null && attacker.getItemInHand().getType().name().startsWith("DIAMOND_")) {
            context.reduce(percent(pants, "less_damage_vs_diamond_weapons", 0));
        }
        if (plugin.getBountyManager().get(attacker.getUniqueId()) > 0) {
            context.reduce(percent(pants, "less_damage_vs_bounties", 0));
        }
        context.reduce(percent(pants, "less_damage_when_high_bounty", 0) * (plugin.getBountyManager().get(victim.getUniqueId()) / 1000));
        if (onGrass(victim) || onGrass(attacker)) {
            context.reduce(percent(pants, "less_damage_on_grass", 0));
        }
        if (context.melee && victim.getLocation().getBlock().isLiquid()) {
            context.reduce(percent(pants, "less_damage_when_swimming", 0));
        }
        context.reduce(percent(pants, "less_damage_nearby_players", 0) * Math.min(GLADIATOR_MAX, nearbyPlayers(victim, GLADIATOR_RANGE)));
        Integer solitude = pants.get("solitude");
        if (solitude != null) {
            // Lv I: 近くに他のプレイヤーが 1 人以下、Lv II / III: 2 人以下
            int allowed = solitude == 1 ? 1 : 2;
            if (nearbyPlayers(victim, SOLITUDE_RANGE) <= allowed) {
                context.reduce(EnchantNumbers.percent(enchant("solitude"), solitude, 0));
            }
        }
        Integer funky = pants.get("power_against_crits");
        if (funky != null && context.critical) {
            // 「クリティカルは 80% のダメージ」→ -20%
            context.reduce(1 - EnchantNumbers.percent(enchant("power_against_crits"), funky, 0));
            double empower = EnchantNumbers.percent(enchant("power_against_crits"), funky, 1);
            if (empower > 0) {
                funkyEmpower.put(victim.getUniqueId(), empower);
            }
        }
        Integer prick = pants.get("thorns");
        if (prick != null) {
            double damage = EnchantNumbers.hearts(enchant("thorns"), prick, 0);
            context.afterHit(dealt -> enemyTrueDamage(attacker, victim, damage));
        }
        // New Deal II / III: 受けるダメージ -4% / -8%
        context.reduce(percent(pants, "new_deal", 0));
    }

    private void melee(DamageContext context, Player attacker) {
        Player victim = context.victim;
        Double empowered = funkyEmpower.remove(attacker.getUniqueId());
        if (empowered != null) {
            context.increase(empowered);
        }
        Map<UUID, Double> mixed = mixedCombat.get(attacker.getUniqueId());
        if (mixed != null && mixed.containsKey(victim.getUniqueId())) {
            context.increase(mixed.remove(victim.getUniqueId()));
        }
        Map<String, Integer> pants = pantsEnchants(attacker);
        Integer regularity = pants.get("regularity");
        if (regularity != null && !context.isExtraStrike()) {
            double below = EnchantNumbers.hearts(enchant("regularity"), regularity, 0);
            double share = EnchantNumbers.percent(enchant("regularity"), regularity, 0);
            double base = context.baseDamage;
            context.afterHit(finalDamage -> {
                double firstMultiplier = context.multiplier();
                double firstFlat = context.flat();
                // 衝撃吸収で受けた分は数えず、本当に減った体力で判定する (finalDamage は衝撃吸収の分を引いた後の値)。
                // 同じ一撃の確定ダメージ (Gamble など) も足して判定する
                // (録画 victim.mcrc: Gamble で相手に 3❤ 入った一撃 (1.65 + 6) には追撃が無く、自分に来た一撃には追撃があった)
                double dealt = finalDamage + context.getDealtTrueDamage();
                plugin.getDamageCalculator().debug(attacker, victim, String.format(java.util.Locale.US,
                        "Regularity: health %.3f (absorbed %.2f)%s %s %.3f → %s", finalDamage,
                        context.event.isApplicable(org.bukkit.event.entity.EntityDamageEvent.DamageModifier.ABSORPTION)
                                ? -context.event.getDamage(org.bukkit.event.entity.EntityDamageEvent.DamageModifier.ABSORPTION) : 0,
                        context.getDealtTrueDamage() > 0 ? String.format(java.util.Locale.US, " + true %.2f = %.3f",
                                context.getDealtTrueDamage(), dealt) : "",
                        dealt < below ? "<" : "≥", below,
                        dealt < below ? "strike again in " + REGULARITY_DELAY_TICKS + " ticks (base "
                                + String.format(java.util.Locale.US, "%.3f ×%.3f", base, context.multiplier() * (1 + share)) + ")" : "no strike"));
                if (dealt < below) {
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (attacker.isOnline() && !victim.isDead() && victim.isValid()) {
                            // 追撃は最初の一撃と同じ攻撃力・同じ倍率に × 1.75 (録画: どの相手でもちょうど 1.75 倍)
                            plugin.getDamageCalculator().extraStrike(attacker, victim, base, firstMultiplier * (1 + share), firstFlat);
                        }
                    }, REGULARITY_DELAY_TICKS);
                }
            });
        }

        ItemStack held = attacker.getItemInHand();
        if (MysticType.of(held) != MysticType.SWORD || !MysticItems.isMystic(held)) {
            return;
        }
        Map<String, Integer> sword = MysticData.read(held).getEnchants();
        int strike = context.isExtraStrike() ? 0 : nextCombo(strikes, strikeTimes, attacker.getUniqueId());

        context.increase(percent(sword, "plain_melee_damage", 0));
        if (victim.getItemInHand() != null && victim.getItemInHand().getType() == Material.BOW) {
            context.increase(percent(sword, "melee_damage_vs_bows", 0));
        }
        if (plugin.getBountyManager().get(victim.getUniqueId()) > 0) {
            context.increase(percent(sword, "melee_damage_vs_bounties", 0));
        }
        if (wears(victim, "DIAMOND_")) {
            context.increase(percent(sword, "melee_damage_vs_diamond", 0));
        }
        if (wears(victim, "LEATHER_")) {
            context.increase(percent(sword, "melee_damage_vs_leather", 0));
        }
        if (victim.getHealth() > victim.getMaxHealth() / 2) {
            context.increase(percent(sword, "melee_damage_vs_high_hp", 0));
        } else {
            context.increase(percent(sword, "melee_damage_vs_low_hp", 0));
        }
        double missingHearts = (attacker.getMaxHealth() - attacker.getHealth()) / 2;
        context.increase(percent(sword, "melee_damage_when_low", 0) * missingHearts);
        context.increase(percent(sword, "melee_damage_when_close_low_players", 0) * lowPlayersNear(attacker));
        Integer combo = sword.get("melee_combo_damage");
        if (combo != null && strike > 0 && strike % (combo == 1 ? 4 : 3) == 0) {
            context.increase(EnchantNumbers.percent(enchant("melee_combo_damage"), combo, 0));
            comboSound(attacker);
        }
        combos(context, attacker, sword, strike);
        if (onGrass(attacker) || onGrass(victim)) {
            context.increase(percent(sword, "melee_damage_when_on_grass", 0));
        }
        if (((CraftPlayer) attacker).getHandle().getAbsorptionHearts() > 0) {
            context.increase(percent(sword, "melee_damage_when_absorption", 0));
        }
        if (victim.getUniqueId().equals(lastKiller.get(attacker.getUniqueId()))) {
            context.increase(percent(sword, "melee_avenge", 0));
        }
        Integer billionaire = sword.get("melee_literally_p2w");
        if (billionaire != null && !pantsEnchants(victim).containsKey("new_deal")) {
            // New Deal を着ている相手には効かない
            PitEnchant enchant = enchant("melee_literally_p2w");
            // "+33.3% のダメージだが 100g かかる" (古い説明は "1.33x"): 払えたときだけ増える (Regularity の追撃は払わずに同じ増加)
            boolean extra = context.isExtraStrike();
            if (extra || plugin.getGoldManager().take(attacker.getUniqueId(), cost(enchant, billionaire))) {
                double percent = EnchantNumbers.percent(enchant, billionaire, 0);
                context.increase(percent > 0 ? percent : EnchantNumbers.times(enchant, billionaire) - 1);
                if (!extra) {
                    // 録画: 払った一撃ごとに random.orb (1.0 / 0.71) が殴った人に
                    attacker.playSound(attacker.getLocation(), "random.orb", 1.0f, 45 / 63f);
                }
            }
        }
        Integer berserker = sword.get("melee_crit_midair");
        if (berserker != null && !context.critical
                && ThreadLocalRandom.current().nextDouble() < EnchantNumbers.percent(enchant("melee_crit_midair"), berserker, 0)) {
            context.increase(EnchantNumbers.percent(enchant("melee_crit_midair"), berserker, 1));
        }
        Integer lifesteal = sword.get("melee_heal_on_hit");
        if (lifesteal != null) {
            double share = EnchantNumbers.percent(enchant("melee_heal_on_hit"), lifesteal, 0);
            // 同じ一撃で相手に入った確定ダメージ (Gamble など) も回復の元に入れる
            context.afterHit(dealt -> heal(attacker, Math.min(LIFESTEAL_MAX, (dealt + context.getDealtTrueDamage()) * share)));
        }
        Integer executioner = sword.get("melee_execute");
        if (executioner != null) {
            double below = EnchantNumbers.hearts(enchant("melee_execute"), executioner, 0);
            double threshold = below > 0 ? below : EXECUTE_BELOW;
            context.afterHit(dealt -> {
                double left = victim.getHealth() - dealt;
                if (left > 0 && left < threshold) {
                    context.event.setDamage(context.event.getDamage() + victim.getHealth() * 100);
                    // 録画 (victim.mcrc): 殴られた人へのチャットと、その場所で mob.villager.death (1.0 / 0.49)
                    victim.sendMessage("§c§lEXECUTED!§7 by " + plugin.getRankManager().nameColor(attacker.getUniqueId())
                            + attacker.getName() + "§7 (insta-kill below §c" + String.format(java.util.Locale.US, "%.1f", threshold / 2) + "❤§7)");
                    // 録画は 1.0 だが大きすぎるのでユーザーの指定で下げる
                    worldSound(victim.getLocation(), "mob.villager.death", 0.4f, 31 / 63f);
                }
            });
        }
        // Gamble は Regularity の追撃でも発動する (録画: 追撃でも 3❤ の確定ダメージが入った)
        Integer gamble = sword.get("melee_gamble");
        if (gamble != null) {
            double damage = EnchantNumbers.hearts(enchant("melee_gamble"), gamble, 0);
            if (ThreadLocalRandom.current().nextBoolean()) {
                context.addTrueDamage(damage);
                // 録画: 相手に入ったときは note.pling (1.0 / 2.0)、自分に来たときは (1.0 / 1.49)、殴った人にだけ
                attacker.playSound(attacker.getLocation(), "note.pling", 1.0f, 126 / 63f);
            } else {
                attacker.playSound(attacker.getLocation(), "note.pling", 1.0f, 94 / 63f);
                context.afterHit(dealt -> trueDamage(attacker, damage));
            }
        }
        if (context.isExtraStrike()) {
            // 追撃では Perun / Healer は発動しない
            return;
        }
        Integer perun = sword.get("melee_lightning");
        if (perun != null && strike > 0 && strike % (perun == 1 ? PERUN_EVERY : PERUN_EVERY - 1) == 0) {
            // III は 1❤ + 相手のダイヤの防具 1 つにつき 1❤ (録画: ダイヤ 3 つの相手に 4❤)
            double hearts = EnchantNumbers.hearts(enchant("melee_lightning"), perun, 0);
            if (perun >= 3) {
                hearts += 2 * diamondPieces(victim);
            }
            context.addTrueDamage(hearts);
            victim.getWorld().strikeLightningEffect(victim.getLocation());
        }
        Integer healer = sword.get("melee_healer");
        if (healer != null) {
            long now = System.currentTimeMillis();
            Long last = healerUsed.get(attacker.getUniqueId());
            if (last == null || now - last >= HEALER_COOLDOWN_MILLIS) {
                healerUsed.put(attacker.getUniqueId(), now);
                PitEnchant enchant = enchant("melee_healer");
                heal(attacker, EnchantNumbers.hearts(enchant, healer, 0));
                context.afterHit(dealt -> heal(victim, EnchantNumbers.hearts(enchant, healer, 1)));
            }
        }
    }

    /** 撃った弓のエンチャントを矢に覚えさせる (Volley の 2 本目以降など)。 */
    public void tagArrow(Arrow arrow, Map<String, Integer> enchants) {
        if (!enchants.isEmpty()) {
            arrow.setMetadata(BOW_METADATA, new FixedMetadataValue(plugin, new HashMap<>(enchants)));
        }
    }

    /** 矢を撃った弓のエンチャント (無ければ空)。 */
    @SuppressWarnings("unchecked")
    public Map<String, Integer> arrowEnchants(Arrow arrow) {
        for (MetadataValue value : arrow.getMetadata(BOW_METADATA)) {
            if (value.getOwningPlugin() == plugin) {
                return (Map<String, Integer>) value.value();
            }
        }
        return Collections.emptyMap();
    }

    private void arrow(DamageContext context, Player shooter) {
        Arrow arrow = context.arrow;
        Map<String, Integer> bow = arrowEnchants(arrow);
        if (bow.isEmpty()) {
            return;
        }
        Player victim = context.victim;
        double distance = shooter.getWorld().equals(victim.getWorld()) ? shooter.getLocation().distance(victim.getLocation()) : 0;
        context.increase(percent(bow, "bow_damage", 0));
        Set<UUID> targets = shotTargets.computeIfAbsent(shooter.getUniqueId(), id -> new HashSet<>());
        if (targets.add(victim.getUniqueId())) {
            context.increase(percent(bow, "first_shot", 0));
        }
        if (!((Entity) shooter).isOnGround()) {
            context.increase(percent(bow, "jump_spammer", 0));
        }
        if (distance > SNIPER_DISTANCE) {
            context.increase(percent(bow, "sniper", 0));
        }
        if (distance < SPAMMER_DISTANCE) {
            context.increase(percent(bow, "bow_spammer", 0));
        }
        if (arrow.hasMetadata(ARMORY_METADATA)) {
            context.increase(percent(bow, "damage_per_arrow", 0));
        }
        Integer chipping = bow.get("arrow_true_damage");
        if (chipping != null) {
            context.addTrueDamage(EnchantNumbers.hearts(enchant("arrow_true_damage"), chipping, 0));
        }
        Integer trueShot = bow.get("bow_to_true_damage");
        if (trueShot != null) {
            // 矢のダメージの 25% を防具を無視する確定ダメージにする
            double share = EnchantNumbers.percent(enchant("bow_to_true_damage"), trueShot, 0);
            context.reduce(share);
            // II / III は "35% + 0.25❤" のように固定の分も足す
            context.addTrueDamage(context.baseDamage * share + EnchantNumbers.hearts(enchant("bow_to_true_damage"), trueShot, 0));
        }
        Integer lucky = bow.get("lucky_shot");
        if (lucky != null && ThreadLocalRandom.current().nextDouble() < EnchantNumbers.percent(enchant("lucky_shot"), lucky, 0)) {
            // 4 倍のダメージ = +300%
            context.increase(3);
            shooter.sendMessage("§e§lLUCKY SHOT! §7Quadruple damage!");
        }
        Integer mixed = bow.get("mixed_combat");
        if (mixed != null) {
            mixedCombat.computeIfAbsent(shooter.getUniqueId(), id -> new HashMap<>())
                    .put(victim.getUniqueId(), EnchantNumbers.percent(enchant("mixed_combat"), mixed, 0));
        }
    }

    private double percent(Map<String, Integer> enchants, String key, int index) {
        Integer level = enchants.get(key);
        return level == null ? 0 : EnchantNumbers.percent(enchant(key), level, index);
    }

    private static Map<String, Integer> pantsEnchants(Player player) {
        if (MysticAccess.isSuppressed(player)) {
            return Collections.emptyMap();
        }
        return MysticAccess.pantsEnchants(player);
    }

    private static boolean hasEnchant(ItemStack item, String key) {
        return item != null && item.getType() != Material.AIR && MysticData.read(item).getEnchantLevel(key) > 0;
    }

    private static boolean wears(Player player, String materialPrefix) {
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && armor.getType().name().startsWith(materialPrefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean onGrass(Player player) {
        Block below = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
        return below.getType() == Material.GRASS;
    }

    /** 近く (range ブロック以内) にいる自分以外のプレイヤー (Bot も含む) の数。 */
    private static int nearbyPlayers(Player player, double range) {
        int count = 0;
        for (Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof Player && entity.getLocation().distanceSquared(player.getLocation()) <= range * range) {
                // Sybil を履いた人は何人分にも数える
                count += MysticAccess.playerWeight((Player) entity);
            }
        }
        return count;
    }

    /** Shark: 近く (12 ブロック) にいる体力 6❤ 未満の他のプレイヤーの数。 */
    private static int lowPlayersNear(Player player) {
        int count = 0;
        for (Entity entity : player.getNearbyEntities(SHARK_RANGE, SHARK_RANGE, SHARK_RANGE)) {
            if (entity instanceof Player && ((Player) entity).getHealth() < LOW_HP_SHARK) {
                count += MysticAccess.playerWeight((Player) entity);
            }
        }
        return count;
    }

    private static double cost(PitEnchant enchant, int level) {
        Matcher matcher = COST.matcher(org.bukkit.ChatColor.stripColor(String.join(" ", enchant.getDescription(level))));
        return matcher.find() ? Double.parseDouble(matcher.group(1)) : 0;
    }

    /**
     * Combo: Swift / Combo: Heal / Combo: Stun / Crush / Hemorrhage (録画 Attacker.mcrc / victim.mcrc の音・表示)。
     * <pre>
     * Combo の発動     : 殴った人にだけ mob.guardian.hit (0.8 / 2.0)、効果ごとに 1 回
     * Combo: Stun     : 殴られた人に STUNNED! / You cannot move!、その場所で random.anvil_land (0.5 / 0.095)
     * Hemorrhage      : 殴られた人に HEMORRHAGE! のチャット、その場所で game.neutral.hurt.fall.big (0.8 / 0.49)
     * </pre>
     */
    private void combos(DamageContext context, Player attacker, Map<String, Integer> sword, int strike) {
        Player victim = context.victim;
        Integer swift = sword.get("melee_combo_speed");
        if (swift != null && strike > 0 && strike % (swift == 1 ? 4 : 3) == 0) {
            int[] seconds = {3, 4, 5};
            attacker.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, seconds[Math.min(swift, 3) - 1] * 20,
                    swift == 1 ? 0 : 1), true);
            comboSound(attacker);
        }
        Integer heal = sword.get("melee_combo_heal");
        if (heal != null && strike > 0 && strike % 4 == 0) {
            double hearts = EnchantNumbers.hearts(enchant("melee_combo_heal"), heal, 0);
            heal(attacker, hearts);
            // 衝撃吸収は足すのではなく、発動するたびに説明文の値 (例: 0.8❤) にする。
            // 多く付いていても (Respawn: Absorption の 15❤ など) この値で上書きされる (ユーザーの指定)
            ((CraftPlayer) attacker).getHandle().setAbsorptionHearts(
                    (float) EnchantNumbers.hearts(enchant("melee_combo_heal"), heal, 1));
            comboSound(attacker);
        }
        Integer stun = sword.get("melee_stun");
        if (stun != null && strike > 0 && strike % 5 == 0) {
            long now = System.currentTimeMillis();
            Long last = stunned.get(victim.getUniqueId());
            if (stun >= 3 || last == null || now - last >= STUN_COOLDOWN_MILLIS) {
                stunned.put(victim.getUniqueId(), now);
                int[] ticks = {10, 16, 30};
                int duration = ticks[Math.min(stun, 3) - 1];
                victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, 7), true);
                victim.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, duration, 128), true);
                PlayerPackets.title(victim, "§6§c§lSTUNNED!", "§6§eYou cannot move!", 0, duration, 5);
                worldSound(victim.getLocation(), "random.anvil_land", 0.5f, 6 / 63f);
            }
        }
        Integer crush = sword.get("melee_weakness");
        if (crush != null) {
            long now = System.currentTimeMillis();
            Long last = crushUsed.get(attacker.getUniqueId());
            if (last == null || now - last >= CRUSH_COOLDOWN_MILLIS) {
                crushUsed.put(attacker.getUniqueId(), now);
                int[] ticks = {4, 8, 10};
                victim.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, ticks[Math.min(crush, 3) - 1],
                        3 + Math.min(crush, 3)), true);
            }
        }
        Integer bleed = sword.get("melee_bleed");
        if (bleed != null) {
            long now = System.currentTimeMillis();
            long[] cooldown = {6_000, 4_000, 2_000};
            Long last = bleedUsed.get(attacker.getUniqueId());
            if (last == null || now - last >= cooldown[Math.min(bleed, 3) - 1]) {
                bleedUsed.put(attacker.getUniqueId(), now);
                int[] seconds = {4, 4, 5};
                bleed(attacker, victim, seconds[Math.min(bleed, 3) - 1] * 20);
            }
        }
    }

    /** Hemorrhage: 鈍足 I と、その間は衝撃吸収 ❤ が付かない。 */
    private void bleed(Player attacker, Player victim, int ticks) {
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, ticks, 0), true);
        victim.sendMessage("§c§lHEMORRHAGE! §7No absorption + slow because of "
                + PrestigeRules.levelTag(plugin.prestigeOf(attacker), plugin.levelOf(attacker)) + " §7" + attacker.getName());
        worldSound(victim.getLocation(), "game.neutral.hurt.fall.big", 0.8f, 31 / 63f);
        new BukkitRunnable() {
            private int left = ticks;

            @Override
            public void run() {
                if (left-- <= 0 || !victim.isOnline() || victim.isDead()) {
                    cancel();
                    return;
                }
                ((CraftPlayer) victim).getHandle().setAbsorptionHearts(0);
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    /** コンボの何回目か (前の一撃から 3.5 秒より空くと 1 から数え直し)。 */
    private static int nextCombo(Map<UUID, Integer> counts, Map<UUID, Long> times, UUID id) {
        long now = System.currentTimeMillis();
        Long last = times.put(id, now);
        if (last == null || now - last > COMBO_TIMEOUT_MILLIS) {
            counts.remove(id);
        }
        return counts.merge(id, 1, Integer::sum);
    }

    /** Combo: Venom のコンボを数える (Venom の毒で自分のエンチャントが止まっていても数える。相手の Somber には止められる)。 */
    private void venomCombo(Player attacker, Player victim) {
        if (!MysticAccess.pantsEnchants(attacker).containsKey("venom")) {
            return;
        }
        if (MysticAccess.wearsSomber(victim) && !MysticAccess.wearsArmageddon(attacker)) {
            return;
        }
        if (nextCombo(venomStrikes, venomTimes, attacker.getUniqueId()) % 3 == 0) {
            venom(attacker, victim);
        }
    }

    /**
     * Combo: Venom: 3 回目ごとに相手を毒 (見た目だけ、ダメージなし) にして 12 秒エンチャントを止め、
     * 自分も毒で 24 秒エンチャントが止まる (ユーザーの指定)。
     */
    private void venom(Player attacker, Player victim) {
        // 既に Venom されている相手の時間は延ばさない (ユーザーの指定)
        if (!MysticAccess.isSuppressed(victim)) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, (int) (VENOM_VICTIM_MILLIS / 50), 0), true);
            MysticAccess.suppress(victim, VENOM_VICTIM_MILLIS);
        }
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.POISON, (int) (VENOM_SELF_MILLIS / 50), 0), true);
        MysticAccess.suppress(attacker, VENOM_SELF_MILLIS);
        comboSound(attacker);
    }

    /** Venom の毒は見た目だけ (毒のダメージは受けない)。 */
    @EventHandler(ignoreCancelled = true)
    public void onPoison(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.POISON) {
            event.setCancelled(true);
        }
    }

    private static void comboSound(Player attacker) {
        attacker.playSound(attacker.getLocation(), "mob.guardian.hit", 0.8f, 126 / 63f);
    }

    /** 近くのプレイヤー全員に聞こえる音。 */
    static void worldSound(org.bukkit.Location at, String sound, float volume, float pitch) {
        for (Player player : at.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(at) <= 32 * 32) {
                player.playSound(at, sound, volume, pitch);
            }
        }
    }

    private static int diamondPieces(Player player) {
        int count = 0;
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && armor.getType().name().startsWith("DIAMOND_")) {
                count++;
            }
        }
        return count;
    }

    private static void heal(Player player, double amount) {
        if (amount > 0 && !player.isDead()) {
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + amount));
        }
    }

    /** 相手から受ける確定ダメージ (Prick など)。Mirror を着ていれば受けず、II / III は source に跳ね返す。 */
    private void enemyTrueDamage(Player target, Player source, double amount) {
        Integer mirror = pantsEnchants(target).get("immune_true_damage");
        if (mirror == null) {
            trueDamage(target, amount);
            return;
        }
        double reflect = EnchantNumbers.percent(enchant("immune_true_damage"), mirror, 0);
        if (reflect > 0 && source != null && !source.equals(target)) {
            trueDamage(source, amount * reflect);
        }
    }

    /** 防具を無視するダメージ (倒れるときは通常のダメージとして与え、HP 0 の処理に任せる)。 */
    private static void trueDamage(Player player, double amount) {
        if (amount <= 0 || player.isDead()) {
            return;
        }
        if (player.getHealth() - amount > 0) {
            player.setHealth(player.getHealth() - amount);
        } else {
            // 倒れるときは本当に死なせず (アイテムが落ちる) 、HP 0 と同じ扱い (少し前に殴っていた人のキル) にする
            com.pitremake.PitPlugin plugin = com.pitremake.PitPlugin.getInstance();
            if (plugin != null) {
                plugin.getKillListener().oof(player);
            }
        }
    }
}
