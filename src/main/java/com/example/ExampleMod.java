package com.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ExampleMod implements ModInitializer {
    public static final String MOD_ID = "elt";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final HashMap<UUID, Map<BlockPos, Long>> cooldownMap = new HashMap<>();
    private static final long COOLDOWN_TIME = 1000; // in milliseconds
    private static final int VILLAGER_SEARCH_RADIUS = 128; //in blocks
    private static final int MAX_REROLL_COUNT = 10000; //in blocks
    private static final int durationTicks = 5; //in ticks


    @Override
    public void onInitialize() {
        LOGGER.info("Mod initialized!");

        registerEvent();
    }

    private void registerEvent() {
        // Register the event to listen for right-click interactions
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            BlockPos clickedPos = hitResult.getBlockPos();
            UUID playerUUID = player.getUUID();
            long currentTime = System.currentTimeMillis();
            // Check if the player is still on cooldown
            if (cooldownMap.containsKey(playerUUID)) {
                Long lastClickTime = cooldownMap.get(playerUUID).get(clickedPos);
                if (lastClickTime != null) {
                    long difference = currentTime - lastClickTime;
                    if (difference < COOLDOWN_TIME) {
                        return InteractionResult.PASS;  // Prevents further execution
                    }
                }

            }
            Block blockClicked = world.getBlockState(clickedPos).getBlock();
            List<String> signTexts = getSignTexts(world, blockClicked, clickedPos);
            List<EnchFilter> filters = getEnchFilters(signTexts);
            if (!filters.isEmpty()) {
                Villager villager = getVillagerForLectern(player, world, clickedPos, filters);
                if (villager != null) {
                    FilterResult filterResult = filterTrade(player, world, villager, filters);
                    spawnParticles(world, filterResult, villager, clickedPos);
                    cooldownMap.put(playerUUID, Map.of(clickedPos, currentTime));
                }
            }
            return InteractionResult.PASS; // Continue normal behavior for other blocks
        });
    }

    private List<EnchFilter> getEnchFilters(List<String> signTexts) {
        List<EnchFilter> filters = new ArrayList<>();
        if (signTexts != null) {
            for (String line : signTexts) {
                if (line != null && !line.isEmpty()) {
                    String[] filterText = line.trim().split(" ");
                    if (filterText.length > 1) {
                        if (StringUtils.isNumeric(filterText[1])) {
                            int enchLevel = Integer.parseInt(filterText[1]);
                            if (enchLevel > 0) {
                                if (filterText.length > 2 && StringUtils.isNumeric(filterText[2])) {
                                    int price = Integer.parseInt(filterText[2]);
                                    filters.add(new EnchFilter(filterText[0], enchLevel, price));
                                } else {
                                    filters.add(new EnchFilter(filterText[0], enchLevel, 0));
                                }
                            }
                        }
                    } else {
                        filters.add(new EnchFilter(filterText[0], 0, 0));
                    }
                }
            }
        }

        return filters;
    }

    private List<String> getSignTexts(Level world, Block blockClicked, BlockPos clickedPos) {

        // Check if the block clicked is a lectern
        if (blockClicked == Blocks.LECTERN) {
            // Get the lectern's facing direction
            Direction facingDirection = world.getBlockState(clickedPos).getValue(LecternBlock.FACING);

            // Get the position in front of the lectern (based on its facing direction)
            BlockPos signPos = clickedPos.relative(facingDirection);
            // Get the BlockEntity (SignBlockEntity) of the WallSign
            if (world.getBlockEntity(signPos) instanceof SignBlockEntity signEntity) {
                // Retrieve the text written on the sign
                return Arrays.stream(signEntity.getFrontText().getMessages(false)).map(Component::getString).toList();
            }

        }
        return null;
    }

    private Villager getVillagerForLectern(Player player, Level world, BlockPos clickedPos, List<EnchFilter> filters) {
        AABB box = player.getBoundingBox().inflate(VILLAGER_SEARCH_RADIUS); // use inflate, not expandTowards

        List<Villager> nearbyVillagers = world.getEntitiesOfClass(Villager.class, box, v -> true);
        for (Villager villager : nearbyVillagers) {
            // Check if the villager is a librarianProfession
            if (villager.getVillagerData().profession().is(VillagerProfession.LIBRARIAN)) {
                Optional<GlobalPos> jobSitePosOptional = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
                // Convert GlobalPos to BlockPos and compare with clicked lectern
                if (jobSitePosOptional.isPresent()) {
                    BlockPos jobSitePos = jobSitePosOptional.get().pos(); // Extract BlockPos from GlobalPos
                    if (jobSitePos.equals(clickedPos)) {
                        if (villager.getVillagerXp() == 0) {
                            return villager;
                        }
                    }
                }
            }
        }
        return null;
    }

    private FilterResult filterTrade(Player player, Level world, Villager villager, List<EnchFilter> filters) {
        if (world instanceof ServerLevel) {
            UUID playerUUID = player.getUUID();
            long currentTime = System.currentTimeMillis();
            // Check if the player is still on cooldown
//            if (cooldownMap.containsKey(playerUUID)) {
//                long lastClickTime = cooldownMap.get(playerUUID);
//                long difference = currentTime - lastClickTime;
//                if (difference < COOLDOWN_TIME) {
//                    return FilterResult.COOLDOWN; // Prevents further execution
//                }
//            }
            if (villager != null && !world.isClientSide()) {
                RegistryAccess access = villager.level().registryAccess();
                int recycleCount = 0;

                while (recycleCount <= MAX_REROLL_COUNT - 1) {
                    // --- Reset profession to NONE ---
                    VillagerData data = villager.getVillagerData();
                    Holder<VillagerProfession> noneProfession = access.getOrThrow(VillagerProfession.NONE);
                    villager.setVillagerData(data.withProfession(noneProfession));


                    // --- Reassign to LIBRARIAN ---
                    Holder<VillagerProfession> librarianProfession = access.getOrThrow(VillagerProfession.LIBRARIAN);
                    villager.setVillagerData(villager.getVillagerData().withProfession(librarianProfession));
                    recycleCount++;

                    // --- Check trades ---
                    MerchantOffers offers = villager.getOffers();
                    for (MerchantOffer trade : offers) {
                        ItemStack sellItem = trade.getResult();

                        // Only look at enchanted books
                        if (sellItem.getItem() == Items.ENCHANTED_BOOK) {
                            ItemEnchantments enchantments = sellItem.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);

                            for (var entry : enchantments.entrySet()) {
                                Holder<Enchantment> enchHolder = entry.getKey();
                                int enchBookLevel = entry.getIntValue();

                                // Get the simple name (e.g., "efficiency")
                                String enchName = enchHolder.unwrapKey()
                                        .map(k -> k.location().getPath())
                                        .orElse("unknown");

                                System.out.println("Found enchantment: " + enchName + " enchBookLevel " + enchBookLevel + " price " + trade.getCostA().getCount());

                                // Compare with filters (partial match, exact enchBookLevel)
                                for (EnchFilter filter : filters) {
                                    int expectedLevel = filter.enchLevel;
                                    if (enchName.startsWith(filter.enchName.toLowerCase())) {
                                        if (expectedLevel == 0) {
                                            Enchantment enchantment = enchHolder.value();
                                            expectedLevel = enchantment.getMaxLevel();
                                        }
                                        if (enchBookLevel == expectedLevel) {
                                            if (filter.price > 0) {
                                                if (trade.getCostA().getCount() <= filter.price) {

                                                    System.out.println("✅ Found matching enchantment: " + enchName + " " + enchBookLevel);
                                                    return FilterResult.SUCCESS;
                                                }
                                            } else {
                                                System.out.println("✅ Found matching enchantment: " + enchName + " " + enchBookLevel);
                                                return FilterResult.SUCCESS;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }
        return FilterResult.FAILED;
    }

    public record EnchFilter(String enchName, int enchLevel, int price) {
    }

    Map<Integer, Integer> getPriceMap() {
        return Map.of(
                1, 5,
                2, 8,
                3, 11,
                4, 14,
                5, 17);
    }

    enum FilterResult {
        SUCCESS,
        FAILED,
        COOLDOWN
    }


    private void spawnParticles(Level world, FilterResult filterResult, Villager villager, BlockPos clickedPos) {
        if (filterResult == FilterResult.SUCCESS) {
            world.playSound(null, villager,
                    SoundEvents.VILLAGER_YES,
                    SoundSource.NEUTRAL, 1f, 1f);
            for (int i = 0; i < durationTicks; i++) {
                ((ServerLevel) world).sendParticles(
                        ParticleTypes.HAPPY_VILLAGER,
                        villager.getX() + 0.5,
                        villager.getY() + 1,
                        villager.getZ() + 0.5,
                        8, 0.3, 0.3, 0.3, 0.01
                );
                ((ServerLevel) world).sendParticles(
                        ParticleTypes.HAPPY_VILLAGER,
                        clickedPos.getX() + 0.5,
                        clickedPos.getY() + 1,
                        clickedPos.getZ() + 0.5,
                        8, 0.3, 0.3, 0.3, 0.01
                );
            }
        }
        if (filterResult == FilterResult.FAILED) {
            world.playSound(null, villager,
                    SoundEvents.VILLAGER_NO,
                    SoundSource.NEUTRAL, 1f, 1f);
            for (int i = 0; i < durationTicks; i++) {
                ((ServerLevel) world).sendParticles(
                        ParticleTypes.ANGRY_VILLAGER,
                        villager.getX() + 0.5,
                        villager.getY() + 1,
                        villager.getZ() + 0.5,
                        8, 0.3, 0.3, 0.3, 0.01
                );
                ((ServerLevel) world).sendParticles(
                        ParticleTypes.ANGRY_VILLAGER,
                        clickedPos.getX() + 0.5,
                        clickedPos.getY() + 1,
                        clickedPos.getZ() + 0.5,
                        8, 0.3, 0.3, 0.3, 0.01
                );
            }

        }
    }

    public static void spawnLineParticles(ServerLevel world, BlockPos from, BlockPos to) {
        double x1 = from.getX() + 0.5;
        double y1 = from.getY() + 0.5;
        double z1 = from.getZ() + 0.5;

        double x2 = to.getX() + 0.5;
        double y2 = to.getY() + 0.5;
        double z2 = to.getZ() + 0.5;

        int steps = 20; // number of points along the line

        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;

            double x = x1 + (x2 - x1) * t;
            double y = y1 + (y2 - y1) * t;
            double z = z1 + (z2 - z1) * t;

            world.sendParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    x, y, z,
                    1,   // count
                    0, 0, 0, // no spread
                    0     // no speed
            );
        }
    }


}
