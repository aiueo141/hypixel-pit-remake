package com.pitremake.listener;

import org.bukkit.Difficulty;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * マップを変えないためのルール。
 * <pre>
 * 雪・氷 : 解けない、新しくできない
 * 火     : 燃え広がらない、ブロックを燃やさない
 * 水・溶岩: 空気のところにだけ流れる
 * 天気   : ずっと晴れ
 * 難易度 : イージー、空腹ゲージは減らない
 * 落下   : ダメージなし (スポーンから下まで 30 ブロック以上落ちる)
 * 耐久値 : 減らない
 * </pre>
 */
public final class WorldListener implements Listener {

    public WorldListener(JavaPlugin plugin) {
        for (World world : plugin.getServer().getWorlds()) {
            world.setStorm(false);
            world.setThundering(false);
            world.setDifficulty(Difficulty.EASY);
        }
    }

    /** 空腹ゲージは減らない (食べて増えるのはそのまま)。 */
    @EventHandler(ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player && event.getFoodLevel() < ((Player) event.getEntity()).getFoodLevel()) {
            event.setCancelled(true);
        }
    }

    /** 入ったときは満腹 (前に減っていた分を戻す)。 */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().setFoodLevel(20);
        event.getPlayer().setSaturation(5);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFade(BlockFadeEvent event) {
        if (isSnowOrIce(event.getBlock().getType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onForm(BlockFormEvent event) {
        if (isSnowOrIce(event.getNewState().getType())) {
            event.setCancelled(true);
        }
    }

    /** 火は燃え広がらない・ブロックを燃やさない (Lava Bucket の溶岩でマップのカーペットなどが燃えないように)。 */
    @EventHandler(ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        if (event.getCause() != BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        event.setCancelled(true);
    }

    /** 流れる水・溶岩は空気のところにだけ広がる (カーペット・松明などを流して消さない)。 */
    @EventHandler(ignoreCancelled = true)
    public void onFlow(BlockFromToEvent event) {
        Material target = event.getToBlock().getType();
        if (target != Material.AIR && !event.getToBlock().isLiquid()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onWeather(WeatherChangeEvent event) {
        if (event.toWeatherState()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onThunder(ThunderChangeEvent event) {
        if (event.toThunderState()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFall(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    /** アイテムは全部耐久無限 (減らない)。Kept on death でないものは死ぬと無くなる。 */
    @EventHandler(ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        event.setCancelled(true);
    }

    private static boolean isSnowOrIce(Material material) {
        return material == Material.ICE || material == Material.PACKED_ICE
                || material == Material.SNOW || material == Material.SNOW_BLOCK;
    }
}
