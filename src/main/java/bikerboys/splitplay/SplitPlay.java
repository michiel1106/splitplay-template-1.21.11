package bikerboys.splitplay;

import bikerboys.splitplay.data.*;
import eu.midnightdust.lib.config.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class SplitPlay implements ModInitializer {

	public static final String MOD_ID = "splitplay";

	private static MinecraftServer lastServer = null;

	public static boolean paused = true;
	public static final List<SplitPlayerGroup> groups = new ArrayList<>();

	@Override
	public void onInitialize() {

		MidnightConfig.init(MOD_ID, SplitConfig.class);

		ServerTickEvents.START_SERVER_TICK.register(server -> {

			if (server != lastServer) {
				lastServer = server;
				groups.clear();
			}

			loadGroupsIfNeeded(server);

			if (!paused) {
				groups.forEach(group -> group.tick(server));
			}
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			groups.clear();
			lastServer = null;
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, ctx, selection) -> {

			dispatcher.register(Commands.literal("splitplay")

					.then(Commands.literal("group")
							.then(Commands.argument("p1", EntityArgument.player())
									.then(Commands.argument("p2", EntityArgument.player())
											.executes(ctx2 -> {

												List<ServerPlayer> players = List.of(
														EntityArgument.getPlayer(ctx2, "p1"),
														EntityArgument.getPlayer(ctx2, "p2")
												);

												createGroup(ctx2.getSource().getServer(), players);

												ctx2.getSource().sendSuccess(() ->
														Component.literal("Group created (" + players.size() + " players)"), false);

												return 1;
											})))
					)

					.then(Commands.literal("add")
							.then(Commands.argument("player", EntityArgument.player())
									.then(Commands.argument("target", EntityArgument.player())
											.executes(ctx2 -> {

												ServerPlayer toAdd = EntityArgument.getPlayer(ctx2, "player");
												ServerPlayer target = EntityArgument.getPlayer(ctx2, "target");

												boolean success = addPlayerToGroup(ctx2.getSource().getServer(), toAdd, target);

												if (!success) {
													ctx2.getSource().sendFailure(Component.literal("Target is not in a group"));
													return 0;
												}

												ctx2.getSource().sendSuccess(() ->
																Component.literal("Added " + toAdd.getName().getString() + " to group"),
														false
												);

												return 1;
											}))
							))


					.then(Commands.literal("unlink")
							.then(Commands.argument("player", EntityArgument.player())
									.executes(ctx2 -> {

										ServerPlayer player = EntityArgument.getPlayer(ctx2, "player");

										boolean success = unlinkPlayer(ctx2.getSource().getServer(), player);

										if (!success) {
											ctx2.getSource().sendFailure(Component.literal("Player is not in a group"));
											return 0;
										}

										ctx2.getSource().sendSuccess(() ->
														Component.literal("Removed " + player.getName().getString() + " from group"),
												false
										);

										return 1;
									}))
					)

					.then(Commands.literal("dissolve")
							.then(Commands.argument("player", EntityArgument.player())
									.executes(ctx2 -> {

										ServerPlayer player = EntityArgument.getPlayer(ctx2, "player");

										boolean success = dissolveGroup(ctx2.getSource().getServer(), player);

										if (!success) {
											ctx2.getSource().sendFailure(Component.literal("Player is not in a group"));
											return 0;
										}

										ctx2.getSource().sendSuccess(() ->
														Component.literal("Group dissolved"),
												false
										);

										return 1;
									}))
					)

					.then(Commands.literal("list")
							.executes(ctx2 -> {

								MinecraftServer server = ctx2.getSource().getServer();

								if (groups.isEmpty()) {
									ctx2.getSource().sendFailure(Component.literal("No groups found"));
									return 0;
								}

								int index = 1;

								for (SplitPlayerGroup group : groups) {

									Component line = Component.literal("Group " + index + ": ");

									for (int i = 0; i < group.players.size(); i++) {

										UUID uuid = group.players.get(i);
										ServerPlayer player = server.getPlayerList().getPlayer(uuid);

										String name = (player != null)
												? player.getName().getString()
												: uuid.toString().substring(0, 8);

										boolean isActive = (i == group.activeIndex);

										Component nameComponent = Component.literal(name)
												.withStyle(style -> style.withColor(
														isActive ? net.minecraft.ChatFormatting.GREEN
																: net.minecraft.ChatFormatting.RED
												));

										line = line.copy().append(nameComponent);

										if (i < group.players.size() - 1) {
											line = line.copy().append(Component.literal(", "));
										}
									}

									Component finalLine = line;
									ctx2.getSource().sendSuccess(() -> finalLine, false);
									index++;
								}

								return 1;
							})
					)

					.then(Commands.literal("toggle").executes(ctx2 -> {
						paused = !paused;
						ctx2.getSource().sendSuccess(() ->
								Component.literal(paused ? "Paused" : "Resumed"), false);
						return 1;
					}))
					);
		});
	}


	private static boolean unlinkPlayer(MinecraftServer server, ServerPlayer player) {

		UUID uuid = player.getUUID();

		SplitPlayState state = getState(server);
		List<SplitPlayState.Group> newGroups = new ArrayList<>();

		boolean modified = false;

		for (SplitPlayState.Group g : state.getGroups()) {

			if (g.players().contains(uuid)) {

				List<UUID> updated = new ArrayList<>(g.players());
				updated.remove(uuid);

				// If group becomes empty or 1 player, remove it entirely
				if (updated.size() > 1) {
					int newActive = g.activeIndex();

					// Fix active index if needed
					if (newActive >= updated.size()) {
						newActive = 0;
					}

					newGroups.add(new SplitPlayState.Group(updated, newActive));
				}

				modified = true;

			} else {
				newGroups.add(g);
			}
		}

		if (modified) {
			state.setData(new SplitPlayState.Packed(newGroups));

			// ALSO update runtime groups
			groups.removeIf(group -> {
				if (group.players.contains(uuid)) {
					group.players.remove(uuid);
					return group.players.size() <= 1;
				}
				return false;
			});
		}

		return modified;
	}


	private static boolean dissolveGroup(MinecraftServer server, ServerPlayer player) {

		UUID uuid = player.getUUID();

		SplitPlayState state = getState(server);
		List<SplitPlayState.Group> newGroups = new ArrayList<>();

		boolean modified = false;

		for (SplitPlayState.Group g : state.getGroups()) {

			if (g.players().contains(uuid)) {
				modified = true;
				continue; // skip → removes group
			}

			newGroups.add(g);
		}

		if (modified) {
			state.setData(new SplitPlayState.Packed(newGroups));

			// ALSO update runtime groups
			groups.removeIf(group -> group.players.contains(uuid));
		}

		return modified;
	}

	private static boolean addPlayerToGroup(MinecraftServer server, ServerPlayer toAdd, ServerPlayer target) {

		UUID targetUUID = target.getUUID();
		UUID addUUID = toAdd.getUUID();

		SplitPlayState state = getState(server);
		List<SplitPlayState.Group> newGroups = new ArrayList<>();

		boolean modified = false;

		for (int i = 0; i < state.getGroups().size(); i++) {

			SplitPlayState.Group g = state.getGroups().get(i);

			if (g.players().contains(targetUUID)) {

				if (g.players().contains(addUUID)) {
					newGroups.add(g);
					continue;
				}

				List<UUID> updated = new ArrayList<>(g.players());
				updated.add(addUUID);

				// --- SAVE DATA ---
				newGroups.add(new SplitPlayState.Group(updated, g.activeIndex()));
				modified = true;

				// --- RUNTIME FIX (THIS WAS MISSING) ---
				SplitPlayerGroup runtimeGroup = groups.get(i);
				runtimeGroup.players.add(addUUID);

			} else {
				newGroups.add(g);
			}
		}

		if (modified) {
			state.setData(new SplitPlayState.Packed(newGroups));
		}

		return modified;
	}

	private static void loadGroupsIfNeeded(MinecraftServer server) {
		if (!groups.isEmpty()) return;

		SplitPlayState state = getState(server);

		for (var g : state.getGroups()) {
			SplitPlayerGroup group = new SplitPlayerGroup(g.players(), server);
			group.activeIndex = g.activeIndex();
			groups.add(group);
		}
	}

	private static void createGroup(MinecraftServer server, List<ServerPlayer> players) {
		List<UUID> uuids = players.stream().map(ServerPlayer::getUUID).toList();

		groups.add(new SplitPlayerGroup(uuids, server));

		SplitPlayState state = getState(server);
		state.addGroup(uuids);
	}

	public static SplitPlayState getState(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(SplitPlayState.TYPE);
	}
}