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
package com.plotsquared.core.listener;

import com.google.common.collect.Lists;
import com.plotsquared.core.util.WEManager;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Mask2D;
import com.sk89q.worldedit.function.mask.RegionMask;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionIntersection;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.Set;

public class WEExtent extends AbstractDelegateExtent {

    public static BlockState AIRSTATE = BlockTypes.AIR.getDefaultState();
    public static BaseBlock AIRBASE = BlockTypes.AIR.getDefaultState().toBaseBlock();
    private final Mask mask;
    private final Mask2D mask2D;

    public WEExtent(Set<Region> mask, Extent extent) {
        this(new RegionMask(new RegionIntersection(Lists.newArrayList(mask))), extent);
    }

    public WEExtent(Mask mask, Extent extent) {
        super(extent);
        this.mask = mask;
        this.mask2D = mask.toMask2D();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean setBlock(BlockVector3 location, BlockStateHolder block)
            throws WorldEditException {
        return WEManager.maskContains(this.mask, location.getX(), location.getY(), location.getZ())
                && super.setBlock(location, block);
    }

    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        if (WEManager.maskContains(this.mask, location.getBlockX(), location.getBlockY(),
                location.getBlockZ()
        )) {
            return super.createEntity(location, entity);
        }
        return null;
    }

    @Override
    public boolean setBiome(BlockVector2 position, BiomeType biome) {
        return WEManager.maskContains(this.mask2D, position.getX(), position.getZ()) && super
                .setBiome(position, biome);
    }

    @Override
    public BlockState getBlock(BlockVector3 location) {
        if (WEManager.maskContains(this.mask, location.getX(), location.getY(), location.getZ())) {
            return super.getBlock(location);
        }
        return AIRSTATE;
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 location) {
        if (WEManager.maskContains(this.mask, location.getX(), location.getY(), location.getZ())) {
            return super.getFullBlock(location);
        }
        return AIRBASE;
    }

}
