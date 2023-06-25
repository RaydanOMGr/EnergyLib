package com.buby.energylib;

import com.buby.energylib.event.UpdateSecondEvent;
import com.buby.energylib.impl.CoalMiner;
import com.buby.energylib.impl.GeneratorMachine;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class MachineManager implements Listener {

    private final EnergyLib plugin;
    private final Set<MachineBlock> machineRegistry = new HashSet<>();

    public MachineManager(EnergyLib plugin){
        this.plugin = plugin;

        registerMachine(new GeneratorMachine(plugin));
        registerMachine(new CoalMiner(plugin));

        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.getPluginManager().callEvent(new UpdateSecondEvent());
            }

        }.runTaskTimer(plugin, 0, 20);
    }

    public void registerMachine(MachineBlock block){
        machineRegistry.add(block);
        plugin.register(block);
    }

    public MachineBlock getHandler(Block block){
        for(MachineBlock machineBlock : machineRegistry){
            if(machineBlock.isApplicable(block))
                return machineBlock;
        }
        return null;
    }

    public MachineBlock getHandler(String ID){
        for(MachineBlock machineBlock : machineRegistry){
            if(machineBlock.machineID.equals(ID))
                return machineBlock;
        }
        return null;
    }

    public MachineBlock getHandler(Class<? extends MachineBlock> clazz){
        for(MachineBlock machineBlock : machineRegistry){
            if(machineBlock.getClass().equals(clazz))
                return machineBlock;
        }
        return null;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event){
        Block block = event.getClickedBlock();
        if(block == null) return;

        MachineBlock handler = getHandler(block);
        if(handler == null) return;

        handler.onQuery(block, QueryPriority.PLAYER);
        handler.onInteract(event);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event){
        Block block = event.getBlockPlaced();
        if(block.getType() == Material.FURNACE) {
            MachineBlock handler = getHandler(GeneratorMachine.class);
            handler.applyToBlock(block);
        }
        if(block.getType() == Material.BEACON){
            MachineBlock handler = getHandler(CoalMiner.class);
            handler.applyToBlock(block);
        }

    }
}
