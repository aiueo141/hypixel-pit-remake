package com.pitremake.perk;

import com.pitremake.PitPlugin;
import com.pitremake.combat.DamageCalculator;
import com.pitremake.combat.DamageContext;
import com.pitremake.combat.DamageModifier;
import com.pitremake.bounty.BountyRules;
import com.pitremake.item.MysticItems;
import com.pitremake.renown.RenownUpgrade;
import com.pitremake.shop.ShopItem;
import com.pitremake.stats.KillRewards;
import com.pitremake.util.PlayerPackets;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * パークの効果。数値は {@link PerkRules}。
 * <pre>
 * キルしたとき   : 金リンゴ (Golden Heads / Soup / Olympus で置き換え、Vampire / Rambo はもらえない)、
 *                 Vampire の再生、Rambo の全回復、Dirty の耐性、Strength-Chaining、Mineman の丸石、Lucky Diamond
 * キルの報酬     : Bounty Hunter +4g、Streaker、Spammer (KillListener が {@link #addBoosts} を呼ぶ)
 * ダメージ       : Gladiator / Marathon / Strength-Chaining / Bounty Hunter / First Strike / Recon / Soup / Barbarian /
 *                 Kung Fu Knowledge / Vampire の回復
 * 攻撃を消す     : Bonk! (録画 8 本で 105 回: largeexplode を足元 +0.5 に 3 個、散らばり 0.05 / 0.3 / 0.05、速さ 0.01。
 *                 金床の音は録画に入っていない = 当事者にだけ鳴らしている)。Idling → Fighting を 1 回防ぐもので、
 *                 一度 Fighting になってから Idling に戻るとまた使える。Kung Fu Knowledge の剣
 * スポーン       : Fishing Rod / Lava Bucket / Safety First / Mineman / Barbarian / Marathon
 * 最大体力       : Thick / Rambo ({@link #baseMaxHealth}、MegastreakManager が設定する)
 * </pre>
 */
public final class PerkManager implements Listener, DamageModifier {

    private static final String BONK_SOUND = "random.anvil_land";
    private static final String EAT_SOUND = "random.eat";
    private static final long HEAD_COOLDOWN_MILLIS = 1_000;
    private static final double BONK_PARTICLE_HEIGHT = 0.5;
    private static final double PARTICLE_VIEW_DISTANCE = 64;

    private final PitPlugin plugin;
    /** この人生で Bonk! を使った人。 */
    private final Set<UUID> bonked = new HashSet<>();
    /** Bonk! を使った後に Fighting になった人 (Idling に戻ったら Bonk! がまた使える)。 */
    private final Set<UUID> foughtSinceBonk = new HashSet<>();
    private final Map<UUID, StrengthStack> strength = new HashMap<>();
    /** First Strike: この人生で 1 回殴った相手。 */
    private final Map<UUID, Set<UUID>> struck = new HashMap<>();
    /** Spammer: 矢を当てた相手。 */
    private final Map<UUID, Set<UUID>> shot = new HashMap<>();
    private final Map<UUID, Integer> reconShots = new HashMap<>();
    private final Map<UUID, Integer> kungFuStrikes = new HashMap<>();
    /** Tasty Soup を飲んで次の近接攻撃が強い人。 */
    private final Set<UUID> soupEmpowered = new HashSet<>();
    private final Map<UUID, Long> lastHead = new HashMap<>();
    /** Lava Bucket で置いた溶岩。 */
    private final Set<Location> placedLava = new HashSet<>();

    private static final class StrengthStack {
        int stacks;
        long expires;
    }

    public PerkManager(PitPlugin plugin) {
        this.plugin = plugin;
    }

    /** サーバー停止時: 置いた溶岩を消す。 */
    public void stop() {
        for (Location location : placedLava) {
            Block block = location.getBlock();
            if (block.getType() == Material.LAVA || block.getType() == Material.STATIONARY_LAVA) {
                block.setType(Material.AIR);
            }
        }
        placedLava.clear();
    }

    public boolean has(Player player, Perk perk) {
        return plugin.getUpgradeManager().hasPerk(player.getUniqueId(), perk);
    }

    // ---------------------------------------------------------------- 体力・スポーン

    /** 最大体力 (Rambo は 8❤、Thick は +2❤、Renown の Extra Hearts)。メガストリークで減る分は MegastreakManager が引く。 */
    public double baseMaxHealth(Player player) {
        double health = has(player, Perk.RAMBO) ? PerkRules.RAMBO_MAX_HEALTH : PerkRules.BASE_MAX_HEALTH;
        if (has(player, Perk.THICK)) {
            health += PerkRules.THICK_HEALTH;
        }
        return health + plugin.getRenownEffects().extraHealth(player);
    }

    /**
     * 2 秒ごと・パークを替えたとき (MegastreakManager から): Barbarian の斧、Marathon はブーツを履けない、
     * Bonk! は Status が Idling に戻ったらまた使える。
     */
    public void refresh(Player player) {
        applyBarbarian(player);
        // Bonk! は Idling → Fighting になるのを 1 回防ぐ: 一度 Fighting になってから Idling に戻ったらまた使える
        UUID id = player.getUniqueId();
        if (plugin.getStatsManager().isFighting(id, BountyRules.combatExtensionMillis(plugin.getBountyManager().get(id)))) {
            if (bonked.contains(id)) {
                foughtSinceBonk.add(id);
            }
        } else if (foughtSinceBonk.remove(id)) {
            bonked.remove(id);
        }
        plugin.getRenownEffects().refresh(player);
        if (has(player, Perk.MARATHON)) {
            ItemStack boots = player.getInventory().getBoots();
            if (boots != null && boots.getType() != Material.AIR) {
                player.getInventory().setBoots(null);
                if (!isSpawnArmor(boots)) {
                    give(player, boots);
                }
            }
        }
    }

    /** スポーンしたとき (初期装備の後): パークのアイテム。持っているものはもらわない。 */
    public void giveSpawnItems(Player player) {
        PlayerInventory inventory = player.getInventory();
        if (has(player, Perk.FISHING_ROD) && !inventory.contains(Material.FISHING_ROD)) {
            give(player, PerkItems.fishingRod());
        }
        if (has(player, Perk.LAVA_BUCKET) && !inventory.contains(Material.LAVA_BUCKET) && !inventory.contains(Material.BUCKET)) {
            give(player, PerkItems.lavaBucket(true));
        }
        if (has(player, Perk.SAFETY_FIRST) && inventory.getHelmet() == null) {
            inventory.setHelmet(PerkItems.safetyHelmet());
        }
        if (has(player, Perk.MINEMAN)) {
            if (!inventory.contains(Material.DIAMOND_PICKAXE)) {
                give(player, PerkItems.minemanPickaxe());
            }
            int cobble = count(player, PerkItems.Type.MINEMAN_COBBLE);
            if (cobble < PerkRules.MINEMAN_COBBLE) {
                give(player, PerkItems.cobblestone(PerkRules.MINEMAN_COBBLE - cobble));
            }
        }
        refresh(player);
        player.updateInventory();
    }

    /**
     * Barbarian: 初期装備の鉄の剣と Items の村人の Diamond Sword を斧にする (外したら剣に戻す)。
     * 手に持っているものも含めて持ち物全部。Diamond Sword の印 (バウンティ持ちに +20%) はそのまま残る。
     */
    private void applyBarbarian(Player player) {
        boolean barbarian = has(player, Perk.BARBARIAN);
        PlayerInventory inventory = player.getInventory();
        boolean changed = false;
        // 初期装備の鉄の剣・斧はどちらか 1 本だけ (入り直したときに初期装備の剣がまた配られるので)
        Material keep = barbarian ? Material.IRON_AXE : Material.IRON_SWORD;
        boolean hasIron = false;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == keep && isSpawnWeapon(item)) {
                hasIron = true;
            }
        }
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            Material to = item == null ? null : barbarianSwap(item, barbarian);
            if (to == null) {
                continue;
            }
            boolean iron = to == Material.IRON_AXE || to == Material.IRON_SWORD;
            inventory.setItem(slot, iron && hasIron ? null : withType(item, to));
            hasIron |= iron;
            changed = true;
        }
        if (changed) {
            player.updateInventory();
        }
    }

    private static ItemStack withType(ItemStack item, Material type) {
        ItemStack copy = item.clone();
        copy.setType(type);
        return copy;
    }

    /** 斧 (剣) に替えるならその種類、替えないなら null。 */
    private static Material barbarianSwap(ItemStack item, boolean toAxe) {
        Material type = item.getType();
        if (isSpawnWeapon(item)) {
            if (toAxe && type == Material.IRON_SWORD) {
                return Material.IRON_AXE;
            }
            return !toAxe && type == Material.IRON_AXE ? Material.IRON_SWORD : null;
        }
        if (ShopItem.of(item) != ShopItem.DIAMOND_SWORD) {
            return null;
        }
        if (toAxe && type == Material.DIAMOND_SWORD) {
            return Material.DIAMOND_AXE;
        }
        return !toAxe && type == Material.DIAMOND_AXE ? Material.DIAMOND_SWORD : null;
    }

    /** 初期装備の武器 (名前なし・Mystic でもショップでもパークのアイテムでもない)。 */
    private static boolean isSpawnWeapon(ItemStack item) {
        boolean plain = !item.hasItemMeta() || !item.getItemMeta().hasDisplayName();
        return plain && !MysticItems.isMystic(item) && ShopItem.of(item) == null && PerkItems.of(item) == null;
    }

    // ---------------------------------------------------------------- キル・死亡

    /** キルの報酬に足す (KillRewards の計算前)。 */
    public void addBoosts(Player killer, Player victim, KillRewards.Boosts boosts) {
        if (has(killer, Perk.BOUNTY_HUNTER)) {
            boosts.flatGold += PerkRules.BOUNTY_HUNTER_GOLD;
        }
        if (has(killer, Perk.STREAKER)) {
            boosts.streakXpMultiplier *= PerkRules.STREAKER_MULTIPLIER;
        }
        Set<UUID> targets = shot.get(killer.getUniqueId());
        if (has(killer, Perk.SPAMMER) && targets != null && targets.contains(victim.getUniqueId())) {
            boosts.addGold(PerkRules.SPAMMER_GOLD_BONUS);
        }
        if (isPromoted(killer)) {
            boosts.addXp(PerkRules.PROMOTION_XP);
            boosts.flatGold += PerkRules.PROMOTION_GOLD;
        }
    }

    /** Assistant (to the) Streaker と Renown の Promotion!! の両方があるか。 */
    public boolean isPromoted(Player player) {
        return has(player, Perk.ASSISTANT_STREAKER)
                && plugin.getUpgradeManager().hasRenown(player.getUniqueId(), RenownUpgrade.PROMOTION);
    }

    /** アシストの報酬の元 (キルしたときの値) に足す分: Co-op Cat +50%、Promotion!! +15% XP。 */
    public void addAssistBoosts(Player assister, KillRewards.Boosts boosts) {
        if (has(assister, Perk.COOP_CAT)) {
            boosts.addXp(PerkRules.COOP_CAT_BONUS);
            boosts.addGold(PerkRules.COOP_CAT_BONUS);
        }
        if (isPromoted(assister)) {
            boosts.addXp(PerkRules.PROMOTION_XP);
        }
    }

    /** アシストの割合を掛けた後に足すゴールド: Spammer +2g、Promotion!! +2g、Bounty Hunter はバウンティの割合分。 */
    public double assistExtraGold(Player assister, double share, int victimBounty) {
        double gold = 0;
        if (has(assister, Perk.SPAMMER)) {
            gold += PerkRules.SPAMMER_ASSIST_GOLD;
        }
        if (isPromoted(assister)) {
            gold += PerkRules.PROMOTION_GOLD;
        }
        if (has(assister, Perk.BOUNTY_HUNTER)) {
            gold += share * victimBounty;
        }
        return gold;
    }

    /** アシストしたとき: Soup は Assist Soup、Assistant (to the) Streaker は参加の割合だけストリークが増える。 */
    public void onAssist(Player assister, double share) {
        if (has(assister, Perk.SOUP) && soups(assister) < limit(assister, PerkRules.SOUP_LIMIT, has(assister, Perk.OVERHEAL))) {
            give(assister, PerkItems.assistSoup());
        }
    }

    /** Assistant (to the) Streaker: アシストの参加の割合をストリークに足すか。 */
    public boolean countsAssists(Player player) {
        return has(player, Perk.ASSISTANT_STREAKER);
    }

    /** Strength-Chaining の今のレベル (切れていたら 0)。 */
    public int strengthLevel(Player player) {
        StrengthStack stack = strength.get(player.getUniqueId());
        return stack == null || stack.expires < System.currentTimeMillis() || !has(player, Perk.STRENGTH_CHAINING) ? 0 : stack.stacks;
    }

    /** Strength-Chaining が切れるまでの秒数 (切り上げ)。 */
    public int strengthSeconds(Player player) {
        StrengthStack stack = strength.get(player.getUniqueId());
        if (stack == null) {
            return 0;
        }
        return (int) Math.max(0, Math.ceil((stack.expires - System.currentTimeMillis()) / 1000.0));
    }

    /** Conglomerate: キルの XP をもらわず、その 20% をゴールドでもらう。 */
    public boolean convertsXp(Player killer) {
        return has(killer, Perk.CONGLOMERATE);
    }

    public static double conglomerateGold(int xp) {
        return PerkRules.conglomerateGold(xp);
    }

    /** キルの報酬を渡した後 (相手の持ち物の処理より前)。 */
    public void onKill(Player killer, Player victim) {
        giveHealingItem(killer);
        if (has(killer, Perk.VAMPIRE)) {
            killer.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, PerkRules.VAMPIRE_REGEN_TICKS, 0), true);
        }
        if (has(killer, Perk.RAMBO)) {
            killer.setHealth(killer.getMaxHealth());
        }
        if (has(killer, Perk.DIRTY)) {
            killer.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, PerkRules.DIRTY_RESISTANCE_TICKS, 1), true);
        }
        if (has(killer, Perk.STRENGTH_CHAINING)) {
            StrengthStack stack = strength.get(killer.getUniqueId());
            if (stack == null || stack.expires < System.currentTimeMillis()) {
                stack = new StrengthStack();
                strength.put(killer.getUniqueId(), stack);
            }
            stack.stacks = Math.min(stack.stacks + 1, PerkRules.STRENGTH_MAX_STACKS);
            stack.expires = System.currentTimeMillis() + PerkRules.STRENGTH_MILLIS;
        }
        if (has(killer, Perk.MINEMAN)) {
            int room = PerkRules.MINEMAN_MAX_COBBLE - count(killer, PerkItems.Type.MINEMAN_COBBLE);
            if (room > 0) {
                give(killer, PerkItems.cobblestone(Math.min(room, PerkRules.MINEMAN_COBBLE_PER_KILL)));
            }
        }
        if (has(killer, Perk.LUCKY_DIAMOND)) {
            for (ItemStack armor : victim.getInventory().getArmorContents()) {
                Material diamond = armor == null ? null : diamondVersion(armor.getType());
                if (diamond != null && ThreadLocalRandom.current().nextDouble() < PerkRules.LUCKY_DIAMOND_CHANCE) {
                    give(killer, unbreakable(diamond));
                }
            }
        }
    }

    /** 死んだとき (Bot も): この人生だけの状態を消す。 */
    public void onDeath(Player victim) {
        UUID id = victim.getUniqueId();
        bonked.remove(id);
        foughtSinceBonk.remove(id);
        strength.remove(id);
        struck.remove(id);
        shot.remove(id);
        reconShots.remove(id);
        kungFuStrikes.remove(id);
        soupEmpowered.remove(id);
        for (Set<UUID> targets : struck.values()) {
            targets.remove(id);
        }
        for (Set<UUID> targets : shot.values()) {
            targets.remove(id);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        onDeath(event.getPlayer());
        lastHead.remove(event.getPlayer().getUniqueId());
    }

    /** キルでもらう回復アイテム (金リンゴか、パークで置き換えたもの)。持てる数を超える分はもらわない。 */
    private void giveHealingItem(Player killer) {
        if (has(killer, Perk.VAMPIRE) || has(killer, Perk.RAMBO)) {
            return;
        }
        boolean overheal = has(killer, Perk.OVERHEAL);
        if (has(killer, Perk.OLYMPUS)) {
            if (count(killer, PerkItems.Type.OLYMPUS_POTION) < limit(killer, PerkRules.OLYMPUS_LIMIT, overheal)) {
                give(killer, PerkItems.olympusPotion());
            }
        } else if (has(killer, Perk.SOUP)) {
            if (soups(killer) < limit(killer, PerkRules.SOUP_LIMIT, overheal)) {
                give(killer, PerkItems.tastySoup());
            }
        } else if (has(killer, Perk.GOLDEN_HEADS)) {
            if (count(killer, PerkItems.Type.GOLDEN_HEAD) < limit(killer, PerkRules.GOLDEN_HEAD_LIMIT, overheal)) {
                give(killer, PerkItems.goldenHead());
            }
        } else if (goldenApples(killer) < limit(killer, PerkRules.GOLDEN_APPLE_LIMIT, overheal)) {
            give(killer, PerkItems.goldenApple());
        }
    }

    // ---------------------------------------------------------------- ダメージ

    /**
     * Bonk! と Kung Fu Knowledge (攻撃を消す・素手の攻撃力)。スポーンの保護 (LOWEST) の後、
     * ダメージの計算 (HIGH) の前。
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player victim = (Player) event.getEntity();
        Player attacker = attacker(event.getDamager());
        if (attacker == null || attacker.equals(victim)) {
            return;
        }
        boolean melee = event.getDamager() instanceof Player;
        if (melee && has(attacker, Perk.KUNG_FU_KNOWLEDGE)) {
            Material held = attacker.getItemInHand() == null ? Material.AIR : attacker.getItemInHand().getType();
            if (held.name().endsWith("_SWORD")) {
                event.setCancelled(true);
                return;
            }
            if (held == Material.AIR) {
                event.setDamage(DamageCalculator.isCritical(attacker) ? PerkRules.KUNG_FU_CRIT : PerkRules.KUNG_FU_FIST);
            }
        }
        if (has(victim, Perk.BONK) && bonked.add(victim.getUniqueId())) {
            event.setCancelled(true);
            victim.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, PerkRules.BONK_RESISTANCE_TICKS, 0), true);
            bonkEffect(victim, attacker);
        }
    }

    private void bonkEffect(Player victim, Player attacker) {
        Location at = victim.getLocation().add(0, BONK_PARTICLE_HEIGHT, 0);
        PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(EnumParticle.EXPLOSION_LARGE, true,
                (float) at.getX(), (float) at.getY(), (float) at.getZ(), 0.05f, 0.3f, 0.05f, 0.01f, 3);
        for (Player viewer : victim.getWorld().getPlayers()) {
            if (viewer.getLocation().distanceSquared(at) <= PARTICLE_VIEW_DISTANCE * PARTICLE_VIEW_DISTANCE) {
                PlayerPackets.send(viewer, packet);
            }
        }
        victim.playSound(at, BONK_SOUND, 1.0f, 1.0f);
        if (!plugin.getBotManager().isBot(attacker)) {
            attacker.playSound(at, BONK_SOUND, 1.0f, 1.0f);
        }
    }

    @Override
    public void modify(DamageContext context) {
        Player victim = context.victim;
        if (has(victim, Perk.GLADIATOR)) {
            context.reduce(PerkRules.gladiatorReduction(nearbyPlayers(victim)));
        }
        if (has(victim, Perk.MARATHON) && victim.hasPotionEffect(PotionEffectType.SPEED)) {
            context.reduce(PerkRules.MARATHON_BONUS);
        }
        if (!context.byPlayer()) {
            return;
        }
        Player attacker = context.attacker;
        UUID id = attacker.getUniqueId();
        StrengthStack stack = strength.get(id);
        if (stack != null && stack.expires >= System.currentTimeMillis() && has(attacker, Perk.STRENGTH_CHAINING)) {
            context.increase(PerkRules.strengthBonus(stack.stacks));
        }
        if (has(attacker, Perk.BOUNTY_HUNTER)) {
            // Hunt the Hunter (相手のパンツ) で半分 / 無し
            context.increase(PerkRules.bountyHunterBonus(plugin.getBountyManager().get(victim.getUniqueId()))
                    * plugin.getSimpleEnchants().bountyHunterFactor(victim, attacker));
        }
        if (has(attacker, Perk.MARATHON) && attacker.hasPotionEffect(PotionEffectType.SPEED)) {
            context.increase(PerkRules.MARATHON_BONUS);
        }
        if (context.isExtraStrike()) {
            return;
        }
        if (has(attacker, Perk.FIRST_STRIKE) && targets(struck, id).add(victim.getUniqueId())) {
            context.increase(PerkRules.FIRST_STRIKE_BONUS);
            attacker.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PerkRules.FIRST_STRIKE_SPEED_TICKS, 0), true);
        }
        if (context.melee) {
            melee(context, attacker);
        } else if (context.arrow != null) {
            arrow(context, attacker);
        }
        if (has(attacker, Perk.VAMPIRE) && (context.melee || context.arrow != null)) {
            double heal = context.arrow != null && context.arrow.isCritical()
                    ? PerkRules.VAMPIRE_HEAL * PerkRules.VAMPIRE_ARROW_CRIT_MULTIPLIER : PerkRules.VAMPIRE_HEAL;
            context.afterHit(damage -> heal(attacker, heal));
        }
    }

    private void melee(DamageContext context, Player attacker) {
        UUID id = attacker.getUniqueId();
        if (soupEmpowered.remove(id)) {
            context.increase(PerkRules.SOUP_MELEE_BONUS);
        }
        Material held = attacker.getItemInHand() == null ? Material.AIR : attacker.getItemInHand().getType();
        if (has(attacker, Perk.BARBARIAN) && held.name().endsWith("_AXE")) {
            context.addDamage(PerkRules.BARBARIAN_EXTRA);
        }
        if (has(attacker, Perk.KUNG_FU_KNOWLEDGE) && held == Material.AIR) {
            int strikes = kungFuStrikes.containsKey(id) ? kungFuStrikes.get(id) + 1 : 1;
            kungFuStrikes.put(id, strikes);
            if (strikes % PerkRules.KUNG_FU_EVERY == 0) {
                attacker.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PerkRules.KUNG_FU_SPEED_TICKS, 1), true);
            }
        }
    }

    private void arrow(DamageContext context, Player shooter) {
        UUID id = shooter.getUniqueId();
        if (has(shooter, Perk.RECON)) {
            int shots = reconShots.containsKey(id) ? reconShots.get(id) + 1 : 1;
            reconShots.put(id, shots);
            if (shots % PerkRules.RECON_EVERY == 0) {
                context.increase(PerkRules.RECON_BONUS);
                plugin.getStatsManager().addXp(id, PerkRules.RECON_XP);
                shooter.setLevel(plugin.getStatsManager().getLevel(id));
                shooter.setExp(plugin.getStatsManager().getProgress(id));
            }
        }
        if (has(shooter, Perk.SPAMMER)) {
            targets(shot, id).add(context.victim.getUniqueId());
            give(shooter, new ItemStack(Material.ARROW, PerkRules.SPAMMER_ARROWS));
        }
    }

    private static Set<UUID> targets(Map<UUID, Set<UUID>> map, UUID owner) {
        Set<UUID> targets = map.get(owner);
        if (targets == null) {
            targets = new HashSet<>();
            map.put(owner, targets);
        }
        return targets;
    }

    // ---------------------------------------------------------------- 回復アイテム

    /** Golden Head と Tasty Soup は右クリックですぐ使う。 */
    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        PerkItems.Type type = PerkItems.of(event.getItem());
        if (type != PerkItems.Type.GOLDEN_HEAD && type != PerkItems.Type.TASTY_SOUP && type != PerkItems.Type.ASSIST_SOUP) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (type == PerkItems.Type.GOLDEN_HEAD) {
            Long last = lastHead.get(player.getUniqueId());
            if (last != null && System.currentTimeMillis() - last < HEAD_COOLDOWN_MILLIS) {
                return;
            }
            lastHead.put(player.getUniqueId(), System.currentTimeMillis());
            // Very Yummy! (Renown): 再生 +2 秒、衝撃吸収 +0.5❤
            boolean veryYummy = plugin.getUpgradeManager().hasRenown(player.getUniqueId(), RenownUpgrade.VERY_YUMMY);
            effect(player, PotionEffectType.SPEED, 8 * 20, 0);
            effect(player, PotionEffectType.REGENERATION, (veryYummy ? 7 : 5) * 20, 1);
            absorption(player, veryYummy ? 7 : 6);
            plugin.getRenownEffects().onEat(player);
        } else if (type == PerkItems.Type.TASTY_SOUP) {
            effect(player, PotionEffectType.SPEED, 7 * 20, 0);
            heal(player, 3);
            absorption(player, 2);
            soupEmpowered.add(player.getUniqueId());
        } else {
            effect(player, PotionEffectType.SPEED, 7 * 20, 0);
            effect(player, PotionEffectType.REGENERATION, 5 * 20, 1);
            absorption(player, 2);
            soupEmpowered.add(player.getUniqueId());
        }
        takeOneInHand(player);
        player.playSound(player.getLocation(), EAT_SOUND, 1.0f, 1.0f);
    }

    /** Olympus Potion: 飲み終わったときにパークの効果 (ポーションそのものの効果は使わない)。 */
    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (PerkItems.of(event.getItem()) != PerkItems.Type.OLYMPUS_POTION) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        effect(player, PotionEffectType.SPEED, 24 * 20, 0);
        effect(player, PotionEffectType.REGENERATION, 10 * 20, 2);
        effect(player, PotionEffectType.DAMAGE_RESISTANCE, 4 * 20, 1);
        plugin.getStatsManager().addXp(player.getUniqueId(), 27);
        player.setLevel(plugin.getStatsManager().getLevel(player.getUniqueId()));
        player.setExp(plugin.getStatsManager().getProgress(player.getUniqueId()));
        takeOneInHand(player);
    }

    // ---------------------------------------------------------------- 溶岩

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }
        Block block = event.getBlockClicked().getRelative(event.getBlockFace());
        if (event.getBucket() != Material.LAVA_BUCKET || plugin.getPitSpawn().isProtected(block.getLocation())
                || plugin.getPitMap().isCenter(block.getLocation())) {
            event.setCancelled(true);
            return;
        }
        Location location = block.getLocation();
        placedLava.add(location);
        // 空になったバケツにもパークのアイテムの印を残す (捨てられない・もう一度汲める)
        event.setItemStack(PerkItems.lavaBucket(false));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (placedLava.remove(location)) {
                Block lava = location.getBlock();
                if (lava.getType() == Material.LAVA || lava.getType() == Material.STATIONARY_LAVA) {
                    lava.setType(Material.AIR);
                }
            }
        }, PerkRules.LAVA_TICKS);
    }

    /** バケツで汲めるのは置いた溶岩だけ。 */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }
        Location location = event.getBlockClicked().getLocation();
        if (!placedLava.remove(location)) {
            event.setCancelled(true);
            return;
        }
        event.setItemStack(PerkItems.lavaBucket(true));
    }

    /** パークのアイテムは捨てられない。 */
    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE && PerkItems.of(event.getItemDrop().getItemStack()) != null) {
            event.setCancelled(true);
        }
    }

    // ---------------------------------------------------------------- 小物

    private static Player attacker(Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        }
        if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof Player) {
            return (Player) ((Projectile) damager).getShooter();
        }
        return null;
    }

    private static int nearbyPlayers(Player player) {
        double range = PerkRules.GLADIATOR_RANGE;
        int count = 0;
        for (Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof Player && entity.getLocation().distanceSquared(player.getLocation()) <= range * range) {
                count++;
            }
        }
        return count;
    }

    private static void heal(Player player, double amount) {
        if (!player.isDead()) {
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + amount));
        }
    }

    /** 衝撃吸収の体力をその量まで (重ねない)。 */
    private static void absorption(Player player, float health) {
        EntityPlayer handle = ((CraftPlayer) player).getHandle();
        handle.setAbsorptionHearts(Math.max(handle.getAbsorptionHearts(), health));
    }

    private static void effect(Player player, PotionEffectType type, int ticks, int amplifier) {
        player.addPotionEffect(new PotionEffect(type, ticks, amplifier), true);
    }

    private static void takeOneInHand(Player player) {
        ItemStack hand = player.getItemInHand();
        if (hand.getAmount() > 1) {
            hand.setAmount(hand.getAmount() - 1);
        } else {
            player.setItemInHand(null);
        }
        player.updateInventory();
    }

    private static int count(Player player, PerkItems.Type type) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (PerkItems.of(item) == type) {
                total += item.getAmount();
            }
        }
        return total;
    }

    /** Tasty Soup と Assist Soup の合計 (持てる数は合わせて数える)。 */
    private static int soups(Player player) {
        return count(player, PerkItems.Type.TASTY_SOUP) + count(player, PerkItems.Type.ASSIST_SOUP);
    }

    private static int goldenApples(Player player) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.GOLDEN_APPLE) {
                total += item.getAmount();
            }
        }
        return total;
    }

    /** 回復アイテムを持てる数 (Overheal で 2 倍、パンツの Excess で +1 / +2 / +3)。 */
    private int limit(Player player, int base, boolean overheal) {
        return PerkRules.healingLimit(base, overheal) + plugin.getSimpleEnchants().excess(player);
    }

    /**
     * /spawn したとき: Excess で増えた分の回復アイテムを消す (Excess 無しで持てる数まで減らす)。
     * 金リンゴ・Golden Head・Tasty / Assist Soup・Olympus Potion。
     */
    public void trimExcessHealing(Player player) {
        trimHealing(player, 0);
    }

    /** 今履いているパンツの Excess で持てる数まで回復アイテムを減らす (Excess のパンツを脱いだとき: 4 → 2 など)。 */
    public void trimHealingToLimit(Player player) {
        trimHealing(player, plugin.getSimpleEnchants().excess(player));
    }

    private void trimHealing(Player player, int extra) {
        boolean overheal = has(player, Perk.OVERHEAL);
        trim(player, extra + PerkRules.healingLimit(PerkRules.GOLDEN_APPLE_LIMIT, overheal),
                item -> item.getType() == Material.GOLDEN_APPLE && PerkItems.of(item) != PerkItems.Type.GOLDEN_HEAD);
        trim(player, extra + PerkRules.healingLimit(PerkRules.GOLDEN_HEAD_LIMIT, overheal), item -> PerkItems.of(item) == PerkItems.Type.GOLDEN_HEAD);
        trim(player, extra + PerkRules.healingLimit(PerkRules.SOUP_LIMIT, overheal), item -> PerkItems.of(item) == PerkItems.Type.TASTY_SOUP
                || PerkItems.of(item) == PerkItems.Type.ASSIST_SOUP);
        trim(player, extra + PerkRules.healingLimit(PerkRules.OLYMPUS_LIMIT, overheal), item -> PerkItems.of(item) == PerkItems.Type.OLYMPUS_POTION);
    }

    /** matches に合うアイテムを合計 max 個まで減らす (後ろのスロットから)。 */
    private static void trim(Player player, int max, java.util.function.Predicate<ItemStack> matches) {
        ItemStack[] contents = player.getInventory().getContents();
        int total = 0;
        for (ItemStack item : contents) {
            if (item != null && matches.test(item)) {
                total += item.getAmount();
            }
        }
        for (int i = contents.length - 1; i >= 0 && total > max; i--) {
            ItemStack item = contents[i];
            if (item == null || !matches.test(item)) {
                continue;
            }
            int remove = Math.min(item.getAmount(), total - max);
            total -= remove;
            if (remove >= item.getAmount()) {
                player.getInventory().setItem(i, null);
            } else {
                item.setAmount(item.getAmount() - remove);
                player.getInventory().setItem(i, item);
            }
        }
    }

    private static void give(Player player, ItemStack item) {
        for (ItemStack leftover : player.getInventory().addItem(item).values()) {
            player.getWorld().dropItem(player.getLocation(), leftover);
        }
    }

    /** 初期装備の防具 (チェーン・鉄で名前なし) か。 */
    private static boolean isSpawnArmor(ItemStack item) {
        String type = item.getType().name();
        return (type.startsWith("CHAINMAIL_") || type.startsWith("IRON_")) && !item.getItemMeta().hasDisplayName();
    }

    private static Material diamondVersion(Material material) {
        switch (material) {
            case IRON_HELMET:
                return Material.DIAMOND_HELMET;
            case IRON_CHESTPLATE:
                return Material.DIAMOND_CHESTPLATE;
            case IRON_LEGGINGS:
                return Material.DIAMOND_LEGGINGS;
            case IRON_BOOTS:
                return Material.DIAMOND_BOOTS;
            default:
                return null;
        }
    }

    private static ItemStack unbreakable(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.spigot().setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }
}
