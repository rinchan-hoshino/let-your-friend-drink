package dev.rinchan.letyourfrienddrink.fabric.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.rinchan.letyourfrienddrink.client.FriendDrinkConfigScreen;

public final class FriendDrinkModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return FriendDrinkConfigScreen::new;
    }
}
