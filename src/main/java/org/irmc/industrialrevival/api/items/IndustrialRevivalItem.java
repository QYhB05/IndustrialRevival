package org.irmc.industrialrevival.api.items;

import com.google.common.base.Preconditions;

import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.OverridingMethodsMustInvokeSuper;

import lombok.Builder;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.irmc.industrialrevival.api.IndustrialRevivalAddon;
import org.irmc.industrialrevival.api.items.attributes.BlockDropItem;
import org.irmc.industrialrevival.api.items.attributes.ItemDroppable;
import org.irmc.industrialrevival.api.items.attributes.MobDropItem;
import org.irmc.industrialrevival.api.items.attributes.NotPlaceable;
import org.irmc.industrialrevival.api.items.attributes.VanillaSmeltingItem;
import org.irmc.industrialrevival.api.items.groups.ItemGroup;
import org.irmc.industrialrevival.api.items.handlers.ItemHandler;
import org.irmc.industrialrevival.api.objects.exceptions.IncompatibleItemHandlerException;
import org.irmc.industrialrevival.api.recipes.CraftMethod;
import org.irmc.industrialrevival.api.recipes.RecipeType;
import org.irmc.industrialrevival.utils.Constants;
import org.irmc.industrialrevival.implementation.IndustrialRevival;
import org.irmc.industrialrevival.api.recipes.RecipeContent;
import org.irmc.industrialrevival.api.recipes.RecipeContents;
import org.irmc.pigeonlib.items.ItemUtils;
import org.irmc.pigeonlib.pdc.PersistentDataAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An industrial revival item.<br>
 * By default, the item is not registered in the game.<br>
 * To register the item, use the register method.<br>
 * The block is placeable by default if you want it to be unplaceable, please see {@link NotPlaceable}
 */
@Builder
public class IndustrialRevivalItem {
    private final Map<Class<? extends ItemHandler>, ItemHandler> itemHandlers = new HashMap<>();

    @Getter
    private ItemGroup group;

    private IndustrialRevivalItemStack itemStack;

    @Getter
    private IndustrialRevivalAddon addon;

    @Getter
    private String wikiText;

    private ItemState state = ItemState.UNREGISTERED;

    private final Set<String> disabledInWorld = new HashSet<>();

    @Getter
    private final Set<CraftMethod> craftMethods = new HashSet<>();
    public IndustrialRevivalItem() {}

    public static IndustrialRevivalItem getById(String id) {
        return IndustrialRevival.getInstance().getRegistry().getItems().get(id);
    }

    public static IndustrialRevivalItem getByItem(@Nullable ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        if (item instanceof IndustrialRevivalItemStack irStack) {
            return getById(irStack.getId());
        }

        String id = PersistentDataAPI.getString(item.getItemMeta(), Constants.ITEM_ID_KEY);
        if (id != null) {
            return getById(id);
        }

        return null;
    }

    public IndustrialRevivalItemStack getItem() {
        return itemStack;
    }

    public IndustrialRevivalItem addCraftMethod(@Nonnull CraftMethod craftMethod) {
        Preconditions.checkArgument(craftMethod != null, "CraftMethod cannot be null");
        craftMethods.add(craftMethod);

        if (craftMethod.getRecipeType() == RecipeType.VANILLA_CRAFTING) {
            NamespacedKey key = new NamespacedKey(addon.getPlugin(), "rt_crafting_" + getId().toLowerCase());
            ShapedRecipe shapedRecipe = new ShapedRecipe(key, itemStack.clone());
            shapedRecipe.shape("abc", "def", "ghi");
            char[] chars = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i'};
            for (int i = 0; i < 9; i++) {
                if (craftMethod.getIngredients()[i] != null) {
                    shapedRecipe.setIngredient(chars[i], craftMethod.getIngredients()[i]);
                }
            }

            Bukkit.addRecipe(shapedRecipe);
        }

        RecipeContents.addRecipeContent(this.getId(),
                new RecipeContent(
                        craftMethod.getRecipeType(),
                        craftMethod.getRecipeType().getMakerItem(),
                        craftMethod.getIngredients(),
                        this
                )
        );

        return this;
    }

    public String getId() {
        return itemStack.getId();
    }

    /**
     * Registers the item in the registry.
     *
     * @return WILL RETURN NULL IF THE ITEM IS NOT REGISTERED SUCCESSFULLY!!
     */
    public IndustrialRevivalItem register(@Nonnull IndustrialRevivalAddon addon) {
        Preconditions.checkArgument(addon != null, "Addon cannot be null");
        checkRegistered();

        if (!addon.getPlugin().isEnabled()) {
            return null;
        }

        this.addon = addon;

        try {
            this.preRegister();
        } catch (Exception e) {
            e.printStackTrace();
            this.state = ItemState.DISABLED;
            return null;
        }

        try {
            this.postRegister();
        } catch (Exception e) {
            e.printStackTrace();
            this.state = ItemState.DISABLED;
            return null;
        }

        IndustrialRevival.getInstance().getRegistry().registerItem(this);
        this.state = ItemState.ENABLED;

        return this;
    }

