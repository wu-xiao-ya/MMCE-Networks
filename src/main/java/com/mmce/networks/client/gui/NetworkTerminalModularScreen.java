package com.mmce.networks.client.gui;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.cleanroommc.modularui.widgets.layout.Column;
import com.mmce.networks.MMCENetworksMod;
import com.mmce.networks.common.data.NetworkResourcePool;
import com.mmce.networks.common.data.NetworkTechTree;
import com.mmce.networks.common.data.NetworkValueDisplayRegistry.ValueDisplaySpec;
import com.mmce.networks.common.network.MessageRenameNetwork;
import com.mmce.networks.common.network.NetworkHandler;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagString;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class NetworkTerminalModularScreen {
    private static final int TAB_VALUES = 0;
    private static final int TAB_RESOURCES = 1;
    private static final int TAB_TECH = 2;

    private static final int PANEL_WIDTH = 392;
    private static final int PANEL_HEIGHT = 232;
    private static final int SIDEBAR_WIDTH = 98;
    private static final int CONTENT_WIDTH = 260;
    private static final int CONTENT_HEIGHT = 150;

    private static final int LINE_SECTION = 0;
    private static final int LINE_VALUE = 1;
    private static final int LINE_STAT = 2;
    private static final int LINE_POSITIVE = 3;
    private static final int LINE_NEGATIVE = 4;
    private static final int LINE_HINT = 5;

    private final String initialNetworkId;
    private int selectedTab = TAB_VALUES;
    private int appliedRevision = -1;
    private ListWidget sidebarList;
    private ListWidget contentList;
    private TextFieldWidget renameField;
    private String renameFieldNetworkId = "";

    private NetworkTerminalModularScreen(final String networkId) {
        this.initialNetworkId = networkId == null ? "" : networkId;
    }

    public static void open(final String networkId) {
        NetworkTerminalModularScreen terminal = new NetworkTerminalModularScreen(networkId);
        UISettings settings = new UISettings();
        settings.getRecipeViewerSettings().disableRecipeViewer();
        ClientGUI.open(
            new ModularScreen(MMCENetworksMod.MOD_ID, ignored -> terminal.buildPanel()).pausesGame(false),
            settings
        );
    }

    private ModularPanel buildPanel() {
        ModularPanel panel = ModularPanel.defaultPanel("network_terminal", PANEL_WIDTH, PANEL_HEIGHT);
        panel.background(new Rectangle().setColor(0xE5121214).setCornerRadius(8));
        panel.onUpdateListener(ignored -> refreshIfNeeded(), true);

        panel.child(createText(IKey.lang("gui.mmcenetworks.terminal.title"), 0, 10, PANEL_WIDTH, 14, 0xFFF3E3BF, Alignment.Center));
        panel.child(createText(IKey.dynamic(this::buildNetworkLine), 118, 36, 220, 12, 0xFFDAD2C1, Alignment.TopLeft));
        panel.child(createText(IKey.lang("gui.mmcenetworks.terminal.rename"), 118, 50, 54, 12, 0xFFF0CC84, Alignment.TopLeft));
        panel.child(createText(IKey.dynamic(this::buildStatusLine), 118, 70, 220, 12, 0xFF8FE2A8, Alignment.TopLeft));
        panel.child(createText(IKey.dynamic(this::buildSummaryLine), 300, 36, 74, 12, 0xFFD8C7A5, Alignment.CenterRight));
        panel.child(createText(IKey.lang("gui.mmcenetworks.terminal.networks"), 16, 36, 80, 12, 0xFFF0CC84, Alignment.TopLeft));

        panel.child(createTabButton(TAB_VALUES, 118, 16, "gui.mmcenetworks.terminal.tab.values"));
        panel.child(createTabButton(TAB_RESOURCES, 200, 16, "gui.mmcenetworks.terminal.tab.resources"));
        panel.child(createTabButton(TAB_TECH, 282, 16, "gui.mmcenetworks.terminal.tab.tech"));
        renameField = createRenameField();
        panel.child(renameField);
        panel.child(createActionButton(298, 46, 76, 18, "gui.mmcenetworks.terminal.rename.save", this::renameCurrentNetwork));
        panel.child(createActionButton(118, 198, 70, 18, "gui.mmcenetworks.terminal.refresh", () -> NetworkTerminalClientState.requestRefresh()));
        panel.child(createActionButton(304, 198, 70, 18, "gui.mmcenetworks.terminal.close", ClientGUI::close));

        sidebarList = new ListWidget();
        sidebarList.scrollDirection(GuiAxis.Y);
        sidebarList.background(new Rectangle().setColor(0x5A211812).setCornerRadius(5));
        sidebarList.pos(14, 54);
        sidebarList.size(SIDEBAR_WIDTH, CONTENT_HEIGHT);

        contentList = new ListWidget();
        contentList.scrollDirection(GuiAxis.Y);
        contentList.background(new Rectangle().setColor(0x4A120F12).setCornerRadius(6));
        contentList.pos(118, 88);
        contentList.size(CONTENT_WIDTH, 130);

        panel.child(sidebarList);
        panel.child(contentList);

        refreshIfNeeded();
        return panel;
    }

    private ButtonWidget<?> createTabButton(final int tabId, final int x, final int y, final String langKey) {
        ButtonWidget<?> button = new ButtonWidget<>()
            .onMousePressed(mouseButton -> {
                if (selectedTab != tabId) {
                    selectedTab = tabId;
                    rebuildContent();
                }
                return true;
            })
            .background(new Rectangle().setColor(0x6630221C).setCornerRadius(4));
        button.child(createDynamicText(IKey.lang(langKey), 0, 3, 76, 12, () -> selectedTab == tabId ? 0xFFF8E4B2 : 0xFFD0C4AF, Alignment.Center));
        button.pos(x, y);
        button.size(76, 18);
        return button;
    }

    private ButtonWidget<?> createActionButton(final int x, final int y, final int width, final int height, final String langKey, final Runnable action) {
        ButtonWidget<?> button = new ButtonWidget<>()
            .onMousePressed(mouseButton -> {
                action.run();
                return true;
            })
            .background(new Rectangle().setColor(0x88443226).setCornerRadius(4))
            .hoverBackground(new Rectangle().setColor(0xAA5D412A).setCornerRadius(4));
        button.child(createText(IKey.lang(langKey), 0, 3, width, 12, 0xFFF3E8D1, Alignment.Center));
        button.pos(x, y);
        button.size(width, height);
        return button;
    }

    private void refreshIfNeeded() {
        int revision = NetworkTerminalClientState.getStateRevision();
        if (revision == appliedRevision || sidebarList == null || contentList == null) {
            return;
        }
        appliedRevision = revision;
        syncRenameField();
        rebuildSidebar();
        rebuildContent();
    }

    private void rebuildSidebar() {
        clearList(sidebarList);

        List<NetworkTerminalClientState.NetworkSummary> networks = NetworkTerminalClientState.getAvailableNetworks();
        if (networks.isEmpty()) {
            TextWidget<?> empty = new TextWidget<>(IKey.lang("gui.mmcenetworks.terminal.networks.empty"));
            empty.color(0xFFBBB2A6);
            empty.margin(8);
            empty.size(SIDEBAR_WIDTH - 20, 12);
            sidebarList.child(empty);
            return;
        }

        String active = getDisplayNetworkId();
        for (NetworkTerminalClientState.NetworkSummary network : networks) {
            boolean selected = network.getNetworkId().equals(active);
            ButtonWidget<?> button = new ButtonWidget<>()
                .onMousePressed(mouseButton -> {
                    NetworkTerminalClientState.selectNetwork(network.getNetworkId());
                    return true;
                })
                .background(new Rectangle().setColor(selected ? 0xAA50311D : 0x66322620).setCornerRadius(4))
                .hoverBackground(new Rectangle().setColor(0x88553A25).setCornerRadius(4));
            button.child(createText(IKey.str(network.getDisplayName()), 6, 3, SIDEBAR_WIDTH - 26, 12, selected ? 0xFFF9E9BD : 0xFFD6C8AF, Alignment.TopLeft));
            button.margin(6, 4);
            button.size(SIDEBAR_WIDTH - 12, 18);
            sidebarList.child(button);
        }
    }

    private void rebuildContent() {
        clearList(contentList);
        if (selectedTab == TAB_VALUES) {
            buildValueContent();
            return;
        }

        List<DisplayLine> lines = selectedTab == TAB_RESOURCES
            ? collectResourceLines(NetworkTerminalClientState.getSharedData())
            : collectTechLines(NetworkTerminalClientState.getSharedData());

        for (DisplayLine line : lines) {
            TextWidget<?> text = new TextWidget<>(IKey.str(line.text));
            text.color(pickLineColor(line.kind));
            text.margin(10, 4);
            text.size(CONTENT_WIDTH - 26, 12);
            contentList.child(text);
        }
    }

    private void buildValueContent() {
        List<ValueCard> cards = collectValueCards(NetworkTerminalClientState.getSharedData());
        if (cards.isEmpty()) {
            contentList.child(buildHintCard(
                translate("gui.mmcenetworks.terminal.values.empty"),
                translate("gui.mmcenetworks.terminal.values.hint")
            ));
            return;
        }

        for (ValueCard card : cards) {
            Column cardWidget = new Column();
            cardWidget.coverChildrenHeight();
            cardWidget.size(CONTENT_WIDTH - 18, 54);
            cardWidget.padding(8);
            cardWidget.childPadding(3);
            cardWidget.margin(8, 5);
            cardWidget.background(new Rectangle().setColor(0x7A201922).setCornerRadius(6));

            cardWidget.child(createText(IKey.str(card.title), 0, 0, CONTENT_WIDTH - 40, 12, 0xFFF4D18C, Alignment.TopLeft));
            cardWidget.child(createText(IKey.str(card.value), 0, 0, CONTENT_WIDTH - 40, 12, 0xFF8FD8FF, Alignment.TopLeft));
            cardWidget.child(createText(IKey.str(card.description), 0, 0, CONTENT_WIDTH - 40, 24, 0xFFD9D2C4, Alignment.TopLeft));

            contentList.child(cardWidget);
        }
    }

    private Column buildHintCard(final String title, final String description) {
        Column column = new Column();
        column.coverChildrenHeight();
        column.size(CONTENT_WIDTH - 18, 50);
        column.padding(8);
        column.childPadding(4);
        column.margin(8, 5);
        column.background(new Rectangle().setColor(0x6630221C).setCornerRadius(6));
        column.child(createText(IKey.str(title), 0, 0, CONTENT_WIDTH - 40, 12, 0xFFF0D59D, Alignment.TopLeft));
        column.child(createText(IKey.str(description), 0, 0, CONTENT_WIDTH - 40, 24, 0xFFBEB4A5, Alignment.TopLeft));
        return column;
    }

    private void clearList(final ListWidget listWidget) {
        while (!listWidget.getChildren().isEmpty()) {
            listWidget.remove(listWidget.getChildren().size() - 1);
        }
    }

    private TextWidget<?> createText(
        final IKey key,
        final int x,
        final int y,
        final int width,
        final int height,
        final int color,
        final Alignment alignment
    ) {
        TextWidget<?> widget = new TextWidget<>(key);
        widget.alignment(alignment);
        widget.color(color);
        widget.pos(x, y);
        widget.size(width, height);
        return widget;
    }

    private TextWidget<?> createDynamicText(
        final IKey key,
        final int x,
        final int y,
        final int width,
        final int height,
        final java.util.function.IntSupplier color,
        final Alignment alignment
    ) {
        TextWidget<?> widget = new TextWidget<>(key);
        widget.alignment(alignment);
        widget.color(color);
        widget.pos(x, y);
        widget.size(width, height);
        return widget;
    }

    private TextFieldWidget createRenameField() {
        TextFieldWidget widget = new TextFieldWidget();
        widget.pos(176, 46);
        widget.size(116, 18);
        widget.background(new Rectangle().setColor(0x77312622).setCornerRadius(4));
        widget.setTextColor(0xFFF6E9C8);
        widget.setMarkedColor(0xFF8FD8FF);
        widget.hintText(translate("gui.mmcenetworks.terminal.rename.hint"));
        widget.hintColor(0xFF9D927E);
        widget.setMaxLength(32);
        return widget;
    }

    private void syncRenameField() {
        if (renameField == null) {
            return;
        }
        String activeNetworkId = getDisplayNetworkId();
        if (!activeNetworkId.equals(renameFieldNetworkId)) {
            renameFieldNetworkId = activeNetworkId;
            renameField.setText(NetworkTerminalClientState.getActiveNetworkDisplayName());
        }
    }

    private void renameCurrentNetwork() {
        String networkId = getDisplayNetworkId();
        if (networkId == null || networkId.isEmpty() || renameField == null) {
            return;
        }
        String displayName = renameField.getText();
        if (displayName == null || displayName.trim().isEmpty()) {
            return;
        }
        NetworkHandler.CHANNEL.sendToServer(new MessageRenameNetwork(networkId, displayName.trim()));
        renameFieldNetworkId = networkId;
        renameField.setText(displayName.trim());
        NetworkTerminalClientState.requestRefresh();
    }

    private String buildNetworkLine() {
        return translate("gui.mmcenetworks.terminal.network", NetworkTerminalClientState.getActiveNetworkDisplayName());
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

    private String getDisplayNetworkId() {
        String active = NetworkTerminalClientState.getActiveNetworkId();
        return active == null || active.isEmpty() ? initialNetworkId : active;
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

    private DisplayLine line(final int kind, final String text) {
        return new DisplayLine(kind, text);
    }

    private static final class DisplayLine {
        private final int kind;
        private final String text;

        private DisplayLine(final int kind, final String text) {
            this.kind = kind;
            this.text = text;
        }
    }

    private static final class ValueCard {
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
