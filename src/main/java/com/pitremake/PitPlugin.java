package com.pitremake;

import com.pitremake.bot.BotManager;
import com.pitremake.bounty.BountyEffects;
import com.pitremake.bounty.BountyManager;
import com.pitremake.combat.AccountDamage;
import com.pitremake.combat.BowEffects;
import com.pitremake.combat.DamageCalculator;
import com.pitremake.combat.DamageContext;
import com.pitremake.combat.EnchantEffects;
import com.pitremake.command.BotCommand;
import com.pitremake.command.PitBountyCommand;
import com.pitremake.command.PitNpcCommand;
import com.pitremake.command.PitLevelCommand;
import com.pitremake.command.PitPrestigeCommand;
import com.pitremake.command.PitRenownCommand;
import com.pitremake.perk.PerkManager;
import com.pitremake.command.CoolCommand;
import com.pitremake.command.OofCommand;
import com.pitremake.command.SpawnCommand;
import com.pitremake.map.PitMapType;
import com.pitremake.renown.RenownEffects;
import com.pitremake.shop.SpawnKit;
import com.pitremake.spawn.Launcher;
import com.pitremake.stats.KillListener;
import com.pitremake.command.ViewCommand;
import com.pitremake.command.PitSpawnCommand;
import com.pitremake.command.PitStreakCommand;
import com.pitremake.spawn.PitSpawn;
import com.pitremake.npc.NpcManager;
import com.pitremake.hologram.HologramManager;
import com.pitremake.prestige.PrestigeMenu;
import com.pitremake.rank.PitChatListener;
import com.pitremake.rank.RankManager;
import com.pitremake.rank.RankMenu;
import com.pitremake.shop.ItemShopMenu;
import com.pitremake.shop.ShopItemListener;
import com.pitremake.util.ClickMenuListener;
import com.pitremake.upgrade.MegastreakManager;
import com.pitremake.upgrade.UpgradeManager;
import com.pitremake.upgrade.UpgradeMenu;
import com.pitremake.command.FillMysticCommand;
import com.pitremake.command.FreshCommand;
import com.pitremake.command.InstantUpgradeCommand;
import com.pitremake.command.MysticEnchantCommand;
import com.pitremake.command.PitCommand;
import com.pitremake.command.PitGoldCommand;
import com.pitremake.enchant.EnchantRegistry;
import com.pitremake.enchant.list.AquaEnchants;
import com.pitremake.enchant.list.BowEnchants;
import com.pitremake.enchant.list.DarkEnchants;
import com.pitremake.enchant.list.PantsEnchants;
import com.pitremake.enchant.list.RageEnchants;
import com.pitremake.enchant.list.ResourceEnchants;
import com.pitremake.enchant.list.SewerEnchants;
import com.pitremake.enchant.list.SwordEnchants;
import com.pitremake.gem.GemListener;
import com.pitremake.gold.GoldManager;
import com.pitremake.listener.ArmorListener;
import com.pitremake.listener.PlayerListener;
import com.pitremake.listener.WorldListener;
import com.pitremake.scoreboard.PitSidebar;
import com.pitremake.special.SpecialItemListener;
import com.pitremake.stats.StatsManager;
import com.pitremake.well.MysticWellListener;
import com.pitremake.well.MysticWellMenu;
import com.pitremake.well.MysticWellRoller;
import com.pitremake.well.MysticWellStorage;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class PitPlugin extends JavaPlugin {

    private static PitPlugin instance;

    private EnchantRegistry enchantRegistry;
    private MysticWellRoller wellRoller;
    private MysticWellStorage wellStorage;
    private GoldManager goldManager;
    private BotManager botManager;
    private StatsManager statsManager;
    private PitSidebar sidebar;
    private BountyManager bountyManager;
    private BountyEffects bountyEffects;
    private NpcManager npcManager;
    private UpgradeManager upgradeManager;
    private MegastreakManager megastreakManager;
    private PitSpawn pitSpawn;
    private ShopItemListener shopItemListener;
    private com.pitremake.combat.SimpleEnchants simpleEnchants;
    private com.pitremake.renown.EnderChestListener enderChests;
    private com.pitremake.upgrade.KillstreakManager killstreakManager;
    private com.pitremake.gold.GoldIngots goldIngots;
    private com.pitremake.data.PlayerProfiles playerProfiles;
    private com.pitremake.data.BackupManager backupManager;
    private RankManager rankManager;
    private DamageCalculator damageCalculator;
    private EnchantEffects enchantEffects;
    private HologramManager hologramManager;
    private com.pitremake.leaderboard.Leaderboard leaderboard;
    private PerkManager perkManager;
    private RenownEffects renownEffects;
    private Launcher launcher;
    private com.pitremake.night.NightCycle nightCycle;
    private com.pitremake.spire.SpireEvent spireEvent;
    private com.pitremake.koth.KothEvent kothEvent;
    private com.pitremake.rewards.DoubleRewardsEvent doubleRewards;
    private com.pitremake.stash.ItemStash itemStash;
    private com.pitremake.util.EventBossBar eventBossBar;
    private com.pitremake.util.PacketTap packetTap;
    private com.pitremake.auction.AuctionManager auctionManager;
    private com.pitremake.event.EventSchedule eventSchedule;
    private com.pitremake.permission.PitPermissions permissions;
    private com.pitremake.trade.TradeManager tradeManager;
    private KillListener killListener;
    private PitMapType pitMap = PitMapType.SEASONS;

    /** ワールドを読む前: 地形を元 (plugins/Pit/map-template) に戻す。 */
    @Override
    public void onLoad() {
        saveDefaultConfig();
        new com.pitremake.map.MapTemplate(this).restoreOnStartup();
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        PitMapType configured = PitMapType.fromName(getConfig().getString("map", "Seasons"));
        if (configured != null) {
            pitMap = configured;
        }
        loadDamageStacking();

        enchantRegistry = new EnchantRegistry();
        ResourceEnchants.registerAll(enchantRegistry);
        SwordEnchants.registerAll(enchantRegistry);
        BowEnchants.registerAll(enchantRegistry);
        PantsEnchants.registerAll(enchantRegistry);
        RageEnchants.registerAll(enchantRegistry);
        DarkEnchants.registerAll(enchantRegistry);
        SewerEnchants.registerAll(enchantRegistry);
        AquaEnchants.registerAll(enchantRegistry);
        wellRoller = new MysticWellRoller(enchantRegistry);
        wellStorage = new MysticWellStorage(this);
        wellStorage.load();
        goldManager = new GoldManager(this);
        goldManager.load();
        statsManager = new StatsManager(this);
        statsManager.load();
        bountyManager = new BountyManager();
        bountyEffects = new BountyEffects(this, bountyManager);
        bountyEffects.start();
        upgradeManager = new UpgradeManager(this);
        upgradeManager.load();
        rankManager = new RankManager(this);
        rankManager.load();
        megastreakManager = new MegastreakManager(this);
        megastreakManager.start();
        npcManager = new NpcManager(this);
        npcManager.setClickHandler((player, type) -> {
            switch (type) {
                case UPGRADES:
                    new UpgradeMenu(this, player).open();
                    break;
                case ITEMS:
                    new ItemShopMenu(this, player).open();
                    break;
                case PRESTIGE:
                    new PrestigeMenu(this, player).open();
                    break;
                case RANKS:
                    new RankMenu(this, player).open();
                    break;
                case STATS:
                    new com.pitremake.stats.PitStatsMenu(this, player).open();
                    break;
                case QUEST_MASTER:
                    new com.pitremake.quest.QuestMenu(this, player).open();
                    break;
                default:
                    break;
            }
        });
        npcManager.start();
        pitSpawn = new PitSpawn(this);
        botManager = new BotManager(this, pitSpawn);
        botManager.start();
        shopItemListener = new ShopItemListener(this);
        shopItemListener.start();
        hologramManager = new HologramManager(this);
        hologramManager.start();
        // ダメージの増減はすべてここで集めて掛け算する (アカウントの補正・メガストリーク・エンチャント・パーク)
        damageCalculator = new DamageCalculator();
        enchantEffects = new EnchantEffects(this);
        damageCalculator.register(new AccountDamage(this));
        damageCalculator.register(megastreakManager);
        damageCalculator.register(enchantEffects);
        damageCalculator.register(shopItemListener);
        renownEffects = new RenownEffects(this);
        perkManager = new PerkManager(this);
        damageCalculator.register(perkManager);
        simpleEnchants = new com.pitremake.combat.SimpleEnchants(this);
        damageCalculator.register(simpleEnchants);
        damageCalculator.setInterceptor(simpleEnchants::interceptHit);
        killstreakManager = new com.pitremake.upgrade.KillstreakManager(this);
        damageCalculator.register(killstreakManager);

        killListener = new KillListener(this);
        launcher = new Launcher(this);
        launcher.start();
        nightCycle = new com.pitremake.night.NightCycle(this);
        nightCycle.start();
        packetTap = new com.pitremake.util.PacketTap(this);
        packetTap.start();
        eventSchedule = new com.pitremake.event.EventSchedule(this);
        auctionManager = new com.pitremake.auction.AuctionManager(this);
        auctionManager.start();
        tradeManager = new com.pitremake.trade.TradeManager(this);
        tradeManager.start();
        permissions = new com.pitremake.permission.PitPermissions(this);
        permissions.start();
        registerCommands();
        registerListeners();
        // PlayerProfiles (registerListeners で作る) を使うので、その後で出す
        leaderboard = new com.pitremake.leaderboard.Leaderboard(this);
        leaderboard.start();
        // 前回途中で止まった Spire の地形もここで戻す
        itemStash = new com.pitremake.stash.ItemStash(this);
        itemStash.start();
        eventBossBar = new com.pitremake.util.EventBossBar(this);
        eventBossBar.start();
        spireEvent = new com.pitremake.spire.SpireEvent(this);
        damageCalculator.register(spireEvent);
        spireEvent.start();
        kothEvent = new com.pitremake.koth.KothEvent(this);
        kothEvent.start();
        doubleRewards = new com.pitremake.rewards.DoubleRewardsEvent(this);
        doubleRewards.start();
        eventSchedule.add(spireEvent);
        eventSchedule.add(kothEvent);
        eventSchedule.add(doubleRewards);
        eventSchedule.add(auctionManager);
        warnOverlaps(null);
        sidebar = new PitSidebar(this);
        sidebar.start();

        getLogger().info("Pit v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        // 塔を壊して地形を戻し、預かった持ち物を返す (ほかより先に)
        if (spireEvent != null) {
            spireEvent.stop();
        }
        if (kothEvent != null) {
            kothEvent.stop();
        }
        if (doubleRewards != null) {
            doubleRewards.stop();
        }
        if (eventBossBar != null) {
            eventBossBar.stop();
        }
        if (sidebar != null) {
            sidebar.stop();
        }
        if (bountyEffects != null) {
            bountyEffects.stop();
        }
        if (npcManager != null) {
            npcManager.stop();
        }
        if (hologramManager != null) {
            hologramManager.stop();
        }
        if (leaderboard != null) {
            leaderboard.stop();
        }
        if (shopItemListener != null) {
            shopItemListener.stop();
        }
        if (goldIngots != null) {
            goldIngots.stop();
        }
        if (playerProfiles != null) {
            playerProfiles.stop();
        }
        if (perkManager != null) {
            perkManager.stop();
        }
        if (launcher != null) {
            launcher.stop();
        }
        if (nightCycle != null) {
            nightCycle.stop();
        }
        if (permissions != null) {
            permissions.stop();
        }
        if (auctionManager != null) {
            auctionManager.stop();
        }
        if (tradeManager != null) {
            tradeManager.stop();
        }
        if (packetTap != null) {
            packetTap.stop();
        }
        if (megastreakManager != null) {
            megastreakManager.stop();
        }
        if (upgradeManager != null) {
            upgradeManager.save();
        }
        if (rankManager != null) {
            rankManager.save();
        }
        if (botManager != null) {
            botManager.stop();
        }
        // 開いている Mystic Well を閉じる (アイテムは井戸のストレージに残る)
        for (Player player : getServer().getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof MysticWellMenu) {
                player.closeInventory();
            }
        }
        if (wellStorage != null) {
            wellStorage.save();
        }
        if (goldManager != null) {
            goldManager.save();
        }
        if (statsManager != null) {
            statsManager.save();
        }
        getLogger().info("Pit disabled.");
        instance = null;
    }

    private void registerCommands() {
        PluginCommand pit = getCommand("pit");
        PitCommand executor = new PitCommand(this);
        pit.setExecutor(executor);
        pit.setTabCompleter(executor);

        PluginCommand fresh = getCommand("fresh");
        FreshCommand freshExecutor = new FreshCommand();
        fresh.setExecutor(freshExecutor);
        fresh.setTabCompleter(freshExecutor);

        PluginCommand mysticEnchant = getCommand("menchant");
        MysticEnchantCommand mysticEnchantExecutor = new MysticEnchantCommand(this);
        mysticEnchant.setExecutor(mysticEnchantExecutor);
        mysticEnchant.setTabCompleter(mysticEnchantExecutor);

        getCommand("fillmystic").setExecutor(new FillMysticCommand());
        getCommand("trade").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player)) {
                return true;
            }
            if (args.length != 1) {
                sender.sendMessage("§cUsage: /" + label + " <player>");
                return true;
            }
            tradeManager.request((org.bukkit.entity.Player) sender, getServer().getPlayerExact(args[0]));
            return true;
        });
        getCommand("eventbook").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                spireEvent.openBook((org.bukkit.entity.Player) sender);
            }
            return true;
        });
        getCommand("tradeaccept").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player && args.length == 1) {
                tradeManager.accept((org.bukkit.entity.Player) sender, args[0]);
            }
            return true;
        });
        PluginCommand events = getCommand("events");
        com.pitremake.command.EventsCommand eventsExecutor = new com.pitremake.command.EventsCommand(this);
        events.setExecutor(eventsExecutor);
        events.setTabCompleter(eventsExecutor);
        getCommand("auctionmenu").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player) {
                auctionManager.open((org.bukkit.entity.Player) sender);
            }
            return true;
        });

        PluginCommand instantUpgrade = getCommand("instantupgrade");
        InstantUpgradeCommand instantUpgradeExecutor = new InstantUpgradeCommand(this);
        instantUpgrade.setExecutor(instantUpgradeExecutor);
        instantUpgrade.setTabCompleter(instantUpgradeExecutor);

        PluginCommand pitBot = getCommand("pitbot");
        BotCommand pitBotExecutor = new BotCommand(botManager, pitSpawn::randomPoint);
        pitBot.setExecutor(pitBotExecutor);
        pitBot.setTabCompleter(pitBotExecutor);

        PluginCommand pitGold = getCommand("pitgold");
        PitGoldCommand pitGoldExecutor = new PitGoldCommand(goldManager);
        pitGold.setExecutor(pitGoldExecutor);
        pitGold.setTabCompleter(pitGoldExecutor);

        PluginCommand pitNpc = getCommand("pitnpc");
        PitNpcCommand pitNpcExecutor = new PitNpcCommand(npcManager);
        pitNpc.setExecutor(pitNpcExecutor);
        pitNpc.setTabCompleter(pitNpcExecutor);

        PluginCommand pitSpawnCommand = getCommand("pitspawn");
        PitSpawnCommand pitSpawnExecutor = new PitSpawnCommand(pitSpawn);
        pitSpawnCommand.setExecutor(pitSpawnExecutor);
        pitSpawnCommand.setTabCompleter(pitSpawnExecutor);

        PluginCommand view = getCommand("view");
        ViewCommand viewExecutor = new ViewCommand(this);
        view.setExecutor(viewExecutor);
        view.setTabCompleter(viewExecutor);

        PluginCommand pitLevel = getCommand("pitlevel");
        PitLevelCommand pitLevelExecutor = new PitLevelCommand(this);
        pitLevel.setExecutor(pitLevelExecutor);
        pitLevel.setTabCompleter(pitLevelExecutor);

        PluginCommand pitStreak = getCommand("pitstreak");
        PitStreakCommand pitStreakExecutor = new PitStreakCommand(this);
        pitStreak.setExecutor(pitStreakExecutor);
        pitStreak.setTabCompleter(pitStreakExecutor);

        getCommand("oof").setExecutor(new OofCommand(this));
        getCommand("spawn").setExecutor(new SpawnCommand(this));
        getCommand("cool").setExecutor(new CoolCommand(this));

        PluginCommand pitPrestige = getCommand("pitprestige");
        PitPrestigeCommand pitPrestigeExecutor = new PitPrestigeCommand(this);
        pitPrestige.setExecutor(pitPrestigeExecutor);
        pitPrestige.setTabCompleter(pitPrestigeExecutor);

        PluginCommand pitRenown = getCommand("pitrenown");
        PitRenownCommand pitRenownExecutor = new PitRenownCommand(this);
        pitRenown.setExecutor(pitRenownExecutor);
        pitRenown.setTabCompleter(pitRenownExecutor);

        PluginCommand pitPermission = getCommand("pitpermission");
        com.pitremake.command.PitPermissionCommand pitPermissionExecutor = new com.pitremake.command.PitPermissionCommand(permissions);
        pitPermission.setExecutor(pitPermissionExecutor);
        pitPermission.setTabCompleter(pitPermissionExecutor);

        PluginCommand pitBounty = getCommand("pitbounty");
        PitBountyCommand pitBountyExecutor = new PitBountyCommand(this);
        pitBounty.setExecutor(pitBountyExecutor);
        pitBounty.setTabCompleter(pitBountyExecutor);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new ArmorListener(), this);
        getServer().getPluginManager().registerEvents(new MysticWellListener(this), this);
        getServer().getPluginManager().registerEvents(new GemListener(this), this);
        getServer().getPluginManager().registerEvents(new SpecialItemListener(this), this);
        getServer().getPluginManager().registerEvents(killListener, this);
        getServer().getPluginManager().registerEvents(pitSpawn, this);
        getServer().getPluginManager().registerEvents(new WorldListener(this), this);
        getServer().getPluginManager().registerEvents(new ClickMenuListener(), this);
        getServer().getPluginManager().registerEvents(new com.pitremake.stats.StatsTracker(this), this);
        getServer().getPluginManager().registerEvents(shopItemListener, this);
        getServer().getPluginManager().registerEvents(new PitChatListener(this), this);
        getServer().getPluginManager().registerEvents(damageCalculator, this);
        getServer().getPluginManager().registerEvents(enchantEffects, this);
        getServer().getPluginManager().registerEvents(new BowEffects(this), this);
        getServer().getPluginManager().registerEvents(perkManager, this);
        getServer().getPluginManager().registerEvents(renownEffects, this);
        enderChests = new com.pitremake.renown.EnderChestListener(this);
        getServer().getPluginManager().registerEvents(enderChests, this);
        getServer().getPluginManager().registerEvents(simpleEnchants, this);
        getServer().getPluginManager().registerEvents(killstreakManager, this);
        goldIngots = new com.pitremake.gold.GoldIngots(this);
        getServer().getPluginManager().registerEvents(goldIngots, this);
        goldIngots.start();
        playerProfiles = new com.pitremake.data.PlayerProfiles(this);
        getServer().getPluginManager().registerEvents(playerProfiles, this);
        playerProfiles.start();
        backupManager = new com.pitremake.data.BackupManager(this);
        backupManager.start();
        getServer().getPluginManager().registerEvents(new com.pitremake.spawn.SpawnRules(this), this);
        getServer().getPluginManager().registerEvents(new com.pitremake.prestige.FancyHatMenu.ChatInput(this), this);
    }

    /** プレステージ (Bot は Bot のプレステージ)。 */
    public int prestigeOf(Player player) {
        int bot = botManager.prestigeOf(player);
        return bot >= 0 ? bot : statsManager.getPrestige(player.getUniqueId());
    }

    /** Pit レベル (Bot は Bot のレベル)。 */
    public int levelOf(Player player) {
        int bot = botManager.levelOf(player);
        return bot > 0 ? bot : statsManager.getLevel(player.getUniqueId());
    }

    public static PitPlugin getInstance() {
        return instance;
    }

    public EnchantRegistry getEnchantRegistry() {
        return enchantRegistry;
    }

    public MysticWellRoller getWellRoller() {
        return wellRoller;
    }

    public MysticWellStorage getWellStorage() {
        return wellStorage;
    }

    public GoldManager getGoldManager() {
        return goldManager;
    }

    public BotManager getBotManager() {
        return botManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public PitSidebar getSidebar() {
        return sidebar;
    }

    public BountyManager getBountyManager() {
        return bountyManager;
    }

    public PitSpawn getPitSpawn() {
        return pitSpawn;
    }

    public com.pitremake.data.PlayerProfiles getPlayerProfiles() {
        return playerProfiles;
    }

    public com.pitremake.data.BackupManager getBackupManager() {
        return backupManager;
    }

    /** Pit のデータを全部ファイルに書く (バックアップの前)。 */
    public void saveAllData() {
        statsManager.save();
        goldManager.save();
        upgradeManager.save();
        rankManager.save();
        wellStorage.save();
        if (playerProfiles != null) {
            playerProfiles.stop();
        }
    }

    public com.pitremake.gold.GoldIngots getGoldIngots() {
        return goldIngots;
    }

    public com.pitremake.upgrade.KillstreakManager getKillstreakManager() {
        return killstreakManager;
    }

    public com.pitremake.renown.EnderChestListener getEnderChests() {
        return enderChests;
    }

    public com.pitremake.combat.SimpleEnchants getSimpleEnchants() {
        return simpleEnchants;
    }

    public ShopItemListener getShopItemListener() {
        return shopItemListener;
    }

    public RankManager getRankManager() {
        return rankManager;
    }

    public DamageCalculator getDamageCalculator() {
        return damageCalculator;
    }

    public EnchantEffects getEnchantEffects() {
        return enchantEffects;
    }

    public UpgradeManager getUpgradeManager() {
        return upgradeManager;
    }

    public MegastreakManager getMegastreakManager() {
        return megastreakManager;
    }

    public PerkManager getPerkManager() {
        return perkManager;
    }

    public RenownEffects getRenownEffects() {
        return renownEffects;
    }

    public Launcher getLauncher() {
        return launcher;
    }

    public com.pitremake.night.NightCycle getNightCycle() {
        return nightCycle;
    }

    public com.pitremake.spire.SpireEvent getSpireEvent() {
        return spireEvent;
    }

    public com.pitremake.stash.ItemStash getItemStash() {
        return itemStash;
    }

    public com.pitremake.rewards.DoubleRewardsEvent getDoubleRewards() {
        return doubleRewards;
    }

    public com.pitremake.event.EventSchedule getEventSchedule() {
        return eventSchedule;
    }

    /** /pit reload: 自動イベントの次の時刻を新しい config で決め直す。 */
    public void rescheduleEvents(org.bukkit.command.CommandSender sender) {
        for (com.pitremake.event.PitEvent event : eventSchedule.getEvents()) {
            event.scheduleNext();
        }
        warnOverlaps(sender);
    }

    /** 自動イベントの時間が重なる設定ならログ (と sender) に出す。 */
    public void warnOverlaps(org.bukkit.command.CommandSender sender) {
        java.util.List<String> overlaps = eventSchedule.overlaps(System.currentTimeMillis());
        for (String line : overlaps) {
            getLogger().warning("Events overlap: " + line + " (change offset-minutes in config.yml)");
            if (sender != null) {
                sender.sendMessage("§cEvents overlap: " + line);
            }
        }
    }

    public com.pitremake.koth.KothEvent getKothEvent() {
        return kothEvent;
    }

    public com.pitremake.util.EventBossBar getEventBossBar() {
        return eventBossBar;
    }

    /**
     * Auction / Spire / KOTH / 2X Rewards のどれかが (予告中も含めて) 開催中か。イベントは同時には始まらない。
     * 自動で始まる時間に別のイベントがあれば、終わってから始まる (1 分ごとに見直す)。
     */
    public boolean isEventActive() {
        return (auctionManager != null && auctionManager.isRunning())
                || (spireEvent != null && spireEvent.isActive())
                || (kothEvent != null && kothEvent.isRunning())
                || (doubleRewards != null && doubleRewards.isRunning());
    }

    public com.pitremake.util.PacketTap getPacketTap() {
        return packetTap;
    }

    public com.pitremake.auction.AuctionManager getAuctionManager() {
        return auctionManager;
    }

    public com.pitremake.trade.TradeManager getTradeManager() {
        return tradeManager;
    }

    public KillListener getKillListener() {
        return killListener;
    }

    /** config の damage.stacking (additive / multiplicative) を読む。 */
    public void loadDamageStacking() {
        DamageContext.Stacking stacking = DamageContext.Stacking.fromName(getConfig().getString("damage.stacking", "additive"));
        DamageContext.setStacking(stacking == null ? DamageContext.Stacking.ADDITIVE : stacking);
    }

    /** 今の Pit のマップ (config の map、今は Seasons)。 */
    public PitMapType getPitMap() {
        return pitMap;
    }

    /** スポーンしたときの持ち物: 初期装備 (Armory の矢込み)、パークのアイテム、Fancy Hat。持っているものはもらわない。 */
    public void giveSpawnKit(Player player) {
        SpawnKit.give(player, upgradeManager.hasSpawnItems(player.getUniqueId()), renownEffects.extraArrows(player));
        perkManager.giveSpawnItems(player);
        renownEffects.giveSpawnItems(player);
    }
}
