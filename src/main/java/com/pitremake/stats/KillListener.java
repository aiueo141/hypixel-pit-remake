package com.pitremake.stats;

import com.pitremake.PitPlugin;
import com.pitremake.bot.BotManager;
import com.pitremake.gold.GoldManager;
import com.pitremake.item.MysticData;
import com.pitremake.item.MysticItems;
import com.pitremake.perk.PerkManager;
import com.pitremake.perk.PerkRules;
import com.pitremake.renown.RenownUpgrade;
import com.pitremake.util.PlayerPackets;
import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * キル・アシスト・死亡・レベルアップの演出 (録画 Assist Kill Bounty Sound logs.mcrc / Death.mcrc と同じ文字・音)。
 * <pre>
 * (レベルが上がったとき)
 *   チャット  §b§lPIT LEVEL UP! §7[§77§7] §7➟ §7[§78§7]
 *   タイトル  §b§lLEVEL UP!  /  §7[§77§7] §7➟ §7[§78§7]
 *   音       random.levelup 1.0 / 1.0
 * キル        §a§lKILL! §7on [86] PeeNut_ §b+9XP §6+14.00g
 *             前のキルから 3 秒以内なら DOUBLE KILL! / TRIPLE KILL! / QUADRA KILL! / PENTA KILL! §7(5) / MULTI KILL! §7(6)…
 * アクションバー §6[§4§l86§6] §7PeeNut_ §a§lKILL!
 * 音         random.orb を 2 tick ごとに 5 回 (音程 113 / 115 / 118 / 120 / 123)
 * アシスト    §a§lASSIST! §725% on [86] PeeNut_ §b+2XP §6+3.50g   (音なし。XP が 0 なら XP は書かない)
 *             もらえるのは (与えたダメージの割合) × (自分がキルしていたらもらえた XP とゴールド)
 * 死亡        §c§lDEATH! §7by [85] Matidaberek2 §e§lVIEW RECAP   (キルした人がいなければ "by" は無し)
 *             タイトル §cYOU DIED! (5 / 20 / 5 tick)、game.neutral.hurt (0.5) → 2 tick 後に mob.zombie.infect (0.5 / 1.7)
 * 全体チャット
 *   §c§lSTREAK! §7of §c40 §7kills by [107] 名前                 (5 キルと 10 キルごと)
 *   §6§lBOUNTY! §7of §6§l100g §7on [119] 名前 §7for high streak   (バウンティが初めて付いた)
 *   §6§lBOUNTY §7bump §6§l100g §7on [16] 名前 §7for high streak   (上がった。本人に mob.wither.spawn 0.65 / 1.79)
 *   §6§lBOUNTY CLAIMED! [85] キルした人 §7killed [16] 相手 §7for §6§l100g
 * </pre>
 * バニラの経験値バーには Pit のレベルと次のレベルまでの進み具合を出す。
 */
public final class KillListener implements Listener {

    private static final String KILL_SOUND = "random.orb";
    /** キルの音の音程 (パケットの値)。2 tick ごとに鳴らす。 */
    private static final int[] KILL_PITCHES = {113, 115, 118, 120, 123};
    private static final int KILL_SOUND_INTERVAL = 2;
    private static final float ASSIST_VOLUME = 0.6f;
    /** Mystic のアイテムが落ちたときの水しぶきの時間 (約 0.7 秒)。 */
    private static final int MYSTIC_SPLASH_TICKS = 14;
    private static final String DROP_OWNER = "pitDropOwner";
    private static final String LEVEL_UP_SOUND = "random.levelup";
    private static final String DEATH_SOUND = "game.neutral.hurt";
    private static final String RESPAWN_SOUND = "mob.zombie.infect";
    private static final String BOUNTY_BUMP_SOUND = "mob.wither.spawn";
    private static final int TITLE_FADE_IN = 20;
    private static final int TITLE_STAY = 20;
    private static final int TITLE_FADE_OUT = 20;
    /** 全体へ STREAK! を流すキル数: 5 と、10 キルごと (録画では 5 / 10 / 20 / 40 / 50 / 180)。 */
    private static final int STREAK_BROADCAST_FIRST = 5;
    private static final int STREAK_BROADCAST_EVERY = 10;
    /** 前のキルからこの時間内なら MULTI KILL (録画では 2.95 秒は続き、3.22 秒は切れた)。 */
    static final long MULTI_KILL_MILLIS = 3_000;
    /** この時間より前のダメージはアシストに数えない (推測)。 */
    private static final long ASSIST_MILLIS = 30_000;

