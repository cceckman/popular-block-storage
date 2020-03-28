package com.cceckman.blocks;

import java.util.function.Consumer;
import java.util.logging.Logger;

public class Server extends Thread {
    private final Logger logger_;
    private final Consumer<ChestEvent> send_;

    public Server(final Logger logger, final Consumer<ChestEvent> send) {
        this.logger_ = logger;
        this.send_ = send;
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
            send_.accept(new ChestEvent(offset, length));
            offset += length;
        }
    }
}