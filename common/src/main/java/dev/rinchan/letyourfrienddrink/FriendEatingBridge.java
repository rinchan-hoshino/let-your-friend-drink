package dev.rinchan.letyourfrienddrink;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface FriendEatingBridge {
    boolean canFeed(UUID feederId);

    void recordFeed(UUID feederId);

    void recordSuccessfulFeed(ServerLevel level, UUID feederId, UUID recipientId);

    ItemStack finishDrink(ItemStack offered, ServerLevel level, Player recipient);
}
