package com.pitremake.command;

import com.pitremake.PitPlugin;
import com.pitremake.event.PitEvent;
import com.pitremake.spire.SpireRules;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * /events: 開催中のイベントと、次に自動で始まるイベントまでの時間 (録画に無いので Pit のチャットの色に合わせた)。
 * 開催中のものが先、あとは始まるのが早い順。
 * /events auto [on|off] / /events auto &lt;spire|koth|auction|multireward&gt; [on|off] (pit.cmd.event):
 * 自動で始まるかのオン/オフ (config の &lt;key&gt;.enabled に保存)。オフにしても開催中のものはそのまま終わりまで続く。
 */
public final class EventsCommand implements CommandExecutor, TabCompleter {

    /** /events auto (/pitpermission の event)。 */
    private static final String ADMIN = "pit.cmd.event";

    private final PitPlugin plugin;

    public EventsCommand(PitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("auto")) {
            return auto(sender, label, args);
        }
        long now = System.currentTimeMillis();
        List<PitEvent> running = new ArrayList<>();
        List<PitEvent> upcoming = new ArrayList<>();
        for (PitEvent event : events()) {
            if (event.endsAt() > 0) {
                running.add(event);
            } else if (event.isScheduled()) {
                upcoming.add(event);
            }
        }
        upcoming.sort((a, b) -> Long.compare(a.nextStart(), b.nextStart()));

        sender.sendMessage("§e§lEVENTS");
        for (PitEvent event : running) {
            sender.sendMessage(" §a§lNOW " + event.displayName() + " §7ending in §a" + SpireRules.time(seconds(event.endsAt(), now)));
        }
        for (PitEvent event : upcoming) {
            sender.sendMessage(" " + event.displayName() + " §7in §e" + SpireRules.time(seconds(event.nextStart(), now)));
        }
        if (running.isEmpty() && upcoming.isEmpty()) {
            sender.sendMessage(" §7No events are scheduled.");
        }
        return true;
    }

    private boolean auto(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission(ADMIN)) {
            sender.sendMessage("§cYou don't have permission.");
            return true;
        }
        // /events auto [on|off] は全部、/events auto <名前> [on|off] は 1 つ
        List<PitEvent> targets = new ArrayList<>(events());
        int valueIndex = 1;
        if (args.length >= 2 && !isOnOff(args[1])) {
            PitEvent event = find(args[1]);
            if (event == null) {
                sender.sendMessage("§cUnknown event: " + args[1]);
                sender.sendMessage("§e/" + label + " auto [spire|koth|auction|multireward] [on|off]");
                return true;
            }
            targets = Collections.singletonList(event);
            valueIndex = 2;
        }
        if (args.length > valueIndex) {
            if (!isOnOff(args[valueIndex])) {
                sender.sendMessage("§cInvalid argument: " + args[valueIndex]);
                return true;
            }
            boolean on = args[valueIndex].equalsIgnoreCase("on");
            for (PitEvent event : targets) {
                plugin.getConfig().set(event.configKey() + ".enabled", on);
                if (on) {
                    // 止まっている間の古い時刻ですぐ始まらないように、次の時刻を決め直す
                    event.scheduleNext();
                }
            }
            plugin.saveConfig();
            if (on) {
                plugin.warnOverlaps(sender);
            }
        }
        for (PitEvent event : targets) {
            boolean on = plugin.getConfig().getBoolean(event.configKey() + ".enabled", false);
            sender.sendMessage("§7Auto " + event.displayName() + "§7: " + (on ? "§aON" : "§cOFF"));
        }
        return true;
    }

    private List<PitEvent> events() {
        return plugin.getEventSchedule().getEvents();
    }

    /** コマンドでの名前 (2X REWARDS は /pit multireward に合わせて multireward)。 */
    private static String name(PitEvent event) {
        return event.configKey().equals("double-rewards") ? "multireward" : event.configKey();
    }

    private PitEvent find(String name) {
        for (PitEvent event : events()) {
            if (name(event).equalsIgnoreCase(name) || event.configKey().equalsIgnoreCase(name)) {
                return event;
            }
        }
        return null;
    }

    private static boolean isOnOff(String arg) {
        return arg.equalsIgnoreCase("on") || arg.equalsIgnoreCase("off");
    }

    private static long seconds(long time, long now) {
        return (time - now + 999) / 1000;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (!sender.hasPermission(ADMIN)) {
            return options;
        }
        if (args.length == 1) {
            options.add("auto");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("auto")) {
            options.add("on");
            options.add("off");
            for (PitEvent event : events()) {
                options.add(name(event));
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("auto") && !isOnOff(args[1])) {
            options.add("on");
            options.add("off");
        }
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.startsWith(args[args.length - 1].toLowerCase())) {
                result.add(option);
            }
        }
        return result;
    }
}
