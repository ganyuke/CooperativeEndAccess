package io.github.ganyuke.cooperativeEndAccess;

import org.bukkit.Location;
import java.util.*;

public class State {
    private final Map<Location, Map<UUID, Integer>> portalContributors;
    private boolean dragonDefeated;

    public State(Map<Location, Map<UUID, Integer>> portalContributors, boolean dragonDefeated) {
        this.portalContributors = portalContributors;
        this.dragonDefeated = dragonDefeated;
    }

    public Map<Location, Map<UUID, Integer>> getAllPortalContributors() {
        return portalContributors; // let's just hope I don't write code that modifies this
    }

    public int getContributions(Location center, UUID id) {
        return portalContributors
                .getOrDefault(center, Map.of())
                .getOrDefault(id, 0);
    }

    public void addContributor(Location center, UUID id) {
        portalContributors.computeIfAbsent(center, k -> new HashMap<>())
                .merge(id, 1, Integer::sum);
    }

    public boolean removeContributor(Location center, UUID id) {
        Map<UUID, Integer> contributors = portalContributors.get(center);
        if (contributors == null) return false;
        Integer count = contributors.get(id);
        if (count == null) return false;

        if (count <= 1) {
            contributors.remove(id);
            if (contributors.isEmpty()) portalContributors.remove(center);
        } else {
            contributors.put(id, count - 1);
        }
        return true;
    }

    public void setDragonDefeatedStatus(boolean status) { this.dragonDefeated = status; }
    public boolean getDragonDefeatStatus() { return dragonDefeated; }
}