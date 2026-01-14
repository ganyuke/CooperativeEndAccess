package io.github.ganyuke.cooperativeEndAccess;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumMap;
import java.util.Objects;

public class Config {
    private final EnumMap<MessageKey, Component> messages = new EnumMap<>(MessageKey.class);
    private final double activationRadius;
    private final FileConfiguration config;

    Config(FileConfiguration config) {
        this.config = config;
        this.activationRadius = config.getDouble("activation_radius");
    }

    public double getActivationRadius() {
        return activationRadius;
    }

    public enum MessageKey {
        MAX_EYES_ERROR("max_eyes_error"),
        COMMITTED_NOTICE("committed_notice"),
        RESCIND_WARNING("rescind_warning"),
        DRAGON_DEFEAT_NOTICE("dragon_defeat_notice");

        private final String path; // relative to "messages."

        MessageKey(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }
    }

    public void loadMessages() {
        MiniMessage miniMessage = MiniMessage.miniMessage();
        var messageConfigSection = config.getConfigurationSection("messages");
        if (messageConfigSection == null) {
            throw new IllegalStateException("Missing `messages` section in `config.yml`.");
        }
        messages.clear();

        for (MessageKey key : MessageKey.values()) {
            String path = key.getPath();
            String message = messageConfigSection.getString(path);
            if (message == null) {
                throw new IllegalStateException("Undefined message for `messages.`" + path + " in `config.yml`.");
            }
            messages.put(key, miniMessage.deserialize(message));
        }
    }

    public Component getMessage(MessageKey key) {
        return Objects.requireNonNull(messages.get(key)); // should be not null
    }
}
