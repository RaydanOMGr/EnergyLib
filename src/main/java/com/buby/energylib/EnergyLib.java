package com.buby.energylib;

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
import org.bukkit.plugin.java.JavaPlugin;

public final class EnergyLib extends JavaPlugin {

    public MachineManager machineManager;
    public static final String ID_KEY = "machine-id";
    public static final String LAST_INTERACTION_KEY = "last-interaction-millis";

    @Override
    public void onEnable() {
        machineManager = new MachineManager(this);
        register(machineManager);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public void register(Listener... listeners){
        for(Listener listener : listeners){
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }
}
