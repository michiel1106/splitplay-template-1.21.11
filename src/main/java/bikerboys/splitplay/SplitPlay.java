package bikerboys.splitplay;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.*;
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


	}
}