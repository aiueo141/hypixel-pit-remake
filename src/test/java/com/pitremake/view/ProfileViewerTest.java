package com.pitremake.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProfileViewerTest {

    @Test
    void lastLoginOfOfflinePlayers() {
        assertEquals("0 minutes ago", ProfileViewer.ago(30_000));
        assertEquals("1 minute ago", ProfileViewer.ago(60_000));
        assertEquals("59 minutes ago", ProfileViewer.ago(59 * 60_000));
        assertEquals("1 hour ago", ProfileViewer.ago(60 * 60_000));
        assertEquals("23 hours ago", ProfileViewer.ago(23 * 60 * 60_000L));
        assertEquals("3 days ago", ProfileViewer.ago(3 * 24 * 60 * 60_000L));
    }
}