    public Component getItemName() {
        return ItemUtils.getDisplayName(getItem());
    }

    @Nullable public <T extends ItemHandler> T getItemHandler(Class<T> clazz) {
        return (T) itemHandlers.get(clazz);
    }

    protected IndustrialRevivalItem addItemHandlers(ItemHandler... handlers) {
        checkRegistered();
        for (ItemHandler handler : handlers) {
            itemHandlers.put(handler.getClass(), handler);
        }
        return this;
    }

    @OverridingMethodsMustInvokeSuper
    protected void preRegister() throws Exception {
        for (ItemHandler handler : itemHandlers.values()) {
            IncompatibleItemHandlerException ex = handler.isCompatible(this);
            if (ex != null) {
                throw ex;
            }
        }

        if (this instanceof ItemDroppable && !ItemUtils.isActualBlock(getItem().getType())) {
            throw new UnsupportedOperationException("Only actual block can be drop items!");
        }
    }

    @OverridingMethodsMustInvokeSuper
    protected void postRegister() {
        if (this instanceof MobDropItem) {
            IndustrialRevival.getInstance().getRegistry().registerMobDrop(this);
        }

        if (this instanceof BlockDropItem) {
            IndustrialRevival.getInstance().getRegistry().registerBlockDrop(this);
        }

        if (this instanceof VanillaSmeltingItem vsi) {
            NamespacedKey key = new NamespacedKey(addon.getPlugin(), "rt_smelting_" + getId().toLowerCase());
            FurnaceRecipe fr = new FurnaceRecipe(
                    key, vsi.getRecipeOutput(), vsi.getRecipeInput(), vsi.getExp(), vsi.getCookingTime());

            Bukkit.addRecipe(fr);
        }
    }

    protected void checkRegistered() {
        if (state != ItemState.UNREGISTERED) {
            throw new IllegalStateException("Item is registered and cannot be modified");
        }
    }

    public boolean isDisabledInWorld(@Nonnull World world) {
        return state == ItemState.DISABLED || disabledInWorld.contains(world.getName());
    }

    public IndustrialRevivalItem setDisabledInWorld(@Nonnull World world, boolean disabled) {
        Preconditions.checkArgument(world != null, "World cannot be null");
        ConfigurationSection setting = getItemSetting();
        List<String> worlds = setting.getStringList("disabled_in_worlds");

        if (disabled) {
            disabledInWorld.add(world.getName());
            worlds.add(world.getName());
        } else {
            disabledInWorld.remove(world.getName());
            worlds.remove(world.getName());
        }

        setting.set("disabled_in_worlds", worlds);
        return this;
    }

    @NotNull
    protected ConfigurationSection getItemSetting() {
        return IndustrialRevival.getInstance().getItemSettings().getSetting(getId());
    }

    public IndustrialRevivalItem setWikiText(@Nonnull String wikiText) {
        checkRegistered();
        Preconditions.checkArgument(wikiText != null, "WikiText cannot be null");
        this.wikiText = wikiText;
        return this;
    }

    public IndustrialRevivalItem setDisabled(boolean disabled) {
        checkRegistered();
        if (disabled) {
            this.state = ItemState.DISABLED;
        } else {
            this.state = ItemState.ENABLED;
        }
        return this;
    }

    public IndustrialRevivalItem setItemGroup(@Nonnull ItemGroup group) {
        checkRegistered();
        Preconditions.checkArgument(group != null, "ItemGroup cannot be null");
        this.group = group;
        return this;
    }

    public IndustrialRevivalItem setItemStack(@Nonnull IndustrialRevivalItemStack itemStack) {
        checkRegistered();
        Preconditions.checkArgument(itemStack != null, "ItemStack cannot be null");
        this.itemStack = itemStack;
        return this;
    }

    public boolean isEnabled() {
        return state == ItemState.ENABLED;
    }

    public boolean isDisabled() {
        return state == ItemState.DISABLED;
    }

    @Nullable
    public ItemStack[] getRecipeIngredients(RecipeType recipeType) {
        for (CraftMethod craftMethod : craftMethods) {
            if (craftMethod.getRecipeType() == recipeType) {
                return craftMethod.getIngredients();
            }
        }

        return null;
    }

    @Nullable
    public ItemStack getRecipeOutput(RecipeType recipeType) {
        for (CraftMethod craftMethod : craftMethods) {
            if (craftMethod.getRecipeType() == recipeType) {
                return craftMethod.getOutput();
            }
        }

        return null;
    }

    public enum ItemState {
        UNREGISTERED,
        ENABLED,
        DISABLED
    }
}
