package io.github.ganyuke.cooperativeEndAccess;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

public class PortalManager {
    private final State state;
    private final Config config;
    private final Map<Location, Set<UUID>> lastKnownNearby = new HashMap<>();
    private World endWorld;
    private final Map<Location, Boolean> portalPhysicalState = new HashMap<>();

    public PortalManager(State state, Config config) {
        this.state = state;
        this.config = config;
    }

    public void update() {
        if (state.getDragonDefeatStatus()) return;

        double activationRadiusSq = Math.pow(config.getActivationRadius(), 2);
        double actionBarRadiusSq = Math.pow(config.getActionBarRadius(), 2);
        World end = getEndWorld();

        for (var entry : state.getAllPortalContributors().entrySet()) {
            Location center = entry.getKey();
            Set<UUID> committed = entry.getValue().keySet();

            if (!Util.isEndPortalFilled(center)) {
                // ensure that the portal closes
                updatePhysicalPortal(center, false);
                lastKnownNearby.remove(center);
                continue;
            }

            PortalResult result = evaluate(center, committed, end, activationRadiusSq);

            updatePhysicalPortal(center, result.isActive);
            handleFeedback(center, committed, result, actionBarRadiusSq);

            lastKnownNearby.put(center, result.nearbyPlayers);
        }
    }

    private void updatePhysicalPortal(Location center, boolean shouldBeOpen) {
        if (!portalPhysicalState.containsKey(center)) {
            // handle when server restarts while an unstable End Portal is open
            boolean isActuallyOpenInWorld = center.getBlock().getType() == Material.END_PORTAL;
            portalPhysicalState.put(center, isActuallyOpenInWorld);
        }

        boolean wasOpen = portalPhysicalState.getOrDefault(center, false);

        // when state changes, trigger creating/destroying portal and playing associated SFX
        if (shouldBeOpen != wasOpen) {
            if (shouldBeOpen) {
                Util.fillPortal(center, Material.END_PORTAL);
                config.play(Config.SoundKey.PORTAL_OPEN, center);
            } else {
                Util.fillPortal(center, Material.AIR);
                config.play(Config.SoundKey.PORTAL_CLOSE, center);
            }
            portalPhysicalState.put(center, shouldBeOpen);
        }
    }

    private void handleFeedback(Location center, Set<UUID> committed, PortalResult result, double actionBarRadiusSq) {
        Set<UUID> previousNearby = lastKnownNearby.getOrDefault(center, Collections.emptySet());
        World centerWorld = center.getWorld();

        for (UUID id : committed) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || !p.getWorld().equals(centerWorld)) continue;

            double distSq = p.getLocation().distanceSquared(center);

            // control enter/exit sound and particles
            if (result.nearbyPlayers.contains(id)) {
                spawnParticles(p, center);
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

    private PortalResult evaluate(Location center, Set<UUID> committed, World end, double radiusSq) {
        PortalResult res = new PortalResult();
        World world = center.getWorld();

        for (UUID id : committed) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) {
                res.missingNames.add("Offline Player");
                continue;
            }

            // check if player is present in the End to stabilize the portal
            if (end != null && p.getWorld().equals(end) && !p.isDead()) {
                res.stabilizerName = p.getName();
            }

            // check player's proximity to portal
            // I presume when someone wants unlimited activation, they just
            // care about the player being online somewhere in the server,
            // so we'll disregard their current world
            if (config.isUnlimitedActivation()
                    || (p.getWorld().equals(world)
                    && p.getLocation().distanceSquared(center) <= radiusSq)
            ) {
                res.nearbyPlayers.add(id);
            } else {
                res.missingNames.add(p.getName());
            }
        }

        res.isActive = (res.stabilizerName != null) || (res.nearbyPlayers.size() == committed.size());
        return res;
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

    private World getEndWorld() {
        if (endWorld == null) {
            endWorld = Bukkit.getWorlds().stream()
                    .filter(w -> w.getEnvironment() == World.Environment.THE_END)
                    .findFirst().orElse(null);
        }
        return endWorld;
    }

    private static class PortalResult {
        boolean isActive;
        String stabilizerName;
        final Set<UUID> nearbyPlayers = new HashSet<>();
        final List<String> missingNames = new ArrayList<>();
    }
}