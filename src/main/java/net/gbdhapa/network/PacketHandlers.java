package net.gbdhapa.network;


import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.gbdhapa.network.payload.ConfigRequestPayload;
import net.gbdhapa.network.payload.ConfigResponsePayload;
import net.minecraft.server.level.ServerPlayer;

public class PacketHandlers {

    public static void register() {

        ServerPlayNetworking.registerGlobalReceiver(
                ConfigRequestPayload.TYPE,
                (payload, context) -> {
                    ServerPlayer player = context.player();

                    boolean isOp = player.hasPermissions(2);

                    context.responseSender().sendPacket(
                            new ConfigResponsePayload(isOp)
                    );
                }
        );
    }
}