package net.aaavein.tidymods.client.gui;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.aaavein.tidymods.client.ModDataManager;
import net.aaavein.tidymods.client.gui.widget.IconButton;
import net.aaavein.tidymods.config.TidyModsConfig;
import net.aaavein.tidymods.data.ModData;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ModListWidget extends ContainerObjectSelectionList<ModListWidget.Entry> {

    private static final Map<String, TextureInfo> TEXTURE_CACHE = new HashMap<>();
    private static final int TOOLTIP_MAX_WIDTH = 200;
    private static final String LIBRARY_CATEGORY = "library";

    private final ModListScreen screen;
    private String query = "";
    private int modCount = 0;

    public ModListWidget(ModListScreen screen, @NotNull Minecraft mc) {
        super(mc, screen.width, screen.height - 64, 48, 24);
        this.screen = screen;
        refresh();
    }

    public void updateLayout(int width, HeaderAndFooterLayout layout) {
        setSize(width, layout.getContentHeight());
        setY(layout.getHeaderHeight());
    }

    public void filter(String text) {
        query = text.toLowerCase(Locale.ROOT);
        refresh();
    }

    public int getModCount() {
        return modCount;
    }

    public void refresh() {
        double scroll = getScrollAmount();
        clearEntries();
        modCount = 0;

        Map<String, List<ModData>> grouped = groupMods();
        List<String> sorted = sortCategories(grouped);

        for (String category : sorted) {
            List<ModData> mods = grouped.get(category);
            if (mods == null || mods.isEmpty()) continue;

            mods.sort(Comparator.comparing(ModData::displayName, String.CASE_INSENSITIVE_ORDER));
            addEntry(new CategoryEntry(Component.translatable("tidymods.category." + category)));

            for (ModData mod : mods) {
                addEntry(new ModEntry(mod, category));
            }
        }

        setScrollAmount(scroll);
    }

    private Map<String, List<ModData>> groupMods() {
        Map<String, List<ModData>> grouped = new HashMap<>();

        for (ModData mod : ModDataManager.all()) {
            if (TidyModsConfig.isHidden(mod.modId())) continue;
            if (!query.isEmpty() && !matches(mod)) continue;
            if (TidyModsConfig.configsOnly() && !mod.hasConfig()) continue;

            modCount++;
            String category = ModDataManager.category(mod.modId());
            grouped.computeIfAbsent(category, k -> new ArrayList<>()).add(mod);
        }

        return grouped;
    }

    private List<String> sortCategories(Map<String, List<ModData>> grouped) {
        String defaultCategory = TidyModsConfig.defaultCategory();
        boolean librariesAtBottom = TidyModsConfig.librariesAtBottom();

        List<String> sorted = new ArrayList<>(grouped.keySet());
        sorted.sort((a, b) -> {
            boolean aDefault = a.equals(defaultCategory);
            boolean bDefault = b.equals(defaultCategory);
            boolean aLibrary = a.equals(LIBRARY_CATEGORY);
            boolean bLibrary = b.equals(LIBRARY_CATEGORY);

            if (librariesAtBottom) {
                if (aLibrary && !bLibrary) return 1;
                if (bLibrary && !aLibrary) return -1;
            }

            if (aDefault && !bDefault) return 1;
            if (bDefault && !aDefault) return -1;

            return a.compareTo(b);
        });

        return sorted;
    }

    private boolean matches(ModData mod) {
        if (query.isEmpty()) {
            return true;
        }

        List<ParsedFilter> filters = parseFilters(query);

        for (ParsedFilter filter : filters) {
            if (!matchesSingleFilter(mod, filter)) {
                return false;
            }
        }

        return true;
    }

    private List<ParsedFilter> parseFilters(String text) {
        List<ParsedFilter> filters = new ArrayList<>();
        int i = 0;

        while (i < text.length()) {
            while (i < text.length() && text.charAt(i) == ' ') {
                i++;
            }
            if (i >= text.length()) {
                break;
            }

            char firstChar = text.charAt(i);

            if (isFilterPrefix(firstChar)) {
                i++;
                String value;

                if (i < text.length() && text.charAt(i) == '"') {
                    i++;
                    int start = i;
                    while (i < text.length() && text.charAt(i) != '"') {
                        i++;
                    }
                    value = text.substring(start, i);
                    if (i < text.length()) {
                        i++;
                    }
                } else {
                    int start = i;
                    while (i < text.length() && text.charAt(i) != ' ') {
                        i++;
                    }
                    value = text.substring(start, i);
                }

                filters.add(new ParsedFilter(firstChar, value.toLowerCase(Locale.ROOT)));
            } else {
                String value;

                if (firstChar == '"') {
                    i++;
                    int start = i;
                    while (i < text.length() && text.charAt(i) != '"') {
                        i++;
                    }
                    value = text.substring(start, i);
                    if (i < text.length()) {
                        i++;
                    }
                } else {
                    int start = i;
                    while (i < text.length() && text.charAt(i) != ' ') {
                        i++;
                    }
                    value = text.substring(start, i);
                }

                filters.add(new ParsedFilter('\0', value.toLowerCase(Locale.ROOT)));
            }
        }

        return filters;
    }

    private boolean matchesSingleFilter(ModData mod, ParsedFilter filter) {
        if (filter.value.isEmpty()) {
            return true;
        }

        return switch (filter.type) {
            case '#' -> ModDataManager.category(mod.modId()).toLowerCase(Locale.ROOT).contains(filter.value);
            case '@' -> mod.modId().toLowerCase(Locale.ROOT).contains(filter.value);
            case '!' -> {
                String authors = mod.authors();
                yield authors != null && authors.toLowerCase(Locale.ROOT).contains(filter.value);
            }
            case '$' -> {
                String license = mod.license();
                yield license != null && license.toLowerCase(Locale.ROOT).contains(filter.value);
            }
            default -> mod.displayName().toLowerCase(Locale.ROOT).contains(filter.value);
        };
    }

    private record ParsedFilter(char type, String value) {}

    private boolean isFilterPrefix(char c) {
        return c == '#' || c == '@' || c == '!' || c == '$';
    }

    @Override
    public int getRowWidth() {
        return 340;
    }

    @Override
    protected int getScrollbarPosition() {
        return width / 2 + 180;
    }

    public abstract static class Entry extends ContainerObjectSelectionList.Entry<Entry> {}

    public class CategoryEntry extends Entry {

        private final Component name;
        private final int textWidth;

        public CategoryEntry(Component name) {
            this.name = name;
            this.textWidth = minecraft.font.width(name);
        }

        @Override
        public void render(
                @NotNull GuiGraphics gfx,
                int idx, int top, int left, int w, int h,
                int mx, int my, boolean hover, float pt
        ) {
            int x = ModListWidget.this.width / 2 - textWidth / 2;
            int y = top + h - 10;
            gfx.drawString(minecraft.font, name, x, y, 0xFFFFFF, false);
        }

        @Override
        @Nullable
        public ComponentPath nextFocusPath(@NotNull FocusNavigationEvent e) {
            return null;
        }

        @Override
        @NotNull
        public List<? extends GuiEventListener> children() {
            return Collections.emptyList();
        }

        @Override
        @NotNull
        public List<? extends NarratableEntry> narratables() {
            return ImmutableList.of(new NarratableEntry() {
                @Override
                @NotNull
                public NarrationPriority narrationPriority() {
                    return NarrationPriority.HOVERED;
                }

                @Override
                public void updateNarration(@NotNull NarrationElementOutput out) {
                    out.add(NarratedElementType.TITLE, name);
                }
            });
        }
    }

    public class ModEntry extends Entry {

        private final ModData mod;
        private final String category;
        private final Button configBtn;
        private final Button webBtn;
        private final Button issuesBtn;

        public ModEntry(ModData mod, String category) {
            this.mod = mod;
            this.category = category;

            configBtn = new IconButton(
                    0, 0, IconButton.CONFIG,
                    createConfigTooltip(mod),
                    b -> mod.openConfig(screen)
            );
            configBtn.active = mod.hasConfig();

            webBtn = new IconButton(
                    0, 0, IconButton.WEB,
                    createUrlTooltip(mod.homepage(), "tidymods.gui.website"),
                    b -> openUrl(mod.homepage())
            );
            webBtn.active = mod.homepage() != null;

            issuesBtn = new IconButton(
                    0, 0, IconButton.ISSUES,
                    createUrlTooltip(mod.issueTracker(), "tidymods.gui.issues"),
                    b -> openUrl(mod.issueTracker())
            );
            issuesBtn.active = mod.issueTracker() != null;
        }

        private Component createConfigTooltip(ModData mod) {
            String key = mod.hasConfig()
                    ? "tidymods.gui.config.tooltip"
                    : "tidymods.gui.config.unavailable";
            return Component.translatable(key);
        }

        private Component createUrlTooltip(@Nullable String url, String key) {
            if (url == null) {
                return Component.translatable(key + ".unavailable");
            }
            return Component.translatable(key + ".tooltip")
                    .append("\n")
                    .append(Component.literal(url).withStyle(ChatFormatting.GRAY));
        }

        private void openUrl(@Nullable String url) {
            if (url != null) {
                Util.getPlatform().openUri(url);
            }
        }

        @Override
        public void render(
                @NotNull GuiGraphics gfx,
                int idx, int top, int left, int w, int h,
                int mx, int my, boolean hover, float pt
        ) {
            Font font = minecraft.font;
            int scrollbarX = getScrollbarPosition();
            int y = top + 2;

            boolean hideConfig = TidyModsConfig.hideConfigButtons();

            int issuesX = scrollbarX - 30;
            issuesBtn.setPosition(issuesX, y);
            issuesBtn.render(gfx, mx, my, pt);

            int webX = issuesX - 22;
            webBtn.setPosition(webX, y);
            webBtn.render(gfx, mx, my, pt);

            int buttonsEndX;
            if (hideConfig) {
                buttonsEndX = webX;
            } else {
                int configX = webX - 22;
                configBtn.setPosition(configX, y);
                configBtn.render(gfx, mx, my, pt);
                buttonsEndX = configX;
            }

            renderIcon(gfx, left, top + (h - 16) / 2);

            int textX = left + 24;
            int textY = top + h / 2 - 4;
            int maxWidth = buttonsEndX - textX - 8;

            FormattedCharSequence text = buildDisplayText(maxWidth, font).getVisualOrderText();
            gfx.drawString(font, text, textX, textY, 0xFFFFFF);

            if (hover && mx < buttonsEndX - 4 && !isOverButton(mx, my)) {
                renderTooltip(gfx, mx, my);
            }
        }

        private boolean isOverButton(int mx, int my) {
            if (TidyModsConfig.hideConfigButtons()) {
                return webBtn.isMouseOver(mx, my) || issuesBtn.isMouseOver(mx, my);
            }
            return configBtn.isMouseOver(mx, my)
                    || webBtn.isMouseOver(mx, my)
                    || issuesBtn.isMouseOver(mx, my);
        }

        private void renderTooltip(GuiGraphics gfx, int mx, int my) {

            MutableComponent title = Component.literal(mod.displayName())
                    .withStyle(ChatFormatting.BOLD, ChatFormatting.WHITE);

            String version = mod.version();
            if (version != null && !version.isEmpty()) {
                title.append(Component.literal(" (" + version + ")")
                        .withStyle(ChatFormatting.BOLD, ChatFormatting.GRAY));
            }

            List<FormattedCharSequence> lines = new ArrayList<>(minecraft.font.split(title, TOOLTIP_MAX_WIDTH));

            if (TidyModsConfig.tooltipShowAuthors()) {
                String authors = mod.authors();
                if (authors != null && !authors.isEmpty()) {
                    lines.add(FormattedCharSequence.EMPTY);
                    MutableComponent authorsLine = Component.translatable("tidymods.gui.tooltip.authors")
                            .withStyle(ChatFormatting.GRAY)
                            .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal(authors).withStyle(ChatFormatting.WHITE));
                    lines.addAll(minecraft.font.split(authorsLine, TOOLTIP_MAX_WIDTH));
                }
            }

            if (TidyModsConfig.tooltipShowDescription()) {
                String description = getDescription();
                if (description != null && !description.isBlank()) {
                    lines.add(FormattedCharSequence.EMPTY);
                    lines.addAll(minecraft.font.split(
                            Component.literal(description).withStyle(ChatFormatting.GRAY),
                            TOOLTIP_MAX_WIDTH
                    ));
                }
            }

            if (TidyModsConfig.tooltipShowLicense()) {
                String license = mod.license();
                if (license != null && !license.isEmpty()) {
                    lines.add(FormattedCharSequence.EMPTY);
                    MutableComponent licenseLine = Component.translatable("tidymods.gui.tooltip.license")
                            .withStyle(ChatFormatting.GRAY)
                            .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal(license).withStyle(ChatFormatting.WHITE));
                    lines.addAll(minecraft.font.split(licenseLine, TOOLTIP_MAX_WIDTH));
                }
            }

            if (!lines.isEmpty()) {
                gfx.renderTooltip(minecraft.font, lines, mx, my);
            }
        }

        @Nullable
        private String getDescription() {
            String key = "tidymods.description." + mod.modId();
            if (I18n.exists(key)) {
                return I18n.get(key);
            }
            return mod.description();
        }

        private Component buildDisplayText(int maxWidth, Font font) {
            String name = TidyModsConfig.showModIds() ? mod.modId() : mod.displayName();
            if (name == null || name.isEmpty()) {
                name = mod.modId();
            }

            boolean showVersion = TidyModsConfig.entryShowVersion();
            boolean showAuthors = TidyModsConfig.entryShowAuthors();

            String version = showVersion ? mod.version() : null;
            String authors = showAuthors ? mod.authors() : null;

            MutableComponent full = Component.empty()
                    .append(Component.literal(name).withStyle(ChatFormatting.WHITE));

            if (version != null && !version.isEmpty()) {
                full.append(Component.literal(" (" + version + ")")
                        .withStyle(ChatFormatting.GRAY));
            }

            if (authors != null && !authors.isEmpty()) {
                full.append(Component.translatable("tidymods.gui.by")
                        .withStyle(ChatFormatting.DARK_GRAY));
                full.append(Component.literal(authors)
                        .withStyle(ChatFormatting.GRAY));
            }

            if (font.width(full) <= maxWidth) {
                return full;
            }

            return truncateText(name, version, authors, maxWidth, font);
        }

        private Component truncateText(
                String name,
                @Nullable String version,
                @Nullable String authors,
                int maxWidth,
                Font font
        ) {
            MutableComponent truncated = Component.empty()
                    .append(Component.literal(name).withStyle(ChatFormatting.WHITE));

            if (version != null && !version.isEmpty()) {
                MutableComponent withVersion = truncated.copy()
                        .append(Component.literal(" (" + version + ")")
                                .withStyle(ChatFormatting.GRAY));

                if (font.width(withVersion) + font.width("...") <= maxWidth) {
                    truncated = withVersion;

                    if (authors != null && !authors.isEmpty()) {
                        MutableComponent withAuthors = truncated.copy()
                                .append(Component.translatable("tidymods.gui.by")
                                        .withStyle(ChatFormatting.DARK_GRAY))
                                .append(Component.literal(authors)
                                        .withStyle(ChatFormatting.GRAY));

                        if (font.width(withAuthors) <= maxWidth) {
                            return withAuthors;
                        }
                    }
                }
            }

            if (font.width(truncated) + font.width("...") > maxWidth) {
                String cut = font.plainSubstrByWidth(name, maxWidth - font.width("..."));
                return Component.literal(cut + "...").withStyle(ChatFormatting.WHITE);
            }

            return truncated.append(Component.literal("...").withStyle(ChatFormatting.GRAY));
        }

        private void renderIcon(GuiGraphics gfx, int x, int y) {
            if (TidyModsConfig.useCategoryIcons()) {
                ResourceLocation categoryIcon = TidyModsConfig.categoryIcon(category);
                if (categoryIcon == null) {
                    categoryIcon = TidyModsConfig.categoryIcon("uncategorized");
                }
                if (categoryIcon != null) {
                    blitIcon(gfx, categoryIcon, x, y, 16, 16);
                    return;
                }
            }

            cacheTexture(mod);
            TextureInfo info = TEXTURE_CACHE.get(mod.modId());
            if (info != null) {
                blitIcon(gfx, info.loc, x, y, info.w, info.h);
                return;
            }

            ResourceLocation defaultIcon = TidyModsConfig.defaultModIcon();
            if (defaultIcon != null) {
                blitIcon(gfx, defaultIcon, x, y, 16, 16);
            }
        }

        private void blitIcon(GuiGraphics gfx, ResourceLocation loc, int x, int y, int w, int h) {
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.enableBlend();
            gfx.blit(loc, x, y, 16, 16, 0, 0, w, h, w, h);
            RenderSystem.disableBlend();
        }

        @Override
        @NotNull
        public List<? extends GuiEventListener> children() {
            if (TidyModsConfig.hideConfigButtons()) {
                return ImmutableList.of(webBtn, issuesBtn);
            }
            return ImmutableList.of(configBtn, webBtn, issuesBtn);
        }

        @Override
        @NotNull
        public List<? extends NarratableEntry> narratables() {
            if (TidyModsConfig.hideConfigButtons()) {
                return ImmutableList.of(webBtn, issuesBtn);
            }
            return ImmutableList.of(configBtn, webBtn, issuesBtn);
        }
    }

    private void cacheTexture(ModData mod) {
        if (TEXTURE_CACHE.containsKey(mod.modId())) return;
        TEXTURE_CACHE.put(mod.modId(), null);

        String path = mod.logoFile();
        if (path == null || path.isEmpty()) return;

        ModDataManager.loadImage(mod.modId(), path, img -> {
            DynamicTexture texture = new DynamicTexture(img) {
                @Override
                public void upload() {
                    bind();
                    NativeImage pixels = getPixels();
                    if (pixels != null) {
                        pixels.upload(0, 0, 0, 0, 0,
                                pixels.getWidth(), pixels.getHeight(),
                                mod.smoothLogo(), false, false, false);
                    }
                }
            };
            ResourceLocation loc = minecraft.getTextureManager().register("tidymods_icon", texture);
            TEXTURE_CACHE.put(mod.modId(), new TextureInfo(loc, img.getWidth(), img.getHeight()));
        });
    }

    private record TextureInfo(ResourceLocation loc, int w, int h) {}
}