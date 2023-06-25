package com.buby.energylib.impl;

import com.buby.energylib.EnergyLib;
import com.buby.energylib.MachineBlock;
import com.buby.energylib.QueryPriority;
import com.buby.energylib.Util;
import com.buby.energylib.impl.inertf.EnergyConductor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class GeneratorMachine extends MachineBlock implements EnergyConductor {
    public GeneratorMachine(EnergyLib plugin) {
        super(plugin, "GENERATOR");
    }

    @Override
    public void onQuery(Block block, QueryPriority priority) {
        TileState tile = (TileState) block.getState();
        PersistentDataContainer pdc = tile.getPersistentDataContainer();

        pdc.set(new NamespacedKey(plugin, EnergyLib.LAST_INTERACTION_KEY), PersistentDataType.LONG, System.currentTimeMillis());
        tile.update();

        //Calculate power & consumption
        long lastQuery = pdc.get(new NamespacedKey(plugin, EnergyLib.LAST_INTERACTION_KEY), PersistentDataType.LONG);
        long lastFuelInsert = pdc.get(new NamespacedKey(plugin, "LAST_FUEL_INSERT"), PersistentDataType.LONG);

        int maxEnergy = pdc.get(new NamespacedKey(plugin, "MAX_ENERGY"), PersistentDataType.INTEGER);
        int currentEnergy = pdc.get(new NamespacedKey(plugin, "CURRENT_ENERGY"), PersistentDataType.INTEGER);
        int maxFuelAmount = pdc.get(new NamespacedKey(plugin, "MAX_FUEL_AMOUNT"), PersistentDataType.INTEGER);
        int fuelAmount = pdc.get(new NamespacedKey(plugin, "FUEL_AMOUNT"), PersistentDataType.INTEGER);
        int fuelEnergy = pdc.get(new NamespacedKey(plugin, "FUEL_ENERGY"), PersistentDataType.INTEGER);
        int fuelDuration = pdc.get(new NamespacedKey(plugin, "FUEL_DURATION_MILLIS"), PersistentDataType.INTEGER);

        if(currentEnergy == maxEnergy) return; // dont need to calculate
        if(fuelAmount == 0) return;

        long elapsedTime = System.currentTimeMillis() - lastFuelInsert;
        int usedFuel = Math.min((int)Math.floor((double) elapsedTime / fuelDuration), fuelAmount);
        int energyDiff = maxEnergy - currentEnergy;

        int fuelPotential = energyDiff/fuelEnergy;
        usedFuel = Math.min(fuelPotential, usedFuel);


        fuelAmount -= usedFuel;
        currentEnergy += usedFuel * fuelEnergy;

        pdc.set(new NamespacedKey(plugin, "FUEL_AMOUNT"), PersistentDataType.INTEGER, fuelAmount);
        pdc.set(new NamespacedKey(plugin, "CURRENT_ENERGY"), PersistentDataType.INTEGER, currentEnergy);


        if(usedFuel > 0)
            pdc.set(new NamespacedKey(plugin, "LAST_FUEL_INSERT"), PersistentDataType.LONG, System.currentTimeMillis());
        tile.update();
    }

    @Override
    public void onInteract(PlayerInteractEvent event){
        if(event.getAction() == Action.LEFT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        TileState tile = (TileState) block.getState();

        PersistentDataContainer pdc = tile.getPersistentDataContainer();

        //update fuel if shift clicked
        if(player.isSneaking()
                && player.getInventory().getItemInMainHand().getType() == Material.COAL
                && event.getAction() == Action.RIGHT_CLICK_BLOCK){
            int maxFuelAmount = pdc.get(new NamespacedKey(plugin, "MAX_FUEL_AMOUNT"), PersistentDataType.INTEGER);
            int fuelAmount = pdc.get(new NamespacedKey(plugin, "FUEL_AMOUNT"), PersistentDataType.INTEGER);
            fuelAmount = Math.min(fuelAmount, maxFuelAmount);
            pdc.set(new NamespacedKey(plugin, "FUEL_AMOUNT"), PersistentDataType.INTEGER, fuelAmount + 1);
            tile.update();
        }

        String machineID = pdc.get(new NamespacedKey(plugin, EnergyLib.ID_KEY), PersistentDataType.STRING);
        long lastQuery = pdc.get(new NamespacedKey(plugin, EnergyLib.LAST_INTERACTION_KEY), PersistentDataType.LONG);
        long lastFuelInsert = pdc.get(new NamespacedKey(plugin, "LAST_FUEL_INSERT"), PersistentDataType.LONG);

        int maxEnergy = pdc.get(new NamespacedKey(plugin, "MAX_ENERGY"), PersistentDataType.INTEGER);
        int currentEnergy = pdc.get(new NamespacedKey(plugin, "CURRENT_ENERGY"), PersistentDataType.INTEGER);
        int maxFuelAmount = pdc.get(new NamespacedKey(plugin, "MAX_FUEL_AMOUNT"), PersistentDataType.INTEGER);
        int fuelAmount = pdc.get(new NamespacedKey(plugin, "FUEL_AMOUNT"), PersistentDataType.INTEGER);
        int fuelEnergy = pdc.get(new NamespacedKey(plugin, "FUEL_ENERGY"), PersistentDataType.INTEGER);
        int fuelDuration = pdc.get(new NamespacedKey(plugin, "FUEL_DURATION_MILLIS"), PersistentDataType.INTEGER);

        player.sendMessage(Util.translate("&7ID: &e" + machineID));
        player.sendMessage(Util.translate("&7Last Query (Millis): &e" + lastQuery + " (now)"));
        player.sendMessage(Util.translate("&7----------"));
        player.sendMessage(Util.translate("&7Last Fuel Insert (Millis): &e" + lastFuelInsert));
        player.sendMessage(Util.translate("&7Energy: &e" + currentEnergy + "&7/&e" + maxEnergy));
        player.sendMessage(Util.translate("&7Fuel Amount: &e" + fuelAmount + "&7/&e" + maxFuelAmount));
        player.sendMessage(Util.translate("&7Energy Per Fuel: &e" + fuelEnergy));
        player.sendMessage(Util.translate("&7Fuel Duration (Millis): &e" + fuelDuration));
        player.sendMessage(Util.translate("&7Fuel Duration (Second): &e" + (fuelDuration/1000)));


        event.setCancelled(true);
    }

    @Override
    public void applyToBlock(Block block){
        if(!(block.getState() instanceof TileState tile)) return;

        PersistentDataContainer pdc = tile.getPersistentDataContainer();
        pdc.set(new NamespacedKey(plugin, EnergyLib.ID_KEY), PersistentDataType.STRING, machineID);
        pdc.set(new NamespacedKey(plugin, EnergyLib.LAST_INTERACTION_KEY), PersistentDataType.LONG, System.currentTimeMillis());
        pdc.set(new NamespacedKey(plugin, "LAST_FUEL_INSERT"), PersistentDataType.LONG, System.currentTimeMillis());

        pdc.set(new NamespacedKey(plugin, "MAX_ENERGY"), PersistentDataType.INTEGER, 100);
        pdc.set(new NamespacedKey(plugin, "CURRENT_ENERGY"), PersistentDataType.INTEGER, 0);

        pdc.set(new NamespacedKey(plugin, "MAX_FUEL_AMOUNT"), PersistentDataType.INTEGER, 64);
        pdc.set(new NamespacedKey(plugin, "FUEL_AMOUNT"), PersistentDataType.INTEGER, 0);
        pdc.set(new NamespacedKey(plugin, "FUEL_ENERGY"), PersistentDataType.INTEGER, 10);
        pdc.set(new NamespacedKey(plugin, "FUEL_DURATION_MILLIS"), PersistentDataType.INTEGER, 1000);



        tile.update();
    }

    //Handle hopper move
    @EventHandler
    public void inventoryMoveEvent(InventoryMoveItemEvent event) {
        Block block = event.getDestination().getLocation().getBlock();

        if (!isApplicable(block)) return;
        TileState tile = (TileState) block.getState();
        PersistentDataContainer pdc = tile.getPersistentDataContainer();

        int maxFuelAmount = pdc.get(new NamespacedKey(plugin, "MAX_FUEL_AMOUNT"), PersistentDataType.INTEGER);
        int fuelAmount = pdc.get(new NamespacedKey(plugin, "FUEL_AMOUNT"), PersistentDataType.INTEGER);

        if (fuelAmount == maxFuelAmount) {
            event.setCancelled(true);
            return;
        }
        fuelAmount += 1;
        pdc.set(new NamespacedKey(plugin, "FUEL_AMOUNT"), PersistentDataType.INTEGER, fuelAmount);
        tile.update();
        ((Furnace)block.getState()).getInventory().clear();
        onQuery(block, QueryPriority.CHAIN);
    }
}
