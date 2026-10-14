package com.pitremake.bot;

import com.mojang.authlib.GameProfile;
import com.pitremake.spawn.PitSpawn;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import net.minecraft.server.v1_8_R3.PacketPlayOutAnimation;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_8_R3.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 1 体の Bot。近くのプレイヤーや他の Bot に向かって歩き、狙って剣を振る。
 * <pre>
 * 移動   : 歩く (たまに少しダッシュ)。壁や段差にぶつかったときと、たまにジャンプする
 * スポーン: 戦わずに中心 (0, 0) へ歩き、下の戦う場所へ落ちてから戦い始める
 * 壁     : 3 秒ぶつかり続けたら Pit の復活地点に戻る (カーペットは壁にしない)
 * 遠い   : まっすぐ近づく
 * 近い   : 左右に動きながら間合いを保つ
 * 射程内 : 一定間隔で剣を振る (命中率あり)
 * 体力   : 少しずつ回復する
 * 死亡   : 少し待ってから Pit の復活地点に復活する
 * </pre>
 */
final class PitBot {

    /** 狙うプレイヤーを探す距離。 */
    private static final double TARGET_RANGE = 16;
    /** 攻撃が届く距離。 */
    private static final double REACH = 2.7;
    /** この距離より遠いとまっすぐ近づき、近いと横移動する。 */
    private static final double CLOSE_RANGE = 2.4;
    private static final double TOO_CLOSE = 1.6;
    /** 横移動の速さ (1 = プレイヤーと同じ)。 */
    private static final float STRAFE_SPEED = 0.5f;
    /** 1 秒あたりのクリック数。 */
    private static final double CPS = 4;
    /** 狙いのずれ (度、正規分布の標準偏差)。かなり下手。 */
    private static final double AIM_ERROR = 30;
    /** 横の狙いのずれがこの角度 (度) 以内のときだけ当たる (約 4 割)。 */
    private static final double HIT_ANGLE = 15;
    /** 地面にいるとき 1 tick ごとにジャンプする確率 (平均 1 秒に 1 回)。 */
    private static final double JUMP_CHANCE = 0.05;
    /** 前に歩いているとき 1 tick ごとにダッシュを始める確率 (平均 5 秒に 1 回)。 */
    private static final double SPRINT_CHANCE = 0.01;
    /** ダッシュを続ける時間 (tick、この範囲でランダム)。 */
    private static final int SPRINT_MIN_TICKS = 20;
    private static final int SPRINT_MAX_TICKS = 60;
    /** 壁にぶつかり続けたらスポーンに戻すまでの時間 (tick)。 */
    private static final int STUCK_TICKS = 60;
    /** 狙う相手を見つけてから攻撃し始めるまで (tick)。 */
    private static final int REACTION_TICKS = 10;
    /** 死んでから復活するまで (tick)。 */
    private static final int RESPAWN_TICKS = 60;
    /** この間隔 (tick) で体力が 1 (半ハート) 回復する。 */
    private static final int REGEN_INTERVAL_TICKS = 40;
    private static final int MAX_PRESTIGE = 15;
    private static final double NON_PRESTIGE_CHANCE = 0.55;
    /** Renown の Fancy Hat が買えるプレステージ。 */
    private static final int FANCY_HAT_PRESTIGE = 5;
    /** Fresh Pants を履いて出てくることがあるプレステージと確率 (出現するたびに引く)。 */
    private static final int FRESH_PANTS_PRESTIGE = 3;
    private static final double FRESH_PANTS_CHANCE = 0.35;

    private final String name;
    /** 次に出現する場所 (最初は呼び出した場所、復活するときは Pit の復活地点)。 */
    private Location home;
    /** スポーン (攻撃不可の範囲)。この中にいる間は戦わずに中心へ向かって歩き、下へ落ちる。 */
    private final PitSpawn spawnArea;
    /** キルしたときの表示と報酬に使う Pit レベル (ランダム)。 */
    private final int level = 1 + ThreadLocalRandom.current().nextInt(120);
    /** プレステージ (半分強はプレステージなし、残りは 1〜15。ユーザーの指定でプレステージなしを多めに)。 */
    private final int prestige = ThreadLocalRandom.current().nextDouble() < NON_PRESTIGE_CHANCE
            ? 0 : 1 + ThreadLocalRandom.current().nextInt(MAX_PRESTIGE);
    /** Fancy Hat の色 (プレステージ V から。Bot ごとにランダムで、復活しても同じ)。 */
    private final org.bukkit.Color hatColor = org.bukkit.Color.fromRGB(ThreadLocalRandom.current().nextInt(0x1000000));
    private BotPlayer handle;

