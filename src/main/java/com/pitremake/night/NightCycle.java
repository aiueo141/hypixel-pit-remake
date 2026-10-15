package com.pitremake.night;

import com.pitremake.PitPlugin;
import com.pitremake.prestige.HeresyMenu;
import com.pitremake.renown.RenownUpgrade;
import com.pitremake.special.SpecialItems;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Pit の昼と夜 ({@link NightRules}) と Night Quest。
 * <pre>
 * 昼 (config night.day-minutes) → 日暮れ 94 tick → 夜 (night.night-minutes) → 夜明け → 昼 …
 * 日暮れの 20 tick 後、Heresy を持っていて Night Quests をオフにしていないプレイヤーに
 *   §9§lNIGHT QUEST! §7Kill §c30 §7players!  (ホバー: Complete before daybreak: +650 XP & +1 Chunk of Vile)
 *   と portal.travel (0.31 / 126) (録画 2026-10-15_10-34-42.mcrc)
 * 夜明けまでに 30 キル (Bot を含む) で報酬。夜明けで未達成のクエストは消える
 * </pre>
 * 達成したときのチャットと音は録画に無いので推測。夜の途中で参加したプレイヤーにもクエストを出す。
 */
public final class NightCycle implements Listener {

    private enum Phase { DAY, NIGHTFALL, NIGHT, MORNING }

    private static final String QUEST_SOUND = "portal.travel";
    private static final float QUEST_SOUND_VOLUME = 0.31f;
    private static final float QUEST_SOUND_PITCH = 126 / 63f;
    /** 達成したときの音 (推測)。 */
    private static final String COMPLETE_SOUND = "random.levelup";

    private final PitPlugin plugin;
    /** Night Quest のキル数 (夜の間だけ)。 */
    private final Map<UUID, Integer> progress = new HashMap<>();
    private final Set<UUID> completed = new HashSet<>();
    private Phase phase = Phase.DAY;
    /** 今の段階に入ってからの tick 数。 */
    private int ticks;
    /** 日暮れの開始からの tick 数 (夜の長さを数える)。 */
    private int nightTicks;
    private BukkitTask task;

    public NightCycle(PitPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        World world = world();
        world.setGameRuleValue("doDaylightCycle", "false");
        world.setTime(NightRules.DAY_TIME);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1, 1);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    private World world() {
        return plugin.getServer().getWorlds().get(0);
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("night.enabled", true);
    }

    private int dayTicks() {
        return (int) Math.round(plugin.getConfig().getDouble("night.day-minutes", 20) * 60 * 20);
    }

    private int nightLengthTicks() {
        return (int) Math.round(plugin.getConfig().getDouble("night.night-minutes", 10) * 60 * 20);
    }

    /** 日暮れの途中から夜明けの前まで (Night Quest の期間)。 */
    public boolean isNight() {
        return phase == Phase.NIGHTFALL || phase == Phase.NIGHT;
    }

    /** /pit time night: すぐに日暮れを始める。 */
    public void forceNight() {
        if (phase == Phase.DAY || phase == Phase.MORNING) {
            startNightfall();
        }
    }

    /** /pit time day: すぐに夜明けを始める。 */
    public void forceDay() {
        if (phase == Phase.NIGHTFALL || phase == Phase.NIGHT) {
            startMorning();
        }
    }

    private void tick() {
        ticks++;
        switch (phase) {
            case DAY:
                if (enabled() && ticks >= dayTicks()) {
                    startNightfall();
                }
                break;
            case NIGHTFALL:
                nightTicks++;
                world().setTime(NightRules.nightfall(ticks));
                if (ticks == NightRules.QUEST_DELAY_TICKS) {
                    for (Player player : plugin.getServer().getOnlinePlayers()) {
                        giveQuest(player);
                    }
                }
                if (ticks >= NightRules.NIGHTFALL_TICKS) {
                    phase = Phase.NIGHT;
                }
                break;
            case NIGHT:
                nightTicks++;
                if (nightTicks >= nightLengthTicks()) {
                    startMorning();
                }
                break;
            case MORNING:
                world().setTime(NightRules.morning(ticks));
                if (ticks >= NightRules.MORNING_TICKS) {
                    phase = Phase.DAY;
                    ticks = 0;
                }
                break;
            default:
                break;
        }
    }

