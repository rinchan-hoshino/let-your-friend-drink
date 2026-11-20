package dev.rinchan.letyourfrienddrink.client;

import dev.rinchan.letyourfrienddrink.config.FriendDrinkConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Native zero-dependency configuration screen using vanilla Minecraft 1.21.1 widgets. */
public final class FriendDrinkConfigScreen extends Screen {
    private final Screen parent;
    private boolean allowHarmfulDrinks;
    private boolean allowFeedingAtFullHunger;
    private String excludedItemsCsv;
    private EditBox excludedItemsBox;
    private int labelX;
    private int labelY;

    public FriendDrinkConfigScreen(Screen parent) {
        super(Component.translatable("screen.let_your_friend_drink.title"));
        this.parent = parent;
        loadFrom(FriendDrinkConfig.get());
    }

    private void loadFrom(FriendDrinkConfig config) {
        this.allowHarmfulDrinks = config.allowHarmfulDrinks();
        this.allowFeedingAtFullHunger = config.allowFeedingAtFullHunger();
        this.excludedItemsCsv = config.formatExcludedItemsCsv();
    }

    @Override
    protected void init() {
        int totalW = Math.min(320, this.width - 24);
        int left = (this.width - totalW) / 2;
        int y = 40;

        this.addRenderableWidget(CycleButton.onOffBuilder(this.allowHarmfulDrinks)
            .create(left, y, totalW, 20,
                Component.translatable("screen.let_your_friend_drink.allow_harmful_drinks"),
                (btn, value) -> this.allowHarmfulDrinks = value));
        y += 28;

        this.addRenderableWidget(CycleButton.onOffBuilder(this.allowFeedingAtFullHunger)
            .create(left, y, totalW, 20,
                Component.translatable("screen.let_your_friend_drink.allow_feeding_at_full_hunger"),
                (btn, value) -> this.allowFeedingAtFullHunger = value));
        y += 32;

        this.labelX = left;
        this.labelY = y;
        y += 14;

        this.excludedItemsBox = new EditBox(
            this.font, left, y, totalW, 20,
            Component.translatable("screen.let_your_friend_drink.excluded_items")
        );
        this.excludedItemsBox.setMaxLength(512);
        this.excludedItemsBox.setValue(this.excludedItemsCsv);
        this.excludedItemsBox.setResponder(v -> this.excludedItemsCsv = v);
        this.addRenderableWidget(this.excludedItemsBox);

        int btnY = Math.min(this.height - 28, y + 38);
        int btnW = (totalW - 12) / 3;
        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.let_your_friend_drink.reset"),
                btn -> {
                    loadFrom(FriendDrinkConfig.defaults());
                    this.rebuildWidgets();
                })
            .bounds(left, btnY, btnW, 20)
            .build());
        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.let_your_friend_drink.cancel"),
                btn -> onClose())
            .bounds(left + btnW + 6, btnY, btnW, 20)
            .build());
        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.let_your_friend_drink.save"),
                btn -> saveAndClose())
            .bounds(left + (btnW + 6) * 2, btnY, totalW - (btnW + 6) * 2, 20)
            .build());
    }

    private void saveAndClose() {
        String csv = this.excludedItemsBox != null ? this.excludedItemsBox.getValue() : this.excludedItemsCsv;
        FriendDrinkConfig updated = new FriendDrinkConfig(
            this.allowHarmfulDrinks,
            this.allowFeedingAtFullHunger,
            FriendDrinkConfig.parseCommaSeparatedEntries(csv)
        );
        FriendDrinkConfig.updateAndSave(updated);
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFF);
        graphics.drawString(
            this.font,
            Component.translatable("screen.let_your_friend_drink.excluded_items"),
            this.labelX, this.labelY, 0xCCCCCC
        );
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
