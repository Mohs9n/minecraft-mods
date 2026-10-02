package com.simpleclaims.client;

import com.simpleclaims.network.SubmitClaimNamePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Fully custom-rendered claim-naming screen (no vanilla anvil/dialog reuse): a dark panel
 * with gold corner accents, a real text field, a live character counter, a validation
 * hint, and Create/Cancel buttons. Submitting sends {@link SubmitClaimNamePayload} to the
 * server, which re-validates and creates the claim from the player's current wand
 * selection - this screen never touches an item or inventory slot at all.
 */
public class ClaimNameScreen extends Screen {

    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 190;
    private static final int GOLD = 0xFFD4AF37;
    private static final int GOLD_DIM = 0xFF8A6D1A;
    private static final int PANEL_BG = 0xF0121212;
    private static final int PANEL_BORDER = 0xFF3A3A3A;
    private static final int MIN_LEN = 3;
    private static final int MAX_LEN = 16;

    private int panelX;
    private int panelY;
    private EditBox nameBox;
    private Button createButton;
    private boolean submitted = false;

    public ClaimNameScreen() {
        super(Component.literal("Name Your Claim"));
    }

    @Override
    protected void init() {
        this.panelX = (this.width - PANEL_WIDTH) / 2;
        this.panelY = (this.height - PANEL_HEIGHT) / 2;

        int boxWidth = PANEL_WIDTH - 40;
        int boxX = panelX + 20;
        int boxY = panelY + 78;

        this.nameBox = new EditBox(this.font, boxX, boxY, boxWidth, 20, Component.literal("Claim name"));
        this.nameBox.setMaxLength(MAX_LEN);
        this.nameBox.setValue("MyClaim");
        this.nameBox.setResponder(value -> updateValidity());
        addRenderableWidget(this.nameBox);
        setInitialFocus(this.nameBox);

        int buttonY = panelY + PANEL_HEIGHT - 32;
        this.createButton = addRenderableWidget(new Button.Builder(Component.literal("§a§lCreate Claim"), btn -> trySubmit())
                .bounds(panelX + 20, buttonY, 140, 20)
                .build());
        addRenderableWidget(new Button.Builder(Component.literal("§cCancel"), btn -> onClose())
                .bounds(panelX + PANEL_WIDTH - 20 - 100, buttonY, 100, 20)
                .build());

        updateValidity();
    }

    private static boolean isValidName(String value) {
        return value != null && value.length() >= MIN_LEN && value.length() <= MAX_LEN && value.matches("[A-Za-z0-9]+");
    }

    private void updateValidity() {
        if (createButton != null) {
            createButton.active = isValidName(nameBox.getValue());
        }
    }

    private void trySubmit() {
        String value = nameBox.getValue();
        if (!isValidName(value) || submitted) {
            return;
        }
        submitted = true;
        ClientPlayNetworking.send(new SubmitClaimNamePayload(value));
        onClose();
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (nameBox != null && nameBox.isFocused() && isEnterKey(event)) {
            trySubmit();
            return true;
        }
        return super.keyPressed(event);
    }

    private static boolean isEnterKey(net.minecraft.client.input.KeyEvent event) {
        return event.key() == 257 || event.key() == 335; // GLFW_KEY_ENTER / KP_ENTER
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Panel background + border
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, PANEL_BG);
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + 1, PANEL_BORDER);
        graphics.fill(panelX, panelY + PANEL_HEIGHT - 1, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, PANEL_BORDER);
        graphics.fill(panelX, panelY, panelX + 1, panelY + PANEL_HEIGHT, PANEL_BORDER);
        graphics.fill(panelX + PANEL_WIDTH - 1, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, PANEL_BORDER);
        drawCorner(graphics, panelX, panelY, 1, 1);
        drawCorner(graphics, panelX + PANEL_WIDTH, panelY, -1, 1);
        drawCorner(graphics, panelX, panelY + PANEL_HEIGHT, 1, -1);
        drawCorner(graphics, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, -1, -1);

        // Title
        graphics.centeredText(this.font, "§6§l♦ CREATE YOUR CLAIM ♦", panelX + PANEL_WIDTH / 2, panelY + 12, GOLD);
        graphics.fill(panelX + 16, panelY + 28, panelX + PANEL_WIDTH - 16, panelY + 29, GOLD_DIM);
        graphics.centeredText(this.font, "§7Choose a unique name for your claim", panelX + PANEL_WIDTH / 2, panelY + 38, 0xFFAAAAAA);

        // Field label
        graphics.text(this.font, "§fName", panelX + 20, panelY + 64, 0xFFFFFFFF);

        boolean valid = isValidName(nameBox.getValue());
        String counter = nameBox.getValue().length() + "/" + MAX_LEN;
        graphics.text(this.font, (valid ? "§7" : "§c") + counter,
                panelX + PANEL_WIDTH - 20 - this.font.width(counter), panelY + 64, valid ? 0xFFAAAAAA : 0xFFFF5555);

        String hint = valid ? "✔ Letters and numbers only • 3-16 characters" : "✖ Letters and numbers only • 3-16 characters";
        graphics.text(this.font, (valid ? "§a" : "§c") + hint, panelX + 20, panelY + 104, valid ? 0xFF55FF55 : 0xFFFF5555);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawCorner(GuiGraphicsExtractor graphics, int x, int y, int dirX, int dirY) {
        int len = 10;
        int thick = 2;
        int startX = dirX > 0 ? x : x - len;
        int startY = dirY > 0 ? y : y - thick;
        graphics.fill(startX, startY, startX + len, startY + thick, GOLD);
        int startX2 = dirX > 0 ? x : x - thick;
        int startY2 = dirY > 0 ? y : y - len;
        graphics.fill(startX2, startY2, startX2 + thick, startY2 + len, GOLD);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
