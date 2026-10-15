package com.pitremake.util;

import org.bukkit.inventory.InventoryHolder;

/** クリックでボタンを押すだけの画面 (アイテムは動かせない)。{@link ClickMenuListener} がクリックを渡す。 */
public interface ClickMenu extends InventoryHolder {

    /** 画面の中のスロットがクリックされたとき。 */
    void click(int slot);

    /** 右クリックを区別したい画面はこちらを上書きする。 */
    default void click(int slot, boolean rightClick) {
        click(slot);
    }

    /** 下の自分のインベントリ (0〜35、0〜8 がホットバー) がクリックされたとき。トレードの画面などが上書きする。 */
    default void clickInventory(int slot, boolean rightClick) {
    }

    /** 画面が閉じられたとき。 */
    default void close() {
    }
}
