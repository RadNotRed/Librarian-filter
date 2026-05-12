package com.gbdhapa.quilt.client;

import net.fabricmc.api.ClientModInitializer;
import com.gbdhapa.quickcraft.Constants;
import com.gbdhapa.quilt.client.macro.QuiltCraftingMacro;

public class QuickCraftQuiltClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Constants.LOG.info("Initializing " + Constants.MOD_NAME + " Client (Quilt)");
        QuiltCraftingMacro.register();
    }
}
