package io.github.ganyuke.cooperativeEndAccess;

import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.Objects;

public class Config {
    private final EnumMap<MessageKey, Component> messages = new EnumMap<>(MessageKey.class);
    private final EnumMap<SoundKey, Sound> sounds = new EnumMap<>(SoundKey.class);

    private final double activationRadius;
    private final double actionBarRadius;

    private final String stabilizedActionBarTemplate;
    private final String waitingActionBarTemplate;
    private final String nonOwnerRescindWarningTemplate;

    private final int maxEyesPerPlayer;
    private final FileConfiguration config;
    private final MiniMessage miniMessage;

    Config(FileConfiguration config) {
        this.config = config;
        this.activationRadius = config.getDouble("activation_radius", 8.0);
        this.maxEyesPerPlayer = config.getInt("max_eyes_per_player", 1);
        this.actionBarRadius = config.getDouble("action_bar_radius", 60.0);
        this.stabilizedActionBarTemplate = config.getString("messages.stabilized_action_bar");
        this.waitingActionBarTemplate = config.getString("messages.waiting_action_bar");
        this.nonOwnerRescindWarningTemplate = config.getString("messages.non_owner_rescind_warning");
        this.miniMessage = MiniMessage.miniMessage();
    }

    public double getActivationRadius() {
        return activationRadius;
    }
    public double getActionBarRadius() { return actionBarRadius; }
    public int getMaxEyesPerPlayer() {
        return maxEyesPerPlayer;
    }

    // support -1 in config for "anywhere"
    public boolean isUnlimitedActivation() {
        return getActivationRadius() < 0;
    }

    public boolean isUnlimitedActionBar() {
        return getActionBarRadius() < 0;
    }

    public enum MessageKey {
        MAX_EYES_ERROR("max_eyes_error"),
        COMMITTED_NOTICE("committed_notice"),
        RESCIND_WARNING("rescind_warning"),
        DRAGON_DEFEAT_NOTICE("dragon_defeat_notice"),
        ACTIVE_ACTION_BAR("active_action_bar");

        private final String path; // relative to "messages."

        MessageKey(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }
    }

    public void loadMessages() {
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

    public Component getStabilizedActionBar(String name) {
        return miniMessage.deserialize(this.stabilizedActionBarTemplate, Placeholder.unparsed("name", name));
    }

    public Component getWaitingActionBar(String names) {
        return miniMessage.deserialize(this.waitingActionBarTemplate, Placeholder.unparsed("names", names));
    }

    public Component getOwnerRescindWarning(String name) {
        return miniMessage.deserialize(this.nonOwnerRescindWarningTemplate, Placeholder.unparsed("name", name));
    }

    public Component getMessage(MessageKey key) {
        return Objects.requireNonNull(messages.get(key)); // should be not null
    }

    public enum SoundKey {
        PORTAL_OPEN("portal_open"),
        PORTAL_CLOSE("portal_close"),
        PLAYER_ENTER("player_enter"),
        PLAYER_LEAVE("player_leave");

        private final String path; // relative to "sounds."

        SoundKey(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }
    }

    // IDK why IDEA complains about "Unsubstituted expression"
    // when using Kyori Adventure's key
    @SuppressWarnings("PatternValidation")
    public void loadSounds() {
        var soundConfigSection = config.getConfigurationSection("sounds");
        if (soundConfigSection == null) {
            throw new IllegalStateException("Missing `sounds` section in `config.yml`.");
        }
        sounds.clear();

        for (SoundKey soundKey : SoundKey.values()) {
            String path = soundKey.getPath();
            ConfigurationSection specificSoundSection = soundConfigSection.getConfigurationSection(path);
            if (specificSoundSection == null) {
                throw new IllegalStateException("Undefined section for `sounds.`" + path + " in `config.yml`.");
            }
            String specificSoundKey = specificSoundSection.getString("key");
            if (specificSoundKey == null) {
                throw new IllegalStateException("Undefined sound key provided for `sounds.`" + path + " in `config.yml`.");
            }
            Key key;
            try {
                key = Key.key(specificSoundKey);
            } catch (InvalidKeyException e) {
                throw new IllegalStateException("Invalid sound key provided for `sounds.`" + path + " in `config.yml`.");
            }
            String sourceName = specificSoundSection.getString("source", "block");
            Sound.Source source = Sound.Source.NAMES.value(sourceName.toLowerCase());

            if (source == null) {
                throw new IllegalStateException("Invalid source key provided for `sounds.`" + path + " in `config.yml`.");
            }

            float volume = (float) specificSoundSection.getDouble("volume", 1.0);
            float pitch = (float) specificSoundSection.getDouble("pitch", 1.0);

            Sound sound = Sound.sound(key, source, volume, pitch);
            sounds.put(soundKey, sound);
        }
    }

    public void play(SoundKey soundKey, Location loc) {
        Sound sound = Objects.requireNonNull(sounds.get(soundKey)); // should be not null
        loc.getWorld().playSound(sound, loc.getX(), loc.getY(), loc.getZ());
    }

    public void play(SoundKey soundKey, Player player) {
        Sound sound = Objects.requireNonNull(sounds.get(soundKey)); // should be not null
        player.playSound(sound, Sound.Emitter.self());
    }
}
