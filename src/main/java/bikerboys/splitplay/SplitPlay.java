package bikerboys.splitplay;

import bikerboys.splitplay.data.*;
import eu.midnightdust.lib.config.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.message.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.intellij.lang.annotations.*;

import java.util.*;

public class SplitPlay implements ModInitializer {

	public static final ResourceLocation ID = new ResourceLocation("splitplay", "update_number");

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



	public static boolean isInactivePlayer(ServerPlayer player) {
		for (SplitPlayerGroup group : groups) {
			if (group.players.contains(player.getUUID())) {
				if (group.activeIndex == group.players.indexOf(player.getUUID())) {
					return false;
				} else {
					return true;
				}
			}
		}

		return false;
	}

	private static void resetInactivePlayer(MinecraftServer server, UUID uuid) {
		ServerPlayer player = server.getPlayerList().getPlayer(uuid);
		if (player == null) return;

		var spawnPos = player.getRespawnPosition();

		// Find safe Y (top solid block)
		int y = server.overworld().getHeight(
				net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				spawnPos.getX(),
				spawnPos.getZ()
		);

		player.teleportTo(
                spawnPos.getX() + 0.5,
				y,
				spawnPos.getZ() + 0.5
        );

		player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
	}

	private static boolean unlinkPlayer(MinecraftServer server, ServerPlayer player) {

		UUID uuid = player.getUUID();

		SplitPlayState state = getState(server);
		List<SplitPlayState.Group> newGroups = new ArrayList<>();

		boolean modified = false;

		for (SplitPlayState.Group g : state.getGroups()) {

			if (g.players().contains(uuid)) {

				List<UUID> updated = new ArrayList<>(g.players());
				int removedIndex = updated.indexOf(uuid);

				updated.remove(uuid);

				// If removed player was NOT active → reset them
				if (removedIndex != g.activeIndex()) {
					resetInactivePlayer(server, uuid);
				}

				if (updated.size() > 1) {
					int newActive = g.activeIndex();

					if (removedIndex < newActive) {
						newActive--; // shift index left
					}

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

					int removedIndex = group.players.indexOf(uuid);

					group.players.remove(uuid);

					if (removedIndex != group.activeIndex) {
						resetInactivePlayer(server, uuid);
					}

					if (group.players.size() <= 1) {
						return true;
					}

					if (removedIndex < group.activeIndex) {
						group.activeIndex--;
					}

					if (group.activeIndex >= group.players.size()) {
						group.activeIndex = 0;
					}

					return false;
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

				// Reset ALL inactive players
				for (int i = 0; i < g.players().size(); i++) {
					if (i != g.activeIndex()) {
						resetInactivePlayer(server, g.players().get(i));
					}
				}

				modified = true;
				continue;
			}

			newGroups.add(g);
		}

		if (modified) {
			state.setData(new SplitPlayState.Packed(newGroups));

			// ALSO update runtime groups
			groups.removeIf(group -> {
				if (group.players.contains(uuid)) {

					for (int i = 0; i < group.players.size(); i++) {
						if (i != group.activeIndex) {
							resetInactivePlayer(server, group.players.get(i));
						}
					}

					return true;
				}
				return false;
			});
		}

		return modified;
	}

	private static boolean addPlayerToGroup(MinecraftServer server, ServerPlayer toAdd, ServerPlayer target) {

		UUID targetUUID = target.getUUID();
		UUID addUUID = toAdd.getUUID();

		SplitPlayState state = getState(server);

		// ❗ NEW CHECK
		if (isInAnyGroup(state, addUUID)) {
			return false;
		}

		List<SplitPlayState.Group> newGroups = new ArrayList<>();
		boolean modified = false;

		for (int i = 0; i < state.getGroups().size(); i++) {

			SplitPlayState.Group g = state.getGroups().get(i);

			if (g.players().contains(targetUUID)) {

				List<UUID> updated = new ArrayList<>(g.players());
				updated.add(addUUID);

				newGroups.add(new SplitPlayState.Group(updated, g.activeIndex()));
				modified = true;

				// runtime sync
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

	private static boolean isInAnyGroup(SplitPlayState state, UUID uuid) {
		return groups.stream()
				.anyMatch(g -> g.players.contains(uuid));
	}


	private static void createGroup(MinecraftServer server, List<ServerPlayer> players) {
		SplitPlayState state = getState(server);

		for (ServerPlayer player : players) {
			if (isInAnyGroup(state, player.getUUID())) {
				return; // or send error message
			}
		}

		List<UUID> uuids = players.stream().map(ServerPlayer::getUUID).toList();

		groups.add(new SplitPlayerGroup(uuids, server));
		state.addGroup(uuids);
	}

	public static SplitPlayState getState(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(
				SplitPlayState::load,   // load from NBT
				SplitPlayState::new,    // create new if absent
				"splitplay_groups"      // unique ID
		);
	}
}