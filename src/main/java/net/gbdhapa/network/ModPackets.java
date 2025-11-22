package net.gbdhapa.network;


import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.gbdhapa.network.payload.ConfigRequestPayload;
import net.gbdhapa.network.payload.ConfigResponsePayload;

public class ModPackets {
    public static void register() {

        PayloadTypeRegistry.playC2S().register(
                ConfigRequestPayload.TYPE,
                ConfigRequestPayload.CODEC
        );

        PayloadTypeRegistry.playS2C().register(
                ConfigResponsePayload.TYPE,
                ConfigResponsePayload.CODEC
        );
    }
}