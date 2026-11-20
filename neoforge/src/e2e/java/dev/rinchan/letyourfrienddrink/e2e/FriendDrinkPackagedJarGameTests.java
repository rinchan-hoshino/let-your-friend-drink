package dev.rinchan.letyourfrienddrink.e2e;

import com.mojang.authlib.GameProfile;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

/**
 * Packaged-JAR black-box E2E GameTests running on real Minecraft 1.21.1 NeoForge with
 * real upstream Let Your Friend Eating + Cloth Config JARs.
 *
 * ZERO direct calls to internal mod classes: every feeding journey flows exclusively through
 * vanilla {@link ServerPlayer#interactOn(net.minecraft.world.entity.Entity, InteractionHand)} ->
 * NeoForge EventBus -> upstream PlayerFeedHandler/PlayerFeeder -> packaged-JAR Mixin.
 */
@GameTestHolder(FriendDrinkE2EMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FriendDrinkPackagedJarGameTests {
    private static final String EMPTY_TEMPLATE = "empty";

    @GameTest(template = EMPTY_TEMPLATE)
    public static void packagedJarAndUpstreamModsLoaded(GameTestHelper helper) {
        helper.assertTrue(ModList.get().isLoaded("let_your_friend_drink"), "let_your_friend_drink must be loaded");
        helper.assertTrue(ModList.get().isLoaded("letyourfriendeating"), "upstream letyourfriendeating must be loaded");
        helper.assertTrue(ModList.get().isLoaded("cloth_config"), "upstream cloth_config must be loaded");

        try {
            Class<?> modClass = Class.forName("dev.rinchan.letyourfrienddrink.LetYourFriendDrink");
            URL location = modClass.getProtectionDomain().getCodeSource().getLocation();
            String locStr = location != null ? location.toString() : "";
            helper.assertTrue(
                locStr.contains("let_your_friend_drink-neoforge-") && locStr.contains(".jar"),
                "let_your_friend_drink MUST be loaded from real packaged .jar, got: " + locStr
            );
        } catch (ClassNotFoundException e) {
            helper.fail("Failed to load packaged LetYourFriendDrink class: " + e.getMessage());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void harmfulPoisonPotionBlockedWithRedActionBar(GameTestHelper helper) {
        PlayerPair pair = PlayerPair.spawn(helper, GameType.SURVIVAL);
        ItemStack poison = PotionContents.createItemStack(Items.POTION, Potions.POISON);
        pair.feeder.setItemInHand(InteractionHand.MAIN_HAND, poison.copy());

        InteractionResult result = pair.feeder.interactOn(pair.recipient, InteractionHand.MAIN_HAND);

        helper.assertTrue(
            result != InteractionResult.SUCCESS,
            "Poison potion must be blocked when allowHarmfulDrinks=false, got: " + result
        );
        helper.assertFalse(
            pair.recipient.hasEffect(MobEffects.POISON),
            "Recipient must NOT receive Poison effect when blocked"
        );
        helper.assertTrue(
            ItemStack.isSameItemSameComponents(poison, pair.feeder.getItemInHand(InteractionHand.MAIN_HAND)),
            "Feeder must keep the Poison potion intact when blocked"
        );
        assertReceivedRedHarmfulBlockedActionBar(helper, pair.feederPackets);
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void harmfulOminousBottleBlockedWithRedActionBar(GameTestHelper helper) {
        PlayerPair pair = PlayerPair.spawn(helper, GameType.SURVIVAL);
        ItemStack ominous = new ItemStack(Items.OMINOUS_BOTTLE);
        pair.feeder.setItemInHand(InteractionHand.MAIN_HAND, ominous.copy());

        InteractionResult result = pair.feeder.interactOn(pair.recipient, InteractionHand.MAIN_HAND);

        helper.assertTrue(
            result != InteractionResult.SUCCESS,
            "Ominous bottle must be blocked when allowHarmfulDrinks=false, got: " + result
        );
        helper.assertFalse(
            pair.recipient.hasEffect(MobEffects.BAD_OMEN),
            "Recipient must NOT receive Bad Omen effect when blocked"
        );
        helper.assertTrue(
            pair.feeder.getItemInHand(InteractionHand.MAIN_HAND).is(Items.OMINOUS_BOTTLE),
            "Feeder must keep the Ominous Bottle intact when blocked"
        );
        assertReceivedRedHarmfulBlockedActionBar(helper, pair.feederPackets);
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void regenerationPotionFeedsAndReturnsEmptyGlassBottle(GameTestHelper helper) {
        PlayerPair pair = PlayerPair.spawn(helper, GameType.SURVIVAL);
        ItemStack regen = PotionContents.createItemStack(Items.POTION, Potions.REGENERATION);
        pair.feeder.setItemInHand(InteractionHand.MAIN_HAND, regen);

        InteractionResult result = pair.feeder.interactOn(pair.recipient, InteractionHand.MAIN_HAND);

        helper.assertTrue(
            result == InteractionResult.SUCCESS,
            "Regeneration potion feeding must succeed, got: " + result
        );
        helper.assertTrue(
            pair.recipient.hasEffect(MobEffects.REGENERATION),
            "Recipient must receive Regeneration buff"
        );
        ItemStack heldAfter = pair.feeder.getItemInHand(InteractionHand.MAIN_HAND);
        helper.assertTrue(
            heldAfter.is(Items.GLASS_BOTTLE) && heldAfter.getCount() == 1,
            "Survival feeder must receive empty glass bottle, got: " + heldAfter
        );
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void honeyBottleFeedsCuresPoisonAndReturnsGlassBottle(GameTestHelper helper) {
        PlayerPair pair = PlayerPair.spawn(helper, GameType.SURVIVAL);
        pair.recipient.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
        pair.recipient.getFoodData().setFoodLevel(10);
        pair.feeder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.HONEY_BOTTLE));

        InteractionResult result = pair.feeder.interactOn(pair.recipient, InteractionHand.MAIN_HAND);

        helper.assertTrue(
            result == InteractionResult.SUCCESS,
            "Honey bottle feeding must succeed, got: " + result
        );
        helper.assertFalse(
            pair.recipient.hasEffect(MobEffects.POISON),
            "Honey bottle must cure recipient's Poison effect"
        );
        helper.assertTrue(
            pair.recipient.getFoodData().getFoodLevel() > 10,
            "Honey bottle must restore recipient's hunger"
        );
        ItemStack heldAfter = pair.feeder.getItemInHand(InteractionHand.MAIN_HAND);
        helper.assertTrue(
            heldAfter.is(Items.GLASS_BOTTLE) && heldAfter.getCount() == 1,
            "Survival feeder must receive empty glass bottle after honey bottle, got: " + heldAfter
        );
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void creativeModeDrinkDoesNotConsumeItem(GameTestHelper helper) {
        PlayerPair pair = PlayerPair.spawn(helper, GameType.CREATIVE);
        ItemStack regen = PotionContents.createItemStack(Items.POTION, Potions.REGENERATION);
        pair.feeder.setItemInHand(InteractionHand.MAIN_HAND, regen.copy());

        InteractionResult result = pair.feeder.interactOn(pair.recipient, InteractionHand.MAIN_HAND);

        helper.assertTrue(
            result == InteractionResult.SUCCESS,
            "Creative mode potion feeding must succeed, got: " + result
        );
        helper.assertTrue(
            pair.recipient.hasEffect(MobEffects.REGENERATION),
            "Recipient must receive Regeneration buff from creative feeder"
        );
        ItemStack heldAfter = pair.feeder.getItemInHand(InteractionHand.MAIN_HAND);
        helper.assertTrue(
            ItemStack.isSameItemSameComponents(regen, heldAfter) && heldAfter.getCount() == 1,
            "Creative feeder must NOT consume potion or replace with glass bottle, got: " + heldAfter
        );
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void fullHungerGoldenAppleAndBreadFeedSafely(GameTestHelper helper) {
        // Case 1: Golden Apple at full hunger (foodLevel == 20)
        PlayerPair gapplePair = PlayerPair.spawn(helper, GameType.SURVIVAL);
        gapplePair.recipient.getFoodData().setFoodLevel(20);
        gapplePair.recipient.getFoodData().setSaturation(5.0F);
        gapplePair.feeder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GOLDEN_APPLE, 2));

        InteractionResult gappleResult = gapplePair.feeder.interactOn(gapplePair.recipient, InteractionHand.MAIN_HAND);
        helper.assertTrue(
            gappleResult == InteractionResult.SUCCESS,
            "Full-hunger Golden Apple feeding must succeed, got: " + gappleResult
        );
        helper.assertTrue(
            gapplePair.feeder.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1,
            "Golden Apple count must decrement from 2 to 1"
        );
        helper.assertTrue(
            gapplePair.recipient.hasEffect(MobEffects.REGENERATION)
                && gapplePair.recipient.hasEffect(MobEffects.ABSORPTION),
            "Recipient must receive Golden Apple buffs (Regeneration + Absorption)"
        );
        int gappleFoodAfter = gapplePair.recipient.getFoodData().getFoodLevel();
        helper.assertTrue(
            gappleFoodAfter == 20,
            "Recipient foodLevel after Golden Apple at full hunger must remain legal 20, got: " + gappleFoodAfter
        );

        // Case 2: Bread at full hunger (foodLevel == 20)
        PlayerPair breadPair = PlayerPair.spawn(helper, GameType.SURVIVAL);
        breadPair.recipient.getFoodData().setFoodLevel(20);
        breadPair.recipient.getFoodData().setSaturation(5.0F);
        breadPair.feeder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD, 2));

        InteractionResult breadResult = breadPair.feeder.interactOn(breadPair.recipient, InteractionHand.MAIN_HAND);
        helper.assertTrue(
            breadResult == InteractionResult.SUCCESS,
            "Full-hunger Bread feeding must succeed via FullHungerBridge, got: " + breadResult
        );
        helper.assertTrue(
            breadPair.feeder.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1,
            "Bread count must decrement from 2 to 1"
        );
        int breadFoodAfter = breadPair.recipient.getFoodData().getFoodLevel();
        helper.assertTrue(
            breadFoodAfter == 20,
            "Recipient foodLevel after Bread at full hunger must remain legal 20, got: " + breadFoodAfter
        );
        helper.succeed();
    }

    private static void assertReceivedRedHarmfulBlockedActionBar(GameTestHelper helper, List<Packet<?>> packets) {
        for (Packet<?> packet : packets) {
            Component msg = null;
            if (packet instanceof ClientboundSystemChatPacket sys && sys.overlay()) {
                msg = sys.content();
            } else if (packet instanceof ClientboundSetActionBarTextPacket bar) {
                msg = bar.text();
            }
            if (msg != null) {
                boolean isRed = msg.getStyle().getColor() != null
                    && msg.getStyle().getColor().getValue() == ChatFormatting.RED.getColor();
                boolean matchesKey = msg.getContents() instanceof TranslatableContents tc
                    && "message.let_your_friend_drink.harmful_blocked".equals(tc.getKey());
                if (isRed && matchesKey) {
                    return;
                }
            }
        }
        helper.fail(
            "Expected red actionbar packet with key 'message.let_your_friend_drink.harmful_blocked', captured: "
                + packets.size() + " packets"
        );
    }

    private record PlayerPair(ServerPlayer feeder, ServerPlayer recipient, List<Packet<?>> feederPackets) {
        static PlayerPair spawn(GameTestHelper helper, GameType feederGameType) {
            ServerLevel level = helper.getLevel();
            MinecraftServer server = level.getServer();
            Vec3 base = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));

            List<Packet<?>> feederPackets = new ArrayList<>();
            ServerPlayer feeder = createPlayer(server, level, "Feeder", base, feederGameType, feederPackets);
            ServerPlayer recipient = createPlayer(
                server, level, "Recipient", base.add(1.0, 0.0, 0.0), GameType.SURVIVAL, new ArrayList<>()
            );
            return new PlayerPair(feeder, recipient, feederPackets);
        }

        private static ServerPlayer createPlayer(
            MinecraftServer server,
            ServerLevel level,
            String namePrefix,
            Vec3 pos,
            GameType gameType,
            List<Packet<?>> packetSink
        ) {
            UUID uuid = UUID.randomUUID();
            String shortName = namePrefix + "_" + uuid.toString().substring(0, 6);
            GameProfile profile = new GameProfile(uuid, shortName);
            ServerPlayer player = new ServerPlayer(server, level, profile, ClientInformation.createDefault());
            player.setPos(pos.x, pos.y, pos.z);
            Connection conn = new Connection(PacketFlow.SERVERBOUND);
            CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
            player.connection = new ServerGamePacketListenerImpl(server, conn, player, cookie) {
                @Override
                public void send(Packet<?> packet) {
                    packetSink.add(packet);
                }

                @Override
                public void send(Packet<?> packet, @Nullable PacketSendListener listener) {
                    packetSink.add(packet);
                }
            };
            player.setGameMode(gameType);
            return player;
        }
    }
}
