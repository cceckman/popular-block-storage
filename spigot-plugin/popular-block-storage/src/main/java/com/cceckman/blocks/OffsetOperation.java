package com.cceckman.blocks;

import java.util.Arrays;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Chest;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

// Runnable operation of updating or reading from a block storage device.
// Generated by a server thread; closes over a World.
// Per: https://www.spigotmc.org/wiki/using-the-event-api/
public class OffsetOperation extends BukkitRunnable {
    public OffsetOperation(
        final Logger l, 
        final Location origin, 
        final boolean write,
        final long offset, 
        final byte[] buffer) {
        logger_ = l;
        origin_ = origin;
        write_ = write;
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

    // Semantic fields: r/w, offset & buffer (which implies length.)
    private final boolean write_;
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
    // The way Bukkit's API is set up, we only get half of the inventory at a time, so we only go through slots 0-27.
    // TODO(cceckman): Use large chests (54 slots)?
    static final int kSlotsPerChest = 27;
    private static int slot(final long offset) {
        return (int)(offset % kSlotsPerChest);
    }

    // We can derive the column index from the other constants.
    private static int column(final long offset) {
        final int slice_offset = (int)(offset % kBytesPerSlice);  // [0, 4096)
        final int chest_index = slice_offset / (kSlotsPerChest);  // Index of a pair of chests: [0, 4*19)
        return chest_index / kRowsPerSlice;                       // Column of pairs-of-chests: 0, 19
    }

    private Block buddy(final long offset) {
        final int col = column(offset);
        // "Even" columns have a buddy at +1; "odd" columns have a buddy at -1.
        int step = -1;
        if(col % 1 == 0) {
            step = 1;
        }
        return location(offset).clone().add(new Vector(step, 0, 0)).getBlock();
    }

    private void ensureChest(final long offset) {
        Block block = location(offset).getBlock();
        // Block buddy = buddy(offset);

        // If either is not a chest, we're in repair mode.
        if(block.getType() != Material.CHEST /*|| buddy.getType() != Material.CHEST*/) {
            block.setType(Material.CHEST);
            //buddy.setType(Material.CHEST);
        }

        // Set object metadata.
        Chest blockData = (Chest)block.getBlockData();
        // Chest buddyData= (Chest)buddy.getBlockData();
        blockData.setType(Chest.Type.SINGLE);
        //blockData.setType(Chest.Type.LEFT);
        // buddyData.setType(Chest.Type.RIGHT);
        blockData.setFacing(BlockFace.NORTH);
        // buddyData.setFacing(BlockFace.NORTH);

        block.setBlockData(blockData);
        // buddy.setBlockData(buddyData);
    }

    private Location location(final long offset) {
        int z = slice(offset) * 2;      // Space out slices by 1 block (deep).
        int y = row(offset) * 2;       // Space out rows by 1 block (high). Chest has to open!
        // Column takes some trickiness: "even" columns are at N*3, "odd" columns at N*3+1.
        // This gives us the appropriate left/right side of a chest.
        int col = column(offset);
        int x = col * 3 + (col % 2);
        return origin_.clone().add(new Vector(x,y,z));
    }

    // Applies this transformation to the world.
    // Must be applied from the main thread!
    public void run() {
        logger_.info(String.format("Received request for @%d (%d bytes)", offset_, buffer_.length));

        for(int i = 0; i < buffer_.length; i++) {
            final var offset =  this.offset_ + i;
            final var slot = slot(offset);

            ensureChest(offset);
            var block = (org.bukkit.block.Chest)location(offset).getBlock().getState();
            var inv = block.getSnapshotInventory();

            // Place the byte in the chest, OR read into buffer.
            if(write_) {
                inv.setItem(slot, Materials.value(buffer_[i]));
            } else {
                buffer_[i] = Materials.value(inv.getItem(slot));
            }

            // Commit the state-change back to the block.
            block.update(true);
        }

        // logger_.info(String.format("Flushing block (%d, %d %d) at end of offsets (%d bytes)", state.getX(), state.getY(), state.getZ(), buffer_.length));
        if(!write_) {
            logger_.info(String.format(
                "Collected data with hash: %d", Arrays.hashCode(buffer_)));
        }
        this.complete();
    }

    // Sync code.
    final Lock lock = new ReentrantLock();
    final Condition guard_done = lock.newCondition();
    boolean done = false;
    private void complete() {
        lock.lock();
        this.done = true;
        guard_done.signalAll();
        lock.unlock();
    }
    public boolean await() throws InterruptedException {
        lock.lock();
        try {
            while( !done) {
                guard_done.await();
            }
            return done;
        } finally {
            lock.unlock();
        }
    }
}