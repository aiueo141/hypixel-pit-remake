package com.pitremake.combat;

import com.pitremake.PitPlugin;
import com.pitremake.enchant.PitEnchant;
import com.pitremake.item.MysticData;
import com.pitremake.item.MysticItems;
import com.pitremake.item.MysticType;
import com.pitremake.stats.PrestigeRules;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 説明文どおりに動くだけの簡単なエンチャント (数値は説明文から読む {@link EnchantNumbers}、レベルごとの表は説明文の値)。
 * <pre>
 * パンツ (常に)       : Gotta go fast (歩く速さ) / Hearts (最大体力) / Passive health regen
 * パンツ (受けたとき) : Peroxide / Counter-Offensive / Fortitude (resistance_when_low) / Revitalize / Superspeed (3❤ 以下)
 * パンツ (キル)       : Golden Heart / Refresh speed on kill
 * パンツ (スポーン)   : Respawn: Absorption / Respawn: Resistance / Out of combat で全回復 (regen_when_ooc)
 * 剣 (殴ったとき)     : Pitpocket (ゴールドを盗む・コウモリの音) / Speed on hit / Gold per hit / Gold per crit
 * 剣 (キル)           : Speed on kill / Heal on kill / Resistance on kill
 * 弓 (当たったとき)   : Pin down (最大まで引いた矢で Speed と Jump Boost を消して、しばらく付かなくする) /
 *                       Weakness on hit / Arrows on hit
 * </pre>
 */
public final class SimpleEnchants implements DamageModifier, Listener {

    /** 「3❤ になったとき」系の体力 (3❤ = 6)。 */
    private static final double LOW_HEALTH = 6;
    private static final long GOTTA_GO_FAST_TRAIL_TICKS = 4;
    private static final double KNOCKBACK_PER_LEVEL = 0.5;
    /** Hidden Jewel が Tier I のアイテムになるキル数。 */
    static final int JEWEL_KILLS = 117;

    private final PitPlugin plugin;
    private final Map<UUID, Long> peroxideUsed = new HashMap<>();
    private final Map<UUID, Long> revitalizeUsed = new HashMap<>();
    private final Map<UUID, Long> superspeedUsed = new HashMap<>();
    private final Map<UUID, Long> speedOnHitUsed = new HashMap<>();
    private final Map<UUID, Long> goldPerHitUsed = new HashMap<>();
    private final Map<UUID, Long> pitpocketUsed = new HashMap<>();
    private final Map<UUID, Integer> hitsTaken = new HashMap<>();
    /** Pin down: 相手 → 終わる時刻。 */
    private final Map<UUID, Long> pinned = new HashMap<>();
    /** Pin down: 相手 → 撃った人。 */
    private final Map<UUID, UUID> pinnedBy = new HashMap<>();
    /** 戦闘中だったか (regen_when_ooc の「戦闘に入ったとき」)。 */
    private final Map<UUID, Boolean> wasFighting = new HashMap<>();
    /** 履いているパンツの Nonce (履き替えの判定)。 */
    private final Map<UUID, Integer> wornPants = new HashMap<>();
    /** 前に履いていたパンツの Respawn: Absorption / Resistance のレベル (脱いだときに消す)。 */
    private final Map<UUID, int[]> wornRespawnLevels = new HashMap<>();
    /** Duelist: ガードした人 → (殴ってきた人 → ガードした回数)。 */
    private final Map<UUID, Map<UUID, Integer>> duelistBlocks = new HashMap<>();
    /** Duelist: 次の一撃が強くなる (ガードした人 → 相手)。 */
    private final Map<UUID, java.util.Set<UUID>> duelistEmpowered = new HashMap<>();
    private int seconds;

