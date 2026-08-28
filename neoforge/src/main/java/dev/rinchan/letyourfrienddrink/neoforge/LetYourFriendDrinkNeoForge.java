package dev.rinchan.letyourfrienddrink.neoforge;

import dev.rinchan.letyourfrienddrink.FriendEatingAccess;
import dev.rinchan.letyourfrienddrink.LetYourFriendDrink;
import net.neoforged.fml.common.Mod;

@Mod(LetYourFriendDrink.MOD_ID)
public final class LetYourFriendDrinkNeoForge {
    public LetYourFriendDrinkNeoForge() {
        FriendEatingAccess.install(new NeoForgeFriendEatingBridge());
    }
}
