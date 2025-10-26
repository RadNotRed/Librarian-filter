package com.example;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.world.World;

public class ServerBeaconChecker {

    private static int tickCounter = 0;

    public static void register() {
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter < 100) return; // 20 ticks ~ 1 second
            tickCounter = 0;

            for (ServerWorld world : server.getWorlds()) {
                for (ServerPlayerEntity player : world.getPlayers()) {
                    BlockPos playerPos = player.getBlockPos();
                    int radius = 256; // scan radius around player

                    for (int dx = -radius; dx <= radius; dx++) {
                        for (int dy = -radius; dy <= radius; dy++) {
                            for (int dz = -radius; dz <= radius; dz++) {
                                BlockPos checkPos = playerPos.add(dx, dy, dz);
                                if (world.getBlockState(checkPos).getBlock() == Blocks.BEACON) {
                                    if (world.getBlockEntity(checkPos) instanceof BeaconBlockEntity beacon) {
                                        int layers = getBeaconLevels(world, checkPos);
                                        if (layers >= 5) {
                                            player.getStatusEffects().forEach(entry -> {
                                                if (isValidStatusEffect(entry.getEffectType()) && entry.getDuration() >= 10) {
                                                    player.addStatusEffect(new StatusEffectInstance(
                                                            entry.getEffectType(),
                                                            300,  // 15 seconds duration (20 ticks/sec)
                                                            layers - 3    // amplifier 2 = Haste III
                                                    ));
                                                }
                                            });
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    /**
     * Returns the number of full beacon layers (0–4+).
     * Only counts layers where the square is complete and valid.
     */
    private static int getBeaconLevels(World world, BlockPos beaconPos) {
        int level = 0;

        // Check up to 4 layers beneath the beacon
        for (int y = 1; y <= 100; y++) {
            int size = y + 1; // layer size (1 -> 3x3, 2 -> 5x5, etc.)
            boolean fullLayer = true;

            for (int dx = -y; dx <= y; dx++) {
                for (int dz = -y; dz <= y; dz++) {
                    BlockPos checkPos = beaconPos.down(y).add(dx, 0, dz);
                    if (!isValidBlock(world.getBlockState(checkPos).getBlock())) {
                        fullLayer = false;
                        break;
                    }
                }
                if (!fullLayer) break;
            }

            if (fullLayer) level++;
            else break; // stop at first incomplete layer
        }

        return level;
    }


    private static final Block[] VALID_BEACON_BLOCKS = {
            Blocks.IRON_BLOCK,
            Blocks.GOLD_BLOCK,
            Blocks.DIAMOND_BLOCK,
            Blocks.EMERALD_BLOCK,
            Blocks.NETHERITE_BLOCK
    };

    private static boolean isValidBlock(Block block) {
        for (Block b : VALID_BEACON_BLOCKS) {
            if (block == b) return true;
        }
        return false;
    }

    private static final StatusEffect[] VALID_STATUS_EFFECT = {
            StatusEffects.HASTE,
            StatusEffects.REGENERATION,
            StatusEffects.RESISTANCE,
            StatusEffects.SPEED,
            StatusEffects.JUMP_BOOST,
            StatusEffects.STRENGTH,
    };

    private static boolean isValidStatusEffect(StatusEffect statusEffect) {
        for (StatusEffect b : VALID_STATUS_EFFECT) {
            if (statusEffect == b) return true;
        }
        return false;
    }
}
