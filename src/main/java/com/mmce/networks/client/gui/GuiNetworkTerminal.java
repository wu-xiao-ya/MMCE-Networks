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
import java.util.List;
import java.util.Locale;

public class GuiNetworkTerminal extends GuiScreen {
    private static final float GUI_SCALE = 1.5F;
    private static final int PANEL_WIDTH = 219;
    private static final int PANEL_HEIGHT = 146;
    private static final int SIDEBAR_WIDTH = 82;
    private static final int NETWORK_ROW_HEIGHT = 16;
    private static final int NETWORK_ROW_GAP = 2;
    private static final int NETWORK_ROW_COUNT = 5;
    private static final int EXPANDED_TOGGLE_X = 8;
    private static final int TOGGLE_Y = 8;
    private static final int COMPACT_TOGGLE_X = 9;
    private static final int EXPANDED_NETWORK_X = 7;
    private static final int EXPANDED_NETWORK_Y = 27;
    private static final int EXPANDED_NETWORK_WIDTH = 70;
    private static final int COMPACT_NETWORK_X = 9;
    private static final int COMPACT_NETWORK_Y = 27;
    private static final int COMPACT_NETWORK_WIDTH = 14;
    private static final int COMPACT_NETWORK_HEIGHT = 14;
    private static final int COMPACT_NETWORK_GAP = 3;
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
        0xFFB8332D, 0xFFE36F2C, 0xFFE8B640, 0xFF7FB14B,
        0xFF43A86F, 0xFF39A6A3, 0xFF3C7FC2, 0xFF504CA8,
        0xFF7B4AB0, 0xFFC05A9D, 0xFFDD7A8A, 0xFF8B5A3C,
        0xFFCBC3B5, 0xFF8E8D84, 0xFF4F5860, 0xFF1F242B
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
        int panelLeft = getPanelLeft();
        int panelTop = getPanelTop();
        renameField = new GuiTextField(0, fontRenderer, panelLeft + scale(105), panelTop + scale(21), scale(78), scale(12));
        renameField.setMaxStringLength(32);
        renameField.setEnableBackgroundDrawing(false);
        renameField.setTextColor(0xFFE8D8B7);
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
            int max = Math.max(0, getNetworks().size() - NETWORK_ROW_COUNT);
            networkScrollOffset = clamp(networkScrollOffset + (delta > 0 ? -1 : 1), 0, max);
            return;
        }

        int max = Math.max(0, collectValueCards().size() - 5);
        valueScrollOffset = clamp(valueScrollOffset + (delta > 0 ? -1 : 1), 0, max);
    }

    @Override
    protected void mouseClicked(final int mouseX, final int mouseY, final int mouseButton) throws IOException {
        if (renameField != null) {
            renameField.mouseClicked(mouseX, mouseY, mouseButton);
        }

        int localX = toLocalX(mouseX);
        int localY = toLocalY(mouseY);
        if (isInside(localX, localY, getToggleX(), TOGGLE_Y, getToggleWidth(), getToggleHeight())) {
            togglePressed = true;
            return;
        }

        if (isInside(localX, localY, 174, 123, 30, 12)) {
            NetworkTerminalClientState.requestRefresh();
            return;
        }

        if (isInside(localX, localY, 205, 4, 10, 10)) {
            mc.displayGuiScreen(null);
            return;
        }

        if (isInside(localX, localY, 186, 21, 20, 12)) {
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
            if (sidebarExpanded && isInside(localX, localY, rowX + EXPANDED_NETWORK_WIDTH - 11, rowY + 1, 8, 7)) {
                if (mouseButton == 0) {
                    updateNetworkStyle(network, null, !network.isPinned());
                }
                return;
            }
            if (sidebarExpanded && isInside(localX, localY, rowX + EXPANDED_NETWORK_WIDTH - 11, rowY + 8, 8, 7)) {
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
        int visibleNameWidth = sidebarExpanded ? 45 : 0;
        for (int row = 0; row < NETWORK_ROW_COUNT; row++) {
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
                drawExpandedNetworkRow(network, selected, hovered, x, y, visibleNameWidth);
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
        final int visibleNameWidth
    ) {
        drawRect(x, y, x + EXPANDED_NETWORK_WIDTH, y + NETWORK_ROW_HEIGHT, 0xFF101112);
        drawRect(x + 2, y + 2, x + EXPANDED_NETWORK_WIDTH - 2, y + NETWORK_ROW_HEIGHT - 2, tintNetworkColor(network.getColor(), selected, hovered));
        drawRect(x + 2, y + NETWORK_ROW_HEIGHT - 3, x + EXPANDED_NETWORK_WIDTH - 2, y + NETWORK_ROW_HEIGHT - 2, 0x66000000);

        int colorIndex = getPaletteIndex(network.getColor());
        drawAtlasIcon(network.isPinned() ? PIN_ON_ICONS : PIN_OFF_ICONS, colorIndex, x + EXPANDED_NETWORK_WIDTH - 11, y + 1, 8, 7, COLOR_ICON_ATLAS_WIDTH, COLOR_ICON_ATLAS_HEIGHT, COLOR_ICON_CELL_WIDTH, COLOR_ICON_CELL_HEIGHT);
        drawAtlasIcon(COLOR_BUTTON_ICONS, colorIndex, x + EXPANDED_NETWORK_WIDTH - 11, y + 8, 8, 7, COLOR_ICON_ATLAS_WIDTH, COLOR_ICON_ATLAS_HEIGHT, COLOR_ICON_CELL_WIDTH, COLOR_ICON_CELL_HEIGHT);

        String name = trimToScaledWidth(network.getDisplayName(), visibleNameWidth + 4);
        drawScaledString(name, x + 6, y + 5, selected ? 0xFFFFFFFF : 0xFFE8E2D7);
    }

    private void drawCompactNetworkRow(
        final NetworkTerminalClientState.NetworkSummary network,
        final boolean selected,
        final boolean hovered,
        final int x,
        final int y
    ) {
        int colorIndex = getPaletteIndex(network.getColor());
        if (selected || hovered) {
            drawRect(x - 1, y - 1, x + COMPACT_NETWORK_WIDTH + 1, y + COMPACT_NETWORK_HEIGHT + 1, selected ? 0xFFFFFFFF : 0xFFE6E6E6);
        }
        drawAtlasIcon(NETWORK_OPTION_ICONS, colorIndex, x, y, COMPACT_NETWORK_WIDTH, COMPACT_NETWORK_HEIGHT, OPTION_ICON_ATLAS_WIDTH, OPTION_ICON_ATLAS_HEIGHT, OPTION_ICON_CELL_WIDTH, OPTION_ICON_CELL_HEIGHT);
    }

    private void drawHeader() {
        drawScaledString(I18n.format("gui.mmcenetworks.terminal.title"), 96, 7, 0xFFFFE7B8);
        drawScaledString(I18n.format("gui.mmcenetworks.terminal.rename"), 82, 23, 0xFFD2C2A4);
        drawScaledString(I18n.format("gui.mmcenetworks.terminal.rename.save"), 187, 23, 0xFFB7D9EF);
        drawScaledString(buildStatusLine(), 82, 38, 0xFF8FE2A8);
        drawScaledString(I18n.format("gui.mmcenetworks.terminal.refresh"), 174, 125, 0xFFE9D4AA);
    }

    private void drawValues() {
        List<ValueCard> cards = collectValueCards();
        int startX = 84;
        int startY = 52;
        if (cards.isEmpty()) {
            drawScaledString(I18n.format("gui.mmcenetworks.terminal.values.empty"), startX, startY, 0xFFD7CCBB);
            drawScaledString(I18n.format("gui.mmcenetworks.terminal.values.hint"), startX, startY + 12, 0xFF9E9487);
            return;
        }

        int maxRows = 5;
        for (int i = 0; i < maxRows; i++) {
            int index = valueScrollOffset + i;
            if (index >= cards.size()) {
                break;
            }

            ValueCard card = cards.get(index);
            int y = startY + i * 14;
            drawScaledString(trimToScaledWidth(card.title, 47), startX, y, 0xFFFFD98E);
            drawScaledString(trimToScaledWidth(card.value, 76), startX + 50, y, 0xFF9FD8FF);
            if (!card.description.isEmpty()) {
                drawScaledString(trimToScaledWidth(card.description, 120), startX, y + 7, 0xFFAFA696);
            }
        }

        if (cards.size() > maxRows) {
            drawScaledString(
                I18n.format("gui.mmcenetworks.terminal.scroll", valueScrollOffset + 1, cards.size() - maxRows + 1),
                86,
                126,
                0xFF9E9487
            );
        }
    }

    private void drawColorPanel() {
        if (colorPanelNetworkId.isEmpty()) {
            return;
        }
        drawTexture(COLOR_PANEL, colorPanelX, colorPanelY, COLOR_PANEL_WIDTH, COLOR_PANEL_HEIGHT);
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
        NetworkHandler.CHANNEL.sendToServer(new MessageUpdateNetworkStyle(
            network.getNetworkId(),
            color == null ? network.getColor() : color,
            pinned == null ? network.isPinned() : pinned
        ));
        NetworkTerminalClientState.requestRefresh();
    }

    private List<ValueCard> collectValueCards() {
        NBTTagCompound sharedData = NetworkTerminalClientState.getSharedData();
        List<ValueCard> cards = new ArrayList<>();
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
                cards.add(new ValueCard(spec.getDisplayName(), rawValue, description));
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
            cards.add(new ValueCard(key, rawValue, I18n.format("gui.mmcenetworks.terminal.values.raw_description", rawValue)));
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

    private int getNetworkRowAt(final int mouseX, final int mouseY) {
        int x = getNetworkListX();
        int y = getNetworkListY();
        if (!isInside(mouseX, mouseY, x, y, getNetworkListWidth(), getNetworkListHeight())) {
            return -1;
        }
        int stride = sidebarExpanded ? NETWORK_ROW_HEIGHT + NETWORK_ROW_GAP : COMPACT_NETWORK_HEIGHT + COMPACT_NETWORK_GAP;
        int row = (mouseY - y) / stride;
        int rowY = y + row * stride;
        int rowHeight = sidebarExpanded ? NETWORK_ROW_HEIGHT : COMPACT_NETWORK_HEIGHT;
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
        networkScrollOffset = clamp(networkScrollOffset, 0, Math.max(0, getNetworks().size() - NETWORK_ROW_COUNT));
        valueScrollOffset = clamp(valueScrollOffset, 0, Math.max(0, collectValueCards().size() - 5));
    }

    private int getPanelLeft() {
        return (width - scale(PANEL_WIDTH)) / 2;
    }

    private int getPanelTop() {
        return (height - scale(PANEL_HEIGHT)) / 2;
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
            return NETWORK_ROW_COUNT * NETWORK_ROW_HEIGHT + Math.max(0, NETWORK_ROW_COUNT - 1) * NETWORK_ROW_GAP;
        }
        return NETWORK_ROW_COUNT * COMPACT_NETWORK_HEIGHT + Math.max(0, NETWORK_ROW_COUNT - 1) * COMPACT_NETWORK_GAP;
    }

    private int getNetworkRowY(final int row) {
        if (sidebarExpanded) {
            return EXPANDED_NETWORK_Y + row * (NETWORK_ROW_HEIGHT + NETWORK_ROW_GAP);
        }
        return COMPACT_NETWORK_Y + row * (COMPACT_NETWORK_HEIGHT + COMPACT_NETWORK_GAP);
    }

    private int tintNetworkColor(final int color, final boolean selected, final boolean hovered) {
        int alpha = selected ? 0xFF : hovered ? 0xEE : 0xDD;
        return (alpha << 24) | (color & 0x00FFFFFF);
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

    private static final class ValueCard {
        private final String title;
        private final String value;
        private final String description;

        private ValueCard(final String title, final String value, final String description) {
            this.title = title;
            this.value = value;
            this.description = description;
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
