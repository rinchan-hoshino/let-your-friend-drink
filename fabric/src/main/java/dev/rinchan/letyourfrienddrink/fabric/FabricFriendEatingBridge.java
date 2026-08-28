package dev.rinchan.letyourfrienddrink.fabric;

import dev.rinchan.letyourfrienddrink.FriendEatingBridge;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import th.in.tamkungz.letyourfriendeating.data.FeedStatsSavedData;
import th.in.tamkungz.letyourfriendeating.manager.CooldownManager;

final class FabricFriendEatingBridge implements FriendEatingBridge {
    @Override
    public boolean canFeed(UUID feederId) {
        return CooldownManager.canFeed(feederId);
    }

    @Override
    public void recordFeed(UUID feederId) {
        CooldownManager.recordFeed(feederId);
    }

    @Override
    public void recordSuccessfulFeed(ServerLevel level, UUID feederId, UUID recipientId) {
        FeedStatsSavedData stats = FeedStatsSavedData.get(level);
        stats.incrementFedCount(feederId);
        stats.incrementEatenCount(recipientId);
    }

    @Override
    public ItemStack finishDrink(ItemStack offered, ServerLevel level, Player recipient) {
        return offered.finishUsingItem(level, recipient);
    }
}
