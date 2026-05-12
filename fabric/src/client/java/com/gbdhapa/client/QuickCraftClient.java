package com.gbdhapa.client;

import net.fabricmc.api.ClientModInitializer;
import com.gbdhapa.quickcraft.Constants;
import com.gbdhapa.client.macro.CraftingMacro;

public class QuickCraftClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Constants.LOG.info("Initializing Quick Craft Client (Fabric)");
        
        // Force non-headless mode for Robot compatibility
        System.setProperty("java.awt.headless", "false");
        
        CraftingMacro.register();
    }
}
