package com.mmce.networks.client.gui;

import com.mmce.networks.client.util.ClientCompat;
import com.mmce.networks.common.data.NetworkResourcePool;
import com.mmce.networks.common.data.NetworkTechTree;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

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
        buttonList.add(new GuiButton(TAB_VALUES, left, top - 24, 70, 20, "数值"));
        buttonList.add(new GuiButton(TAB_RESOURCES, left + 75, top - 24, 70, 20, "资源池"));
        buttonList.add(new GuiButton(TAB_TECH, left + 150, top - 24, 70, 20, "科技"));
        buttonList.add(new GuiButton(100, left, top + 184, 80, 20, "刷新"));
        buttonList.add(new GuiButton(101, left + 160, top + 184, 60, 20, "关闭"));
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

        drawCenteredString(fontRenderer, "MMCE Networks Terminal", width / 2, top + 5, 0xFFF8E7B9);
        drawString(fontRenderer, "Network: " + networkId, left + 8, top + 26, 0xFFD7C8A1);
        drawString(fontRenderer, "Tab: " + getTabTitle(), left + 8, top + 38, 0xFFAFD5FF);
        drawString(fontRenderer, buildStatusLine(), left + 8, top + 50, 0xFF7FE0A7);

        List<String> lines = collectVisibleLines();
        int lineTop = top + 66;
        int maxLines = 10;
        for (int i = 0; i < maxLines; i++) {
            int index = scrollOffset + i;
            if (index >= lines.size()) {
                break;
            }
            drawString(fontRenderer, lines.get(index), left + 8, lineTop + i * 11, 0xFFF3EEE4);
        }

        if (lines.size() > maxLines) {
            drawString(
                fontRenderer,
                String.format(Locale.ROOT, "Scroll %d/%d", scrollOffset + 1, Math.max(1, lines.size() - maxLines + 1)),
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
            return "Status: syncing...";
        }

        long updated = NetworkTerminalClientState.getLastUpdatedAt();
        if (updated <= 0L) {
            return "Status: no snapshot";
        }

        return "Updated: " + new SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(new Date(updated));
    }

    private String getTabTitle() {
        if (selectedTab == TAB_RESOURCES) {
            return "资源池";
        }
        if (selectedTab == TAB_TECH) {
            return "科技树";
        }
        return "普通数值";
    }

    private List<String> collectVisibleLines() {
        NBTTagCompound sharedData = NetworkTerminalClientState.getSharedData();
        if (selectedTab == TAB_RESOURCES) {
            return collectResourceLines(sharedData);
        }
        if (selectedTab == TAB_TECH) {
            return collectTechLines(sharedData);
        }
        return collectValueLines(sharedData);
    }

    private List<String> collectValueLines(final NBTTagCompound sharedData) {
        List<String> lines = new ArrayList<>();
        List<String> keys = new ArrayList<>(sharedData.getKeySet());
        keys.remove("_resourcePools");
        keys.remove("_techTree");
        keys.sort(Comparator.naturalOrder());

        if (keys.isEmpty()) {
            lines.add("No plain shared values.");
            lines.add("Use Networks.setInt/setString/etc to populate data.");
            return lines;
        }

        for (String key : keys) {
            NBTBase tag = sharedData.getTag(key);
            lines.add(key + " = " + (tag == null ? "<null>" : tag.toString()));
        }
        return lines;
    }

    private List<String> collectResourceLines(final NBTTagCompound sharedData) {
        List<String> lines = new ArrayList<>();
        NBTTagCompound pools = NetworkResourcePool.getAllPoolsSnapshot(sharedData);
        List<String> keys = new ArrayList<>(pools.getKeySet());
        keys.sort(Comparator.naturalOrder());

        if (keys.isEmpty()) {
            lines.add("No resource pools.");
            lines.add("Example: Networks.setSupply(controller, \"compute\", 5)");
            return lines;
        }

        for (String key : keys) {
            NBTTagCompound pool = pools.getCompoundTag(key);
            lines.add(key + " cap=" + pool.getLong("capacity") + " used=" + pool.getLong("used") + " free=" + pool.getLong("available"));

            NBTTagCompound providers = pool.hasKey("providers", 10) ? pool.getCompoundTag("providers") : new NBTTagCompound();
            for (String source : sortedKeys(providers)) {
                lines.add("  + " + trimSource(source) + " = " + providers.getLong(source));
            }

            NBTTagCompound consumers = pool.hasKey("consumers", 10) ? pool.getCompoundTag("consumers") : new NBTTagCompound();
            for (String source : sortedKeys(consumers)) {
                lines.add("  - " + trimSource(source) + " = " + consumers.getLong(source));
            }
        }
        return lines;
    }

    private List<String> collectTechLines(final NBTTagCompound sharedData) {
        List<String> lines = new ArrayList<>();
        NBTTagCompound tree = NetworkTechTree.getTreeSnapshot(sharedData);
        List<String> techIds = new ArrayList<>(tree.getKeySet());
        techIds.sort(Comparator.naturalOrder());

        if (techIds.isEmpty()) {
            lines.add("No tech definitions.");
            lines.add("Example: Networks.defineTech(controller, \"techA\")");
            return lines;
        }

        for (String techId : techIds) {
            NBTTagCompound tech = tree.getCompoundTag(techId);
            String state = tech.getBoolean("unlocked") ? "UNLOCKED" : "LOCKED";
            String ready = tech.getBoolean("canUnlock") ? "ready" : "blocked";
            lines.add(techId + " [" + state + ", " + ready + "]");

            NBTTagCompound requires = tech.hasKey("requires", 10) ? tech.getCompoundTag("requires") : new NBTTagCompound();
            List<String> prerequisites = sortedKeys(requires);
            if (prerequisites.isEmpty()) {
                lines.add("  requires: <none>");
            } else {
                lines.add("  requires: " + String.join(", ", prerequisites));
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
}
