// SPDX-License-Identifier: MPL-2.0
// SPDX-FileCopyrightText: ganyuke
package io.github.ganyuke.cooperativeEndAccess.portal;

import io.github.ganyuke.cooperativeEndAccess.util.BlockKey;
import org.bukkit.*;
import org.bukkit.block.Block;

import java.util.*;

class PortalWorldTechnician {
    private final Map<BlockKey, Boolean> portalPhysicalState = new HashMap<>();
    private final PortalFeedback portalFeedback;

    PortalWorldTechnician(PortalFeedback portalFeedback) {
        this.portalFeedback = portalFeedback;
    }

    private void fillPortal(BlockKey center, Material material) {
        World world = center.getWorld();
        if (world == null) return;

        int cx = center.x();
        int cy = center.y(); // End Portals are always horizontal, so no Y-offset needed
        int cz = center.z();

        for (int offX = -1; offX <= 1; offX++) {
            for (int offZ = -1; offZ <= 1; offZ++) {
                int x = cx + offX;
                int z = cz + offZ;

                Block b = world.getBlockAt(x, cy, z);
                if (b.getType() != material) {
                    b.setType(material, false);
                }
            }
        }
    }

    protected void updatePhysicalPortal(BlockKey center, boolean shouldBeOpen) {
        if (!portalPhysicalState.containsKey(center)) {
            World world = center.getWorld();
            if (world == null) return;
            // handle when server restarts while an unstable End Portal is open
            boolean isActuallyOpenInWorld = world.getBlockAt(center.x(), center.y(), center.z()).getType() == Material.END_PORTAL;
            portalPhysicalState.put(center, isActuallyOpenInWorld);
        }

        boolean wasOpen = portalPhysicalState.getOrDefault(center, false);

        // when state changes, trigger creating/destroying portal and playing associated SFX
        if (shouldBeOpen != wasOpen) {
            if (shouldBeOpen) {
                this.fillPortal(center, Material.END_PORTAL);
                portalFeedback.openPortalFx(center);
            } else {
                this.fillPortal(center, Material.AIR);
                portalFeedback.closePortalFx(center);
            }
            portalPhysicalState.put(center, shouldBeOpen);
        }
    }


}
