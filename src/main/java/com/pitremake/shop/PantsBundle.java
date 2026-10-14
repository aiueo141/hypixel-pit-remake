package com.pitremake.shop;

import com.pitremake.item.MysticData;
import com.pitremake.item.MysticItems;
import com.pitremake.item.PantsColor;
import com.pitremake.util.NbtUtil;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NBTTagList;
import net.minecraft.server.v1_8_R3.NBTTagString;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pants Bundle (pit.wiki の Item Shop: 持って右クリックで Fresh Pants を最大 10 本しまう。Kept on death)。
 * <pre>
 * 空のとき       : 持ち物の Fresh Pants (5 色・Rage・Dark・Sewer・Aqua) をしまう
 * 入っているとき : 全部取り出す (入りきらなければ残してチャット)
 * </pre>
 * 見た目は空ならトロッコ、入っていればチェスト付きトロッコ。中身はアイテムの NBT (ExtraAttributes.PantsBundle) に Base64 でそのまま入れる。
 */
public final class PantsBundle {

    static final String KEY = "PantsBundle";
    static final int CAPACITY = 10;

    private PantsBundle() {
    }

    public static boolean is(ItemStack item) {
        return ShopItem.of(item) == ShopItem.PANTS_BUNDLE;
    }

    /** 空の Pants Bundle。 */
    static ItemStack create() {
        return write(new ItemStack(Material.MINECART), new ArrayList<ItemStack>());
    }

    /** 右クリックしたとき: 空ならしまい、入っていれば取り出す。手に持つアイテムを入れ替える。 */
    public static void use(Player player) {
        PlayerInventory inventory = player.getInventory();
        int slot = inventory.getHeldItemSlot();
        ItemStack bundle = inventory.getItem(slot);
        List<ItemStack> contents = read(bundle);
        if (contents.isEmpty()) {
            for (int i = 0; i < inventory.getSize() && contents.size() < CAPACITY; i++) {
                ItemStack item = inventory.getItem(i);
                if (i != slot && isFreshPants(item)) {
                    for (int n = 0; n < item.getAmount() && contents.size() < CAPACITY; n++) {
                        ItemStack one = item.clone();
                        one.setAmount(1);
                        contents.add(one);
                    }
                    inventory.setItem(i, null);
                }
            }
            if (contents.isEmpty()) {
                player.sendMessage("§cYou don't have any Fresh Pants to store!");
                player.playSound(player.getLocation(), "mob.villager.no", 1.0f, 1.0f);
                return;
            }
            inventory.setItem(slot, write(bundle, contents));
            player.sendMessage("§b§lPANTS BUNDLE! §7Stored §f" + contents.size() + " Fresh Pants§7.");
            player.playSound(player.getLocation(), "mob.horse.armor", 1.0f, 1.0f);
            return;
        }
        List<ItemStack> left = new ArrayList<>();
        int taken = 0;
        for (ItemStack pants : contents) {
            if (inventory.firstEmpty() < 0) {
                left.add(pants);
            } else {
                inventory.addItem(pants);
                taken++;
            }
        }
        inventory.setItem(slot, write(bundle, left));
        if (!left.isEmpty()) {
            player.sendMessage("§cNot enough inventory space! §7" + left.size() + " pants left in the bundle.");
            player.playSound(player.getLocation(), "mob.villager.no", 1.0f, 1.0f);
        }
        if (taken > 0) {
            player.sendMessage("§b§lPANTS BUNDLE! §7Took out §f" + taken + " Fresh Pants§7.");
            player.playSound(player.getLocation(), "mob.horse.armor", 1.0f, 1.2f);
        }
    }

    static boolean isFreshPants(ItemStack item) {
        return MysticItems.isMysticPants(item) && MysticData.read(item).isFresh();
    }

    static List<ItemStack> read(ItemStack bundle) {
        List<ItemStack> items = new ArrayList<>();
        NBTTagCompound extra = NbtUtil.readExtraAttributes(bundle);
        NBTTagList list = extra.getList(KEY, 8);
        for (int i = 0; i < list.size(); i++) {
            try {
                items.add(NbtUtil.fromBase64(list.getString(i)));
            } catch (IOException ignored) {
                // 壊れたものは捨てる
            }
        }
        return items;
    }

    /** 中身を書き込み、名前と説明 (ユーザーのスクリーンショットの形) を合わせたコピー。 */
    static ItemStack write(ItemStack bundle, List<ItemStack> contents) {
        // 中身があるときはチェスト付きトロッコ、空ならトロッコ
        ItemStack item = new ItemStack(contents.isEmpty() ? Material.MINECART : Material.STORAGE_MINECART);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§bPants Bundle");
        List<String> lore = new ArrayList<>();
        lore.add("§7Kept on death");
        lore.add("");
        if (contents.isEmpty()) {
            lore.add("§7Stores up to §f" + CAPACITY + " Fresh Pants§7.");
            lore.add("");
            lore.add("§eHold and right-click to store!");
        } else {
            lore.add("§7Contents:");
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (ItemStack pants : contents) {
                String name = pantsName(pants);
                counts.put(name, counts.containsKey(name) ? counts.get(name) + 1 : 1);
            }
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                lore.add("§7" + entry.getValue() + "x " + entry.getKey());
            }
            lore.add("");
            lore.add("§eHold and right-click to open!");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        NBTTagCompound extra = NbtUtil.readExtraAttributes(item);
        extra.setInt(ShopItem.KEY, ShopItem.PANTS_BUNDLE.ordinal() + 1);
        NBTTagList list = new NBTTagList();
        for (ItemStack pants : contents) {
            try {
                list.add(new NBTTagString(NbtUtil.toBase64(pants)));
            } catch (IOException ignored) {
                // 書けないものは無い
            }
        }
        extra.set(KEY, list);
        return NbtUtil.writeExtraAttributes(item, extra);
    }

    private static String pantsName(ItemStack pants) {
        PantsColor color = pants.getItemMeta() instanceof LeatherArmorMeta
                ? PantsColor.fromColor(((LeatherArmorMeta) pants.getItemMeta()).getColor()) : null;
        if (color == null) {
            return pants.getItemMeta().hasDisplayName() ? pants.getItemMeta().getDisplayName() : "§fPants";
        }
        return color.getChatColor() + color.getDisplayName() + " Pants";
    }
}
