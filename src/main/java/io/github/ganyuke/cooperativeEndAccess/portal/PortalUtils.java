package io.github.ganyuke.cooperativeEndAccess.portal;

import io.github.ganyuke.cooperativeEndAccess.util.BlockKey;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.EndPortalFrame;

import java.util.*;

public final class PortalUtils {
    protected static class PortalResult {
        boolean isActive;
        String stabilizerName;
        final Set<UUID> nearbyPlayers = new HashSet<>();
        final List<String> missingNames = new ArrayList<>();
    }

    public static final int[][] FRAME_OFFSETS = {
            {2, -1}, {2, 0}, {2, 1},   // East side
            {-2, -1}, {-2, 0}, {-2, 1}, // West side
            {-1, 2}, {0, 2}, {1, 2},    // South side
            {-1, -2}, {0, -2}, {1, -2}  // North side
    };

    private static boolean isEndPortalValid(BlockKey center, boolean shouldBeFilled) {
        // calculate if End Portal frames surround the center and (optionally) are
        // all filled with eyes.
        // assumes that the End Portal is a standard 5x5 End Portal without corners.
        // assumes that all frames are facing the correct direction.
        World world = center.getWorld();
        if (world == null) return false;

        int cx = center.x();
        int cy = center.y(); // End Portals are always horizontal, so no Y-offset needed
        int cz = center.z();

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

    public static boolean isEndPortalFilled(BlockKey center) {
        return isEndPortalValid(center, true);
    }

    public static boolean isEndPortalCenter(BlockKey center) {
        return isEndPortalValid(center, false);
    }

    public static int countEndFrameEyes(BlockKey center) {
        World world = center.getWorld();
        if (world == null) return -1;

        int cx = center.x();
        int cy = center.y(); // End Portals are always horizontal, so no Y-offset needed
        int cz = center.z();

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

    public static BlockKey findPortalCenter(BlockKey frameLoc) {
        World world = frameLoc.getWorld();
        if (world == null) return null;

        int fx = frameLoc.x();
        int fy = frameLoc.y();
        int fz = frameLoc.z();

        Block b = world.getBlockAt(fx, fy, fz);
        if (b.getType() != Material.END_PORTAL_FRAME) return null;

        EndPortalFrame data = (EndPortalFrame) b.getBlockData();
        BlockFace facing = data.getFacing();

        int inwardX = facing.getModX();
        int inwardZ = facing.getModZ();

        // center of portal is always 2 blocks inward from the frame
        // so we just need to figure out the direction of the frame
        // and whether it is a north-south or east-west frame
        int centerXAxis = fx + (inwardX * 2);
        int centerZAxis = fz + (inwardZ * 2);

        String worldName = frameLoc.worldName();

        // the center is now three possible locations: to the left, to the right, or in the middle
        BlockKey[] candidates = {
                new BlockKey(worldName, centerXAxis, fy, centerZAxis),
                // swap inwardX/Z to get sideways direction
                new BlockKey(worldName, centerXAxis + inwardZ, fy, centerZAxis + inwardX),
                new BlockKey(worldName, centerXAxis - inwardZ, fy, centerZAxis - inwardX)
        };

        for (BlockKey loc : candidates) {
            if (isEndPortalCenter(loc)) return loc;
        }

        return null;
    }
}
