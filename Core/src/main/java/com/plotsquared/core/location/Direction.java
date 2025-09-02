/*
 * PlotSquared, a land and world management plugin for Minecraft.
 * Copyright (C) IntellectualSites <https://intellectualsites.com>
 * Copyright (C) IntellectualSites team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.plotsquared.core.location;

import com.sk89q.worldedit.math.Vector3;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;

public enum Direction {
    ALL(-1, "all", "a"),
    NORTH(0, "north", "n"),
    EAST(1, "east", "e"),
    SOUTH(2, "south", "s"),
    WEST(3, "west", "w"),
    NORTHEAST(4, "northeast", "ne"),
    SOUTHEAST(5, "southeast", "se"),
    SOUTHWEST(6, "southwest", "sw"),
    NORTHWEST(7, "northwest", "nw"),
    ;

    private final int index;
    private final String name;
    private final String alias;

    Direction(int index, String name, String alias) {
        this.index = index;
        this.name = name;
        this.alias = alias;
    }

    public static Direction getFromIndex(int index) {
        for (Direction value : values()) {
            if (value.getIndex() == index) {
                return value;
            }
        }
        return NORTH;
    }

    @Nullable
    public static Direction parseString(String input) {
        String test = input.toLowerCase(Locale.ROOT).replaceAll("\\W", "");
        for (Direction value : values()) {
            if (value.getName().equals(test) || value.getAlias().equals(test)) {
                return value;
            }
        }
        return null;
    }

    /**
     * {@return the opposite direction}
     * If this is {@link Direction#ALL}, then {@link Direction#ALL} is returned.
     * @since 7.2.0
     */
    public Direction opposite() {
        return switch (this) {
            case ALL -> ALL;
            case NORTH -> SOUTH;
            case EAST -> WEST;
            case SOUTH -> NORTH;
            case WEST -> EAST;
            case NORTHEAST -> SOUTHWEST;
            case SOUTHEAST -> NORTHWEST;
            case SOUTHWEST -> NORTHEAST;
            case NORTHWEST -> SOUTHEAST;
        };
    }

    public Vector3 toVector() {
        return com.sk89q.worldedit.util.Direction.valueOf(name()).toVector();
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public String getAlias() {
        return alias;
    }
}
