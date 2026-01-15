package io.github.ganyuke.cooperativeEndAccess.data;

import io.github.ganyuke.cooperativeEndAccess.portal.PortalUtils;
import io.github.ganyuke.cooperativeEndAccess.util.BlockKey;

import java.util.*;

public class State {
    private final Map<BlockKey, UUID> eyeOwners;
    private final Set<BlockKey> portalCenters;
    private final Map<UUID, String> committedNames;
    private boolean dragonDefeated;

    public State(Map<BlockKey, UUID> eyeOwners, Set<BlockKey> portalCenters, Map<UUID, String> committedNames, boolean dragonDefeated) {
        this.eyeOwners = eyeOwners;
        this.portalCenters = portalCenters;
        this.committedNames = committedNames;
        this.dragonDefeated = dragonDefeated;
    }

    public void addName(UUID uuid, String name) {
        committedNames.put(uuid, name);
    }

    public void removeName(UUID uuid) {
        committedNames.remove(uuid);
    }

    public String getName(UUID uuid) {
        return committedNames.get(uuid);
    }

    public void addEye(BlockKey center, BlockKey frameLoc, UUID id) {
        eyeOwners.put(frameLoc, id);
        portalCenters.add(center);
    }

    public UUID getEyeOwner(BlockKey frameLoc) {
        return eyeOwners.get(frameLoc);
    }

    public Map<UUID, String> getCommittedNames() {
        return committedNames;
    }

    public boolean removeEye(BlockKey frameLoc) {
        BlockKey center = PortalUtils.findPortalCenter(frameLoc);
        if (center == null) return false;
        int eyeCount = PortalUtils.countEndFrameEyes(center);
        if (eyeCount - 1 == 0) {
            // gc centers with no more owners as well
            portalCenters.remove(center);
        }
        return eyeOwners.remove(frameLoc) != null;
    }

    public int countFramesOwnedBy(BlockKey center, UUID id) {
        int cx = center.x();
        int cy = center.y(); // End Portals are always horizontal, so no Y-offset needed
        int cz = center.z();

        int count = 0;

        for (int[] offset : PortalUtils.FRAME_OFFSETS) {
            int x = cx + offset[0];
            int z = cz + offset[1];

            var frameLoc = new BlockKey(center.worldName(), x, cy, z);
            var ownerId = this.getEyeOwner(frameLoc);
            if (id.equals(ownerId)) {
                count += 1;
            }
        }
        return count;
    }

    public Set<UUID> getCommittedPlayers(BlockKey center) {
        int cx = center.x();
        int cy = center.y(); // End Portals are always horizontal, so no Y-offset needed
        int cz = center.z();

        Set<UUID> committed = new HashSet<>();

        for (int[] offset : PortalUtils.FRAME_OFFSETS) {
            int x = cx + offset[0];
            int z = cz + offset[1];


            BlockKey frameLoc = new BlockKey(center.worldName(), x, cy, z);
            UUID owner = this.getEyeOwner(frameLoc);

            if (owner != null) {
                committed.add(owner);
            }
        }
        return committed;
    }

    public Map<BlockKey, UUID> getEyeOwners() { return eyeOwners; }
    public Set<BlockKey> getPortalCenters() { return portalCenters; }

    public void setDragonDefeatedStatus(boolean status) { this.dragonDefeated = status; }
    public boolean getDragonDefeatStatus() { return dragonDefeated; }
}