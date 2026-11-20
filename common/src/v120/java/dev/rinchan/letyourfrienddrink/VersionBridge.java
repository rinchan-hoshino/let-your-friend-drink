package dev.rinchan.letyourfrienddrink;

import com.mojang.datafixers.util.Pair;
import dev.rinchan.letyourfrienddrink.config.ExclusionMatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionUtils;

/** Minecraft 1.20.1 version bridge. */
public final class VersionBridge {
    private static final TagKey<Item> BUILTIN_EXCLUDED_TAG = TagKey.create(
        Registries.ITEM,
        new ResourceLocation(LetYourFriendDrink.MOD_ID, "excluded")
    );

    private VersionBridge() {}

    public static boolean isDrink(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getUseAnimation() == UseAnim.DRINK;
    }

    public static boolean isFood(ItemStack stack, Player recipient) {
        return stack != null && !stack.isEmpty() && stack.isEdible();
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
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key != null && "minecraft:ominous_bottle".equals(key.toString())) {
            return true;
        }
        for (MobEffectInstance effect : PotionUtils.getMobEffects(stack)) {
            if (effect != null && effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                return true;
            }
        }
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("Effects", Tag.TAG_LIST)) {
            ListTag effects = tag.getList("Effects", Tag.TAG_COMPOUND);
            for (int i = 0; i < effects.size(); i++) {
                CompoundTag entry = effects.getCompound(i);
                MobEffect effect = MobEffect.byId(entry.getInt("EffectId"));
                if (effect != null && effect.getCategory() == MobEffectCategory.HARMFUL) {
                    return true;
                }
            }
        }
        FoodProperties food = stack.getItem().getFoodProperties();
        if (food != null) {
            for (Pair<MobEffectInstance, Float> pair : food.getEffects()) {
                if (pair.getFirst() != null
                    && pair.getFirst().getEffect().getCategory() == MobEffectCategory.HARMFUL) {
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
