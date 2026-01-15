package io.github.ganyuke.cooperativeEndAccess.data;

import io.github.ganyuke.cooperativeEndAccess.util.BlockKey;

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

        Map<BlockKey, UUID> eyeOwners = new HashMap<>();
        Set<BlockKey> centers = new HashSet<>();
        Map<UUID, String> committedNames = new HashMap<>();

        ConfigurationSection eyesSection = config.getConfigurationSection("eyes");
        if (eyesSection != null) {
            for (String key : eyesSection.getKeys(false)) {
                var uuid = UUID.fromString(Objects.requireNonNull(eyesSection.getString(key)));
                eyeOwners.put(BlockKey.from(key), uuid);
            }
        }

        List<String> centerStrings = config.getStringList("centers");
        for (String s : centerStrings) centers.add(BlockKey.from(s));

        ConfigurationSection committedNamesSection = config.getConfigurationSection("names");
        if (committedNamesSection != null) {
            for (String key : committedNamesSection.getKeys(false)) {
                var uuid = UUID.fromString(key);
                var name = Objects.requireNonNull(committedNamesSection.getString(key));
                committedNames.put(uuid, name);
            }
        }

        return new State(eyeOwners, centers, committedNames, dragonDefeated);
    }

    public void saveData(State state) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("dragon_defeated", state.getDragonDefeatStatus());
        config.set("eyes", null);
        config.set("centers", null);
        config.set("names", null);

        for (Map.Entry<BlockKey, UUID> entry : state.getEyeOwners().entrySet()) {
            config.set("eyes." + entry.getKey().toString(), entry.getValue().toString());
        }

        List<String> centerStrings = state.getPortalCenters().stream()
                .map(BlockKey::toString).toList();
        config.set("centers", centerStrings);

        for (Map.Entry<UUID, String> entry : state.getCommittedNames().entrySet()) {
            config.set("names." + entry.getKey().toString(), entry.getValue());
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save `config.yml`", e);
        }
    }
}