    private Player lastTarget;
    private int attackCooldown;
    private int strafeDirection = 1;
    private int strafeTicks;
    private int deadTicks;
    private int aliveTicks;
    private int stuckTicks;
    private int sprintTicks;

    /** スキン (MineSkin から。無ければ null で Steve / Alex)。復活しても同じ。 */
    private final SkinPool.Skin skin;

    PitBot(String name, Location home, PitSpawn spawnArea, SkinPool.Skin skin) {
        this.name = name;
        this.skin = skin;
        this.home = home.clone();
        this.spawnArea = spawnArea;
    }

    String getName() {
        return name;
    }

    int getLevel() {
        return level;
    }

    int getPrestige() {
        return prestige;
    }

    /** 例: "Bot1 [57] (12.3, 64.0, -5.1) HP 20.0" */
    String describe() {
        if (handle == null) {
            return name + " (despawned)";
        }
        EntityDamageEvent lastDamage = handle.getBukkitEntity().getLastDamageCause();
        return String.format("%s [%d] (%.1f, %.1f, %.1f) HP %.1f food %d%s%s", name, level, handle.locX, handle.locY, handle.locZ,
                handle.getHealth(), handle.getFoodData().getFoodLevel(), handle.onGround ? "" : " [air]",
                lastDamage == null ? "" : " last damage: " + lastDamage.getCause());
    }

    boolean is(Player player) {
        return handle != null && handle.getBukkitEntity().equals(player);
    }

    boolean hasId(UUID id) {
        return handle != null && handle.getUniqueID().equals(id);
    }

    /** 生きていればその Bot の Bukkit のプレイヤー、そうでなければ null。 */
    Player entity() {
        return handle == null || handle.dead || handle.getHealth() <= 0 ? null : handle.getBukkitEntity();
    }

