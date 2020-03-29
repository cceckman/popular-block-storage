package com.cceckman.blocks;

import java.util.logging.Logger;

import org.bukkit.Location;

public class OffsetOperationFactory {
    public OffsetOperationFactory(final Logger l, final Location origin) {
        logger_ = l;
        origin_ = origin;
    }

    public OffsetOperation newOp(final boolean write, final long offset, final byte[] buffer) {
        return new OffsetOperation(logger_, origin_, write, offset, buffer);
    }

    private Logger logger_;
    private Location origin_;
}