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
    private final boolean below1_21_9;

    public boolean isBelow1_21_9() {
        return below1_21_9;
    }

    private boolean checkIsBelow1_21_9() {
        String v = Bukkit.getMinecraftVersion();
        String[] parts = v.split("\\.");
        // I'm sure Minecraft 2.0 is not coming any time soon
        int minor = Integer.parseInt(parts[1]);
        int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

        return minor == 21 && patch < 9;
    }

    PortalFeedback(Config config) {
        this.config = config;
        below1_21_9 = checkIsBelow1_21_9();
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
        // beginning in 1.21.9, you need to supply a float to the `data` argument
        // lest you incur the wrath of the NullPointerException
        if (isBelow1_21_9()) {
            world.spawnParticle(
                    Particle.DRAGON_BREATH,
                    new Location(world, x, y, z),
                    50,
                    1, 0, 1,
                    0.1,
                    null
            );
        } else {
            world.spawnParticle(
                    Particle.DRAGON_BREATH,
                    new Location(world, x, y, z),
                    50,
                    1, 0, 1,
                    0.1,
                    1.0f
            );
        }
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
