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
package com.plotsquared.core.command;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.configuration.Settings;
import com.plotsquared.core.configuration.caption.Caption;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.events.PlotMergeEvent;
import com.plotsquared.core.events.Result;
import com.plotsquared.core.location.Direction;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.permissions.Permission;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.util.EconHandler;
import com.plotsquared.core.util.EventDispatcher;
import com.plotsquared.core.util.PlotExpression;
import com.sk89q.worldedit.math.Vector3;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.UUID;

@CommandDeclaration(command = "merge",
        aliases = "m",
        permission = "plots.merge",
        usage = "/plot merge <forward | auto | <direction>> [removeroads]",
        category = CommandCategory.SETTINGS,
        requiredType = RequiredType.NONE,
        confirmation = true)
public class Merge extends SubCommand {

    private static final Logger log = LogManager.getLogger(Merge.class);
    private final EventDispatcher eventDispatcher;
    private final EconHandler econHandler;

    @Inject
    public Merge(
            final @NonNull EventDispatcher eventDispatcher,
            final @NonNull EconHandler econHandler
    ) {
        this.eventDispatcher = eventDispatcher;
        this.econHandler = econHandler;
    }

    @Deprecated(forRemoval = true)
    public static String direction(float yaw) {
        yaw = yaw / 90;
        int i = Math.round(yaw);
        return switch (i) {
            case -4, 0, 4 -> "SOUTH";
            case -1, 3 -> "EAST";
            case -2, 2 -> "NORTH";
            case -3, 1 -> "WEST";
            default -> "";
        };
    }

    protected Direction findForward(List<Direction> directions, Location location) {
        Vector3 dir = location.getDirection();
        Direction closest = null;
        double closestDot = -2;
        for (Direction direction : directions) {
            double dot = direction.toVector().dot(dir);
            if (dot >= closestDot) {
                closest = direction;
                closestDot = dot;
            }
        }
        return closest;
    }

    private static final String ACTION_MERGE_FORWARD = "forward";
    private static final String ACTION_MERGE_AUTO = "auto";

