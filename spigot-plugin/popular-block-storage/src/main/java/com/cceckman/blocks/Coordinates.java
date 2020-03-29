package com.cceckman.blocks;

// Coordinates of a datum in the world.
public class Coordinates {
    public final int x, y, z, slot;

    static final int kPageSize = 4096;
    static final int kSlotsPerChest = 54;
    static final int kMaxHeight = 4; // Max chests high.
    // So how many chests wide do we need?
    // (This comes out to a non-round number, "ceil" by addition).
    static final int kWidth = (kPageSize / (kSlotsPerChest * kMaxHeight)) + 1;


    Coordinates(long offset) {
        // We interpret...
        // Low-order bits: offset within chest
        this.slot = (int)(offset % kSlotsPerChest);
        offset /= kSlotsPerChest;

        // Next-heighest bits: chest number, vertically
        // TODO(cceckman): Offset by scale (alternating rows of chests)
        z = (int)(offset % kMaxHeight);
        offset /= kMaxHeight;

        // Next-heighest bits: chest number, horizontally
        // TODO(cceckman): Offset by scale (alternating rows of chests)
        y = (int)(offset % kWidth);
        x = (int)(offset / kWidth);
    }
}
