package com.buby.energylib.impl;

import com.buby.energylib.EnergyLib;
import com.buby.energylib.MachineBlock;
import com.buby.energylib.QueryPriority;
import com.buby.energylib.Util;
import com.buby.energylib.event.UpdateSecondEvent;
import com.buby.energylib.impl.inertf.EnergyConductor;
import com.buby.energylib.impl.inertf.EnergyConsumer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Furnace;
import org.bukkit.block.TileState;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class CoalMiner extends MachineBlock implements EnergyConsumer {
    public CoalMiner(EnergyLib plugin) {
        super(plugin, "COAL_MINER");
    }

    @Override
    public void onQuery(Block block, QueryPriority priority) {
        blockCache.add(block);
        TileState tile = (TileState) block.getState();
        PersistentDataContainer pdc = tile.getPersistentDataContainer();

        pdc.set(new NamespacedKey(plugin, EnergyLib.LAST_INTERACTION_KEY), PersistentDataType.LONG, System.currentTimeMillis());
        tile.update();

        long lastGen = pdc.get(new NamespacedKey(plugin, "LAST_GENERATION"), PersistentDataType.LONG);
        long millisPerCoal = pdc.get(new NamespacedKey(plugin, "MILLIS_PER_COAL"), PersistentDataType.LONG);
        long lastEnergyTick = pdc.get(new NamespacedKey(plugin, "LAST_ENERGY_TICK"), PersistentDataType.LONG);
        long millisPerEnergy = pdc.get(new NamespacedKey(plugin, "MILLIS_PER_ENERGY_TICK"), PersistentDataType.LONG);
        int energyConsumption = pdc.get(new NamespacedKey(plugin, "ENERGY_CONSUMPTION"), PersistentDataType.INTEGER);
        int coalAmount = pdc.get(new NamespacedKey(plugin, "COAL_AMOUNT"), PersistentDataType.INTEGER);

        //skip if no energy connected
        MachineBlock energySource = getConnectedEnergySource(block);
        if(energySource == null) return;

        Block energyBlock = getConnectedEnergySourceBlock(block);
        TileState energyTile = (TileState) energyBlock.getState();
        PersistentDataContainer energyPDC = energyTile.getPersistentDataContainer();

        energySource.onQuery(energyBlock, QueryPriority.CHAIN_NECESSITY);
        int currentEnergy = energyPDC.get(new NamespacedKey(plugin, "CURRENT_ENERGY"), PersistentDataType.INTEGER);
        if(currentEnergy == 0) return;



        //coal tick
        if(System.currentTimeMillis() - lastGen > millisPerCoal){
            lastGen = System.currentTimeMillis();
            //coalAmount += 1;

            //add to connected generator
            if(energySource instanceof  GeneratorMachine){
                int fuelAmount = energyPDC.get(new NamespacedKey(plugin, "FUEL_AMOUNT"), PersistentDataType.INTEGER);
                int maxFuelAmount = energyPDC.get(new NamespacedKey(plugin, "MAX_FUEL_AMOUNT"), PersistentDataType.INTEGER);

                if(fuelAmount < maxFuelAmount){
                    fuelAmount += 1;
                    energyPDC.set(new NamespacedKey(plugin, "FUEL_AMOUNT"), PersistentDataType.INTEGER, fuelAmount);
                    energyTile.update();
                    return;
                }
            }

            //Spit out coal
            block.getWorld().dropItem(block.getLocation().add(0, 1, 0), new ItemStack(Material.COAL, 1));

        }

        //energy tick
        if(System.currentTimeMillis() - lastEnergyTick > millisPerEnergy){
            lastEnergyTick = System.currentTimeMillis();
            currentEnergy -= energyConsumption;
            energyPDC.set(new NamespacedKey(plugin, "CURRENT_ENERGY"), PersistentDataType.INTEGER, currentEnergy);
            energyTile.update();
        }

        pdc.set(new NamespacedKey(plugin, "COAL_AMOUNT"), PersistentDataType.INTEGER, coalAmount);
        pdc.set(new NamespacedKey(plugin, "LAST_GENERATION"), PersistentDataType.LONG, lastGen);
        pdc.set(new NamespacedKey(plugin, "LAST_ENERGY_TICK"), PersistentDataType.LONG, lastEnergyTick);
        tile.update();
    }

    @Override
    public void onInteract(PlayerInteractEvent event){
        if(event.getAction() == Action.LEFT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        TileState tile = (TileState) block.getState();

        PersistentDataContainer pdc = tile.getPersistentDataContainer();
        String machineID = pdc.get(new NamespacedKey(plugin, EnergyLib.ID_KEY), PersistentDataType.STRING);
        long lastQuery = pdc.get(new NamespacedKey(plugin, EnergyLib.LAST_INTERACTION_KEY), PersistentDataType.LONG);
        long lastGen = pdc.get(new NamespacedKey(plugin, "LAST_GENERATION"), PersistentDataType.LONG);
        long lastEnergyTick = pdc.get(new NamespacedKey(plugin, "LAST_ENERGY_TICK"), PersistentDataType.LONG);

        long millisPerCoal = pdc.get(new NamespacedKey(plugin, "MILLIS_PER_COAL"), PersistentDataType.LONG);
        long millisPerEnergy = pdc.get(new NamespacedKey(plugin, "MILLIS_PER_ENERGY_TICK"), PersistentDataType.LONG);
        int energyConsumption = pdc.get(new NamespacedKey(plugin, "ENERGY_CONSUMPTION"), PersistentDataType.INTEGER);

        int coalAmount = pdc.get(new NamespacedKey(plugin, "COAL_AMOUNT"), PersistentDataType.INTEGER);


        player.sendMessage(Util.translate("&7ID: &e" + machineID));
        player.sendMessage(Util.translate("&7Last Query (Millis): &e" + lastQuery + " (now)"));
        player.sendMessage(Util.translate("&7----------"));
        player.sendMessage(Util.translate("&7Last Generation (Millis): &e" + lastGen));
        player.sendMessage(Util.translate("&7Last Energy Tick (Millis): &e" + lastEnergyTick));
        player.sendMessage(Util.translate("&7Amount Stored: &e" + coalAmount));
        player.sendMessage(Util.translate("&7Coal Per Second: &e" + 1000/millisPerCoal + "&7/&e1s"));
        player.sendMessage(Util.translate("&7Energy Consumption: &e" + (1000D/(double)millisPerEnergy) * (double)energyConsumption + "&7/&e1s"));

        MachineBlock energySource = getConnectedEnergySource(block);
        player.sendMessage(Util.translate("&7Power Source: " + (energySource == null ? "&cNot Connected" : "&aConnected -> &e" + energySource.machineID)));


        event.setCancelled(true);
    }

    @Override
    public void applyToBlock(Block block){
        if(!(block.getState() instanceof TileState tile)) return;
        blockCache.add(block);

        PersistentDataContainer pdc = tile.getPersistentDataContainer();
        pdc.set(new NamespacedKey(plugin, EnergyLib.ID_KEY), PersistentDataType.STRING, machineID);
        pdc.set(new NamespacedKey(plugin, EnergyLib.LAST_INTERACTION_KEY), PersistentDataType.LONG, System.currentTimeMillis());
        pdc.set(new NamespacedKey(plugin, "LAST_GENERATION"), PersistentDataType.LONG, System.currentTimeMillis());
        pdc.set(new NamespacedKey(plugin, "LAST_ENERGY_TICK"), PersistentDataType.LONG, System.currentTimeMillis());


        pdc.set(new NamespacedKey(plugin, "MILLIS_PER_COAL"), PersistentDataType.LONG, 1000L);
        pdc.set(new NamespacedKey(plugin, "MILLIS_PER_ENERGY_TICK"), PersistentDataType.LONG, 1000L);
        pdc.set(new NamespacedKey(plugin, "ENERGY_CONSUMPTION"), PersistentDataType.INTEGER, 10);

        pdc.set(new NamespacedKey(plugin, "COAL_AMOUNT"), PersistentDataType.INTEGER, 0);



        tile.update();
    }

    public MachineBlock getConnectedEnergySource(Block block){
        BlockFace[] dirs = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        for(BlockFace dir : dirs){
            Block rel = block.getRelative(dir);
            MachineBlock machineBlock = plugin.machineManager.getHandler(rel);
            if(machineBlock == null) continue;

            if(machineBlock instanceof EnergyConductor){
                return machineBlock;
            }
        }
        return null;
    }

    public Block getConnectedEnergySourceBlock(Block block){
        BlockFace[] dirs = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        for(BlockFace dir : dirs){
            Block rel = block.getRelative(dir);
            MachineBlock machineBlock = plugin.machineManager.getHandler(rel);
            if(machineBlock == null) continue;

            if(machineBlock instanceof EnergyConductor){
                return rel;
            }
        }
        return null;
    }

    private Set<Block> blockCache = new HashSet<>();

    //Tick every second
    @EventHandler
    public void updateSecond(UpdateSecondEvent event){
        for(Block block : blockCache){
            if(block == null || !isApplicable(block)){
                continue;
            }
            onQuery(block, QueryPriority.CHAIN);
        }
    }

}
