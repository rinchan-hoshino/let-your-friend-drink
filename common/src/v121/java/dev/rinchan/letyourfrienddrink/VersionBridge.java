package dev.rinchan.letyourfrienddrink;

import dev.rinchan.letyourfrienddrink.config.ExclusionMatcher;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.SuspiciousStewEffects;

/** Minecraft 1.21.1 version bridge. */
public final class VersionBridge {
    private static final TagKey<Item> BUILTIN_EXCLUDED_TAG = TagKey.create(
        Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(LetYourFriendDrink.MOD_ID, "excluded")
    );

    private VersionBridge() {}

    public static boolean isDrink(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getUseAnimation() == UseAnim.DRINK;
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
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String itemId = key != null ? key.toString() : null;
        return matcher.matches(itemId, tagId -> {
            ResourceLocation loc = ResourceLocation.tryParse(tagId);
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
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food != null) {
            for (FoodProperties.PossibleEffect entry : food.effects()) {
                if (entry.effect().getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void playDrinkSound(ServerLevel level, Player recipient, ItemStack original) {
        recipient.playNotifySound(original.getDrinkingSound(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    public static void sendActionBar(Player player, Component message) {
        player.displayClientMessage(message, true);
    }

    public static boolean consumesAction(InteractionResult result) {
        return result != null && result.consumesAction();
    }
}
