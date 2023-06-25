package com.buby.energylib;

import org.bukkit.ChatColor;

public class Util {
    public static String translate(String str){
        return ChatColor.translateAlternateColorCodes('&', str);
    }
}
