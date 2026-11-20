package dev.rinchan.letyourfrienddrink.neoforge.client;

import dev.rinchan.letyourfrienddrink.LetYourFriendDrink;
import dev.rinchan.letyourfrienddrink.client.FriendDrinkConfigScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = LetYourFriendDrink.MOD_ID, dist = Dist.CLIENT)
public final class LetYourFriendDrinkNeoForgeClient {
    public LetYourFriendDrinkNeoForgeClient(ModContainer container) {
        container.registerExtensionPoint(
            IConfigScreenFactory.class,
            (modContainer, parent) -> new FriendDrinkConfigScreen(parent)
        );
    }
}
