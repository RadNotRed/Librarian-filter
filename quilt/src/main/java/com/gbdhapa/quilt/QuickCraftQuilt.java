package com.gbdhapa.quilt;

import net.fabricmc.api.ModInitializer;
import com.gbdhapa.quickcraft.QuickCraftCommon;

public class QuickCraftQuilt implements ModInitializer {
    @Override
    public void onInitialize() {
        QuickCraftCommon.init();
    }
}
