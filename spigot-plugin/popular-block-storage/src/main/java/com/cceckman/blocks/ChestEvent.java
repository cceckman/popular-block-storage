package com.cceckman.blocks;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

// Event for "chest activity" from the block storage API.
// Per: https://www.spigotmc.org/wiki/using-the-event-api/
public class ChestEvent extends Event {
    public ChestEvent(long offset, long length) {
        offset_ = offset;
        length_ = length;
    }
    private static final HandlerList HANDLERS = new HandlerList();

    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    private long offset_; // Offset of the request: first address read / written.
    private long length_; // Length of request: read / written.

    public long getOffset() {
        return offset_;
    }
    public long getLength() {
        return length_;
    }
}