    private void startNightfall() {
        phase = Phase.NIGHTFALL;
        ticks = 0;
        nightTicks = 0;
        progress.clear();
        completed.clear();
        world().setTime(NightRules.DAY_TIME);
    }

    private void startMorning() {
        phase = Phase.MORNING;
        ticks = 0;
        // 夜明けで未達成のクエストは消える
        progress.clear();
        completed.clear();
        world().setTime(NightRules.NIGHT_TIME);
    }

    /** Night Quest を受けられるか: Heresy を持っていて、Heresy の画面で Night Quests をオフにしていない (Bot は除く)。 */
    private boolean eligible(Player player) {
        UUID id = player.getUniqueId();
        return !plugin.getBotManager().isBot(player)
                && plugin.getUpgradeManager().hasRenown(id, RenownUpgrade.HERESY)
                && !plugin.getUpgradeManager().isUnlocked(id, HeresyMenu.NIGHT_QUESTS_OFF);
    }

    private void giveQuest(Player player) {
        UUID id = player.getUniqueId();
        if (!eligible(player) || progress.containsKey(id) || completed.contains(id)) {
            return;
        }
        progress.put(id, 0);
        TextComponent message = new TextComponent(TextComponent.fromLegacyText(
                "§9§lNIGHT QUEST! §7Kill §c" + NightRules.QUEST_KILLS + " §7players!"));
        BaseComponent[] hover = new ComponentBuilder("Complete before daybreak: ").color(ChatColor.YELLOW)
                .append("+" + NightRules.QUEST_XP + " XP ").color(ChatColor.AQUA)
                .append("& ").color(ChatColor.GRAY)
                .append("+" + NightRules.QUEST_VILE + " Chunk of Vile").color(ChatColor.DARK_PURPLE)
                .create();
        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover));
        player.spigot().sendMessage(message);
        player.playSound(player.getLocation(), QUEST_SOUND, QUEST_SOUND_VOLUME, QUEST_SOUND_PITCH);
    }

    /** 夜の途中で参加したプレイヤーにもクエストを出す。 */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (isNight() && (phase == Phase.NIGHT || ticks >= NightRules.QUEST_DELAY_TICKS)) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && isNight()) {
                    giveQuest(player);
                }
            }, NightRules.QUEST_DELAY_TICKS);
        }
    }

    /** キルしたとき (KillListener から)。 */
    public void onKill(Player killer) {
        UUID id = killer.getUniqueId();
        Integer kills = progress.get(id);
        if (kills == null) {
            return;
        }
        kills++;
        if (kills < NightRules.QUEST_KILLS) {
            progress.put(id, kills);
            return;
        }
        progress.remove(id);
        completed.add(id);
        // 報酬 (録画のホバー: +650 XP & +1 Chunk of Vile)。チャットと音は推測
        plugin.getKillListener().giveXp(killer, NightRules.QUEST_XP);
        ItemStack vile = SpecialItems.vile(NightRules.QUEST_VILE);
        for (ItemStack left : killer.getInventory().addItem(vile).values()) {
            killer.getWorld().dropItem(killer.getLocation(), left);
        }
        plugin.getPlayerProfiles().addNightQuest(id);
        killer.sendMessage("§9§lNIGHT QUEST! §7Completed! §b+" + NightRules.QUEST_XP + " XP §7& §5+"
                + NightRules.QUEST_VILE + " Chunk of Vile");
        killer.playSound(killer.getLocation(), COMPLETE_SOUND, 1.0f, 1.0f);
    }

    /** 今の Night Quest のキル数。受けていなければ -1。 */
    public int getProgress(UUID player) {
        Integer kills = progress.get(player);
        return kills == null ? -1 : kills;
    }
}
