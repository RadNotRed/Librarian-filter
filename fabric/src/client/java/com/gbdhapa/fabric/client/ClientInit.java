package com.gbdhapa.fabric.client;

import com.gbdhapa.network.ConfigRequestPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;

public class ClientInit {
    private static KeyMapping OPEN_CONFIG_KEY;

    public static void init() {
        OPEN_CONFIG_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.librarian-filter.open_trade_config",
                InputConstants.KEY_O,
                KeyMapping.Category.GAMEPLAY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null)
                return;

            while (OPEN_CONFIG_KEY.consumeClick()) {
                ClientPlayNetworking.send(new ConfigRequestPayload());
            }
        });
    }
}
