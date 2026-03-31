package bikerboys.splitplay;

import static bikerboys.splitplay.SplitPlay.ID;
import bikerboys.splitplay.Util.*;
import com.sun.jna.platform.win32.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.*;
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

		HudRenderCallback.EVENT.register(((guiGraphics, v) -> {
			if (!display.isEmpty()) {
				DebugUtils.debug(display);

				renderOverlayMessage(guiGraphics, v);
			}

		}));






		ClientTickEvents.START_CLIENT_TICK.register((minecraft -> {

			if (timeRemaining == 0) {
				display = "";
			}

			if (timeRemaining > 0) {
				timeRemaining--;
			}


		}));



		ClientPlayNetworking.registerGlobalReceiver(ID, (client, handler, buf, responseSender) -> {

			net.minecraft.network.FriendlyByteBuf copy = new net.minecraft.network.FriendlyByteBuf(buf.copy());
			String text = String.valueOf(copy.readVarInt()); // ✅ safe copy

			client.execute(() -> {
				display = text;
				timeRemaining = 20;
			});

		});




	}



	private void renderOverlayMessage(GuiGraphics guiGraphics, float deltaTracker) {
		Font font = Minecraft.getInstance().gui.getFont();
		if (this.display != null && this.timeRemaining > 0) {

			float j = this.timeRemaining - deltaTracker;
			int m = (int)(j * 255.0F / 20.0F);
			if (m > 255) {
				m = 255;
			}

			m = 255;
			if (m > 8) {
				guiGraphics.pose().pushPose();
				guiGraphics.pose().translate((float)(guiGraphics.guiWidth() / 2), (float)(guiGraphics.guiHeight() - 58), 0.0F);
				int l = 16777215;

				int n = m << 24 & 0xFF000000;
				guiGraphics.pose().scale(1.6f, 1.6f, 0f);
				int o = font.width(this.display);
				drawBackdrop(guiGraphics, font, -4, o, 16777215 | n);
				guiGraphics.drawString(font, this.display, -o / 2, -4, l | n);
				guiGraphics.pose().popPose();
			}

		}
	}

	private void drawBackdrop(GuiGraphics guiGraphics, Font font, int i, int j, int k) {
		int l = Minecraft.getInstance().options.getBackgroundColor(0.0F);
		if (l != 0) {
			int m = -j / 2;
			guiGraphics.fill(m - 2, i - 2, m + j + 2, i + 9 + 2, FastColor.ARGB32.multiply(l, k));
		}
	}
}