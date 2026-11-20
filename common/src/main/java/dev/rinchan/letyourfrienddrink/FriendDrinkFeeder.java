package dev.rinchan.letyourfrienddrink;

import dev.rinchan.letyourfrienddrink.api.FriendDrinkCompletedEvent;
import dev.rinchan.letyourfrienddrink.api.FriendDrinkCompletedEvents;
import dev.rinchan.letyourfrienddrink.config.FriendDrinkConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class FriendDrinkFeeder {
    private FriendDrinkFeeder() {
    }

    public static InteractionResult tryFeedDrink(Player feeder, Player recipient, InteractionHand hand) {
        ItemStack held = feeder.getItemInHand(hand);
        boolean drinkAnim = VersionBridge.isDrink(held);
        boolean handledByBaseMod = held.is(Items.MILK_BUCKET);
        if (!FriendDrinkPolicy.shouldHandle(drinkAnim, handledByBaseMod)) {
            return null;
        }

        FriendDrinkConfig config = FriendDrinkConfig.get();
        if (VersionBridge.isExcluded(held, config.exclusionMatcher())) {
            return InteractionResult.PASS;
        }

        if (feeder == recipient
            || !feeder.isAlive()
            || !recipient.isAlive()
            || feeder.isSpectator()
            || !(feeder.level() instanceof ServerLevel level)
            || recipient.level() != level) {
            return InteractionResult.PASS;
        }

        if (FriendDrinkPolicy.shouldBlockHarmfulDrink(
            VersionBridge.containsHarmfulEffect(held),
            config.allowHarmfulDrinks()
        )) {
            VersionBridge.sendActionBar(
                feeder,
                Component.translatable("message.let_your_friend_drink.harmful_blocked")
                    .withStyle(ChatFormatting.RED)
            );
            return InteractionResult.FAIL;
        }

        if (!FriendEatingAccess.canFeed(feeder.getUUID())) {
            return InteractionResult.FAIL;
        }

        ItemStack offered = held.copyWithCount(1);
        ItemStack original = offered.copy();
        boolean recipientHadInfiniteMaterials = recipient.getAbilities().instabuild;
        ItemStack remainder;
        recipient.getAbilities().instabuild = false;
        try {
            remainder = FriendEatingAccess.finishDrink(offered, level, recipient);
        } finally {
            recipient.getAbilities().instabuild = recipientHadInfiniteMaterials;
        }

        FriendEatingAccess.recordFeed(feeder.getUUID());
        FriendEatingAccess.recordSuccessfulFeed(level, feeder.getUUID(), recipient.getUUID());
        feeder.swing(hand, true);
        VersionBridge.playDrinkSound(level, recipient, original);
        level.sendParticles(
            ParticleTypes.HAPPY_VILLAGER,
            recipient.getX(), recipient.getY() + 1.0D, recipient.getZ(),
            5, 0.5D, 0.5D, 0.5D, 0.0D
        );
        transferRemainder(feeder, hand, held, original, remainder);
        FriendDrinkCompletedEvents.post(new FriendDrinkCompletedEvent(feeder, recipient, hand, original, remainder));
        return InteractionResult.SUCCESS;
    }

    private static void transferRemainder(
        Player feeder,
        InteractionHand hand,
        ItemStack held,
        ItemStack original,
        ItemStack remainder
    ) {
        if (FriendDrinkPolicy.preservesHeldItem(feeder.isCreative(), ItemStack.matches(original, remainder))) {
            return;
        }
        held.shrink(1);
        if (remainder.isEmpty()) {
            return;
        }
        if (held.isEmpty()) {
            feeder.setItemInHand(hand, remainder);
        } else if (!feeder.getInventory().add(remainder)) {
            feeder.drop(remainder, false);
        }
    }
}
