package io.github.ganyuke.cea.core.config;

import io.github.ganyuke.cea.core.util.SoundCategory;
import io.github.ganyuke.cea.core.util.SoundSpec;
import org.slf4j.Logger;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

public final class ConfigFactory {
    private static <T extends Number & Comparable<T>> T readNumber(
            Logger logger, Map<?, ?> map, String key, T fallback, T min, T max, Function<Number, T> extractor
    ) {
        Object value = map.get(key);
        if (value == null) return fallback;
        if (!(value instanceof Number num)) {
            logger.warn("Configuration key '{}' must be a number, but got: {}. Using default: {}", key, value, fallback);
            return fallback;
        }
        T parsed = extractor.apply(num);
        if (parsed.compareTo(min) < 0 || parsed.compareTo(max) > 0) {
            T clamped = parsed.compareTo(min) < 0 ? min : max;
            logger.warn("Configuration key '{}' ({}) is out of bounds [{}, {}]. Clamping to: {}", key, parsed, min, max, clamped);
            return clamped;
        }
        return parsed;
    }

    private static SoundCategory readSoundCategory(Logger logger, String path, String input) {
        SoundCategory fallback = SoundCategory.AMBIENT;

        if (input == null) return fallback;

        String cleaned = input.trim().toUpperCase();

        // in Bukkit, RECORD, BLOCK, and PLAYER are singular.
        // in net.minecraft, those are plural. So for the sake
        // of users, we should handle both Bukkit and general
        // Minecraft format.
        SoundCategory category = switch (cleaned) {
            case "MASTER" -> SoundCategory.MASTER;
            case "MUSIC" -> SoundCategory.MUSIC;
            case "RECORD", "RECORDS" -> SoundCategory.RECORDS;
            case "WEATHER" -> SoundCategory.WEATHER;
            case "BLOCK", "BLOCKS" -> SoundCategory.BLOCKS;
            case "HOSTILE" -> SoundCategory.HOSTILE;
            case "NEUTRAL" -> SoundCategory.NEUTRAL;
            case "PLAYER", "PLAYERS" -> SoundCategory.PLAYERS;
            case "AMBIENT" -> SoundCategory.AMBIENT;
            case "VOICE" -> SoundCategory.VOICE;
            default -> null;
        };

        if (category != null) {
            return category;
        }

        logger.warn("Invalid sound source '{}' found at `sounds.{}`. Using default: {}", input, path, fallback.name());
        return fallback;
    }

    private static EnumMap<MessageKey, String> readMessages(Map<?, ?> section) {
        EnumMap<MessageKey, String> messages = new EnumMap<>(MessageKey.class);
        for (MessageKey key : MessageKey.values()) {
            String string = asString(section.get(key.path()));
            if (string == null)
                throw new IllegalStateException("Undefined message for `messages." + key.path() + "` in `config.yml`.");
            messages.put(key, string);
        }
        return messages;
    }

    private static EnumMap<SoundKey, SoundSpec> readSounds(Map<?, ?> section, Logger logger) {
        EnumMap<SoundKey, SoundSpec> sounds = new EnumMap<>(SoundKey.class);

        for (SoundKey key : SoundKey.values()) {
            Map<?, ?> specific = getSection(section, key.path(), "sounds." + key.path());
            String soundKey = asString(specific.get("key"));
            if (soundKey == null)
                throw new IllegalStateException("Undefined sound key for `sounds." + key.path() + "` in `config.yml`.");

            float volume = readNumber(logger, specific, "volume", 1.0f, 0.0f, Float.MAX_VALUE, Number::floatValue);
            float pitch = readNumber(logger, specific, "pitch", 1.0f, 0.5f, 2.0f, Number::floatValue);
            SoundCategory source = readSoundCategory(logger, key.name(), asString(specific.get("source")));

            sounds.put(key, new SoundSpec(soundKey, source, volume, pitch));
        }
        return sounds;
    }

    private static String asString(Object value) {
        return value instanceof String s ? s : null;
    }

    private static Map<?, ?> getSection(Map<?, ?> root, String sectionName) {
        return getSection(root, sectionName, sectionName);
    }

    private static Map<?, ?> getSection(Map<?, ?> root, String sectionName, String sectionDisplayName) {
        if (!(root.get(sectionName) instanceof Map<?, ?> section))
            throw new IllegalStateException("Missing or invalid `" + sectionDisplayName + "` section in `config.yml`.");
        return section;
    }

    public static Config load(Map<String, Object> root, Logger logger) {
        int maxEyes = readNumber(logger, root, "max_eyes_per_player", 1, 0, 12, Number::intValue);
        double activationRadius = readNumber(logger, root, "activation_radius", 8.0, -1.0, Double.MAX_VALUE, Number::doubleValue);
        double actionBarRadius = readNumber(logger, root, "action_bar_radius", 60.0, -1.0, Double.MAX_VALUE, Number::doubleValue);

        EnumMap<MessageKey, String> messages = readMessages(getSection(root, "messages"));
        EnumMap<SoundKey, SoundSpec> sounds = readSounds(getSection(root, "sounds"), logger);

        return new Config(maxEyes, activationRadius, actionBarRadius, messages, sounds);
    }
}
