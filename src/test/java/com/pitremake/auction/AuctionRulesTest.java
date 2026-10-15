package com.pitremake.auction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuctionRulesTest {

    @Test
    void nextBidIsTenPercentMoreLikeTheRecording() {
        assertEquals(4_950, AuctionRules.nextBid(4_500));
        assertEquals(5_445, AuctionRules.nextBid(4_950));
        assertEquals(18_150, AuctionRules.nextBid(16_500));
        assertEquals(19_965, AuctionRules.nextBid(18_150));
        // 10 で割り切れないときは切り上げ
        assertEquals(17, AuctionRules.nextBid(15));
        assertEquals(1_100, AuctionRules.nextBid(1_000));
    }

    @Test
    void textLikeTheRecording() {
        assertEquals("00:56", AuctionRules.time(56));
        assertEquals("01:10", AuctionRules.time(70));
        assertEquals("00:00", AuctionRules.time(-3));
        assertEquals("Just now", AuctionRules.ago(400));
        assertEquals("2 seconds ago", AuctionRules.ago(2_050));
        assertEquals("63 seconds ago", AuctionRules.ago(63_900));
        assertEquals("4,500g", AuctionRules.gold(4_500));
        assertEquals("19,965g", AuctionRules.gold(19_965));
    }

    @Test
    void signInput() {
        assertEquals(10_000, AuctionRules.parseBid("10000"));
        assertEquals(10_000, AuctionRules.parseBid("10,000g"));
        assertEquals(0, AuctionRules.parseBid("0"));
        assertEquals(0, AuctionRules.parseBid(""));
        assertEquals(0, AuctionRules.parseBid(null));
    }
}
