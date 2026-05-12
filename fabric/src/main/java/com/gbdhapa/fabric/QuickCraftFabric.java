package com.gbdhapa.fabric;

import net.fabricmc.api.ModInitializer;
import com.gbdhapa.quickcraft.QuickCraftCommon;

public class QuickCraftFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        QuickCraftCommon.init();
    }
}
