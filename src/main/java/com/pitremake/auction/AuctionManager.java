package com.pitremake.auction;

import com.pitremake.PitPlugin;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * オークション (Minor Event)。録画 2026-10-15_11-11-03.mcrc と pit.wiki/Auction。
 * <pre>
 * 開始 : §d§lMINOR EVENT! §6§lAUCTION! … §6§lOFFER: 品 at 最低額 [CLICK] (クリックで /auctionmenu) と note.pling の和音 5 回、
 *        スポーンにオークショニア 5 体。入札できるのは始まったときにいたプレイヤーだけ
 * 入札 : 次の最低額は最高額 + 10%。ゴールドはその場で払い (前に入れた分との差額)、負けたら終わりに返す。
 *        残り 20 秒未満なら +10 秒。§6§lAUCTION! 名前 §7has bid §6金額g§7! [§a+10s] [CLICK]
 * 終了 : 60 秒 (+延長) で §6§lIT'S OVER! 名前 §7bought 品 §7for §6金額g§7! (ホバーで品)。画面を閉じ、オークショニアを消す
 * </pre>
 * 次のオークションまでの間隔は config の auction.interval-minutes (録画には無い)。
 * 落札した人がいなくなっていたら、いる人の中で一番高く入れた人が落札する。いない人には返金しない (pit.wiki)。
 * 落札した人へのチャットは録画に無いので推測。
 */
public final class AuctionManager implements Listener, com.pitremake.event.PitEvent {

    /**
     * 開始の音: note.pling (0.78) の 4 音の和音を 5 回 (録画のピッチ)。録画の間隔は 5 tick だが、
     * 速すぎると感じたので config の auction.start-sound-interval-ticks (初期値 7) で変えられる。
     */
    private static final int[][] START_CHORDS = {{50, 63, 74, 100}, {50, 63, 74, 100}, {44, 56, 66, 89}, {59, 74, 89, 118},
            {50, 63, 74, 100}};
    private static final int CHORD_INTERVAL = 7;
    private static final float START_VOLUME = 0.78f;
    private static final String DENY_SOUND = "mob.villager.no";
    /** 入札できたときの音 (録画には自分の入札が無いので推測)。 */
    private static final String BID_SOUND = "note.pling";
    private static final float BID_PITCH = 2.0f;

    static final class Bid {
        final UUID player;
        final String name;
        final long amount;
        final long time;

        Bid(UUID player, String name, long amount, long time) {
            this.player = player;
            this.name = name;
            this.amount = amount;
            this.time = time;
        }
    }

    static final class Auction {
        final AuctionOffer offer;
        final Set<UUID> participants;
        final List<Bid> bids = new ArrayList<>();
        /** プレイヤーごとの払ったゴールド (一番高い入札額)。 */
        final Map<UUID, Long> paid = new HashMap<>();
        /** Custom bid の金額 (無ければ New bid)。 */
        final Map<UUID, Long> customBids = new HashMap<>();
        long endTime;

        Auction(AuctionOffer offer, Set<UUID> participants, long endTime) {
            this.offer = offer;
            this.participants = participants;
            this.endTime = endTime;
        }

        Bid top() {
            return bids.isEmpty() ? null : bids.get(bids.size() - 1);
        }

        long secondsLeft() {
            return Math.max(0, (endTime - System.currentTimeMillis()) / 1000);
        }
    }

    private final PitPlugin plugin;
    private final AuctionOffers offers;
    private final Auctioneers auctioneers;
    private final Set<AuctionMenu> menus = new HashSet<>();
    private Auction auction;
    private long nextAuction;
    private BukkitTask task;

    public AuctionManager(PitPlugin plugin) {
        this.plugin = plugin;
        this.offers = new AuctionOffers(plugin);
        this.auctioneers = new Auctioneers(plugin);
    }

