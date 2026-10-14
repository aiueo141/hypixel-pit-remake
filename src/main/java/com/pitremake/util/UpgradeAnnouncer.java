package com.pitremake.util;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 強化でレアなアイテムができたことを全員に知らせる。
 * <pre>
 * RARE! 9062 created Tier III Sword, gg!     (新しく RARE エンチャントが付いた)
 * WOAH! 9062 created Legendary Tier III Sword, gg!   (新しく称号が付いた)
 * </pre>
 * アイテム名にカーソルを合わせるとアイテムの中身が見える。作った本人にはエンダードラゴンの声が鳴る。
 */
public final class UpgradeAnnouncer {

    public static final String RARE = "RARE!";
    public static final String WOAH = "WOAH!";
    private static final String DRAGON_SOUND = "mob.enderdragon.growl";

    private UpgradeAnnouncer() {
    }

    public static void announce(String tag, Player creator, ItemStack item) {
        List<BaseComponent> message = new ArrayList<>();

        TextComponent tagComponent = new TextComponent(tag);
        tagComponent.setColor(ChatColor.LIGHT_PURPLE);
        tagComponent.setBold(true);
        message.add(tagComponent);

        message.add(gray(" " + creator.getName() + " created "));

        String name = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                ? item.getItemMeta().getDisplayName() : item.getType().name();
        HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_ITEM,
                new BaseComponent[]{new TextComponent(NbtUtil.toChatNbt(item))});
        for (BaseComponent part : TextComponent.fromLegacyText(name)) {
            part.setBold(false);
            part.setHoverEvent(hover);
            message.add(part);
        }

        message.add(gray(", gg!"));

        BaseComponent[] components = message.toArray(new BaseComponent[0]);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.spigot().sendMessage(components);
        }
        Bukkit.getConsoleSender().sendMessage(TextComponent.toLegacyText(components));
        // 作った本人にはドラゴンの声
        creator.playSound(creator.getLocation(), DRAGON_SOUND, 1.0f, 1.0f);
    }

    private static TextComponent gray(String text) {
        TextComponent component = new TextComponent(text);
        component.setColor(ChatColor.GRAY);
        component.setBold(false);
        return component;
    }
}
