// SPDX-License-Identifier: MPL-2.0
// SPDX-FileCopyrightText: ganyuke
package io.github.ganyuke.cea.core.util;

public record BlockKey(String worldName, int x, int y, int z) {
    public static BlockKey from(String s) {
        String[] p = s.split(",");
        return new BlockKey(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
    }

    @Override
    public String toString() {
        return this.worldName + "," + this.x + "," + this.y + "," + this.z;
    }
}