    public SimpleEnchants(PitPlugin plugin) {
        this.plugin = plugin;
        new BukkitRunnable() {
            @Override
            public void run() {
                tickPinned();
            }
        }.runTaskTimer(plugin, 1, 1);
        new BukkitRunnable() {
            @Override
            public void run() {
                seconds++;
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (!player.isDead()) {
                        everySecond(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 20, 20);
        new BukkitRunnable() {
            @Override
            public void run() {
                speedTrail();
            }
        }.runTaskTimer(plugin, GOTTA_GO_FAST_TRAIL_TICKS, GOTTA_GO_FAST_TRAIL_TICKS);
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (!player.isDead()) {
                        checkPantsSwap(player);
                        selfCheckout(player);
                        if (plugin.getPitSpawn().isProtected(player.getLocation())) {
                            // スポーンに戻ったら Phoenix と Escape Pod がまた使える (それまでは誰に殴られても発動しない)
                            phoenixUsed.remove(player.getUniqueId());
                            escapePodUsed.remove(player.getUniqueId());
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 5, 5);
        new BukkitRunnable() {
            @Override
            public void run() {
                tickSnowmen();
            }
        }.runTaskTimer(plugin, 7, 7);
        new BukkitRunnable() {
            @Override
            public void run() {
                tickBlobs();
            }
        }.runTaskTimer(plugin, 10, 7);
    }

    /**
     * 履き替えの判定 (0.25 秒ごと): 別のパンツを履いたら Respawn: Absorption / Resistance を付け、
     * それを付けていたパンツを脱いだら衝撃吸収と耐性を消す (ユーザーの指定)。
     * Eggs / Creative / TNT のアイテムは脱ぐと消え、履くとスポーンでもらう数だけもらえる。
     */
    private void checkPantsSwap(Player player) {
        UUID id = player.getUniqueId();
        ItemStack leggings = player.getInventory().getLeggings();
        int nonce = MysticItems.isMysticPants(leggings) ? com.pitremake.util.NbtUtil.getInt(leggings, MysticItems.NONCE) : 0;
        Integer lastNonce = wornPants.put(id, nonce);
        Map<String, Integer> pants = pants(player);
        int[] now = {pants.containsKey("respawn_with_absorption") || pants.containsKey("needless_suffering") ? 1 : 0,
                pants.containsKey("respawn_with_resistance") ? 1 : 0};
        int[] before = wornRespawnLevels.put(id, now);
        if (lastNonce == null || lastNonce == nonce) {
            return;
        }
        // パンツ由来のアイテム (Eggs / Creative / TNT) を今のパンツに合わせ、Excess が減った分の回復アイテムを消す
        resetEnchantItems(player);
        plugin.getPerkManager().trimHealingToLimit(player);
        if (before != null && before[0] == 1) {
            ((CraftPlayer) player).getHandle().setAbsorptionHearts(0);
        }
        if (before != null && before[1] == 1) {
            player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
        }
        // 着替えで付くのはスポーンの中だけ (ユーザーの指定。外で着替えても付かない)
        if (nonce != 0 && plugin.getPitSpawn().isProtected(player.getLocation())) {
            respawnEnchants(player, pants);
        }
    }

    // ---------------------------------------------------------------- Eggs / Creative / TNT / Excess

    /** スポーンでもらう数 (I / II / III) とキルでもらう数。 */
    private static final int[] EGGS_SPAWN = {8, 8, 16};
    private static final int[] EGGS_KILL = {4, 8, 16};
    private static final int[] PLANKS_SPAWN = {16, 32, 48};
    private static final int[] PLANKS_KILL = {6, 12, 18};
    private static final int[] TNT_SPAWN = {1, 2, 3};
    private static final int[] TNT_KILL = {1, 1, 2};
    static final String ITEM_KEY = "PitEnchantItem";
    private static final int ITEM_EGG = 1;
    private static final int ITEM_PLANK = 2;
    private static final int ITEM_TNT = 3;
    private static final int ITEM_INSTABOOM = 4;
    private static final int ITEM_SCOOP = 5;
    private static final int[] INSTABOOM_SPAWN = {1, 1, 2};
    private static final int[] INSTABOOM_KILL = {1, 2, 2};
    private static final double INSTABOOM_RADIUS = 4;
    /** Instaboom II / III: この時間内に続けて使うと自分も痛い (録画に無いので推測)。 */
    private static final long INSTABOOM_CONSECUTIVE_MILLIS = 3_000;
    private static final int SCOOP_SPAWN = 3;
    private static final int[] SCOOP_KILL = {1, 2, 3};
    private static final int[] SCOOP_MAX = {6, 9, 12};
    private static final int SCOOPS_PER_SNOWMAN = 3;
    private static final int[] SNOWMAN_SECONDS = {30, 60, 90};
    /** 録画: 雪だるまの体力 25。 */
    private static final double SNOWMAN_HEALTH = 25;
    private static final double SNOWMAN_RANGE = 12;
    /** 雪だるまの雪玉のダメージ (録画に無いので推測、0.5❤)。 */
    private static final double SNOWBALL_DAMAGE = 1;
    private final Map<UUID, Long> instaboomUsed = new HashMap<>();
    /** Snowmen Army の雪だるま → {持ち主, 消える時刻}。 */
    private final Map<UUID, Object[]> snowmen = new HashMap<>();
    /** Creative の木材が消えるまで (30 秒)。 */
    public static final long PLANK_TICKS = 30 * 20;
    private static final int TNT_FUSE_TICKS = 30;
    private static final double TNT_RADIUS = 3;
    private static final String TNT_METADATA = "pitEnchantTnt";

    /** Excess: 回復アイテムを持てる数に足す分 (+1 / +2 / +3)。 */
    public int excess(Player player) {
        Integer excess = pants(player).get("overheal_enchant");
        return excess == null ? 0 : Math.min(excess, 3);
    }

    /** パンツのエンチャントのアイテム (録画: どれも説明の 1 行目が §7Enchant item)。 */
    private static ItemStack enchantItem(int kind, int amount) {
        ItemStack item;
        String name = null;
        java.util.List<String> lore = new java.util.ArrayList<>(java.util.Collections.singletonList("§7Enchant item"));
        switch (kind) {
            case ITEM_EGG:
                item = new ItemStack(Material.EGG, amount);
                break;
            case ITEM_PLANK:
                item = new ItemStack(Material.WOOD, amount);
                break;
            case ITEM_INSTABOOM:
                // 録画 2026-10-14_23-00-44.mcrc
                item = new ItemStack(Material.TNT, amount);
                name = "§cInsta Boom TNT";
                break;
            case ITEM_SCOOP:
                // 録画 2026-10-14_22-44-09.mcrc
                item = new ItemStack(Material.SNOW_BLOCK, amount);
                name = "§bSnowman Scoop";
                lore.addAll(java.util.Arrays.asList("§7Place this block to spawn a", "§7snowman. Consumes 3 blocks per", "§7snowman."));
                break;
            default:
                item = new ItemStack(Material.TNT, amount);
                name = "§cTNT";
                break;
        }
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (name != null) {
            meta.setDisplayName(name);
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return com.pitremake.util.NbtUtil.setInt(item, ITEM_KEY, kind);
    }

    private int count(Player player, int kind) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (itemKind(item) == kind) {
                total += item.getAmount();
            }
        }
        return total;
    }

    private static int itemKind(ItemStack item) {
        return item == null || item.getType() == Material.AIR ? 0 : com.pitremake.util.NbtUtil.getInt(item, ITEM_KEY);
    }

    /** Creative の木材か (置ける・30 秒で消える)。 */
    public static boolean isCreativePlank(ItemStack item) {
        return itemKind(item) == ITEM_PLANK;
    }

    private static void give(Player player, ItemStack item) {
        if (item.getAmount() <= 0) {
            return;
        }
        for (ItemStack leftover : player.getInventory().addItem(item).values()) {
            player.getWorld().dropItem(player.getLocation(), leftover);
        }
    }

    /**
     * Eggs / Creative / TNT のアイテムをスポーンでもらう数に戻す (増えた分は消え、足りない分はもらう)。
     * 死んで戻ったとき・/spawn したとき (ユーザーの指定: /spawn で元に戻る)。
     */
    public void resetEnchantItems(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (itemKind(contents[i]) != 0) {
                player.getInventory().setItem(i, null);
            }
        }
        Map<String, Integer> pants = pants(player);
        Integer eggs = pants.get("eggs");
        if (eggs != null) {
            give(player, enchantItem(ITEM_EGG, EGGS_SPAWN[index(eggs)]));
        }
        Integer creative = pants.get("wood_blocks");
        if (creative != null) {
            give(player, enchantItem(ITEM_PLANK, PLANKS_SPAWN[index(creative)]));
        }
        Integer tnt = pants.get("tnt");
        if (tnt != null) {
            give(player, enchantItem(ITEM_TNT, TNT_SPAWN[index(tnt)]));
        }
        Integer instaboom = pants.get("instaboom_tnt");
        if (instaboom != null) {
            give(player, enchantItem(ITEM_INSTABOOM, INSTABOOM_SPAWN[index(instaboom)]));
        }
        if (pants.containsKey("snowmen")) {
            give(player, enchantItem(ITEM_SCOOP, SCOOP_SPAWN));
        }
    }

    /** /spawn したとき: Eggs / Creative / TNT のアイテムを元に戻し、Excess で増えた回復アイテムを消す。 */
    public void onSpawnCommand(Player player) {
        resetEnchantItems(player);
        plugin.getPerkManager().trimExcessHealing(player);
    }

    /** パンツのエンチャントでもらったアイテム (Eggs / Creative / TNT) は捨てられない。 */
    @EventHandler(ignoreCancelled = true)
    public void onDrop(org.bukkit.event.player.PlayerDropItemEvent event) {
        if (itemKind(event.getItemDrop().getItemStack()) != 0) {
            event.setCancelled(true);
        }
    }

    /** Eggs の卵からはヒヨコが出ない (どの卵も)。 */
    @EventHandler
    public void onEggThrow(org.bukkit.event.player.PlayerEggThrowEvent event) {
        event.setHatching(false);
    }

    /**
     * TNT: 置くとすぐ点火した TNT になり、1.5 秒後に爆発する (ブロックは置かない)。
     * 半径 3 ブロックの自分以外に 1 / 1.5 / 2❤ (防具を無視)。
     */
    @EventHandler(priority = org.bukkit.event.EventPriority.LOW, ignoreCancelled = true)
    public void onPlaceTnt(org.bukkit.event.block.BlockPlaceEvent event) {
        int kind = itemKind(event.getItemInHand());
        if (kind == ITEM_INSTABOOM) {
            event.setCancelled(true);
            instaboom(event.getPlayer(), event.getBlockPlaced().getLocation());
            return;
        }
        if (kind == ITEM_SCOOP) {
            event.setCancelled(true);
            placeScoop(event.getPlayer(), event.getBlockPlaced().getLocation());
            return;
        }
        if (kind != ITEM_TNT) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        Integer level = pants(player).get("tnt");
        // スポーンと真ん中のブロックを置けない範囲には置けない
        if (level == null || plugin.getPitSpawn().isProtected(event.getBlockPlaced().getLocation())
                || plugin.getPitMap().isCenter(event.getBlockPlaced().getLocation())) {
            return;
        }
        ItemStack hand = player.getItemInHand();
        if (hand.getAmount() > 1) {
            hand.setAmount(hand.getAmount() - 1);
            player.setItemInHand(hand);
        } else {
            player.setItemInHand(null);
        }
        org.bukkit.Location at = event.getBlockPlaced().getLocation().add(0.5, 0, 0.5);
        org.bukkit.entity.TNTPrimed tnt = at.getWorld().spawn(at, org.bukkit.entity.TNTPrimed.class);
        tnt.setFuseTicks(TNT_FUSE_TICKS);
        tnt.setMetadata(TNT_METADATA, new org.bukkit.metadata.FixedMetadataValue(plugin,
                new Object[]{player.getUniqueId(), EnchantNumbers.hearts(enchant("tnt"), level, 0)}));
        EnchantEffects.worldSound(at, "game.tnt.primed", 1.0f, 1.0f);
    }

    /**
     * TNT の爆発: 壊せるのは置かれた岩盤・丸石・Creative の木材だけ (黒曜石は壊れない、ユーザーの指定)。
     * ダメージは爆発のダメージの代わりにここで与える。
     */
    @EventHandler(ignoreCancelled = true)
    public void onTntExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        Object[] data = tntData(event.getEntity());
        if (data == null) {
            return;
        }
        event.setYield(0);
        Iterator<org.bukkit.block.Block> blocks = event.blockList().iterator();
        while (blocks.hasNext()) {
            org.bukkit.block.Block block = blocks.next();
            Material type = block.getType();
            boolean breakable = (type == Material.BEDROCK || type == Material.COBBLESTONE || type == Material.WOOD)
                    && plugin.getShopItemListener().isPlaced(block.getLocation());
            if (breakable) {
                plugin.getShopItemListener().forgetPlaced(block.getLocation());
            } else {
                blocks.remove();
            }
        }
        Player owner = plugin.getServer().getPlayer((UUID) data[0]);
        double damage = (Double) data[1];
        org.bukkit.Location at = event.getLocation();
        for (Player target : at.getWorld().getPlayers()) {
            if (target.isDead() || target.getUniqueId().equals(data[0]) || target.getLocation().distanceSquared(at) > TNT_RADIUS * TNT_RADIUS
                    || plugin.getPitSpawn().isProtected(target.getLocation())) {
                continue;
            }
            if (owner != null) {
                plugin.getKillListener().recordAttack(owner, target, damage);
            }
            if (target.getHealth() - damage > 0) {
                target.setHealth(target.getHealth() - damage);
                target.playEffect(org.bukkit.EntityEffect.HURT);
            } else {
                // 倒れるときは少し前に攻撃した人 (TNT を置いた人) のキルになる
                target.damage(target.getHealth() * 100);
            }
        }
    }

    /**
     * Instaboom (録画 2026-10-14_23-00-44.mcrc): 置くとその場ですぐ爆発する (ブロックは置かない)。
     * 置いたブロックの真ん中に largeexplode 1 個 (速さ 1) と random.explode (1.0 / 1.49)。
     * 半径 4 ブロックの自分以外に 0.25 / 0.5 / 0.75❤。II / III は続けて使うと自分も同じだけ痛い。
     */
    private void instaboom(Player player, org.bukkit.Location block) {
        Integer level = pants(player).get("instaboom_tnt");
        if (level == null || plugin.getPitSpawn().isProtected(block) || plugin.getPitMap().isCenter(block)) {
            return;
        }
        consumeHand(player, 1);
        org.bukkit.Location at = block.clone().add(0.5, 0.5, 0.5);
        net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles boom = new net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles(
                net.minecraft.server.v1_8_R3.EnumParticle.EXPLOSION_LARGE, true, (float) at.getX(), (float) at.getY(), (float) at.getZ(),
                0, 0, 0, 1, 1);
        for (Player viewer : at.getWorld().getPlayers()) {
            if (viewer.getLocation().distanceSquared(at) <= 64 * 64) {
                com.pitremake.util.PlayerPackets.send(viewer, boom);
            }
        }
        EnchantEffects.worldSound(at, "random.explode", 1.0f, 94 / 63f);
        double damage = EnchantNumbers.hearts(enchant("instaboom_tnt"), level, 0);
        for (Player target : at.getWorld().getPlayers()) {
            if (target.equals(player) || target.isDead() || target.getLocation().distanceSquared(at) > INSTABOOM_RADIUS * INSTABOOM_RADIUS
                    || plugin.getPitSpawn().isProtected(target.getLocation())) {
                continue;
            }
            hurt(player, target, damage);
        }
        long now = System.currentTimeMillis();
        Long last = instaboomUsed.put(player.getUniqueId(), now);
        if (level >= 2 && last != null && now - last <= INSTABOOM_CONSECUTIVE_MILLIS) {
            // Consecutive uses hurt you!
            hurt(null, player, damage);
        }
    }

    /** 防具を無視して damage 与える (倒れるときは attacker のキル)。 */
    private void hurt(Player attacker, Player target, double damage) {
        if (attacker != null) {
            plugin.getKillListener().recordAttack(attacker, target, damage);
        }
        if (target.getHealth() - damage > 0) {
            target.setHealth(target.getHealth() - damage);
            target.playEffect(org.bukkit.EntityEffect.HURT);
        } else {
            target.damage(target.getHealth() * 100);
        }
    }

    private static void consumeHand(Player player, int amount) {
        ItemStack hand = player.getItemInHand();
        if (hand.getAmount() > amount) {
            hand.setAmount(hand.getAmount() - amount);
            player.setItemInHand(hand);
        } else {
            player.setItemInHand(null);
        }
    }

    // ---------------------------------------------------------------- Hunt the Hunter / Divine Miracle / The Punch / Pit Blob

    /** Hunt the Hunter: 相手の Bounty Hunter の増加に掛ける数 (I = 0.5、II / III = 0)。 */
    public double bountyHunterFactor(Player victim, Player attacker) {
        if (MysticAccess.disabled(victim, attacker)) {
            return 1;
        }
        Integer level = pants(victim).get("counter_bounty_hunter");
        return level == null ? 1 : level == 1 ? 0.5 : 0;
    }

    /** Divine Miracle: 15 / 30 / 45% で死んでも持ち物の Lives が減らない (死ぬたびに 1 回引く)。 */
    public boolean rollDivineMiracle(Player player) {
        Integer level = pants(player).get("chance_dont_lose_life");
        return level != null && java.util.concurrent.ThreadLocalRandom.current().nextDouble()
                < EnchantNumbers.percent(enchant("chance_dont_lose_life"), level, 0);
    }

    private final Map<UUID, Long> punchUsed = new HashMap<>();
    private static final long[] PUNCH_COOLDOWN_MILLIS = {30_000, 25_000, 20_000};

    /** The Punch: 殴った相手を Escape Pod と同じく真上に打ち上げる (30 / 25 / 20 秒に 1 回)。 */
    private void punch(Player attacker, Player victim, int level) {
        if (!ready(punchUsed, attacker.getUniqueId(), PUNCH_COOLDOWN_MILLIS[index(level)])) {
            return;
        }
        org.bukkit.Location at = victim.getLocation();
        net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles boom = new net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles(
                net.minecraft.server.v1_8_R3.EnumParticle.EXPLOSION_HUGE, true, (float) at.getX(), (float) at.getY() + 0.5f, (float) at.getZ(),
                0, 0, 0, 0, 1);
        for (Player viewer : at.getWorld().getPlayers()) {
            if (viewer.getLocation().distanceSquared(at) <= 64 * 64) {
                com.pitremake.util.PlayerPackets.send(viewer, boom);
            }
        }
        EnchantEffects.worldSound(at, "random.explode", 1.0f, 56 / 63f);
        // 普通のノックバックの後 (次の tick) に真上へ
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (victim.isValid() && !victim.isDead()) {
                victim.setVelocity(new org.bukkit.util.Vector(0, ESCAPE_POD_LAUNCH, 0));
            }
        });
    }

    /** Pit Blob: 持ち主 → スライム。 */
    private final Map<UUID, org.bukkit.entity.Slime> blobs = new HashMap<>();
    private static final int BLOB_MAX_SIZE = 7;
    /** キルで大きくなる確率 (I / II / III。ユーザーの指定: レベルでの違いは大きくなりやすさ、数値は推測)。 */
    private static final double[] BLOB_GROW_CHANCE = {0.34, 0.67, 1.0};
    private static final String BLOB_METADATA = "pitBlob";

    /** キルしたとき: The Blob が居なければ出し、居れば (確率で) 大きくなって体力も戻る。 */
    private void blobOnKill(Player killer) {
        Integer level = pants(killer).get("the_blob");
        if (level == null) {
            return;
        }
        org.bukkit.entity.Slime blob = blobs.get(killer.getUniqueId());
        if (blob == null || !blob.isValid()) {
            blob = killer.getWorld().spawn(killer.getLocation(), org.bukkit.entity.Slime.class);
            blob.setSize(1);
            blob.setMetadata(BLOB_METADATA, new org.bukkit.metadata.FixedMetadataValue(plugin, killer.getUniqueId()));
            blob.setCustomName("§aThe Blob");
            blob.setCustomNameVisible(true);
            blobs.put(killer.getUniqueId(), blob);
        } else if (blob.getSize() < BLOB_MAX_SIZE && java.util.concurrent.ThreadLocalRandom.current().nextDouble() < BLOB_GROW_CHANCE[index(level)]) {
            blob.setSize(blob.getSize() + 1);
        }
        blob.setHealth(blob.getMaxHealth());
    }

    /** The Blob の狙う相手 (近くの敵、居なければ持ち主について行く)。7 tick ごと。 */
    private void tickBlobs() {
        Iterator<Map.Entry<UUID, org.bukkit.entity.Slime>> it = blobs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, org.bukkit.entity.Slime> entry = it.next();
            org.bukkit.entity.Slime blob = entry.getValue();
            Player owner = plugin.getServer().getPlayer(entry.getKey());
            if (!blob.isValid() || owner == null || owner.isDead() || !pants(owner).containsKey("the_blob")) {
                blob.remove();
                it.remove();
                continue;
            }
            Player target = null;
            double best = 10 * 10;
            for (Player candidate : blob.getWorld().getPlayers()) {
                double distance = candidate.getLocation().distanceSquared(blob.getLocation());
                if (candidate.equals(owner) || candidate.isDead() || distance > best
                        || plugin.getPitSpawn().isProtected(candidate.getLocation())) {
                    continue;
                }
                best = distance;
                target = candidate;
            }
            if (target == null && blob.getLocation().distanceSquared(owner.getLocation()) > 20 * 20) {
                blob.teleport(owner.getLocation());
            }
            // 敵が居なければ持ち主を狙わせてついて来させる (持ち主へのダメージは消す)
            ((org.bukkit.craftbukkit.v1_8_R3.entity.CraftSlime) blob).getHandle().setGoalTarget(
                ((CraftPlayer) (target != null ? target : owner)).getHandle(),
                org.bukkit.event.entity.EntityTargetEvent.TargetReason.CUSTOM, false);
        }
    }

    private UUID blobOwner(org.bukkit.entity.Entity entity) {
        for (org.bukkit.metadata.MetadataValue value : entity.getMetadata(BLOB_METADATA)) {
            if (value.getOwningPlugin() == plugin) {
                return (UUID) value.value();
            }
        }
        return null;
    }

    /** The Blob の攻撃: 持ち主には当たらない。敵へのダメージは持ち主の攻撃として覚える。 */
    @EventHandler(priority = org.bukkit.event.EventPriority.LOW, ignoreCancelled = true)
    public void onBlobAttack(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        UUID owner = blobOwner(event.getDamager());
        if (owner == null || !(event.getEntity() instanceof Player)) {
            return;
        }
        Player victim = (Player) event.getEntity();
        if (victim.getUniqueId().equals(owner) || plugin.getPitSpawn().isProtected(victim.getLocation())) {
            event.setCancelled(true);
            return;
        }
        Player ownerPlayer = plugin.getServer().getPlayer(owner);
        if (ownerPlayer != null) {
            plugin.getKillListener().recordAttack(ownerPlayer, victim, event.getDamage());
        }
    }

    /** The Blob は分裂しない・何も落とさない。 */
    @EventHandler(ignoreCancelled = true)
    public void onBlobSplit(org.bukkit.event.entity.SlimeSplitEvent event) {
        if (blobOwner(event.getEntity()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlobDeath(org.bukkit.event.entity.EntityDeathEvent event) {
        UUID owner = blobOwner(event.getEntity());
        if (owner != null) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            blobs.remove(owner);
        }
    }

    // ---------------------------------------------------------------- Purple Gold / Lodbrok

    /** Purple Gold: 置かれた黒曜石を壊すと +7 / 11 / 15g、II / III は Regen III (2 / 3 秒)。count は壊した数。 */
    public void onObsidianBroken(Player player, int count) {
        Integer level = pants(player).get("gold_break_obsidian");
        if (level == null || count <= 0) {
            return;
        }
        int[] gold = {7, 11, 15};
        plugin.getGoldManager().add(player.getUniqueId(), gold[index(level)] * count);
        plugin.getSidebar().update(player);
        if (level >= 2) {
            effect(player, PotionEffectType.REGENERATION, (level >= 3 ? 3 : 2) * 20, 2);
        }
    }

    /** 防具が落ちる確率 (普通は 30%、Lodbrok で 40 / 55 / 75%)。 */
    private static final double ARMOR_DROP_CHANCE = 0.30;

    /**
     * キルしたとき: 相手の着ているダイヤの防具のうち 1 つを選び、確率 (pit.wiki: 普通は 30%) で落とす
     * (前は 1 つずつ判定していて落ちやすすぎた: ユーザーの指摘)。キルした人が Lodbrok を履いていれば確率が上がる。
     * Archangel Chestplate (Golden Helmet / Armageddon Boots も)・ミスティック・Beastmode のヘルメットは落とさない。
     * 持ち物の処理 (死ぬと無くなる) の前に呼ぶ。
     */
    public void armorDrops(Player killer, Player victim) {
        Integer lodbrok = pants(killer).get("increase_armor_drops");
        double chance = lodbrok == null ? ARMOR_DROP_CHANCE : EnchantNumbers.percent(enchant("increase_armor_drops"), lodbrok, 0);
        java.util.List<ItemStack> candidates = new java.util.ArrayList<>();
        for (ItemStack armor : victim.getInventory().getArmorContents()) {
            if (canDropOnKill(armor)) {
                candidates.add(armor);
            }
        }
        java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();
        if (!candidates.isEmpty() && random.nextDouble() < chance) {
            victim.getWorld().dropItemNaturally(victim.getLocation(), candidates.get(random.nextInt(candidates.size())).clone());
        }
    }

    /** キルで落ちる防具: 普通のダイヤの防具だけ。 */
    static boolean canDropOnKill(ItemStack armor) {
        if (armor == null || !armor.getType().name().startsWith("DIAMOND_") || MysticItems.isMystic(armor)) {
            return false;
        }
        for (com.pitremake.item.RewardItems.Type type : com.pitremake.item.RewardItems.Type.values()) {
            if (com.pitremake.item.RewardItems.is(armor, type)) {
                return false;
            }
        }
        // Beastmode のヘルメットは死んでも残るので落とさない (増えてしまう)
        return !(armor.hasItemMeta() && "§aBeastmode Helmet".equals(armor.getItemMeta().getDisplayName()));
    }

    // ---------------------------------------------------------------- Self-checkout

    private static final int SELF_CHECKOUT_BOUNTY = 5_000;

    /**
     * Self-checkout: バウンティが 5,000g 以上で履いている (履いた) ら、バウンティを消して +2,000 / 3,000 / 5,000g、
     * パンツの Lives が 1 減る (0 で壊れる)。会計の音は録画に無いので自作 (レジのピッ・チーン)。
     */
    private void selfCheckout(Player player) {
        UUID id = player.getUniqueId();
        if (plugin.getBountyManager().get(id) < SELF_CHECKOUT_BOUNTY) {
            return;
        }
        Integer level = pants(player).get("max_bounty_self_claim");
        if (level == null) {
            return;
        }
        int[] gold = {2_000, 3_000, 5_000};
        int reward = gold[index(level)];
        plugin.getBountyManager().set(id, 0);
        plugin.getGoldManager().add(id, reward);
        ItemStack leggings = player.getInventory().getLeggings();
        com.pitremake.item.MysticData data = com.pitremake.item.MysticData.read(leggings);
        int lives = data.getLives() - 1;
        if (lives <= 0) {
            player.getInventory().setLeggings(null);
            player.playSound(player.getLocation(), "random.break", 1.0f, 1.0f);
        } else {
            data.setLives(lives);
            player.getInventory().setLeggings(MysticItems.applyDisplay(data.write(leggings), plugin.getEnchantRegistry()));
        }
        player.sendMessage("§6§lSELF-CHECKOUT! §7Cleared your bounty for §6+" + String.format(java.util.Locale.US, "%,d", reward) + "g§7!");
        checkoutSound(player);
        plugin.getSidebar().update(player);
    }

    /** レジの音 (自作): ピッ (高い note.pling) → チーン (random.orb と note.harp の和音)。 */
    private void checkoutSound(Player player) {
        player.playSound(player.getLocation(), "note.pling", 1.0f, 2.0f);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.playSound(player.getLocation(), "note.pling", 1.0f, 1.6f);
            }
        }, 3);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.playSound(player.getLocation(), "random.orb", 1.0f, 1.2f);
                player.playSound(player.getLocation(), "note.harp", 1.0f, 1.5f);
                player.playSound(player.getLocation(), "note.harp", 1.0f, 2.0f);
            }
        }, 7);
    }

    // ---------------------------------------------------------------- Escape Pod / Phoenix

    /** この人生で Escape Pod / Phoenix II を使った人。 */
    private final java.util.Set<UUID> escapePodUsed = new java.util.HashSet<>();
    private final java.util.Set<UUID> phoenixUsed = new java.util.HashSet<>();
    /** Phoenix III: 重なった +15% と終わる時刻。 */
    private final Map<UUID, double[]> phoenixBuff = new HashMap<>();
    /** 録画: Escape Pod の上向きの速さ。 */
    private static final double ESCAPE_POD_LAUNCH = 3.9;
    private static final double ESCAPE_POD_BELOW = 4;
    /** Escape Pod でダメージを与える範囲 (録画に無いので推測)。 */
    private static final double ESCAPE_POD_RADIUS = 5;

    /**
     * 最後のダメージが決まった後 (DamageCalculator から): 攻撃を止めるなら true。
     * Escape Pod (録画 2026-10-14_23-04-53.mcrc): 2❤ 未満になる一撃は当たらず、真上に打ち上がる (速さ 3.9)。
     *   その場所に hugeexplosion と random.explode (1.0 / 0.89)、近くの敵に 1 / 2 / 3❤、Regen II / III / IV (20 / 25 / 30 秒)。
     *   1 回発動したらスポーンに戻るまで (死ぬか、生きたままスポーンに入るまで) 発動しない
     * Phoenix (録画 2026-10-14_23-05-32.mcrc): 倒れる一撃は当たらず、パンツの Lives を 1 使って体力が全部戻る。
     *   mob.bat.death (1.0 / 0.49) と頭の高さに lava 30 個 (散らばり 0.2 / 0.1 / 0.2、速さ 4)。
     *   スポーンから出て戻るまでに 1 回だけ (ユーザーの指定)。III は +15% (10 秒)
     * どちらも落下・溶岩・自分の矢など自分で減らした体力でも発動する。Somber の人の一撃では
     * (Armageddon Boots を履いていても) 発動しない (ユーザーの指定)。
     */
    public boolean interceptHit(DamageContext context, double healthLeft) {
        Player victim = context.victim;
        org.bukkit.event.entity.EntityDamageEvent.DamageCause cause = context.event.getCause();
        if (cause == org.bukkit.event.entity.EntityDamageEvent.DamageCause.VOID
                || cause == org.bukkit.event.entity.EntityDamageEvent.DamageCause.SUICIDE) {
            return false;
        }
        // Somber の人は見えない扱い: Somber の一撃では Armageddon Boots を履いていても発動しない (ユーザーの指定)
        if (context.byPlayer() && MysticAccess.wearsSomber(context.attacker)) {
            return false;
        }
        // 落下・溶岩・自分の矢など自分で減らした体力 (attacker が居ない) でも発動する
        if (MysticAccess.disabled(victim, context.byPlayer() ? context.attacker : null)) {
            return false;
        }
        Map<String, Integer> pants = pants(victim);
        UUID id = victim.getUniqueId();
        Integer phoenix = pants.get("phoenix");
        // Phoenix はスポーンから出て戻るまでに 1 回だけ (ユーザーの指定。どのレベルも)
        if (healthLeft <= 0 && phoenix != null && !phoenixUsed.contains(id)) {
            phoenix(victim, phoenix);
            return true;
        }
        Integer pod = pants.get("escape_pod");
        if (healthLeft < ESCAPE_POD_BELOW && pod != null && escapePodUsed.add(id)) {
            escapePod(victim, context.attacker, pod);
            return true;
        }
        return false;
    }

    private void escapePod(Player player, Player attacker, int level) {
        org.bukkit.Location at = player.getLocation();
        net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles boom = new net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles(
                net.minecraft.server.v1_8_R3.EnumParticle.EXPLOSION_HUGE, true, (float) at.getX(), (float) at.getY() + 0.5f, (float) at.getZ(),
                0, 0, 0, 0, 1);
        for (Player viewer : at.getWorld().getPlayers()) {
            if (viewer.getLocation().distanceSquared(at) <= 64 * 64) {
                com.pitremake.util.PlayerPackets.send(viewer, boom);
            }
        }
        EnchantEffects.worldSound(at, "random.explode", 1.0f, 56 / 63f);
        int[] regenSeconds = {20, 25, 30};
        effect(player, PotionEffectType.REGENERATION, regenSeconds[index(level)] * 20, Math.min(level, 3));
        double damage = EnchantNumbers.hearts(enchant("escape_pod"), level, 0);
        for (Player target : at.getWorld().getPlayers()) {
            // Somber (と Venom の毒) の人には当たらない。自分が Armageddon Boots を履いていれば Somber の人にも当たる
            if (target.equals(player) || target.isDead() || target.getLocation().distanceSquared(at) > ESCAPE_POD_RADIUS * ESCAPE_POD_RADIUS
                    || plugin.getPitSpawn().isProtected(target.getLocation()) || MysticAccess.disabled(player, target)) {
                continue;
            }
            hurt(player, target, damage);
        }
        // 当たらなかった一撃のノックバックの代わりに真上へ (次の tick)
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isValid() && !player.isDead()) {
                player.setVelocity(new org.bukkit.util.Vector(0, ESCAPE_POD_LAUNCH, 0));
            }
        });
    }

    private void phoenix(Player player, int level) {
        UUID id = player.getUniqueId();
        ItemStack leggings = player.getInventory().getLeggings();
        com.pitremake.item.MysticData data = com.pitremake.item.MysticData.read(leggings);
        int lives = data.getLives() - 1;
        if (lives <= 0) {
            // 最後の Life を使ったパンツは無くなる
            player.getInventory().setLeggings(null);
        } else {
            data.setLives(lives);
            player.getInventory().setLeggings(MysticItems.applyDisplay(data.write(leggings), plugin.getEnchantRegistry()));
        }
        phoenixUsed.add(id);
        if (level >= 3) {
            double[] buff = phoenixBuff.get(id);
            double stacks = buff != null && buff[1] > System.currentTimeMillis() ? buff[0] + 1 : 1;
            phoenixBuff.put(id, new double[]{stacks, System.currentTimeMillis() + 10_000});
        }
        player.setHealth(player.getMaxHealth());
        org.bukkit.Location head = player.getLocation().add(0, 1.7, 0);
        net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles lava = new net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles(
                net.minecraft.server.v1_8_R3.EnumParticle.LAVA, true, (float) head.getX(), (float) head.getY(), (float) head.getZ(),
                0.2f, 0.1f, 0.2f, 4, 30);
        for (Player viewer : head.getWorld().getPlayers()) {
            if (viewer.getLocation().distanceSquared(head) <= 64 * 64) {
                com.pitremake.util.PlayerPackets.send(viewer, lava);
            }
        }
        EnchantEffects.worldSound(player.getLocation(), "mob.bat.death", 1.0f, 31 / 63f);
    }

    /** Phoenix III の重なった増加 (+15% × 回数、10 秒)。 */
    private double phoenixDamage(Player player) {
        double[] buff = phoenixBuff.get(player.getUniqueId());
        return buff != null && buff[1] > System.currentTimeMillis() ? 0.15 * buff[0] : 0;
    }

    // ---------------------------------------------------------------- Snowmen Army

    /**
     * Snowmen Army (録画 2026-10-14_22-44-09.mcrc): Snowman Scoop (雪ブロック) を置くと 3 個使って雪だるまが出る。
     * 雪だるまは 30 / 60 / 90 秒いて、名前は §b残り秒数s [レベル] 持ち主 (約 0.35 秒ごとに更新)、体力 25。
     * 近くの敵 (持ち主以外) に雪玉を投げて守る。持ち主が死ぬと消える。
     */
    private void placeScoop(Player player, org.bukkit.Location block) {
        Integer level = pants(player).get("snowmen");
        if (level == null || plugin.getPitSpawn().isProtected(block)) {
            return;
        }
        if (count(player, ITEM_SCOOP) < SCOOPS_PER_SNOWMAN) {
            player.sendMessage("§cYou need 3 scoops to spawn a snowman!");
            return;
        }
        int left = SCOOPS_PER_SNOWMAN;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && left > 0; i++) {
            if (itemKind(contents[i]) != ITEM_SCOOP) {
                continue;
            }
            int take = Math.min(left, contents[i].getAmount());
            left -= take;
            if (take >= contents[i].getAmount()) {
                player.getInventory().setItem(i, null);
            } else {
                contents[i].setAmount(contents[i].getAmount() - take);
                player.getInventory().setItem(i, contents[i]);
            }
        }
        org.bukkit.entity.Snowman snowman = block.getWorld().spawn(block.clone().add(0.5, 0, 0.5), org.bukkit.entity.Snowman.class);
        snowman.setMaxHealth(SNOWMAN_HEALTH);
        snowman.setHealth(SNOWMAN_HEALTH);
        snowman.setCustomNameVisible(true);
        long until = System.currentTimeMillis() + SNOWMAN_SECONDS[index(level)] * 1000L;
        snowmen.put(snowman.getUniqueId(), new Object[]{player.getUniqueId(), until, snowman});
        updateSnowman(snowman, player, until);
    }

    private void updateSnowman(org.bukkit.entity.Snowman snowman, Player owner, long until) {
        double left = Math.max(0, (until - System.currentTimeMillis()) / 1000.0);
        snowman.setCustomName(String.format(java.util.Locale.US, "§b%.1fs ", left) + tag(owner).replace(" §7", " "
                + plugin.getRankManager().nameColor(owner.getUniqueId())));
    }

    /** 雪だるまの名前の更新・狙う相手・時間切れ (7 tick ごと)。 */
    private void tickSnowmen() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Object[]>> it = snowmen.entrySet().iterator();
        while (it.hasNext()) {
            Object[] data = it.next().getValue();
            org.bukkit.entity.Snowman snowman = (org.bukkit.entity.Snowman) data[2];
            Player owner = plugin.getServer().getPlayer((UUID) data[0]);
            if (!snowman.isValid() || owner == null || owner.isDead() || now >= (Long) data[1]) {
                snowman.remove();
                it.remove();
                continue;
            }
            updateSnowman(snowman, owner, (Long) data[1]);
            Player target = null;
            double best = SNOWMAN_RANGE * SNOWMAN_RANGE;
            for (Player candidate : snowman.getWorld().getPlayers()) {
                double distance = candidate.getLocation().distanceSquared(snowman.getLocation());
                if (candidate.equals(owner) || candidate.isDead() || distance > best
                        || plugin.getPitSpawn().isProtected(candidate.getLocation())) {
                    continue;
                }
                best = distance;
                target = candidate;
            }
            snowman.setTarget(target);
        }
    }

    /** 持ち主の雪だるまを消す (死んだとき)。 */
    private void removeSnowmen(Player owner) {
        Iterator<Map.Entry<UUID, Object[]>> it = snowmen.entrySet().iterator();
        while (it.hasNext()) {
            Object[] data = it.next().getValue();
            if (owner.getUniqueId().equals(data[0])) {
                ((org.bukkit.entity.Snowman) data[2]).remove();
                it.remove();
            }
        }
    }

    /** 雪だるまの雪玉: 持ち主の敵に 0.5❤ (持ち主には当たらない)。 */
    @EventHandler(priority = org.bukkit.event.EventPriority.LOW, ignoreCancelled = true)
    public void onSnowball(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof org.bukkit.entity.Snowball) || !(event.getEntity() instanceof Player)) {
            return;
        }
        org.bukkit.projectiles.ProjectileSource source = ((org.bukkit.entity.Snowball) event.getDamager()).getShooter();
        if (!(source instanceof org.bukkit.entity.Snowman)) {
            return;
        }
        Object[] data = snowmen.get(((org.bukkit.entity.Snowman) source).getUniqueId());
        Player victim = (Player) event.getEntity();
        if (data == null || victim.getUniqueId().equals(data[0])) {
            event.setCancelled(true);
            return;
        }
        event.setDamage(SNOWBALL_DAMAGE);
        Player owner = plugin.getServer().getPlayer((UUID) data[0]);
        if (owner != null) {
            plugin.getKillListener().recordAttack(owner, victim, SNOWBALL_DAMAGE);
        }
    }

    /** 雪だるまは雪を残さない。 */
    @EventHandler(ignoreCancelled = true)
    public void onSnowTrail(org.bukkit.event.block.EntityBlockFormEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Snowman) {
            event.setCancelled(true);
        }
    }

    /** 雪だるまは何も落とさない。 */
    @EventHandler
    public void onSnowmanDeath(org.bukkit.event.entity.EntityDeathEvent event) {
        if (snowmen.remove(event.getEntity().getUniqueId()) != null) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }

    /** TNT の爆発そのもののダメージは与えない (上で与える)。 */
    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST, ignoreCancelled = true)
    public void onTntDamage(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (tntData(event.getDamager()) != null) {
            event.setCancelled(true);
        }
    }

    private Object[] tntData(org.bukkit.entity.Entity entity) {
        if (entity == null) {
            return null;
        }
        for (org.bukkit.metadata.MetadataValue value : entity.getMetadata(TNT_METADATA)) {
            if (value.getOwningPlugin() == plugin) {
                return (Object[]) value.value();
            }
        }
        return null;
    }

    /**
     * Gotta go fast を履いている人の足元のエフェクト (録画 2026-10-14_18-27-05.mcrc: 止まっていても 4 tick ごとに
     * explode 2 個、散らばり 0.02、速さ 0.02、足元 +0.12)。周りの全員に見える。
     */
    private void speedTrail() {
        for (org.bukkit.World world : plugin.getServer().getWorlds()) {
            java.util.List<Player> players = world.getPlayers();
            for (Player wearer : players) {
                if (wearer.isDead() || !pants(wearer).containsKey("perma_speed")) {
                    continue;
                }
                org.bukkit.Location at = wearer.getLocation();
                net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles packet = new net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles(
                        net.minecraft.server.v1_8_R3.EnumParticle.EXPLOSION_NORMAL, true, (float) at.getX(), (float) (at.getY() + 0.12),
                        (float) at.getZ(), 0.02f, 0.02f, 0.02f, 0.02f, 2);
                for (Player viewer : players) {
                    if (viewer.getLocation().distanceSquared(at) <= 64 * 64) {
                        com.pitremake.util.PlayerPackets.send(viewer, packet);
                    }
                }
            }
        }
    }

    private PitEnchant enchant(String key) {
        return plugin.getEnchantRegistry().get(key);
    }

    private static Map<String, Integer> pants(Player player) {
        if (MysticAccess.isSuppressed(player)) {
            return Collections.emptyMap();
        }
        return MysticAccess.pantsEnchants(player);
    }

    private static Map<String, Integer> sword(Player player) {
        if (MysticAccess.isSuppressed(player) || MysticAccess.mysticItemsBlocked(player)) {
            return Collections.emptyMap();
        }
        ItemStack held = player.getItemInHand();
        return MysticType.of(held) == MysticType.SWORD && MysticItems.isMystic(held)
                ? MysticData.read(held).getEnchants() : Collections.<String, Integer>emptyMap();
    }

    private static int index(int level) {
        return Math.min(level, 3) - 1;
    }

    private static boolean ready(Map<UUID, Long> used, UUID id, long cooldownMillis) {
        long now = System.currentTimeMillis();
        Long last = used.get(id);
        if (last != null && now - last < cooldownMillis) {
            return false;
        }
        used.put(id, now);
        return true;
    }

    private static void effect(Player player, PotionEffectType type, int ticks, int amplifier) {
        player.addPotionEffect(new PotionEffect(type, ticks, amplifier), true);
    }

    private static void heal(Player player, double amount) {
        if (amount > 0 && !player.isDead()) {
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + amount));
        }
    }

    private String tag(Player player) {
        return PrestigeRules.levelTag(plugin.prestigeOf(player), plugin.levelOf(player))
                + " §7" + player.getName();
    }

    // ---------------------------------------------------------------- 常に

    /** Hearts: 最大体力に足す分 (0.25❤ / 0.5❤ / 1❤)。MegastreakManager.refresh で足す。 */
    public double extraMaxHealth(Player player) {
        Integer hearts = pants(player).get("higher_max_hp");
        return hearts == null ? 0 : EnchantNumbers.hearts(enchant("higher_max_hp"), hearts, 0);
    }

    /** Gotta go fast: 歩く速さ (+4% / +10% / +20%)。MegastreakManager.refresh で合わせる。 */
    public void refresh(Player player) {
        if (plugin.getBotManager().isBot(player)) {
            return;
        }
        Integer fast = pants(player).get("perma_speed");
        // Rush (キルストリーク) の速さは Gotta go fast があると上書きされる (pit.wiki)
        double bonus = fast != null ? EnchantNumbers.percent(enchant("perma_speed"), fast, 0) : plugin.getKillstreakManager().rushSpeed(player);
        float speed = (float) (0.2 * (1 + bonus));
        if (Math.abs(player.getWalkSpeed() - speed) > 1e-4) {
            player.setWalkSpeed(speed);
        }
    }

    private void everySecond(Player player) {
        Map<String, Integer> pants = pants(player);
        nostalgiaGlow(player);

        Integer regen = pants.get("passive_health_regen");
        if (regen != null) {
            // 5 / 4 / 3 秒ごとに 1❤
            int[] every = {5, 4, 3};
            if (seconds % every[index(regen)] == 0) {
                heal(player, 2);
            }
        }
        Integer ooc = pants.get("regen_when_ooc");
        boolean fighting = plugin.getStatsManager().isFighting(player.getUniqueId());
        Boolean before = wasFighting.put(player.getUniqueId(), fighting);
        if (ooc != null) {
            if (!fighting) {
                heal(player, player.getMaxHealth());
            } else if (before != null && !before && ooc >= 2) {
                // II / III: 戦闘に入ったとき Regen IV (1 / 2 秒)
                effect(player, PotionEffectType.REGENERATION, (ooc >= 3 ? 2 : 1) * 20, 3);
            }
        }
    }

    // ---------------------------------------------------------------- 当たったとき

    @Override
    public void modify(DamageContext context) {
        if (!context.byPlayer()) {
            return;
        }
        Player victim = context.victim;
        Player attacker = context.attacker;
        // Renown の Heresy II: 革の防具を着た相手に +5% (Mystic ではないので Somber / Venom に関係なく効く)
        if (plugin.getUpgradeManager().getRenownTier(attacker.getUniqueId(), com.pitremake.renown.RenownUpgrade.HERESY) >= 2
                && wearsLeather(victim)) {
            context.increase(0.05);
        }
        if (!MysticAccess.disabled(victim, attacker)) {
            // Dark Pants の Dark エンチャント (darkDefense) は Somber と一緒に付くので、Somber でも止めない
            defense(context, victim, attacker);
            darkDefense(context, victim, attacker);
        }
        if (MysticAccess.disabled(attacker, victim)) {
            return;
        }
        double phoenixBonus = phoenixDamage(attacker);
        if (phoenixBonus > 0) {
            context.increase(phoenixBonus);
        }
        if (context.melee) {
            darkMelee(context, attacker, victim);
        }
        if (context.melee && !context.isExtraStrike()) {
            melee(context, attacker, victim);
        } else if (context.arrow != null) {
            arrow(context, attacker, victim);
        }
    }

    private void defense(DamageContext context, Player victim, Player attacker) {
        guard(context, victim, attacker);
        Map<String, Integer> pants = pants(victim);
        if (pants.isEmpty()) {
            return;
        }
        UUID id = victim.getUniqueId();
        Integer peroxide = pants.get("regen_when_hit");
        if (peroxide != null && ready(peroxideUsed, id, 1_500)) {
            double amount = EnchantNumbers.hearts(enchant("regen_when_hit"), peroxide, 0);
            context.afterHit(dealt -> heal(victim, amount));
        }
        Integer counter = pants.get("speed_when_hit_few_times");
        if (counter != null) {
            // 4 / 3 / 2 回殴られると Speed II (3 / 5 / 7 秒)
            int[] hits = {4, 3, 2};
            int[] secondsOf = {3, 5, 7};
            int count = hitsTaken.merge(id, 1, Integer::sum);
            if (count >= hits[index(counter)]) {
                hitsTaken.remove(id);
                effect(victim, PotionEffectType.SPEED, secondsOf[index(counter)] * 20, 1);
            }
        }
        Integer fortitude = pants.get("resistance_when_low");
        Integer revitalize = pants.get("regen_speed_when_low");
        Integer superspeed = pants.get("superspeed_when_low");
        if (fortitude == null && revitalize == null && superspeed == null) {
            return;
        }
        double before = victim.getHealth();
        context.afterHit(dealt -> {
            double after = before - dealt - context.getDealtTrueDamage();
            if (after <= 0) {
                return;
            }
            if (fortitude != null && before > LOW_HEALTH && after <= LOW_HEALTH) {
                // Resistance I / II / III (3 / 4 / 4 秒)
                int[] secondsOf = {3, 4, 4};
                effect(victim, PotionEffectType.DAMAGE_RESISTANCE, secondsOf[index(fortitude)] * 20, index(fortitude));
            }
            if (revitalize != null && before > LOW_HEALTH && after <= LOW_HEALTH && ready(revitalizeUsed, id, 45_000)) {
                int[] speedSeconds = {5, 5, 7};
                int[] regenSeconds = {3, 3, 5};
                effect(victim, PotionEffectType.SPEED, speedSeconds[index(revitalize)] * 20, revitalize >= 2 ? 1 : 0);
                effect(victim, PotionEffectType.REGENERATION, regenSeconds[index(revitalize)] * 20, revitalize >= 2 ? 2 : 1);
            }
            if (superspeed != null) {
                double at = superspeed >= 2 ? 8 : LOW_HEALTH;
                if (before > at && after <= at && ready(superspeedUsed, id, 10_000)) {
                    int[] secondsOf = {3, 6, 9};
                    effect(victim, PotionEffectType.SPEED, secondsOf[index(superspeed)] * 20, 2);
                }
            }
        });
    }

    private void melee(DamageContext context, Player attacker, Player victim) {
        rage(context, attacker, victim);
        duelistStrike(context, attacker, victim);
        Map<String, Integer> sword = sword(attacker);
        Integer knockback = sword.get("melee_knockback");
        if (knockback != null) {
            knockback(attacker, victim, knockback);
        }
        if (sword.isEmpty()) {
            return;
        }
        UUID id = attacker.getUniqueId();
        Integer punchLevel = sword.get("melee_launch");
        if (punchLevel != null) {
            punch(attacker, victim, punchLevel);
        }
        Integer speed = sword.get("melee_speed_on_hit");
        if (speed != null) {
            long[] cooldown = {3_000, 2_000, 1_000};
            int[] secondsOf = {5, 7, 9};
            if (ready(speedOnHitUsed, id, cooldown[index(speed)])) {
                effect(attacker, PotionEffectType.SPEED, secondsOf[index(speed)] * 20, 0);
            }
        }
        Integer goldPerHit = sword.get("gold_per_hit");
        if (goldPerHit != null && ready(goldPerHitUsed, id, 1_000)) {
            addGold(attacker, Math.min(goldPerHit, 3));
        }
        Integer goldPerCrit = sword.get("gold_per_crit");
        if (goldPerCrit != null && context.critical) {
            addGold(attacker, 2 * Math.min(goldPerCrit, 3));
        }
        Integer pitpocket = sword.get("pickpocket");
        if (pitpocket != null) {
            long[] cooldown = {25_000, 20_000, 13_000};
            int[] steal = {15, 20, 25};
            if (ready(pitpocketUsed, id, cooldown[index(pitpocket)])) {
                pitpocket(attacker, victim, steal[index(pitpocket)]);
            }
        }
    }

    /**
     * Pitpocket: 殴った相手からゴールドを盗む (相手の持っている分まで減らし、自分は説明文の分もらう)。
     * 殴った人と相手にチャット、その場所でコウモリの音 (ユーザーの指定)。
     */
    private void pitpocket(Player attacker, Player victim, int amount) {
        UUID victimId = victim.getUniqueId();
        double taken = Math.min(amount, plugin.getGoldManager().get(victimId));
        if (taken > 0) {
            plugin.getGoldManager().take(victimId, taken);
            plugin.getSidebar().update(victim);
            victim.sendMessage("§c§lPITPOCKETED! §7by " + tag(attacker) + " §6-" + (int) taken + "g");
        }
        addGold(attacker, amount);
        attacker.sendMessage("§6§lPITPOCKET! §7Stole §6" + amount + "g §7from " + tag(victim) + "§7!");
        EnchantEffects.worldSound(victim.getLocation(), "mob.bat.idle", 1.0f, 1.0f);
    }

    private void addGold(Player player, double amount) {
        plugin.getGoldManager().add(player.getUniqueId(), amount);
        plugin.getSidebar().update(player);
    }

    /**
     * Rage Pants: Do it like the French (Golden Helmet をかぶった相手に +0.5 / 1 / 1.5❤) と
     * Heigh-Ho (Mirror を履いた相手に +1 / +2 / +3 ダメージ。武器の攻撃力に足す: 6.5 → 9.5)。
     */
    private void rage(DamageContext context, Player attacker, Player victim) {
        Map<String, Integer> pants = pants(attacker);
        // Brakes!: 殴った相手の Speed を Speed I に下げる (残り時間はそのまま)
        if (pants.containsKey("brakes")) {
            for (PotionEffect active : victim.getActivePotionEffects()) {
                if (active.getType().equals(PotionEffectType.SPEED) && active.getAmplifier() > 0) {
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, active.getDuration(), 0, active.isAmbient()), true);
                }
            }
        }
        // Combo: Breaching Charge: 4 / 3 / 2 回目ごとに相手の Resistance を消す (コンボは 3.5 秒で途切れる)
        Integer breaching = pants.get("breaching_charge");
        if (breaching != null) {
            long now = System.currentTimeMillis();
            Long last = breachingTimes.put(attacker.getUniqueId(), now);
            if (last == null || now - last > 3_500) {
                breachingStrikes.remove(attacker.getUniqueId());
            }
            int strike = breachingStrikes.merge(attacker.getUniqueId(), 1, Integer::sum);
            int every = 5 - Math.min(breaching, 3);
            if (strike % every == 0) {
                victim.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
                EnchantEffects.worldSound(victim.getLocation(), "mob.guardian.hit", 0.8f, 2.0f);
            }
        }
        Integer french = pants.get("do_it_like_the_french");
        ItemStack helmet = victim.getInventory().getHelmet();
        if (french != null && helmet != null && helmet.getType() == Material.GOLD_HELMET) {
            context.addDamage(EnchantNumbers.hearts(enchant("do_it_like_the_french"), french, 0));
        }
        Integer heighHo = pants.get("heigh_ho");
        if (heighHo != null && pants(victim).containsKey("immune_true_damage")) {
            context.addDamage(Math.min(heighHo, 3));
        }
    }

    /**
     * Knockback: 殴った相手のノックバックを 3 / 6 / 9 ブロック分増やす
     * (バニラの Knockback I の +0.5 で約 3 ブロック。普通のノックバックの後、次の tick に足す)。
     */
    private void knockback(Player attacker, Player victim, int level) {
        double extra = KNOCKBACK_PER_LEVEL * Math.min(level, 3);
        float yaw = attacker.getLocation().getYaw();
        double x = -Math.sin(Math.toRadians(yaw)) * extra;
        double z = Math.cos(Math.toRadians(yaw)) * extra;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (victim.isValid() && !victim.isDead()) {
                victim.setVelocity(victim.getVelocity().add(new org.bukkit.util.Vector(x, 0.1, z)));
            }
        });
    }

    /**
     * 剣でガードしているときのエンチャント (ガードしている人の剣)。
     * Bruiser : 受けるダメージ -0.5 / 1 / 2❤ (最後のダメージから引く)
     * Duelist : 同じ相手の攻撃を 2 回ガードすると、その相手への次の一撃が +20 / 40 / 75% と 0.5 / 1 / 1.5❤ 回復
     */
    private void guard(DamageContext context, Player victim, Player attacker) {
        if (!victim.isBlocking()) {
            return;
        }
        Map<String, Integer> sword = sword(victim);
        Integer bruiser = sword.get("increased_blocking");
        if (bruiser != null) {
            context.reduceFlat(EnchantNumbers.hearts(enchant("increased_blocking"), bruiser, 0));
        }
        if (sword.containsKey("melee_strike_after_block") && context.melee) {
            Map<UUID, Integer> blocks = duelistBlocks.computeIfAbsent(victim.getUniqueId(), k -> new HashMap<>());
            int count = blocks.merge(attacker.getUniqueId(), 1, Integer::sum);
            if (count >= 2) {
                blocks.remove(attacker.getUniqueId());
                duelistEmpowered.computeIfAbsent(victim.getUniqueId(), k -> new java.util.HashSet<>()).add(attacker.getUniqueId());
            }
        }
    }

    private void duelistStrike(DamageContext context, Player attacker, Player victim) {
        java.util.Set<UUID> empowered = duelistEmpowered.get(attacker.getUniqueId());
        if (empowered == null || !empowered.remove(victim.getUniqueId())) {
            return;
        }
        Integer duelist = sword(attacker).get("melee_strike_after_block");
        if (duelist == null) {
            return;
        }
        PitEnchant enchant = enchant("melee_strike_after_block");
        context.increase(EnchantNumbers.percent(enchant, duelist, 0));
        heal(attacker, EnchantNumbers.hearts(enchant, duelist, 0));
    }

    private void arrow(DamageContext context, Player shooter, Player victim) {
        Map<String, Integer> bow = plugin.getEnchantEffects().arrowEnchants(context.arrow);
        if (bow.isEmpty()) {
            return;
        }
        Integer pin = bow.get("pin_down");
        // 最大まで引いた矢 (バニラは最大まで引くとクリティカル。Mega Longbow も)
        if (pin != null && context.arrow.isCritical()) {
            int[] secondsOf = {3, 5, 10};
            pin(shooter, victim, secondsOf[index(pin)]);
        }
        Integer parasite = bow.get("parasite");
        if (parasite != null) {
            // Parasite: 矢を当てると 0.25 / 0.5 / 1❤ 回復
            heal(shooter, EnchantNumbers.hearts(enchant("parasite"), parasite, 0));
        }
        Integer weakness = bow.get("bow_weakness_on_hit");
        if (weakness != null) {
            int[] secondsOf = {6, 11, 16};
            effect(victim, PotionEffectType.WEAKNESS, secondsOf[index(weakness)] * 20, weakness);
        }
        Integer arrows = bow.get("gain_arrows_on_hit");
        if (arrows != null) {
            int[] amount = {1, 3, 8};
            shooter.getInventory().addItem(new ItemStack(Material.ARROW, amount[index(arrows)]));
        }
    }

    /**
     * Pin down: 今付いている Speed と Jump Boost を消し、しばらく付かなくする。消すたびにチャット (ユーザーの指定):
     * 撃った人 §a§lIT'S A PIN! §7you removed Speed from [120] 名前!
     * 相手     §c§lPINNED! §7by [120] 名前, Speed cancelled!
     */
    private void pin(Player shooter, Player victim, int secondsOf) {
        pinned.put(victim.getUniqueId(), System.currentTimeMillis() + secondsOf * 1000L);
        pinnedBy.put(victim.getUniqueId(), shooter.getUniqueId());
        cancelPinned(victim, shooter);
    }

    /** Speed と Jump Boost を消し、消すたびに撃った人と相手にチャット (Pin down の間ずっと、ユーザーの指定)。 */
    private void cancelPinned(Player victim, Player shooter) {
        for (PotionEffectType type : new PotionEffectType[]{PotionEffectType.SPEED, PotionEffectType.JUMP}) {
            if (victim.hasPotionEffect(type)) {
                victim.removePotionEffect(type);
                String name = type == PotionEffectType.SPEED ? "Speed" : "Jump Boost";
                if (shooter != null) {
                    shooter.sendMessage("§a§lIT'S A PIN! §7you removed " + name + " from " + tag(victim) + "§7!");
                    victim.sendMessage("§c§lPINNED! §7by " + tag(shooter) + "§7, " + name + " cancelled!");
                }
            }
        }
    }

    private void tickPinned() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> it = pinned.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player == null) {
                player = plugin.getBotManager().find(entry.getKey());
            }
            if (player == null || now >= entry.getValue()) {
                it.remove();
                pinnedBy.remove(entry.getKey());
                continue;
            }
            UUID shooterId = pinnedBy.get(entry.getKey());
            Player shooter = shooterId == null ? null : plugin.getServer().getPlayer(shooterId);
            if (shooter == null && shooterId != null) {
                shooter = plugin.getBotManager().find(shooterId);
            }
            cancelPinned(player, shooter);
        }
    }

    // ---------------------------------------------------------------- Dark Pants

    /** 革の防具を 1 つでも着ているか (Fresh / Mystic Pants も革)。 */
    private static boolean wearsLeather(Player player) {
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && armor.getType().name().startsWith("LEATHER_")) {
                return true;
            }
        }
        return false;
    }

    private final Map<UUID, Long> sanguisugeUsed = new HashMap<>();
    private final Map<UUID, Integer> breachingStrikes = new HashMap<>();
    private final Map<UUID, Long> breachingTimes = new HashMap<>();
    /** Grim Reaper: キルの報酬の倍率 (-80%)。 */
    private static final double GRIM_REAPER_REWARDS = 0.2;
    private static final double GRIM_REAPER_RANGE = 10;
    private static final double GRIM_REAPER_DAMAGE = 4;
    private static final double MIND_ASSAULT_RANGE = 11;
    private static final double MIND_ASSAULT_PER_PLAYER = 1.5;
    private static final double MIND_ASSAULT_MAX = 16;
    private static final double NEEDLESS_SUFFERING_ABSORPTION = 60;

    /**
     * 殴った側の Dark エンチャント (説明文どおり)。
     * Mind Assault : 与ダメージ -60%。11 ブロック以内の革の防具を着たプレイヤー 1 人につき +0.75❤ (最大 +8❤)
     * Misery       : 革の防具を着た相手に +0.5❤ の確定ダメージ、自分は 0.3❤ 受ける
     * Spite        : 革の防具を着た相手に +20%
     * Sanguisuge   : 革の防具を着た相手を殴ると 0.5❤ 回復 (2 秒に 1 回、Vampire があれば 2 倍)
     */
    private void darkMelee(DamageContext context, Player attacker, Player victim) {
        Map<String, Integer> pants = pants(attacker);
        if (pants.isEmpty()) {
            return;
        }
        boolean leather = wearsLeather(victim);
        if (pants.containsKey("mind_assault")) {
            context.reduce(0.60);
            int count = 0;
            for (org.bukkit.entity.Entity near : attacker.getNearbyEntities(MIND_ASSAULT_RANGE, MIND_ASSAULT_RANGE, MIND_ASSAULT_RANGE)) {
                if (near instanceof Player && near.getLocation().distanceSquared(attacker.getLocation()) <= MIND_ASSAULT_RANGE * MIND_ASSAULT_RANGE
                        && wearsLeather((Player) near)) {
                    count += MysticAccess.playerWeight((Player) near);
                }
            }
            context.addDamage(Math.min(MIND_ASSAULT_MAX, MIND_ASSAULT_PER_PLAYER * count));
        }
        if (pants.containsKey("misery") && leather && !context.isExtraStrike()) {
            context.addTrueDamage(1.0);
            context.afterHit(dealt -> {
                if (attacker.getHealth() - 0.6 > 0) {
                    attacker.setHealth(attacker.getHealth() - 0.6);
                } else {
                    attacker.damage(attacker.getHealth() * 100);
                }
            });
        }
        if (pants.containsKey("spite") && leather) {
            context.increase(0.20);
        }
        if (pants.containsKey("sanguisuge") && leather && !context.isExtraStrike()
                && ready(sanguisugeUsed, attacker.getUniqueId(), 2_000)) {
            boolean vampire = plugin.getPerkManager().has(attacker, com.pitremake.perk.Perk.VAMPIRE);
            heal(attacker, vampire ? 2 : 1);
        }
    }

    /**
     * 殴られた側の Dark エンチャント。
     * Spite     : 革の防具を着た相手からのダメージ +5%
     * Nostalgia : 受けるダメージ -10%
     */
    private void darkDefense(DamageContext context, Player victim, Player attacker) {
        Map<String, Integer> pants = pants(victim);
        // Ace of Spades: Combat Spade で殴られたときのダメージ -0.5 / 1 / 1.5❤
        Integer ace = pants.get("ace_of_spades");
        if (ace != null && context.melee && com.pitremake.shop.ShopItem.of(attacker.getItemInHand()) == com.pitremake.shop.ShopItem.COMBAT_SPADE) {
            context.reduceFlat(EnchantNumbers.hearts(enchant("ace_of_spades"), ace, 0));
        }
        // Brakes! II / III: 自分より上にいる相手からのダメージ -5% / -10%
        Integer brakes = pants.get("brakes");
        if (brakes != null && brakes >= 2 && attacker.getLocation().getY() > victim.getLocation().getY() + 0.5) {
            context.reduce(EnchantNumbers.percent(enchant("brakes"), brakes, 0));
        }
        // Hunt the Hunter III: Bounty Hunter を付けた相手からのダメージ -20%
        Integer hunt = pants.get("counter_bounty_hunter");
        if (hunt != null && hunt >= 3 && plugin.getPerkManager().bountyHunting(attacker)) {
            context.reduce(0.20);
        }
        if (pants.containsKey("spite") && wearsLeather(attacker)) {
            context.increase(0.05);
        }
        if (pants.containsKey("nostalgia")) {
            context.reduce(0.10);
        }
    }

    /** Grim Reaper のキルの報酬の倍率 (無ければ 1)。 */
    /** Pit MBA: 15 ブロック以内に他のプレイヤーが 5 人以上いれば、キルのゴールド +10/30/50%・XP +10/20/30%。 */
    public void addKillBoosts(Player killer, com.pitremake.stats.KillRewards.Boosts boosts) {
        Integer mba = pants(killer).get(com.pitremake.enchant.list.PantsEnchants.PIT_MBA);
        if (mba == null) {
            return;
        }
        int players = 0;
        for (org.bukkit.entity.Entity entity : killer.getNearbyEntities(PIT_MBA_RANGE, PIT_MBA_RANGE, PIT_MBA_RANGE)) {
            if (entity instanceof Player && entity.getLocation().distanceSquared(killer.getLocation()) <= PIT_MBA_RANGE * PIT_MBA_RANGE) {
                players++;
            }
        }
        if (players < PIT_MBA_PLAYERS) {
            return;
        }
        double[] gold = {0.10, 0.30, 0.50};
        double[] xp = {0.10, 0.20, 0.30};
        boosts.addGold(gold[index(mba)]);
        boosts.addXp(xp[index(mba)]);
    }

    private static final double PIT_MBA_RANGE = 15;
    private static final int PIT_MBA_PLAYERS = 5;

    public double killRewardMultiplier(Player killer) {
        return pants(killer).containsKey("grim_reaper") ? GRIM_REAPER_REWARDS : 1;
    }

    /** Grim Reaper: 倒す一撃の直前に、10 ブロック以内のプレイヤーに 2❤。 */
    private void grimReaper(Player killer, Player victim) {
        if (!pants(killer).containsKey("grim_reaper")) {
            return;
        }
        for (Player target : killer.getWorld().getPlayers()) {
            if (target.equals(killer) || target.equals(victim) || target.isDead()
                    || target.getLocation().distanceSquared(victim.getLocation()) > GRIM_REAPER_RANGE * GRIM_REAPER_RANGE
                    || plugin.getPitSpawn().isProtected(target.getLocation()) || MysticAccess.disabled(killer, target)) {
                continue;
            }
            plugin.getKillListener().recordAttack(killer, target, GRIM_REAPER_DAMAGE);
            if (target.getHealth() - GRIM_REAPER_DAMAGE > 0) {
                target.setHealth(target.getHealth() - GRIM_REAPER_DAMAGE);
                target.playEffect(org.bukkit.EntityEffect.HURT);
            } else {
                target.damage(target.getHealth() * 100);
            }
        }
    }

    /**
     * Needless Suffering は回復できない。Sanguisuge は Regeneration で回復しない。
     */
    @EventHandler(ignoreCancelled = true)
    public void onRegain(org.bukkit.event.entity.EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Map<String, Integer> pants = pants((Player) event.getEntity());
        if (pants.containsKey("needless_suffering")) {
            event.setCancelled(true);
        } else if (pants.containsKey("sanguisuge")
                && event.getRegainReason() == org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason.MAGIC_REGEN) {
            event.setCancelled(true);
        }
    }

    /** Nostalgia: 着ているダイヤの防具が光る (脱いだら・Nostalgia でなくなったら消す)。 */
    private void nostalgiaGlow(Player player) {
        boolean glow = pants(player).containsKey("nostalgia");
        ItemStack[] armor = player.getInventory().getArmorContents();
        boolean changed = false;
        for (int i = 0; i < armor.length; i++) {
            ItemStack piece = armor[i];
            if (piece == null || !piece.getType().name().startsWith("DIAMOND_")) {
                continue;
            }
            boolean glowing = com.pitremake.util.NbtUtil.has(piece, NOSTALGIA_KEY);
            if (glow && !glowing) {
                org.bukkit.inventory.meta.ItemMeta meta = piece.getItemMeta();
                meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                piece.setItemMeta(meta);
                armor[i] = com.pitremake.util.NbtUtil.setInt(piece, NOSTALGIA_KEY, 1);
                changed = true;
            } else if (!glow && glowing) {
                org.bukkit.inventory.meta.ItemMeta meta = piece.getItemMeta();
                meta.removeEnchant(org.bukkit.enchantments.Enchantment.DURABILITY);
                piece.setItemMeta(meta);
                net.minecraft.server.v1_8_R3.NBTTagCompound extra = com.pitremake.util.NbtUtil.readExtraAttributes(piece);
                extra.remove(NOSTALGIA_KEY);
                armor[i] = com.pitremake.util.NbtUtil.writeExtraAttributes(piece, extra);
                changed = true;
            }
        }
        if (changed) {
            player.getInventory().setArmorContents(armor);
        }
    }

    private static final String NOSTALGIA_KEY = "NostalgiaGlow";

    // ---------------------------------------------------------------- キル・スポーン

    /** キルしたとき。 */
    public void onKill(Player killer, Player victim) {
        hiddenJewel(killer);
        grimReaper(killer, victim);
        blobOnKill(killer);
        if (killer.isDead()) {
            return;
        }
        Map<String, Integer> pants = pants(killer);
        Integer eggs = pants.get("eggs");
        if (eggs != null) {
            give(killer, enchantItem(ITEM_EGG, EGGS_KILL[index(eggs)]));
        }
        Integer creative = pants.get("wood_blocks");
        if (creative != null) {
            give(killer, enchantItem(ITEM_PLANK, PLANKS_KILL[index(creative)]));
        }
        Integer tnt = pants.get("tnt");
        if (tnt != null) {
            give(killer, enchantItem(ITEM_TNT, TNT_KILL[index(tnt)]));
        }
        Integer instaboom = pants.get("instaboom_tnt");
        if (instaboom != null) {
            give(killer, enchantItem(ITEM_INSTABOOM, INSTABOOM_KILL[index(instaboom)]));
        }
        Integer snowmenLevel = pants.get("snowmen");
        if (snowmenLevel != null) {
            int room = SCOOP_MAX[index(snowmenLevel)] - count(killer, ITEM_SCOOP);
            if (room > 0) {
                give(killer, enchantItem(ITEM_SCOOP, Math.min(room, SCOOP_KILL[index(snowmenLevel)])));
            }
        }
        Integer goldenHeart = pants.get("absorption_on_kill");
        if (goldenHeart != null) {
            PitEnchant enchant = enchant("absorption_on_kill");
            double add = EnchantNumbers.hearts(enchant, goldenHeart, 0);
            double max = EnchantNumbers.hearts(enchant, goldenHeart, 1);
            CraftPlayer handle = (CraftPlayer) killer;
            float now = handle.getHandle().getAbsorptionHearts();
            if (now < max) {
                handle.getHandle().setAbsorptionHearts((float) Math.min(max, now + add));
            }
        }
        Integer refresh = pants.get("refresh_speed_on_kill");
        if (refresh != null) {
            for (PotionEffect active : killer.getActivePotionEffects()) {
                if (active.getType().equals(PotionEffectType.SPEED)) {
                    int[] add = {2, 4, 6};
                    int[] max = {18, 24, 30};
                    int addTicks = add[index(refresh)] * 20 / (active.getAmplifier() >= 1 ? 2 : 1);
                    int duration = Math.min(max[index(refresh)] * 20, active.getDuration() + addTicks);
                    if (duration > active.getDuration()) {
                        effect(killer, PotionEffectType.SPEED, duration, active.getAmplifier());
                    }
                }
            }
        }
        Map<String, Integer> sword = sword(killer);
        Integer speed = sword.get("etspeed_on_kill");
        if (speed != null) {
            int[] secondsOf = {4, 7, 12};
            effect(killer, PotionEffectType.SPEED, secondsOf[index(speed)] * 20, 0);
        }
        Integer heal = sword.get("melee_heal_on_kill");
        if (heal != null) {
            heal(killer, EnchantNumbers.hearts(enchant("melee_heal_on_kill"), heal, 0));
        }
        Integer resistance = sword.get("resistance_on_kill");
        if (resistance != null) {
            int[] secondsOf = {2, 3, 5};
            effect(killer, PotionEffectType.DAMAGE_RESISTANCE, secondsOf[index(resistance)] * 20, 0);
        }
    }

    /**
     * Hidden Jewel: 手に持つ剣・履いているパンツのキルを数え、117 キルで Tier I のアイテム (Tier III のエンチャント 1 つ) になる。
     * パンツは普通の 5 色のどれか。全体にチャット (ユーザーのスクリーンショット): §3§lJEWEL! §7[120] 名前 found §eTier I Orange Pants!
     * アイテム名にカーソルを合わせると中身が見える。
     */
    private void hiddenJewel(Player killer) {
        org.bukkit.inventory.PlayerInventory inventory = killer.getInventory();
        for (boolean pantsSlot : new boolean[]{false, true}) {
            ItemStack item = pantsSlot ? inventory.getLeggings() : killer.getItemInHand();
            // Somber を履いていると剣の Hidden Jewel は数えない
            if (!MysticItems.isMystic(item) || (!pantsSlot && MysticAccess.mysticItemsBlocked(killer))) {
                continue;
            }
            MysticData data = MysticData.read(item);
            if (!data.getEnchants().containsKey(com.pitremake.well.MysticWellRoller.HIDDEN_JEWEL)) {
                continue;
            }
            int kills = data.getHiddenJewelKills() + 1;
            ItemStack updated;
            if (kills < JEWEL_KILLS) {
                data.setHiddenJewelKills(kills);
                updated = MysticItems.applyDisplay(data.write(item), plugin.getEnchantRegistry());
            } else {
                MysticType type = MysticType.of(item);
                String name;
                ItemStack fresh;
                if (type == MysticType.PANTS) {
                    java.util.List<com.pitremake.item.PantsColor> colors = com.pitremake.item.PantsColor.standardColors();
                    com.pitremake.item.PantsColor color = colors.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(colors.size()));
                    fresh = MysticItems.freshPants(color);
                    name = "Tier I " + color.getDisplayName() + " Pants";
                } else if (type == MysticType.BOW) {
                    fresh = MysticItems.freshBow();
                    name = "Tier I Bow";
                } else {
                    fresh = MysticItems.freshSword();
                    name = "Tier I Sword";
                }
                MysticData recycled = MysticData.read(fresh);
                plugin.getWellRoller().jewelRecycle(recycled, type);
                updated = MysticItems.applyDisplay(recycled.write(fresh), plugin.getEnchantRegistry());
                announceJewel(killer, name, updated);
            }
            if (pantsSlot) {
                inventory.setLeggings(updated);
            } else {
                killer.setItemInHand(updated);
            }
        }
    }

    private void announceJewel(Player finder, String name, ItemStack item) {
        java.util.List<net.md_5.bungee.api.chat.BaseComponent> message = new java.util.ArrayList<>(
                java.util.Arrays.asList(net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§3§lJEWEL! §7" + tag(finder) + " §7found ")));
        net.md_5.bungee.api.chat.HoverEvent hover = new net.md_5.bungee.api.chat.HoverEvent(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_ITEM,
                new net.md_5.bungee.api.chat.BaseComponent[]{new net.md_5.bungee.api.chat.TextComponent(com.pitremake.util.NbtUtil.toChatNbt(item))});
        for (net.md_5.bungee.api.chat.BaseComponent part : net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§e" + name + "!")) {
            part.setHoverEvent(hover);
            message.add(part);
        }
        net.md_5.bungee.api.chat.BaseComponent[] components = message.toArray(new net.md_5.bungee.api.chat.BaseComponent[0]);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.spigot().sendMessage(components);
        }
    }

    /** スポーンしたとき (効果を消して初期装備を渡した後)。 */
    public void onSpawn(Player player) {
        pinned.remove(player.getUniqueId());
        hitsTaken.remove(player.getUniqueId());
        removeSnowmen(player);
        org.bukkit.entity.Slime blob = blobs.remove(player.getUniqueId());
        if (blob != null) {
            blob.remove();
        }
        escapePodUsed.remove(player.getUniqueId());
        phoenixUsed.remove(player.getUniqueId());
        phoenixBuff.remove(player.getUniqueId());
        respawnEnchants(player, pants(player));
        resetEnchantItems(player);
    }

    /** Respawn: Absorption (5 / 10 / 15❤ の衝撃吸収を付け直す) と Respawn: Resistance (Resistance I 20 / 30 秒、II 40 秒)。 */
    private void respawnEnchants(Player player, Map<String, Integer> pants) {
        if (pants.containsKey("needless_suffering")) {
            // Needless Suffering: 30❤ の衝撃吸収で出てくる
            ((CraftPlayer) player).getHandle().setAbsorptionHearts((float) NEEDLESS_SUFFERING_ABSORPTION);
        }
        Integer absorption = pants.get("respawn_with_absorption");
        if (absorption != null) {
            ((CraftPlayer) player).getHandle().setAbsorptionHearts(
                    (float) EnchantNumbers.hearts(enchant("respawn_with_absorption"), absorption, 0));
        }
        Integer resistance = pants.get("respawn_with_resistance");
        if (resistance != null) {
            int[] secondsOf = {20, 30, 40};
            effect(player, PotionEffectType.DAMAGE_RESISTANCE, secondsOf[index(resistance)] * 20, resistance >= 3 ? 1 : 0);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        pinned.remove(id);
        hitsTaken.remove(id);
        wasFighting.remove(id);
        wornPants.remove(id);
        removeSnowmen(event.getPlayer());
        wornRespawnLevels.remove(id);
        duelistBlocks.remove(id);
        duelistEmpowered.remove(id);
    }
}
