package com.buby.energylib;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public abstract class MachineBlock implements Listener {
    public final String machineID;
    protected final EnergyLib plugin;


    public MachineBlock(EnergyLib plugin, String machineID){
        this.plugin = plugin;
        this.machineID = machineID;
    }

    public void onInteract(PlayerInteractEvent event){}
    public abstract void onQuery(Block block, QueryPriority priority);

    protected boolean isApplicable(Block block){
        if(block == null) return false;
        if(!(block.getState() instanceof TileState tile)) return false;

        PersistentDataContainer pdc = tile.getPersistentDataContainer();
        String ID = pdc.get(new NamespacedKey(plugin, EnergyLib.ID_KEY), PersistentDataType.STRING);

        return machineID.equals(ID);
    }

    public void applyToBlock(Block block){
        if(!(block.getState() instanceof TileState tile)) return;

        PersistentDataContainer pdc = tile.getPersistentDataContainer();
        pdc.set(new NamespacedKey(plugin, EnergyLib.ID_KEY), PersistentDataType.STRING, machineID);
        pdc.set(new NamespacedKey(plugin, EnergyLib.LAST_INTERACTION_KEY), PersistentDataType.LONG, System.currentTimeMillis());

        tile.update();
    }
}
