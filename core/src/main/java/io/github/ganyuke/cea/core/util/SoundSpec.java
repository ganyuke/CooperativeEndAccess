// SPDX-License-Identifier: MPL-2.0
// SPDX-FileCopyrightText: ganyuke
package io.github.ganyuke.cea.core.util;

import org.intellij.lang.annotations.Subst;

public record SoundSpec(@Subst("minecraft:block.note_block.bass")String key, SoundCategory source, float volume, float pitch) {}
