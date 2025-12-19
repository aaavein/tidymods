package net.aaavein.tidymods.client.gui;

import net.aaavein.tidymods.client.ModDataManager;
import net.aaavein.tidymods.client.gui.widget.AutoCompleteEditBox;
import net.aaavein.tidymods.config.TidyModsConfig;
import net.aaavein.tidymods.data.ModData;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModListScreen extends Screen {

    private static final Component TITLE = Component.translatable("tidymods.gui.title");
    private static final Component SEARCH_HINT = Component.translatable("tidymods.gui.search.hint")
            .withStyle(s -> s.withColor(0x888888));

    private static double savedScrollAmount = 0;
    private static String savedSearchQuery = "";

    private final Screen parent;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 48, 33);
    private final ConfigSnapshot configSnapshot = new ConfigSnapshot();

    private AutoCompleteEditBox searchBox;
    private ModListWidget list;
    private List<String> searchSuggestions;

    public ModListScreen(@NotNull Screen parent) {
        super(TITLE);
        this.parent = parent;
        configSnapshot.capture();
    }

    @Override
    protected void init() {
        buildSearchSuggestions();

        searchBox = new AutoCompleteEditBox(
                font,
                width / 2 - 100,
                22,
                200,
                20,
                Component.translatable("tidymods.gui.search"),
                () -> searchSuggestions
        );
        searchBox.setHint(SEARCH_HINT);
        searchBox.setResponder(s -> {
            if (list != null) list.filter(s);
        });
        searchBox.setValue(savedSearchQuery);
        addWidget(searchBox);

        list = addRenderableWidget(new ModListWidget(this, Minecraft.getInstance()));
        list.filter(savedSearchQuery);
        list.setScrollAmount(savedScrollAmount);

        LinearLayout footer = layout.addToFooter(LinearLayout.horizontal().spacing(8));
        footer.addChild(
                Button.builder(
                        Component.translatable("tidymods.gui.open_mods_folder"),
                        b -> Util.getPlatform().openFile(FMLPaths.MODSDIR.get().toFile())
                ).width(150).build()
        );
        footer.addChild(
                Button.builder(CommonComponents.GUI_DONE, b -> onClose()).width(150).build()
        );

        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    private void buildSearchSuggestions() {
        Set<String> displayNames = new HashSet<>();
        Set<String> modIds = new HashSet<>();
        Set<String> categories = new HashSet<>();
        Set<String> authors = new HashSet<>();
        Set<String> licenses = new HashSet<>();

        for (ModData mod : ModDataManager.all()) {
            if (!TidyModsConfig.isHidden(mod.modId())) {
                displayNames.add(mod.displayName());
                modIds.add(mod.modId());
                categories.add(ModDataManager.category(mod.modId()));

                String authorString = mod.authors();
                if (authorString != null && !authorString.isBlank()) {
                    for (String author : authorString.split(",")) {
                        String trimmed = author.trim();
                        if (!trimmed.isEmpty()) {
                            authors.add(trimmed);
                        }
                    }
                }

                String license = mod.license();
                if (license != null && !license.isBlank()) {
                    licenses.add(license);
                }
            }
        }

        List<String> nameSuggestions = new ArrayList<>(displayNames);
        nameSuggestions.sort(String.CASE_INSENSITIVE_ORDER);
        List<String> suggestions = new ArrayList<>(nameSuggestions);

        List<String> idSuggestions = new ArrayList<>();
        for (String modId : modIds) {
            idSuggestions.add("@" + modId);
        }
        idSuggestions.sort(String.CASE_INSENSITIVE_ORDER);
        suggestions.addAll(idSuggestions);

        List<String> categorySuggestions = new ArrayList<>();
        for (String category : categories) {
            categorySuggestions.add("#" + category);
        }
        categorySuggestions.sort(String.CASE_INSENSITIVE_ORDER);
        suggestions.addAll(categorySuggestions);

        List<String> authorSuggestions = new ArrayList<>();
        for (String author : authors) {
            authorSuggestions.add("!" + author);
        }
        authorSuggestions.sort(String.CASE_INSENSITIVE_ORDER);
        suggestions.addAll(authorSuggestions);

        List<String> licenseSuggestions = new ArrayList<>();
        for (String license : licenses) {
            licenseSuggestions.add("$" + license);
        }
        licenseSuggestions.sort(String.CASE_INSENSITIVE_ORDER);
        suggestions.addAll(licenseSuggestions);

        searchSuggestions = suggestions;
    }

    @Override
    protected void repositionElements() {
        layout.arrangeElements();
        if (list != null) {
            list.updateLayout(width, layout);
        }
        if (searchBox != null) {
            searchBox.setX(width / 2 - 100);
        }
    }

    @Override
    protected void setInitialFocus() {
        setInitialFocus(searchBox);
    }

    @Override
    public void onClose() {
        if (list != null) {
            savedScrollAmount = list.getScrollAmount();
        }
        if (searchBox != null) {
            savedSearchQuery = searchBox.getValue();
        }
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void tick() {
        super.tick();
        if (configSnapshot.hasChanged()) {
            ModDataManager.reloadCategories();
            buildSearchSuggestions();
            if (list != null) {
                list.refresh();
            }
            configSnapshot.capture();
        }
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mx, int my, float pt) {
        super.render(gfx, mx, my, pt);
        gfx.drawCenteredString(font, buildTitle(), width / 2, 8, 0xFFFFFF);
        searchBox.render(gfx, mx, my, pt);
    }

    private Component buildTitle() {
        if (TidyModsConfig.showModCount() && list != null) {
            return Component.translatable("tidymods.gui.title.count", list.getModCount());
        }
        return TITLE;
    }

    private static class ConfigSnapshot {

        private boolean showIds;
        private boolean showCount;
        private boolean configsOnly;
        private boolean categoryIcons;
        private boolean librariesAtBottom;
        private boolean hideConfigButtons;
        private boolean entryShowVersion;
        private boolean entryShowAuthors;
        private boolean tooltipShowAuthors;
        private boolean tooltipShowDescription;
        private boolean tooltipShowLicense;
        private String defaultCategory;
        private int hiddenHash;
        private int overridesHash;

        void capture() {
            showIds = TidyModsConfig.showModIds();
            showCount = TidyModsConfig.showModCount();
            configsOnly = TidyModsConfig.configsOnly();
            categoryIcons = TidyModsConfig.useCategoryIcons();
            librariesAtBottom = TidyModsConfig.librariesAtBottom();
            hideConfigButtons = TidyModsConfig.hideConfigButtons();
            entryShowVersion = TidyModsConfig.entryShowVersion();
            entryShowAuthors = TidyModsConfig.entryShowAuthors();
            tooltipShowAuthors = TidyModsConfig.tooltipShowAuthors();
            tooltipShowDescription = TidyModsConfig.tooltipShowDescription();
            tooltipShowLicense = TidyModsConfig.tooltipShowLicense();
            defaultCategory = TidyModsConfig.defaultCategory();
            hiddenHash = TidyModsConfig.hiddenMods().hashCode();
            overridesHash = TidyModsConfig.categoryOverrides().hashCode();
        }

        boolean hasChanged() {
            return showIds != TidyModsConfig.showModIds()
                    || showCount != TidyModsConfig.showModCount()
                    || configsOnly != TidyModsConfig.configsOnly()
                    || categoryIcons != TidyModsConfig.useCategoryIcons()
                    || librariesAtBottom != TidyModsConfig.librariesAtBottom()
                    || hideConfigButtons != TidyModsConfig.hideConfigButtons()
                    || entryShowVersion != TidyModsConfig.entryShowVersion()
                    || entryShowAuthors != TidyModsConfig.entryShowAuthors()
                    || tooltipShowAuthors != TidyModsConfig.tooltipShowAuthors()
                    || tooltipShowDescription != TidyModsConfig.tooltipShowDescription()
                    || tooltipShowLicense != TidyModsConfig.tooltipShowLicense()
                    || !defaultCategory.equals(TidyModsConfig.defaultCategory())
                    || hiddenHash != TidyModsConfig.hiddenMods().hashCode()
                    || overridesHash != TidyModsConfig.categoryOverrides().hashCode();
        }
    }
}