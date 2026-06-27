// SPDX-License-Identifier: MPL-2.0
// SPDX-FileCopyrightText: ganyuke
package io.github.ganyuke.cea.core.data;

import io.github.ganyuke.cea.core.portal.PortalGeometry;
import io.github.ganyuke.cea.core.util.BlockKey;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public final class Persist {
    private final Path dataFile;
    private final Yaml yaml;
    private final PortalGeometry geometry;

    public Persist(Path dataFile, PortalGeometry geometry) {
        this.dataFile = dataFile;
        this.geometry = geometry;
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(false);
        this.yaml = new Yaml(options);
    }

    public @NotNull State loadData() {
        Map<String, Object> config = loadMap(dataFile);
        boolean dragonDefeated = asBoolean(config.get("dragon_defeated"), false);
        Map<BlockKey, UUID> eyeOwners = new HashMap<>();
        Set<BlockKey> centers = new HashSet<>();
        Map<UUID, String> committedNames = new HashMap<>();

        Map<String, Object> eyesSection = asMap(config.get("eyes"));
        if (eyesSection != null) {
            for (Map.Entry<String, Object> entry : eyesSection.entrySet()) {
                eyeOwners.put(BlockKey.from(entry.getKey()), UUID.fromString(String.valueOf(entry.getValue())));
            }
        }

        Object centersValue = config.get("centers");
        if (centersValue instanceof Iterable<?> iterable) {
            for (Object value : iterable) centers.add(BlockKey.from(String.valueOf(value)));
        }

        Map<String, Object> namesSection = asMap(config.get("names"));
        if (namesSection != null) {
            for (Map.Entry<String, Object> entry : namesSection.entrySet()) {
                committedNames.put(UUID.fromString(entry.getKey()), String.valueOf(entry.getValue()));
            }
        }

        return new State(eyeOwners, centers, committedNames, dragonDefeated, geometry);
    }

    public void saveData(@NotNull State state) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("dragon_defeated", state.getDragonDefeatStatus());

        Map<String, Object> eyes = new LinkedHashMap<>();
        for (Map.Entry<BlockKey, UUID> entry : state.getEyeOwners().entrySet()) {
            eyes.put(entry.getKey().toString(), entry.getValue().toString());
        }
        if (!eyes.isEmpty()) config.put("eyes", eyes);

        List<String> centerStrings = new ArrayList<>();
        for (BlockKey center : state.getPortalCenters()) centerStrings.add(center.toString());
        config.put("centers", centerStrings);

        // persist the cache so no reverse name lookups needed on cold start
        Map<String, Object> names = new LinkedHashMap<>();
        for (Map.Entry<UUID, String> entry : state.getPlayerNameCache().entrySet()) {
            // normally you'd probably want to clear the dead names, but I don't really
            // care enough to clear the dead names.
            names.put(entry.getKey().toString(), entry.getValue());
        }
        if (!names.isEmpty()) config.put("names", names);

        try {
            Files.createDirectories(dataFile.getParent());
            try (Writer writer = Files.newBufferedWriter(dataFile)) {
                yaml.dump(config, writer);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save `data.yml`", e);
        }
    }

    public static @NotNull Map<String, Object> loadMap(Path path) {
        if (!Files.exists(path)) return new LinkedHashMap<>();
        DumperOptions options = new DumperOptions();
        Yaml yaml = new Yaml(options);
        try (Reader reader = Files.newBufferedReader(path)) {
            Object loaded = yaml.load(reader);
            Map<String, Object> map = asMap(loaded);
            return map == null ? new LinkedHashMap<>() : map;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load `" + path.getFileName() + "`", e);
        }
    }

    private static boolean asBoolean(Object value, boolean fallback) { return value instanceof Boolean b ? b : fallback; }
    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) { return value instanceof Map<?, ?> map ? (Map<String, Object>) map : null; }
}
