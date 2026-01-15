// SPDX-License-Identifier: MPL-2.0
// SPDX-FileCopyrightText: ganyuke
package io.github.ganyuke.cooperativeEndAccess.portal;

import io.github.ganyuke.cooperativeEndAccess.config.Config;
import io.github.ganyuke.cooperativeEndAccess.util.BlockKey;
import io.github.ganyuke.cooperativeEndAccess.portal.PortalUtils.PortalResult;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

class PortalFeedback {
    private final Map<BlockKey, Set<UUID>> lastKnownNearby = new HashMap<>();
    private final Config config;

    PortalFeedback(Config config) {
        this.config = config;
    }

    protected void addAudience(BlockKey center, Set<UUID> nearbyCommittedPlayers) {
        lastKnownNearby.put(center, nearbyCommittedPlayers);
    }

    protected void removeAudience(BlockKey center) {
        lastKnownNearby.remove(center);
    }

    private void sendStatusMessage(PortalResult result, Player p) {
        Component msg =
                result.stabilizerName != null
                        ? config.getStabilizedActionBar(result.stabilizerName)
                        : result.isActive
                        ? config.getMessage(Config.MessageKey.ACTIVE_ACTION_BAR)
                        : config.getWaitingActionBar(String.join(", ", result.missingNames));

        p.sendActionBar(msg);
    }

    private void spawnParticles(Player p, Location center) {
        Location start = p.getLocation().add(0, 1.2, 0);
        Vector dir = center.toVector().subtract(start.toVector()).normalize().multiply(0.5);
        for(int i=0; i<3; i++) {
            p.getWorld().spawnParticle(Particle.WITCH, start.add(dir), 1, 0, 0, 0, 0);
        }
    }

    private void spawnParticles(World world, int x, int y, int z) {
        world.spawnParticle(
                Particle.DRAGON_BREATH,
                new Location(world, x, y, z),
                50,
                1, 0, 1,
                0.1,
                1.0f
        );
    }

    protected void openPortalFx(BlockKey center) {
        config.play(Config.SoundKey.PORTAL_OPEN, center);
        World world = center.getWorld();
        if (world == null) return;
        spawnParticles(world, center.x(), center.y(), center.z());
    }

    protected void closePortalFx(BlockKey center) {
        config.play(Config.SoundKey.PORTAL_CLOSE, center);
    }

    protected void handleFeedback(BlockKey center, Set<UUID> committed, PortalResult result, double actionBarRadiusSq) {
        Set<UUID> previousNearby = lastKnownNearby.getOrDefault(center, Collections.emptySet());
        World centerWorld = center.getWorld();

        for (UUID id : committed) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || !p.getWorld().equals(centerWorld)) continue;

            double distSq = p.getLocation().distanceSquared(center.toLocation());

            // control enter/exit sound and particles
            if (result.nearbyPlayers.contains(id)) {
                spawnParticles(p, center.toLocation());
                if (!previousNearby.contains(id)) {
                    config.play(Config.SoundKey.PLAYER_ENTER, center);
                }
            } else if (previousNearby.contains(id)) {
                config.play(Config.SoundKey.PLAYER_LEAVE, center);
            }

            // send nearby committed players the status message
            if (config.isUnlimitedActionBar() || distSq <= actionBarRadiusSq) {
                sendStatusMessage(result, p);
            }
        }
    }
}
