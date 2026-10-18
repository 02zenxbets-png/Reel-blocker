package com.juno.reelblock;

import android.content.Context;
import android.content.SharedPreferences;

/** Toggle + counter storage. */
public final class Prefs {
    private static final String FILE = "reelblock";
    public static final String KEY_BLOCK_REELS = "block_reels";
    public static final String KEY_BLOCK_SHORTS = "block_shorts";
    public static final String KEY_DELAY_SECS = "delay_secs";
    public static final String KEY_STRICT = "strict_mode";
    public static final String KEY_BLOCKED_COUNT = "count_blocked";
    public static final String KEY_WATCHED_COUNT = "count_watched";

    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static boolean blockReels(Context c) {
        return sp(c).getBoolean(KEY_BLOCK_REELS, true);
    }

    public static boolean blockShorts(Context c) {
        return sp(c).getBoolean(KEY_BLOCK_SHORTS, true);
    }

    public static int delaySecs(Context c) {
        return sp(c).getInt(KEY_DELAY_SECS, 10);
    }

    public static boolean strictMode(Context c) {
        return sp(c).getBoolean(KEY_STRICT, true);
    }

    public static void set(Context c, String key, boolean v) {
        sp(c).edit().putBoolean(key, v).apply();
    }

    public static void setDelay(Context c, int secs) {
        sp(c).edit().putInt(KEY_DELAY_SECS, secs).apply();
    }

    public static int blockedCount(Context c) {
        return sp(c).getInt(KEY_BLOCKED_COUNT, 0);
    }

    public static int watchedCount(Context c) {
        return sp(c).getInt(KEY_WATCHED_COUNT, 0);
    }

    public static void incBlocked(Context c) {
        SharedPreferences s = sp(c);
        s.edit().putInt(KEY_BLOCKED_COUNT, s.getInt(KEY_BLOCKED_COUNT, 0) + 1).apply();
    }

    public static void incWatched(Context c) {
        SharedPreferences s = sp(c);
        s.edit().putInt(KEY_WATCHED_COUNT, s.getInt(KEY_WATCHED_COUNT, 0) + 1).apply();
    }

    public static void resetCounters(Context c) {
        sp(c).edit()
                .putInt(KEY_BLOCKED_COUNT, 0)
                .putInt(KEY_WATCHED_COUNT, 0)
                .apply();
    }
}
