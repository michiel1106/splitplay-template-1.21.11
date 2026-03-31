package bikerboys.splitplay.Util;

import bikerboys.splitplay.*;
import static bikerboys.splitplay.SplitPlay.ID;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.chat.*;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.portal.*;
import net.minecraft.world.phys.*;

import java.util.*;
import java.util.concurrent.atomic.*;

public class PlayerUtils {


    public static void moveInventory(Player from, Player to) {
        Inventory fromInventory = from.getInventory();
        Inventory toInventory = to.getInventory();
        toInventory.clearContent();

        for (int i = 0; i < fromInventory.getContainerSize(); i++) {
            toInventory.setItem(i, fromInventory.getItem(i).copy());
        }

        fromInventory.clearContent();

    }

    public static void setToPlayerPosition(ServerPlayer target, ServerPlayer subject) {
        // Store target data
        ServerLevel targetLevel = target.serverLevel();
        double targetX = target.getX();
        double targetY = target.getY();
        double targetZ = target.getZ();
        float targetYaw = target.getYRot();
        float targetPitch = target.getXRot();

        // Store subject data
        ServerLevel subjectLevel = subject.serverLevel();
        double subjectX = subject.getX();
        double subjectY = subject.getY();
        double subjectZ = subject.getZ();
        float subjectYaw = subject.getYRot();
        float subjectPitch = subject.getXRot();

        // Swap positions
        target.teleportTo(
                subjectLevel,
                subjectX,
                subjectY,
                subjectZ,
                subjectYaw,
                subjectPitch
        );

        subject.teleportTo(
                targetLevel,
                targetX,
                targetY,
                targetZ,
                targetYaw,
                targetPitch
        );
    }

    public static void sendTitle(Component text, int fadein, int stay, int fadeout, Player... players) {




        for (Player player : players) {
            net.minecraft.network.FriendlyByteBuf buf = PacketByteBufs.create();
            buf.writeVarInt(Integer.parseInt(text.getString()));
            ServerPlayNetworking.send((ServerPlayer) player, ID, buf);
        }
    }




}
