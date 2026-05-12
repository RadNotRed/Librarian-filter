package com.gbdhapa.neoforge.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.awt.Robot;
import java.awt.event.KeyEvent;

public class NeoForgeCraftingMacro {
    public static KeyMapping macroKey;
    private static int tickCounter = 0;
    private static Robot robot;

    public NeoForgeCraftingMacro() {
        try {
            robot = new Robot();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (client.screen == null || robot == null || macroKey == null) return;

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
    }
}
