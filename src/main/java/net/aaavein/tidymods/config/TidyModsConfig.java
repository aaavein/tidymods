package net.aaavein.tidymods.config;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TidyModsConfig {

    public static final ModConfigSpec SPEC;
    private static final Config CONFIG;

    private static final Map<String, ResourceLocation> ICON_CACHE = new HashMap<>();
    private static final Set<String> HIDDEN_CACHE = new HashSet<>();
    private static ResourceLocation defaultIconCache = null;
    private static boolean cacheValid = false;

    static {
        Pair<Config, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(Config::new);
        CONFIG = pair.getLeft();
        SPEC = pair.getRight();
    }

    private TidyModsConfig() {}

    public static boolean hideRealmsButton() {
        return CONFIG.hideRealmsButton.get();
    }

    public static boolean showModCount() {
        return CONFIG.showModCount.get();
    }

    public static boolean showModIds() {
        return CONFIG.showModIds.get();
    }

    public static boolean entryShowVersion() {
        return CONFIG.entryShowVersion.get();
    }

    public static boolean entryShowAuthors() {
        return CONFIG.entryShowAuthors.get();
    }

    public static boolean hideConfigButtons() {
        return CONFIG.hideConfigButtons.get();
    }

    public static boolean tooltipShowAuthors() {
        return CONFIG.tooltipShowAuthors.get();
    }

    public static boolean tooltipShowDescription() {
        return CONFIG.tooltipShowDescription.get();
    }

    public static boolean tooltipShowLicense() {
        return CONFIG.tooltipShowLicense.get();
    }

    public static boolean configsOnly() {
        return CONFIG.configsOnly.get();
    }

    public static List<? extends String> hiddenMods() {
        return CONFIG.hiddenMods.get();
    }

    public static boolean isHidden(String modId) {
        ensureCached();
        return HIDDEN_CACHE.contains(modId.toLowerCase());
    }

    public static String defaultCategory() {
        return CONFIG.defaultCategory.get().toLowerCase();
    }

    public static boolean librariesAtBottom() {
        return CONFIG.librariesAtBottom.get();
    }

    public static List<? extends String> categoryOverrides() {
        return CONFIG.categoryOverrides.get();
    }

    public static boolean useCategoryIcons() {
        return CONFIG.useCategoryIcons.get();
    }

    public static ResourceLocation categoryIcon(String category) {
        ensureCached();
        return ICON_CACHE.get(category.toLowerCase());
    }

    public static ResourceLocation defaultModIcon() {
        ensureCached();
        return defaultIconCache;
    }

    public static boolean printUncategorized() {
        return CONFIG.printUncategorized.get();
    }

    public static void clearCaches() {
        cacheValid = false;
    }

    private static void ensureCached() {
        if (cacheValid) return;
        cacheValid = true;

        ICON_CACHE.clear();
        for (String entry : CONFIG.categoryIcons.get()) {
            String[] parts = entry.split(";", 2);
            if (parts.length == 2) {
                ResourceLocation loc = ResourceLocation.tryParse(parts[1].trim());
                if (loc != null) {
                    ICON_CACHE.put(parts[0].trim().toLowerCase(), loc);
                }
            }
        }

        HIDDEN_CACHE.clear();
        for (String modId : CONFIG.hiddenMods.get()) {
            String trimmed = modId.trim().toLowerCase();
            if (!trimmed.isEmpty()) {
                HIDDEN_CACHE.add(trimmed);
            }
        }

        defaultIconCache = ResourceLocation.tryParse(CONFIG.defaultModIcon.get());
    }

    private static boolean isValidString(Object obj) {
        return obj instanceof String;
    }

    private static boolean isValidMapping(Object obj) {
        return obj instanceof String s && s.contains(";");
    }

    private static class Config {

        final ModConfigSpec.BooleanValue hideRealmsButton;

        final ModConfigSpec.BooleanValue showModCount;
        final ModConfigSpec.BooleanValue showModIds;

        final ModConfigSpec.BooleanValue entryShowVersion;
        final ModConfigSpec.BooleanValue entryShowAuthors;
        final ModConfigSpec.BooleanValue hideConfigButtons;

        final ModConfigSpec.BooleanValue tooltipShowAuthors;
        final ModConfigSpec.BooleanValue tooltipShowDescription;
        final ModConfigSpec.BooleanValue tooltipShowLicense;

        final ModConfigSpec.BooleanValue configsOnly;
        final ModConfigSpec.ConfigValue<List<? extends String>> hiddenMods;

        final ModConfigSpec.ConfigValue<String> defaultCategory;
        final ModConfigSpec.BooleanValue librariesAtBottom;
        final ModConfigSpec.ConfigValue<List<? extends String>> categoryOverrides;

        final ModConfigSpec.BooleanValue useCategoryIcons;
        final ModConfigSpec.ConfigValue<List<? extends String>> categoryIcons;
        final ModConfigSpec.ConfigValue<String> defaultModIcon;

        final ModConfigSpec.BooleanValue printUncategorized;

        Config(ModConfigSpec.Builder builder) {

            builder.push("display");

            showModCount = builder
                    .comment(" Shows the total number of loaded mods in the screen title.")
                    .define("show_mod_count", true);

            showModIds = builder
                    .comment(" Displays technical mod IDs instead of human-readable names.")
                    .define("show_mod_ids", false);

            hideRealmsButton = builder
                    .comment(" Hides the Realms button on the title screen.")
                    .define("hide_realms_button", true);

            builder.pop();

            builder.push("entries");

            entryShowVersion = builder
                    .comment(" Shows the version number next to each mod name.")
                    .define("entry_show_version", true);

            entryShowAuthors = builder
                    .comment(" Shows the author names next to each mod name.")
                    .define("entry_show_authors", false);

            hideConfigButtons = builder
                    .comment(" Hides the configuration buttons.")
                    .define("hide_config_buttons", false);

            builder.pop();

            builder.push("tooltips");

            tooltipShowAuthors = builder
                    .comment(" Includes author information in the mod tooltip.")
                    .define("tooltip_show_authors", true);

            tooltipShowDescription = builder
                    .comment(" Includes the mod description in the mod tooltip.")
                    .define("tooltip_show_description", true);

            tooltipShowLicense = builder
                    .comment(" Includes license information in the mod tooltip.")
                    .define("tooltip_show_license", true);

            builder.pop();

            builder.push("filtering");

            configsOnly = builder
                    .comment(" Only shows mods that have a configuration screen.")
                    .define("configs_only", false);

            hiddenMods = builder
                    .comment(
                            " List of mod IDs to hide from the mod list.",
                            " Format: modid"
                    )
                    .defineListAllowEmpty(
                            "hidden_mods",
                            List.of(),
                            () -> "",
                            TidyModsConfig::isValidString
                    );

            builder.pop();

            builder.push("categories");

            defaultCategory = builder
                    .comment(
                            " Category assigned to mods without a predefined category.",
                            " This category always appears at the bottom of the list."
                    )
                    .define("default_category", "uncategorized");

            librariesAtBottom = builder
                    .comment(" Places the libraries category at the bottom, below the default category.")
                    .define("libraries_at_bottom", true);

            categoryOverrides = builder
                    .comment(
                            " Custom category assignments that override built-in defaults.",
                            " Format: modid;category"
                    )
                    .defineListAllowEmpty(
                            "category_overrides",
                            List.of(),
                            () -> "",
                            TidyModsConfig::isValidMapping
                    );

            builder.pop();

            builder.push("icons");

            useCategoryIcons = builder
                    .comment(" Uses a shared icon for all mods in each category instead of individual mod icons.")
                    .define("use_category_icons", false);

            categoryIcons = builder
                    .comment(
                            " Texture mappings for category icons.",
                            " Format: category;namespace:path/to/texture.png"
                    )
                    .defineListAllowEmpty(
                            "category_icons",
                            List.of(
                                    "library;minecraft:textures/item/book.png",
                                    "technology;minecraft:textures/item/copper_ingot.png",
                                    "magic;minecraft:textures/item/blaze_powder.png",
                                    "agriculture;minecraft:textures/item/wheat.png",
                                    "worldgen;minecraft:textures/block/oak_sapling.png",
                                    "mobs;minecraft:textures/item/egg.png",
                                    "equipment;minecraft:textures/item/iron_sword.png",
                                    "storage;minecraft:textures/item/chest_minecart.png",
                                    "building;minecraft:textures/item/brick.png",
                                    "utility;minecraft:textures/item/redstone.png",
                                    "system;minecraft:textures/item/feather.png",
                                    "functional;minecraft:textures/item/compass_28.png",
                                    "uncategorized;minecraft:textures/item/paper.png"
                            ),
                            () -> "",
                            TidyModsConfig::isValidMapping
                    );

            defaultModIcon = builder
                    .comment(" Fallback texture for mods without an icon. Used when category icons are disabled.")
                    .define("default_mod_icon", "minecraft:textures/misc/unknown_server.png");

            builder.pop();

            builder.push("debug");

            printUncategorized = builder
                    .comment(" Logs all uncategorized mods to the console during startup.")
                    .define("print_uncategorized", false);

            builder.pop();
        }
    }
}