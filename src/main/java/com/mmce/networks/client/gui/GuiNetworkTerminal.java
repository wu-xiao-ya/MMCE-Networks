package com.mmce.networks.client.gui;

import com.mmce.networks.MMCENetworksMod;
import com.mmce.networks.common.config.MMCENetworksConfig;
import com.mmce.networks.common.data.NetworkValueDisplayRegistry.ValueDisplaySpec;
import com.mmce.networks.common.network.MessageRenameNetwork;
import com.mmce.networks.common.network.MessageUpdateNetworkStyle;
import com.mmce.networks.common.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GuiNetworkTerminal extends GuiScreen {
    private static final float GUI_SCALE = 1.5F;
    private static final int PANEL_WIDTH = 219;
    private static final int PANEL_HEIGHT = 146;
    private static final int SIDEBAR_WIDTH = 82;
    private static final int CONTENT_EXPANDED_LEFT = 82;
    private static final int CONTENT_COMPACT_LEFT = 29;
    private static final int CONTENT_RIGHT = 205;
    private static final int RENAME_FIELD_OFFSET_X = 23;
    private static final int RENAME_FIELD_Y = 21;
    private static final int RENAME_SAVE_WIDTH = 20;
    private static final int RENAME_SAVE_HEIGHT = 12;
    private static final int REFRESH_WIDTH = 30;
    private static final int REFRESH_HEIGHT = 12;
    private static final int EXPANDED_NETWORK_ROW_COUNT = 5;
    private static final int COMPACT_NETWORK_ROW_COUNT = 8;
    private static final int EXPANDED_ROW_HEIGHT = 18;
    private static final int EXPANDED_ROW_GAP = 0;
    private static final int COMPACT_ROW_HEIGHT = 14;
    private static final int COMPACT_ROW_GAP = 0;
    private static final int EXPANDED_TOGGLE_X = 7;
    private static final int COMPACT_TOGGLE_X = 7;
    private static final int TOGGLE_Y = 7;
    private static final int EXPANDED_NETWORK_X = 8;
    private static final int EXPANDED_NETWORK_Y = 28;
    private static final int EXPANDED_NETWORK_WIDTH = 68;
    private static final int EXPANDED_NETWORK_HEIGHT = 108;
    private static final int EXPANDED_BUTTON_X = 60;
    private static final int EXPANDED_BUTTON_WIDTH = 8;
    private static final int EXPANDED_BUTTON_HEIGHT = 7;
    private static final int EXPANDED_PIN_BUTTON_Y = 2;
    private static final int EXPANDED_COLOR_BUTTON_Y = 10;
    private static final int EXPANDED_NAME_X = 4;
    private static final int EXPANDED_NAME_Y = 5;
    private static final int EXPANDED_NAME_WIDTH = EXPANDED_BUTTON_X - EXPANDED_NAME_X - 2;
    private static final int COMPACT_NETWORK_X = 7;
    private static final int COMPACT_NETWORK_Y = 25;
    private static final int COMPACT_NETWORK_WIDTH = 16;
    private static final int COMPACT_NETWORK_HEIGHT = 14;
    private static final int COMPACT_ICON_X_OFFSET = 2;
    private static final int COMPACT_ICON_Y_OFFSET = 1;
    private static final int COMPACT_ICON_WIDTH = 12;
    private static final int COMPACT_ICON_HEIGHT = 13;
    private static final int COMPACT_PIN_ICON_X_OFFSET = 4;
    private static final int COMPACT_PIN_ICON_Y_OFFSET = 3;
    private static final int COMPACT_PIN_ICON_WIDTH = 8;
    private static final int COMPACT_PIN_ICON_HEIGHT = 9;
    private static final int COLOR_PANEL_WIDTH = 47;
    private static final int COLOR_PANEL_HEIGHT = 49;
    private static final int ICON_ATLAS_COLUMNS = 4;
    private static final int COLOR_ICON_ATLAS_WIDTH = 35;
    private static final int COLOR_ICON_ATLAS_HEIGHT = 39;
    private static final int COLOR_ICON_CELL_WIDTH = 8;
    private static final int COLOR_ICON_CELL_HEIGHT = 9;
    private static final int OPTION_ICON_ATLAS_WIDTH = 51;
    private static final int OPTION_ICON_ATLAS_HEIGHT = 55;
    private static final int OPTION_ICON_CELL_WIDTH = 12;
    private static final int OPTION_ICON_CELL_HEIGHT = 13;
    private static final int ICON_ATLAS_GAP = 1;
    private static final int COLOR_PANEL_SWATCH_X = 3;
    private static final int COLOR_PANEL_SWATCH_Y = 3;
    private static final int COLOR_PANEL_SWATCH_SIZE = 8;
    private static final int COLOR_PANEL_SWATCH_STRIDE = 11;

    private static final ResourceLocation SIDEBAR_EXPANDED = texture("sidebar_expanded_bg.png");
    private static final ResourceLocation SIDEBAR_COMPACT = texture("sidebar_compact_bg.png");
    private static final ResourceLocation PIN_OFF_ICONS = texture("pin_off_icons.png");
    private static final ResourceLocation PIN_ON_ICONS = texture("pin_on_icons.png");
    private static final ResourceLocation COLOR_BUTTON_ICONS = texture("color_button_icons.png");
    private static final ResourceLocation NETWORK_OPTION_ICONS = texture("network_option_icons.png");
    private static final ResourceLocation COLOR_PANEL = texture("color_panel.png");
    private static final ResourceLocation TOGGLE_EXPANDED_IDLE = texture("toggle_expanded_0.png");
    private static final ResourceLocation TOGGLE_EXPANDED_HOVER = texture("toggle_expanded_1.png");
    private static final ResourceLocation TOGGLE_EXPANDED_ACTIVE = texture("toggle_expanded_2.png");
    private static final ResourceLocation TOGGLE_COMPACT_IDLE = texture("toggle_compact_0.png");
    private static final ResourceLocation TOGGLE_COMPACT_HOVER = texture("toggle_compact_1.png");
    private static final ResourceLocation TOGGLE_COMPACT_ACTIVE = texture("toggle_compact_2.png");

    private static final int[] PALETTE = {
        0xFFAA212B, 0xFF6E4A12, 0xFFD9782F, 0xFFFFCF40,
        0xFF4EC04E, 0xFF079B6B, 0xFF22B0AE, 0xFF69B9FF,
        0xFF337FF0, 0xFF6E5CB8, 0xFFC15189, 0xFFD86EAA,
        0xFFC6C6C6, 0xFF7E7E7E, 0xFF4F4F4F, 0xFF131313
    };

    private final String initialNetworkId;
    private GuiTextField renameField;
    private int networkScrollOffset;
    private int valueScrollOffset;
    private int lastRevision = -1;
    private String renameNetworkId = "";
    private boolean sidebarExpanded = true;
    private boolean togglePressed;
    private String colorPanelNetworkId = "";
    private int colorPanelX = 25;
    private int colorPanelY = 32;

    public GuiNetworkTerminal(final String networkId) {
        this.initialNetworkId = networkId == null ? "" : networkId;
    }

    public static void open(final String networkId) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null) {
            minecraft.displayGuiScreen(new GuiNetworkTerminal(networkId));
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();
        renameField = new GuiTextField(0, fontRenderer, 0, 0, 0, scale(12));
        renameField.setMaxStringLength(32);
        renameField.setEnableBackgroundDrawing(false);
        renameField.setTextColor(0xFFE8D8B7);
        updateRenameFieldBounds();
        syncRenameField();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        MMCENetworksConfig.reloadIfChanged();
        if (renameField != null) {
            renameField.updateCursorCounter();
        }
        int revision = NetworkTerminalClientState.getStateRevision();
        if (revision != lastRevision) {
            lastRevision = revision;
            syncRenameField();
            clampScroll();
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int delta = getMouseWheelDelta();
        if (delta == 0) {
            return;
        }

        int mouseX = getScaledMouseX();
        int mouseY = getScaledMouseY();
        int localX = toLocalX(mouseX);
        int localY = toLocalY(mouseY);
        if (isInside(localX, localY, getNetworkListX(), getNetworkListY(), getNetworkListWidth(), getNetworkListHeight())) {
            int max = Math.max(0, getNetworks().size() - getNetworkRowCount());
            networkScrollOffset = clamp(networkScrollOffset + (delta > 0 ? -1 : 1), 0, max);
            return;
        }

        int max = Math.max(0, collectDisplayCards().size() - 5);
        valueScrollOffset = clamp(valueScrollOffset + (delta > 0 ? -1 : 1), 0, max);
    }

    @Override
    protected void mouseClicked(final int mouseX, final int mouseY, final int mouseButton) throws IOException {
        if (renameField != null) {
            updateRenameFieldBounds();
            renameField.mouseClicked(mouseX, mouseY, mouseButton);
        }

        int localX = toLocalX(mouseX);
        int localY = toLocalY(mouseY);
        if (isInside(localX, localY, getToggleX(), TOGGLE_Y, getToggleWidth(), getToggleHeight())) {
            togglePressed = true;
            return;
        }

        if (isInside(localX, localY, getRefreshX(), 123, REFRESH_WIDTH, REFRESH_HEIGHT)) {
            NetworkTerminalClientState.requestRefresh();
            return;
        }

        if (isInside(localX, localY, 205, 4, 10, 10)) {
            mc.displayGuiScreen(null);
            return;
        }

        if (isInside(localX, localY, getRenameSaveX(), RENAME_FIELD_Y, RENAME_SAVE_WIDTH, RENAME_SAVE_HEIGHT)) {
            renameCurrentNetwork();
            return;
        }

        if (!colorPanelNetworkId.isEmpty()) {
            int colorIndex = getColorIndexAt(localX, localY);
            if (colorIndex >= 0) {
                updateNetworkStyle(findNetwork(colorPanelNetworkId), PALETTE[colorIndex], null);
                colorPanelNetworkId = "";
                return;
            }
            if (isInside(localX, localY, colorPanelX, colorPanelY, COLOR_PANEL_WIDTH, COLOR_PANEL_HEIGHT)) {
                return;
            }
        }

        int row = getNetworkRowAt(localX, localY);
        if (row >= 0) {
            List<NetworkTerminalClientState.NetworkSummary> networks = getNetworks();
            int index = networkScrollOffset + row;
            if (index >= networks.size()) {
                return;
            }

            NetworkTerminalClientState.NetworkSummary network = networks.get(index);
            int rowX = getNetworkListX();
            int rowY = getNetworkRowY(row);
            int rowHeight = getNetworkRowHeight(row);
            int dividerY = rowY + rowHeight / 2;
            if (sidebarExpanded && isInside(localX, localY, rowX + EXPANDED_BUTTON_X, rowY, EXPANDED_BUTTON_WIDTH, dividerY - rowY)) {
                if (mouseButton == 0) {
                    updateNetworkStyle(network, null, !network.isPinned());
                }
                return;
            }
            if (sidebarExpanded && isInside(localX, localY, rowX + EXPANDED_BUTTON_X, dividerY, EXPANDED_BUTTON_WIDTH, rowY + rowHeight - dividerY)) {
                toggleColorPanel(network, rowX + EXPANDED_NETWORK_WIDTH + 2, rowY - 16);
                return;
            }
            if (!sidebarExpanded && isInside(localX, localY, rowX, rowY, COMPACT_NETWORK_WIDTH, COMPACT_NETWORK_HEIGHT)) {
                if (mouseButton == 1) {
                    toggleColorPanel(network, rowX + COMPACT_NETWORK_WIDTH + 4, rowY - 16);
                } else {
                    colorPanelNetworkId = "";
                    NetworkTerminalClientState.selectNetwork(network.getNetworkId());
                }
                return;
            }

            if (mouseButton == 0) {
                colorPanelNetworkId = "";
                NetworkTerminalClientState.selectNetwork(network.getNetworkId());
                return;
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(final int mouseX, final int mouseY, final int state) {
        if (togglePressed && state == 0) {
            int localX = toLocalX(mouseX);
            int localY = toLocalY(mouseY);
            if (isInside(localX, localY, getToggleX(), TOGGLE_Y, getToggleWidth(), getToggleHeight())) {
                sidebarExpanded = !sidebarExpanded;
                colorPanelNetworkId = "";
                updateRenameFieldBounds();
            }
        }
        togglePressed = false;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(final char typedChar, final int keyCode) throws IOException {
        if (renameField != null && renameField.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        if (keyCode == 28 || keyCode == 156) {
            renameCurrentNetwork();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void drawScreen(final int mouseX, final int mouseY, final float partialTicks) {
        drawDefaultBackground();
        int panelLeft = getPanelLeft();
        int panelTop = getPanelTop();

        int localMouseX = toLocalX(mouseX);
        int localMouseY = toLocalY(mouseY);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.pushMatrix();
        GlStateManager.translate(panelLeft, panelTop, 0.0F);
        GlStateManager.scale(GUI_SCALE, GUI_SCALE, 1.0F);
        drawTexture(sidebarExpanded ? SIDEBAR_EXPANDED : SIDEBAR_COMPACT, 0, 0, PANEL_WIDTH, PANEL_HEIGHT);
        updateRenameFieldBounds();
        drawToggleButton(localMouseX, localMouseY);
        drawNetworks(localMouseX, localMouseY);
        drawHeader();
        drawValues();
        drawColorPanel();
        GlStateManager.popMatrix();

        if (renameField != null) {
            renameField.drawTextBox();
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
        drawCompactNetworkTooltip(mouseX, mouseY, localMouseX, localMouseY);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void drawToggleButton(final int mouseX, final int mouseY) {
        boolean hovered = isInside(mouseX, mouseY, getToggleX(), TOGGLE_Y, getToggleWidth(), getToggleHeight());
        ResourceLocation texture;
        if (sidebarExpanded) {
            texture = togglePressed && hovered ? TOGGLE_EXPANDED_ACTIVE : hovered ? TOGGLE_EXPANDED_HOVER : TOGGLE_EXPANDED_IDLE;
        } else {
            texture = togglePressed && hovered ? TOGGLE_COMPACT_ACTIVE : hovered ? TOGGLE_COMPACT_HOVER : TOGGLE_COMPACT_IDLE;
        }
        drawTexture(texture, getToggleX(), TOGGLE_Y, getToggleWidth(), getToggleHeight());
    }

    private void drawNetworks(final int mouseX, final int mouseY) {
        List<NetworkTerminalClientState.NetworkSummary> networks = getNetworks();
        if (networks.isEmpty()) {
            drawScaledString(I18n.format("gui.mmcenetworks.terminal.networks.empty"), 14, 40, 0xFFB8AA91);
            return;
        }

        String active = getActiveNetworkId();
        int visibleNameWidth = sidebarExpanded ? EXPANDED_NAME_WIDTH : 0;
        for (int row = 0; row < getNetworkRowCount(); row++) {
            int index = networkScrollOffset + row;
            if (index >= networks.size()) {
                break;
            }

            NetworkTerminalClientState.NetworkSummary network = networks.get(index);
            boolean selected = network.getNetworkId().equals(active);
            boolean hovered = getNetworkRowAt(mouseX, mouseY) == row;
            int x = getNetworkListX();
            int y = getNetworkRowY(row);

            if (sidebarExpanded) {
                drawExpandedNetworkRow(network, selected, hovered, x, y, getNetworkRowHeight(row), visibleNameWidth);
            } else {
                drawCompactNetworkRow(network, selected, hovered, x, y);
            }
        }
    }

    private void drawExpandedNetworkRow(
        final NetworkTerminalClientState.NetworkSummary network,
        final boolean selected,
        final boolean hovered,
        final int x,
        final int y,
        final int height,
        final int visibleNameWidth
    ) {
        int colorIndex = getPaletteIndex(network.getColor());
        int rowColor = PALETTE[colorIndex];
        drawRect(x, y, x + EXPANDED_NETWORK_WIDTH, y + height, rowColor);
        drawRect(x, y + Math.max(0, height - 4), x + EXPANDED_NETWORK_WIDTH, y + Math.max(0, height - 3), 0x66000000);
        drawRect(x + EXPANDED_BUTTON_X - 1, y, x + EXPANDED_NETWORK_WIDTH, y + height, tintButtonStripColor(rowColor));
        int buttonDividerY = y + height / 2;
        drawRect(x + EXPANDED_BUTTON_X - 1, buttonDividerY, x + EXPANDED_NETWORK_WIDTH, buttonDividerY + 1, 0x66000000);

        drawAtlasIcon(network.isPinned() ? PIN_ON_ICONS : PIN_OFF_ICONS, colorIndex, x + EXPANDED_BUTTON_X, y + 2, EXPANDED_BUTTON_WIDTH, EXPANDED_BUTTON_HEIGHT, COLOR_ICON_ATLAS_WIDTH, COLOR_ICON_ATLAS_HEIGHT, COLOR_ICON_CELL_WIDTH, COLOR_ICON_CELL_HEIGHT);
        drawAtlasIcon(COLOR_BUTTON_ICONS, colorIndex, x + EXPANDED_BUTTON_X, buttonDividerY + 2, EXPANDED_BUTTON_WIDTH, EXPANDED_BUTTON_HEIGHT, COLOR_ICON_ATLAS_WIDTH, COLOR_ICON_ATLAS_HEIGHT, COLOR_ICON_CELL_WIDTH, COLOR_ICON_CELL_HEIGHT);

        String name = trimToScaledWidth(network.getDisplayName(), visibleNameWidth);
        drawScaledString(name, x + EXPANDED_NAME_X, y + Math.max(4, (height - 8) / 2), selected ? 0xFFFFFFFF : 0xFFE8E2D7);
    }

    private void drawCompactNetworkRow(
        final NetworkTerminalClientState.NetworkSummary network,
        final boolean selected,
        final boolean hovered,
        final int x,
        final int y
    ) {
        int colorIndex = getPaletteIndex(network.getColor());
        drawAtlasIcon(NETWORK_OPTION_ICONS, colorIndex, x + COMPACT_ICON_X_OFFSET, y + COMPACT_ICON_Y_OFFSET, COMPACT_ICON_WIDTH, COMPACT_ICON_HEIGHT, OPTION_ICON_ATLAS_WIDTH, OPTION_ICON_ATLAS_HEIGHT, OPTION_ICON_CELL_WIDTH, OPTION_ICON_CELL_HEIGHT);
        if (network.isPinned()) {
            drawAtlasIcon(PIN_ON_ICONS, colorIndex, x + COMPACT_PIN_ICON_X_OFFSET, y + COMPACT_PIN_ICON_Y_OFFSET, COMPACT_PIN_ICON_WIDTH, COMPACT_PIN_ICON_HEIGHT, COLOR_ICON_ATLAS_WIDTH, COLOR_ICON_ATLAS_HEIGHT, COLOR_ICON_CELL_WIDTH, COLOR_ICON_CELL_HEIGHT);
        }
    }

    private void drawCompactNetworkTooltip(final int mouseX, final int mouseY, final int localMouseX, final int localMouseY) {
        if (sidebarExpanded) {
            return;
        }

        int row = getNetworkRowAt(localMouseX, localMouseY);
        int index = networkScrollOffset + row;
        List<NetworkTerminalClientState.NetworkSummary> networks = getNetworks();
        if (row < 0 || index < 0 || index >= networks.size()) {
            return;
        }

        NetworkTerminalClientState.NetworkSummary network = networks.get(index);
        List<String> lines = new ArrayList<>();
        lines.add(I18n.format("gui.mmcenetworks.terminal.tooltip.network_name", network.getDisplayName()));
        lines.add(I18n.format("gui.mmcenetworks.terminal.tooltip.network_id", network.getNetworkId()));
        drawHoveringText(lines, mouseX, mouseY);
    }

    private void drawHeader() {
        int contentLeft = getContentLeft();
        drawCenteredScaledString(I18n.format("gui.mmcenetworks.terminal.title"), contentLeft, getContentRight(), 7, 0xFFFFE7B8);
        drawScaledString(I18n.format("gui.mmcenetworks.terminal.rename"), contentLeft, 23, 0xFFD2C2A4);
        drawScaledString(I18n.format("gui.mmcenetworks.terminal.rename.save"), getRenameSaveX(), 23, 0xFFB7D9EF);
        drawScaledString(buildStatusLine(), contentLeft, 38, 0xFF8FE2A8);
        drawScaledString(I18n.format("gui.mmcenetworks.terminal.refresh"), getRefreshX(), 125, 0xFFE9D4AA);
    }

    private void drawValues() {
        List<DisplayCard> cards = collectDisplayCards();
        int startX = getContentLeft() + 2;
        int startY = 52;
        int width = Math.max(80, getContentRight() - startX - 3);
        if (cards.isEmpty()) {
            drawScaledString(I18n.format("gui.mmcenetworks.terminal.values.empty"), startX, startY, 0xFFD7CCBB);
            drawScaledString(I18n.format("gui.mmcenetworks.terminal.values.hint"), startX, startY + 12, 0xFF9E9487);
            return;
        }

        String layout = NetworkTerminalClientState.getValueDisplayLayout();
        int y = startY;
        int visibleCount = 0;
        int visibleEnd = valueScrollOffset;
        for (int index = valueScrollOffset; index < cards.size() && y < 119; index++) {
            DisplayCard card = cards.get(index);
            int height = getCardHeight(card, layout);
            drawDisplayCard(card, layout, startX, y, width, height);
            y += height + getCardGap(layout);
            visibleCount++;
            visibleEnd = index + 1;
        }

        if (visibleEnd < cards.size() || valueScrollOffset > 0) {
            drawScaledString(
                I18n.format("gui.mmcenetworks.terminal.scroll", valueScrollOffset + 1, Math.max(1, cards.size() - visibleCount + 1)),
                startX + 2,
                126,
                0xFF9E9487
            );
        }
    }

    private void drawDisplayCard(final DisplayCard card, final String layout, final int x, final int y, final int width, final int height) {
        if ("dashboard".equals(layout) || "machine".equals(layout)) {
            drawRect(x - 1, y - 1, x + width + 1, y + height, 0x26000000);
        }

        if ("text".equals(card.type) || "story".equals(layout)) {
            drawTextCard(card, x, y, width);
            return;
        }
        if ("bar".equals(card.type)) {
            drawBarCard(card, x, y, width);
            return;
        }
        if ("status".equals(card.type)) {
            drawStatusCard(card, x, y, width);
            return;
        }
        drawValueCard(card, x, y, width);
    }

    private void drawValueCard(final DisplayCard card, final int x, final int y, final int width) {
        int titleWidth = sidebarExpanded ? 47 : 68;
        int valueX = x + titleWidth + 3;
        int valueWidth = Math.max(30, x + width - valueX);
        drawScaledString(trimToScaledWidth(card.title, titleWidth), x, y, 0xFFFFD98E);
        drawScaledString(trimToScaledWidth(card.value, valueWidth), valueX, y, 0xFF9FD8FF);
        if (!card.description.isEmpty()) {
            drawScaledString(trimToScaledWidth(card.description, width), x, y + 7, 0xFFAFA696);
        }
    }

    private void drawTextCard(final DisplayCard card, final int x, final int y, final int width) {
        drawScaledString(trimToScaledWidth(card.title, width), x, y, 0xFFFFD98E);
        drawScaledString(trimToScaledWidth(card.description.isEmpty() ? card.value : card.description, width), x, y + 8, 0xFFD7CCBB);
    }

    private void drawBarCard(final DisplayCard card, final int x, final int y, final int width) {
        double max = readCardMax(card);
        double ratio = max <= 0.0D ? 0.0D : clampDouble(card.number / max, 0.0D, 1.0D);
        int barY = y + 9;
        int fillWidth = (int) Math.round((width - 2) * ratio);
        drawScaledString(trimToScaledWidth(card.title + "  " + card.value, width), x, y, 0xFFFFD98E);
        drawRect(x, barY, x + width, barY + 5, 0x66000000);
        drawRect(x + 1, barY + 1, x + 1 + fillWidth, barY + 4, 0xFF8FE2A8);
    }

    private void drawStatusCard(final DisplayCard card, final int x, final int y, final int width) {
        boolean active = readCardStatus(card);
        int color = active ? 0xFF8FE2A8 : 0xFFE08B78;
        String status = active ? optionOrDefault(card.options, "true", card.value) : optionOrDefault(card.options, "false", card.value);
        drawRect(x, y + 2, x + 5, y + 7, color);
        drawScaledString(trimToScaledWidth(card.title, 58), x + 8, y, 0xFFFFD98E);
        drawScaledString(trimToScaledWidth(status, Math.max(20, width - 66)), x + 66, y, color);
    }

    private void drawColorPanel() {
        if (colorPanelNetworkId.isEmpty()) {
            return;
        }
        float previousZLevel = zLevel;
        GlStateManager.pushMatrix();
        GlStateManager.disableDepth();
        GlStateManager.translate(0.0F, 0.0F, 300.0F);
        zLevel = 300.0F;
        drawTexture(COLOR_PANEL, colorPanelX, colorPanelY, COLOR_PANEL_WIDTH, COLOR_PANEL_HEIGHT);
        zLevel = previousZLevel;
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }

    private void drawAtlasIcon(
        final ResourceLocation texture,
        final int colorIndex,
        final int x,
        final int y,
        final int width,
        final int height,
        final int atlasWidth,
        final int atlasHeight,
        final int cellWidth,
        final int cellHeight
    ) {
        int index = clamp(colorIndex, 0, PALETTE.length - 1);
        int u = (index % ICON_ATLAS_COLUMNS) * (cellWidth + ICON_ATLAS_GAP);
        int v = (index / ICON_ATLAS_COLUMNS) * (cellHeight + ICON_ATLAS_GAP);
        mc.getTextureManager().bindTexture(texture);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        drawScaledCustomSizeModalRect(x, y, u, v, cellWidth, cellHeight, width, height, atlasWidth, atlasHeight);
    }

    private void syncRenameField() {
        if (renameField == null) {
            return;
        }
        String active = getActiveNetworkId();
        if (!active.equals(renameNetworkId)) {
            renameNetworkId = active;
            renameField.setText(NetworkTerminalClientState.getActiveNetworkDisplayName());
            renameField.setCursorPositionEnd();
        }
    }

    private void renameCurrentNetwork() {
        String networkId = getActiveNetworkId();
        if (networkId.isEmpty() || renameField == null) {
            return;
        }

        String name = renameField.getText() == null ? "" : renameField.getText().trim();
        if (name.isEmpty()) {
            return;
        }

        NetworkHandler.CHANNEL.sendToServer(new MessageRenameNetwork(networkId, name));
        NetworkTerminalClientState.requestRefresh();
    }

    private void updateNetworkStyle(
        final NetworkTerminalClientState.NetworkSummary network,
        final Integer color,
        final Boolean pinned
    ) {
        if (network == null) {
            return;
        }
        int nextColor = color == null ? network.getColor() : color;
        boolean nextPinned = pinned == null ? network.isPinned() : pinned;
        NetworkHandler.CHANNEL.sendToServer(new MessageUpdateNetworkStyle(
            network.getNetworkId(),
            nextColor,
            nextPinned
        ));
        NetworkTerminalClientState.applyLocalStyle(network.getNetworkId(), nextColor, nextPinned);
        NetworkTerminalClientState.requestRefresh();
    }

    private List<DisplayCard> collectDisplayCards() {
        NBTTagCompound sharedData = NetworkTerminalClientState.getSharedData();
        List<DisplayCard> cards = new ArrayList<>();
        List<ValueDisplaySpec> specs = NetworkTerminalClientState.getValueDisplaySpecs();
        if (!specs.isEmpty()) {
            for (ValueDisplaySpec spec : specs) {
                if (!sharedData.hasKey(spec.getKey())) {
                    continue;
                }

                String rawValue = formatTagValue(sharedData.getTag(spec.getKey()));
                String description = spec.getTemplate()
                    .replace("{name}", spec.getDisplayName())
                    .replace("{key}", spec.getKey())
                    .replace("{value}", rawValue);
                cards.add(new DisplayCard(
                    spec.getDisplayName(),
                    rawValue,
                    description,
                    spec.getCardType(),
                    parseOptions(spec.getOptions()),
                    readDouble(sharedData.getTag(spec.getKey()), 0.0D)
                ));
            }
            if (!cards.isEmpty()) {
                return cards;
            }
        }

        List<String> keys = new ArrayList<>(sharedData.getKeySet());
        keys.remove("_resourcePools");
        keys.remove("_techTree");
        keys.sort(Comparator.naturalOrder());
        for (String key : keys) {
            String rawValue = formatTagValue(sharedData.getTag(key));
            cards.add(new DisplayCard(
                key,
                rawValue,
                I18n.format("gui.mmcenetworks.terminal.values.raw_description", rawValue),
                "value",
                new HashMap<>(),
                readDouble(sharedData.getTag(key), 0.0D)
            ));
        }
        return cards;
    }

    private String buildStatusLine() {
        if (NetworkTerminalClientState.isLoading()) {
            return I18n.format("gui.mmcenetworks.terminal.status.syncing");
        }

        long updated = NetworkTerminalClientState.getLastUpdatedAt();
        if (updated <= 0L) {
            return I18n.format("gui.mmcenetworks.terminal.status.empty");
        }

        return I18n.format("gui.mmcenetworks.terminal.status.updated", new SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(new Date(updated)));
    }

    private String formatTagValue(final NBTBase tag) {
        if (tag == null) {
            return "<null>";
        }
        if (tag instanceof NBTTagString) {
            return ((NBTTagString) tag).getString();
        }
        if (tag instanceof NBTTagInt) {
            return Integer.toString(((NBTTagInt) tag).getInt());
        }
        if (tag instanceof NBTTagLong) {
            return Long.toString(((NBTTagLong) tag).getLong());
        }
        if (tag instanceof NBTTagDouble) {
            double value = ((NBTTagDouble) tag).getDouble();
            if (value == (long) value) {
                return Long.toString((long) value);
            }
            return Double.toString(value);
        }
        if (tag instanceof NBTTagByte) {
            byte value = ((NBTTagByte) tag).getByte();
            if (value == 0 || value == 1) {
                return I18n.format(value == 1 ? "gui.mmcenetworks.terminal.value.true" : "gui.mmcenetworks.terminal.value.false");
            }
            return Byte.toString(value);
        }
        return tag.toString();
    }

    private Map<String, String> parseOptions(final String options) {
        Map<String, String> result = new HashMap<>();
        if (options == null || options.trim().isEmpty()) {
            return result;
        }
        String[] entries = options.split(";");
        for (String entry : entries) {
            int split = entry.indexOf('=');
            if (split <= 0) {
                continue;
            }
            String key = entry.substring(0, split).trim().toLowerCase(Locale.ROOT);
            String value = entry.substring(split + 1).trim();
            if (!key.isEmpty()) {
                result.put(key, value);
            }
        }
        return result;
    }

    private double readCardMax(final DisplayCard card) {
        String max = card.options.get("max");
        if (max == null || max.isEmpty()) {
            return 100.0D;
        }
        try {
            return Double.parseDouble(max);
        } catch (NumberFormatException ignored) {
            return 100.0D;
        }
    }

    private boolean readCardStatus(final DisplayCard card) {
        String value = card.value == null ? "" : card.value.trim().toLowerCase(Locale.ROOT);
        return "true".equals(value) || "yes".equals(value) || "on".equals(value) || "1".equals(value) || card.number > 0.0D;
    }

    private String optionOrDefault(final Map<String, String> options, final String key, final String fallback) {
        String value = options.get(key);
        return value == null || value.isEmpty() ? fallback : value;
    }

    private double readDouble(final NBTBase tag, final double fallback) {
        if (tag instanceof NBTTagInt) {
            return ((NBTTagInt) tag).getInt();
        }
        if (tag instanceof NBTTagLong) {
            return ((NBTTagLong) tag).getLong();
        }
        if (tag instanceof NBTTagDouble) {
            return ((NBTTagDouble) tag).getDouble();
        }
        if (tag instanceof NBTTagByte) {
            return ((NBTTagByte) tag).getByte();
        }
        if (tag instanceof NBTTagString) {
            try {
                return Double.parseDouble(((NBTTagString) tag).getString());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private int getCardHeight(final DisplayCard card, final String layout) {
        if ("text".equals(card.type) || "story".equals(layout)) {
            return 18;
        }
        if ("bar".equals(card.type)) {
            return 15;
        }
        return "dashboard".equals(layout) || "machine".equals(layout) ? 12 : 14;
    }

    private int getCardGap(final String layout) {
        return "dashboard".equals(layout) || "machine".equals(layout) ? 3 : 0;
    }

    private int getNetworkRowAt(final int mouseX, final int mouseY) {
        int x = getNetworkListX();
        int y = getNetworkListY();
        if (!isInside(mouseX, mouseY, x, y, getNetworkListWidth(), getNetworkListHeight())) {
            return -1;
        }
        if (sidebarExpanded) {
            for (int row = 0; row < getNetworkRowCount(); row++) {
                int rowY = getNetworkRowY(row);
                if (mouseY >= rowY && mouseY < rowY + getNetworkRowHeight(row)) {
                    return row;
                }
            }
            return -1;
        }
        int stride = sidebarExpanded ? EXPANDED_ROW_HEIGHT + EXPANDED_ROW_GAP : COMPACT_ROW_HEIGHT + COMPACT_ROW_GAP;
        int row = (mouseY - y) / stride;
        int rowY = y + row * stride;
        int rowHeight = sidebarExpanded ? EXPANDED_ROW_HEIGHT : COMPACT_ROW_HEIGHT;
        return mouseY < rowY + rowHeight ? row : -1;
    }

    private int getColorIndexAt(final int mouseX, final int mouseY) {
        int x = colorPanelX;
        int y = colorPanelY;
        if (!isInside(mouseX, mouseY, x + COLOR_PANEL_SWATCH_X, y + COLOR_PANEL_SWATCH_Y, COLOR_PANEL_SWATCH_STRIDE * 4, COLOR_PANEL_SWATCH_STRIDE * 4)) {
            return -1;
        }
        int localX = mouseX - (x + COLOR_PANEL_SWATCH_X);
        int localY = mouseY - (y + COLOR_PANEL_SWATCH_Y);
        int column = localX / COLOR_PANEL_SWATCH_STRIDE;
        int row = localY / COLOR_PANEL_SWATCH_STRIDE;
        if (localX % COLOR_PANEL_SWATCH_STRIDE >= COLOR_PANEL_SWATCH_SIZE || localY % COLOR_PANEL_SWATCH_STRIDE >= COLOR_PANEL_SWATCH_SIZE) {
            return -1;
        }
        int index = row * 4 + column;
        return column >= 0 && column < 4 && row >= 0 && row < 4 && index < PALETTE.length ? index : -1;
    }

    private NetworkTerminalClientState.NetworkSummary findNetwork(final String networkId) {
        for (NetworkTerminalClientState.NetworkSummary network : getNetworks()) {
            if (network.getNetworkId().equals(networkId)) {
                return network;
            }
        }
        return null;
    }

    private void toggleColorPanel(final NetworkTerminalClientState.NetworkSummary network, final int preferredX, final int preferredY) {
        if (network == null) {
            return;
        }
        if (network.getNetworkId().equals(colorPanelNetworkId)) {
            colorPanelNetworkId = "";
            return;
        }
        colorPanelNetworkId = network.getNetworkId();
        colorPanelX = clamp(preferredX, 4, PANEL_WIDTH - COLOR_PANEL_WIDTH - 4);
        colorPanelY = clamp(preferredY, 4, PANEL_HEIGHT - COLOR_PANEL_HEIGHT - 4);
    }

    private List<NetworkTerminalClientState.NetworkSummary> getNetworks() {
        return NetworkTerminalClientState.getAvailableNetworks();
    }

    private String getActiveNetworkId() {
        String active = NetworkTerminalClientState.getActiveNetworkId();
        return active == null || active.isEmpty() ? initialNetworkId : active;
    }

    private void clampScroll() {
        networkScrollOffset = clamp(networkScrollOffset, 0, Math.max(0, getNetworks().size() - getNetworkRowCount()));
        valueScrollOffset = clamp(valueScrollOffset, 0, Math.max(0, collectDisplayCards().size() - 5));
    }

    private int getPanelLeft() {
        return (width - scale(PANEL_WIDTH)) / 2;
    }

    private int getPanelTop() {
        return (height - scale(PANEL_HEIGHT)) / 2;
    }

    private void updateRenameFieldBounds() {
        if (renameField == null) {
            return;
        }
        int localX = getContentLeft() + RENAME_FIELD_OFFSET_X;
        int localWidth = Math.max(20, getRenameSaveX() - localX - 3);
        renameField.x = getPanelLeft() + scale(localX);
        renameField.y = getPanelTop() + scale(RENAME_FIELD_Y);
        renameField.width = scale(localWidth);
        renameField.height = scale(12);
    }

    private int getContentLeft() {
        return sidebarExpanded ? CONTENT_EXPANDED_LEFT : CONTENT_COMPACT_LEFT;
    }

    private int getContentRight() {
        return CONTENT_RIGHT;
    }

    private int getRenameSaveX() {
        return getContentRight() - RENAME_SAVE_WIDTH;
    }

    private int getRefreshX() {
        return getContentRight() - REFRESH_WIDTH - 1;
    }

    private void drawTexture(final ResourceLocation texture, final int x, final int y, final int width, final int height) {
        mc.getTextureManager().bindTexture(texture);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        drawModalRectWithCustomSizedTexture(x, y, 0, 0, width, height, width, height);
    }

    private void drawScaledString(final String text, final int x, final int y, final int color) {
        double textScale = clampTextScale();
        GlStateManager.pushMatrix();
        GlStateManager.scale(textScale, textScale, 1.0D);
        fontRenderer.drawString(text, (float) (x / textScale), (float) (y / textScale), color, false);
        GlStateManager.popMatrix();
    }

    private void drawCenteredScaledString(final String text, final int left, final int right, final int y, final int color) {
        int x = left + Math.max(0, right - left - getScaledStringWidth(text)) / 2;
        drawScaledString(text, x, y, color);
    }

    private int getScaledStringWidth(final String text) {
        return (int) Math.ceil(fontRenderer.getStringWidth(text) * clampTextScale());
    }

    private String trimToScaledWidth(final String text, final int width) {
        return fontRenderer.trimStringToWidth(text, (int) Math.max(1.0D, width / clampTextScale()));
    }

    private double clampTextScale() {
        return Math.max(0.4D, Math.min(1.2D, MMCENetworksConfig.terminalTextScale));
    }

    private int getScaledMouseX() {
        return MouseAccessor.getEventX(width);
    }

    private int getScaledMouseY() {
        return MouseAccessor.getEventY(height);
    }

    private int getMouseWheelDelta() {
        return MouseAccessor.getWheelDelta();
    }

    private static boolean isInside(final int mouseX, final int mouseY, final int x, final int y, final int width, final int height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }

    private static int clamp(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(final double value, final double min, final double max) {
        return Math.max(min, Math.min(max, value));
    }

    private int toLocalX(final int screenX) {
        return (int) ((screenX - getPanelLeft()) / GUI_SCALE);
    }

    private int toLocalY(final int screenY) {
        return (int) ((screenY - getPanelTop()) / GUI_SCALE);
    }

    private static int scale(final int value) {
        return Math.round(value * GUI_SCALE);
    }

    private int getToggleWidth() {
        return sidebarExpanded ? 70 : 16;
    }

    private int getToggleHeight() {
        return 17;
    }

    private int getToggleX() {
        return sidebarExpanded ? EXPANDED_TOGGLE_X : COMPACT_TOGGLE_X;
    }

    private int getNetworkListX() {
        return sidebarExpanded ? EXPANDED_NETWORK_X : COMPACT_NETWORK_X;
    }

    private int getNetworkListY() {
        return sidebarExpanded ? EXPANDED_NETWORK_Y : COMPACT_NETWORK_Y;
    }

    private int getNetworkListWidth() {
        return sidebarExpanded ? EXPANDED_NETWORK_WIDTH : COMPACT_NETWORK_WIDTH;
    }

    private int getNetworkListHeight() {
        if (sidebarExpanded) {
            return EXPANDED_NETWORK_HEIGHT;
        }
        return getNetworkRowCount() * COMPACT_ROW_HEIGHT + Math.max(0, getNetworkRowCount() - 1) * COMPACT_ROW_GAP;
    }

    private int getNetworkRowCount() {
        return sidebarExpanded ? EXPANDED_NETWORK_ROW_COUNT : COMPACT_NETWORK_ROW_COUNT;
    }

    private int getNetworkRowY(final int row) {
        if (sidebarExpanded) {
            return EXPANDED_NETWORK_Y + row * EXPANDED_NETWORK_HEIGHT / EXPANDED_NETWORK_ROW_COUNT;
        }
        return COMPACT_NETWORK_Y + row * (COMPACT_ROW_HEIGHT + COMPACT_ROW_GAP);
    }

    private int getNetworkRowHeight(final int row) {
        if (sidebarExpanded) {
            return getNetworkRowY(row + 1) - getNetworkRowY(row);
        }
        return COMPACT_ROW_HEIGHT;
    }

    private int tintNetworkColor(final int color, final boolean selected, final boolean hovered) {
        int alpha = selected ? 0xFF : hovered ? 0xEE : 0xDD;
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private int tintButtonStripColor(final int color) {
        int alpha = (color >>> 24) & 0xFF;
        int red = Math.min(255, (((color >>> 16) & 0xFF) * 3 + 255) / 4);
        int green = Math.min(255, (((color >>> 8) & 0xFF) * 3 + 255) / 4);
        int blue = Math.min(255, ((color & 0xFF) * 3 + 255) / 4);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private int getPaletteIndex(final int color) {
        int rgb = color & 0x00FFFFFF;
        int bestIndex = 0;
        int bestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < PALETTE.length; i++) {
            int paletteRgb = PALETTE[i] & 0x00FFFFFF;
            int dr = ((rgb >> 16) & 0xFF) - ((paletteRgb >> 16) & 0xFF);
            int dg = ((rgb >> 8) & 0xFF) - ((paletteRgb >> 8) & 0xFF);
            int db = (rgb & 0xFF) - (paletteRgb & 0xFF);
            int distance = dr * dr + dg * dg + db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static ResourceLocation texture(final String name) {
        return new ResourceLocation(MMCENetworksMod.MOD_ID, "textures/gui/network_terminal/" + name);
    }

    private static final class DisplayCard {
        private final String title;
        private final String value;
        private final String description;
        private final String type;
        private final Map<String, String> options;
        private final double number;

        private DisplayCard(
            final String title,
            final String value,
            final String description,
            final String type,
            final Map<String, String> options,
            final double number
        ) {
            this.title = title;
            this.value = value;
            this.description = description;
            this.type = type == null || type.isEmpty() ? "value" : type;
            this.options = options == null ? new HashMap<>() : options;
            this.number = number;
        }
    }

    private static final class MouseAccessor {
        private static int getWheelDelta() {
            return invokeInt("getEventDWheel", 0);
        }

        private static int getEventX(final int scaledWidth) {
            int displayWidth = Minecraft.getMinecraft().displayWidth;
            if (displayWidth <= 0) {
                return 0;
            }
            return invokeInt("getEventX", 0) * scaledWidth / displayWidth;
        }

        private static int getEventY(final int scaledHeight) {
            Minecraft minecraft = Minecraft.getMinecraft();
            int displayHeight = minecraft.displayHeight;
            if (displayHeight <= 0) {
                return 0;
            }
            return scaledHeight - invokeInt("getEventY", 0) * scaledHeight / displayHeight - 1;
        }

        private static int invokeInt(final String methodName, final int fallback) {
            try {
                Class<?> mouseClass = Class.forName("org.lwjgl.input.Mouse");
                Method method = mouseClass.getMethod(methodName);
                Object value = method.invoke(null);
                return value instanceof Integer ? (Integer) value : fallback;
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
                return fallback;
            }
        }
    }
}
