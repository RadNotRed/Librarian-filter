package net.gbdhapa;


import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CountCommand {


    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            EntityType<Villager> type = EntityType.VILLAGER;
            dispatcher.register(
                    net.minecraft.commands.Commands.literal("count")
                            .then(Commands.argument("entity", EntityArgument.entities())
                                    .suggests((ctx, builder) -> {
                                        BuiltInRegistries.ENTITY_TYPE.keySet().forEach(id -> {
                                            builder.suggest("@e[type=" + id + "]");
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
        int count = entitiesInRange.size();
        Map<String, Long> entityCountMap =
                entitiesInRange.stream()
                        .collect(Collectors.groupingBy(
                                e -> e.getType().toShortString(),
                                Collectors.counting()
                        ));
        if (count > 1) {
            source.sendSuccess(
                    () -> Component.literal("=============ENTITIES COUNT===================="),
                    false
            );
            entityCountMap.forEach((entityName, entityCount)->{
                source.sendSuccess(
                        () -> Component.literal(entityCount + " " + entityName + "s within " + (int) radius + " blocks"),
                        false
                );
            });
            source.sendSuccess(
                    () -> Component.literal("=============================================="),
                    false
            );

            showRangeBorder(source, radius);


        } else {
            source.sendSuccess(
                    () -> Component.literal(count + " entities within " + (int) radius + " blocks"),
                    false
            );
        }


        return count;
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

}

