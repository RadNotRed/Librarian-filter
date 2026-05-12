package com.gbdhapa.client.mixin;

import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.gbdhapa.quickcraft.Constants;
import com.gbdhapa.client.macro.CraftingMacro;
import org.lwjgl.glfw.GLFW;

@Mixin(KeyboardHandler.class)
public class KeyboardMixin {
    @Inject(method = "keyPress", at = @At("HEAD"))
    private void onKey(long window, int action, net.minecraft.client.input.KeyEvent event, CallbackInfo ci) {
        String actionName = action == 1 ? "PRESSED" : (action == 0 ? "RELEASED" : "HELD");
        
        int key = event.key();
        int modifiers = event.modifiers();

        // Direct hook to update macro state
        if (key == GLFW.GLFW_KEY_V) {
            if (action == 1) { // PRESSED
                CraftingMacro.isKeyDown = true;
            } else if (action == 0) { // RELEASED
                CraftingMacro.isKeyDown = false;
            }
        }

        // Try to get a human-readable name for the key
        String keyName = String.valueOf(key);
        try {
            keyName = com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString();
        } catch (Exception e) {}
        
        // Log to console for debugging
        // Constants.LOG.info("[KeyLogger] Key: " + keyName + " (ID: " + key + "), Action: " + actionName + ", Modifiers: " + modifiers);
    }
}
