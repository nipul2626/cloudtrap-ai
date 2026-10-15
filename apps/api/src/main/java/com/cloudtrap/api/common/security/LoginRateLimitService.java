package com.cloudtrap.api.common.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.List;

@Service
public class LoginRateLimitService {
    private static final DefaultRedisScript<Long> INCREMENT_SCRIPT = new DefaultRedisScript<>(
            "local n = redis.call('INCR', KEYS[1]); if n == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]); end; return n", Long.class);
    private final StringRedisTemplate redisTemplate;
    public LoginRateLimitService(StringRedisTemplate redisTemplate) { this.redisTemplate = redisTemplate; }

    public boolean isAllowed(String clientKey, int limit, Duration window) {
        String safeKey = clientKey == null || clientKey.isBlank() ? "unknown" : clientKey;
        Long count = redisTemplate.execute(INCREMENT_SCRIPT, List.of("cloudtrap:security:login:" + safeKey), Long.toString(window.toSeconds()));
        return count != null && count <= limit;
    }
    public long retryAfterSeconds(String clientKey, Duration window) {
        String safeKey = clientKey == null || clientKey.isBlank()
                ? "unknown"
                : clientKey;

        String redisKey = "cloudtrap:security:login:" + safeKey;

        Long ttl = redisTemplate.getExpire(
                redisKey,
                java.util.concurrent.TimeUnit.SECONDS
        );

        if (ttl == null || ttl <= 0) {
            return Math.max(1, window.toSeconds());
        }

        return ttl;
    }

}
