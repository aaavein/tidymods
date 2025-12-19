package net.aaavein.tidymods.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.aaavein.tidymods.TidyMods;
import net.aaavein.tidymods.config.TidyModsConfig;
import net.aaavein.tidymods.data.MinecraftData;
import net.aaavein.tidymods.data.ModData;
import net.aaavein.tidymods.data.NeoForgeData;
import net.neoforged.fml.ModList;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class ModDataManager {

    private static final Map<String, ModData> MODS = new HashMap<>();
    private static final Map<String, String> CATEGORY_OVERRIDES = new HashMap<>();

    private static boolean modsLoaded = false;
    private static boolean categoriesLoaded = false;

    private ModDataManager() {}

    public static Collection<ModData> all() {
        loadMods();
        return MODS.values();
    }

    public static String category(String modId) {
        loadMods();
        loadCategories();
        String id = modId.toLowerCase();

        String override = CATEGORY_OVERRIDES.get(id);
        if (override != null) return override;

        ModData mod = MODS.get(id);
        if (mod != null) {
            String declared = mod.declaredCategory();
            if (declared != null && !declared.isBlank()) {
                return declared.toLowerCase();
            }
        }

        String builtin = BuiltInCategories.get(id);
        if (builtin != null) return builtin;

        return TidyModsConfig.defaultCategory();
    }

    public static boolean hasCategory(String modId) {
        loadMods();
        loadCategories();
        String id = modId.toLowerCase();

        if (CATEGORY_OVERRIDES.containsKey(id)) return true;

        ModData mod = MODS.get(id);
        if (mod != null) {
            String declared = mod.declaredCategory();
            if (declared != null && !declared.isBlank()) {
                return true;
            }
        }

        return BuiltInCategories.has(id);
    }

    public static List<ModData> uncategorized() {
        loadMods();
        loadCategories();

        List<ModData> result = new ArrayList<>();
        for (ModData mod : MODS.values()) {
            if (!hasCategory(mod.modId())) {
                result.add(mod);
            }
        }
        return result;
    }

    public static void reloadCategories() {
        categoriesLoaded = false;
        loadCategories();
    }

    public static void loadImage(String modId, String path, Consumer<NativeImage> callback) {
        try {
            Path file = ModList.get().getModFileById(modId).getFile().findResource(path);
            if (Files.exists(file)) {
                try (InputStream in = Files.newInputStream(file)) {
                    callback.accept(NativeImage.read(in));
                }
            }
        } catch (Exception e) {
            TidyMods.LOG.debug("Failed to load image {} from {}", path, modId);
        }
    }

    private static void loadMods() {
        if (modsLoaded) return;
        modsLoaded = true;

        ModList.get().getMods().forEach(info -> {
            String id = info.getModId().toLowerCase();
            MODS.put(id, new NeoForgeData(info));
        });
        MODS.put("minecraft", new MinecraftData());
    }

    private static void loadCategories() {
        if (categoriesLoaded) return;
        categoriesLoaded = true;

        CATEGORY_OVERRIDES.clear();

        for (String entry : TidyModsConfig.categoryOverrides()) {
            String[] parts = entry.split(";", 2);
            if (parts.length == 2) {
                String id = parts[0].trim().toLowerCase();
                String cat = parts[1].trim().toLowerCase();
                if (!id.isEmpty() && !cat.isEmpty()) {
                    CATEGORY_OVERRIDES.put(id, cat);
                }
            }
        }
    }
}