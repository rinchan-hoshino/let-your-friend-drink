package dev.rinchan.letyourfrienddrink.fabric;

import dev.rinchan.letyourfrienddrink.FriendEatingAccess;
import dev.rinchan.letyourfrienddrink.LetYourFriendDrink;
import dev.rinchan.letyourfrienddrink.ReflectiveFriendEatingBridge;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class LetYourFriendDrinkFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        LetYourFriendDrink.init(FabricLoader.getInstance().getConfigDir());
        FriendEatingAccess.install(new ReflectiveFriendEatingBridge(
            (offered, level, recipient) -> offered.finishUsingItem(level, recipient)
        ));
    }
}
