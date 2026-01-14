package io.github.ganyuke.cooperativeEndAccess;

import org.bukkit.Location;

import java.util.*;

public class State {
    private final Map<Location, Map<UUID, Integer>> portalContributors;
    private boolean dragonDefeated;

    State(Map<Location, Map<UUID, Integer>> portalContributors, boolean dragonDefeated) {
        this.portalContributors = portalContributors;
        this.dragonDefeated = dragonDefeated;
    }

    private Map<UUID, Integer> contributorsFor(Location center) {
        return portalContributors.computeIfAbsent(center, c -> new HashMap<>());
    }

    public void addContributor(Location center, UUID id) {
        var contributors = this.contributorsFor(center);
        contributors.merge(id, 1, Integer::sum);
    }

    public Integer getContributions(Location center, UUID id) {
        return this.contributorsFor(center)
                .getOrDefault(id, 0);
    }

    public boolean removeContributor(Location center, UUID id) {
        Map<UUID, Integer> contributors = portalContributors.get(center);
        if (contributors == null) return false;

        Integer count = contributors.get(id);
        if (count == null) return false;

        if (count <= 1) {
            contributors.remove(id);
            if (contributors.isEmpty()) {
                portalContributors.remove(center);
            }
        } else {
            contributors.put(id, count - 1);
        }

        return true;
    }

    public void setDragonDefeatedStatus(boolean status) {
        this.dragonDefeated = status;
    }

    public boolean getDragonDefeatStatus() {
        return dragonDefeated;
    }

    public Map<Location, Map<UUID, Integer>> getAllPortalContributors() {
        Map<Location, Map<UUID, Integer>> copy = new HashMap<>();

        for (var entry : portalContributors.entrySet()) {
            copy.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }

        return copy;
    }
}