    void spawn() {
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        WorldServer world = ((CraftWorld) home.getWorld()).getHandle();
        GameProfile profile = new GameProfile(UUID.randomUUID(), name);
        if (skin != null) {
            profile.getProperties().put("textures", new com.mojang.authlib.properties.Property("textures", skin.value, skin.signature));
        }
        handle = new BotPlayer(server, world, profile);
        handle.setLocation(home.getX(), home.getY(), home.getZ(), home.getYaw(), home.getPitch());
        equip(handle.getBukkitEntity(), prestige >= FANCY_HAT_PRESTIGE ? hatColor : null);
        // ある程度プレステージのある Bot は、たまに Fresh Pants を履いて出てくる (見た目だけ。エンチャントなし・落とさない)
        if (prestige >= FRESH_PANTS_PRESTIGE && ThreadLocalRandom.current().nextDouble() < FRESH_PANTS_CHANCE) {
            List<com.pitremake.item.PantsColor> colors = com.pitremake.item.PantsColor.standardColors();
            handle.getBukkitEntity().getInventory().setLeggings(
                    com.pitremake.item.MysticItems.freshPants(colors.get(ThreadLocalRandom.current().nextInt(colors.size()))));
        }

        // クライアントはタブリストに居ないプレイヤーを描画しないので、先に追加してからワールドに出す
        broadcastInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER);
        world.addEntity(handle);
        deadTicks = 0;
    }

    void despawn() {
        if (handle == null) {
            return;
        }
        handle.world.removeEntity(handle);
        broadcastInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER);
        handle = null;
    }

    /** 後から入ってきたプレイヤーにも見えるように、タブリストの情報を送る。 */
    void showTo(Player player) {
        if (handle != null) {
            send(player, new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, handle));
        }
    }

    /** @param otherBots 他の Bot (狙う相手の候補) */
    void tick(List<Player> otherBots) {
        if (handle == null) {
            return;
        }
        if (handle.dead || handle.getHealth() <= 0) {
            if (++deadTicks >= RESPAWN_TICKS) {
                respawnAtPitSpawn();
            }
            return;
        }
        if (++aliveTicks % REGEN_INTERVAL_TICKS == 0 && handle.getHealth() < handle.getMaxHealth()) {
            handle.heal(1);
        }
        // 壁にぶつかったまま動けなくなったらスポーンからやり直す
        stuckTicks = isAgainstWall() ? stuckTicks + 1 : 0;
        if (stuckTicks >= STUCK_TICKS) {
            respawnAtPitSpawn();
            return;
        }

        Location here = handle.getBukkitEntity().getLocation();
        if (spawnArea.isProtected(here)) {
            // スポーンでは戦わず、中心 (0, 0) へ歩いて下の戦う場所へ落ちる
            walkTo(spawnArea.center(here));
            lastTarget = null;
            handle.l();
            return;
        }

        Player target = findTarget(otherBots);
        if (target == null) {
            handle.setInput(0, 0, false, false);
        } else {
            if (!target.equals(lastTarget)) {
                // 新しい相手を見つけたらすぐには殴らない
                attackCooldown = Math.max(attackCooldown, REACTION_TICKS);
            }
            fight(target);
        }
        lastTarget = target;
        // バニラのプレイヤーと同じ処理 (移動・物理・回復・装備の同期)
        handle.l();
    }

    private void fight(Player target) {
        CraftPlayer self = handle.getBukkitEntity();
        Location eye = self.getEyeLocation();
        Vector toTarget = target.getEyeLocation().toVector().subtract(eye.toVector());
        double horizontal = Math.hypot(toTarget.getX(), toTarget.getZ());
        ThreadLocalRandom random = ThreadLocalRandom.current();

        double yawError = random.nextGaussian() * AIM_ERROR;
        float yaw = (float) (Math.toDegrees(Math.atan2(-toTarget.getX(), toTarget.getZ())) + yawError);
        float pitch = (float) (-Math.toDegrees(Math.atan2(toTarget.getY(), horizontal)) + random.nextGaussian() * AIM_ERROR / 2);
        handle.look(yaw, pitch);

        if (--strafeTicks <= 0) {
            strafeDirection = random.nextBoolean() ? 1 : -1;
            strafeTicks = 15 + random.nextInt(25);
        }
        float forward;
        float strafe;
        if (horizontal > CLOSE_RANGE) {
            forward = 1;
            strafe = 0;
        } else {
            forward = horizontal < TOO_CLOSE ? -0.5f : 0.3f;
            strafe = strafeDirection * STRAFE_SPEED;
        }
        // 歩く (たまにダッシュ)。たまにジャンプする
        handle.setInput(forward, strafe, randomJump(), sprint(forward > 0.5f));

        if (--attackCooldown <= 0 && toTarget.length() <= REACH) {
            swing();
            // 狙いが大きくずれていたら空振り
            if (Math.abs(yawError) <= HIT_ANGLE) {
                handle.attack(((CraftPlayer) target).getHandle());
            }
            attackCooldown = (int) Math.round(20 / CPS) + random.nextInt(2);
        }
    }

    private void walkTo(Location destination) {
        Location here = handle.getBukkitEntity().getLocation();
        double dx = destination.getX() - here.getX();
        double dz = destination.getZ() - here.getZ();
        handle.look((float) Math.toDegrees(Math.atan2(-dx, dz)), 0);
        handle.setInput(1, 0, randomJump(), sprint(true));
    }

    /** たまに少しの間だけダッシュする (前に歩いていないときはやめる)。ダッシュするなら true。 */
    private boolean sprint(boolean forward) {
        if (!forward) {
            sprintTicks = 0;
        } else if (sprintTicks > 0) {
            sprintTicks--;
        } else if (ThreadLocalRandom.current().nextDouble() < SPRINT_CHANCE) {
            sprintTicks = SPRINT_MIN_TICKS + ThreadLocalRandom.current().nextInt(SPRINT_MAX_TICKS - SPRINT_MIN_TICKS + 1);
        }
        return sprintTicks > 0;
    }

    /** 壁 (カーペットなどの段差も) にぶつかったらジャンプ。それ以外でもたまにジャンプする。 */
    private boolean randomJump() {
        return handle.onGround && (handle.positionChanged || ThreadLocalRandom.current().nextDouble() < JUMP_CHANCE);
    }

    /** 死んだとき (HP 0) や動けなくなったとき: Pit の復活地点に出し直す。 */
    void respawnAtPitSpawn() {
        despawn();
        home = spawnArea.randomPoint();
        stuckTicks = 0;
        spawn();
    }

    /**
     * 横向きに壁にぶつかっているか。すぐ隣 (足と頭の高さ) に当たり判定のあるブロックがあるときだけで、
     * カーペットや雪 (薄いブロック) は壁とみなさない。
     */
    private boolean isAgainstWall() {
        if (!handle.positionChanged) {
            return false;
        }
        Location here = handle.getBukkitEntity().getLocation();
        double fx = here.getX() - Math.floor(here.getX());
        double fz = here.getZ() - Math.floor(here.getZ());
        int[][] sides = {{fx < 0.31 ? -1 : 0, 0}, {fx > 0.69 ? 1 : 0, 0}, {0, fz < 0.31 ? -1 : 0}, {0, fz > 0.69 ? 1 : 0}};
        for (int[] side : sides) {
            if (side[0] == 0 && side[1] == 0) {
                continue;
            }
            for (int dy = 0; dy <= 1; dy++) {
                Material type = here.getBlock().getRelative(side[0], dy, side[1]).getType();
                if (type.isSolid() && type != Material.CARPET && type != Material.SNOW) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 同じワールドで一番近い、攻撃できるプレイヤーか Bot (スポーンにいる相手は狙わない)。 */
    private Player findTarget(List<Player> otherBots) {
        Location location = handle.getBukkitEntity().getLocation();
        List<Player> candidates = new ArrayList<>(Bukkit.getOnlinePlayers());
        candidates.addAll(otherBots);
        Player nearest = null;
        double best = TARGET_RANGE * TARGET_RANGE;
        for (Player player : candidates) {
            if (!player.getWorld().equals(location.getWorld()) || player.isDead() || spawnArea.isProtected(player.getLocation())
                    || (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE)) {
                continue;
            }
            double distance = player.getLocation().distanceSquared(location);
            if (distance < best) {
                best = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    private void swing() {
        PacketPlayOutAnimation packet = new PacketPlayOutAnimation(handle, 0);
        for (Player player : Bukkit.getOnlinePlayers()) {
            send(player, packet);
        }
    }

    private void broadcastInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction action) {
        PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo(action, handle);
        for (Player player : Bukkit.getOnlinePlayers()) {
            send(player, packet);
        }
    }

    private static void send(Player player, net.minecraft.server.v1_8_R3.Packet<?> packet) {
        EntityPlayer viewer = ((CraftPlayer) player).getHandle();
        if (viewer.playerConnection != null) {
            viewer.playerConnection.sendPacket(packet);
        }
    }

    /**
     * 頭はプレステージ V からその色の Fancy Hat (それ以外は無し)。胴・脚・足は必ずチェーン 2 + 鉄 1 (どこが鉄かはランダム)。
     * 出現 (復活) するたびに決め直す。
     */
    private static void equip(Player bot, org.bukkit.Color hat) {
        PlayerInventory inventory = bot.getInventory();
        inventory.setItem(0, new ItemStack(Material.IRON_SWORD));
        inventory.setHeldItemSlot(0);
        inventory.setHelmet(hat == null ? null : com.pitremake.perk.PerkItems.fancyHat(hat));

        List<Boolean> iron = new ArrayList<>(Arrays.asList(true, false, false));
        Collections.shuffle(iron, ThreadLocalRandom.current());
        inventory.setChestplate(new ItemStack(iron.get(0) ? Material.IRON_CHESTPLATE : Material.CHAINMAIL_CHESTPLATE));
        inventory.setLeggings(new ItemStack(iron.get(1) ? Material.IRON_LEGGINGS : Material.CHAINMAIL_LEGGINGS));
        inventory.setBoots(new ItemStack(iron.get(2) ? Material.IRON_BOOTS : Material.CHAINMAIL_BOOTS));
    }
}
