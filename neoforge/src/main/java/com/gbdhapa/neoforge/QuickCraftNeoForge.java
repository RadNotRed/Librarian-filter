package com.gbdhapa.neoforge;

import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import com.gbdhapa.quickcraft.Constants;
import com.gbdhapa.quickcraft.QuickCraftCommon;
import com.gbdhapa.neoforge.client.NeoForgeCraftingMacro;

@Mod(Constants.MOD_ID)
public class QuickCraftNeoForge {
    public QuickCraftNeoForge(IEventBus modEventBus) {
        QuickCraftCommon.init();
        
        // Register the client setup listener
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::onRegisterKeyMappings);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        // Register the macro on the main NeoForge event bus
        NeoForge.EVENT_BUS.register(new NeoForgeCraftingMacro());
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        NeoForgeCraftingMacro.macroKey = new KeyMapping(
                "key.quick_craft.macro",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                KeyMapping.Category.getOrCreate(Constants.KEY_CATEGORY)
        );
        event.register(NeoForgeCraftingMacro.macroKey);
    }
}
