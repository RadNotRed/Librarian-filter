package net.gbdhapa.network.payload;


import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ConfigResponsePayload(boolean allowed)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ConfigResponsePayload> TYPE =
            CustomPacketPayload.createType("elt_config_response");

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigResponsePayload> CODEC =
            StreamCodec.of(
                    (buf, value) -> buf.writeBoolean(value.allowed()),
                    buf -> new ConfigResponsePayload(buf.readBoolean())
            );
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}