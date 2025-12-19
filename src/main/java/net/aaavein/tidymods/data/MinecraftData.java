package net.aaavein.tidymods.data;

import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import org.jetbrains.annotations.Nullable;

public record MinecraftData() implements ModData {

    private static final String MOD_ID = "minecraft";
    private static final String DISPLAY_NAME = "Minecraft";
    private static final String DESCRIPTION = "The base game.";
    private static final String AUTHORS = "Mojang Studios";
    private static final String LICENSE = "Proprietary";
    private static final String HOMEPAGE = "https://www.minecraft.net";
    private static final String ISSUE_TRACKER = "https://bugs.mojang.com/projects/MC/issues";

    @Override
    public String modId() {
        return MOD_ID;
    }

    @Override
    public String displayName() {
        return DISPLAY_NAME;
    }

    @Override
    public String version() {
        return SharedConstants.getCurrentVersion().getName();
    }

    @Override
    public String description() {
        return DESCRIPTION;
    }

    @Override
    public String authors() {
        return AUTHORS;
    }

    @Override
    public String license() {
        return LICENSE;
    }

    @Override
    public String homepage() {
        return HOMEPAGE;
    }

    @Override
    public String issueTracker() {
        return ISSUE_TRACKER;
    }

    @Override
    @Nullable
    public String logoFile() {
        return null;
    }

    @Override
    public boolean hasConfig() {
        return true;
    }

    @Override
    public boolean smoothLogo() {
        return true;
    }

    @Override
    public void openConfig(Screen parent) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new OptionsScreen(parent, mc.options));
    }
}