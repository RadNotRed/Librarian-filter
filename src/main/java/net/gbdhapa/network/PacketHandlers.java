package net.gbdhapa.network;


import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.gbdhapa.network.payload.ConfigRequestPayload;
import net.gbdhapa.network.payload.ConfigResponsePayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

public class PacketHandlers {

    public static void register() {

        ServerPlayNetworking.registerGlobalReceiver(
                ConfigRequestPayload.TYPE,
                (payload, context) -> {
                    ServerPlayer player = context.player();
                    if (player == null) return; // guard against null player

                    boolean isOp = player.permissions().hasPermission(Permissions.COMMANDS_ADMIN);

                    context.responseSender().sendPacket(
                            new ConfigResponsePayload(isOp)
                    );
                }
        );
    }
}