package bikerboys.splitplay;

import bikerboys.splitplay.Util.*;
import bikerboys.splitplay.networking.*;
import com.sun.jna.platform.win32.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.fabricmc.fabric.impl.client.rendering.hud.*;
import net.fabricmc.fabric.impl.screenhandler.client.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.network.chat.*;
import net.minecraft.resources.*;
import net.minecraft.util.*;
import net.minecraft.util.profiling.*;

public class SplitPlayClient implements ClientModInitializer {
	String display = "";
	int timeRemaining = 0;



	@Override
	public void onInitializeClient() {
		HudElementRegistryImpl.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE, Identifier.fromNamespaceAndPath(SplitPlay.MOD_ID, "splitplay"), new HudElement() {
			@Override
			public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
				if (!display.isEmpty()) {
					DebugUtils.debug(display);

					renderOverlayMessage(guiGraphics, deltaTracker);
				}
			}
		});





		ClientTickEvents.START_CLIENT_TICK.register((minecraft -> {

			if (timeRemaining == 0) {
				display = "";
			}

			if (timeRemaining > 0) {
				timeRemaining--;
			}


		}));

		ClientPlayNetworking.registerGlobalReceiver(UpdateNumberPacket.ID, (updateNumberPacket, context) -> {
			context.client().execute(() -> {
				display = updateNumberPacket.text();
				timeRemaining = 20;
			});
		});




	}



	private void renderOverlayMessage(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
		Font font = Minecraft.getInstance().gui.getFont();
		if (this.display != null && this.timeRemaining > 0) {

			float f = this.timeRemaining - deltaTracker.getGameTimeDeltaPartialTick(false);
			int i = (int)(f * 255.0F / 40.0F);
			if (i > 255) {
				i = 255;
			}
			i = 255;

			if (i > 0) {
				guiGraphics.nextStratum();
				guiGraphics.pose().pushMatrix();

				guiGraphics.pose().translate((float) guiGraphics.guiWidth() / 2, guiGraphics.guiHeight() - 58);
				int j = ARGB.white(i);
				guiGraphics.pose().scale(1.6f, 1.6f);
				int k = font.width(this.display);
				guiGraphics.drawStringWithBackdrop(font, Component.nullToEmpty(display), -k / 2, -4, k, j);
				guiGraphics.pose().popMatrix();
			}

			Profiler.get().pop();
		}
	}
}