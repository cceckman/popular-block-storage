package com.cceckman.blocks;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.bukkit.plugin.Plugin;

public class Server extends Thread {
    public Server(final Logger logger, final Plugin p, final OffsetOperationFactory f, final int port) {
        this.logger_ = logger;
        this.plugin_ = p;
        this.op_factory_ = f;
        this.port_ = port;
        this.client_pool_ = Executors.newFixedThreadPool(16);
    }

    @Override
    public void run() {

        // Dummy task to keep things busy
        /*
        client_pool_.submit(() -> {

            long offset = 0;
            final int length = 1;
            while (true) {
                try {
                    Thread.sleep(10000);
                    logger_.info("Dummy thread tick.");
                } catch (final InterruptedException e) {
                    logger_.info("Dummy thread received shutdown signal, stopping.");
                    return;
                }

                final var buf = new byte[length];

                // Send a fake read event.
                final OffsetOperation op = op_factory_.newOp(true, offset, buf);
                logger_.info("Running task");
                final var task = op.runTask(plugin_);
                logger_.info(String.format("Ran task with ID: %d", task.getTaskId()));
                offset += 51;
            }
        });
        */

        ServerSocket listener;
        try {
            listener = new ServerSocket(port_);
        } catch (IOException e) {
            logger_.severe(String.format("Could not start server: %s", e));
            return;
        }
        client_pool_.submit(() -> {
            int connectionNumber = 0;
            while(!this.isInterrupted()) {
                try{
                    var sock = listener.accept();
                    logger_.info(String.format(
                        "Server received connection from %s, starting worker for connection %d",
                        sock.getInetAddress(), connectionNumber));
                    // Java doesn't (?) have explicit captures, or explicit copy vs. reference...
                    // so, temporary local for connection number.
                    int thisConnectionNumber = connectionNumber++;
                    client_pool_.submit(() -> {
                        handleChannel(sock, thisConnectionNumber);
                    });
                } catch(final IOException e) {
                    logger_.warning(String.format("Server received exception in IO loop: %s\nExiting", e.toString()));
                    break;
                }
            }
        });

        // Wait for an interrupt, then shut things down.
        try {
            while(!this.isInterrupted()) {
                synchronized(this) {
                    this.wait();
                }
            }
        } catch (final InterruptedException e) {
            // That's fine!
        }
        try {
            listener.close();
        } catch (IOException e) {
            logger_.warning("Server got error closing socket");
            e.printStackTrace();
        }
        client_pool_.shutdown();
    }

    private void handleChannel(final Socket s, final int connectionNumber) {
        try {
            var input = s.getInputStream();
            var output = s.getOutputStream();
            // Read header: r/w byte, offset, length
            final int headerSize = 1 + 4 + 4;
            var header = new byte[headerSize];
            var headerAccess = ByteBuffer.wrap(header);
            while(true) {
                int headerBytes = 0;
                while(headerBytes < headerSize) {
                    int read = input.read(header, headerBytes, headerSize - headerBytes);
                    if(read == -1) {
                        // Reached end of input stream.
                        throw new IOException(String.format(
                            "Connection %d: Reached end of stream within header",
                            connectionNumber));
                    }
                    headerBytes += read;
                }
                // Decode header.
                boolean isReadRequest = header[0] == 0;
                boolean isWriteRequest = !isReadRequest;
                long offset = headerAccess.getInt(1);
                int length = headerAccess.getInt(5);

                // Get data buffer. Yes, we should reuse these. No, we aren't going to.
                var data = new byte[length];
                if(isWriteRequest) {
                    // Read request data into local buffer, for writing in the game thread.
                    int totalRead= 0;
                    while(totalRead < length) {
                        int read = input.read(data, totalRead, length - totalRead);
                        if(read == -1) {
                            // Reached end of input stream.
                            throw new IOException(String.format( 
                                "Connection %d: Reached end of stream while reading data",
                                connectionNumber));
                        }
                        totalRead += read;
                    }
                }

                // Propagate to application.
                // Run as a synchronous operation with the game thread, so that each client has local consistency
                // (i.e. their operations were in order with respect to their other operations.)
                final OffsetOperation op = op_factory_.newOp(isWriteRequest, offset, data);
                logger_.info(String.format("Connection %d: Scheduling task on game thread", connectionNumber));
                final var task = op.runTask(plugin_);
                logger_.info(String.format(
                    "Connection %d: Scheduled task with ID %d, awaiting completion", connectionNumber, task.getTaskId()));
                op.await();

                logger_.info(String.format(
                    "Connection %d: Completed task %d on game thread", connectionNumber, task.getTaskId()));

                // Send response.
                output.write(header);
                if(isReadRequest) {
                    logger_.info(String.format(
                        "Connection %d: Writing back data (hash: %d)", connectionNumber, Arrays.hashCode(data)));
                    output.write(data);
                }
            }
        } catch (IOException e) {
            logger_.warning(String.format(
                "Connection %d got IO exception during read phase, shutting down", connectionNumber));
            e.printStackTrace();
        } catch (InterruptedException e) {
            logger_.warning(String.format(
                "Connection %d got interrupt exception during read phase, shutting down", connectionNumber));
            e.printStackTrace();
        }

        try {
            s.close();
        }catch(IOException e) {
            logger_.warning("Connection %d excepted during shutdown");
            e.printStackTrace();
        }
    }

    private final Logger logger_;
    private final Plugin plugin_;
    private final OffsetOperationFactory op_factory_;
    private final int port_;

    private final ExecutorService client_pool_;
}