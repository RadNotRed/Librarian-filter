package com.gbdhapa;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Librarianfilter implements ModInitializer {
	public static final String MOD_ID = "librarian-filter";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		com.gbdhapa.config.TradeConfig.load();
		
		net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(com.gbdhapa.network.TradeConfigSyncPayload.ID, com.gbdhapa.network.TradeConfigSyncPayload.CODEC);
		net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(com.gbdhapa.network.OpenConfigScreenPayload.ID, com.gbdhapa.network.OpenConfigScreenPayload.CODEC);
		net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.serverboundPlay().register(com.gbdhapa.network.TradeConfigUpdatePayload.ID, com.gbdhapa.network.TradeConfigUpdatePayload.CODEC);
		net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.serverboundPlay().register(com.gbdhapa.network.ConfigRequestPayload.ID, com.gbdhapa.network.ConfigRequestPayload.CODEC);

		net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(com.gbdhapa.network.TradeConfigUpdatePayload.ID, (payload, context) -> {
			if (context.player().level().getServer().getPlayerList().isOp(new net.minecraft.server.players.NameAndId(context.player().getGameProfile()))) {
				com.gbdhapa.config.TradeConfig.INSTANCE.enableReroll = payload.enableReroll();
				com.gbdhapa.config.TradeConfig.INSTANCE.enableEachLevelReroll = payload.enableEachLevelReroll();
				com.gbdhapa.config.TradeConfig.save();
				
				// Sync to all clients
				com.gbdhapa.network.TradeConfigSyncPayload syncPayload = new com.gbdhapa.network.TradeConfigSyncPayload(payload.enableReroll(), payload.enableEachLevelReroll());
				for (net.minecraft.server.level.ServerPlayer player : context.player().level().getServer().getPlayerList().getPlayers()) {
					net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, syncPayload);
				}
			}
		});

		net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(com.gbdhapa.network.ConfigRequestPayload.ID, (payload, context) -> {
			if (context.player().level().getServer().getPlayerList().isOp(new net.minecraft.server.players.NameAndId(context.player().getGameProfile()))) {
				net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(context.player(), new com.gbdhapa.network.OpenConfigScreenPayload(
					com.gbdhapa.config.TradeConfig.INSTANCE.enableReroll,
					com.gbdhapa.config.TradeConfig.INSTANCE.enableEachLevelReroll
				));
			}
		});

		net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(net.minecraft.commands.Commands.literal("reroll")
				.requires(source -> {
					try {
						return source.getServer().getPlayerList().isOp(new net.minecraft.server.players.NameAndId(source.getPlayerOrException().getGameProfile()));
					} catch (Exception e) {
						return false;
					}
				})
				.then(net.minecraft.commands.Commands.literal("config")
					.executes(context -> {
						net.minecraft.server.level.ServerPlayer player = context.getSource().getPlayerOrException();
						net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new com.gbdhapa.network.OpenConfigScreenPayload(
								com.gbdhapa.config.TradeConfig.INSTANCE.enableReroll,
								com.gbdhapa.config.TradeConfig.INSTANCE.enableEachLevelReroll
						));
						return 1;
					})
				)
			);
		});

		new RerollHelper();
	}
}