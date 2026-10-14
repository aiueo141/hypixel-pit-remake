package com.pitremake.util;

import net.minecraft.server.v1_8_R3.NBTCompressedStreamTools;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NBTTagList;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * Hypixel と同じく tag.ExtraAttributes 配下にカスタムデータを保存する。
 */
public final class NbtUtil {

    private static final String EXTRA_ATTRIBUTES = "ExtraAttributes";

    private NbtUtil() {
    }

    public static ItemStack setInt(ItemStack item, String key, int value) {
        NBTTagCompound extra = readExtraAttributes(item);
        extra.setInt(key, value);
        return writeExtraAttributes(item, extra);
    }

    /** ExtraAttributes のコピーを返す。無ければ空の compound。 */
    public static NBTTagCompound readExtraAttributes(ItemStack item) {
        NBTTagCompound extra = getExtraAttributes(item);
        return extra == null ? new NBTTagCompound() : extra;
    }

    /** ExtraAttributes を丸ごと置き換えたコピーを返す。 */
    public static ItemStack writeExtraAttributes(ItemStack item, NBTTagCompound extra) {
        net.minecraft.server.v1_8_R3.ItemStack nms = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nms.hasTag() ? nms.getTag() : new NBTTagCompound();
        tag.set(EXTRA_ATTRIBUTES, extra);
        nms.setTag(tag);
        return CraftItemStack.asBukkitCopy(nms);
    }

    /**
     * AttributeModifiers を設定する (加算, メインハンド)。既定の修飾子は置き換えられる。
     * 例: generic.attackDamage
     */
    public static ItemStack setAttribute(ItemStack item, String attributeName, double amount) {
        net.minecraft.server.v1_8_R3.ItemStack nms = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nms.hasTag() ? nms.getTag() : new NBTTagCompound();

        UUID uuid = UUID.nameUUIDFromBytes(attributeName.getBytes(StandardCharsets.UTF_8));
        NBTTagCompound modifier = new NBTTagCompound();
        modifier.setString("AttributeName", attributeName);
        modifier.setString("Name", attributeName);
        modifier.setDouble("Amount", amount);
        modifier.setInt("Operation", 0);
        modifier.setLong("UUIDMost", uuid.getMostSignificantBits());
        modifier.setLong("UUIDLeast", uuid.getLeastSignificantBits());

        NBTTagList modifiers = new NBTTagList();
        modifiers.add(modifier);
        tag.set("AttributeModifiers", modifiers);
        nms.setTag(tag);
        return CraftItemStack.asBukkitCopy(nms);
    }

    /** チャットのホバー (show_item) 用の NBT 文字列。例: {id:"minecraft:golden_sword",Count:1b,tag:{...}} */
    public static String toChatNbt(ItemStack item) {
        return CraftItemStack.asNMSCopy(item).save(new NBTTagCompound()).toString();
    }

    /** アイテムを NBT ごと Base64 文字列にする (ファイル保存用, 情報が欠けない)。 */
    public static String toBase64(ItemStack item) throws IOException {
        NBTTagCompound compound = CraftItemStack.asNMSCopy(item).save(new NBTTagCompound());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        NBTCompressedStreamTools.a(compound, out);
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    /** {@link #toBase64} の逆変換。 */
    public static ItemStack fromBase64(String data) throws IOException {
        NBTTagCompound compound = NBTCompressedStreamTools.a(new ByteArrayInputStream(Base64.getDecoder().decode(data)));
        return CraftItemStack.asBukkitCopy(net.minecraft.server.v1_8_R3.ItemStack.createStack(compound));
    }

    public static boolean has(ItemStack item, String key) {
        NBTTagCompound extra = getExtraAttributes(item);
        return extra != null && extra.hasKey(key);
    }

    /** キーが無い場合は 0 を返す。 */
    public static int getInt(ItemStack item, String key) {
        NBTTagCompound extra = getExtraAttributes(item);
        return extra == null ? 0 : extra.getInt(key);
    }

    private static NBTTagCompound getExtraAttributes(ItemStack item) {
        if (item == null) {
            return null;
        }
        net.minecraft.server.v1_8_R3.ItemStack nms = CraftItemStack.asNMSCopy(item);
        if (nms == null || !nms.hasTag() || !nms.getTag().hasKey(EXTRA_ATTRIBUTES)) {
            return null;
        }
        return nms.getTag().getCompound(EXTRA_ATTRIBUTES);
    }
}
