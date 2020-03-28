package com.cceckman.blocks;

import java.util.logging.Logger;

import org.bukkit.World;

public class OffsetOperationFactory {
    public OffsetOperationFactory(final Logger l, final World w) {
        logger_ = l;
        world_ = w;
    }

    public OffsetOperation newOp(final long offset, final long length) {
        return new OffsetOperation(logger_, world_, offset, length);
    }

    private Logger logger_;
    private World world_;
}