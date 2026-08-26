package com.notifyme.config.constants;

/**
 * Key constants for Redis Cache.
 * Centralizes key prefixes to avoid duplication and inconsistencies across services.
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
        // Private constructor to prevent instantiation
    }

    /**
     * Prefix for user preference keys (e.g., user:prefs:123).
     */
    public static final String USER_PREFS_KEY_PREFIX = "user:prefs:";

    /**
     * Prefix for video idempotency lock keys (e.g., lock:video:vid_987).
     */
    public static final String VIDEO_IDEMPOTENCY_PREFIX = "lock:video:";
}
