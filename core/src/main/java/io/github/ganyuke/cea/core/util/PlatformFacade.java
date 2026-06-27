// SPDX-License-Identifier: MPL-2.0
// SPDX-FileCopyrightText: ganyuke
package io.github.ganyuke.cea.core.util;

import java.util.Optional;
import java.util.UUID;

public interface PlatformFacade {
    Optional<PlayerSnapshot> player(UUID uuid);
    String offlineName(UUID uuid);
    String endWorldName();
    BlockType blockType(BlockKey loc);
    Direction endFrameFacing(BlockKey loc);
    boolean endFrameHasEye(BlockKey loc);
    void setEndFrameEye(BlockKey loc, boolean hasEye);
    void setBlock(BlockKey loc, BlockType type);
    void sendMessage(UUID playerId, String miniMessage);
    void sendActionBar(UUID playerId, String miniMessage);
    void broadcast(String miniMessage);
    void playSoundAt(SoundSpec sound, BlockKey loc);
    void playSoundTo(UUID playerId, SoundSpec sound);
    void playPortalFrameFill(UUID playerId, BlockKey loc);
    void spawnPortalOpenParticles(BlockKey center);
    void spawnGuidanceParticles(UUID playerId, BlockKey center);
    void giveEyeOrDrop(UUID playerId);
    void consumeInteractionItem(UUID playerId, HandKind hand);

    record PlayerSnapshot(UUID uuid, String name, String worldName, double x, double y, double z, boolean dead) {
        public double distanceSquared(BlockKey loc) {
            double dx = x - loc.x();
            double dy = y - loc.y();
            double dz = z - loc.z();
            return dx * dx + dy * dy + dz * dz;
        }
    }
}
