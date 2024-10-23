package org.irmc.industrialrevival.api.objects.events.handler;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.irmc.industrialrevival.api.items.IndustrialRevivalItem;

@Getter
public class PrepareAnvilIRItemEvent extends PrepareAnvilEvent implements Cancellable {
    private final IndustrialRevivalItem iritem;
    private final PrepareAnvilEvent originalEvent;
    private boolean cancelled;
    public PrepareAnvilIRItemEvent(PrepareAnvilEvent event, IndustrialRevivalItem iritem) {
        super(event.getView(), event.getResult());
        this.originalEvent = event;
        this.iritem = iritem;
    }

    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
        setResult(null);
    }
}
