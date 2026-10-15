package com.pitremake.upgrade;

import com.pitremake.PitPlugin;
import com.pitremake.combat.DamageContext;
import com.pitremake.combat.DamageModifier;
import com.pitremake.item.MysticItems;
import com.pitremake.perk.PerkItems;
import com.pitremake.special.SpecialItems;
import com.pitremake.stats.KillRewards;
import com.pitremake.util.NbtUtil;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * キルストリーク (メガストリーク以外) の効果 (説明文と pit.wiki の Killstreaks)。
 * 選んだ枠のキルストリークが、ストリークがその数の倍数に届くたびに発動する (3 キルごとなら 3 / 6 / 9 …)。
 * 重ねる効果 (Khanate / Tough Skin / Rush / Monster / Super Streaker) と一時的な効果は死ぬとなくなる。
 * Gold Stack / XP Stack は選んでいなくてもずっと付き、プレステージで消える。
 */
public final class KillstreakManager implements Listener, DamageModifier {

    private static final String ITEM_KEY = "PitKillstreakItem";
    private static final int ITEM_SMELLY_BOMB = 1;
    private static final int ITEM_STEAK = 2;
    private static final int ITEM_ASSEMBLER = 3;
    private static final int ITEM_GLASS_PICKAXE = 4;
    private static final int ITEM_AURA = 5;
    private static final int ITEM_ICE_CUBE = 6;
    private static final int MAX_ARROWS = 128;
    private static final double GLASS_PICKAXE_DAMAGE = 8.5;

    private final PitPlugin plugin;
    /** プレイヤー → (重ねる効果 → 重なった数)。 */
    private final Map<UUID, Map<Killstreak, Integer>> stacks = new HashMap<>();
    /** プレイヤー → (一時的な効果の名前 → 終わる時刻)。 */
    private final Map<UUID, Map<String, Long>> buffs = new HashMap<>();
    /** 次の一撃が強くなる (Leech / Assured Strike)。 */
    private final Map<UUID, Map<Killstreak, Boolean>> nextHit = new HashMap<>();

    public KillstreakManager(PitPlugin plugin) {
        this.plugin = plugin;
    }

