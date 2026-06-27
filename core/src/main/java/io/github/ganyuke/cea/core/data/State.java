// SPDX-License-Identifier: MPL-2.0
// SPDX-FileCopyrightText: ganyuke
package io.github.ganyuke.cea.core.data;

import io.github.ganyuke.cea.core.portal.PortalGeometry;
import io.github.ganyuke.cea.core.util.BlockKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The hot state class holding the core data for mapping frame positions to
 * player UUIDs, the positions of portal centers, and the player name cache.
 */
public final class State {
    private final Map<BlockKey, UUID> eyeOwners;
    private final Set<BlockKey> portalCenters;
    private final Map<UUID, String> playerNameCache;
    private boolean dragonDefeated;

    // adapter for locating the portal center and calculating
    // end portal completion
    private final PortalGeometry geometry;

    public State(
            @NotNull Map<BlockKey, UUID> eyeOwners,
            @NotNull Set<BlockKey> portalCenters,
            @NotNull Map<UUID, String> playerNameCache,
            boolean dragonDefeated,
            @NotNull PortalGeometry geometry
    ) {
        this.eyeOwners = eyeOwners;
        this.portalCenters = portalCenters;
        this.playerNameCache = playerNameCache;
        this.dragonDefeated = dragonDefeated;
        this.geometry = geometry;
    }

    /**
     * Add mapping of player UUID to username
     * string to the hot cache.
     * @param uuid player UUID
     * @param name player username
     */
    public void addName(UUID uuid, String name) {
        playerNameCache.put(uuid, name);
    }

    /**
     * Get a player's name from their UUID through
     * the core's hot cache.
     * @param uuid player UUID
     * @return player username associated with the UUID (or {@code null} if not tracked)
     */
    public @Nullable String getName(UUID uuid) {
        return playerNameCache.get(uuid);
    }

    /**
     * Return the entire player UUID-to-username mapping
     * cache. The mapping is immutable.
     * @return immutable map of the player name cache
     */
    public @NotNull Map<UUID, String> getPlayerNameCache() {
        return Collections.unmodifiableMap(playerNameCache);
    }

    /**
     * Assign player UUID with an End Portal frame of a particular
     * End Portal structure.
     * @param center position of the center of the end portal
     * @param frameLoc position of the end portal frame
     * @param uuid player UUID
     */
    public void addEye(BlockKey center, BlockKey frameLoc, UUID uuid) {
        eyeOwners.put(frameLoc, uuid);
        portalCenters.add(center);
    }

    /**
     * Get the player UUID of the owner of an End Portal frame.
     * @param frameLoc position of the end portal frame
     * @return player UUID (or {@code null} if no owner)
     */
    public @Nullable UUID getEyeOwner(BlockKey frameLoc) {
        return eyeOwners.get(frameLoc);
    }

    /**
     * Remove tracked ownership of an End Portal frame location. If
     * this is the last End Portal frame associated with a center,
     * also clean up the center.
     * @param frameLoc position of the end portal frame
     * @return {@code true} if removal was successful, else {@code false}
     */
    public boolean removeEyeOwner(BlockKey frameLoc) {
        boolean removed = eyeOwners.remove(frameLoc) != null;
        BlockKey center = geometry.findPortalCenter(frameLoc);
        if (center != null && geometry.countEndFrameEyes(center) == 0) {
            portalCenters.remove(center);
        }
        return removed;
    }

    /**
     * Count the number of End Portal frames associated with a particular
     * player UUID in a particular End Portal structure (based on the
     * structure's center position).
     * @param center position of the center of the end portal structure
     * @param uuid player uuid
     * @return number of eyes owned by that player
     */
    public int countFramesOwnedBy(BlockKey center, UUID uuid) {
        return (int) PortalGeometry.getFrameLocations(center)
                .filter(frameLoc -> uuid.equals(getEyeOwner(frameLoc)))
                .count();
    }

    /**
     * Get all players associated with a particular End Portal structure
     * @param center position of the center of the end portal structure
     * @return set of player UUIDs associated with End Portal structure
     */
    public @NotNull Set<UUID> getCommittedPlayers(BlockKey center) {
        return PortalGeometry.getFrameLocations(center)
                .map(this::getEyeOwner)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Remove portal center of end portal structure from being tracked.
     * @param center position of center of the end portal structure to remove
     */
    public void removePortalCenter(BlockKey center) {
        portalCenters.remove(center);
    }

    /**
     * Get mapping of End Portal frame positions (assumed to be filled with eyes)
     * and their owner's player UUID.
     * @return immutable map of end portal frame positions and player UUID
     */
    public @NotNull Map<BlockKey, UUID> getEyeOwners() {
        return Collections.unmodifiableMap(eyeOwners);
    }

    /**
     * Get all tracked End Portal structures (specifically their portal centers).
     * @return immutable set of portal center positions
     */
    public @NotNull Set<BlockKey> getPortalCenters() {
        return Collections.unmodifiableSet(portalCenters);
    }

    /**
     * Update the internal status of whether the Ender Dragon has been defeated.
     * @param status boolean indicating dragon defeat
     */
    public void setDragonDefeatedStatus(boolean status) {
        this.dragonDefeated = status;
    }

    /**
     * Get the internal status of whether the Ender Dragon has been marked as defeated
     * @return whether the dragon has been defeated
     */
    public boolean getDragonDefeatStatus() {
        return dragonDefeated;
    }
}
