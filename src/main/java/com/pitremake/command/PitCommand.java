package com.pitremake.command;

import com.pitremake.PitPlugin;
import com.pitremake.combat.DamageContext;
import com.pitremake.item.MysticItems;
import com.pitremake.util.NbtUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class PitCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList("reload", "version", "nonce", "damage", "debug", "backup", "time", "auction", "spire", "koth", "multireward", "map");
    /** イベントをすぐ始める / 終わらせる、昼夜 (/pitpermission の event)。reload・map・backup などは pit.admin (op) だけ。 */
    private static final String EVENT_PERMISSION = "pit.cmd.event";

    private final PitPlugin plugin;

    public PitCommand(PitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " <" + String.join("|", SUBCOMMANDS) + ">");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("pit.admin")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission.");
                    return true;
                }
                plugin.reloadConfig();
                plugin.loadDamageStacking();
                plugin.rescheduleEvents(sender);
                sender.sendMessage(ChatColor.GREEN + "Config reloaded.");
                return true;
            case "damage":
                return damage(sender, args);
            case "time":
                return time(sender, args);
            case "auction":
                return auction(sender, args);
            case "spire":
                return spire(sender, args);
            case "koth":
                return koth(sender, args);
            case "multireward":
                return multiReward(sender, args);
            case "map":
                return map(sender, args);
            case "backup":
                // ワールド (地形・置かれたブロック) と plugins/Pit を backups/ に zip で保存
                if (!sender.hasPermission("pit.admin")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission.");
                    return true;
                }
                plugin.getBackupManager().backup(sender);
                return true;
            case "debug":
                if (!(sender instanceof Player) || !sender.hasPermission("pit.admin")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission.");
                    return true;
                }
                boolean on = plugin.getDamageCalculator().toggleDebug((Player) sender);
                sender.sendMessage(on ? ChatColor.GREEN + "Damage debug ON: every hit you deal or take is shown in chat."
                        : ChatColor.YELLOW + "Damage debug OFF.");
                return true;
            case "version":
                sender.sendMessage(ChatColor.GOLD + "Pit v" + plugin.getDescription().getVersion());
                return true;
            case "nonce":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                    return true;
                }
                ItemStack held = ((Player) sender).getItemInHand();
                if (!NbtUtil.has(held, MysticItems.NONCE)) {
                    sender.sendMessage(ChatColor.RED + "This item has no nonce.");
                    return true;
                }
                sender.sendMessage(ChatColor.GRAY + "Nonce: " + ChatColor.WHITE + NbtUtil.getInt(held, MysticItems.NONCE));
                return true;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand: " + args[0]);
                return true;
        }
    }

    /** /pit damage [additive|multiplicative] : ダメージの増減の重ね方を見る・変える (config にも保存)。 */
    private boolean damage(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Damage stacking: " + ChatColor.WHITE
                    + DamageContext.getStacking().name().toLowerCase() + ChatColor.GRAY + " (/pit damage <additive|multiplicative>)");
            return true;
        }
        if (!sender.hasPermission("pit.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }
        DamageContext.Stacking stacking = DamageContext.Stacking.fromName(args[1]);
        if (stacking == null) {
            sender.sendMessage(ChatColor.RED + "Use additive or multiplicative.");
            return true;
        }
        DamageContext.setStacking(stacking);
        plugin.getConfig().set("damage.stacking", stacking.name().toLowerCase());
        plugin.saveConfig();
        sender.sendMessage(ChatColor.GREEN + "Damage stacking is now " + stacking.name().toLowerCase() + ".");
        return true;
    }

    /** /pit auction <start|end> : オークションをすぐに始める / 終わらせる (確認用)。 */
    private boolean auction(CommandSender sender, String[] args) {
        if (!sender.hasPermission(EVENT_PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Auction: " + (plugin.getAuctionManager().isRunning() ? "running" : "not running")
                    + ChatColor.GRAY + " (/pit auction <start|end>)");
            return true;
        }
        if (args[1].equalsIgnoreCase("start")) {
            sender.sendMessage(plugin.getAuctionManager().begin() ? ChatColor.GREEN + "Auction started."
                    : ChatColor.RED + "An event (Auction / Spire / KOTH) is already running.");
        } else if (args[1].equalsIgnoreCase("end")) {
            sender.sendMessage(plugin.getAuctionManager().end() ? ChatColor.GREEN + "Auction ended."
                    : ChatColor.RED + "No auction is running.");
        } else {
            sender.sendMessage(ChatColor.RED + "Use start or end.");
        }
        return true;
    }

    /** /pit spire|koth addpos <名前> / removepos / positions : 出す場所 (立っている地面のブロック = 建物の真ん中の真下)。 */
    private boolean positions(CommandSender sender, String[] args, com.pitremake.map.EventPositions positions, String what) {
        // 出す場所はマップのデータなので op だけ (/pitpermission の event では変えられない)
        if (!sender.hasPermission("pit.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }
        String sub = args[1].toLowerCase();
        if (sub.equals("positions")) {
            List<com.pitremake.map.EventPositions.Position> list = positions.list();
            String map = plugin.getPitMap().getName();
            sender.sendMessage(ChatColor.YELLOW + what + " positions on " + map + " ("
                    + (positions.overridden() ? "config.yml " + positions.path() : "built into the plugin") + ")"
                    + (!list.isEmpty() ? ":" : positions.recorded() != null ? ": none, using the recorded one " + positions.recorded()
                    : ": none, so no " + what + " on this map"));
            for (com.pitremake.map.EventPositions.Position position : list) {
                sender.sendMessage(ChatColor.GRAY + " - " + position);
            }
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        if (sub.equals("addpos")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "/pit " + what.toLowerCase() + " addpos <name>  (stand on the ground at the center)");
                return true;
            }
            String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            sender.sendMessage(ChatColor.GREEN + "Added " + what + " position " + positions.add(player.getLocation(), name));
        } else {
            com.pitremake.map.EventPositions.Position removed = positions.removeNear(player.getLocation());
            sender.sendMessage(removed == null ? ChatColor.RED + "No " + what + " position within 5 blocks."
                    : ChatColor.GREEN + "Removed " + what + " position " + removed);
        }
        return true;
    }

    /** /pit map save : 今の地形を元 (起動するたびに戻す地形) として保存する。 */
    private boolean map(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pit.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase("save")) {
            sender.sendMessage(ChatColor.YELLOW + "/pit map save" + ChatColor.GRAY
                    + " : save the current terrain as the map restored on every start");
            return true;
        }
        if (plugin.getSpireEvent().isActive() || plugin.getKothEvent().isRunning()) {
            sender.sendMessage(ChatColor.RED + "Wait until the Spire / KOTH is over (its blocks would be saved too).");
            return true;
        }
        try {
            int files = new com.pitremake.map.MapTemplate(plugin).save(plugin.getServer().getWorlds().get(0));
            sender.sendMessage(ChatColor.GREEN + "Saved the map template (" + files + " region files).");
        } catch (java.io.IOException e) {
            sender.sendMessage(ChatColor.RED + "Failed: " + e.getMessage());
        }
        return true;
    }

    /**
     * /pit multireward &lt;X&gt; start : 区域 (Mid) の中のキルの XP・ゴールドが X 倍の Minor Event を始める (4 分)。
     * /pit multireward end : 終わらせる。
     */
    private boolean multiReward(CommandSender sender, String[] args) {
        if (!sender.hasPermission(EVENT_PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }
        com.pitremake.rewards.DoubleRewardsEvent event = plugin.getDoubleRewards();
        String usage = ChatColor.GRAY + " (/pit multireward <X> start, /pit multireward end)";
        if (args.length >= 2 && args[1].equalsIgnoreCase("end")) {
            sender.sendMessage(event.end() ? ChatColor.GREEN + "Reward event ended." : ChatColor.RED + "No reward event is running.");
            return true;
        }
        if (args.length < 3 || !args[2].equalsIgnoreCase("start")) {
            sender.sendMessage(ChatColor.YELLOW + "Reward event: " + (event.isRunning()
                    ? "running (" + com.pitremake.rewards.DoubleRewardsEvent.label(event.getMultiplier()) + "x)" : "not running") + usage);
            return true;
        }
        double multiplier;
        try {
            multiplier = Double.parseDouble(args[1].toLowerCase().replace("x", ""));
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Not a multiplier: " + args[1] + usage);
            return true;
        }
        if (multiplier <= 0) {
            sender.sendMessage(ChatColor.RED + "The multiplier must be more than 0.");
            return true;
        }
        sender.sendMessage(event.begin(multiplier)
                ? ChatColor.GREEN + com.pitremake.rewards.DoubleRewardsEvent.label(multiplier) + "x reward event started."
                : ChatColor.RED + "An event (Auction / Spire / KOTH / reward event) is already running.");
        return true;
    }

    /** /pit koth <start|end> : KOTH をすぐに始める / 終わらせる (確認用)。 */
    private boolean koth(CommandSender sender, String[] args) {
        if (!sender.hasPermission(EVENT_PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }
        com.pitremake.koth.KothEvent koth = plugin.getKothEvent();
        if (args.length >= 2 && Arrays.asList("addpos", "removepos", "positions").contains(args[1].toLowerCase())) {
            return positions(sender, args, koth.getPositions(), "KOTH");
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "KOTH: " + (koth.isRunning() ? "running" : "not running")
                    + ChatColor.GRAY + " (/pit koth <start|end|addpos|removepos|positions>)");
            return true;
        }
        if (args[1].equalsIgnoreCase("start")) {
            sender.sendMessage(koth.begin() ? ChatColor.GREEN + "KOTH started."
                    : ChatColor.RED + "An event (Auction / Spire / KOTH) is already running (or this map has no hill).");
        } else if (args[1].equalsIgnoreCase("end")) {
            sender.sendMessage(koth.end() ? ChatColor.GREEN + "KOTH ended." : ChatColor.RED + "No KOTH is running.");
        } else {
            sender.sendMessage(ChatColor.RED + "Use start or end.");
        }
        return true;
    }

    /** /pit spire <start|now|end> : Spire を 3 分前の予告から / すぐに始める、終わらせる (確認用)。 */
    private boolean spire(CommandSender sender, String[] args) {
        if (!sender.hasPermission(EVENT_PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }
        com.pitremake.spire.SpireEvent spire = plugin.getSpireEvent();
        if (args.length >= 2 && Arrays.asList("addpos", "removepos", "positions").contains(args[1].toLowerCase())) {
            return positions(sender, args, spire.getPositions(), "Spire");
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Spire: " + (spire.isRunning() ? "running" : spire.isActive() ? "starting soon" : "not running")
                    + ChatColor.GRAY + " (/pit spire <start|now|end|addpos|removepos|positions>)");
            return true;
        }
        if (args[1].equalsIgnoreCase("start")) {
            int seconds = com.pitremake.spire.SpireRules.WARNING_SECONDS;
            if (args.length >= 3) {
                try {
                    seconds = Math.max(1, Integer.parseInt(args[2]));
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Not a number: " + args[2]);
                    return true;
                }
            }
            sender.sendMessage(spire.warn(seconds) ? ChatColor.GREEN + "Spire starts in " + seconds + "s."
                    : ChatColor.RED + "An event (Auction / Spire / KOTH) is already running (or this map has no Spire).");
        } else if (args[1].equalsIgnoreCase("now")) {
            sender.sendMessage(spire.beginNow() ? ChatColor.GREEN + "Spire started."
                    : ChatColor.RED + "An event (Auction / Spire / KOTH) is already running (or this map has no Spire).");
        } else if (args[1].equalsIgnoreCase("end")) {
            sender.sendMessage(spire.end() ? ChatColor.GREEN + "Spire ended." : ChatColor.RED + "No Spire is running.");
        } else {
            sender.sendMessage(ChatColor.RED + "Use start, now or end.");
        }
        return true;
    }

    /** /pit time <day|night> : すぐに夜明け / 日暮れを始める (Night Quest の確認用)。 */
    private boolean time(CommandSender sender, String[] args) {
        if (!sender.hasPermission(EVENT_PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "It is " + (plugin.getNightCycle().isNight() ? "night" : "day")
                    + ChatColor.GRAY + " (/pit time <day|night>)");
            return true;
        }
        if (args[1].equalsIgnoreCase("night")) {
            plugin.getNightCycle().forceNight();
            sender.sendMessage(ChatColor.GREEN + "Night is falling.");
        } else if (args[1].equalsIgnoreCase("day")) {
            plugin.getNightCycle().forceDay();
            sender.sendMessage(ChatColor.GREEN + "Day is breaking.");
        } else {
            sender.sendMessage(ChatColor.RED + "Use day or night.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 2 && args[0].equalsIgnoreCase("damage")) {
            return Arrays.asList("additive", "multiplicative");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("time")) {
            return Arrays.asList("day", "night");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("spire")) {
            return Arrays.asList("start", "now", "end", "addpos", "removepos", "positions");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("multireward")) {
            return Arrays.asList("2", "3", "end");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("multireward") && !args[1].equalsIgnoreCase("end")) {
            return Arrays.asList("start");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("map")) {
            return Arrays.asList("save");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("koth")) {
            return Arrays.asList("start", "end", "addpos", "removepos", "positions");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("auction")) {
            return Arrays.asList("start", "end");
        }
        if (args.length != 1) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String sub : SUBCOMMANDS) {
            if (sub.startsWith(args[0].toLowerCase())) {
                result.add(sub);
            }
        }
        return result;
    }
}
