package bikerboys.splitplay;

import bikerboys.splitplay.Util.*;
import bikerboys.splitplay.data.*;
import eu.midnightdust.core.*;
import eu.midnightdust.core.config.*;
import eu.midnightdust.lib.config.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.*;
import net.minecraft.server.*;
import net.minecraft.server.level.*;
import net.minecraft.server.permissions.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.level.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SplitPlay implements ModInitializer {

	public static final String MOD_ID = "splitplay";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static boolean paused = true;
	public static final List<SplitPlayerPair> splitPlayerPairs = new ArrayList<>();

	@Override
	public void onInitialize() {

		MidnightConfig.init(MOD_ID, SplitConfig.class);

		// ===== TICK =====
		ServerTickEvents.START_SERVER_TICK.register(server -> {

			loadPairsIfNeeded(server);

			if (!paused) {
				splitPlayerPairs.forEach(pair -> pair.tick(server));
			}
		});

		// ===== COMMANDS =====
		CommandRegistrationCallback.EVENT.register((dispatcher, ctx, selection) -> {

			dispatcher.register(Commands.literal("splitplay")
					.requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))

					// ===== LINK =====
					.then(Commands.literal("link")
							.then(Commands.argument("player1", EntityArgument.player())
									.then(Commands.argument("player2", EntityArgument.player())
											.executes(ctx2 -> {

												ServerPlayer p1 = EntityArgument.getPlayer(ctx2, "player1");
												ServerPlayer p2 = EntityArgument.getPlayer(ctx2, "player2");

												if (PlayerUtils.isALinkedPlayer(p1)) {
													ctx2.getSource().sendFailure(Component.literal(p1.getName().getString() + " is already linked!"));
													return 0;
												}

												if (PlayerUtils.isALinkedPlayer(p2)) {
													ctx2.getSource().sendFailure(Component.literal(p2.getName().getString() + " is already linked!"));
													return 0;
												}

												linkPlayers(ctx2.getSource().getServer(), p1, p2);

												ctx2.getSource().sendSuccess(() ->
																Component.literal("Linked " + p1.getName().getString() + " and " + p2.getName().getString()),
														false
												);

												return 1;
											}))))

					// ===== UNLINK =====
					.then(Commands.literal("unlink")
							.then(Commands.argument("player", EntityArgument.player())
									.executes(ctx2 -> {

										ServerPlayer player = EntityArgument.getPlayer(ctx2, "player");

										boolean removed = unlinkPlayer(ctx2.getSource().getServer(), player);

										if (!removed) {
											ctx2.getSource().sendFailure(Component.literal("Player is not linked"));
											return 0;
										}

										ctx2.getSource().sendSuccess(() ->
														Component.literal("Unlinked " + player.getName().getString()),
												false
										);

										return 1;
									})))

					// ===== TOGGLE =====
					.then(Commands.literal("toggle").executes(ctx2 -> {
						paused = !paused;

						ctx2.getSource().sendSuccess(() ->
										Component.literal(paused ? "Paused" : "Resumed"),
								false
						);

						return 1;
					}))
			);
		});
	}

	// ===== LOAD =====
	private static void loadPairsIfNeeded(MinecraftServer server) {
		if (!splitPlayerPairs.isEmpty()) return;

		SplitPlayState state = getState(server);

		for (var pair : state.getPairs()) {
			SplitPlayerPair pairObj = new SplitPlayerPair(pair.player1(), pair.player2(), server);
			pairObj.activePlayer = pair.player1IsActive() ? SplitPlayerPair.ActivePlayer.PLAYER1
					: SplitPlayerPair.ActivePlayer.PLAYER2;
			splitPlayerPairs.add(pairObj);
		}
	}

	// ===== LINK =====
	private static void linkPlayers(MinecraftServer server, ServerPlayer p1, ServerPlayer p2) {
		splitPlayerPairs.add(new SplitPlayerPair(p1.getUUID(), p2.getUUID(), server));

		SplitPlayState state = getState(server);
		state.addPair(p1.getUUID(), p2.getUUID());
	}

	// ===== UNLINK =====
	private static boolean unlinkPlayer(MinecraftServer server, ServerPlayer player) {

		UUID uuid = player.getUUID();

		// Find affected pairs first
		List<SplitPlayerPair> affectedPairs = splitPlayerPairs.stream()
				.filter(pair -> pair.player1uuid.equals(uuid) || pair.player2uuid.equals(uuid))
				.toList();

		if (affectedPairs.isEmpty()) return false;

		// Handle players BEFORE removal
		for (SplitPlayerPair pair : affectedPairs) {

			Player active = pair.getActivePlayer();
			Player inactive = pair.getInactivePlayer();

			// Determine which one is inactive relative to the command target
			Player toReset = (active.getUUID().equals(uuid)) ? inactive : active;

			if (toReset instanceof ServerPlayer sp) {

				ServerLevel level = server.overworld();
				BlockPos spawn = level.getRespawnData().pos();

				// Teleport to world spawn
				sp.teleportTo(
						level,
						spawn.getX() + 0.5,
						spawn.getY(),
						spawn.getZ() + 0.5,
						Set.of(),
						sp.getYRot(),
						sp.getXRot(),
						false
				);

				// Set survival
				sp.setGameMode(GameType.SURVIVAL);
			}
		}

		// Remove from runtime list
		splitPlayerPairs.removeIf(pair ->
				pair.player1uuid.equals(uuid) || pair.player2uuid.equals(uuid)
		);

		// Remove from saved data
		SplitPlayState state = getState(server);

		List<SplitPlayState.Pair> newPairs = state.getPairs().stream()
				.filter(p -> !p.player1().equals(uuid) && !p.player2().equals(uuid))
				.toList();

		state.setData(new SplitPlayState.Packed(newPairs));

		return true;
	}

	// ===== STATE ACCESS =====
	public static SplitPlayState getState(MinecraftServer server) {
		return server.overworld()
				.getDataStorage()
				.computeIfAbsent(SplitPlayState.TYPE);
	}
}