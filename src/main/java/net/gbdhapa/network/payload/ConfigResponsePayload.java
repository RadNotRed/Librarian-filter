package net.gbdhapa.network.payload;


import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ConfigResponsePayload(boolean allowed)
        implements CustomPacketPayload {

    public static final Type<ConfigResponsePayload> TYPE =
            new Type<>(ResourceLocation.tryBuild("elt", "config_response"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigResponsePayload> CODEC =
            StreamCodec.of(
                    (buf, value) -> buf.writeBoolean(value.allowed()),
                    buf -> new ConfigResponsePayload(buf.readBoolean())
            );
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}