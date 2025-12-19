package net.aaavein.tidymods.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.aaavein.tidymods.TidyMods;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IconButton extends Button {

    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            TidyMods.ID,
            "textures/gui/icons.png"
    );

    public static final int CONFIG = 0;
    public static final int WEB = 1;
    public static final int ISSUES = 2;

    private static final int SIZE = 20;
    private static final int ICON_SIZE = 10;
    private static final int ICON_OFFSET = 5;
    private static final int TEXTURE_SIZE = 64;

    private final int icon;

    public IconButton(int x, int y, int icon, @Nullable Component tooltip, @NotNull OnPress action) {
        super(x, y, SIZE, SIZE, CommonComponents.EMPTY, action, DEFAULT_NARRATION);
        this.icon = icon;
        if (tooltip != null) {
            setTooltip(Tooltip.create(tooltip));
        }
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics gfx, int mx, int my, float pt) {
        super.renderWidget(gfx, mx, my, pt);

        float brightness = active ? 1.0F : 0.5F;
        RenderSystem.setShaderColor(brightness, brightness, brightness, alpha);
        RenderSystem.enableBlend();

        gfx.blit(
                TEXTURE,
                getX() + ICON_OFFSET,
                getY() + ICON_OFFSET,
                icon * ICON_SIZE,
                0,
                ICON_SIZE,
                ICON_SIZE,
                TEXTURE_SIZE,
                TEXTURE_SIZE
        );

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }
}