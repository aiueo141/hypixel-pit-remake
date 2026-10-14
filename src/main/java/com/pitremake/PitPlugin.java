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
import com.pitremake.command.FreshCommand;
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
    private PerkManager perkManager;
    private RenownEffects renownEffects;
    private Launcher launcher;
    private KillListener killListener;
    private PitMapType pitMap = PitMapType.SEASONS;

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
        registerCommands();
        registerListeners();
        sidebar = new PitSidebar(this);
        sidebar.start();

        getLogger().info("Pit v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
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
