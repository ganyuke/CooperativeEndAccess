// SPDX-License-Identifier: MPL-2.0
// SPDX-FileCopyrightText: ganyuke
package io.github.ganyuke.cooperativeEndAccess.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public record BlockKey(String worldName, int x, int y, int z) {
    public static BlockKey from(Location loc) {
        return new BlockKey(
                loc.getWorld().getName(),
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ()
        );
    }

    public static BlockKey from(String s) {
        String[] p = s.split(",");
        return new BlockKey(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
    }

    public Location toLocation() {
        World world = this.getWorld();
        return new Location(world, x, y, z);
    }

    public @Nonnull String toString() {
        return this.worldName + "," + this.x + "," + this.y + "," + this.z;
    }

    @Nullable
    public World getWorld() {
        return Bukkit.getWorld(worldName);
    }
}