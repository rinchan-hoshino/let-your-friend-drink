package dev.rinchan.letyourfrienddrink.forge.client;

import dev.rinchan.letyourfrienddrink.client.FriendDrinkConfigScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

public final class LetYourFriendDrinkForgeClient {
    private LetYourFriendDrinkForgeClient() {}

    public static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new FriendDrinkConfigScreen(parent))
        );
    }
}
