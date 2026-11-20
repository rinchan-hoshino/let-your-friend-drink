package dev.rinchan.letyourfrienddrink.neoforge;

import dev.rinchan.letyourfrienddrink.FriendEatingAccess;
import dev.rinchan.letyourfrienddrink.LetYourFriendDrink;
import dev.rinchan.letyourfrienddrink.ReflectiveFriendEatingBridge;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.EventHooks;

@Mod(LetYourFriendDrink.MOD_ID)
public final class LetYourFriendDrinkNeoForge {
    public LetYourFriendDrinkNeoForge() {
        LetYourFriendDrink.init(FMLPaths.CONFIGDIR.get());
        FriendEatingAccess.install(new ReflectiveFriendEatingBridge((offered, level, recipient) -> {
            ItemStack original = offered.copy();
            ItemStack completed = offered.finishUsingItem(level, recipient);
            return EventHooks.onItemUseFinish(recipient, original, 0, completed);
        }));
    }
}
