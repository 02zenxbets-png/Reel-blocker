package com.juno.reelblock;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

/**
 * Watches the foreground app. When Instagram Reels or YouTube Shorts PLAYER
 * is detected, backs out and shows the BlockActivity screen.
 *
 * IMPORTANT: the home feeds also contain reels/shorts strips, so view-level
 * checks must target the fullscreen PLAYER only — never the feed/shelf.
 */
public class ReelBlockService extends AccessibilityService {

    private static final String PKG_INSTAGRAM = "com.instagram.android";
    private static final String PKG_YOUTUBE = "com.google.android.youtube";

    private long lastBlockAt = 0;
    private String lastBlockedKey = "";
    private long lastWatchedAt = 0;
    private String lastWatchedKey = "";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int type = event.getEventType();
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return;
        }
        CharSequence pkg = event.getPackageName();
        if (pkg == null) return;
        String packageName = pkg.toString();

        String kind = null;
        if (PKG_INSTAGRAM.equals(packageName) && looksLikeInstagramReels(event)) {
            kind = "reels";
        } else if (PKG_YOUTUBE.equals(packageName) && looksLikeYouTubeShorts(event)) {
            kind = "shorts";
        }
        if (kind == null) return;

        boolean blockingOn = "reels".equals(kind) ? Prefs.blockReels(this) : Prefs.blockShorts(this);
        if (blockingOn) {
            blockReel(packageName, kind);
        } else {
            countWatched(packageName, kind);
        }
    }

    /** Blocking ON: back out, count it, show the friction screen. */
    private void blockReel(String packageName, String kind) {
        // Debounce: don't re-block the same surface within 1.5 seconds.
        String key = packageName + ":" + kind;
        long now = SystemClock.uptimeMillis();
        if (key.equals(lastBlockedKey) && now - lastBlockAt < 1500) return;
        lastBlockedKey = key;
        lastBlockAt = now;

        Prefs.incBlocked(this);
        performGlobalAction(GLOBAL_ACTION_BACK);

        Intent i = new Intent(this, BlockActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra(BlockActivity.EXTRA_KIND, kind);
        startActivity(i);
    }

    /**
     * Blocking OFF: the reel/shorts was allowed through — count it as watched.
     * Debounced (3s) so one viewing session counts once.
     */
    private void countWatched(String packageName, String kind) {
        String key = packageName + ":" + kind;
        long now = SystemClock.uptimeMillis();
        if (key.equals(lastWatchedKey) && now - lastWatchedAt < 3000) return;
        lastWatchedKey = key;
        lastWatchedAt = now;
        Prefs.incWatched(this);
    }

    private boolean looksLikeInstagramReels(AccessibilityEvent event) {
        int type = event.getEventType();
        // Window-level class check only on state changes (content changes carry
        // individual view classes, e.g. feed items — too noisy).
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && classNameHits(event, "reel", "clip", "clipsviewer")) {
            return true;
        }
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        try {
            // Reels tab selected in bottom nav = reels viewer open.
            List<AccessibilityNodeInfo> reelsTabs =
                    root.findAccessibilityNodeInfosByText("Reels");
            for (AccessibilityNodeInfo n : reelsTabs) {
                if (n.isSelected()) return true;
            }
            return false;
        } finally {
            root.recycle();
        }
    }

    /**
     * YouTube Shorts PLAYER detection. The home feed embeds a Shorts shelf, so
     * we must NOT match shelf/feed/carousel/thumbnail views — only the player.
     */
    private boolean looksLikeYouTubeShorts(AccessibilityEvent event) {
        int type = event.getEventType();
        // 1. Window-level class check only on state changes.
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && classNameHits(event, "shorts", "reel")) {
            return true;
        }
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        try {
            // 2. Shorts pivot tab selected = shorts feed/player open.
            List<AccessibilityNodeInfo> shortsTabs =
                    root.findAccessibilityNodeInfosByText("Shorts");
            for (AccessibilityNodeInfo n : shortsTabs) {
                if (n.isSelected()) return true;
            }
            // 3. Player-container view IDs (excludes home shelf).
            if (hasPlayerViewId(root)) return true;
            // 4. Player chrome descriptions.
            if (hasAnyText(root, "Shorts player")) return true;
            if (hasContentDescFragment(root, "shorts player")) return true;
            if (hasContentDescFragment(root, "reel player")) return true;
            return false;
        } finally {
            root.recycle();
        }
    }

    /** True if the event's window class name contains any of the fragments. */
    private boolean classNameHits(AccessibilityEvent event, String... fragments) {
        CharSequence cls = event.getClassName();
        if (cls == null) return false;
        String c = cls.toString().toLowerCase();
        for (String f : fragments) {
            if (c.contains(f)) return true;
        }
        return false;
    }

    /**
     * Matches player/watch/fragment/container view IDs containing reel/shorts,
     * but explicitly excludes shelf/carousel/feed/item/thumbnail (home feed).
     */
    private boolean hasPlayerViewId(AccessibilityNodeInfo root) {
        return walkForPlayerId(root, 0);
    }

    private boolean walkForPlayerId(AccessibilityNodeInfo node, int depth) {
        if (node == null || depth > 7) return false;
        CharSequence id = node.getViewIdResourceName();
        if (id != null) {
            String s = id.toString().toLowerCase();
            boolean isReelOrShorts = s.contains("reel") || s.contains("shorts");
            boolean isPlayerish = s.contains("player") || s.contains("watch")
                    || s.contains("fragment") || s.contains("container");
            boolean isFeedish = s.contains("shelf") || s.contains("carousel")
                    || s.contains("feed") || s.contains("item")
                    || s.contains("thumbnail") || s.contains("chip");
            if (isReelOrShorts && isPlayerish && !isFeedish) return true;
        }
        int count = node.getChildCount();
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            boolean hit = walkForPlayerId(child, depth + 1);
            if (child != null) child.recycle();
            if (hit) return true;
        }
        return false;
    }

    private boolean hasAnyText(AccessibilityNodeInfo root, String... texts) {
        for (String t : texts) {
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(t);
            boolean found = !nodes.isEmpty();
            for (AccessibilityNodeInfo n : nodes) n.recycle();
            if (found) return true;
        }
        return false;
    }

    private boolean hasContentDescFragment(AccessibilityNodeInfo root, String fragment) {
        return walkForDesc(root, fragment.toLowerCase(), 0);
    }

    private boolean walkForDesc(AccessibilityNodeInfo node, String fragment, int depth) {
        if (node == null || depth > 7) return false;
        CharSequence desc = node.getContentDescription();
        if (desc != null && desc.toString().toLowerCase().contains(fragment)) return true;
        int count = node.getChildCount();
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            boolean hit = walkForDesc(child, fragment, depth + 1);
            if (child != null) child.recycle();
            if (hit) return true;
        }
        return false;
    }

    @Override
    public void onInterrupt() { /* no-op */ }
}
