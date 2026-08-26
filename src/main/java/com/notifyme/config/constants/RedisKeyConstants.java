package com.notifyme.config.constants;

/**
 * Constantes de chaves para o Redis Cache.
 * Centraliza os prefixos para evitar duplicação e divergências no código.
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
        // Construtor privado para evitar instanciação
    }

    /**
     * Prefixo para chaves de preferências de usuário (ex: user:prefs:123).
     */
    public static final String USER_PREFS_KEY_PREFIX = "user:prefs:";

    /**
     * Prefixo para travas de idempotência de vídeos (ex: lock:video:vid_987).
     */
    public static final String VIDEO_IDEMPOTENCY_PREFIX = "lock:video:";
}
