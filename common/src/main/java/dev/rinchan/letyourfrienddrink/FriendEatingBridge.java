package dev.rinchan.letyourfrienddrink;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;

public interface FriendEatingBridge {
    boolean canFeed(UUID feederId);

    void recordFeed(UUID feederId);

    void recordSuccessfulFeed(ServerLevel level, UUID feederId, UUID recipientId);
}
