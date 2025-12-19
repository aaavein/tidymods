package net.aaavein.tidymods.client.event;

import net.aaavein.tidymods.TidyMods;
import net.aaavein.tidymods.client.gui.ModListScreen;
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

    private ScreenEvents() {}

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (screen instanceof TitleScreen || screen instanceof PauseScreen) {
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

    private static void openModList(Screen parent) {
        Minecraft.getInstance().setScreen(new ModListScreen(parent));
    }
}