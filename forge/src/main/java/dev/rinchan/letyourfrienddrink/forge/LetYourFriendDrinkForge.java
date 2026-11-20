package dev.rinchan.letyourfrienddrink.forge;

import dev.rinchan.letyourfrienddrink.FriendEatingAccess;
import dev.rinchan.letyourfrienddrink.LetYourFriendDrink;
import dev.rinchan.letyourfrienddrink.ReflectiveFriendEatingBridge;
import dev.rinchan.letyourfrienddrink.client.FriendDrinkConfigScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
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
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> LetYourFriendDrinkForge::registerConfigScreen);
    }

    private static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new FriendDrinkConfigScreen(parent))
        );
    }
}
