package net.gbdhapa.network.payload;


import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ConfigRequestPayload() implements CustomPacketPayload {

    public static final Type<ConfigRequestPayload> TYPE =
            new Type<>(ResourceLocation.tryBuild("elt", "config_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigRequestPayload> CODEC =
            StreamCodec.of(
                    (buf, value) -> {},          // encode nothing
                    buf -> new ConfigRequestPayload()  // decode empty record
            );
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}