package com.cceckman.blocks;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;

public class App extends JavaPlugin {

    @Override
    public void onEnable() {
        listener_ = new ListenThread(this);
        getLogger().info("Starting block device listener...");
        listener_.start();
        getLogger().info("Block device listener started!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Stopping block device listener...");
        listener_.interrupt();
        try {
            listener_.join();
        } catch (InterruptedException e) {
            StringWriter sw = new StringWriter();
            sw.append("Got interrupted while waiting for listener to close: ");
            e.printStackTrace(new PrintWriter(sw));
            getLogger().warning(sw.toString());
        }
        getLogger().info("Block device listener stopped.");
    }

    private class ListenThread extends Thread {
        private final App parent_;

        public ListenThread(final App parent) {
            this.parent_ = parent;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(1000);
                parent_.getLogger().info("Listener tick.");
            } catch (InterruptedException e) {
                parent_.getLogger().info("Listener received shutdown signal, stopping.");
                return;
            }
        }

    }

    private ListenThread listener_;
}