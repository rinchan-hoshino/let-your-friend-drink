package dev.rinchan.letyourfrienddrink;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class FriendEatingAccess {
    private static volatile FriendEatingBridge bridge;

    private FriendEatingAccess() {
    }

    public static synchronized void install(FriendEatingBridge candidate) {
        Objects.requireNonNull(candidate, "candidate");
        if (bridge != null) {
            throw new IllegalStateException("Let Your Friend Eating bridge is already installed");
        }
        bridge = candidate;
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

    public static ItemStack finishDrink(ItemStack offered, ServerLevel level, Player recipient) {
        return requireBridge().finishDrink(offered, level, recipient);
    }

    private static FriendEatingBridge requireBridge() {
        FriendEatingBridge installed = bridge;
        if (installed == null) {
            throw new IllegalStateException("Let Your Friend Eating bridge is not installed");
        }
        return installed;
    }
}
