package dev.rinchan.letyourfrienddrink;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Runtime bridge to Let Your Friend Eating that transparently supports both
 * legacy {@code th.in.tamkungz.letyourfriendeating} (1.1.x) and relocated
 * {@code me.tamkungz.letyourfriendeating} (1.2.x+) packages.
 */
public final class ReflectiveFriendEatingBridge implements FriendEatingBridge {
    private static final String[] PACKAGES = {
        "me.tamkungz.letyourfriendeating",
        "th.in.tamkungz.letyourfriendeating"
    };

    @FunctionalInterface
    public interface DrinkFinisher {
        ItemStack finish(ItemStack offered, ServerLevel level, Player recipient);
    }

    private final DrinkFinisher drinkFinisher;
    private volatile ResolvedHandles handles;

    public ReflectiveFriendEatingBridge(DrinkFinisher drinkFinisher) {
        this.drinkFinisher = Objects.requireNonNull(drinkFinisher, "drinkFinisher");
    }

    @Override
    public boolean canFeed(UUID feederId) {
        ResolvedHandles resolved = resolve();
        try {
            return (boolean) resolved.canFeed.invoke(null, feederId);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to invoke CooldownManager.canFeed", e);
        }
    }

    @Override
    public void recordFeed(UUID feederId) {
        ResolvedHandles resolved = resolve();
        try {
            resolved.recordFeed.invoke(null, feederId);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to invoke CooldownManager.recordFeed", e);
        }
    }

    @Override
    public void recordSuccessfulFeed(ServerLevel level, UUID feederId, UUID recipientId) {
        ResolvedHandles resolved = resolve();
        try {
            Object stats = resolved.getStats.invoke(null, level);
            if (stats != null) {
                resolved.incrementFedCount.invoke(stats, feederId);
                resolved.incrementEatenCount.invoke(stats, recipientId);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to record feed statistics", e);
        }
    }

    @Override
    public ItemStack finishDrink(ItemStack offered, ServerLevel level, Player recipient) {
        return drinkFinisher.finish(offered, level, recipient);
    }

    private ResolvedHandles resolve() {
        ResolvedHandles cached = handles;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (handles != null) {
                return handles;
            }
            ClassLoader cl = ReflectiveFriendEatingBridge.class.getClassLoader();
            Throwable lastError = null;
            for (String basePkg : PACKAGES) {
                try {
                    Class<?> cooldownCls = Class.forName(basePkg + ".manager.CooldownManager", false, cl);
                    Class<?> statsCls = Class.forName(basePkg + ".data.FeedStatsSavedData", false, cl);
                    Method canFeed = cooldownCls.getMethod("canFeed", UUID.class);
                    Method recordFeed = cooldownCls.getMethod("recordFeed", UUID.class);
                    Method getStats = statsCls.getMethod("get", ServerLevel.class);
                    Method incrementFed = statsCls.getMethod("incrementFedCount", UUID.class);
                    Method incrementEaten = statsCls.getMethod("incrementEatenCount", UUID.class);
                    handles = new ResolvedHandles(canFeed, recordFeed, getStats, incrementFed, incrementEaten);
                    return handles;
                } catch (Throwable t) {
                    lastError = t;
                }
            }
            throw new IllegalStateException(
                "Could not locate Let Your Friend Eating classes under me.tamkungz or th.in.tamkungz",
                lastError
            );
        }
    }

    private record ResolvedHandles(
        Method canFeed,
        Method recordFeed,
        Method getStats,
        Method incrementFedCount,
        Method incrementEatenCount
    ) {}
}
