package dev.rinchan.letyourfrienddrink.fabric;

import dev.rinchan.letyourfrienddrink.FriendEatingAccess;
import net.fabricmc.api.ModInitializer;

public final class LetYourFriendDrinkFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        FriendEatingAccess.install(new FabricFriendEatingBridge());
    }
}
