package com.cceckman.blocks;

import java.util.logging.Logger;

import org.bukkit.plugin.Plugin;

public class Server extends Thread {
    private final Logger logger_;
    private final Plugin plugin_;
    private final OffsetOperationFactory op_factory_;

    public Server(final Logger logger, final Plugin p, final OffsetOperationFactory f) {
        this.logger_ = logger;
        this.plugin_ = p;
        this.op_factory_ = f;
    }

    @Override
    public void run() {
        long offset = 0;
        final long length = 4096;
        while (true) {
            try {
                Thread.sleep(1000);
                logger_.info("Server tick.");
            } catch (final InterruptedException e) {
                logger_.info("Server received shutdown signal, stopping.");
                return;
            }

            // Send a fake event.
            OffsetOperation op = op_factory_.newOp(offset, length);
            logger_.info("Running task");
            var task = op.runTask(plugin_);
            logger_.info(String.format("Ran task with ID: %d", task.getTaskId()));
            offset += length;
        }
    }
}