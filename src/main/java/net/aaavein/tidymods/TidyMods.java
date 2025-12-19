package net.aaavein.tidymods;

import net.aaavein.tidymods.client.ModDataManager;
import net.aaavein.tidymods.config.TidyModsConfig;
import net.aaavein.tidymods.data.ModData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Mod(TidyMods.ID)
public class TidyMods {

    public static final String ID = "tidymods";
    public static final Logger LOG = LoggerFactory.getLogger("tidymods");

    public TidyMods(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, TidyModsConfig.SPEC, "tidymods.toml");
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        bus.addListener(this::onConfigReload);
    }

    private void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == TidyModsConfig.SPEC) {
            TidyModsConfig.clearCaches();
            ModDataManager.reloadCategories();
        }
    }

    @EventBusSubscriber(modid = ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientSetup {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            if (!TidyModsConfig.printUncategorized()) return;

            List<ModData> uncategorized = ModDataManager.uncategorized();
            if (uncategorized.isEmpty()) {
                LOG.info("All mods have categories assigned.");
                return;
            }

            LOG.info("Uncategorized mods ({}):", uncategorized.size());
            for (ModData mod : uncategorized) {
                LOG.info("  - {}", mod.modId());
            }
        }
    }
}