package com.cceckman.blocks;

import java.util.logging.Logger;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;


// Event handler for ChestEvents, per https://www.spigotmc.org/wiki/using-the-event-api/
public class ChestEventHandler implements Listener {
    public ChestEventHandler(Logger logger) {
        logger_ = logger;
    }

    @EventHandler
    public void onChestEvent(ChestEvent e){
        logger_.info("Received event, handling appropriately.");
    }

    private Logger logger_;
}