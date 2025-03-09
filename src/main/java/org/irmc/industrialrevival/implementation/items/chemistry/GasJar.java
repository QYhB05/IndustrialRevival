package org.irmc.industrialrevival.implementation.items.chemistry;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.irmc.industrialrevival.api.elements.ElementProportion;
import org.irmc.industrialrevival.api.elements.ElementType;
import org.irmc.industrialrevival.api.elements.reaction.ReactResult;
import org.irmc.industrialrevival.api.items.IndustrialRevivalItem;
import org.irmc.industrialrevival.api.items.attributes.ChemReactable;
import org.irmc.industrialrevival.api.items.attributes.GasStorage;
import org.irmc.industrialrevival.implementation.IndustrialRevival;
import org.irmc.industrialrevival.utils.KeyUtil;
import org.irmc.pigeonlib.objects.percentage.PositiveHundredPercentage;
import org.irmc.pigeonlib.pdc.PersistentDataAPI;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GasJar extends IndustrialRevivalItem implements GasStorage {
    private static final NamespacedKey CATHER_KEY = KeyUtil.customKey("cather_insertion_method");
    private static final NamespacedKey GAS_STORAGE_CAPACITY_KEY = KeyUtil.customKey("gas_storage_capacity");
    private static final NamespacedKey STORED_REACTABLE_KEY = KeyUtil.customKey("reactable");

    private static final NamespacedKey ITEM_KEY = KeyUtil.customKey("gas_jar");

    public GasJar() {
        setAddon(IndustrialRevival.getInstance());
        setId(ITEM_KEY);

        registerReactable();
    }

    @Override
    public CatheterInsertionMethod getCatheterInsertionMethod(ItemStack item) {
        return CatheterInsertionMethod.values()[PersistentDataAPI.getOrDefault(item.getItemMeta(), CATHER_KEY, PersistentDataType.INTEGER, 0)];
    }

    @Override
    public void setCatheterInsertionMethod(CatheterInsertionMethod catheterInsertionMethod, ItemStack item) {
        PersistentDataAPI.set(item.getItemMeta(), CATHER_KEY, PersistentDataType.INTEGER, catheterInsertionMethod.ordinal());
    }

    @Override
    public PositiveHundredPercentage getGasStorageCapacity(ItemStack item) {
        return PositiveHundredPercentage.fromDoublePercentage(PersistentDataAPI.getOrDefault(item.getItemMeta(), GAS_STORAGE_CAPACITY_KEY, PersistentDataType.DOUBLE, 0.0));
    }

    @Override
    public void setGasStorageCapacity(PositiveHundredPercentage gasStorageCapacity, ItemStack item) {
        PersistentDataAPI.set(item.getItemMeta(), GAS_STORAGE_CAPACITY_KEY, PersistentDataType.DOUBLE, gasStorageCapacity.getDecimalValue());
    }

    @Override
    public void insertGas(ItemStack item, ChemReactable gas) {
        PersistentDataAPI.set(item.getItemMeta(), STORED_REACTABLE_KEY, PersistentDataType.STRING, gas.getKey().toString());
    }

    @Override
    public ElementProportion[] getElementProportions(ItemStack item) {
        String key = PersistentDataAPI.getOrDefault(item.getItemMeta(), STORED_REACTABLE_KEY, PersistentDataType.STRING, "");
        if (key.isEmpty()) {
            return new ElementProportion[0];
        }

        ChemReactable reactable = IndustrialRevival.getInstance().getRegistry().getChemReactables().get(NamespacedKey.fromString(key));
        if (reactable == null) {
            return new ElementProportion[0];
        }

        return reactable.getElementProportions(item); // gas will host the element proportions
    }

    @Override
    public int getMass(ItemStack item) {
        String key = PersistentDataAPI.getOrDefault(item.getItemMeta(), STORED_REACTABLE_KEY, PersistentDataType.STRING, "");
        if (key.isEmpty()) {
            return 0;
        }

        ChemReactable reactable = IndustrialRevival.getInstance().getRegistry().getChemReactables().get(NamespacedKey.fromString(key));
        if (reactable == null) {
            return 0;
        }

        return reactable.getMass(item); // gas will host the mass
    }

    @Override
    public ReactResult react(ItemStack item, ElementType.ReactCondition[] conditions, ChemReactable... other) {
        if (getMass(item) == 0) {
            return ReactResult.FAILED;
        }

        List<ChemReactable> sediments = new ArrayList<>();
        List<ChemReactable> gases = new ArrayList<>();
        List<ChemReactable> others = new ArrayList<>();

        for (ChemReactable reactable : other) {
            if (reactable.getMass(item) == 0) {
                continue;
            }

            List<ChemReactable> reactants = new ArrayList<>(List.of(other));
            reactants.remove(reactable);

            ReactResult reactResult = reactable.react(item, conditions, reactants.toArray(new ChemReactable[]{}));

            if (reactResult != ReactResult.FAILED) {
                if (reactResult.sediments() != null) {
                    sediments.addAll(List.of(reactResult.sediments().reactables()));
                }

                if (reactResult.gases() != null) {
                    gases.addAll(List.of(reactResult.gases().reactables()));
                }

                if (reactResult.otherReactables() != null && reactResult.otherReactables().length != 0) {
                    others.addAll(List.of(reactResult.otherReactables()));
                }
            }
        }

        return new ReactResult(
                new ReactResult.Sediment(sediments.toArray(new ChemReactable[]{})),
                new ReactResult.Gas(gases.toArray(new ChemReactable[]{})),
                others.toArray(new ChemReactable[]{})
        );
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return ITEM_KEY;
    }
}
