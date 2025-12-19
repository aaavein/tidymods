package net.aaavein.tidymods.data;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforgespi.language.IModInfo;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record NeoForgeData(IModInfo info) implements ModData {

    private static final String MISSING_DESCRIPTION = "MISSING DESCRIPTION";

    @Override
    public String modId() {
        return info.getModId();
    }

    @Override
    public String displayName() {
        return info.getDisplayName();
    }

    @Override
    public String version() {
        return info.getVersion().toString();
    }

    @Override
    @Nullable
    public String description() {
        String desc = info.getDescription();
        if (desc == null || desc.isBlank() || desc.equals(MISSING_DESCRIPTION)) {
            return null;
        }
        return desc.trim();
    }

    @Override
    @Nullable
    public String authors() {
        return getConfigString("authors");
    }

    @Override
    @Nullable
    public String license() {
        try {
            String license = info.getOwningFile().getLicense();
            if (license != null && !license.isBlank()) {
                return license.trim();
            }
        } catch (Exception ignored) {}
        return null;
    }

    @Override
    @Nullable
    public String homepage() {
        return getConfigString("displayURL");
    }

    @Override
    @Nullable
    public String issueTracker() {
        String url = getConfigString("issueTrackerURL");
        if (url != null) return url;

        try {
            return info.getOwningFile()
                    .getConfig()
                    .<String>getConfigElement("issueTrackerURL")
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    @Nullable
    public String logoFile() {
        return info.getLogoFile().orElse(null);
    }

    @Override
    @Nullable
    public String declaredCategory() {
        return getConfigString("modCategory");
    }

    @Override
    public boolean hasConfig() {
        return getContainer()
                .map(c -> c.getCustomExtension(IConfigScreenFactory.class).isPresent())
                .orElse(false);
    }

    @Override
    public boolean smoothLogo() {
        return info.getLogoBlur();
    }

    @Override
    public void openConfig(Screen parent) {
        getContainer().ifPresent(container ->
                container.getCustomExtension(IConfigScreenFactory.class)
                        .ifPresent(factory ->
                                Minecraft.getInstance().setScreen(factory.createScreen(container, parent))
                        )
        );
    }

    private Optional<? extends ModContainer> getContainer() {
        return ModList.get().getModContainerById(info.getModId());
    }

    @Nullable
    private String getConfigString(String key) {
        try {
            return info.getConfig().<String>getConfigElement(key).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}