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
    private static final int MAX_VISIBLE_LINES = 10;
    private static final int LINE_SECTION = 0;
    private static final int LINE_VALUE = 1;
    private static final int LINE_STAT = 2;
    private static final int LINE_POSITIVE = 3;
    private static final int LINE_NEGATIVE = 4;
    private static final int LINE_HINT = 5;

    private final String networkId;
    private int selectedTab = TAB_VALUES;
    private int scrollOffset;

    public GuiNetworkTerminal(final String networkId) {
        this.networkId = networkId;
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
        buttonList.clear();

        int left = width / 2 - 120;
        int top = height / 2 - 90;
        buttonList.add(new GuiButton(TAB_VALUES, left, top - 24, 70, 20, translate("gui.mmcenetworks.terminal.tab.values")));
        buttonList.add(new GuiButton(TAB_RESOURCES, left + 75, top - 24, 70, 20, translate("gui.mmcenetworks.terminal.tab.resources")));
        buttonList.add(new GuiButton(TAB_TECH, left + 150, top - 24, 70, 20, translate("gui.mmcenetworks.terminal.tab.tech")));
        buttonList.add(new GuiButton(100, left, top + 184, 80, 20, translate("gui.mmcenetworks.terminal.refresh")));
        buttonList.add(new GuiButton(101, left + 160, top + 184, 60, 20, translate("gui.mmcenetworks.terminal.close")));
    }

    @Override
    protected void actionPerformed(final GuiButton button) throws IOException {
        if (button.id >= TAB_VALUES && button.id <= TAB_TECH) {
            selectedTab = button.id;
            scrollOffset = 0;
            return;
        }
        if (button.id == 100) {
            NetworkTerminalClientState.requestRefresh();
            return;
        }
        if (button.id == 101) {
            mc.displayGuiScreen(null);
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
        int maxOffset = Math.max(0, collectVisibleLines().size() - 10);
        scrollOffset = Math.max(0, Math.min(maxOffset, scrollOffset + direction));
    }

    @Override
    public void drawScreen(final int mouseX, final int mouseY, final float partialTicks) {
        drawDefaultBackground();

        int left = width / 2 - 120;
        int top = height / 2 - 90;
        int right = width / 2 + 120;
        int bottom = top + 178;

        drawRect(left - 2, top - 2, right + 2, bottom + 2, 0xFF2C2218);
        drawRect(left, top, right, bottom, 0xE018120E);
        drawRect(left, top, right, top + 18, 0xFF6A4A22);
        drawRect(left + 6, top + 62, right - 6, bottom - 18, 0x66110B08);

        String summaryLine = buildSummaryLine();
        int summaryWidth = fontRenderer.getStringWidth(summaryLine);
        int networkLineWidth = Math.max(60, right - left - 24 - summaryWidth);

        drawCenteredString(fontRenderer, translate("gui.mmcenetworks.terminal.title"), width / 2, top + 5, 0xFFF8E7B9);
        drawString(fontRenderer, trimToWidth(translate("gui.mmcenetworks.terminal.network", networkId), networkLineWidth), left + 8, top + 26, 0xFFD7C8A1);
        drawString(fontRenderer, translate("gui.mmcenetworks.terminal.tab", getTabTitle()), left + 8, top + 38, 0xFFAFD5FF);
        drawString(fontRenderer, buildStatusLine(), left + 8, top + 50, 0xFF7FE0A7);

        drawString(fontRenderer, summaryLine, right - 8 - summaryWidth, top + 26, 0xFFCEC0A4);

        List<DisplayLine> lines = collectVisibleLines();
        int lineTop = top + 66;
        for (int i = 0; i < MAX_VISIBLE_LINES; i++) {
            int index = scrollOffset + i;
            if (index >= lines.size()) {
                break;
            }
            DisplayLine line = lines.get(index);
            drawString(fontRenderer, line.text, left + 10, lineTop + i * 11, pickLineColor(line.kind));
        }

        drawScrollBar(left, top, right, bottom, lines.size());

        if (lines.size() > MAX_VISIBLE_LINES) {
            drawString(
                fontRenderer,
                translate("gui.mmcenetworks.terminal.scroll", scrollOffset + 1, Math.max(1, lines.size() - MAX_VISIBLE_LINES + 1)),
                right - 76,
                bottom - 12,
                0xFF9D927E
            );
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
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

    private List<DisplayLine> collectVisibleLines() {
        NBTTagCompound sharedData = NetworkTerminalClientState.getSharedData();
        if (selectedTab == TAB_RESOURCES) {
            return collectResourceLines(sharedData);
        }
        if (selectedTab == TAB_TECH) {
            return collectTechLines(sharedData);
        }
        return collectValueLines(sharedData);
    }

    private List<DisplayLine> collectValueLines(final NBTTagCompound sharedData) {
        List<DisplayLine> lines = new ArrayList<>();
        List<ValueDisplaySpec> specs = NetworkTerminalClientState.getValueDisplaySpecs();
        if (!specs.isEmpty()) {
            return collectConfiguredValueLines(sharedData, specs);
        }

        List<String> keys = new ArrayList<>(sharedData.getKeySet());
        keys.remove("_resourcePools");
        keys.remove("_techTree");
        keys.sort(Comparator.naturalOrder());

        if (keys.isEmpty()) {
            lines.add(line(LINE_HINT, translate("gui.mmcenetworks.terminal.values.empty")));
            lines.add(line(LINE_HINT, translate("gui.mmcenetworks.terminal.values.hint")));
            return lines;
        }

        for (String key : keys) {
            NBTBase tag = sharedData.getTag(key);
            lines.add(line(LINE_SECTION, translate("gui.mmcenetworks.terminal.values.key", key)));
            lines.add(line(LINE_VALUE, translate("gui.mmcenetworks.terminal.values.value", formatTagValue(tag))));
        }
        return lines;
    }

    private List<DisplayLine> collectConfiguredValueLines(final NBTTagCompound sharedData, final List<ValueDisplaySpec> specs) {
        List<DisplayLine> lines = new ArrayList<>();
        for (ValueDisplaySpec spec : specs) {
            if (!sharedData.hasKey(spec.getKey())) {
                continue;
            }

            NBTBase tag = sharedData.getTag(spec.getKey());
            String rawValue = formatTagValue(tag);
            String text = spec.getTemplate()
                .replace("{name}", spec.getDisplayName())
                .replace("{key}", spec.getKey())
                .replace("{value}", rawValue);
            lines.add(line(LINE_SECTION, spec.getDisplayName()));
            lines.add(line(LINE_VALUE, text));
        }

        if (lines.isEmpty()) {
            lines.add(line(LINE_HINT, translate("gui.mmcenetworks.terminal.values.empty")));
            lines.add(line(LINE_HINT, translate("gui.mmcenetworks.terminal.values.config_hint")));
        }
        return lines;
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
        NBTTagCompound sharedData = NetworkTerminalClientState.getSharedData();
        if (selectedTab == TAB_VALUES) {
            int count = Math.max(0, sharedData.getKeySet().size() - 2);
            return translate("gui.mmcenetworks.terminal.summary.entries", count);
        }
        if (selectedTab == TAB_RESOURCES) {
            int count = NetworkResourcePool.getAllPoolsSnapshot(sharedData).getKeySet().size();
            return translate("gui.mmcenetworks.terminal.summary.pools", count);
        }
        int count = NetworkTechTree.getTreeSnapshot(sharedData).getKeySet().size();
        return translate("gui.mmcenetworks.terminal.summary.techs", count);
    }

    private void drawScrollBar(final int left, final int top, final int right, final int bottom, final int totalLines) {
        int trackLeft = right - 10;
        int trackTop = top + 66;
        int trackBottom = bottom - 22;
        drawRect(trackLeft, trackTop, trackLeft + 4, trackBottom, 0x553B2D26);

        if (totalLines <= MAX_VISIBLE_LINES) {
            drawRect(trackLeft, trackTop, trackLeft + 4, trackBottom, 0xAA9C805A);
            return;
        }

        int range = totalLines - MAX_VISIBLE_LINES;
        int thumbHeight = Math.max(12, (trackBottom - trackTop) * MAX_VISIBLE_LINES / totalLines);
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
}
