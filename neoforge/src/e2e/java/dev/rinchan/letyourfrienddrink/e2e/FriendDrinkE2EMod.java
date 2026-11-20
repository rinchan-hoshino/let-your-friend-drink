package dev.rinchan.letyourfrienddrink.e2e;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@Mod(FriendDrinkE2EMod.MOD_ID)
public final class FriendDrinkE2EMod {
    public static final String MOD_ID = "lyfd_e2e";

    public FriendDrinkE2EMod(IEventBus modBus) {
        modBus.addListener(this::onRegisterGameTests);
    }

    private void onRegisterGameTests(RegisterGameTestsEvent event) {
        event.register(FriendDrinkPackagedJarGameTests.class);
    }
}
