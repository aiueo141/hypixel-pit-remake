package com.pitremake.util;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Blocks;
import net.minecraft.server.v1_8_R3.ChatComponentText;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayInUpdateSign;
import net.minecraft.server.v1_8_R3.PacketPlayInUseEntity;
import net.minecraft.server.v1_8_R3.PacketPlayOutBlockChange;
import net.minecraft.server.v1_8_R3.PacketPlayOutOpenSignEditor;
import net.minecraft.server.v1_8_R3.PacketPlayOutUpdateSign;
import net.minecraft.server.v1_8_R3.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * プレイヤーの通信に割り込んで、サーバーが普通には受け取らない操作を拾う。
 * <pre>
 * 看板の入力 : 本物の看板の無い場所で看板の編集画面を開き、書かれた 4 行を受け取る (オークションの入札額。録画と同じく
 *             1 行目が入力欄、2〜4 行目が説明)
 * NPC のクリック: パケットだけで出しているエンティティ (オークショニア) の右クリック・左クリック
 * </pre>
 * 拾った操作はメインスレッドで処理する。
 */
public final class PacketTap implements Listener {

    private static final String HANDLER = "pit_packet_tap";
    private static final Field USE_ENTITY_ID;

    static {
        Field field = null;
        try {
            field = PacketPlayInUseEntity.class.getDeclaredField("a");
            field.setAccessible(true);
        } catch (NoSuchFieldException e) {
            // 見つからなければ NPC のクリックは拾えない
        }
        USE_ENTITY_ID = field;
    }

    private final JavaPlugin plugin;
    private final Map<UUID, Consumer<String[]>> signInputs = new ConcurrentHashMap<>();
    private final Map<UUID, BlockPosition> signPositions = new ConcurrentHashMap<>();
    private final Map<Integer, Consumer<Player>> entityClicks = new ConcurrentHashMap<>();

    public PacketTap(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            inject(player);
        }
    }

    public void stop() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Channel channel = channel(player);
            if (channel != null && channel.pipeline().get(HANDLER) != null) {
                channel.eventLoop().execute(() -> {
                    if (channel.pipeline().get(HANDLER) != null) {
                        channel.pipeline().remove(HANDLER);
                    }
                });
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        inject(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        signInputs.remove(event.getPlayer().getUniqueId());
        signPositions.remove(event.getPlayer().getUniqueId());
    }

    private static Channel channel(Player player) {
        try {
            return ((CraftPlayer) player).getHandle().playerConnection.networkManager.channel;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void inject(Player player) {
        Channel channel = channel(player);
        if (channel == null || channel.pipeline().get(HANDLER) != null || channel.pipeline().get("packet_handler") == null) {
            return;
        }
        UUID id = player.getUniqueId();
        try {
            channel.pipeline().addBefore("packet_handler", HANDLER, new ChannelDuplexHandler() {
                @Override
                public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
                    if (handle(id, message)) {
                        return;
                    }
                    super.channelRead(context, message);
                }
            });
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.WARNING, "Could not listen to packets of " + player.getName(), e);
        }
    }

    /** 拾ったら true (サーバーには渡さない)。通信のスレッドで呼ばれる。 */
    private boolean handle(UUID id, Object message) throws IllegalAccessException {
        if (message instanceof PacketPlayInUpdateSign) {
            Consumer<String[]> callback = signInputs.remove(id);
            BlockPosition position = signPositions.remove(id);
            if (callback == null) {
                return false;
            }
            IChatBaseComponent[] components = ((PacketPlayInUpdateSign) message).b();
            String[] lines = new String[components.length];
            for (int i = 0; i < components.length; i++) {
                lines[i] = components[i] == null ? "" : components[i].c();
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                Player player = plugin.getServer().getPlayer(id);
                if (player != null) {
                    restore(player, position);
                    callback.accept(lines);
                }
            });
            return true;
        }
        if (message instanceof PacketPlayInUseEntity && USE_ENTITY_ID != null) {
            PacketPlayInUseEntity use = (PacketPlayInUseEntity) message;
            Consumer<Player> callback = entityClicks.get((int) USE_ENTITY_ID.get(use));
            if (callback == null) {
                return false;
            }
            // 右クリックは INTERACT_AT と INTERACT の 2 つが届くので INTERACT_AT は無視する
            if (use.a() != PacketPlayInUseEntity.EnumEntityUseAction.INTERACT_AT) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Player player = plugin.getServer().getPlayer(id);
                    if (player != null) {
                        callback.accept(player);
                    }
                });
            }
            return true;
        }
        return false;
    }

    /** パケットだけのエンティティがクリックされたときの処理。 */
    public void onEntityClick(int entityId, Consumer<Player> callback) {
        entityClicks.put(entityId, callback);
    }

    public void removeEntityClick(int entityId) {
        entityClicks.remove(entityId);
    }

    /**
     * 看板の編集画面を開き、書き終わったら 4 行を返す。足元の y=0 に見えない看板を置いたことにして開く
     * (終わったら元のブロックに戻して見せる)。
     */
    public void openSign(Player player, String[] lines, Consumer<String[]> callback) {
        inject(player);
        World world = ((CraftWorld) player.getWorld()).getHandle();
        BlockPosition position = new BlockPosition(player.getLocation().getBlockX(), 0, player.getLocation().getBlockZ());
        IChatBaseComponent[] components = new IChatBaseComponent[4];
        for (int i = 0; i < components.length; i++) {
            components[i] = new ChatComponentText(i < lines.length ? lines[i] : "");
        }
        PacketPlayOutBlockChange sign = new PacketPlayOutBlockChange(world, position);
        sign.block = Blocks.STANDING_SIGN.getBlockData();
        signInputs.put(player.getUniqueId(), callback);
        signPositions.put(player.getUniqueId(), position);
        PlayerPackets.send(player, sign);
        PlayerPackets.send(player, new PacketPlayOutUpdateSign(world, position, components));
        PlayerPackets.send(player, new PacketPlayOutOpenSignEditor(position));
    }

    private static void restore(Player player, BlockPosition position) {
        if (position != null) {
            PlayerPackets.send(player, new PacketPlayOutBlockChange(((CraftWorld) player.getWorld()).getHandle(), position));
        }
    }
}
