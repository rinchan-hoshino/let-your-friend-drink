package dev.rinchan.letyourfrienddrink.api;

import java.util.Objects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class FriendDrinkCompletedEvent {
    private final Player feeder;
    private final Player recipient;
    private final InteractionHand hand;
    private final ItemStack offered;
    private final ItemStack remainder;

    public FriendDrinkCompletedEvent(
        Player feeder,
        Player recipient,
        InteractionHand hand,
        ItemStack offered,
        ItemStack remainder
    ) {
        this.feeder = Objects.requireNonNull(feeder, "feeder");
        this.recipient = Objects.requireNonNull(recipient, "recipient");
        this.hand = Objects.requireNonNull(hand, "hand");
        this.offered = Objects.requireNonNull(offered, "offered").copy();
        this.remainder = Objects.requireNonNull(remainder, "remainder").copy();
    }

    public Player feeder() {
        return feeder;
    }

    public Player recipient() {
        return recipient;
    }

    public InteractionHand hand() {
        return hand;
    }

    public ItemStack offered() {
        return offered.copy();
    }

    public ItemStack remainder() {
        return remainder.copy();
    }
}
