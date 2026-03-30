package bikerboys.splitplay.Util;

import bikerboys.splitplay.*;
import bikerboys.splitplay.networking.*;
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
        PositionMoveRotation targetPMR = PositionMoveRotation.of(target);
        PositionMoveRotation subjectPMR = PositionMoveRotation.of(subject);

        ServerLevel targetLevel = target.level();
        ServerLevel subjectLevel = subject.level();

        // Swap positions + dimensions
        target.teleportTo(
                subjectLevel,
                subjectPMR.position().x,
                subjectPMR.position().y,
                subjectPMR.position().z,
                Set.of(),
                subjectPMR.yRot(),
                subjectPMR.xRot(),
                false
        );

        subject.teleportTo(
                targetLevel,
                targetPMR.position().x,
                targetPMR.position().y,
                targetPMR.position().z,
                Set.of(),
                targetPMR.yRot(),
                targetPMR.xRot(),
                false
        );



    }

    public static void sendTitle(Component text, int fadein, int stay, int fadeout, Player... players) {
        UpdateNumberPacket packet = new UpdateNumberPacket(text.getString());


        for (Player player : players) {
            ServerPlayNetworking.send((ServerPlayer) player, packet);
        }
    }




}