    @Override
    public boolean onCommand(final PlotPlayer<?> player, String[] args) {
        Location location = player.getLocationFull();
        final Plot plot = location.getPlotAbs();
        if (plot == null) {
            player.sendMessage(TranslatableCaption.of("errors.not_in_plot"));
            return false;
        }
        if (!plot.hasOwner()) {
            player.sendMessage(TranslatableCaption.of("info.plot_unowned"));
            return false;
        }
        if (plot.getVolume() > Integer.MAX_VALUE) {
            player.sendMessage(TranslatableCaption.of("schematics.schematic_too_large"));
            return false;
        }
        List<Direction> options = Lists.newArrayList();
        if (player.hasPermission(Permission.PERMISSION_MERGE_ALL)) {
            options.add(Direction.ALL);
        }
        options.addAll(plot.getRelativeDirections());

        Direction direction;
        String input = args.length != 0 ? args[0].toLowerCase() : ACTION_MERGE_FORWARD;
        if (ACTION_MERGE_FORWARD.equals(input) || ACTION_MERGE_FORWARD.substring(0, 1).equals(input)) {
            direction = findForward(options, player.getLocationFull());
        } else if (ACTION_MERGE_AUTO.equals(input) || ACTION_MERGE_AUTO.substring(0, 1).equals(input)) {
            direction = Direction.ALL;
        } else {
            direction = Direction.parseString(input);
        }

        if (direction == Direction.ALL && !player.hasPermission(Permission.PERMISSION_MERGE_ALL)) {
            player.sendMessage(
                    TranslatableCaption.of("permission.no_permission"),
                    TagResolver.resolver("node", Tag.inserting(Permission.PERMISSION_MERGE_ALL.asComponent()))
            );
            return false;
        }
        if (!options.contains(direction)) {
            List<String> aliases = Lists.newArrayListWithCapacity(options.size());
            for (Direction option : options) {
                if (option == Direction.ALL) {
                    aliases.add(option.getName());
                } else {
                    aliases.add(option.getAlias());
                }
            }
            aliases.add(ACTION_MERGE_FORWARD);

            player.sendMessage(
                    TranslatableCaption.of("invalid.not_valid_block"),
                    TagResolver.resolver("value", Tag.inserting(
                            TranslatableCaption.of("flags.flag_error_enum").toComponent(
                                    player,
                                    TagResolver.resolver("list", Tag.inserting(
                                            Component.text(String.join(", ", aliases))
                                    ))
                            )
                    ))
            );
            direction = findForward(options, player.getLocationFull());
            player.sendMessage(
                    TranslatableCaption.of("help.direction"),
                    TagResolver.resolver("dir", Tag.inserting(Component.text(direction.getName())))
            );
            return false;
        }

        final int size = plot.getConnectedPlots().size();
        int max = player.hasPermissionRange("plots.merge", Settings.Limit.MAX_PLOTS);
        PlotMergeEvent event = this.eventDispatcher.callMerge(plot, direction, max, player);
        if (event.getEventResult() == Result.DENY) {
            player.sendMessage(
                    TranslatableCaption.of("events.event_denied"),
                    TagResolver.resolver("value", Tag.inserting(Component.text("Merge")))
            );
            return false;
        }
        boolean force = event.getEventResult() == Result.FORCE;
        final int maxSize = event.getMax();
        direction = event.getDir();

        checkTrue(force || options.contains(direction),
                TranslatableCaption.of("events.event_denied"),
                TagResolver.resolver("value", Tag.inserting(
                        Component.text("Invalid direction " + direction.getName())
                ))
        );

        if (!force && size - 1 > maxSize) {
            player.sendMessage(
                    TranslatableCaption.of("permission.no_permission"),
                    TagResolver.resolver("node", Tag.inserting(Component.text(Permission.PERMISSION_MERGE + "." + (size + 1))))
            );
            return false;
        }
        final PlotArea plotArea = plot.getArea();
        PlotExpression priceExr = plotArea.getPrices().getOrDefault("merge", null);
        final double price = priceExr == null ? 0d : priceExr.evaluate(size);

        UUID uuid = player.getUUID();

        if (!force && !plot.isOwner(uuid)) {
            if (!player.hasPermission(Permission.PERMISSION_ADMIN_COMMAND_MERGE)) {
                player.sendMessage(TranslatableCaption.of("permission.no_plot_perms"));
                return false;
            } else {
                uuid = plot.getOwnerAbs();
            }
        }

        boolean removeRoads = args.length < 2 || ("true".equalsIgnoreCase(args[1]) || "t".equalsIgnoreCase(args[1]));

        if (direction == Direction.ALL) {
            if (!force && !removeRoads && !player.hasPermission(Permission.PERMISSION_MERGE_KEEP_ROAD)) {
                player.sendMessage(
                        TranslatableCaption.of("permission.no_permission"),
                        TagResolver.resolver(
                                "node",
                                Tag.inserting(Permission.PERMISSION_MERGE_KEEP_ROAD)
                        )
                );
                return true;
            }
            if (plot.getPlotModificationManager().autoMerge(Direction.ALL, maxSize, uuid, player, removeRoads)) {
                if (this.econHandler.isEnabled(plotArea) && !player.hasPermission(Permission.PERMISSION_ADMIN_BYPASS_ECON) && price > 0d) {
                    this.econHandler.withdrawMoney(player, price);
                    player.sendMessage(
                            TranslatableCaption.of("economy.removed_balance"),
                            TagResolver.resolver("money", Tag.inserting(Component.text(this.econHandler.format(price)))),
                            TagResolver.resolver(
                                    "balance",
                                    Tag.inserting(Component.text(this.econHandler.format(this.econHandler.getMoney(player))))
                            )
                    );
                }
                player.sendMessage(TranslatableCaption.of("merge.success_merge"));
                eventDispatcher.callPostMerge(player, plot);
                return true;
            }
            player.sendMessage(TranslatableCaption.of("merge.no_available_automerge"));
            return false;
        }
        if (!force && this.econHandler.isEnabled(plotArea) && !player.hasPermission(Permission.PERMISSION_ADMIN_BYPASS_ECON) && price > 0d && this.econHandler.getMoney(
                player) < price) {
            player.sendMessage(
                    TranslatableCaption.of("economy.cannot_afford_merge"),
                    TagResolver.resolver("money", Tag.inserting(Component.text(this.econHandler.format(price))))
            );
            return false;
        }

        if (!force && !removeRoads && !player.hasPermission(Permission.PERMISSION_MERGE_KEEP_ROAD)) {
            player.sendMessage(
                    TranslatableCaption.of("permission.no_permission"),
                    TagResolver.resolver("node", Tag.inserting(Permission.PERMISSION_MERGE_KEEP_ROAD))
            );
            return true;
        }
        if (plot.getPlotModificationManager().autoMerge(direction, maxSize - size, uuid, player, removeRoads)) {
            if (this.econHandler.isEnabled(plotArea) && !player.hasPermission(Permission.PERMISSION_ADMIN_BYPASS_ECON) && price > 0d) {
                this.econHandler.withdrawMoney(player, price);
                player.sendMessage(
                        TranslatableCaption.of("economy.removed_balance"),
                        TagResolver.resolver("money", Tag.inserting(Component.text(this.econHandler.format(price))))
                );
            }
            player.sendMessage(TranslatableCaption.of("merge.success_merge"));
            eventDispatcher.callPostMerge(player, plot);
            return true;
        }
        Plot adjacent = plot.getRelative(direction);
        if (adjacent == null || !adjacent.hasOwner() || adjacent.isMerged(direction.opposite()) || (!force && adjacent.isOwner(uuid))) {
            player.sendMessage(TranslatableCaption.of("merge.no_available_automerge"));
            return false;
        }
        if (!force && !player.hasPermission(Permission.PERMISSION_MERGE_OTHER)) {
            player.sendMessage(
                    TranslatableCaption.of("permission.no_permission"),
                    TagResolver.resolver("node", Tag.inserting(Permission.PERMISSION_MERGE_OTHER))
            );
            return false;
        }
        java.util.Set<UUID> uuids = adjacent.getOwners();
        boolean isOnline = false;
        for (final UUID owner : uuids) {
            final PlotPlayer<?> accepter = PlotSquared.platform().playerManager().getPlayerIfExists(owner);
            if (!force && accepter == null) {
                continue;
            }
            isOnline = true;
            final Direction dir = direction;
            Runnable run = () -> {
                accepter.sendMessage(TranslatableCaption.of("merge.merge_accepted"));
                plot.getPlotModificationManager().autoMerge(dir, maxSize - size, owner, player, removeRoads);
                PlotPlayer<?> plotPlayer = PlotSquared.platform().playerManager().getPlayerIfExists(player.getUUID());
                if (plotPlayer == null) {
                    accepter.sendMessage(TranslatableCaption.of("merge.merge_not_valid"));
                    return;
                }
                if (this.econHandler.isEnabled(plotArea) && !player.hasPermission(Permission.PERMISSION_ADMIN_BYPASS_ECON) && price > 0d) {
                    if (!force && this.econHandler.getMoney(player) < price) {
                        player.sendMessage(
                                TranslatableCaption.of("economy.cannot_afford_merge"),
                                TagResolver.resolver("money", Tag.inserting(Component.text(this.econHandler.format(price))))
                        );
                        return;
                    }
                    this.econHandler.withdrawMoney(player, price);
                    player.sendMessage(
                            TranslatableCaption.of("economy.removed_balance"),
                            TagResolver.resolver("money", Tag.inserting(Component.text(this.econHandler.format(price))))
                    );
                }
                player.sendMessage(TranslatableCaption.of("merge.success_merge"));
                eventDispatcher.callPostMerge(player, plot);
            };
            if (!force && hasConfirmation(player)) {
                CmdConfirm.addPending(accepter, MINI_MESSAGE.serialize(MINI_MESSAGE
                                .deserialize(
                                        TranslatableCaption.of("merge.merge_request_confirm").getComponent(player),
                                        TagResolver.builder()
                                                .tag("player", Tag.inserting(Component.text(player.getName())))
                                                .tag(
                                                        "location",
                                                        Tag.inserting(Component.text(plot.getWorldName() + " " + plot.getId()))
                                                )
                                                .build()
                                )),
                        run
                );
            } else {
                run.run();
            }
        }
        if (force || !isOnline) {
            if (force || player.hasPermission(Permission.PERMISSION_ADMIN_COMMAND_MERGE_OTHER_OFFLINE)) {
                if (plot.getPlotModificationManager().autoMerge(
                        direction,
                        maxSize - size,
                        uuids.iterator().next(),
                        player,
                        removeRoads
                )) {
                    if (this.econHandler.isEnabled(plotArea) && !player.hasPermission(Permission.PERMISSION_ADMIN_BYPASS_ECON) && price > 0d) {
                        if (!force && this.econHandler.getMoney(player) < price) {
                            player.sendMessage(
                                    TranslatableCaption.of("economy.cannot_afford_merge"),
                                    TagResolver.resolver("money", Tag.inserting(Component.text(this.econHandler.format(price))))
                            );
                            return false;
                        }
                        this.econHandler.withdrawMoney(player, price);
                        player.sendMessage(
                                TranslatableCaption.of("economy.removed_balance"),
                                TagResolver.resolver("money", Tag.inserting(Component.text(this.econHandler.format(price))))
                        );
                    }
                    player.sendMessage(TranslatableCaption.of("merge.success_merge"));
                    eventDispatcher.callPostMerge(player, plot);
                    return true;
                }
            }
            player.sendMessage(TranslatableCaption.of("merge.no_available_automerge"));
            return false;
        }
        player.sendMessage(TranslatableCaption.of("merge.merge_requested"));
        return true;
    }

}
