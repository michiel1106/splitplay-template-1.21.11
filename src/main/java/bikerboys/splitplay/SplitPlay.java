package bikerboys.splitplay;

import bikerboys.splitplay.Util.*;
import com.mojang.brigadier.builder.*;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.*;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.*;
import net.minecraft.server.permissions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SplitPlay implements ModInitializer {
	public static final String MOD_ID = "splitplay";
	public static boolean paused = true;

	public static List<SplitPlayerPair> splitPlayerPairs = new ArrayList<>();

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {

		ServerTickEvents.START_SERVER_TICK.register((minecraftServer -> {
			if (!paused) {
				splitPlayerPairs.forEach(splitPlayerPair -> splitPlayerPair.tick(minecraftServer));
			}
		}));


		CommandRegistrationCallback.EVENT.register(((commandDispatcher, commandBuildContext, commandSelection) -> {

			commandDispatcher.register(Commands.literal("splitplay")
					.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
					.then(Commands.literal("link")
							.then(Commands.argument("player1", EntityArgument.player())
									.then(Commands.argument("player2", EntityArgument.player())
											.executes(ctx -> {
												ServerPlayer player1 = EntityArgument.getPlayer(ctx, "player1");
												ServerPlayer player2 = EntityArgument.getPlayer(ctx, "player2");

												if (PlayerUtils.isALinkedPlayer(player1)) {
													ctx.getSource().sendFailure(Component.literal(player1.getName().getString() + " is already linked!"));
													return 0;
												}

												if (PlayerUtils.isALinkedPlayer(player2)) {
													ctx.getSource().sendFailure(Component.literal(player2.getName().getString() + " is already linked!"));
													return 0;
												}


												SplitPlayerPair playerPair = new SplitPlayerPair(player1.getUUID(), player2.getUUID(), ctx.getSource().getServer());

												splitPlayerPairs.add(playerPair);

												ctx.getSource().sendSuccess(() -> Component.literal("Successfully linked " + player1.getName().getString() + " and " + player2.getName().getString() + "!"), false);

												return 1;
											}))))

					.then(Commands.literal("toggle").executes(context -> {
						paused = !paused;

						if (paused) {
							context.getSource().sendSuccess(() -> Component.literal("Swapping is now paused!"), false);
						} else {
							context.getSource().sendSuccess(() -> Component.literal("Swapping has now resumed!"), false);
						}


						return 1;
					}))


			);


		}));


	}



}