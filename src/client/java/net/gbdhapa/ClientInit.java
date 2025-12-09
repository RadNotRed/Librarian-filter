package net.gbdhapa;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.gbdhapa.network.payload.ConfigRequestPayload;
import net.gbdhapa.network.payload.ConfigResponsePayload;
import net.minecraft.client.KeyMapping;

public class ClientInit {

    public static void init() {
        KeyMapping OPEN_CONFIG_KEY =
                KeyBindingHelper.registerKeyBinding(new KeyMapping(
                        "key.elt.open_trade_config",
                        InputConstants.KEY_O,                   // default key
                        KeyMapping.CATEGORY_GAMEPLAY  // category in keybind menu
                ));
        // receive server response
        ClientPlayNetworking.registerGlobalReceiver(
                ConfigResponsePayload.TYPE,
                (payload, ctx) -> {
                    if (payload.allowed()) {
                        ctx.client().execute(() ->
                                ctx.client().setScreen(new TradeConfigScreen())
                        );
                    }
                }
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            if (client.player == null || client.level == null)
                return; // only in-game

            while (OPEN_CONFIG_KEY.consumeClick()) {
                ClientPlayNetworking.send(new ConfigRequestPayload());
            }
        });
    }

}