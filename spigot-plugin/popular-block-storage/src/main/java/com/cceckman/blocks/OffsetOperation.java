package com.cceckman.blocks;

import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.HandlerList;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

// Runnable operation of updating or reading from a block storage device.
// Generated by a server thread; closes over a World.
// Per: https://www.spigotmc.org/wiki/using-the-event-api/
public class OffsetOperation extends BukkitRunnable {
    public OffsetOperation(
        final Logger l, 
        final Location origin, 
        final long offset, 
        final byte[] buffer) {
        logger_ = l;
        origin_ = origin;
        offset_ = offset;
        buffer_ = buffer;
    }

    private static final HandlerList HANDLERS = new HandlerList();

    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    // Context fields.
    private final Location origin_;
    private final Logger logger_;

    // Semantic fields: offset & buffer (which implies length.)
    private final long offset_; // Offset of the request: first address read / written.
    private final byte[] buffer_;

    // Data is organized hierarchically:
    // - Along the Z-axis by slice (4096 KiB)
    // - Along the Y-axis by row (4 per slice --> 1024 bytes)
    // - Along the X-axis by column (19 per row)
    // - Within a chest by byte index (54 per row)

    static final int kBytesPerSlice = 4096;
    // Slice is the XY-slice that the given offset is in.
    private static int slice(final long offset) {
        return (int)(offset / kBytesPerSlice);
    }

    // Vertical (Y-axis) rows in a a slice.
    static final int kRowsPerSlice = 4;
    private static int row(final long offset) {
        final int slice_offset = (int)(offset % kBytesPerSlice);
        final int chest_index = slice_offset / kSlotsPerChest;
        return chest_index % kRowsPerSlice;
    }

    // Slots in a (small) Minecraft chest. We use one slot per byte.
    static final int kSlotsPerChest = 27;
    private static int slot(final long offset) {
        return (int)(offset % kSlotsPerChest);
    }

    // We can derive the column index from the other constants.
    private static int column(final long offset) {
        final int slice_offset = (int)(offset % kBytesPerSlice); // [0, 4096)
        final int chest_index = slice_offset / kSlotsPerChest;
        return chest_index / kRowsPerSlice;
    }

    private void ensureChest(Block block) {
        // TODO(cceckman)
        block.setType(Material.CHEST);
        logger_.info(String.format("Turned (%d, %d, %d) into a chest",
            block.getX(), block.getY(), block.getZ()));
    }

    private Location location(final long offset) {
        int z = slice(offset) * 2;      // Space out slices by 1 block (deep).
        int y = row(offset_) * 2;       // Space out rows by 1 block (high). Chest has to open!
        int x = column(offset_) * 3;    // Space out columns by 2 blocks; chest is 2 wide.
        return origin_.clone().add(new Vector(x,y,z));
    }

    // Applies this transformation to the world.
    // Must be applied from the main thread!
    public void run() {
        logger_.info(String.format("Received write request for @%d (%d bytes)", offset_, buffer_.length));
        var world = origin_.getWorld();

        Block block = world.getBlockAt(location(offset_));
        ensureChest(block);
        for(long i = 0; i < buffer_.length; i++) {
            final var offset =  this.offset_ + i;
            final var slot = slot(offset);

            if(slot == 0) {
                // We've rolled over into a new block. Check that it's a chest.
                block = world.getBlockAt(location(offset));
                ensureChest(block);
            }
            // Place the byte in the chest, OR read into buffer.
        }
    }
}