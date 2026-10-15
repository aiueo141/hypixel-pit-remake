package com.pitremake.auction;

import java.util.Locale;

/** オークションの数字 (録画 2026-10-15_11-11-03.mcrc と pit.wiki/Auction)。 */
public final class AuctionRules {

    /** 長さ (秒)。 */
    public static final int DURATION_SECONDS = 60;
    /** 残りがこの秒数未満で入札されたら延長する。 */
    public static final int EXTEND_BELOW_SECONDS = 20;
    /** 延長する秒数 (チャットに §a+10s)。 */
    public static final int EXTEND_SECONDS = 10;

    private AuctionRules() {
    }

    /** 次の最低入札額 = 今の最高額 + 10% (録画: 4,500 → 4,950 → 5,445、16,500 → 18,150 → 19,965)。 */
    public static long nextBid(long top) {
        return top + (top + 9) / 10;
    }

    /** 残り時間 (例: 00:56)。 */
    public static String time(long seconds) {
        long s = Math.max(0, seconds);
        return String.format(Locale.US, "%02d:%02d", s / 60, s % 60);
    }

    /** 入札からの時間 (録画: Just now / 2 seconds ago / 63 seconds ago。分にはしない)。 */
    public static String ago(long millis) {
        long seconds = Math.max(0, millis) / 1000;
        if (seconds < 1) {
            return "Just now";
        }
        return seconds + (seconds == 1 ? " second ago" : " seconds ago");
    }

    /** 看板に書かれた入札額 (数字以外は無視。何も無ければ 0。録画: "0" → Custom bid: 0g)。 */
    public static long parseBid(String text) {
        String digits = text == null ? "" : text.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return 0;
        }
        if (digits.length() > 12) {
            return Long.MAX_VALUE / 2;
        }
        return Long.parseLong(digits);
    }

    /** 例: 4,500g */
    public static String gold(long amount) {
        return String.format(Locale.US, "%,dg", amount);
    }
}
