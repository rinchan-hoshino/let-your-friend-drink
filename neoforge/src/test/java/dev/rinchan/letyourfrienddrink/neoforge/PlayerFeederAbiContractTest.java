package dev.rinchan.letyourfrienddrink.neoforge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import th.in.tamkungz.letyourfriendeating.logic.PlayerFeeder;

final class PlayerFeederAbiContractTest {
    private static final String MIXIN_CLASS =
        "dev/rinchan/letyourfrienddrink/neoforge/mixin/PlayerFeederMixin.class";
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
    void public_addon_wraps_the_installed_player_feeder_descriptor() throws IOException {
        ClassNode target = new ClassNode();
        new ClassReader(PlayerFeeder.class.getName()).accept(target, ClassReader.SKIP_CODE);
        List<MethodNode> targetMethods = target.methods.stream()
            .filter(method -> method.name.equals("tryFeedPlayer"))
            .toList();
        assertEquals(1, targetMethods.size());
        assertEquals(TARGET_DESCRIPTOR, targetMethods.getFirst().desc);

        ClassNode mixin = classNode(MIXIN_CLASS);
        assertEquals(
            List.of("th.in.tamkungz.letyourfriendeating.logic.PlayerFeeder"),
            annotationValue(mixin.invisibleAnnotations, "Lorg/spongepowered/asm/mixin/Mixin;", "targets")
        );
        assertEquals(
            Boolean.FALSE,
            annotationValue(mixin.invisibleAnnotations, "Lorg/spongepowered/asm/mixin/Mixin;", "remap")
        );
        MethodNode wrapper = mixin.methods.stream()
            .filter(method -> method.name.equals("letYourFriendDrink$handleDrink"))
            .findFirst().orElseThrow();
        assertEquals(WRAPPER_DESCRIPTOR, wrapper.desc);
        assertEquals(List.of("tryFeedPlayer"), annotationValue(
            wrapper.visibleAnnotations,
            "Lcom/llamalad7/mixinextras/injector/wrapmethod/WrapMethod;",
            "method"
        ));
        assertEquals(Boolean.FALSE, annotationValue(
            wrapper.visibleAnnotations,
            "Lcom/llamalad7/mixinextras/injector/wrapmethod/WrapMethod;",
            "remap"
        ));
        boolean callsPublicOwner = false;
        for (AbstractInsnNode instruction : wrapper.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals("dev/rinchan/letyourfrienddrink/FriendDrinkFeeder")
                    && call.name.equals("tryFeedDrink")
                    && call.desc.equals(TARGET_DESCRIPTOR)) {
                callsPublicOwner = true;
            }
        }
        assertTrue(callsPublicOwner);
    }

    private static ClassNode classNode(String resource) throws IOException {
        try (InputStream stream = PlayerFeederAbiContractTest.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) throw new IllegalStateException("Missing class resource " + resource);
            ClassNode node = new ClassNode();
            new ClassReader(stream).accept(node, 0);
            return node;
        }
    }

    private static Object annotationValue(List<AnnotationNode> annotations, String descriptor, String key) {
        AnnotationNode annotation = annotations.stream()
            .filter(candidate -> candidate.desc.equals(descriptor))
            .findFirst().orElseThrow();
        for (int index = 0; index < annotation.values.size(); index += 2) {
            if (annotation.values.get(index).equals(key)) return annotation.values.get(index + 1);
        }
        throw new IllegalStateException("Missing annotation value " + key);
    }
}
