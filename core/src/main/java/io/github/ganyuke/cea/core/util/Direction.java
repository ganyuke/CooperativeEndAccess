// SPDX-License-Identifier: MPL-2.0
// SPDX-FileCopyrightText: ganyuke
package io.github.ganyuke.cea.core.util;

public enum Direction {
    NORTH(0, -1), SOUTH(0, 1), WEST(-1, 0), EAST(1, 0);
    private final int modX;
    private final int modZ;
    Direction(int modX, int modZ) { this.modX = modX; this.modZ = modZ; }
    public int modX() { return modX; }
    public int modZ() { return modZ; }
}
