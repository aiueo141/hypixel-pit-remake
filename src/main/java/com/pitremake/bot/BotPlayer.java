package com.pitremake.bot;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.EnumProtocolDirection;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import net.minecraft.server.v1_8_R3.NetworkManager;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PlayerConnection;
import net.minecraft.server.v1_8_R3.PlayerInteractManager;
import net.minecraft.server.v1_8_R3.WorldServer;
import net.minecraft.server.v1_8_R3.WorldSettings;

/**
 * サーバー上だけに存在する偽プレイヤー。通信先が無いので送信パケットは捨てる。
 * 移動は入力 (前後・左右・ジャンプ・ダッシュ) を設定して {@link #l()} を呼ぶと、バニラのプレイヤーと同じ物理で動く。
 */
final class BotPlayer extends EntityPlayer {

    BotPlayer(MinecraftServer server, WorldServer world, GameProfile profile) {
        super(server, world, profile, new PlayerInteractManager(world));
        // PlayerConnection のコンストラクタが this.playerConnection に自分を設定する
        new SilentConnection(server, new NetworkManager(EnumProtocolDirection.CLIENTBOUND), this);
        playerInteractManager.setGameMode(WorldSettings.EnumGamemode.SURVIVAL);
    }

    /** 1 tick 分の入力。forward / strafe は -1〜1。 */
    void setInput(float forward, float strafe, boolean jump, boolean sprint) {
        this.ba = forward;
        this.aZ = strafe;
        this.aY = jump;
        setSprinting(sprint);
    }

    void look(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
        f(yaw);
    }

    /** 送信先の無い接続。パケットはすべて捨てる。 */
    private static final class SilentConnection extends PlayerConnection {

        SilentConnection(MinecraftServer server, NetworkManager networkManager, EntityPlayer player) {
            super(server, networkManager, player);
        }

        @Override
        public void sendPacket(Packet packet) {
        }
    }
}