    /** 奈落などで死んだとき、この時間内に殴っていた人のキルにする。 */
    private static final long RECENT_ATTACK_MILLIS = 10_000;

    private final PitPlugin plugin;
    private final Map<UUID, LastAttacker> lastAttackers = new HashMap<>();
    /** 相手 → (攻撃した人 → 与えたダメージ)。アシストの割合に使う。 */
    private final Map<UUID, Map<UUID, Contribution>> damageTaken = new HashMap<>();
    private final Map<UUID, MultiKill> multiKills = new HashMap<>();
    /** この人生のキル数 (Promotion!! の 4 キルごと)。 */
    private final Map<UUID, Integer> lifeKills = new HashMap<>();

    public KillListener(PitPlugin plugin) {
        this.plugin = plugin;
    }

    private static final class Contribution {
        double damage;
        long time;
    }

    private static final class MultiKill {
        int count;
        long time;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player attacker = attacker(event.getDamager());
        if (attacker == null || attacker.equals(event.getEntity())) {
            return;
        }
        Player victim = (Player) event.getEntity();
        tag(attacker);
        tag(victim);
        long now = System.currentTimeMillis();
        lastAttackers.put(victim.getUniqueId(), new LastAttacker(attacker.getUniqueId(), now));
        Map<UUID, Contribution> contributions = damageTaken.get(victim.getUniqueId());
        if (contributions == null) {
            contributions = new HashMap<>();
            damageTaken.put(victim.getUniqueId(), contributions);
        }
        Contribution contribution = contributions.get(attacker.getUniqueId());
        if (contribution == null) {
            contribution = new Contribution();
            contributions.put(attacker.getUniqueId(), contribution);
        }
        contribution.damage += Math.max(0, Math.min(event.getFinalDamage(), victim.getHealth()));
        contribution.time = now;
        if (!plugin.getBotManager().isBot(attacker)) {
            // 殴った相手の体力 (Raw Numbers があれば与えたダメージも)
            boolean raw = plugin.getUpgradeManager().hasRenown(attacker.getUniqueId(), RenownUpgrade.RAW_NUMBERS);
            PlayerPackets.actionBar(attacker, HitDisplay.format(levelTag(victim) + " §7" + victim.getName(), victim.getHealth(),
                    event.getFinalDamage(), victim.getMaxHealth(), raw));
        }
    }

    /**
     * プレイヤーの攻撃以外で与えたダメージ (TNT など) を、攻撃した人のものとして覚える
     * (奈落などで死んだときのキルと、アシストの割合に使う)。
     */
    public void recordAttack(Player attacker, Player victim, double damage) {
        if (attacker.equals(victim)) {
            return;
        }
        tag(attacker);
        tag(victim);
        long now = System.currentTimeMillis();
        lastAttackers.put(victim.getUniqueId(), new LastAttacker(attacker.getUniqueId(), now));
        Contribution contribution = damageTaken.computeIfAbsent(victim.getUniqueId(), k -> new HashMap<>())
                .computeIfAbsent(attacker.getUniqueId(), k -> new Contribution());
        contribution.damage += Math.max(0, Math.min(damage, victim.getHealth()));
        contribution.time = now;
    }

    /**
     * HP が 0 以下になる攻撃は打ち消して、死んだときの処理をしてからスポーンへ移す (死亡画面は無し)。
     * 他のダメージの増減が全部終わった後 (HIGHEST) に判定する。
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLethalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player victim = (Player) event.getEntity();
        if (victim.isDead() || event.getFinalDamage() < victim.getHealth()) {
            return;
        }
        event.setCancelled(true);
        Player killer = null;
        if (event instanceof EntityDamageByEntityEvent) {
            killer = attacker(((EntityDamageByEntityEvent) event).getDamager());
        }
        if (killer == null) {
            // 奈落や /oof で死んだときは、少し前に殴っていた人のキルにする
            killer = recentAttacker(victim);
        }
        handleDeath(victim, killer, true);
    }

    /** /oof: HP 0 になったときと同じく死んだ扱いにする (少し前に殴っていた人がいればその人のキル)。 */
    public void oof(Player player) {
        handleDeath(player, recentAttacker(player), true);
    }

