package com.gbdhapa.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ConfigRequestPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ConfigRequestPayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("librarian-filter", "config_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigRequestPayload> CODEC = StreamCodec.unit(new ConfigRequestPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
