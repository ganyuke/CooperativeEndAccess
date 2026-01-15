// SPDX-License-Identifier: MPL-2.0
// SPDX-FileCopyrightText: ganyuke
package io.github.ganyuke.cooperativeEndAccess.portal;

import io.github.ganyuke.cooperativeEndAccess.util.BlockKey;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.EndPortalFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class PortalUtils {
    protected static class PortalResult {
        boolean isActive;
        String stabilizerName;
        final Set<UUID> nearbyPlayers = new HashSet<>();
        final List<String> missingNames = new ArrayList<>();
    }

    private static boolean isEye(ItemStack item) {
        return item != null && item.getType() == Material.ENDER_EYE;
    }

    protected record PortalInteractionContext(
            PlayerInteractEvent event,
            Block block,
            BlockKey center,
            Player player,
            UUID playerId,
            EndPortalFrame frame,
            boolean isHoldingNothing,
            boolean isHoldingEye,
            boolean isFrameFilled
    ) {
        public static PortalInteractionContext from(PlayerInteractEvent event) {
            Block block = event.getClickedBlock();
            EndPortalFrame frame;
            BlockKey center;
            boolean isFrameFilled;

            if (block == null || block.getType() != Material.END_PORTAL_FRAME) {
                frame = null;
                center = null;
                isFrameFilled = false;
            } else {
                frame = (EndPortalFrame) block.getBlockData();
                center = PortalUtils.findPortalCenter(BlockKey.from(block.getLocation()));
                isFrameFilled = frame.hasEye();
            }

            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();
            boolean isHoldingNothing = event.getItem() == null;
            boolean isHoldingEye = !isHoldingNothing && PortalUtils.isEye(event.getItem());

            return new PortalInteractionContext(
                    event, block, center, player, uuid,
                    frame, isHoldingNothing, isHoldingEye, isFrameFilled
            );
        }
    }

    private static final int[][] FRAME_OFFSETS = {
            {2, -1}, {2, 0}, {2, 1},   // East side
            {-2, -1}, {-2, 0}, {-2, 1}, // West side
            {-1, 2}, {0, 2}, {1, 2},    // South side
            {-1, -2}, {0, -2}, {1, -2}  // North side
    };

    @FunctionalInterface
    public interface FrameProcessor<T> {
        boolean process(BlockKey frameLoc, T accumulator);
    }

    public static <T> T processFrameLocations(BlockKey center, T initial, FrameProcessor<T> processor) {
        // assumes that the End Portal is a standard 5x5 End Portal without corners.
        // assumes that all frames are facing the correct direction.
        int cx = center.x();
        int cy = center.y(); // End Portals are always horizontal, so no Y-offset needed
        int cz = center.z();

        for (int[] offset : PortalUtils.FRAME_OFFSETS) {
            int x = cx + offset[0];
            int z = cz + offset[1];

            BlockKey frameLoc = new BlockKey(center.worldName(), x, cy, z);
            if (!processor.process(frameLoc, initial)) {
                return initial; // More direct - exit immediately
            }
        }

        return initial;
    }

    public static boolean isEndPortalValid(BlockKey center, boolean shouldBeFilled) {
        // calculate if End Portal frames surround the center and (optionally) are
        // all filled with eyes.
        World world = center.getWorld();
        if (world == null) return false;

        AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        PortalUtils.processFrameLocations(center, atomicBoolean, (frameLoc, acc) -> {
            Block block = world.getBlockAt(frameLoc.x(), frameLoc.y(), frameLoc.z());
            if (block.getType() != Material.END_PORTAL_FRAME) {
                acc.set(false);
                return false;
            }

            if (shouldBeFilled) {
                EndPortalFrame frame = (EndPortalFrame) block.getBlockData();
                if (!frame.hasEye()) {
                    acc.set(false);
                    return false;
                }
            }

            acc.set(true);
            return true;
        });

        return atomicBoolean.get();
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

        AtomicInteger count = new AtomicInteger(0);
        PortalUtils.processFrameLocations(center, count, (frameLoc, acc) -> {
            Block block = world.getBlockAt(frameLoc.x(), frameLoc.y(), frameLoc.z());
            if (block.getType() != Material.END_PORTAL_FRAME) {
                acc.set(-1);
                return false;
            }

            EndPortalFrame frame = (EndPortalFrame) block.getBlockData();
            if (frame.hasEye()) {
                acc.incrementAndGet();
            }
            return true;
        });

        return count.get();
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
