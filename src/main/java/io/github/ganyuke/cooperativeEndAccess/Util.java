package io.github.ganyuke.cooperativeEndAccess;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public final class Util {
    public static String locToString(Location l) { return l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ(); }
    public static Location stringToLoc(String s) {
        String[] p = s.split(",");
        return new Location(Bukkit.getWorld(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
    }
}
