package com.cceckman.blocks;

import java.net.ServerSocket;
import java.util.logging.Logger;

import org.bukkit.plugin.Plugin;

public class Server extends Thread {
    public Server(final Logger logger, final Plugin p, final OffsetOperationFactory f, int port) {
        this.logger_ = logger;
        this.plugin_ = p;
        this.op_factory_ = f;
        this.port_ = port;
    }

    @Override
    public void run() {
        // Start up listener...
        // var socket = new ServerSocket(port_);

        long offset = 0;
        final int length = 1;
        while (true) {
            try {
                Thread.sleep(500);
                logger_.info("Server tick.");
            } catch (final InterruptedException e) {
                logger_.info("Server received shutdown signal, stopping.");
                return;
            }

            var buf = new byte[length];

            // Send a fake event.
            OffsetOperation op = op_factory_.newOp(offset, buf);
            logger_.info("Running task");
            var task = op.runTask(plugin_);
            logger_.info(String.format("Ran task with ID: %d", task.getTaskId()));
            offset += 17;
        }
    }

    private final Logger logger_;
    private final Plugin plugin_;
    private final OffsetOperationFactory op_factory_;
    private final int port_;

    // private final AbstractExecutorService client_pool_;
}