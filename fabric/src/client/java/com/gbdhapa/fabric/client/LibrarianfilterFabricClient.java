package com.gbdhapa.fabric.client;

import com.gbdhapa.config.TradeConfig;
import com.gbdhapa.fabric.client.gui.FabricTradeConfigScreen;
import com.gbdhapa.network.OpenConfigScreenPayload;
import com.gbdhapa.network.TradeConfigSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class LibrarianfilterFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientInit.init();
        
        ClientPlayNetworking.registerGlobalReceiver(TradeConfigSyncPayload.ID, (payload, context) -> {
            TradeConfig.INSTANCE.enableReroll = payload.enableReroll();
            TradeConfig.INSTANCE.enableEachLevelReroll = payload.enableEachLevelReroll();
        });

        ClientPlayNetworking.registerGlobalReceiver(OpenConfigScreenPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                context.client().setScreen(new FabricTradeConfigScreen(payload.enableReroll(), payload.enableEachLevelReroll()));
            });
        });
    }
}
