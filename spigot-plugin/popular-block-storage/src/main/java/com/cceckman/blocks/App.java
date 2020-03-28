package com.cceckman.blocks;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;

import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public class App extends JavaPlugin {

    @Override
    public void onEnable() {
        // Event handler is bound to the main world.
        final List<World> worlds = getServer().getWorlds();
        Optional<World> world = Optional.empty();

        // TODO(cceckman) surely there's a find_if in Java.
        for (final World w : worlds) {
            if (w.getEnvironment() == World.Environment.NORMAL) {
                world = Optional.of(w);
            }
        }
        if (!world.isPresent()) {
            getLogger().severe("Could not find a 'normal'-type world");
            return;
        }
        OffsetOperationFactory f = new OffsetOperationFactory(this.getLogger(), world.get());
        server_ = new Server(this.getLogger(), this, f);
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