    private List<Killstreak> selected(Player player) {
        List<Killstreak> list = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Killstreak killstreak = plugin.getUpgradeManager().getKillstreak(player.getUniqueId(), i);
            if (killstreak != null && !list.contains(killstreak)) {
                list.add(killstreak);
            }
        }
        return list;
    }

    private int stack(Player player, Killstreak killstreak) {
        Map<Killstreak, Integer> map = stacks.get(player.getUniqueId());
        Integer value = map == null ? null : map.get(killstreak);
        return value == null ? 0 : value;
    }

    private void addStack(Player player, Killstreak killstreak, int max) {
        Map<Killstreak, Integer> map = stacks.computeIfAbsent(player.getUniqueId(), k -> new EnumMap<>(Killstreak.class));
        map.put(killstreak, Math.min(max, stack(player, killstreak) + 1));
    }

    private void buff(Player player, String name, long millis) {
        buffs.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).put(name, System.currentTimeMillis() + millis);
    }

    private boolean hasBuff(Player player, String name) {
        Map<String, Long> map = buffs.get(player.getUniqueId());
        Long until = map == null ? null : map.get(name);
        return until != null && until > System.currentTimeMillis();
    }

    private static boolean triggers(Killstreak killstreak, int before, int after) {
        return after / killstreak.getEvery() > before / killstreak.getEvery();
    }

    /** Gold Stack の上限 (0.5g + プレステージ 1 つにつき 0.1g。録画: プレステージ XVIII で 2.3g)。 */
    public static double goldStackMax(int prestige) {
        return 0.5 + 0.1 * prestige;
    }

    /** XP Stack の上限 (1 XP + プレステージ 1 つにつき 0.2 XP。録画: 4.6 XP)。 */
    public static double xpStackMax(int prestige) {
        return 1 + 0.2 * prestige;
    }

    // ---------------------------------------------------------------- キル

    /** キルの報酬に足す分 (報酬を計算する前、ストリークが before → after に増えるとき)。 */
    public void addBoosts(Player killer, int before, int after, KillRewards.Boosts boosts) {
        UUID id = killer.getUniqueId();
        boosts.flatGold += plugin.getUpgradeManager().getGoldStack(id);
        boosts.flatXp += plugin.getUpgradeManager().getXpStack(id);
        int superStreaker = stack(killer, Killstreak.SUPER_STREAKER);
        if (superStreaker > 0) {
            boosts.addXp(0.05 * superStreaker);
        }
        for (Killstreak killstreak : selected(killer)) {
            if (!triggers(killstreak, before, after)) {
                continue;
            }
            switch (killstreak) {
                case SECOND_GAPPLE:
                    boosts.flatXp += 5;
                    boosts.flatGold += 5;
                    break;
                case EXPLICIOUS:
                    boosts.flatXp += 12;
                    break;
                case ARQUEBUSIER:
                    boosts.flatGold += 7;
                    break;
                case KHANATE:
                    boosts.flatGold += 8;
                    break;
                case SUPER_STREAKER:
                    boosts.flatXp += 50;
                    break;
                default:
                    break;
            }
        }
    }

    /** キルの後: ストリークが before → after に増えた。発動するキルストリークの効果。 */
    public void onKill(Player killer, int before, int after) {
        int prestige = plugin.getStatsManager().getPrestige(killer.getUniqueId());
        for (Killstreak killstreak : selected(killer)) {
            if (triggers(killstreak, before, after)) {
                trigger(killer, killstreak, prestige);
            }
        }
    }

    private void trigger(Player player, Killstreak killstreak, int prestige) {
        switch (killstreak) {
            case SECOND_GAPPLE:
                give(player, PerkItems.goldenApple());
                break;
            case R_AND_R:
                effect(player, PotionEffectType.DAMAGE_RESISTANCE, 3, 0);
                effect(player, PotionEffectType.REGENERATION, 3, 1);
                break;
            case ARQUEBUSIER:
                int arrows = 0;
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null && item.getType() == Material.ARROW) {
                        arrows += item.getAmount();
                    }
                }
                int add = Math.min(16, MAX_ARROWS - arrows);
                if (add > 0) {
                    give(player, new ItemStack(Material.ARROW, add));
                }
                effect(player, PotionEffectType.SPEED, 10, 0);
                break;
            case KHANATE:
                addStack(player, killstreak, 10);
                break;
            case LEECH:
            case ASSURED_STRIKE:
                nextHit.computeIfAbsent(player.getUniqueId(), k -> new EnumMap<>(Killstreak.class)).put(killstreak, true);
                break;
            case TOUGH_SKIN:
                addStack(player, killstreak, 8);
                break;
            case FIGHT_OR_FLIGHT:
                if (player.getHealth() < player.getMaxHealth() / 2) {
                    effect(player, PotionEffectType.SPEED, 7, 0);
                    effect(player, PotionEffectType.DAMAGE_RESISTANCE, 7, 0);
                } else {
                    buff(player, "fight", 7_000);
                }
                break;
            case PUNGENT:
                give(player, item(ITEM_SMELLY_BOMB, Material.SLIME_BALL, 0, "§cSmelly Bomb",
                        "§7Applies §9Slowness I §7to players within 3", "§7blocks for 5 seconds.", "", "§eRight-click to use!"));
                break;
            case HEROS_HASTE:
                effect(player, PotionEffectType.SPEED, 5, 1);
                break;
            case RUSH:
                addStack(player, killstreak, 10);
                plugin.getSimpleEnchants().refresh(player);
                break;
            case FEAST:
                give(player, item(ITEM_STEAK, Material.MUTTON, 0, "§6AAA-Rated Steak",
                        "§7■ §c+20% damage", "§7■ §eSpeed I", "§7■ §9Resistance I", "§7Insta-eat (0:10)"));
                break;
            case COUNTER_STRIKE:
                buff(player, "counter", 8_000);
                break;
            case GOLD_NANO_FACTORY:
                give(player, item(ITEM_ASSEMBLER, Material.GOLD_NUGGET, 0, "§6Molecular Assembler",
                        "§7Spawns §67 gold ingots§7.", "§7Grants §cRegen IV §7for 2 seconds.", "", "§eRight-click to use!"));
                break;
            case TACTICAL_RETREAT:
                effect(player, PotionEffectType.REGENERATION, 5, 3);
                effect(player, PotionEffectType.WEAKNESS, 5, 3);
                break;
            case GLASS_PICKAXE:
                ItemStack pickaxe = item(ITEM_GLASS_PICKAXE, Material.DIAMOND_PICKAXE, 0, "§bGlass Pickaxe",
                        "§7Single-use weapon.", "§7Deals §c+0.5❤ §7true damage.");
                give(player, NbtUtil.setAttribute(pickaxe, "generic.attackDamage", GLASS_PICKAXE_DAMAGE));
                break;
            case AURA_OF_PROTECTION:
                give(player, item(ITEM_AURA, Material.SLIME_BALL, 0, "§aAura of Protection",
                        "§9Resistance II §7(0:04)", "§eTrue Damage §7immunity §7(0:15)", "", "§eRight-click to use!"));
                break;
            case ICE_CUBE:
                give(player, item(ITEM_ICE_CUBE, Material.PACKED_ICE, 0, "§bIce Cube",
                        "§7Single-Use on melee strike.", "§7Deals §c1❤ §7true damage to victim.", "§7Gain §b40 XP§7.",
                        "§7Attacks slow enemies for 10 seconds."));
                break;
            case SUPER_STREAKER:
                addStack(player, killstreak, 10);
                break;
            case GOLD_STACK:
                plugin.getUpgradeManager().addStacks(player.getUniqueId(), 0.1, goldStackMax(prestige), 0, 0);
                break;
            case XP_STACK:
                plugin.getUpgradeManager().addStacks(player.getUniqueId(), 0, 0, 0.05, xpStackMax(prestige));
                break;
            case MONSTER:
                addStack(player, killstreak, 2);
                plugin.getMegastreakManager().refresh(player);
                break;
            case SPONGESTEVE:
                absorption(player, 30);
                break;
            case APOSTLE_TO_RNGESUS:
                rngesus(player);
                break;
            default:
                break;
        }
    }

    /** Apostle to RNGesus: 1〜100 を引いて、当てはまる効果を全部 (説明文の表)。チャットは推測。 */
    private void rngesus(Player player) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int roll = 1 + random.nextInt(100);
        UUID id = player.getUniqueId();
        player.sendMessage("§e§lRNGESUS! §7You rolled §e" + roll + "§7!");
        if (roll == 27 || roll == 43) {
            plugin.getStatsManager().setRenown(id, plugin.getStatsManager().getRenown(id) + 1);
        }
        if (roll == 42) {
            absorption(player, 20);
        }
        if (roll == 50) {
            give(player, new ItemStack(Material.DIAMOND_HELMET));
        }
        if (roll == 13 || roll == 66) {
            give(player, SpecialItems.vile(3));
        }
        if (roll == 77 || roll == 88) {
            give(player, random.nextBoolean() ? MysticItems.freshSword() : MysticItems.freshBow());
        }
        if (roll == 99) {
            player.getWorld().strikeLightningEffect(player.getLocation());
            if (player.getHealth() - 8 > 0) {
                player.setHealth(player.getHealth() - 8);
            } else {
                player.damage(player.getHealth() * 100);
            }
        }
        if (roll % 2 == 0) {
            effect(player, PotionEffectType.REGENERATION, 20, 0);
        }
        if (roll % 3 == 0) {
            effect(player, PotionEffectType.SPEED, 15, 0);
        }
        if (roll % 4 == 0) {
            effect(player, PotionEffectType.REGENERATION, 10, 1);
        }
        if (roll % 7 == 0) {
            plugin.getGoldManager().add(id, 500 + random.nextInt(4_501));
        }
        if (roll % 8 == 0) {
            plugin.getStatsManager().addXp(id, 500 + random.nextInt(4_501));
        }
        if (roll % 12 == 0) {
            absorption(player, 8);
        }
        if (roll % 13 == 0) {
            buff(player, "rngesus", 30_000);
        }
        plugin.getSidebar().update(player);
    }

    // ---------------------------------------------------------------- ダメージ

    @Override
    public void modify(DamageContext context) {
        if (!context.byPlayer()) {
            return;
        }
        Player attacker = context.attacker;
        Player victim = context.victim;
        // 殴った側
        int khanate = stack(attacker, Killstreak.KHANATE);
        if (khanate > 0 && plugin.getBountyManager().get(victim.getUniqueId()) > 0) {
            context.increase(0.04 * khanate);
        }
        if (hasBuff(attacker, "fight") || hasBuff(attacker, "feast")) {
            context.increase(0.20);
        }
        if (hasBuff(attacker, "counter")) {
            context.increase(0.15);
        }
        if (hasBuff(attacker, "rngesus")) {
            context.increase(0.25);
        }
        Map<Killstreak, Boolean> next = nextHit.get(attacker.getUniqueId());
        if (next != null && !context.isExtraStrike()) {
            if (next.remove(Killstreak.LEECH) != null) {
                context.afterHit(dealt -> heal(attacker, 1 + 0.2 * dealt));
            }
            if (context.melee && next.remove(Killstreak.ASSURED_STRIKE) != null) {
                context.increase(0.35);
                effect(attacker, PotionEffectType.SPEED, 20, 0);
            }
        }
        if (context.melee && !context.isExtraStrike()) {
            int kind = kind(attacker.getItemInHand());
            if (kind == ITEM_GLASS_PICKAXE) {
                context.addTrueDamage(1);
                consumeHand(attacker);
            } else if (kind == ITEM_ICE_CUBE) {
                context.addTrueDamage(2);
                victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 200, 0), true);
                plugin.getStatsManager().addXp(attacker.getUniqueId(), 40);
                consumeHand(attacker);
            }
        }
        // 殴られた側
        int toughSkin = stack(victim, Killstreak.TOUGH_SKIN);
        if (toughSkin > 0) {
            // E の外で掛ける f_p (逆算レポート-1)
            context.multiply(1 - 0.03 * toughSkin);
        }
        if (hasBuff(victim, "counter")) {
            context.reduceFlat(2);
        }
        if (hasBuff(victim, "aura")) {
            context.immuneToTrueDamage();
        }
    }

    /** Monster の分の最大体力 (1 つにつき +1❤)。 */
    public double extraMaxHealth(Player player) {
        return 2.0 * stack(player, Killstreak.MONSTER);
    }

    /** Rush の分の歩く速さ (1 つにつき +1.5%)。 */
    public double rushSpeed(Player player) {
        return 0.015 * stack(player, Killstreak.RUSH);
    }

    /** 死んだとき・ストリークが 0 になったとき: 重ねた効果と一時的な効果を消す。 */
    public void reset(Player player) {
        UUID id = player.getUniqueId();
        boolean hadStacks = stacks.remove(id) != null;
        buffs.remove(id);
        nextHit.remove(id);
        if (hadStacks) {
            plugin.getSimpleEnchants().refresh(player);
        }
    }

    // ---------------------------------------------------------------- アイテム

    private static ItemStack item(int kind, Material material, int data, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1, (short) data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return NbtUtil.setInt(item, ITEM_KEY, kind);
    }

    private static int kind(ItemStack item) {
        return item == null || item.getType() == Material.AIR ? 0 : NbtUtil.getInt(item, ITEM_KEY);
    }

    /**
     * キルストリークでもらった道具 (Aura of Protection / Smelly Bomb / Steak / Assembler / Glass Pickaxe / Ice Cube) は
     * 捨てられない (Q キーでも、画面の外へ出しても)。パークのアイテムと同じくクリエイティブなら捨てられる。
     */
    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE && kind(event.getItemDrop().getItemStack()) != 0) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        int kind = kind(event.getItem());
        switch (kind) {
            case ITEM_SMELLY_BOMB:
                event.setCancelled(true);
                consumeHand(player);
                for (org.bukkit.entity.Entity near : player.getNearbyEntities(3, 3, 3)) {
                    if (near instanceof Player && near.getLocation().distanceSquared(player.getLocation()) <= 9) {
                        ((Player) near).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 0), true);
                    }
                }
                player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.SLIME_WALK2, 1.0f, 0.6f);
                break;
            case ITEM_STEAK:
                event.setCancelled(true);
                consumeHand(player);
                buff(player, "feast", 10_000);
                effect(player, PotionEffectType.SPEED, 10, 0);
                effect(player, PotionEffectType.DAMAGE_RESISTANCE, 10, 0);
                player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.EAT, 1.0f, 1.0f);
                break;
            case ITEM_ASSEMBLER:
                event.setCancelled(true);
                consumeHand(player);
                effect(player, PotionEffectType.REGENERATION, 2, 3);
                // Pit に落ちているものと同じ金インゴット (拾うと GOLD PICKUP!)
                for (int i = 0; i < 7; i++) {
                    plugin.getGoldIngots().drop(player.getLocation().add(0, 0.5, 0), new Vector(
                            ThreadLocalRandom.current().nextDouble(-0.2, 0.2), 0.3, ThreadLocalRandom.current().nextDouble(-0.2, 0.2)));
                }
                break;
            case ITEM_AURA:
                event.setCancelled(true);
                consumeHand(player);
                effect(player, PotionEffectType.DAMAGE_RESISTANCE, 4, 1);
                buff(player, "aura", 15_000);
                player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.LEVEL_UP, 1.0f, 1.5f);
                break;
            case ITEM_ICE_CUBE:
            case ITEM_GLASS_PICKAXE:
                // 置いたり壊したりしない
                event.setCancelled(true);
                break;
            default:
                break;
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        reset(event.getPlayer());
    }

    private static void consumeHand(Player player) {
        ItemStack hand = player.getItemInHand();
        if (hand.getAmount() > 1) {
            hand.setAmount(hand.getAmount() - 1);
            player.setItemInHand(hand);
        } else {
            player.setItemInHand(null);
        }
    }

    private static void give(Player player, ItemStack item) {
        for (ItemStack leftover : player.getInventory().addItem(item).values()) {
            player.getWorld().dropItem(player.getLocation(), leftover);
        }
    }

    private static void effect(Player player, PotionEffectType type, int seconds, int amplifier) {
        player.addPotionEffect(new PotionEffect(type, seconds * 20, amplifier), true);
    }

    private static void heal(Player player, double amount) {
        if (amount > 0 && !player.isDead()) {
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + amount));
        }
    }

    private static void absorption(Player player, float health) {
        net.minecraft.server.v1_8_R3.EntityPlayer handle = ((CraftPlayer) player).getHandle();
        handle.setAbsorptionHearts(Math.max(handle.getAbsorptionHearts(), health));
    }
}
