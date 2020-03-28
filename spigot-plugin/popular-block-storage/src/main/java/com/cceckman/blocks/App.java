package com.cceckman.blocks;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.bukkit.plugin.java.JavaPlugin;

public class App extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("Registering event handler...");
        ChestEventHandler handler = new ChestEventHandler(getLogger());
        getServer().getPluginManager().registerEvents(handler, this);

        server_ = new Server(this.getLogger(), event -> {
            getServer().getPluginManager().callEvent(event);
        });
        getLogger().info("Starting block device server...");
        server_.start();
        getLogger().info("Block device server started!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Stopping block device listener...");
        server_.interrupt();
        try {
            server_.join();
        } catch (final InterruptedException e) {
            final StringWriter sw = new StringWriter();
            sw.append("Got interrupted while waiting for listener to close: ");
            e.printStackTrace(new PrintWriter(sw));
            getLogger().warning(sw.toString());
        }
        getLogger().info("Block device listener stopped.");
    }

    private Server server_;
}