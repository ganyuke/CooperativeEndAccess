// SPDX-License-Identifier: MPL-2.0
// SPDX-FileCopyrightText: ganyuke
package io.github.ganyuke.cea.core.config;

import io.github.ganyuke.cea.core.util.SoundSpec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.EnumMap;

public record Config(
        int maxEyesPerPlayer,
        double activationRadius,
        double actionBarRadius,
        @Unmodifiable EnumMap<MessageKey, String> messages,
        @Unmodifiable EnumMap<SoundKey, SoundSpec> sounds
) {
    public boolean isUnlimitedActivation() {
        return activationRadius < 0;
    }

    public boolean isUnlimitedActionBar() {
        return actionBarRadius < 0;
    }

    public String getMessage(MessageKey key) {
        return messages.get(key);
    }

    public SoundSpec getSound(SoundKey key) {
        return sounds.get(key);
    }

    public @NotNull String getStabilizedActionBar(String name) {
        return getMessage(MessageKey.STABILIZED_ACTION_BAR).replace("<name>", name);
    }

    public @NotNull String getWaitingActionBar(String names) {
        return getMessage(MessageKey.WAITING_ACTION_BAR).replace("<names>", names);
    }

    public @NotNull String getNonOwnerRescindWarning(String name) {
        return getMessage(MessageKey.NON_OWNER_RESCIND_WARNING).replace("<name>", name);
    }


}