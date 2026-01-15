package io.github.ganyuke.cooperativeEndAccess.portal;

import io.github.ganyuke.cooperativeEndAccess.util.BlockKey;
import io.github.ganyuke.cooperativeEndAccess.config.Config;
import io.github.ganyuke.cooperativeEndAccess.data.State;
import io.github.ganyuke.cooperativeEndAccess.portal.PortalUtils.PortalResult;

import org.bukkit.*;
import org.bukkit.entity.Player;

import java.util.*;

public class PortalManager {
    private final State state;
    private final Config config;
    private World endWorld;
    private final PortalFeedback portalFeedback;
    private final PortalWorldTechnician portalWorldTechnician;

    public PortalManager(State state, Config config) {
        this.state = state;
        this.config = config;
        this.portalFeedback = new PortalFeedback(config);
        this.portalWorldTechnician = new PortalWorldTechnician(portalFeedback);
    }

    private World getEndWorld() {
        if (endWorld == null) {
            endWorld = Bukkit.getWorlds().stream()
                    .filter(w -> w.getEnvironment() == World.Environment.THE_END)
                    .findFirst().orElse(null);
        }
        return endWorld;
    }

    public void updateTrackedPortals() {
        if (state.getDragonDefeatStatus()) return;

        double activationRadiusSq = Math.pow(config.getActivationRadius(), 2);
        double actionBarRadiusSq = Math.pow(config.getActionBarRadius(), 2);
        World end = getEndWorld();

        for (BlockKey center : state.getPortalCenters()) {
            Set<UUID> committed = state.getCommittedPlayers(center);

            if (!PortalUtils.isEndPortalFilled(center)) {
                // ensure that the portal closes
                portalWorldTechnician.updatePhysicalPortal(center, false);
                portalFeedback.removeAudience(center);
                continue;
            }

            PortalResult result = evaluatePortalStatus(center, committed, end, activationRadiusSq);

            portalWorldTechnician.updatePhysicalPortal(center, result.isActive);
            portalFeedback.handleFeedback(center, committed, result, actionBarRadiusSq);

            portalFeedback.addAudience(center, result.nearbyPlayers);
        }
    }

    private PortalResult evaluatePortalStatus(BlockKey center, Set<UUID> committed, World end, double radiusSq) {
        PortalResult res = new PortalResult();
        World world = center.getWorld();

        Set<UUID> portalContributors = state.getCommittedPlayers(center);

        for (UUID id : portalContributors) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) {
                var playerName = state.getName(id);
                if (playerName == null) playerName = "Offline Player";
                res.missingNames.add(playerName);
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
                    && p.getLocation().distanceSquared(center.toLocation()) <= radiusSq)
            ) {
                res.nearbyPlayers.add(id);
            } else {
                res.missingNames.add(p.getName());
            }
        }

        res.isActive = (res.stabilizerName != null) || (res.nearbyPlayers.size() == committed.size());
        return res;
    }
}