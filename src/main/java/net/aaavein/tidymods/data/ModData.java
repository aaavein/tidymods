package net.aaavein.tidymods.data;

import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public interface ModData {

    String modId();

    String displayName();

    String version();

    @Nullable
    String description();

    @Nullable
    String authors();

    @Nullable
    String license();

    @Nullable
    String homepage();

    @Nullable
    String issueTracker();

    @Nullable
    String logoFile();

    @Nullable
    default String declaredCategory() {
        return null;
    }

    boolean hasConfig();

    boolean smoothLogo();

    void openConfig(Screen parent);
}