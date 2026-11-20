package dev.rinchan.letyourfrienddrink.forge;

import dev.rinchan.letyourfrienddrink.FriendEatingAccess;
import dev.rinchan.letyourfrienddrink.LetYourFriendDrink;
import dev.rinchan.letyourfrienddrink.ReflectiveFriendEatingBridge;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(LetYourFriendDrink.MOD_ID)
public final class LetYourFriendDrinkForge {
    public LetYourFriendDrinkForge() {
        LetYourFriendDrink.init(FMLPaths.CONFIGDIR.get());
        FriendEatingAccess.install(new ReflectiveFriendEatingBridge((offered, level, recipient) -> {
            ItemStack original = offered.copy();
            ItemStack completed = offered.finishUsingItem(level, recipient);
            return ForgeEventFactory.onItemUseFinish(recipient, original, 0, completed);
        }));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> LetYourFriendDrinkForge::registerClientOnly);
    }

    private static void registerClientOnly() {
        try {
            Class.forName("dev.rinchan.letyourfrienddrink.forge.client.LetYourFriendDrinkForgeClient")
                .getMethod("registerConfigScreen")
                .invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to register Forge config screen", e);
        }
    }
}
