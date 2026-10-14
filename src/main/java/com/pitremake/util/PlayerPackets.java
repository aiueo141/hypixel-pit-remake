package com.pitremake.util;

import net.minecraft.server.v1_8_R3.ChatComponentText;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerListHeaderFooter;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;

/** 1.8 の API に無いアクションバー・タイトル・1 人だけに見えるパーティクルを NMS のパケットで送る。 */
public final class PlayerPackets {

    private PlayerPackets() {
    }

    public static void actionBar(Player player, String text) {
        send(player, new PacketPlayOutChat(new ChatComponentText(text), (byte) 2));
    }

    /** フェードイン・表示・フェードアウトは tick 単位。 */
    public static void title(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        send(player, new PacketPlayOutTitle(fadeIn, stay, fadeOut));
        send(player, new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TITLE, new ChatComponentText(title)));
        send(player, new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.SUBTITLE, new ChatComponentText(subtitle)));
    }

    /** そのプレイヤーにだけ見えるパーティクル。spread は各方向に散らばる幅 (ブロック)。 */
    public static void particle(Player player, EnumParticle particle, Location location, float spread, int count) {
        send(player, new PacketPlayOutWorldParticles(particle, true,
                (float) location.getX(), (float) location.getY(), (float) location.getZ(), spread, spread, spread, 0, count));
    }

    /** tab の上と下の文字。 */
    public static void tabHeaderFooter(Player player, String header, String footer) {
        PacketPlayOutPlayerListHeaderFooter packet = new PacketPlayOutPlayerListHeaderFooter(new ChatComponentText(header));
        try {
            // 下の文字を設定するコンストラクタやメソッドが無いので、フィールドに直接入れる
            Field footerField = PacketPlayOutPlayerListHeaderFooter.class.getDeclaredField("b");
            footerField.setAccessible(true);
            footerField.set(packet, new ChatComponentText(footer));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot set the tab footer", e);
        }
        send(player, packet);
    }

    public static void send(Player player, Packet<?> packet) {
        EntityPlayer handle = ((CraftPlayer) player).getHandle();
        if (handle.playerConnection != null) {
            handle.playerConnection.sendPacket(packet);
        }
    }
}
