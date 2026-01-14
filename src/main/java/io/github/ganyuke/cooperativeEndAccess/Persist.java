package io.github.ganyuke.cooperativeEndAccess;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class Persist {
    private final File dataFile;

    public Persist(File dataFile) {
        this.dataFile = dataFile;
    }

    public State loadData() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        boolean dragonDefeated = config.getBoolean("dragon_defeated", false);
        Map<Location, Map<UUID, Integer>> portalContributors = new HashMap<>();

        var portalsSection = config.getConfigurationSection("portals");
        if (portalsSection != null) {
            for (String key : portalsSection.getKeys(false)) {
                Location loc = Util.stringToLoc(key);
                List<String> uuidStrings = config.getStringList("portals." + key);
                Map<UUID, Integer> map = new HashMap<>();
                for (String s : uuidStrings) {
                    map.merge(UUID.fromString(s), 1, Integer::sum);
                }
                portalContributors.put(loc, map);
            }
        }
        return new State(portalContributors, dragonDefeated);
    }

    public void saveData(State state) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("dragon_defeated", state.getDragonDefeatStatus());
        config.set("portals", null);

        for (var entry : state.getAllPortalContributors().entrySet()) {
            List<String> uuidStrings = new ArrayList<>();
            // Flatten the map (id + count) back into a list of strings
            entry.getValue().forEach((uuid, count) -> {
                for (int i = 0; i < count; i++) uuidStrings.add(uuid.toString());
            });
            config.set("portals." + Util.locToString(entry.getKey()), uuidStrings);
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save `config.yml`", e);
        }
    }
}