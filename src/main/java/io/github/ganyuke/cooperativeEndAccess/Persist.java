package io.github.ganyuke.cooperativeEndAccess;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Persist {
    File dataFile;
    YamlConfiguration dataConfig;

    Persist(File dataFile) {
        this.dataFile = dataFile;
    }

    public State loadData() {
        var dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        var dragonDefeated = dataConfig.getBoolean("dragon_defeated", false);
        var portalsConfig = dataConfig.getConfigurationSection("portals");
        Map<Location, Map<UUID, Integer>> portalContributors = new HashMap<>();

        if (portalsConfig != null) {
            for (String key : portalsConfig.getKeys(false)) {
                Location loc = Util.stringToLoc(key);
                List<String> uuids = dataConfig.getStringList("portals." + key);
                Map<UUID, Integer> map = new HashMap<>();
                for (String s : uuids) {
                    UUID id = UUID.fromString(s);
                    map.merge(id, 1, Integer::sum);
                }
                portalContributors.put(loc, map);
            }
        }
        this.dataConfig = dataConfig;

        return new State(portalContributors, dragonDefeated);
    }

    public void saveData(State state) {
        dataConfig.set("dragon_defeated", state.getDragonDefeatStatus());
        dataConfig.set("portals", null); // clear old portals section before rewriting
        for (Map.Entry<Location, Map<UUID, Integer>> entry : state.getAllPortalContributors().entrySet()) {
            List<String> strings = new ArrayList<>();

            for (var contributor : entry.getValue().entrySet()) {
                for (int i = 0; i < contributor.getValue(); i++) {
                    strings.add(contributor.getKey().toString());
                }
            }

            dataConfig.set("portals." + Util.locToString(entry.getKey()), strings);
        }
        try { dataConfig.save(dataFile); } catch (IOException e) {
            throw new RuntimeException("Failed to write data to " + dataFile.getName(), e);
        }
    }
}
