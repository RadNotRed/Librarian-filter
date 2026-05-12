package com.gbdhapa.client.macro;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import com.gbdhapa.quickcraft.Constants;

public class CraftingMacro {
    public static KeyMapping macroKey;
    public static boolean isKeyDown = false;
    private static int tickCounter = 0;
    private static Robot robot;
    private static boolean isActive = false;

    public static void register() {
        macroKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.quick_craft.macro",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                KeyMapping.Category.MISC
        ));

        try {
            robot = new Robot();
            Constants.LOG.info("[QuickCraft] Robot initialized successfully.");
        } catch (Exception e) {
            Constants.LOG.error("[QuickCraft] Failed to initialize Robot! Macro will not work. Error: " + e.getMessage());
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || robot == null) {
                if (isActive) stopMacro(client);
                return;
            }

            if (isKeyDown) {
                long now = System.currentTimeMillis();
                if (!isActive) {
                    startMacro(client);
                }

                tickCounter++;
                if (tickCounter % 2 == 0) {
                    // Log timing to system log
                    // Constants.LOG.info("[Macro Timing] [" + now + "] SIMULATING: ENTER + Q");
                    
                    robot.keyPress(KeyEvent.VK_ENTER);
                    robot.keyRelease(KeyEvent.VK_ENTER);
                    robot.keyPress(KeyEvent.VK_Q);
                    robot.keyRelease(KeyEvent.VK_Q);
                }
            } else if (isActive) {
                stopMacro(client);
            }
        });
    }

    private static void startMacro(Minecraft client) {
        isActive = true;
        tickCounter = 0;
        // Constants.LOG.info("[QuickCraft] Macro Started (Holding Ctrl+Shift)");
        
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_SHIFT);
    }

    private static void stopMacro(Minecraft client) {
        isActive = false;
        // Constants.LOG.info("[QuickCraft] Macro Stopped");
        
        robot.keyRelease(KeyEvent.VK_CONTROL);
        robot.keyRelease(KeyEvent.VK_SHIFT);
    }
}
