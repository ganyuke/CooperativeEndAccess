// SPDX-License-Identifier: MPL-2.0
// SPDX-FileCopyrightText: ganyuke
package io.github.ganyuke.cea.core.portal;

import io.github.ganyuke.cea.core.util.BlockKey;
import io.github.ganyuke.cea.core.util.BlockType;
import io.github.ganyuke.cea.core.util.Direction;
import io.github.ganyuke.cea.core.util.PlatformFacade;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public final class PortalGeometry {
    private static final int[][] FRAME_OFFSETS = {
                     {-1, -2}, { 0, -2}, { 1, -2},
            {-2, -1},                             { 2, -1},
            {-2,  0},                             { 2,  0},
            {-2,  1},                             { 2,  1},
                     {-1,  2}, { 0,  2}, { 1,  2},
    };

    private final PlatformFacade platform;

    public PortalGeometry(PlatformFacade platform) {
        this.platform = platform;
    }

    private static Direction expectedFacing(@NotNull BlockKey frameLoc, @NotNull BlockKey center) {
        int dx = frameLoc.x() - center.x();
        int dz = frameLoc.z() - center.z();
        if (Math.abs(dx) > Math.abs(dz)) return dx > 0 ? Direction.WEST : Direction.EAST;
        return dz > 0 ? Direction.NORTH : Direction.SOUTH;
    }

    public static Stream<BlockKey> getFrameLocations(@NotNull BlockKey center) {
        int cx = center.x();
        int cy = center.y();
        int cz = center.z();

        return Arrays.stream(FRAME_OFFSETS)
                .map(offset -> new BlockKey(center.worldName(), cx + offset[0], cy, cz + offset[1]));
    }

    public boolean isEndPortalValid(BlockKey center, boolean shouldBeFilled) {
        return PortalGeometry.getFrameLocations(center)
                .allMatch(frameLoc -> {
                    if (platform.blockType(frameLoc) != BlockType.END_PORTAL_FRAME) {
                        return false;
                    }
                    if (shouldBeFilled) {
                        if (platform.endFrameFacing(frameLoc) != expectedFacing(frameLoc, center)) {
                            return false;
                        }
                        return platform.endFrameHasEye(frameLoc);
                    }
                    return true;
                });
    }

    public boolean isEndPortalFilled(BlockKey center) { return isEndPortalValid(center, true); }
    public boolean isEndPortalCenter(BlockKey center) { return isEndPortalValid(center, false); }

    public int countEndFrameEyes(BlockKey center) {
        return (int) PortalGeometry.getFrameLocations(center)
                .filter(frameLoc ->
                        platform.blockType(frameLoc) == BlockType.END_PORTAL_FRAME &&
                                platform.endFrameHasEye(frameLoc)
                )
                .count();
    }

    public @Nullable BlockKey findPortalCenter(BlockKey frameLoc) {
        if (platform.blockType(frameLoc) != BlockType.END_PORTAL_FRAME) return null;
        Direction facing = platform.endFrameFacing(frameLoc);
        if (facing == null) return null;
        int centerXAxis = frameLoc.x() + (facing.modX() * 2);
        int centerZAxis = frameLoc.z() + (facing.modZ() * 2);
        BlockKey[] candidates = {
                new BlockKey(frameLoc.worldName(), centerXAxis, frameLoc.y(), centerZAxis),
                new BlockKey(frameLoc.worldName(), centerXAxis + facing.modZ(), frameLoc.y(), centerZAxis + facing.modX()),
                new BlockKey(frameLoc.worldName(), centerXAxis - facing.modZ(), frameLoc.y(), centerZAxis - facing.modX())
        };
        for (BlockKey loc : candidates) if (isEndPortalCenter(loc)) return loc;
        return null;
    }
}
