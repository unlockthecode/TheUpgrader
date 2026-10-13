package com.unlockthecode.upgrader.listener;

import com.unlockthecode.upgrader.UpgraderPlugin;
import com.unlockthecode.upgrader.config.UpgraderConfig;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AttemptLimiter {

    private final UpgraderConfig config;
    private final Map<UUID, Deque<Long>> attempts = new HashMap<>();

    public AttemptLimiter(UpgraderPlugin plugin) {
        this.config = plugin.getUpgraderConfig();
    }

    public int cooldownRemaining(Player player) {
        int limit = config.attemptLimit;
        int window = config.attemptWindowSeconds;
        if (limit <= 0 || window <= 0) {
            return 0;
        }
        Deque<Long> recent = attempts.get(player.getUniqueId());
        if (recent == null) {
            return 0;
        }
        long windowMs = (long) window * 1000L;
        prune(recent, windowMs);
        if (recent.isEmpty()) {
            attempts.remove(player.getUniqueId());
            return 0;
        }
        if (recent.size() < limit) {
            return 0;
        }
        long freeAt = recent.peekFirst() + windowMs;
        return (int) Math.max(1L, (freeAt - System.currentTimeMillis() + 999L) / 1000L);
    }

    public void record(Player player) {
        if (config.attemptLimit <= 0 || config.attemptWindowSeconds <= 0) {
            return;
        }
        Deque<Long> recent = attempts.computeIfAbsent(player.getUniqueId(), id -> new ArrayDeque<>());
        prune(recent, (long) config.attemptWindowSeconds * 1000L);
        recent.addLast(System.currentTimeMillis());
    }

    public void forget(UUID playerId) {
        attempts.remove(playerId);
    }

    private void prune(Deque<Long> recent, long windowMs) {
        long cutoff = System.currentTimeMillis() - windowMs;
        while (!recent.isEmpty() && recent.peekFirst() <= cutoff) {
            recent.removeFirst();
        }
    }
}