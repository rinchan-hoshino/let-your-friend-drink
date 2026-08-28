package dev.rinchan.letyourfrienddrink;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;

public final class FriendEatingAccess {
    private static volatile FriendEatingBridge bridge;

    private FriendEatingAccess() {
    }

    public static synchronized void install(FriendEatingBridge installedBridge) {
        Objects.requireNonNull(installedBridge, "installedBridge");
        if (bridge != null) throw new IllegalStateException("Let Your Friend Eating bridge already installed");
        bridge = installedBridge;
    }

    public static boolean canFeed(UUID feederId) {
        return requireBridge().canFeed(feederId);
    }

    public static void recordFeed(UUID feederId) {
        requireBridge().recordFeed(feederId);
    }

    public static void recordSuccessfulFeed(ServerLevel level, UUID feederId, UUID recipientId) {
        requireBridge().recordSuccessfulFeed(level, feederId, recipientId);
    }

    private static FriendEatingBridge requireBridge() {
        FriendEatingBridge installed = bridge;
        if (installed == null) throw new IllegalStateException("Let Your Friend Eating bridge is not installed");
        return installed;
    }
}
