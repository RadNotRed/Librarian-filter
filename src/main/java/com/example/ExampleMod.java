package com.example;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleMod implements ModInitializer {
    public static final String MOD_ID = "elt";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Mod initialized!");
        EnchFilterInit enchFilter = new EnchFilterInit();
        enchFilter.registerEvent();
        ServerBeaconChecker.register();
    }

}
