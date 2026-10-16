package com.pitremake.event;

/** 自動で始まるイベント (Spire / KOTH / 2X REWARDS / Auction)。/events と重なりのチェックに使う。 */
public interface PitEvent {

    /** config のキー ("spire" など)。interval-minutes / offset-minutes を読む。 */
    String configKey();

    /** /events に出す名前 (色付き)。 */
    String displayName();

    /** 自動で始まるか (config の enabled と、塔などのデータがあるか)。 */
    boolean isScheduled();

    /** 開始の時刻 (offset-minutes の時刻) から終わるまでの分 (重なりのチェック用。延長の分も入れた目安)。 */
    int lengthMinutes();

    /** 次に始まる時刻 (ミリ秒)。 */
    long nextStart();

    /** 開催中なら終わる時刻 (ミリ秒)、開催中でなければ 0。 */
    long endsAt();

    /** 次に始まる時刻を今の config で決め直す (/pit reload、/events auto でオンにしたとき)。 */
    void scheduleNext();
}
