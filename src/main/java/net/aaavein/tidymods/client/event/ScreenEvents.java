package net.aaavein.tidymods.client.event;

import net.aaavein.tidymods.TidyMods;
import net.aaavein.tidymods.client.gui.ModListScreen;
import net.aaavein.tidymods.config.TidyModsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = TidyMods.ID, value = Dist.CLIENT)
public final class ScreenEvents {

    private static final Component MODS_TEXT = Component.translatable("fml.menu.mods");
    private static final Component REALMS_TEXT = Component.translatable("menu.online");
    private static final int BUTTON_SPACING = 4;
    private static final int BOTTOM_BUTTON_MARGIN = 50;

    private ScreenEvents() {}

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();

        if (screen instanceof TitleScreen) {
            if (TidyModsConfig.hideRealmsButton()) {
                replaceRealmsWithMods(screen, event);
            } else {
                replaceModsButton(screen, event);
            }
        } else if (screen instanceof PauseScreen) {
            replaceModsButton(screen, event);
        }
    }

    private static void replaceModsButton(Screen screen, ScreenEvent.Init.Post event) {
        String targetText = MODS_TEXT.getString();

        for (var listener : event.getListenersList()) {
            if (listener instanceof Button btn && btn.getMessage().getString().equals(targetText)) {
                event.removeListener(btn);
                event.addListener(
                        Button.builder(MODS_TEXT, b -> openModList(screen))
                                .bounds(btn.getX(), btn.getY(), btn.getWidth(), btn.getHeight())
                                .build()
                );
                return;
            }
        }
    }

    private static void replaceRealmsWithMods(Screen screen, ScreenEvent.Init.Post event) {
        String realmsText = REALMS_TEXT.getString();
        String modsText = MODS_TEXT.getString();

        Button realmsBtn = null;
        Button modsBtn = null;

        for (var listener : event.getListenersList()) {
            if (listener instanceof Button btn) {
                String text = btn.getMessage().getString();
                if (text.equals(realmsText)) {
                    realmsBtn = btn;
                } else if (text.equals(modsText)) {
                    modsBtn = btn;
                }
            }
        }

        if (realmsBtn == null) {
            replaceModsButton(screen, event);
            return;
        }

        int realmsX = realmsBtn.getX();
        int realmsY = realmsBtn.getY();
        int realmsW = realmsBtn.getWidth();
        int realmsH = realmsBtn.getHeight();

        event.removeListener(realmsBtn);

        int newModsY = realmsY;

        if (modsBtn != null) {
            int modsY = modsBtn.getY();
            event.removeListener(modsBtn);

            if (modsY != realmsY) {
                int halfRowHeight = (realmsH + BUTTON_SPACING) / 2;
                int bottomThreshold = screen.height - BOTTOM_BUTTON_MARGIN;

                for (var listener : event.getListenersList()) {
                    if (listener instanceof Button btn) {
                        int btnY = btn.getY();

                        if (btnY > bottomThreshold) {
                            continue;
                        }

                        if (btnY < modsY) {
                            btn.setY(btnY + halfRowHeight);
                        } else if (btnY > modsY) {
                            btn.setY(btnY - halfRowHeight);
                        }
                    }
                }

                if (realmsY < modsY) {
                    newModsY = realmsY + halfRowHeight;
                } else {
                    newModsY = realmsY - halfRowHeight;
                }
            }
        }

        event.addListener(
                Button.builder(MODS_TEXT, b -> openModList(screen))
                        .bounds(realmsX, newModsY, realmsW, realmsH)
                        .build()
        );
    }

    private static void openModList(Screen parent) {
        Minecraft.getInstance().setScreen(new ModListScreen(parent));
    }
}