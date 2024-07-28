package org.irmc.industrialrevival.api.items.handlers;

import javax.annotation.Nonnull;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.irmc.industrialrevival.api.items.ItemHandler;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface WeaponUseHandler extends ItemHandler {
    /**
     * This function is called when a {@link Player} attacks an {@link Entity} with a {@link org.irmc.industrialrevival.api.items.IndustrialRevivalItem}
     *
     * @param e The {@link EntityDamageByEntityEvent} that was fired
     * @param player The {@link Player} that used the weapon
     * @param item The {@link ItemStack} that was used to attack
     */
    void onHit(@NotNull EntityDamageByEntityEvent e, @Nonnull Player player, @Nonnull ItemStack item);
}
