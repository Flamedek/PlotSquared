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
package com.plotsquared.core.plot;

import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.location.Direction;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.queue.QueueCoordinator;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.world.biome.BiomeType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PlotModificationManager {

    /**
     * Copy a plot to a location, both physically and the settings
     *
     * @param destination destination plot
     * @param actor       the actor associated with the copy
     * @return Future that completes with {@code true} if the copy was successful, else {@code false}
     */
    CompletableFuture<Boolean> copy(@NonNull Plot destination, @Nullable PlotPlayer<?> actor);

    /**
     * Clear the plot
     *
     * <p>
     * Use {@link #deletePlot(PlotPlayer, Runnable)} to clear and delete a plot
     * </p>
     *
     * @param whenDone A runnable to execute when clearing finishes, or null
     * @see #clear(boolean, boolean, PlotPlayer, Runnable)
     */
    void clear(@Nullable Runnable whenDone);

    /**
     * Clear the plot
     *
     * <p>
     * Use {@link #deletePlot(PlotPlayer, Runnable)} to clear and delete a plot
     * </p>
     *
     * @param checkRunning Whether or not already executing tasks should be checked
     * @param isDelete     Whether or not the plot is being deleted
     * @param actor        The actor clearing the plot
     * @param whenDone     A runnable to execute when clearing finishes, or null
     */
    boolean clear(
            boolean checkRunning,
            boolean isDelete,
            @Nullable PlotPlayer<?> actor,
            @Nullable Runnable whenDone
    );

    /**
     * Sets the biome for a plot asynchronously.
     *
     * @param biome    The biome e.g. "forest"
     * @param whenDone The task to run when finished, or null
     */
    void setBiome(@Nullable BiomeType biome, @NonNull Runnable whenDone);

    /**
     * Unlink the plot and all connected plots.
     *
     * @param createRoad whether to recreate road
     * @param createSign whether to recreate signs
     * @return success/!cancelled
     */
    boolean unlinkPlot(boolean createRoad, boolean createSign);

    /**
     * Unlink the plot and all connected plots.
     *
     * @param createRoad whether to recreate road
     * @param createSign whether to recreate signs
     * @param whenDone   Task to run when unlink is complete
     * @return success/!cancelled
     * @since 6.10.9
     */
    boolean unlinkPlot(boolean createRoad, boolean createSign, Runnable whenDone);

    /**
     * Sets the sign for a plot to a specific name
     *
     * @param name name
     */
    void setSign(@NonNull String name);

    /**
     * Resend all chunks inside the plot to nearby players<br>
     * This should not need to be called
     */
    void refreshChunks();

    /**
     * Remove the plot sign if it is set.
     */
    void removeSign();

    /**
     * Sets the plot sign if plot signs are enabled.
     */
    void setSign();

    /**
     * Register a plot and create it in the database<br>
     * - The plot will not be created if the owner is null<br>
     * - Any setting from before plot creation will not be saved until the server is stopped properly. i.e. Set any values/options after plot
     * creation.
     *
     * @return {@code true} if plot was created successfully
     */
    boolean create();

    /**
     * Register a plot and create it in the database<br>
     * - The plot will not be created if the owner is null<br>
     * - Any setting from before plot creation will not be saved until the server is stopped properly. i.e. Set any values/options after plot
     * creation.
     *
     * @param uuid   the uuid of the plot owner
     * @param notify notify
     * @return {@code true} if plot was created successfully, else {@code false}
     */
    boolean create(@NonNull UUID uuid, boolean notify);

    /**
     * Auto merge a plot in a specific direction.
     *
     * @param dir         the direction to merge
     * @param max         the max number of merges to do
     * @param uuid        the UUID it is allowed to merge with
     * @param actor       The actor executing the task
     * @param removeRoads whether to remove roads
     * @return {@code true} if a merge takes place, else {@code false}
     */
    boolean autoMerge(
            @NonNull Direction dir,
            int max,
            @NonNull UUID uuid,
            @Nullable PlotPlayer<?> actor,
            boolean removeRoads
    );

    /**
     * Moves a plot physically, as well as the corresponding settings.
     *
     * @param destination Plot moved to
     * @param actor       The actor executing the task
     * @param whenDone    task when done
     * @param allowSwap   whether to swap plots
     * @return {@code true} if the move was successful, else {@code false}
     */
    @NonNull
    CompletableFuture<Boolean> move(
            @NonNull Plot destination,
            @Nullable PlotPlayer<?> actor,
            @NonNull Runnable whenDone,
            boolean allowSwap
    );

    /**
     * Unlink a plot and remove the roads
     *
     * @return {@code true} if plot was linked
     * @see #unlinkPlot(boolean, boolean)
     */
    boolean unlink();

    /**
     * Swap the plot contents and settings with another location<br>
     * - The destination must correspond to a valid plot of equal dimensions
     *
     * @param destination The other plot to swap with
     * @param actor       The actor executing the task
     * @param whenDone    A task to run when finished, or null
     * @return Future that completes with {@code true} if the swap was successful, else {@code false}
     */
    @NonNull
    CompletableFuture<Boolean> swap(
            @NonNull Plot destination,
            @Nullable PlotPlayer<?> actor,
            @NonNull Runnable whenDone
    );

    /**
     * Moves the plot to an empty location<br>
     * - The location must be empty
     *
     * @param destination Where to move the plot
     * @param actor       The actor executing the task
     * @param whenDone    A task to run when done, or null
     * @return Future that completes with {@code true} if the move was successful, else {@code false}
     */
    @NonNull
    CompletableFuture<Boolean> move(
            @NonNull Plot destination,
            @Nullable PlotPlayer<?> actor,
            @NonNull Runnable whenDone
    );

    /**
     * Sets a component for a plot to the provided blocks<br>
     * - E.g. floor, wall, border etc.<br>
     * - The available components depend on the generator being used<br>
     *
     * @param component Component to set
     * @param blocks    Pattern to use the generation
     * @param actor     The actor executing the task
     * @param queue     Nullable {@link QueueCoordinator}. If null, creates own queue and enqueues,
     *                  otherwise writes to the queue but does not enqueue.
     * @return {@code true} if the component was set successfully, else {@code false}
     */
    boolean setComponent(
            @NonNull String component,
            @NonNull Pattern blocks,
            @Nullable PlotPlayer<?> actor,
            @Nullable QueueCoordinator queue
    );

    /**
     * Delete a plot (use null for the runnable if you don't need to be notified on completion)
     *
     * <p>
     * Use {@link PlotModificationManager#clear(boolean, boolean, PlotPlayer, Runnable)} to simply clear a plot
     * </p>
     *
     * @param actor    The actor executing the task
     * @param whenDone task to run when plot has been deleted. Nullable
     * @return {@code true} if the deletion was successful, {@code false} if not
     * @see PlotSquared#removePlot(Plot, boolean)
     */
    boolean deletePlot(@Nullable PlotPlayer<?> actor, Runnable whenDone);

    /**
     * Sets components such as border, wall, floor.
     * (components are generator specific)
     *
     * @param component component to set
     * @param blocks    string of block(s) to set component to
     * @param actor     The player executing the task
     * @param queue     Nullable {@link QueueCoordinator}. If null, creates own queue and enqueues,
     *                  otherwise writes to the queue but does not enqueue.
     * @return {@code true} if the update was successful, {@code false} if not
     */
    @Deprecated
    boolean setComponent(
            String component,
            String blocks,
            @Nullable PlotPlayer<?> actor,
            @Nullable QueueCoordinator queue
    );

    /**
     * Remove the south road section of a plot<br>
     * - Used when a plot is merged<br>
     *
     * @param queue Nullable {@link QueueCoordinator}. If null, creates own queue and enqueues,
     *              otherwise writes to the queue but does not enqueue.
     */
    void removeRoadSouth(@Nullable QueueCoordinator queue);

    /**
     * Remove the east road section of a plot<br>
     * - Used when a plot is merged<br>
     *
     * @param queue Nullable {@link QueueCoordinator}. If null, creates own queue and enqueues,
     *              otherwise writes to the queue but does not enqueue.
     */
    void removeRoadEast(@Nullable QueueCoordinator queue);

    /**
     * Remove the SE road (only effects terrain)
     *
     * @param queue Nullable {@link QueueCoordinator}. If null, creates own queue and enqueues,
     *              otherwise writes to the queue but does not enqueue.
     */
    void removeRoadSouthEast(@Nullable QueueCoordinator queue);

}