    /** 本当に死んだとき (HP 0 の仕組みを通らなかったとき)。バニラの死亡メッセージ (キルログ) は出さない。 */
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.setDroppedExp(0);
        event.setDeathMessage(null);
        if (!plugin.getBotManager().isBot(event.getEntity())) {
            // 万一本当に死んでもアイテムは落とさない (持ち物の処理は HP 0 のときと同じ)
            event.setKeepInventory(true);
            event.getDrops().clear();
        }
        handleDeath(event.getEntity(), event.getEntity().getKiller(), false);
    }

    /**
     * 死んだときの処理。fake = HP 0 でスポーンへ移すとき (持ち物の Lives もここで減らす)。
     * <pre>
     * 1. ストリークとバウンティを取り出す
     * 2. キルした人・アシストした人の報酬 (相手の防具を見るので持ち物の処理より前)
     * 3. 死亡のメッセージ、持ち物 (Lives -1。Divine Intervention ならそのまま)
     * 4. メガストリークの死亡時の効果、ストリークを 0 に
     * 5. 体力を戻してスポーンへ (Bot はスポーンに出し直す)
     * </pre>
     */
    private void handleDeath(Player victim, Player killer, boolean fake) {
        BotManager bots = plugin.getBotManager();
        boolean bot = bots.isBot(victim);
        UUID id = victim.getUniqueId();
        lastAttackers.remove(id);
        plugin.getLauncher().cancel(victim);
        plugin.getEnchantEffects().onDeath(victim, killer);
        plugin.getPerkManager().onDeath(victim);
        plugin.getKillstreakManager().reset(victim);
        int victimStreak = bot ? 0 : plugin.getStatsManager().getStreak(id);
        int victimBounty = bot ? 0 : plugin.getBountyManager().claim(id);
        // Spire の塔の中: 持ち物は預かっているので Lives も減らさず、防具も落とさず、スポーンではなく 1 階下へ
        boolean inSpire = plugin.getSpireEvent().inSpire(victim);

        if (killer != null && !killer.equals(victim) && !bots.isBot(killer)) {
            rewardKill(killer, victim, victimStreak, victimBounty);
            // 相手のダイヤの防具が確率で落ちる (Lodbrok で上がる)。持ち物が消える前に
            if (!inSpire) {
                plugin.getSimpleEnchants().armorDrops(killer, victim);
            }
        }
        if (killer != null && !killer.equals(victim)) {
            // Bot が塔の中でキルしても上の階へ
            plugin.getSpireEvent().onKill(killer);
        }
        rewardAssists(victim, killer, victimStreak, victimBounty);
        multiKills.remove(id);
        lifeKills.remove(id);
        if (!bot) {
            plugin.getPlayerProfiles().addDeath(id);
            plugin.getShopItemListener().onDeath(victim);
            sendDeathMessage(victim, killer);
            if (!inSpire) {
                if (plugin.getRenownEffects().rollDivineIntervention(victim)) {
                    victim.sendMessage("§b§lDIVINE INTERVENTION! §7Your inventory was saved!");
                } else {
                    // Divine Miracle (パンツ): 確率で持ち物の Lives が減らない (メッセージは推測)
                    boolean keepLives = plugin.getSimpleEnchants().rollDivineMiracle(victim);
                    if (keepLives) {
                        victim.sendMessage("§b§lDIVINE MIRACLE! §7Your items kept their lives!");
                    }
                    DeathItems.process(victim, plugin.getEnchantRegistry(),
                            plugin.getMegastreakManager().keepsDiamondHelmet(victim, victimStreak), keepLives);
                }
            }
            plugin.getMegastreakManager().onDeath(victim, victimStreak, victimBounty);
            plugin.getStatsManager().resetStreak(id);
        }
        if (inSpire) {
            plugin.getSpireEvent().onDeath(victim, fake);
        } else if (fake) {
            if (bot) {
                bots.respawn(victim);
            } else {
                sendToSpawn(victim);
            }
        }
        plugin.getSidebar().updateAll();
    }

    /** 録画の死亡のチャット・タイトル・音。 */
    private void sendDeathMessage(Player victim, Player killer) {
        String by = killer != null && !killer.equals(victim) ? "§7by " + chatName(killer, levelOf(killer)) + " " : "";
        victim.sendMessage("§c§lDEATH! " + by + "§e§lVIEW RECAP");
        PlayerPackets.title(victim, "§cYOU DIED!", "", 5, 20, 5);
        // 録画 (victim.mcrc / 2026-10-14_15-05-51.mcrc): スポーンに移った後に game.neutral.hurt (0.5 / 1.0)、
        // 2 tick 後に mob.zombie.infect (0.5 / 1.7)。Funky Feather が無くても毎回鳴る
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (victim.isOnline()) {
                victim.playSound(victim.getLocation(), DEATH_SOUND, 0.5f, 1.0f);
            }
        }, 1);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (victim.isOnline()) {
                victim.playSound(victim.getLocation(), RESPAWN_SOUND, 0.5f, 107 / 63f);
            }
        }, 3);
    }

    /** 死んだ代わりに・/oof・/spawn: 体力・空腹・効果を戻してスポーンへ。 */
    public void sendToSpawn(Player player) {
        plugin.getLauncher().cancel(player);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        plugin.getMegastreakManager().refresh(player);
        player.setHealth(player.getMaxHealth());
        // 前の人生の衝撃吸収を残さない (ポーション効果を消しても衝撃吸収の値は残るので)
        ((org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer) player).getHandle().setAbsorptionHearts(0);
        player.setFoodLevel(20);
        player.setSaturation(5);
        player.setFireTicks(0);
        player.setFallDistance(0);
        player.setVelocity(new Vector());
        player.teleport(plugin.getPitSpawn().randomPoint());
        // 先に Autobuy (着ているミスティックの場所はインベントリへ)、そのあと空いている場所だけ初期装備のチェーン・鉄を着せる
        plugin.getShopItemListener().autobuy(player);
        plugin.giveSpawnKit(player);
        plugin.getSimpleEnchants().onSpawn(player);
        showXpBar(player);
    }

    private Player recentAttacker(Player victim) {
        LastAttacker last = lastAttackers.get(victim.getUniqueId());
        if (last == null || System.currentTimeMillis() - last.time > RECENT_ATTACK_MILLIS) {
            return null;
        }
        return find(last.attacker);
    }

    /** オンラインのプレイヤーか Bot。 */
    private Player find(UUID id) {
        Player player = Bukkit.getPlayer(id);
        if (player == null) {
            // Bot はオンラインのプレイヤー一覧に居ないので探し直す
            player = plugin.getBotManager().find(id);
        }
        return player;
    }

    private static final class LastAttacker {
        final UUID attacker;
        final long time;

        LastAttacker(UUID attacker, long time) {
            this.attacker = attacker;
            this.time = time;
        }
    }

    /** 1 回目は KILL!、3 秒以内に続けると DOUBLE KILL! … PENTA KILL! (5)、6 回目からは MULTI KILL! (n)。 */
    static String killLabel(int count) {
        switch (count) {
            case 1:
                return "§a§lKILL!";
            case 2:
                return "§a§lDOUBLE KILL!";
            case 3:
                return "§a§lTRIPLE KILL!";
            case 4:
                return "§a§lQUADRA KILL!";
            case 5:
                return "§a§lPENTA KILL! §7(5)";
            default:
                return "§a§lMULTI KILL! §7(" + count + ")";
        }
    }

    private int nextMultiKill(UUID killer) {
        long now = System.currentTimeMillis();
        MultiKill multi = multiKills.get(killer);
        if (multi == null || now - multi.time > MULTI_KILL_MILLIS) {
            multi = new MultiKill();
            multiKills.put(killer, multi);
        }
        multi.count++;
        multi.time = now;
        return multi.count;
    }

    private void rewardKill(Player killer, Player victim, int victimStreak, int victimBounty) {
        StatsManager stats = plugin.getStatsManager();
        UUID id = killer.getUniqueId();
        int levelBefore = stats.getLevel(id);
        int victimLevel = levelOf(victim);
        int streakBefore = stats.getStreak(id);
        int streak = streakBefore + 1;
        KillRewards.Boosts boosts = plugin.getMegastreakManager().boosts(killer, streak);
        plugin.getPerkManager().addBoosts(killer, victim, boosts);
        plugin.getRenownEffects().addBoosts(killer, boosts);
        plugin.getSimpleEnchants().addKillBoosts(killer, boosts);
        plugin.getKillstreakManager().addBoosts(killer, streakBefore, streak, boosts);
        KillRewards.Kill kill = new KillRewards.Kill(levelBefore, victimLevel, streak, victimStreak,
                armorValue(killer), armorValue(victim), diamondPieces(victim), resourceEnchants(killer))
                .with(boosts);
        int xp = KillRewards.xp(kill);
        double gold = KillRewards.gold(kill);
        // KOTH: 丘の上で倒すと ×4 (バウンティは別に足すので ×4 にならない)
        double koth = plugin.getKothEvent().killMultiplier(killer);
        if (koth != 1) {
            xp = (int) Math.round(xp * koth);
            gold *= koth;
        }
        // 2X Rewards: 区域 (Mid) の中で倒すと ×2 (バウンティは別)
        double doubled = plugin.getDoubleRewards().killMultiplier(killer);
        if (doubled != 1) {
            xp = (int) Math.round(xp * doubled);
            gold *= doubled;
        }
        // Spire: 階の倍率 (1 階 -50% 〜 9 階 +250%)
        double spire = plugin.getSpireEvent().rewardMultiplier(killer);
        if (spire != 1) {
            xp = (int) Math.round(xp * spire);
            gold *= spire;
        }
        // Grim Reaper: キルの報酬 -80%
        double grim = plugin.getSimpleEnchants().killRewardMultiplier(killer);
        if (grim != 1) {
            xp = (int) Math.round(xp * grim);
            gold *= grim;
        }
        if (plugin.getPerkManager().convertsXp(killer)) {
            // Conglomerate: XP の代わりに 20% をゴールドで
            gold += PerkManager.conglomerateGold(xp);
            xp = 0;
        }

        stats.addKill(id);
        plugin.getNightCycle().onKill(killer);
        int kills = lifeKills.containsKey(id) ? lifeKills.get(id) + 1 : 1;
        lifeKills.put(id, kills);
        boolean promoted = plugin.getPerkManager().isPromoted(killer) && kills % PerkRules.PROMOTION_EVERY == 0;
        if (promoted) {
            // Promotion!!: 4 キルごとにストリーク +1
            stats.addStreak(id, 1);
        }
        stats.addXp(id, xp);
        plugin.getGoldManager().add(id, gold);
        plugin.getSpireEvent().recordReward(killer, xp, gold);
        int levelAfter = stats.getLevel(id);
        streak = stats.getStreak(id);

        if (levelAfter > levelBefore) {
            levelUp(killer, levelBefore, levelAfter);
        }
        String victimTag = chatName(victim, victimLevel);
        killer.sendMessage(killLabel(nextMultiKill(id)) + " §7on " + victimTag
                + (xp > 0 ? " §b+" + xp + "XP" : "") + " §6+" + GoldManager.formatExact(gold));
        if (promoted) {
            // キルのチャットの下に (ユーザーのスクリーンショット)
            killer.sendMessage("§e§lPROMOTION! §7Boosted killstreak by §e+1");
        }
        PlayerPackets.actionBar(killer, levelTag(victim) + " §7" + victim.getName() + " §a§lKILL!");
        showXpBar(killer);
        playKillSound(killer);
        ItemStack mystic = plugin.getRenownEffects().rollMysticDrop(killer);
        if (mystic != null) {
            dropMystic(killer, victim, mystic, victimTag);
        }

        // ストリーク・バウンティの全体チャットはプレステージのローマ数字なしの [レベル] (ユーザーの指定)
        String killerTag = broadcastName(killer, levelAfter);
        if (victimBounty > 0) {
            plugin.getGoldManager().add(id, victimBounty);
            broadcast("§6§lBOUNTY CLAIMED! " + killerTag + " §7killed " + broadcastName(victim, victimLevel) + " §7for §6§l" + victimBounty + "g");
        }
        broadcastStreaks(killer, streakBefore, streak, levelAfter);
        plugin.getPlayerProfiles().updateStreak(id, streak);
        plugin.getMegastreakManager().onKill(killer, streakBefore, streak, xp);
        plugin.getKillstreakManager().onKill(killer, streakBefore, streak);
        plugin.getPerkManager().onKill(killer, victim);
        plugin.getRenownEffects().onKill(killer);
        plugin.getShopItemListener().onKill(killer);
        plugin.getSimpleEnchants().onKill(killer, victim);
        int bountyBefore = plugin.getBountyManager().get(id);
        int bump = plugin.getBountyManager().onKill(id, streak, plugin.getMegastreakManager().maxBounty(killer));
        if (bump > 0) {
            // 初めて付いたときは "BOUNTY! of"、上がったときは "BOUNTY bump" (録画 Assist Kill Bounty Sound logs.mcrc)
            broadcast((bountyBefore == 0 ? "§6§lBOUNTY! §7of" : "§6§lBOUNTY §7bump") + " §6§l" + bump + "g §7on " + killerTag
                    + " §7for high streak");
            killer.playSound(killer.getLocation(), BOUNTY_BUMP_SOUND, 0.65f, 113 / 63f);
        }
    }

    /** ストリークが 5 と 10 の倍数を越えたら全体に STREAK! (Promotion!! やアシストで 1 より多く増えても漏らさない)。 */
    private void broadcastStreaks(Player player, int before, int after, int level) {
        for (int s = before + 1; s <= after; s++) {
            if (s == STREAK_BROADCAST_FIRST || s % STREAK_BROADCAST_EVERY == 0) {
                broadcast("§c§lSTREAK! §7of §c" + s + " §7kills by " + broadcastName(player, level));
            }
        }
    }

    /** キル以外の報酬の XP (Night Quest など)。レベルが上がったら LEVEL UP! を出す。 */
    public void giveXp(Player player, long xp) {
        StatsManager stats = plugin.getStatsManager();
        UUID id = player.getUniqueId();
        int levelBefore = stats.getLevel(id);
        stats.addXp(id, xp);
        int levelAfter = stats.getLevel(id);
        if (levelAfter > levelBefore) {
            levelUp(player, levelBefore, levelAfter);
        }
        showXpBar(player);
    }

    private void levelUp(Player player, int levelBefore, int levelAfter) {
        int prestige = plugin.getStatsManager().getPrestige(player.getUniqueId());
        String change = PrestigeRules.levelTag(prestige, levelBefore) + " §7➟ " + PrestigeRules.levelTag(prestige, levelAfter);
        PlayerPackets.title(player, "§b§lLEVEL UP!", change, TITLE_FADE_IN, TITLE_STAY, TITLE_FADE_OUT);
        player.playSound(player.getLocation(), LEVEL_UP_SOUND, 1.0f, 1.0f);
        player.sendMessage("§b§lPIT LEVEL UP! " + change);
    }

    /**
     * Mysticism で落ちた Mystic のアイテム: 死んだ場所に落とし、約 0.7 秒水しぶきを出す。
     * チャット (ユーザーのスクリーンショット): §d§lMYSTIC ITEM! §7dropped from killing [116] 名前§7!
     */
    private void dropMystic(Player killer, Player victim, ItemStack mystic, String victimTag) {
        Item item = victim.getWorld().dropItemNaturally(victim.getLocation(), mystic);
        // 拾えるのはキルした人だけ
        item.setMetadata(DROP_OWNER, new org.bukkit.metadata.FixedMetadataValue(plugin, killer.getUniqueId()));
        killer.sendMessage("§d§lMYSTIC ITEM! §7dropped from killing " + victimTag + "§7!");
        new BukkitRunnable() {
            private int ticks;

            @Override
            public void run() {
                if (ticks++ >= MYSTIC_SPLASH_TICKS || !item.isValid()) {
                    cancel();
                    return;
                }
                Location at = item.getLocation().add(0, 0.3, 0);
                PacketPlayOutWorldParticles splash = new PacketPlayOutWorldParticles(EnumParticle.WATER_SPLASH, true,
                        (float) at.getX(), (float) at.getY(), (float) at.getZ(), 0.2f, 0.2f, 0.2f, 0, 8);
                for (Player viewer : at.getWorld().getPlayers()) {
                    if (viewer.getLocation().distanceSquared(at) <= 64 * 64) {
                        PlayerPackets.send(viewer, splash);
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    /** Mysticism で落ちたアイテムをキルした人以外が拾えないようにする。 */
    @EventHandler(ignoreCancelled = true)
    public void onPickup(org.bukkit.event.player.PlayerPickupItemEvent event) {
        for (org.bukkit.metadata.MetadataValue value : event.getItem().getMetadata(DROP_OWNER)) {
            if (value.getOwningPlugin() == plugin && !event.getPlayer().getUniqueId().equals(value.value())) {
                event.setCancelled(true);
            }
        }
    }

    private void playKillSound(Player player) {
        for (int i = 0; i < KILL_PITCHES.length; i++) {
            float pitch = KILL_PITCHES[i] / 63f;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.playSound(player.getLocation(), KILL_SOUND, 1.0f, pitch);
                }
            }, (long) i * KILL_SOUND_INTERVAL);
        }
    }

    /**
     * アシスト: 死んだ人にダメージを与えていた (キルした人以外の) プレイヤーに、
     * 与えたダメージの割合 × 自分がキルしていたらもらえた XP・ゴールド。
     */
    private void rewardAssists(Player victim, Player killer, int victimStreak, int victimBounty) {
        Map<UUID, Contribution> contributions = damageTaken.remove(victim.getUniqueId());
        if (contributions == null) {
            return;
        }
        long now = System.currentTimeMillis();
        double total = 0;
        for (Contribution contribution : contributions.values()) {
            if (now - contribution.time <= ASSIST_MILLIS) {
                total += contribution.damage;
            }
        }
        if (total <= 0) {
            return;
        }
        int victimLevel = levelOf(victim);
        for (Map.Entry<UUID, Contribution> entry : contributions.entrySet()) {
            Contribution contribution = entry.getValue();
            Player assister = Bukkit.getPlayer(entry.getKey());
            if (assister == null || assister.equals(victim) || assister.equals(killer) || plugin.getBotManager().isBot(assister)
                    || now - contribution.time > ASSIST_MILLIS || contribution.damage <= 0) {
                continue;
            }
            rewardAssist(assister, victim, victimLevel, victimStreak, victimBounty, contribution.damage / total);
        }
    }

    private void rewardAssist(Player assister, Player victim, int victimLevel, int victimStreak, int victimBounty, double share) {
        StatsManager stats = plugin.getStatsManager();
        UUID id = assister.getUniqueId();
        int levelBefore = stats.getLevel(id);
        int streak = stats.getStreak(id) + 1;
        KillRewards.Boosts boosts = plugin.getMegastreakManager().boosts(assister, streak);
        plugin.getPerkManager().addAssistBoosts(assister, boosts);
        plugin.getRenownEffects().addBoosts(assister, boosts);
        KillRewards.Kill kill = new KillRewards.Kill(levelBefore, victimLevel, streak, victimStreak,
                armorValue(assister), armorValue(victim), diamondPieces(victim), resourceEnchants(assister)).with(boosts);
        int xp = (int) Math.round(share * KillRewards.xp(kill));
        double gold = Math.round(share * KillRewards.gold(kill) * 100) / 100.0
                + plugin.getPerkManager().assistExtraGold(assister, share, victimBounty);
        // KOTH: 自分も相手も丘の上なら ×4。2X Rewards: 自分が区域の中なら ×2
        double koth = plugin.getKothEvent().assistMultiplier(assister, victim) * plugin.getDoubleRewards().killMultiplier(assister);
        if (koth != 1) {
            xp = (int) Math.round(xp * koth);
            gold *= koth;
        }
        if (plugin.getPerkManager().convertsXp(assister)) {
            gold += PerkManager.conglomerateGold(xp);
            xp = 0;
        }
        stats.addXp(id, xp);
        plugin.getGoldManager().add(id, gold);
        int percent = (int) Math.round(share * 100);
        assister.sendMessage("§a§lASSIST! §7" + percent + "% on " + chatName(victim, victimLevel)
                + (xp > 0 ? " §b+" + xp + "XP" : "") + " §6+" + GoldManager.formatExact(gold));
        // 録画には無いが頼まれたので: キルの音の最初の 1 音だけ
        assister.playSound(assister.getLocation(), KILL_SOUND, ASSIST_VOLUME, KILL_PITCHES[0] / 63f);
        int levelAfter = stats.getLevel(id);
        if (levelAfter > levelBefore) {
            levelUp(assister, levelBefore, levelAfter);
        }
        showXpBar(assister);
        plugin.getPerkManager().onAssist(assister, share);
        plugin.getPlayerProfiles().addAssist(assister.getUniqueId());
        if (plugin.getPerkManager().countsAssists(assister)) {
            // Assistant (to the) Streaker: 参加の割合だけストリークが増える
            int before = stats.getStreak(id);
            stats.addStreak(id, share);
            int after = stats.getStreak(id);
            broadcastStreaks(assister, before, after, levelAfter);
            plugin.getMegastreakManager().onKill(assister, before, after, 0);
        }
    }

    private static void broadcast(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
        Bukkit.getConsoleSender().sendMessage(message);
    }

    /** バニラの経験値は拾っても増えない。 */
    @EventHandler
    public void onExpChange(PlayerExpChangeEvent event) {
        event.setAmount(0);
    }

    /** 参加したとき: 経験値バーと、足りない初期装備。 */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        showXpBar(player);
        plugin.giveSpawnKit(player);
        plugin.getSimpleEnchants().onSpawn(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        multiKills.remove(id);
        lifeKills.remove(id);
        damageTaken.remove(id);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            showXpBar(player);
            plugin.getSimpleEnchants().onSpawn(player);
            plugin.getSpireEvent().afterRespawn(player);
        });
    }

    private void showXpBar(Player player) {
        player.setLevel(plugin.getStatsManager().getLevel(player.getUniqueId()));
        player.setExp(plugin.getStatsManager().getProgress(player.getUniqueId()));
    }

    /** チャットの名前 (例: §9[§e§eI§9-§6§l67§9] §7名前、Bot はプレステージ 0)。 */
    private String chatName(Player player, int level) {
        return chatTag(player, level) + " " + plugin.getRankManager().nameColor(player.getUniqueId()) + player.getName();
    }

    /** ストリーク・バウンティの全体チャットの名前 (ローマ数字なし。例: §9[§6§l67§9] §7名前)。 */
    private String broadcastName(Player player, int level) {
        return PrestigeRules.levelTag(prestigeOf(player), level) + " " + plugin.getRankManager().nameColor(player.getUniqueId()) + player.getName();
    }

    private String chatTag(Player player, int level) {
        return PrestigeRules.chatTag(prestigeOf(player), level);
    }

    /** アクションバーのレベル (例: §6[§4§l86§6])。 */
    private String levelTag(Player player) {
        return PrestigeRules.levelTag(prestigeOf(player), levelOf(player));
    }

    private int prestigeOf(Player player) {
        return plugin.prestigeOf(player);
    }

    /** Bot は出現ごとに決まったレベル、プレイヤーは保存されている XP から。 */
    private int levelOf(Player player) {
        int botLevel = plugin.getBotManager().levelOf(player);
        return botLevel > 0 ? botLevel : plugin.getStatsManager().getLevel(player.getUniqueId());
    }

    /** 防具の値: ダイヤ 1 つ 4、鉄 1 つ 2、それ以外 0。 */
    private static int armorValue(Player player) {
        int value = 0;
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor == null) {
                continue;
            }
            String type = armor.getType().name();
            if (type.startsWith("DIAMOND_")) {
                value += 4;
            } else if (type.startsWith("IRON_")) {
                value += 2;
            }
        }
        return value;
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

    /** 手に持っている Mystic の武器と履いている Mystic Pants のエンチャント。 */
    private static List<Map<String, Integer>> resourceEnchants(Player player) {
        List<Map<String, Integer>> items = new ArrayList<>();
        ItemStack held = player.getItemInHand();
        // Somber を履いていると剣・弓のリソース系 (Gold Boost など) は効かない
        if (MysticItems.isMystic(held) && !MysticItems.isMysticPants(held)
                && !com.pitremake.combat.MysticAccess.mysticItemsBlocked(player)) {
            items.add(MysticData.read(held).getEnchants());
        }
        ItemStack pants = player.getInventory().getLeggings();
        if (MysticItems.isMysticPants(pants)) {
            items.add(MysticData.read(pants).getEnchants());
        }
        return items;
    }

    private void tag(Player player) {
        if (!plugin.getBotManager().isBot(player)) {
            plugin.getStatsManager().tagCombat(player.getUniqueId());
        }
    }

    private static Player attacker(Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        }
        if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof Player) {
            return (Player) ((Projectile) damager).getShooter();
        }
        return null;
    }
}
