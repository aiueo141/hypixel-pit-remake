package com.pitremake.spire;

import com.pitremake.util.PlayerPackets;
import io.netty.buffer.Unpooled;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NBTTagList;
import net.minecraft.server.v1_8_R3.NBTTagString;
import net.minecraft.server.v1_8_R3.PacketDataSerializer;
import net.minecraft.server.v1_8_R3.PacketPlayOutCustomPayload;
import net.minecraft.server.v1_8_R3.PacketPlayOutSetSlot;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Spire のルールの本 (録画: 予告の [INFO] をクリックすると開く)。
 * 手に持っているスロットを (パケットだけで) 一瞬本にして MC|BOpen を送り、すぐ元に戻す。
 */
final class EventBook {

    /** 1 ページ (JSON)。FLOOR BONUS / SOULS BONUS はホバーで中身。 */
    private static final String PAGE = "{\"text\":\"\",\"extra\":["
            + "{\"text\":\"SPIRE\\n\",\"color\":\"dark_purple\",\"bold\":true},"
            + "{\"text\":\"Major Event\\n\\n\",\"color\":\"gray\"},"
            + "{\"text\":\"Rules\\n\",\"color\":\"black\",\"bold\":true},"
            + "{\"text\":\"◼ \",\"color\":\"dark_gray\"},{\"text\":\"Kill = Go up\\n\",\"color\":\"black\"},"
            + "{\"text\":\"◼ \",\"color\":\"dark_gray\"},{\"text\":\"Die = Go down\\n\",\"color\":\"black\"},"
            + "{\"text\":\"◼ \",\"color\":\"dark_gray\"},{\"text\":\"Rip souls from kills\\n\\n\",\"color\":\"black\"},"
            + "{\"text\":\"FLOOR BONUS\\n\",\"color\":\"gold\",\"bold\":true,\"hoverEvent\":{\"action\":\"show_text\",\"value\":"
            + "\"§7Reach floor §9§lSIX§7:\\n§e+1 Renown\\n§b+1,500XP\\n§6+1,500g\"}},"
            + "{\"text\":\"SOULS BONUS\",\"color\":\"aqua\",\"bold\":true,\"hoverEvent\":{\"action\":\"show_text\",\"value\":"
            + "\"§7Top 3: §6+2,000g §e+2 Renown\\n§7Top 20: §6+500g §e+1 Renown\\n§7Others: §6+100g\"}}"
            + "]}";

    private EventBook() {
    }

    static void open(Player player) {
        net.minecraft.server.v1_8_R3.ItemStack book = CraftItemStack.asNMSCopy(new ItemStack(Material.WRITTEN_BOOK));
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("title", "Spire");
        tag.setString("author", "Pit");
        tag.setBoolean("resolved", true);
        NBTTagList pages = new NBTTagList();
        pages.add(new NBTTagString(PAGE));
        tag.set("pages", pages);
        book.setTag(tag);
        int slot = player.getInventory().getHeldItemSlot();
        // インベントリは変えずにパケットだけ (ウィンドウ 0 のホットバーは 36〜44)
        PlayerPackets.send(player, new PacketPlayOutSetSlot(0, 36 + slot, book));
        PlayerPackets.send(player, new PacketPlayOutCustomPayload("MC|BOpen", new PacketDataSerializer(Unpooled.buffer())));
        PlayerPackets.send(player, new PacketPlayOutSetSlot(0, 36 + slot, CraftItemStack.asNMSCopy(player.getInventory().getItem(slot))));
    }
}
