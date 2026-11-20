package dev.rinchan.letyourfrienddrink.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Path;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Packaged-JAR client Screen lifecycle black-box E2E test running against the real
 * Minecraft 1.21.1 runtime and the final packaged let_your_friend_drink NeoForge JAR.
 */
final class FriendDrinkConfigScreenE2ETest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
    }

    @Test
    void configScreenInitializesAndBuildsAllWidgetsWithZeroExceptions(@TempDir Path configDir) throws Exception {
        Class<?> modClass = Class.forName("dev.rinchan.letyourfrienddrink.LetYourFriendDrink");
        URL modLoc = modClass.getProtectionDomain().getCodeSource().getLocation();
        assertTrue(
            modLoc != null && modLoc.toString().contains("let_your_friend_drink-neoforge-") && modLoc.toString().endsWith(".jar"),
            "LetYourFriendDrink must be loaded from real packaged .jar, got: " + modLoc
        );
        modClass.getMethod("init", Path.class).invoke(null, configDir);

        Class<?> screenClass = Class.forName("dev.rinchan.letyourfrienddrink.client.FriendDrinkConfigScreen");
        URL screenLoc = screenClass.getProtectionDomain().getCodeSource().getLocation();
        assertTrue(
            screenLoc != null && screenLoc.toString().contains("let_your_friend_drink-neoforge-") && screenLoc.toString().endsWith(".jar"),
            "FriendDrinkConfigScreen must be loaded from real packaged .jar, got: " + screenLoc
        );

        Screen screen = (Screen) screenClass.getConstructor(Screen.class).newInstance((Object) null);
        screen.width = 854;
        screen.height = 480;

        Font headlessFont = new Font(id -> null, false);
        Field fontField = Screen.class.getDeclaredField("font");
        fontField.setAccessible(true);
        fontField.set(screen, headlessFont);

        Method initMethod = screenClass.getDeclaredMethod("init");
        initMethod.setAccessible(true);
        initMethod.invoke(screen);

        assertEquals(6, screen.children().size(), "Expected 6 widgets (2 CycleButtons, 1 EditBox, 3 Buttons)");
        long cycleButtons = screen.children().stream().filter(CycleButton.class::isInstance).count();
        long editBoxes = screen.children().stream().filter(EditBox.class::isInstance).count();
        long buttons = screen.children().stream().filter(w -> w.getClass() == Button.class || (w instanceof Button && !(w instanceof CycleButton))).count();

        assertEquals(2, cycleButtons, "Expected 2 CycleButton toggles");
        assertEquals(1, editBoxes, "Expected 1 EditBox for excludedItems");
        assertEquals(3, buttons, "Expected 3 action Buttons (Reset, Cancel, Save)");

        // Exercise onClose() cleanly with null minecraft reference
        screen.onClose();
    }
}
