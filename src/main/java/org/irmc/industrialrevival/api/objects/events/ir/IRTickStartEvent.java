package org.irmc.industrialrevival.api.objects.events.ir;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.irmc.industrialrevival.api.objects.IRBlockData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Getter
public class IRTickStartEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Map<Location, IRBlockData> blockDataMap;
    private final int checkInterval;
    private final long ticked;
    private @Setter boolean cancelled;

    public IRTickStartEvent(Map<Location, IRBlockData> blockDataMap, int checkInterval, long ticked) {
        this.blockDataMap = new HashMap<>(blockDataMap);
        this.checkInterval = checkInterval;
        this.ticked = ticked;
    }

    public static @NotNull HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
