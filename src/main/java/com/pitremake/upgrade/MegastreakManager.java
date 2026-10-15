package com.pitremake.upgrade;

import com.pitremake.PitPlugin;
import com.pitremake.combat.DamageContext;
import com.pitremake.combat.DamageModifier;
import com.pitremake.gem.GemItem;
import com.pitremake.item.MysticData;
import com.pitremake.item.MysticItems;
import com.pitremake.item.RewardItems;
import com.pitremake.perk.Perk;
import com.pitremake.special.SpecialItems;
import com.pitremake.stats.KillRewards;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * メガストリークと Passive の効果。数値は {@link MegastreakRules} と pit.wiki の Passives。
 * 永続のポーション効果 (Speed I など) は 2 秒ごとに 5 秒分を掛け直す (切り替えたらすぐ切れるように)。
 */
public final class MegastreakManager implements Listener, DamageModifier {

    private static final int EFFECT_REFRESH_TICKS = 40;
    private static final int EFFECT_DURATION_TICKS = 100;
    private static final int NIGHT_VISION_AMPLIFIER = 10;
    /** El Gato: Tier の数までのキル (1 回の人生で) に +5g +5XP。 */
    private static final double EL_GATO_REWARD = 5;
    private static final DecimalFormat NUMBER = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));

    private final PitPlugin plugin;
    /** 本当に死んだとき (HP 0 でスポーンへ戻る仕組みを通らなかったとき) の Uberdrop。リスポーン時に渡す。 */
    private final Map<UUID, List<ItemStack>> respawnItems = new HashMap<>();
    private BukkitTask task;

    public MegastreakManager(PitPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::refreshEffects, 1, EFFECT_REFRESH_TICKS);
        potionTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::shortenPotions, 1, 1);
    }

    /** Uberstreak 300 キルから: 付いたポーション効果の時間を半分にする。 */
    private BukkitTask potionTask;
    /** プレイヤー → (効果の種類 → 前の tick の残り時間)。 */
    private final Map<UUID, Map<org.bukkit.potion.PotionEffectType, Integer>> potionDurations = new HashMap<>();

    /**
     * 1.8 にはポーション効果が付いたときのイベントが無いので、毎 tick 見て新しく付いた (残り時間が増えた) 効果を半分の時間で付け直す。
     * ずっと付ける効果 (ambient: 暗視・メガストリークの Speed など) は対象外。
     */
    private void shortenPotions() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            if (player.isDead() || !MegastreakRules.halvesPotions(megastreak(player), streak(player))) {
                potionDurations.remove(id);
                continue;
            }
            Map<org.bukkit.potion.PotionEffectType, Integer> last = potionDurations.computeIfAbsent(id, k -> new HashMap<>());
            Map<org.bukkit.potion.PotionEffectType, Integer> now = new HashMap<>();
            for (PotionEffect effect : player.getActivePotionEffects()) {
                if (effect.isAmbient()) {
                    continue;
                }
                int duration = effect.getDuration();
                Integer before = last.get(effect.getType());
                if (before == null || duration > before) {
                    duration = Math.max(1, duration / 2);
                    player.addPotionEffect(new PotionEffect(effect.getType(), duration, effect.getAmplifier(), false, true), true);
                }
                now.put(effect.getType(), duration);
            }
            potionDurations.put(id, now);
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        if (potionTask != null) {
            potionTask.cancel();
        }
    }

    private Megastreak megastreak(Player player) {
        return plugin.getUpgradeManager().getMegastreak(player.getUniqueId());
    }

    private int streak(Player player) {
        return plugin.getStatsManager().getStreak(player.getUniqueId());
    }

    private int tier(Player player, Passive passive) {
        return plugin.getUpgradeManager().getTier(player.getUniqueId(), passive);
    }

    /** 発動中なら tab に出す文字 (例: §d§lUBER100)、そうでなければ null。 */
    public String tabTag(Player player) {
        Megastreak megastreak = megastreak(player);
        int streak = streak(player);
        return MegastreakRules.isActive(megastreak, streak) ? megastreak.tag(streak) : null;
    }

    /** 発動中ならサイドバーの "Status: " の後ろ (例: §6Highlander)、そうでなければ null。 */
    public String statusName(Player player) {
        Megastreak megastreak = megastreak(player);
        if (!MegastreakRules.isActive(megastreak, streak(player))) {
            return null;
        }
        return megastreak.tag(0).substring(0, 2) + megastreak.getDisplayName();
    }

    public int maxBounty(Player player) {
        return MegastreakRules.maxBounty(megastreak(player), streak(player));
    }

    /** このキル (streak はこのキルを含めたストリーク) の XP・ゴールドの追加分。 */
    public KillRewards.Boosts boosts(Player killer, int streak) {
        Megastreak megastreak = megastreak(killer);
        KillRewards.Boosts boosts = new KillRewards.Boosts();
        // 出どころごとに別の倍率 (メガストリーク / Passive / Golden Helmet の Royalty)
        boosts.addXp(MegastreakRules.xpBonus(megastreak, streak));
        boosts.addXp(0.10 * tier(killer, Passive.XP_BOOST));
        if (RewardItems.is(killer.getInventory().getHelmet(), RewardItems.Type.GOLDEN_HELMET)) {
            boosts.addXp(RewardItems.ROYALTY_XP);
        }
        boosts.addGold(MegastreakRules.goldBonus(megastreak, streak));
        boosts.addGold(0.10 * tier(killer, Passive.GOLD_BOOST));
        boosts.maxXp = MegastreakRules.maxXpBonus(megastreak, streak);
        if (streak <= tier(killer, Passive.EL_GATO)) {
            boosts.flatXp = EL_GATO_REWARD;
            boosts.flatGold = EL_GATO_REWARD;
        }
        return boosts;
    }

    /**
     * キル (とアシストでのストリークの増加) の後。ストリークが発動のキル数を越えたらアナウンスし、To the Moon は XP を貯める。
     * before / streak は増える前と後のストリーク (Promotion!! や Assistant (to the) Streaker で 1 より多く増えることもある)。
     */
    public void onKill(Player killer, int before, int streak, int xp) {
        Megastreak megastreak = megastreak(killer);
        if (before < megastreak.getTrigger() && streak >= megastreak.getTrigger()) {
            activate(killer, megastreak);
        }
        if (megastreak == Megastreak.TO_THE_MOON && MegastreakRules.isActive(megastreak, streak)) {
            plugin.getUpgradeManager().addMoonXp(killer.getUniqueId(), xp);
        }
        if (megastreak == Megastreak.UBERSTREAK) {
            // 100 キルごとにエルダーガーディアンの呪いの音とデバフのチャット (ユーザーのスクリーンショット)
            for (int kills = (before / 100 + 1) * 100; kills <= streak; kills += 100) {
                killer.playSound(killer.getLocation(), "mob.guardian.curse", 1.0f, 1.0f);
                String debuff = uberDebuff(kills);
                if (debuff != null) {
                    killer.sendMessage("§d§lUBERSTREAK! " + debuff);
                }
            }
        }
    }

    /** Uberstreak の 100 / 200 / 300 / 400 キルのデバフのチャット (ユーザーのスクリーンショット)。 */
    static String uberDebuff(int kills) {
        switch (kills) {
            case 100:
                return "§cDeal reduced damage vs non-prestige players!";
            case 200:
                return "§c-2 max ❤";
            case 300:
                return "§cPotion Effects last -50% as long";
            case 400:
                return "§cYou cannot heal!!!";
            default:
                return null;
        }
    }

    /** 録画に無いので文面は推測。 */
    private void activate(Player player, Megastreak megastreak) {
        String message = "§c§lMEGASTREAK! " + plugin.getStatsManager().chatTag(player.getUniqueId())
                + " " + plugin.getRankManager().nameColor(player.getUniqueId()) + player.getName() + " §7activated " + megastreak.tag(0).substring(0, 4)
                + megastreak.getDisplayName().toUpperCase(Locale.US) + "§7!";
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(message);
        }
        Bukkit.getConsoleSender().sendMessage(message);
        player.playSound(player.getLocation(), "mob.wither.spawn", 0.6f, 1.2f);
        if (megastreak == Megastreak.MAGNUM_OPUS) {
            magnumOpus(player);
            return;
        }
        // 発動した人に雷が落ちる (見た目と音だけでダメージなし)
        player.getWorld().strikeLightningEffect(player.getLocation());
        if (megastreak == Megastreak.BEASTMODE) {
            ItemStack old = player.getInventory().getHelmet();
            // ユーザーのスクリーンショット: §aBeastmode Helmet
            ItemStack helmet = new ItemStack(Material.DIAMOND_HELMET);
            org.bukkit.inventory.meta.ItemMeta meta = helmet.getItemMeta();
            meta.setDisplayName("§aBeastmode Helmet");
            helmet.setItemMeta(meta);
            player.getInventory().setHelmet(helmet);
            if (old != null && old.getType() != Material.AIR) {
                for (ItemStack leftover : player.getInventory().addItem(old).values()) {
                    player.getWorld().dropItem(player.getLocation(), leftover);
                }
            }
        }
        refresh(player);
    }

    /** Magnum Opus でばらまく金インゴットの数 (wiki に数が無いので推測)。 */
    private static final int OPUS_INGOTS = 16;
    /** Magnum Opus の花火の飛ぶ時間 (ユーザーの指定: 上がる音は短め)。 */
    private static final int OPUS_FIREWORK_TICKS = 2;
    private static final int OPUS_REPAIR_LIVES = 4;

    /**
     * Magnum Opus の発動 (pit.wiki / ユーザーの指定): 花火 (上がる音は短く、炸裂とキラキラの音) が散り、
     * 金インゴットをばらまいて (ダメージのない爆発) すぐに死ぬ。
     */
    private void magnumOpus(Player player) {
        org.bukkit.Location at = player.getLocation();
        org.bukkit.entity.Firework firework = at.getWorld().spawn(at.clone().add(0, 1, 0), org.bukkit.entity.Firework.class);
        org.bukkit.inventory.meta.FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(org.bukkit.FireworkEffect.builder().with(org.bukkit.FireworkEffect.Type.BALL_LARGE)
                .withColor(org.bukkit.Color.YELLOW, org.bukkit.Color.ORANGE).withFade(org.bukkit.Color.RED)
                .flicker(true).trail(true).build());
        meta.setPower(0);
        firework.setFireworkMeta(meta);
        ((org.bukkit.craftbukkit.v1_8_R3.entity.CraftFirework) firework).getHandle().expectedLifespan = OPUS_FIREWORK_TICKS;
        plugin.getGoldIngots().scatter(at, OPUS_INGOTS);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && !player.isDead()) {
                plugin.getKillListener().oof(player);
            }
        }, 1);
    }

    /**
     * Magnum Opus で死んだとき: 1 Renown と、Mystic Well に入っている Lives の減ったアイテムを Chunk of Vile で 4 Lives まで直す
     * (1 Life に 1 個、Totally Legit Gem を使ったアイテムは 2 個)。チャットは推測。
     */
    private void magnumOpusRewards(Player player) {
        UUID id = player.getUniqueId();
        plugin.getStatsManager().setRenown(id, plugin.getStatsManager().getRenown(id) + 1);
        player.sendMessage("§e§lMAGNUM OPUS! §7Earned §e+1 Renown§7!");
        ItemStack item = plugin.getWellStorage().get(id);
        if (!com.pitremake.item.MysticItems.isMystic(item)) {
            return;
        }
        com.pitremake.item.MysticData data = com.pitremake.item.MysticData.read(item);
        int missing = data.getMaxLives() - data.getLives();
        if (missing <= 0) {
            return;
        }
        int perLife = data.isGemmed() ? 2 : 1;
        int vile = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (com.pitremake.special.SpecialItems.isVile(stack)) {
                vile += stack.getAmount();
            }
        }
        int lives = Math.min(OPUS_REPAIR_LIVES, Math.min(missing, vile / perLife));
        if (lives <= 0) {
            return;
        }
        int take = lives * perLife;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && take > 0; i++) {
            if (!com.pitremake.special.SpecialItems.isVile(contents[i])) {
                continue;
            }
            int used = Math.min(take, contents[i].getAmount());
            take -= used;
            if (used >= contents[i].getAmount()) {
                player.getInventory().setItem(i, null);
            } else {
                contents[i].setAmount(contents[i].getAmount() - used);
                player.getInventory().setItem(i, contents[i]);
            }
        }
        data.setLives(data.getLives() + lives);
        plugin.getWellStorage().set(id, com.pitremake.item.MysticItems.applyDisplay(data.write(item), plugin.getEnchantRegistry()));
        player.sendMessage("§e§lMAGNUM OPUS! §7Fixed §c" + lives + " lives §7on the item in the §dMystic Well§7!");
    }

    /** Beastmode 発動中に死んだときはダイヤのヘルメットが残る (streak は死ぬ前の値)。 */
    public boolean keepsDiamondHelmet(Player player, int streak) {
        Megastreak megastreak = megastreak(player);
        return megastreak == Megastreak.BEASTMODE && MegastreakRules.isActive(megastreak, streak);
    }

    /**
     * 死んだとき (streak / bounty は死ぬ前の値。持ち物の処理の後に呼ぶ)。
     * Overdrive の XP、To the Moon の貯めた XP、Uberstreak の Uberdrop。
     * Highlander は死んでも自分のバウンティはもらえない (ユーザーの指摘。説明文と wiki には書いてあるが実際はもらえない)。
     */
    public void onDeath(Player player, int streak, int bounty) {
        UUID id = player.getUniqueId();
        Megastreak megastreak = megastreak(player);
        long moonXp = plugin.getUpgradeManager().takeMoonXp(id);
        if (!MegastreakRules.isActive(megastreak, streak)) {
            return;
        }
        List<ItemStack> give = new ArrayList<>();
        switch (megastreak) {
            case OVERDRIVE:
                reward(player, MegastreakRules.OVERDRIVE_DEATH_XP, 0, "Overdrive");
                break;
            case TO_THE_MOON:
                long payout = Math.round(moonXp * MegastreakRules.moonPayoutMultiplier(streak));
                if (payout > 0) {
                    reward(player, payout, 0, "To the Moon");
                }
                break;
            case MAGNUM_OPUS:
                magnumOpusRewards(player);
                break;
            case UBERSTREAK:
                if (streak >= MegastreakRules.UBER_DROP_STREAK) {
                    // 中身の決まった Uberdrop の箱 (持ってクリックで開く)
                    ItemStack drop = com.pitremake.special.Uberdrop.roll(plugin, player);
                    give.add(drop);
                    announceUberdrop(player, drop);
                }
                break;
            default:
                break;
        }
        if (give.isEmpty()) {
            return;
        }
        if (player.isDead()) {
            // 本当に死んだとき (HP 0 でスポーンへ戻る仕組みを通らなかったとき) は復活後に渡す
            respawnItems.put(id, give);
        } else {
            giveItems(player, give);
        }
    }

    /**
     * 全員にチャット (ユーザーのスクリーンショット): §d§lUBERDROP! §7[120] 名前 §7obtained an §dUberdrop!
     * 名前はランクの色。"Uberdrop!" にカーソルを合わせると中身が見える。
     */
    private void announceUberdrop(Player player, ItemStack drop) {
        java.util.UUID id = player.getUniqueId();
        String tag = com.pitremake.stats.PrestigeRules.levelTag(plugin.getStatsManager().getPrestige(id), plugin.getStatsManager().getLevel(id));
        java.util.List<net.md_5.bungee.api.chat.BaseComponent> message = new java.util.ArrayList<>(java.util.Arrays.asList(
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§d§lUBERDROP! §7" + tag + " "
                        + plugin.getRankManager().nameColor(id) + player.getName() + " §7obtained an ")));
        net.md_5.bungee.api.chat.HoverEvent hover = new net.md_5.bungee.api.chat.HoverEvent(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_ITEM,
                new net.md_5.bungee.api.chat.BaseComponent[]{new net.md_5.bungee.api.chat.TextComponent(com.pitremake.util.NbtUtil.toChatNbt(drop))});
        for (net.md_5.bungee.api.chat.BaseComponent part : net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§dUberdrop!")) {
            part.setHoverEvent(hover);
            message.add(part);
        }
        net.md_5.bungee.api.chat.BaseComponent[] components = message.toArray(new net.md_5.bungee.api.chat.BaseComponent[0]);
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.spigot().sendMessage(components);
        }
    }

    private static void giveItems(Player player, List<ItemStack> items) {
        for (ItemStack item : items) {
            for (ItemStack leftover : player.getInventory().addItem(item).values()) {
                player.getWorld().dropItem(player.getLocation(), leftover);
            }
        }
    }

    private void reward(Player player, long xp, double gold, String source) {
        if (xp > 0) {
            plugin.getStatsManager().addXp(player.getUniqueId(), xp);
            player.sendMessage("§b+" + NUMBER.format(xp) + " XP §7from §c" + source);
        }
        if (gold > 0) {
            plugin.getGoldManager().add(player.getUniqueId(), gold);
            player.sendMessage("§6+" + NUMBER.format(gold) + "g §7from §c" + source);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        List<ItemStack> items = respawnItems.remove(player.getUniqueId());
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (items != null) {
                giveItems(player, items);
            }
            refresh(player);
        });
    }

    /**
     * ダメージの増減 (メガストリーク・Passive)。倍率は {@link DamageContext} に入れて掛け算
     * (例: 受けるダメージ +10% → 増加 0.10、Uberstreak の与ダメージ -40% → 増加 -0.40)。Bot のストリークは 0 なので何も起きない。
     */
    @Override
    public void modify(DamageContext context) {
        Player victim = context.victim;
        Megastreak victimMega = megastreak(victim);
        int victimStreak = streak(victim);
        // メガストリークの倍率は E (足し算) ではなく、その外で掛ける f_p (逆算レポート-1: Beastmode など)
        context.multiply(MegastreakRules.damageTakenMultiplier(victimMega, victimStreak));
        if (MegastreakRules.isTrueDamageImmune(victimMega, victimStreak)) {
            context.immuneToTrueDamage();
        }
        if (context.arrow != null) {
            // Damage Reduction は wiki によると実際には弓のダメージだけ減る
            context.reduce(0.01 * tier(victim, Passive.DAMAGE_REDUCTION));
        }
        if (!context.byPlayer()) {
            return;
        }
        Player attacker = context.attacker;
        boolean bountied = plugin.getBountyManager().get(victim.getUniqueId()) > 0;
        // Uberstreak 100 キルから: プレステージの無い相手に -40%
        boolean nonPrestige = plugin.prestigeOf(victim) == 0;
        context.multiply(MegastreakRules.damageDealtMultiplier(megastreak(attacker), streak(attacker), nonPrestige, bountied));
        if (plugin.getPerkManager().bountyHunting(attacker)) {
            context.increase(MegastreakRules.bountyHunterDamageTaken(victimMega, victimStreak)
                    * plugin.getSimpleEnchants().bountyHunterFactor(victim, attacker));
        }
        context.increase(context.arrow != null ? 0.03 * tier(attacker, Passive.BOW_DAMAGE)
                : context.melee ? 0.01 * tier(attacker, Passive.MELEE_DAMAGE) : 0);
        // 被ダメージの追加・確定ダメージはプレイヤーから攻撃されたときだけ
        context.addDamage(MegastreakRules.extraDamageTaken(victimMega, victimStreak));
        // 代償の確定ダメージは Mirror でも防げない ("very true damage")
        context.addPenaltyTrueDamage(MegastreakRules.trueDamageTaken(victimMega, victimStreak));
    }

    /** Uberstreak 400 キル以上は回復しない。 */
    @EventHandler(ignoreCancelled = true)
    public void onRegain(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (!MegastreakRules.canHeal(megastreak(player), streak(player))) {
                event.setCancelled(true);
            }
        }
    }

    private void refreshEffects() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isDead()) {
                refresh(player);
            }
        }
    }

    /**
     * 永続のポーション効果と最大体力をメガストリークの状態に合わせる。
     * 暗視はずっと付ける (録画 2026-10-14_08-53-15.mcrc: 強さ 10、時間 32767 tick、パーティクルなし)。
     */
    public void refresh(Player player) {
        if (!player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, NIGHT_VISION_AMPLIFIER, true, false), true);
        }
        Megastreak megastreak = megastreak(player);
        int streak = streak(player);
        boolean active = MegastreakRules.isActive(megastreak, streak);
        if (active && (megastreak == Megastreak.OVERDRIVE || megastreak == Megastreak.HIGHLANDER)) {
            effect(player, PotionEffectType.SPEED);
        }
        if (megastreak == Megastreak.HERMIT) {
            effect(player, PotionEffectType.SLOW);
            if (active) {
                effect(player, PotionEffectType.DAMAGE_RESISTANCE);
            }
        }
        // 最大体力はパーク (Rambo / Thick) から、メガストリークで減る分を引く
        double maxHealth = plugin.getPerkManager().baseMaxHealth(player) + plugin.getSimpleEnchants().extraMaxHealth(player)
                + plugin.getKillstreakManager().extraMaxHealth(player)
                - MegastreakRules.lostMaxHealth(megastreak, streak);
        if (player.getMaxHealth() != maxHealth) {
            player.setMaxHealth(maxHealth);
        }
        plugin.getPerkManager().refresh(player);
        plugin.getSimpleEnchants().refresh(player);
    }

    private static void effect(Player player, PotionEffectType type) {
        player.addPotionEffect(new PotionEffect(type, EFFECT_DURATION_TICKS, 0, true, false), true);
    }
}
