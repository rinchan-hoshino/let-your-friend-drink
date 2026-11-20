package dev.rinchan.letyourfrienddrink;

import dev.rinchan.letyourfrienddrink.config.ExclusionMatcher;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;

/** Minecraft 26.x version bridge. */
public final class VersionBridge {
    private static final TagKey<Item> BUILTIN_EXCLUDED_TAG = TagKey.create(
        Registries.ITEM,
        Identifier.fromNamespaceAndPath(LetYourFriendDrink.MOD_ID, "excluded")
    );

    private VersionBridge() {}

    public static boolean isDrink(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getUseAnimation() == ItemUseAnimation.DRINK;
    }

    public static boolean isFood(ItemStack stack, Player recipient) {
        return stack != null && !stack.isEmpty() && stack.has(DataComponents.FOOD);
    }

    public static boolean isExcluded(ItemStack stack, ExclusionMatcher matcher) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.is(BUILTIN_EXCLUDED_TAG)) return true;
        if (matcher == null || (matcher.itemIds().isEmpty() && matcher.tagIds().isEmpty())) {
            return false;
        }
        Identifier key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String itemId = key != null ? key.toString() : null;
        return matcher.matches(itemId, tagId -> {
            Identifier loc = Identifier.tryParse(tagId);
            return loc != null && stack.is(TagKey.create(Registries.ITEM, loc));
        });
    }

    public static boolean containsHarmfulEffect(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.is(Items.OMINOUS_BOTTLE) || stack.has(DataComponents.OMINOUS_BOTTLE_AMPLIFIER)) {
            return true;
        }
        PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);
        if (potion != null) {
            for (MobEffectInstance effect : potion.getAllEffects()) {
                if (effect != null && effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                    return true;
                }
            }
        }
        SuspiciousStewEffects stew = stack.get(DataComponents.SUSPICIOUS_STEW_EFFECTS);
        if (stew != null) {
            for (SuspiciousStewEffects.Entry entry : stew.effects()) {
                if (entry.effect().value().getCategory() == MobEffectCategory.HARMFUL) {
                    return true;
                }
            }
        }
        Consumable consumable = stack.get(DataComponents.CONSUMABLE);
        if (consumable != null) {
            for (ConsumeEffect ce : consumable.onConsumeEffects()) {
                if (ce instanceof ApplyStatusEffectsConsumeEffect apply) {
                    for (MobEffectInstance effect : apply.effects()) {
                        if (effect != null && effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static void playDrinkSound(ServerLevel level, Player recipient, ItemStack original) {
        Consumable consumable = original.get(DataComponents.CONSUMABLE);
        level.playSound(
            null,
            recipient.getX(), recipient.getY(), recipient.getZ(),
            consumable != null ? consumable.sound().value() : SoundEvents.GENERIC_DRINK.value(),
            SoundSource.PLAYERS, 1.0F, 1.0F
        );
    }

    public static void sendActionBar(Player player, Component message) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(message);
        }
    }

    public static boolean consumesAction(InteractionResult result) {
        return result != null && result.consumesAction();
    }
}
