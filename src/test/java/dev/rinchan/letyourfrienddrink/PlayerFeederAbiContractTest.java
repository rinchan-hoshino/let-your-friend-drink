package dev.rinchan.letyourfrienddrink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class PlayerFeederAbiContractTest {
    private static final String TARGET_CLASS =
        "th/in/tamkungz/letyourfriendeating/logic/PlayerFeeder.class";
    private static final String MIXIN_CLASS =
        "dev/rinchan/letyourfrienddrink/mixin/PlayerFeederMixin.class";
    private static final String TARGET_DESCRIPTOR =
        "(Lnet/minecraft/world/entity/player/Player;"
            + "Lnet/minecraft/world/entity/player/Player;"
            + "Lnet/minecraft/world/InteractionHand;)"
            + "Lnet/minecraft/world/InteractionResult;";
    private static final String WRAPPER_DESCRIPTOR =
        "(Lnet/minecraft/world/entity/player/Player;"
            + "Lnet/minecraft/world/entity/player/Player;"
            + "Lnet/minecraft/world/InteractionHand;"
            + "Lcom/llamalad7/mixinextras/injector/wrapoperation/Operation;)"
            + "Lnet/minecraft/world/InteractionResult;";

    @Test
    void shippedTargetAndWrapperUseTheSamePlayerFeederAbi() throws Exception {
        assertEquals(List.of(TARGET_DESCRIPTOR), descriptors(TARGET_CLASS, "tryFeedPlayer"));
        assertEquals(List.of(WRAPPER_DESCRIPTOR), descriptors(MIXIN_CLASS, "letYourFriendDrink$handleDrinkFirst"));
    }

    @Test
    void wrapperAttemptsDrinkHandlingBeforeCallingTheBaseMethod() throws Exception {
        List<String> calls = methodCalls(MIXIN_CLASS, "letYourFriendDrink$handleDrinkFirst");
        int drink = calls.indexOf("dev/rinchan/letyourfrienddrink/FriendDrinkFeeder.tryFeedDrink");
        int original = calls.indexOf("com/llamalad7/mixinextras/injector/wrapoperation/Operation.call");
        assertTrue(drink >= 0, calls.toString());
        assertTrue(original > drink, calls.toString());
    }

    @Test
    void processedMetadataCarriesTheEnglishIdentityAndReleaseVersion() throws Exception {
        InputStream stream = PlayerFeederAbiContractTest.class.getClassLoader()
            .getResourceAsStream("META-INF/neoforge.mods.toml");
        assertNotNull(stream);
        try (stream) {
            String metadata = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(metadata.contains("version=\"1.0.0\""));
            assertTrue(metadata.contains("displayName=\"Let Your Friend Drink\""));
            assertTrue(metadata.contains("modId=\"let_your_friend_drink\""));
        }
    }

    private static List<String> descriptors(String resource, String methodName) throws IOException {
        List<String> result = new ArrayList<>();
        visit(resource, new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(
                int access,
                String name,
                String descriptor,
                String signature,
                String[] exceptions
            ) {
                if (methodName.equals(name)) {
                    result.add(descriptor);
                }
                return null;
            }
        });
        return result;
    }

    private static List<String> methodCalls(String resource, String methodName) throws IOException {
        List<String> result = new ArrayList<>();
        visit(resource, new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(
                int access,
                String name,
                String descriptor,
                String signature,
                String[] exceptions
            ) {
                if (!methodName.equals(name)) {
                    return null;
                }
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(
                        int opcode,
                        String owner,
                        String name,
                        String descriptor,
                        boolean isInterface
                    ) {
                        result.add(owner + "." + name);
                    }
                };
            }
        });
        return result;
    }

    private static void visit(String resource, ClassVisitor visitor) throws IOException {
        InputStream stream = PlayerFeederAbiContractTest.class.getClassLoader().getResourceAsStream(resource);
        assertNotNull(stream, resource);
        try (stream) {
            new ClassReader(stream).accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
    }
}
