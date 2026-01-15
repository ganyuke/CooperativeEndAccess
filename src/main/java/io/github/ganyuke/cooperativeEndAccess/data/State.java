// SPDX-License-Identifier: MPL-2.0
// SPDX-FileCopyrightText: ganyuke
package io.github.ganyuke.cooperativeEndAccess.data;

import io.github.ganyuke.cooperativeEndAccess.portal.PortalUtils;
import io.github.ganyuke.cooperativeEndAccess.util.BlockKey;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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
        AtomicInteger count = new AtomicInteger(0); // need array to allow mutation
        return PortalUtils.processFrameLocations(center, count, (frameLoc, acc) -> {
            if (id.equals(this.getEyeOwner(frameLoc))) {
                acc.incrementAndGet();
            }
            return true;
        }).get();
    }

    public Set<UUID> getCommittedPlayers(BlockKey center) {
        return PortalUtils.processFrameLocations(center, new HashSet<>(), (frameLoc, committed) -> {
            UUID owner = this.getEyeOwner(frameLoc);
            if (owner != null) {
                committed.add(owner);
            }
            return true;
        });
    }

    public Map<BlockKey, UUID> getEyeOwners() { return eyeOwners; }
    public Set<BlockKey> getPortalCenters() { return portalCenters; }

    public void setDragonDefeatedStatus(boolean status) { this.dragonDefeated = status; }
    public boolean getDragonDefeatStatus() { return dragonDefeated; }
}