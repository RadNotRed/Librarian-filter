package com.gbdhapa.quilt.client.macro;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import com.gbdhapa.quickcraft.Constants;

public class QuiltCraftingMacro {
    public static KeyMapping macroKey;
    private static int tickCounter = 0;
    private static Robot robot;

    public static void register() {
        macroKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.quick_craft.macro",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V, // Default to V
                KeyMapping.Category.getOrCreate(Constants.KEY_CATEGORY)
        ));

        try {
            robot = new Robot();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.screen == null || robot == null) return;

            // Check if the hotkey is pressed
            if (macroKey.isDown()) {
                tickCounter++;
                if (tickCounter % 2 == 0) {
                    robot.keyPress(KeyEvent.VK_ENTER);
                    robot.keyRelease(KeyEvent.VK_ENTER);
                    
                    robot.keyPress(KeyEvent.VK_Q);
                    robot.keyRelease(KeyEvent.VK_Q);
                }
            } else {
                tickCounter = 0;
            }
        });
    }
}