    public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        scheduleNext();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20, 20);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        if (auction != null) {
            // 止めるときは誰も落札せず全員に返す
            refundAll(null);
            closeMenus();
            auctioneers.remove();
            auction = null;
        }
    }

    Auction getAuction() {
        return auction;
    }

    public boolean isRunning() {
        return auction != null;
    }

    /** 次のオークションを時計に合わせて決める (config の auction.interval-minutes / offset-minutes)。 */
    @Override
    public void scheduleNext() {
        nextAuction = plugin.getEventSchedule().next("auction", 15, System.currentTimeMillis());
    }

    @Override
    public String configKey() {
        return "auction";
    }

    @Override
    public String displayName() {
        return "§6§lAUCTION";
    }

    @Override
    public boolean isScheduled() {
        return plugin.getConfig().getBoolean("auction.enabled", false);
    }

    @Override
    public int lengthMinutes() {
        // 60 秒 + 入札での延長
        return 3;
    }

    @Override
    public long nextStart() {
        return nextAuction;
    }

    @Override
    public long endsAt() {
        return auction == null ? 0 : auction.endTime;
    }

    private void tick() {
        if (auction == null) {
            if (plugin.getConfig().getBoolean("auction.enabled", false) && System.currentTimeMillis() >= nextAuction) {
                if (plugin.isEventActive()) {
                    // 別のイベント (Spire / KOTH) の最中は終わるまで待つ
                    nextAuction = System.currentTimeMillis() + 60_000;
                } else if (realPlayers() >= plugin.getConfig().getInt("auction.min-players", 1)) {
                    begin();
                } else {
                    scheduleNext();
                }
            }
            return;
        }
        if (System.currentTimeMillis() >= auction.endTime) {
            finish();
            return;
        }
        refreshMenus();
    }

    private int realPlayers() {
        int count = 0;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!plugin.getBotManager().isBot(player)) {
                count++;
            }
        }
        return count;
    }

    /** /pit auction start: すぐに始める。始まっていれば false。 */
    public boolean begin() {
        if (auction != null || plugin.isEventActive()) {
            return false;
        }
        Set<UUID> participants = new HashSet<>();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            participants.add(player.getUniqueId());
        }
        AuctionOffer offer = offers.random();
        auction = new Auction(offer, participants, System.currentTimeMillis() + AuctionRules.DURATION_SECONDS * 1000L);
        broadcast("§d§lMINOR EVENT! §6§lAUCTION! §7Check auctioneer or chat!\n§6§lOFFER: " + offer.getName() + " §7at §6§6"
                + AuctionRules.gold(offer.getMinimumBid()) + " §7[§e§lCLICK§7]", true, null);
        int interval = Math.max(1, plugin.getConfig().getInt("auction.start-sound-interval-ticks", CHORD_INTERVAL));
        for (int i = 0; i < START_CHORDS.length; i++) {
            int[] chord = START_CHORDS[i];
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    for (int pitch : chord) {
                        player.playSound(player.getLocation(), "note.pling", START_VOLUME, pitch / 63f);
                    }
                }
            }, (long) i * interval);
        }
        auctioneers.create(this::open);
        auctioneers.showAll();
        return true;
    }

    /** /pit auction end: すぐに終わらせる。 */
    public boolean end() {
        if (auction == null) {
            return false;
        }
        finish();
        return true;
    }

    /** /auctionmenu とオークショニアのクリック。 */
    public void open(Player player) {
        if (auction == null) {
            player.sendMessage("§cThere is no auction going on right now!");
            return;
        }
        AuctionMenu menu = new AuctionMenu(this, player);
        menus.add(menu);
        menu.open();
    }

    void closed(AuctionMenu menu) {
        menus.remove(menu);
    }

    private void refreshMenus() {
        for (AuctionMenu menu : new ArrayList<>(menus)) {
            if (menu.getPlayer().isOnline()) {
                menu.render();
            } else {
                menus.remove(menu);
            }
        }
    }

    private void closeMenus() {
        for (AuctionMenu menu : new ArrayList<>(menus)) {
            if (menu.getPlayer().isOnline() && menu.getPlayer().getOpenInventory().getTopInventory().getHolder() == menu) {
                menu.getPlayer().closeInventory();
            }
        }
        menus.clear();
    }

    /** そのプレイヤーが今入れる金額 (Custom bid か、最低額)。 */
    long bidAmount(Player player) {
        Long custom = auction.customBids.get(player.getUniqueId());
        if (custom != null) {
            return custom;
        }
        Bid top = auction.top();
        return top == null ? auction.offer.getMinimumBid() : AuctionRules.nextBid(top.amount);
    }

    /** Participate の最後の行。 */
    String status(Player player, long amount) {
        if (isTopBidder(player)) {
            return "§aYou are the top bidder!";
        }
        if (tooSmall(amount)) {
            return "§cBid too small!";
        }
        if (!canAfford(player, amount)) {
            return "§cNot enough gold!";
        }
        return "§eClick to bid!";
    }

    /** 今の最高額を入れているのが自分か (自分の入札を自分で上げることはできない)。 */
    private boolean isTopBidder(Player player) {
        Bid top = auction.top();
        return top != null && top.player.equals(player.getUniqueId());
    }

    private boolean tooSmall(long amount) {
        Bid top = auction.top();
        return top == null ? amount < auction.offer.getMinimumBid() : amount <= top.amount;
    }

    private long paid(Player player) {
        Long paid = auction.paid.get(player.getUniqueId());
        return paid == null ? 0 : paid;
    }

    private boolean canAfford(Player player, long amount) {
        return plugin.getGoldManager().get(player.getUniqueId()) + paid(player) >= amount;
    }

    /** 左クリック: 今の金額で入札する。 */
    void bid(Player player) {
        if (auction == null) {
            return;
        }
        UUID id = player.getUniqueId();
        if (!auction.participants.contains(id)) {
            player.sendMessage("§c§lNOPE! §7You weren't in the lobby when this auction started!");
            player.playSound(player.getLocation(), DENY_SOUND, 1.0f, 1.0f);
            return;
        }
        if (isTopBidder(player)) {
            player.sendMessage("§c§lOOPS! §7You already have the top bid!");
            player.playSound(player.getLocation(), DENY_SOUND, 1.0f, 1.0f);
            return;
        }
        long amount = bidAmount(player);
        if (tooSmall(amount)) {
            player.sendMessage("§c§lOOPS! §7You have to bid more than the current top bid!");
            player.playSound(player.getLocation(), DENY_SOUND, 1.0f, 1.0f);
            return;
        }
        if (!canAfford(player, amount)) {
            player.sendMessage("§c§lCANNOT AFFORD! §7You don't have enough gold to bid on this!");
            player.playSound(player.getLocation(), DENY_SOUND, 1.0f, 1.0f);
            return;
        }
        // 前に入れた分との差額だけ払う
        plugin.getGoldManager().take(id, amount - paid(player));
        auction.paid.put(id, amount);
        auction.customBids.remove(id);
        String name = plugin.getStatsManager().chatTag(id) + " " + plugin.getRankManager().nameColor(id) + player.getName();
        auction.bids.add(new Bid(id, name, amount, System.currentTimeMillis()));
        boolean extended = auction.secondsLeft() < AuctionRules.EXTEND_BELOW_SECONDS;
        if (extended) {
            auction.endTime += AuctionRules.EXTEND_SECONDS * 1000L;
        }
        broadcast("§6§lAUCTION! " + name + " §7has bid §6" + AuctionRules.gold(amount) + "§7! "
                + (extended ? "§a+" + AuctionRules.EXTEND_SECONDS + "s " : "") + "§7[§e§lCLICK§7]", true, null);
        player.playSound(player.getLocation(), BID_SOUND, 1.0f, BID_PITCH);
        if (plugin.getSidebar() != null) {
            plugin.getSidebar().update(player);
        }
        refreshMenus();
    }

    /** 右クリック: New bid なら看板で金額を入れる、Custom bid なら最低額に戻す。入札が無いうち・自分が最高額のときは何もしない。 */
    void toggleCustomBid(Player player) {
        if (auction == null || auction.top() == null || isTopBidder(player)) {
            return;
        }
        UUID id = player.getUniqueId();
        if (auction.customBids.remove(id) != null) {
            refreshMenus();
            return;
        }
        player.closeInventory();
        long top = auction.top().amount;
        plugin.getPacketTap().openSign(player, new String[]{"", "^^^^^^", "Enter bid", "Top: " + top}, lines -> {
            if (auction == null || !player.isOnline()) {
                return;
            }
            auction.customBids.put(id, AuctionRules.parseBid(lines.length > 0 ? lines[0] : ""));
            open(player);
        });
    }

    private void finish() {
        Auction ended = auction;
        closeMenus();
        auctioneers.remove();
        auction = null;
        scheduleNext();

        Bid winner = null;
        for (int i = ended.bids.size() - 1; i >= 0; i--) {
            Bid bid = ended.bids.get(i);
            Player player = plugin.getServer().getPlayer(bid.player);
            Long paid = ended.paid.get(bid.player);
            // その人の一番高い入札だけを見る (同じ人の前の入札は飛ばす)
            if (player != null && player.isOnline() && paid != null && paid == bid.amount) {
                winner = bid;
                break;
            }
        }
        if (winner == null) {
            refundAll(ended);
            broadcast("§6§lIT'S OVER! §7Nobody bought " + ended.offer.getName() + "§7!", false, ended.offer);
            return;
        }
        Player player = plugin.getServer().getPlayer(winner.player);
        ended.paid.remove(winner.player);
        refundAll(ended);
        ended.offer.deliver(player);
        broadcast("§6§lIT'S OVER! " + winner.name + " §7bought " + ended.offer.getName() + " §7for §6"
                + AuctionRules.gold(winner.amount) + "§7!", false, ended.offer);
        player.sendMessage("§6§lAUCTION! §7You won " + ended.offer.getName() + "§7!");
        player.playSound(player.getLocation(), "random.levelup", 1.0f, 1.0f);
    }

    /** 払ったゴールドを返す (いない人には返さない)。auction が null なら今のオークション。 */
    private void refundAll(Auction target) {
        Auction from = target == null ? auction : target;
        for (Map.Entry<UUID, Long> entry : from.paid.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                plugin.getGoldManager().add(entry.getKey(), entry.getValue());
            }
        }
        from.paid.clear();
    }

    /** clickable: クリックで /auctionmenu。hover: 品 (IT'S OVER)。 */
    private void broadcast(String text, boolean clickable, AuctionOffer hover) {
        TextComponent message = new TextComponent(TextComponent.fromLegacyText(text));
        if (clickable) {
            message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/auctionmenu"));
            message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder("Click to inspect the auction!").color(ChatColor.YELLOW).create()));
        } else if (hover != null) {
            StringBuilder lines = new StringBuilder("§eAuction Offer");
            for (String line : hover.lore()) {
                lines.append('\n').append(line);
            }
            BaseComponent[] tooltip = TextComponent.fromLegacyText(lines.toString());
            message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip));
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.spigot().sendMessage(message);
        }
        plugin.getServer().getConsoleSender().sendMessage(text);
    }

    /** 途中で参加した人にもオークショニアを見せる (入札はできない)。 */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (auction != null) {
            Player player = event.getPlayer();
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> auctioneers.show(player), 20);
        }
    }

    /** リスポーンするとクライアントのエンティティが消えるので見せ直す。 */
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (auction != null) {
            Player player = event.getPlayer();
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> auctioneers.show(player), 5);
        }
    }
}
