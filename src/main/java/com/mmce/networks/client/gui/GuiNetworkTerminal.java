package com.mmce.networks.client.gui;

import com.mmce.networks.client.util.ClientCompat;
import com.mmce.networks.common.data.NetworkResourcePool;
import com.mmce.networks.common.data.NetworkTechTree;
import com.mmce.networks.common.data.NetworkValueDisplayRegistry.ValueDisplaySpec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagString;

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
    private static final int TAB_VALUES = 0;
    private static final int TAB_RESOURCES = 1;
    private static final int TAB_TECH = 2;
    private static final int BUTTON_TOGGLE_SIDEBAR = 102;

    private static final int PANEL_WIDTH = 368;
    private static final int PANEL_HEIGHT = 206;
    private static final int SIDEBAR_WIDTH = 84;
    private static final int CONTENT_TOP = 72;
    private static final int CONTENT_BOTTOM = 24;
    private static final int VISIBLE_LIST_LINES = 10;
    private static final int VISIBLE_VALUE_CARDS = 3;
    private static final int VALUE_CARD_HEIGHT = 34;
    private static final int VALUE_CARD_GAP = 6;
    private static final int NETWORK_ENTRY_HEIGHT = 18;

    private static final int LINE_SECTION = 0;
    private static final int LINE_VALUE = 1;
    private static final int LINE_STAT = 2;
    private static final int LINE_POSITIVE = 3;
    private static final int LINE_NEGATIVE = 4;
    private static final int LINE_HINT = 5;

    private final String initialNetworkId;
    private int selectedTab = TAB_VALUES;
    private int scrollOffset;
    private boolean sidebarOpen = true;

    public GuiNetworkTerminal(final String networkId) {
        this.initialNetworkId = networkId;
    }

    public static void open(final String networkId) {
        Minecraft minecraft = ClientCompat.getMinecraft();
        if (minecraft != null) {
            ClientCompat.displayGuiScreen(minecraft, new GuiNetworkTerminal(networkId));
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        rebuildButtons();
    }

    @Override
    protected void actionPerformed(final GuiButton button) throws IOException {
        if (button.id >= TAB_VALUES && button.id <= TAB_TECH) {
            selectedTab = button.id;
            scrollOffset = 0;
            rebuildButtons();
            return;
        }
        if (button.id == 100) {
            NetworkTerminalClientState.requestRefresh();
            return;
        }
        if (button.id == 101) {
            mc.displayGuiScreen(null);
            return;
        }
        if (button.id == BUTTON_TOGGLE_SIDEBAR) {
            sidebarOpen = !sidebarOpen;
            scrollOffset = 0;
            rebuildButtons();
        }
    }

    @Override
    protected void mouseClicked(final int mouseX, final int mouseY, final int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0 || !sidebarOpen) {
            return;
        }

        List<String> networkIds = NetworkTerminalClientState.getAvailableNetworkIds();
        int sidebarLeft = getSidebarLeft();
        int entryTop = getPanelTop() + 42;
        for (int i = 0; i < networkIds.size(); i++) {
            int top = entryTop + i * NETWORK_ENTRY_HEIGHT;
            if (mouseX >= sidebarLeft + 8 && mouseX <= sidebarLeft + SIDEBAR_WIDTH - 8 && mouseY >= top && mouseY <= top + 14) {
                NetworkTerminalClientState.selectNetwork(networkIds.get(i));
                scrollOffset = 0;
                break;
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int delta = getMouseWheelDelta();
        if (delta == 0) {
            return;
        }

        int direction = delta > 0 ? -1 : 1;
        int maxOffset = Math.max(0, getCurrentEntryCount() - getVisibleCapacity());
        scrollOffset = Math.max(0, Math.min(maxOffset, scrollOffset + direction));
    }

    @Override
    public void drawScreen(final int mouseX, final int mouseY, final float partialTicks) {
        drawDefaultBackground();

        int left = getPanelLeft();
        int top = getPanelTop();
        int right = left + PANEL_WIDTH;
        int bottom = top + PANEL_HEIGHT;
        int mainLeft = getMainLeft();

        drawRect(left - 3, top - 3, right + 3, bottom + 3, 0xFF2A1E14);
        drawRect(left, top, right, bottom, 0xE5130F0D);
        drawRect(left, top, right, top + 22, 0xFF6B4A25);
        drawRect(left, top + 22, right, top + 23, 0xFFB98B4A);

        if (sidebarOpen) {
            drawSidebar(left, top, bottom);
        } else {
            drawRect(left + 6, top + 28, left + 30, bottom - 10, 0x35150F0D);
        }

        drawRect(mainLeft, top + 30, right - 10, bottom - 10, 0x6A120D0B);
        drawRect(mainLeft, top + CONTENT_TOP, right - 10, top + CONTENT_TOP + 1, 0x55795839);

        drawCenteredString(fontRenderer, translate("gui.mmcenetworks.terminal.title"), width / 2, top + 7, 0xFFF8E7B9);

        String summaryLine = buildSummaryLine();
        int summaryWidth = fontRenderer.getStringWidth(summaryLine) + 12;
        int networkLineWidth = Math.max(72, (right - mainLeft - 20) - summaryWidth - 8);
        String activeNetworkId = getDisplayNetworkId();

        drawString(fontRenderer, trimToWidth(translate("gui.mmcenetworks.terminal.network", activeNetworkId), networkLineWidth), mainLeft + 8, top + 34, 0xFFD7C8A1);
        drawString(fontRenderer, translate("gui.mmcenetworks.terminal.tab", getTabTitle()), mainLeft + 8, top + 46, 0xFFAFD5FF);
        drawString(fontRenderer, buildStatusLine(), mainLeft + 8, top + 58, 0xFF7FE0A7);

        drawRect(right - summaryWidth - 12, top + 32, right - 12, top + 46, 0x66402E1D);
        drawCenteredString(fontRenderer, summaryLine, right - summaryWidth / 2 - 12, top + 35, 0xFFCEC0A4);

        if (selectedTab == TAB_VALUES) {
            drawValueCards(mainLeft, top, right, collectValueCards(NetworkTerminalClientState.getSharedData()));
        } else {
            drawListContent(mainLeft, top, collectVisibleLines());
        }

        int totalEntries = getCurrentEntryCount();
        int visibleEntries = getVisibleCapacity();
        drawScrollBar(mainLeft, top, right, bottom, totalEntries, visibleEntries);

        if (totalEntries > visibleEntries) {
            drawString(
                fontRenderer,
                translate("gui.mmcenetworks.terminal.scroll", scrollOffset + 1, Math.max(1, totalEntries - visibleEntries + 1)),
                right - 88,
                bottom - 16,
                0xFF9D927E
            );
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void rebuildButtons() {
        buttonList.clear();

        int left = getPanelLeft();
        int top = getPanelTop();
        int mainLeft = getMainLeft();
        int right = left + PANEL_WIDTH;

        buttonList.add(new GuiButton(BUTTON_TOGGLE_SIDEBAR, left + 6, top + 6, 20, 20, sidebarOpen ? "<" : ">"));
        buttonList.add(new GuiButton(TAB_VALUES, mainLeft, top - 24, 82, 20, translate("gui.mmcenetworks.terminal.tab.values")));
        buttonList.add(new GuiButton(TAB_RESOURCES, mainLeft + 88, top - 24, 82, 20, translate("gui.mmcenetworks.terminal.tab.resources")));
        buttonList.add(new GuiButton(TAB_TECH, mainLeft + 176, top - 24, 82, 20, translate("gui.mmcenetworks.terminal.tab.tech")));
        buttonList.add(new GuiButton(100, mainLeft, top + PANEL_HEIGHT + 6, 94, 20, translate("gui.mmcenetworks.terminal.refresh")));
        buttonList.add(new GuiButton(101, right - 104, top + PANEL_HEIGHT + 6, 94, 20, translate("gui.mmcenetworks.terminal.close")));
    }

    private void drawSidebar(final int left, final int top, final int bottom) {
        int sidebarLeft = getSidebarLeft();
        int sidebarRight = sidebarLeft + SIDEBAR_WIDTH;
        drawRect(sidebarLeft, top + 28, sidebarRight, bottom - 10, 0x6A2A1914);
        drawRect(sidebarRight - 1, top + 28, sidebarRight, bottom - 10, 0xAA74552D);
        drawString(fontRenderer, translate("gui.mmcenetworks.terminal.networks"), sidebarLeft + 8, top + 32, 0xFFF0CC84);

        List<String> networkIds = NetworkTerminalClientState.getAvailableNetworkIds();
        String active = getDisplayNetworkId();
        int entryTop = top + 42;
        if (networkIds.isEmpty()) {
            drawString(fontRenderer, translate("gui.mmcenetworks.terminal.networks.empty"), sidebarLeft + 8, entryTop + 4, 0xFFBFB5A7);
            return;
        }

        for (int i = 0; i < networkIds.size(); i++) {
            int y = entryTop + i * NETWORK_ENTRY_HEIGHT;
            String networkId = networkIds.get(i);
            boolean selected = networkId.equals(active);
            if (selected) {
                drawRect(sidebarLeft + 6, y - 1, sidebarRight - 6, y + 13, 0x88473218);
            }
            drawString(
                fontRenderer,
                trimToWidth(networkId, SIDEBAR_WIDTH - 18),
                sidebarLeft + 8,
                y + 2,
                selected ? 0xFFF8E7B9 : 0xFFD5C5A7
            );
        }
    }

    private String buildStatusLine() {
        if (NetworkTerminalClientState.isLoading()) {
            return translate("gui.mmcenetworks.terminal.status.syncing");
        }

        long updated = NetworkTerminalClientState.getLastUpdatedAt();
        if (updated <= 0L) {
            return translate("gui.mmcenetworks.terminal.status.empty");
        }

        return translate(
            "gui.mmcenetworks.terminal.status.updated",
            new SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(new Date(updated))
        );
    }

    private String getTabTitle() {
        if (selectedTab == TAB_RESOURCES) {
            return translate("gui.mmcenetworks.terminal.tab.resources");
        }
        if (selectedTab == TAB_TECH) {
            return translate("gui.mmcenetworks.terminal.tab.tech_tree");
        }
        return translate("gui.mmcenetworks.terminal.tab.values_plain");
    }

    private String getDisplayNetworkId() {
        String active = NetworkTerminalClientState.getActiveNetworkId();
        return active == null || active.isEmpty() ? initialNetworkId : active;
    }

    private List<DisplayLine> collectVisibleLines() {
        NBTTagCompound sharedData = NetworkTerminalClientState.getSharedData();
        if (selectedTab == TAB_RESOURCES) {
            return collectResourceLines(sharedData);
        }
        if (selectedTab == TAB_TECH) {
            return collectTechLines(sharedData);
        }
        return new ArrayList<>();
    }

    private List<ValueCard> collectValueCards(final NBTTagCompound sharedData) {
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
                cards.add(new ValueCard(spec.getDisplayName(), description, rawValue));
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
            cards.add(new ValueCard(key, translate("gui.mmcenetworks.terminal.values.raw_description", rawValue), rawValue));
        }
        return cards;
    }

    private void drawValueCards(final int contentLeft, final int top, final int right, final List<ValueCard> cards) {
        if (cards.isEmpty()) {
            drawString(fontRenderer, translate("gui.mmcenetworks.terminal.values.empty"), contentLeft + 14, top + CONTENT_TOP + 10, 0xFFBFB5A7);
            drawString(fontRenderer, translate("gui.mmcenetworks.terminal.values.hint"), contentLeft + 14, top + CONTENT_TOP + 22, 0xFFBFB5A7);
            return;
        }

        int cardWidth = right - contentLeft - 24;
        int cardLeft = contentLeft + 8;
        int cardTop = top + CONTENT_TOP + 6;
        for (int i = 0; i < VISIBLE_VALUE_CARDS; i++) {
            int index = scrollOffset + i;
            if (index >= cards.size()) {
                break;
            }

            ValueCard card = cards.get(index);
            int y = cardTop + i * (VALUE_CARD_HEIGHT + VALUE_CARD_GAP);
            drawRect(cardLeft, y, cardLeft + cardWidth, y + VALUE_CARD_HEIGHT, 0x8A23170F);
            drawRect(cardLeft, y, cardLeft + cardWidth, y + 1, 0xCC7D5A2E);
            drawRect(cardLeft, y, cardLeft + 1, y + VALUE_CARD_HEIGHT, 0xAA5E4424);

            String badgeText = card.value;
            int badgeWidth = Math.min(90, fontRenderer.getStringWidth(badgeText) + 12);
            int badgeLeft = cardLeft + cardWidth - badgeWidth - 8;
            drawRect(badgeLeft, y + 6, badgeLeft + badgeWidth, y + 18, 0x884D341A);

            drawString(fontRenderer, trimToWidth(card.title, cardWidth - badgeWidth - 28), cardLeft + 8, y + 6, 0xFFF0CC84);
            drawCenteredString(fontRenderer, trimToWidth(badgeText, badgeWidth - 8), badgeLeft + badgeWidth / 2, y + 8, 0xFFF8F2E8);

            List<String> descLines = wrapText(card.description, cardWidth - 16);
            for (int lineIndex = 0; lineIndex < Math.min(2, descLines.size()); lineIndex++) {
                drawString(fontRenderer, descLines.get(lineIndex), cardLeft + 8, y + 20 + lineIndex * 10, 0xFFE6DCD0);
            }
        }
    }

    private List<DisplayLine> collectResourceLines(final NBTTagCompound sharedData) {
        List<DisplayLine> lines = new ArrayList<>();
        NBTTagCompound pools = NetworkResourcePool.getAllPoolsSnapshot(sharedData);
        List<String> keys = new ArrayList<>(pools.getKeySet());
        keys.sort(Comparator.naturalOrder());

        if (keys.isEmpty()) {
            lines.add(line(LINE_HINT, translate("gui.mmcenetworks.terminal.resources.empty")));
            lines.add(line(LINE_HINT, translate("gui.mmcenetworks.terminal.resources.hint")));
            return lines;
        }

        for (String key : keys) {
            NBTTagCompound pool = pools.getCompoundTag(key);
            lines.add(line(LINE_SECTION, translate("gui.mmcenetworks.terminal.resources.pool", key)));
            lines.add(line(
                LINE_STAT,
                translate(
                    "gui.mmcenetworks.terminal.resources.capacity",
                    pool.getLong("capacity"),
                    pool.getLong("used"),
                    pool.getLong("available")
                )
            ));

            NBTTagCompound providers = pool.hasKey("providers", 10) ? pool.getCompoundTag("providers") : new NBTTagCompound();
            for (String source : sortedKeys(providers)) {
                lines.add(line(LINE_POSITIVE, translate("gui.mmcenetworks.terminal.resources.provider", trimSource(source), providers.getLong(source))));
            }

            NBTTagCompound consumers = pool.hasKey("consumers", 10) ? pool.getCompoundTag("consumers") : new NBTTagCompound();
            for (String source : sortedKeys(consumers)) {
                lines.add(line(LINE_NEGATIVE, translate("gui.mmcenetworks.terminal.resources.consumer", trimSource(source), consumers.getLong(source))));
            }
        }
        return lines;
    }

    private List<DisplayLine> collectTechLines(final NBTTagCompound sharedData) {
        List<DisplayLine> lines = new ArrayList<>();
        NBTTagCompound tree = NetworkTechTree.getTreeSnapshot(sharedData);
        List<String> techIds = new ArrayList<>(tree.getKeySet());
        techIds.sort(Comparator.naturalOrder());

        if (techIds.isEmpty()) {
            lines.add(line(LINE_HINT, translate("gui.mmcenetworks.terminal.tech.empty")));
            lines.add(line(LINE_HINT, translate("gui.mmcenetworks.terminal.tech.hint")));
            return lines;
        }

        for (String techId : techIds) {
            NBTTagCompound tech = tree.getCompoundTag(techId);
            String state = tech.getBoolean("unlocked")
                ? translate("gui.mmcenetworks.terminal.tech.state.unlocked")
                : translate("gui.mmcenetworks.terminal.tech.state.locked");
            String ready = tech.getBoolean("canUnlock")
                ? translate("gui.mmcenetworks.terminal.tech.ready.ready")
                : translate("gui.mmcenetworks.terminal.tech.ready.blocked");
            lines.add(line(LINE_SECTION, translate("gui.mmcenetworks.terminal.tech.entry", techId)));
            lines.add(line(LINE_STAT, translate("gui.mmcenetworks.terminal.tech.status", state, ready)));

            NBTTagCompound requires = tech.hasKey("requires", 10) ? tech.getCompoundTag("requires") : new NBTTagCompound();
            List<String> prerequisites = sortedKeys(requires);
            if (prerequisites.isEmpty()) {
                lines.add(line(LINE_HINT, translate("gui.mmcenetworks.terminal.tech.requires.none")));
            } else {
                lines.add(line(LINE_HINT, translate("gui.mmcenetworks.terminal.tech.requires.list", String.join(", ", prerequisites))));
            }
        }
        return lines;
    }

    private void drawListContent(final int contentLeft, final int top, final List<DisplayLine> lines) {
        int lineTop = top + CONTENT_TOP + 6;
        for (int i = 0; i < VISIBLE_LIST_LINES; i++) {
            int index = scrollOffset + i;
            if (index >= lines.size()) {
                break;
            }
            DisplayLine line = lines.get(index);
            drawString(fontRenderer, line.text, contentLeft + 14, lineTop + i * 11, pickLineColor(line.kind));
        }
    }

    private List<String> sortedKeys(final NBTTagCompound compound) {
        List<String> keys = new ArrayList<>(compound.getKeySet());
        keys.sort(Comparator.naturalOrder());
        return keys;
    }

    private String trimSource(final String source) {
        int separator = source.lastIndexOf(':');
        if (separator < 0 || separator == source.length() - 1) {
            return source;
        }
        return source.substring(separator + 1);
    }

    private int getMouseWheelDelta() {
        try {
            Class<?> mouseClass = Class.forName("org.lwjgl.input.Mouse");
            Method method = mouseClass.getMethod("getEventDWheel");
            Object value = method.invoke(null);
            return value instanceof Integer ? (Integer) value : 0;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            return 0;
        }
    }

    private String buildSummaryLine() {
        if (selectedTab == TAB_VALUES) {
            return translate("gui.mmcenetworks.terminal.summary.entries", collectValueCards(NetworkTerminalClientState.getSharedData()).size());
        }

        NBTTagCompound sharedData = NetworkTerminalClientState.getSharedData();
        if (selectedTab == TAB_RESOURCES) {
            return translate("gui.mmcenetworks.terminal.summary.pools", NetworkResourcePool.getAllPoolsSnapshot(sharedData).getKeySet().size());
        }
        return translate("gui.mmcenetworks.terminal.summary.techs", NetworkTechTree.getTreeSnapshot(sharedData).getKeySet().size());
    }

    private int getCurrentEntryCount() {
        if (selectedTab == TAB_VALUES) {
            return collectValueCards(NetworkTerminalClientState.getSharedData()).size();
        }
        return collectVisibleLines().size();
    }

    private int getVisibleCapacity() {
        return selectedTab == TAB_VALUES ? VISIBLE_VALUE_CARDS : VISIBLE_LIST_LINES;
    }

    private void drawScrollBar(final int contentLeft, final int top, final int right, final int bottom, final int totalLines, final int visibleLines) {
        int trackLeft = right - 18;
        int trackTop = top + CONTENT_TOP + 2;
        int trackBottom = bottom - CONTENT_BOTTOM;
        drawRect(trackLeft, trackTop, trackLeft + 4, trackBottom, 0x553B2D26);

        if (totalLines <= visibleLines) {
            drawRect(trackLeft, trackTop, trackLeft + 4, trackBottom, 0xAA9C805A);
            return;
        }

        int range = totalLines - visibleLines;
        int thumbHeight = Math.max(12, (trackBottom - trackTop) * visibleLines / totalLines);
        int freeSpace = (trackBottom - trackTop) - thumbHeight;
        int thumbTop = trackTop + (range == 0 ? 0 : freeSpace * scrollOffset / range);
        drawRect(trackLeft, thumbTop, trackLeft + 4, thumbTop + thumbHeight, 0xFFD4B27C);
    }

    private int pickLineColor(final int kind) {
        if (kind == LINE_SECTION) {
            return 0xFFF8D28A;
        }
        if (kind == LINE_STAT) {
            return 0xFF9EE0FF;
        }
        if (kind == LINE_POSITIVE) {
            return 0xFF85E89D;
        }
        if (kind == LINE_NEGATIVE) {
            return 0xFFFFA98C;
        }
        if (kind == LINE_VALUE) {
            return 0xFFF3EEE4;
        }
        return 0xFFBFB5A7;
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
                return value == 1 ? translate("gui.mmcenetworks.terminal.value.true") : translate("gui.mmcenetworks.terminal.value.false");
            }
            return Byte.toString(value);
        }
        return tag.toString();
    }

    private List<String> wrapText(final String text, final int maxWidth) {
        List<String> lines = fontRenderer.listFormattedStringToWidth(text, maxWidth);
        return lines == null ? new ArrayList<>() : lines;
    }

    private String translate(final String key, final Object... args) {
        return I18n.format(key, args);
    }

    private String trimToWidth(final String text, final int maxWidth) {
        if (fontRenderer.getStringWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        return fontRenderer.trimStringToWidth(text, Math.max(0, maxWidth - fontRenderer.getStringWidth(ellipsis))) + ellipsis;
    }

    private int getPanelLeft() {
        return width / 2 - PANEL_WIDTH / 2;
    }

    private int getPanelTop() {
        return height / 2 - PANEL_HEIGHT / 2;
    }

    private int getSidebarLeft() {
        return getPanelLeft() + 6;
    }

    private int getMainLeft() {
        return getPanelLeft() + (sidebarOpen ? SIDEBAR_WIDTH + 12 : 32);
    }

    private DisplayLine line(final int kind, final String text) {
        return new DisplayLine(kind, text);
    }

    private static class DisplayLine {
        private final int kind;
        private final String text;

        private DisplayLine(final int kind, final String text) {
            this.kind = kind;
            this.text = text;
        }
    }

    private static class ValueCard {
        private final String title;
        private final String description;
        private final String value;

        private ValueCard(final String title, final String description, final String value) {
            this.title = title;
            this.description = description;
            this.value = value;
        }
    }
}
