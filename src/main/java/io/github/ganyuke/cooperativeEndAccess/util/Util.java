package io.github.ganyuke.cooperativeEndAccess.util;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.EndPortalFrame;

public final class Util {
    public static String locToString(Location l) { return l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ(); }
    public static Location stringToLoc(String s) {
        String[] p = s.split(",");
        return new Location(Bukkit.getWorld(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
    }

    public static final int[][] FRAME_OFFSETS = {
            {2, -1}, {2, 0}, {2, 1},   // East side
            {-2, -1}, {-2, 0}, {-2, 1}, // West side
            {-1, 2}, {0, 2}, {1, 2},    // South side
            {-1, -2}, {0, -2}, {1, -2}  // North side
    };

    private static boolean isEndPortalValid(Location center, boolean shouldBeFilled) {
        // calculate if End Portal frames surround the center and (optionally) are
        // all filled with eyes.
        // assumes that the End Portal is a standard 5x5 End Portal without corners.
        // assumes that all frames are facing the correct direction.
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY(); // End Portals are always horizontal, so no Y-offset needed
        int cz = center.getBlockZ();

        for (int[] offset : FRAME_OFFSETS) {
            int x = cx + offset[0];
            int z = cz + offset[1];

            Block block = world.getBlockAt(x, cy, z);
            if (block.getType() != Material.END_PORTAL_FRAME) {
                return false;
            }

            if (shouldBeFilled) {
                EndPortalFrame frame = (EndPortalFrame) block.getBlockData();
                if (!frame.hasEye()) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isEndPortalFilled(Location center) {
        return isEndPortalValid(center, true);
    }

    public static boolean isEndPortalCenter(Location center) {
        return isEndPortalValid(center, false);
    }

    public static int countEndFrameEyes(Location center) {
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY(); // End Portals are always horizontal, so no Y-offset needed
        int cz = center.getBlockZ();

        int count = 0;

        for (int[] offset : FRAME_OFFSETS) {
            int x = cx + offset[0];
            int z = cz + offset[1];

            Block block = world.getBlockAt(x, cy, z);
            if (block.getType() != Material.END_PORTAL_FRAME) {
                return -1;
            }

            EndPortalFrame frame = (EndPortalFrame) block.getBlockData();
            if (frame.hasEye()) {
                count += 1;
            }
        }
        return count;
    }

    public static void fillPortal(Location center, Material material) {
        World world = center.getWorld();
        boolean changed = false;

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block b = center.clone().add(x, 0, z).getBlock();
                if (b.getType() != material) {
                    b.setType(material, false);
                    changed = true;
                }
            }
        }

        if (changed && material == Material.END_PORTAL) {
            world.spawnParticle(
                    Particle.DRAGON_BREATH,
                    center,
                    50,
                    1, 0, 1,
                    0.1,
                    1.0f
            );

        }
    }

    public static Location findPortalCenter(Location frameLoc) {
        World world = frameLoc.getWorld();
        Block b = world.getBlockAt(frameLoc);
        if (b.getType() != Material.END_PORTAL_FRAME) return null;

        EndPortalFrame data = (EndPortalFrame) b.getBlockData();
        BlockFace facing = data.getFacing();

        int fx = b.getX();
        int fy = b.getY();
        int fz = b.getZ();

        int inwardX = facing.getModX();
        int inwardZ = facing.getModZ();

        // center of portal is always 2 blocks inward from the frame
        // so we just need to figure out the direction of the frame
        // and whether it is a north-south or east-west frame
        int centerXAxis = fx + (inwardX * 2);
        int centerZAxis = fz + (inwardZ * 2);

        // the center is now three possible locations: to the left, to the right, or in the middle
        Location[] candidates = {
                new Location(world, centerXAxis, fy, centerZAxis),
                // swap inwardX/Z to get sideways direction
                new Location(world, centerXAxis + inwardZ, fy, centerZAxis + inwardX),
                new Location(world, centerXAxis - inwardZ, fy, centerZAxis - inwardX)
        };

        for (Location loc : candidates) {
            if (isEndPortalCenter(loc)) return loc;
        }

        return null;
    }
}
