package io.github.ganyuke.cooperativeEndAccess.data;

import io.github.ganyuke.cooperativeEndAccess.util.Util;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
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

        Map<Location, UUID> eyeOwners = new HashMap<>();
        Set<Location> centers = new HashSet<>();

        ConfigurationSection eyesSection = config.getConfigurationSection("eyes");
        if (eyesSection != null) {
            for (String key : eyesSection.getKeys(false)) {
                eyeOwners.put(Util.stringToLoc(key), UUID.fromString(Objects.requireNonNull(eyesSection.getString(key))));
            }
        }

        List<String> centerStrings = config.getStringList("centers");
        for (String s : centerStrings) centers.add(Util.stringToLoc(s));

        return new State(eyeOwners, centers, dragonDefeated);
    }

    public void saveData(State state) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("dragon_defeated", state.getDragonDefeatStatus());
        config.set("portals", null);

        for (Map.Entry<Location, UUID> entry : state.getEyeOwners().entrySet()) {
            config.set("eyes." + Util.locToString(entry.getKey()), entry.getValue().toString());
        }

        List<String> centerStrings = state.getPortalCenters().stream()
                .map(Util::locToString).toList();
        config.set("centers", centerStrings);

        try {
            config.save(dataFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save `config.yml`", e);
        }
    }
}