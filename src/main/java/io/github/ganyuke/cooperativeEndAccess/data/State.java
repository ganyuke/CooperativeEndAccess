package io.github.ganyuke.cooperativeEndAccess.data;

import io.github.ganyuke.cooperativeEndAccess.util.Util;

import org.bukkit.Location;

import java.util.*;

public class State {
    private final Map<Location, UUID> eyeOwners;
    private final Set<Location> portalCenters;
    private boolean dragonDefeated;

    public State(Map<Location, UUID> eyeOwners, Set<Location> portalCenters, boolean dragonDefeated) {
        this.eyeOwners = eyeOwners;
        this.portalCenters = portalCenters;
        this.dragonDefeated = dragonDefeated;
    }

    public void addEye(Location center, Location frameLoc, UUID id) {
        eyeOwners.put(frameLoc, id);
        portalCenters.add(center);
    }

    public UUID getEyeOwner(Location frameLoc) {
        return eyeOwners.get(frameLoc);
    }

    public boolean removeEye(Location frameLoc) {
        return eyeOwners.remove(frameLoc) != null;
    }

    public List<Location> getFramesOwnedBy(Location center, UUID playerId) {
        List<Location> ownedFrames = new ArrayList<>();

        int[][] offsets = Util.FRAME_OFFSETS;

        for (int[] off : offsets) {
            Location frameLoc = center.clone().add(off[0], off[1], off[2]);

            UUID ownerId = this.getEyeOwner(frameLoc);
            if (playerId.equals(ownerId)) {
                ownedFrames.add(frameLoc);
            }
        }

        return ownedFrames;
    }

    public Set<UUID> getCommittedPlayers(Location center) {
        Set<UUID> committed = new HashSet<>();

        int[][] offsets = Util.FRAME_OFFSETS;

        for (int[] off : offsets) {
            Location frameLoc = center.clone().add(off[0], off[1], off[2]);
            UUID owner = this.getEyeOwner(frameLoc);

            if (owner != null) {
                committed.add(owner);
            }
        }

        return committed;
    }

    public Map<Location, UUID> getEyeOwners() { return eyeOwners; }
    public Set<Location> getPortalCenters() { return portalCenters; }

    public void setDragonDefeatedStatus(boolean status) { this.dragonDefeated = status; }
    public boolean getDragonDefeatStatus() { return dragonDefeated; }
}