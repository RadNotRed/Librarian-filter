package com.gbdhapa;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import com.gbdhapa.config.TradeConfig;

public class RerollHelper {
    public static final String MOD_ID = "librarian-filter";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final HashMap<UUID, Map<BlockPos, Long>> cooldownMap = new HashMap<>();
    private static final long COOLDOWN_TIME = 1000;
    private static final int VILLAGER_SEARCH_RADIUS = 128;
    private static final int MAX_REROLL_COUNT = 10000;
    private static final int durationTicks = 5;



    public RerollHelper() {
        LOGGER.info("Villager trade reroll initialized for 26.1!");
        registerEvent();
    }

    private void registerEvent() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!TradeConfig.INSTANCE.enableReroll) {
                return InteractionResult.PASS;
            }
            BlockPos clickedPos = hitResult.getBlockPos();
            UUID playerUUID = player.getUUID();
            long currentTime = System.currentTimeMillis();
            if (isRerollCooldown(playerUUID, clickedPos, currentTime)) {
                return InteractionResult.PASS;
            }
            Block blockClicked = world.getBlockState(clickedPos).getBlock();
            List<String> signTexts = getSignTexts(world, blockClicked, clickedPos);
            if (signTexts == null || signTexts.isEmpty()) {
                return InteractionResult.PASS;
            }
            List<TradeFilter> filters = getEnchFilters(signTexts);
            if (!filters.isEmpty() && world instanceof ServerLevel) {
                Villager villager = getVillagerForWorkstation(player, (ServerLevel) world, clickedPos);
                if (villager != null) {
                    FilterResult filterResult = filterTrade(villager, filters);
                    villager.refreshBrain((ServerLevel) world);
                    spawnParticles((ServerLevel) world, filterResult, villager, clickedPos);
                    cooldownMap.put(playerUUID, Map.of(clickedPos, currentTime));
                }
            }
            return InteractionResult.PASS;
        });
    }

    private Boolean isRerollCooldown(UUID playerUUID, BlockPos clickedPos, long currentTime) {
        if (cooldownMap.containsKey(playerUUID)) {
            Long lastClickTime = cooldownMap.get(playerUUID).get(clickedPos);
            if (lastClickTime != null) {
                long difference = currentTime - lastClickTime;
                if (difference < COOLDOWN_TIME) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<TradeFilter> getEnchFilters(List<String> signTexts) {
        List<TradeFilter> filters = new ArrayList<>();
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
                                    filters.add(new TradeFilter(filterText[0], enchLevel, price));
                                } else {
                                    filters.add(new TradeFilter(filterText[0], enchLevel, 0));
                                }
                            }
                        }
                    } else {
                        filters.add(new TradeFilter(filterText[0], 0, 0));
                    }
                }
            }
        }
        return filters;
    }

    private List<String> getSignTexts(Level world, Block blockClicked, BlockPos clickedPos) {
        if (blockClicked == Blocks.LECTERN) {
            Direction facingDirection = world.getBlockState(clickedPos).getValue(LecternBlock.FACING);
            BlockPos signPos = clickedPos.relative(facingDirection);
            if (world.getBlockEntity(signPos) instanceof SignBlockEntity signEntity) {
                return Arrays.stream(signEntity.getFrontText().getMessages(false)).map(Component::getString).toList();
            }
        }

        if (blockClicked != Blocks.AIR) {
            BlockState blockState = world.getBlockState(clickedPos);
            if (PoiTypes.hasPoi(blockState)) {
                SignBlockEntity signEntity = getAttachedSign(world, clickedPos);
                if (signEntity != null) {
                    return Arrays.stream(signEntity.getFrontText().getMessages(false)).map(Component::getString).toList();
                }
            }
        }
        return new ArrayList<>();
    }

    private Villager getVillagerForWorkstation(Player player, ServerLevel world, BlockPos clickedPos) {
        AABB box = player.getBoundingBox().inflate(VILLAGER_SEARCH_RADIUS);

        List<Villager> nearbyVillagers = world.getEntitiesOfClass(Villager.class, box, v -> true);
        for (Villager villager : nearbyVillagers) {
            if (villager.getVillagerData().profession().is(VillagerProfession.LIBRARIAN)) {
                Optional<GlobalPos> jobSitePosOptional = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
                if (jobSitePosOptional.isPresent()) {
                    BlockPos jobSitePos = jobSitePosOptional.get().pos();
                    if (jobSitePos.equals(clickedPos)) {
                        if (TradeConfig.INSTANCE.enableEachLevelReroll) {
                            return villager;
                        }
                        if (villager.getVillagerXp() == 0) {
                            return villager;
                        }
                    }
                }
            } else {
                Optional<GlobalPos> jobSitePosOptional = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
                if (jobSitePosOptional.isPresent()) {
                    BlockPos jobSitePos = jobSitePosOptional.get().pos();
                    if (jobSitePos.equals(clickedPos)) {
                        if (TradeConfig.INSTANCE.enableEachLevelReroll) {
                            return villager;
                        }
                        if (villager.getVillagerXp() == 0) {
                            return villager;
                        }
                    }
                }
            }
        }
        return null;
    }

    private FilterResult filterTrade(Villager villager, List<TradeFilter> filters) {
        if (villager != null) {
            RegistryAccess access = villager.level().registryAccess();
            int recycleCount = 0;
            MerchantOffers originalOffers = villager.getOffers();
            boolean hasTradedLastOffers = checkIfPlayerHasTradedLastOffers(originalOffers);
            if (hasTradedLastOffers) {
                return FilterResult.FAILED;
            }
            while (recycleCount <= MAX_REROLL_COUNT) {
                VillagerData data = villager.getVillagerData();
                Holder<VillagerProfession> profession = villager.getVillagerData().profession();
                Holder<VillagerProfession> noneProfession = access.getOrThrow(VillagerProfession.NONE);
                villager.setVillagerData(data.withProfession(noneProfession));
                villager.setVillagerData(villager.getVillagerData().withProfession(profession));

                recycleCount++;
                MerchantOffers offers = villager.getOffers();
                for (MerchantOffer trade : offers) {
                    if (profession.is(VillagerProfession.LIBRARIAN)) {
                        if (trade.getResult().getItem() == Items.ENCHANTED_BOOK) {
                            FilterResult result = filterEnchantmentBook(filters, trade);
                            if (result == FilterResult.SUCCESS) {
                                villager.setOffers(new MerchantOffers());
                                originalOffers.removeLast();
                                originalOffers.removeLast();
                                originalOffers.addAll(offers);
                                villager.setOffers(originalOffers);
                                return result;
                            }
                        }
                    } else {
                        FilterResult result = filterTrades(filters, trade);
                        if (result == FilterResult.SUCCESS) {
                            villager.setOffers(new MerchantOffers());
                            originalOffers.removeLast();
                            originalOffers.removeLast();
                            originalOffers.addAll(offers);
                            villager.setOffers(originalOffers);
                            return result;
                        }
                    }
                }
            }

            villager.setOffers(new MerchantOffers());
            villager.setOffers(originalOffers);
        }
        return FilterResult.FAILED;
    }

    private static boolean checkIfPlayerHasTradedLastOffers(MerchantOffers originalOffers) {
        if(originalOffers == null || originalOffers.isEmpty()) return false;
        int offersSize = originalOffers.size();
        if (offersSize % 2 == 0 && offersSize >= 2) {
            MerchantOffer secondLast = originalOffers.get(offersSize - 2);
            MerchantOffer last = originalOffers.get(offersSize - 1);
            return secondLast.getUses() > 0 || last.getUses() > 0;
        } else {
            MerchantOffer last = originalOffers.get(offersSize - 1);
            return last.getUses() > 0;
        }
    }

    private FilterResult filterEnchantmentBook(List<TradeFilter> filters, MerchantOffer trade) {
        ItemEnchantments enchantments = trade.getResult().getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (var entry : enchantments.entrySet()) {
            Holder<Enchantment> enchHolder = entry.getKey();
            int enchBookLevel = entry.getIntValue();

            String enchName = enchHolder.unwrapKey()
                    .map(k -> k.identifier().getPath())
                    .orElse("unknown");

            for (TradeFilter filter : filters) {
                int expectedLevel = filter.enchLevel;
                if (enchName.toLowerCase().startsWith(filter.filterName.toLowerCase())) {
                    if (expectedLevel == 0) {
                        Enchantment enchantment = enchHolder.value();
                        expectedLevel = enchantment.getMaxLevel();
                    }
                    if (enchBookLevel == expectedLevel) {
                        if (filter.price > 0) {
                            if (trade.getCostA().getCount() <= filter.price) {
                                return FilterResult.SUCCESS;
                            }
                        } else {
                            return FilterResult.SUCCESS;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static @Nullable FilterResult filterTrades(List<TradeFilter> filters, MerchantOffer trade) {
        String sellItemName = trade.getResult().getItemName().getString().toLowerCase();
        Optional<TradeFilter> filteredTrade = filters.stream().filter(f -> sellItemName.contains(formatFilterName(f))).findFirst();
        if (filteredTrade.isPresent()) {
            return FilterResult.SUCCESS;
        }
        String buyItem1Name = trade.getCostA().getItemName().getString().toLowerCase();
        filteredTrade = filters.stream().filter(f -> buyItem1Name.contains(formatFilterName(f))).findFirst();
        if (filteredTrade.isPresent()) {
            return FilterResult.SUCCESS;
        }

        ItemStack costB = trade.getCostB();
        if (costB != ItemStack.EMPTY) {
            String buyItem2Name = costB.getItemName().getString().toLowerCase();
            filteredTrade = filters.stream().filter(f -> buyItem2Name.contains(formatFilterName(f))).findFirst();
            if (filteredTrade.isPresent()) {
                return FilterResult.SUCCESS;
            }
        }

        return null;
    }

    private static @NotNull String formatFilterName(TradeFilter f) {
        return f.filterName.toLowerCase().replaceAll("_", " ");
    }

    public record TradeFilter(String filterName, int enchLevel, int price) {}

    enum FilterResult { SUCCESS, FAILED }

    private void spawnParticles(ServerLevel world, FilterResult filterResult, Villager villager, BlockPos clickedPos) {
        if (filterResult == FilterResult.SUCCESS) {
            world.playSound(null, villager,
                    SoundEvents.VILLAGER_YES,
                    SoundSource.NEUTRAL, 1f, 1f);
            for (int i = 0; i < durationTicks; i++) {
                world.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        villager.getX() + 0.5, villager.getY() + 1, villager.getZ() + 0.5,
                        8, 0.3, 0.3, 0.3, 0.01);
                world.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        clickedPos.getX() + 0.5, clickedPos.getY() + 1, clickedPos.getZ() + 0.5,
                        8, 0.3, 0.3, 0.3, 0.01);
            }
        }
        if (filterResult == FilterResult.FAILED) {
            world.playSound(null, villager,
                    SoundEvents.VILLAGER_NO,
                    SoundSource.NEUTRAL, 1f, 1f);
            for (int i = 0; i < durationTicks; i++) {
                world.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                        villager.getX() + 0.5, villager.getY() + 1, villager.getZ() + 0.5,
                        8, 0.3, 0.3, 0.3, 0.01);
                world.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                        clickedPos.getX() + 0.5, clickedPos.getY() + 1, clickedPos.getZ() + 0.5,
                        8, 0.3, 0.3, 0.3, 0.01);
            }
        }
    }

    public static SignBlockEntity getAttachedSign(Level world, BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos side = pos.relative(dir);
            if (world.getBlockEntity(side) instanceof SignBlockEntity signEntity) {
                return signEntity;
            }
        }
        return null;
    }
}
