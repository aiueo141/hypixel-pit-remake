package com.pitremake.auction;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.pitremake.PitPlugin;
import com.pitremake.util.PlayerPackets;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityHeadRotation;
import net.minecraft.server.v1_8_R3.PacketPlayOutNamedEntitySpawn;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_8_R3.PacketPlayOutScoreboardTeam;
import net.minecraft.server.v1_8_R3.PlayerInteractManager;
import net.minecraft.server.v1_8_R3.Scoreboard;
import net.minecraft.server.v1_8_R3.ScoreboardTeam;
import net.minecraft.server.v1_8_R3.ScoreboardTeamBase;
import net.minecraft.server.v1_8_R3.WorldServer;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * オークションの間だけスポーンに立つオークショニア (録画 2026-10-15_11-11-03.mcrc)。
 * <pre>
 * 同じスキンのプレイヤーの NPC 5 体。開始の直後に出て、終わったら消える。名前はチーム (nametag never) で隠す
 * 位置 (-13.5, 114.5, -10.5) / (16.5, 114.5, -14.5) / (16.5, 114.5, 16.5) / (-11.5, 114.5, 15.5) / (12.5, 114.5, 0.5)
 * </pre>
 * パケットだけで見せる (ワールドには居ない)。右クリック・左クリックでオークションの画面を開く ({@link com.pitremake.util.PacketTap})。
 */
final class Auctioneers {

    /** x, y, z, 向き (録画)。 */
    private static final double[][] SPOTS = {
            {-13.5, 114.5, -10.5, -73.1}, {16.5, 114.5, -14.5, 73.1}, {16.5, 114.5, 16.5, 104.1},
            {-11.5, 114.5, 15.5, -153.3}, {12.5, 114.5, 0.5, 90.0}};
    private static final String NAME = "auctioneer";
    private static final String TEAM = "pit_auctioneer";
    /** 録画のオークショニアのスキン (署名付き)。 */
    private static final String SKIN_VALUE = "eyJ0aW1lc3RhbXAiOjE1NTMyMzkzODc3NDIsInByb2ZpbGVJZCI6IjQxZDNhYmMyZDc0OTQwMGM5MDkwZDU0MzRkMDM4MzFiIiwicHJvZmlsZU5hbWUiOiJNZWdha2xvb24iLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzkyOGM3YWI2NmY0M2Y2MzRhMGY3YTdhMzBhNGM2MDViYWI2NzE1OTUyMTUyNmQyODFkYTg3N2E4NjUxYzcxODQifX19";
    private static final String SKIN_SIGNATURE = "BHuWWauLAFhO7dTz0EKqEApv0RP8Wf/r1iU2LYXES5BGEKMKUAmR3oqr9W4zRN3AeNgUDxHAG3s03yAPCYT2RbnEAoe15v4BFP7JFEoSphH74l5HsHa498Yeec6Tq7vAplqWE0IWV9gHXxP7gvqpv/oWjNaSiFneA1cammgF2tdEnezF3roAPerhDk+WW+IFos6urqqqq/rZG42BD46SG6PAoxALoqEvessomxX3g/GVCcTHqp0/w775OKFoSW1R0BMN7g0duHejCnjMVOC7HBJ174tD1gQNPkJvnJAk5i0zwCIZTtZ95KQcpRZvMHYSTwRyb8sacPLhwAUsjHYidIjIuoZ0rR8nhpoUUM8t7UndeWrknPyiG7jkM+T2VY81nncvMFdM5nlDdAWiiYdAVnuS1pSknzUG4jlvGIiLsIqF5sDnlJ+UgxUzKzqVnnV9tpHk90DoRLxdH9dbFaLV+5nSaNHhoIXz5KArR677cjblGvhdN7XvyjOr/lX8o7d1Q+nLZGjD4C13FRC5HDO4gUZSm3ShT65P3oJV+kTY5VmZ/g7maZ2ba5g2t96qFwKF1f6E/00PA8355KOvW+7dpqao/TUPRTVhZtJswYMqtoJ3ayBay96CxAPP6R7Ml1ASGZDdQKq70XhFV0sz5FpZWsAISHBRYkNbH8zJwIJMr/I=";
    /** 少し待ってから tab から消す (すぐ消すとスキンが読まれない)。 */
    private static final int TAB_REMOVE_DELAY = 40;
    /** 体の外側のパーツを全部出す (録画: 126)。 */
    private static final byte SKIN_PARTS = 126;

    private final PitPlugin plugin;
    private final List<EntityPlayer> npcs = new ArrayList<>();

    Auctioneers(PitPlugin plugin) {
        this.plugin = plugin;
    }

    /** NPC を作る (まだ誰にも見せない)。クリックしたときの処理を登録する。 */
    void create(Consumer<Player> onClick) {
        remove();
        WorldServer world = ((CraftWorld) plugin.getServer().getWorlds().get(0)).getHandle();
        GameProfile profile = new GameProfile(UUID.randomUUID(), NAME);
        profile.getProperties().put("textures", new Property("textures", SKIN_VALUE, SKIN_SIGNATURE));
        for (double[] spot : SPOTS) {
            EntityPlayer npc = new EntityPlayer(MinecraftServer.getServer(), world, profile, new PlayerInteractManager(world));
            npc.setLocation(spot[0], spot[1], spot[2], (float) spot[3], 0);
            npc.getDataWatcher().watch(10, SKIN_PARTS);
            npcs.add(npc);
            plugin.getPacketTap().onEntityClick(npc.getId(), onClick);
        }
    }

    /** NPC を消して、クリックの登録も外す。 */
    void remove() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            hide(player);
        }
        for (EntityPlayer npc : npcs) {
            plugin.getPacketTap().removeEntityClick(npc.getId());
        }
        npcs.clear();
    }

    void showAll() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            show(player);
        }
    }

    void show(Player player) {
        if (npcs.isEmpty() || plugin.getBotManager().isBot(player) || !player.getWorld().equals(plugin.getServer().getWorlds().get(0))) {
            return;
        }
        Scoreboard scoreboard = new Scoreboard();
        ScoreboardTeam team = scoreboard.createTeam(TEAM);
        team.setNameTagVisibility(ScoreboardTeamBase.EnumNameTagVisibility.NEVER);
        scoreboard.addPlayerToTeam(NAME, TEAM);
        // 前の分が残っていても作り直せるよう、消してから作る
        PlayerPackets.send(player, new PacketPlayOutScoreboardTeam(team, 1));
        PlayerPackets.send(player, new PacketPlayOutScoreboardTeam(team, 0));
        PlayerPackets.send(player, new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, npcs));
        for (EntityPlayer npc : npcs) {
            PlayerPackets.send(player, new PacketPlayOutNamedEntitySpawn(npc));
            PlayerPackets.send(player, new PacketPlayOutEntityHeadRotation(npc, (byte) (npc.yaw * 256 / 360)));
        }
        List<EntityPlayer> shown = new ArrayList<>(npcs);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                PlayerPackets.send(player, new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, shown));
            }
        }, TAB_REMOVE_DELAY);
    }

    void hide(Player player) {
        if (npcs.isEmpty()) {
            return;
        }
        int[] ids = new int[npcs.size()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = npcs.get(i).getId();
        }
        PlayerPackets.send(player, new PacketPlayOutEntityDestroy(ids));
        PlayerPackets.send(player, new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, npcs));
    }
}
