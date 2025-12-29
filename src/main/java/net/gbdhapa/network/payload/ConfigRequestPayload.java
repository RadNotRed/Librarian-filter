package net.gbdhapa.network.payload;


import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ConfigRequestPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ConfigRequestPayload> TYPE =
            CustomPacketPayload.createType("elt_config_request");

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigRequestPayload> CODEC =
            StreamCodec.of(
                    (buf, value) -> {},          // encode nothing
                    buf -> new ConfigRequestPayload()  // decode empty record
            );
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}