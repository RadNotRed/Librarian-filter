package net.gbdhapa;


import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class CountCommand {


    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    net.minecraft.commands.Commands.literal("count")
                            .requires(source -> true)
                            .then(Commands.argument("entity", EntityArgument.entities())
                                    .suggests((ctx, builder) -> {
                                        String input = builder.getRemaining(); // partial text

                                        // Base selector suggestions
                                        if (input.isEmpty() || "@".startsWith(input)) {
                                            builder.suggest("@e");
                                            builder.suggest("@p");
                                            builder.suggest("@a");
                                            builder.suggest("@s");
                                        }

                                        // Entity type suggestions wrapped in selector
                                        BuiltInRegistries.ENTITY_TYPE.keySet().forEach(id -> {
                                            String suggestion = "@e[type=" + id + "]";
                                            if (suggestion.startsWith(input)) {
                                                builder.suggest(suggestion);
                                            }
                                        });

                                        return builder.buildFuture();
                                    })
                                    .executes(ctx -> execute(
                                            ctx.getSource(),
                                            EntityArgument.getEntities(ctx, "entity"),
                                            128
                                    ))
                                    .then(Commands.argument("radius", DoubleArgumentType.doubleArg(1))
                                            .executes(ctx -> execute(
                                                    ctx.getSource(),
                                                    EntityArgument.getEntities(ctx, "entity"),
                                                    DoubleArgumentType.getDouble(ctx, "radius")
                                            ))
                                    )
                            )
            );
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    Commands.literal("count")
                            .requires(source -> true)
                            .executes(ctx -> execute(
                                    ctx.getSource(),
                                    128
                            ))
                            .then(Commands.argument("radius", DoubleArgumentType.doubleArg(1))
                                    .executes(ctx -> execute(
                                            ctx.getSource(),
                                            DoubleArgumentType.getDouble(ctx, "radius")
                                    ))
                            )
            );
        });
    }

    private static int execute(CommandSourceStack source, Collection<? extends Entity> entities, double radius) {
        var level = source.getLevel();
        var center = source.getPosition();

        AABB box = new AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius
        );

        List<Entity> entitiesInRange = level.getEntities(
                (Entity) null,
                box,
                e -> entities.contains(e) && e.distanceToSqr(center) <= radius * radius
        );
        return countEntities(source, radius, entitiesInRange);
    }

    private static void showRangeBorder(CommandSourceStack source, double radius) {
        var level = source.getLevel();
        var center = source.getPosition();

        int points = 80; // smooth circle

        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI * i) / points;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;

            level.sendParticles(
                    ParticleTypes.END_ROD,
                    x, center.y + 0.1, z,
                    1,
                    0, 0, 0,
                    0
            );
        }
    }

    private static int execute(CommandSourceStack source, double radius) {
        var level = source.getLevel();
        var center = source.getPosition();

        AABB box = new AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius
        );

        List<Entity> entitiesInRange = level.getEntities(
                (Entity) null,
                box,
                e -> e.distanceToSqr(center) <= radius * radius
        );
        return countEntities(source, radius, entitiesInRange);
    }

    private static int countEntities(CommandSourceStack source, double radius, List<Entity> entitiesInRange) {
        int count = entitiesInRange.size();
        Map<String, Long> entityCountMap = new TreeMap<>();

        entitiesInRange.forEach(e -> {
            if (e instanceof ItemEntity itemEntity) {
                // total items
                entityCountMap.merge("items", (long) itemEntity.getItem().getCount(), Long::sum);

                // per-item breakdown
                String itemName = "items:" +
                        BuiltInRegistries.ITEM
                                .getKey(itemEntity.getItem().getItem()).getPath();
                entityCountMap.merge(itemName, (long) itemEntity.getItem().getCount(), Long::sum);
            } else {
                String key = e.getType().toShortString();
                entityCountMap.merge(key, 1L, Long::sum);
            }
        });
        if (count > 1) {
            if (source.getEntity() instanceof ServerPlayer player) {
                player.sendSystemMessage(Component.literal("============= ENTITIES COUNT ============="));
                entityCountMap.forEach((entityName, entityCount) -> {
                    player.sendSystemMessage(Component.literal(entityCount + " " + entityName + " within " + (int) radius + " blocks"));
                });
                player.sendSystemMessage(Component.literal("=============================================="));
                showRangeBorder(source, radius);
            } else {
                source.sendSuccess(
                        () -> Component.literal("=============ENTITIES COUNT===================="), false);
                entityCountMap.forEach((entityName, entityCount) -> {
                    source.sendSuccess(() -> Component.literal(entityCount + " " + entityName + " within " + (int) radius + " blocks"), false);
                });
                source.sendSuccess(() -> Component.literal("=============================================="), false);
            }
        } else {
            if (source.getEntity() instanceof ServerPlayer player) {
                player.sendSystemMessage(Component.literal(count + " entities within " + (int) radius + " blocks"));
            } else {
                source.sendSuccess(
                        () -> Component.literal(count + " entities within " + (int) radius + " blocks"), false);
            }

        }
        return count;
    }

}

