package bikerboys.splitplay.Util;

import bikerboys.splitplay.*;
import net.minecraft.network.chat.*;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.phys.*;

import java.util.concurrent.atomic.*;

public class PlayerUtils {


    public static void moveInventory(Player from, Player to) {
        Inventory fromInventory = from.getInventory();
        Inventory toInventory = to.getInventory();
        toInventory.clearContent();

        for (int i = 0; i < fromInventory.getContainerSize(); i++) {
            toInventory.setItem(i, fromInventory.getItem(i));
        }

        fromInventory.clearContent();

    }

    public static void setToPlayerPosition(Player target, Player subject) {
        Vec3 position = target.position();
        float yRot = target.getYRot();
        float xRot = target.getXRot();

        Vec3 subjectposition = subject.position();
        float subjectyRot = subject.getYRot();
        float subjectxRot = subject.getXRot();

        target.teleportTo(subjectposition.x, subjectposition.y, subjectposition.z);
        target.setYRot(subjectyRot);
        target.setXRot(subjectxRot);


        subject.teleportTo(position.x, position.y, position.z);
        subject.setYRot(yRot);
        subject.setXRot(xRot);

    }

    public static void sendTitle(Component text, int fadein, int stay, int fadeout, Player... players) {
        ClientboundSetTitleTextPacket titleTextPacket = new ClientboundSetTitleTextPacket(text);
        ClientboundSetTitlesAnimationPacket titlesAnimationPacket = new ClientboundSetTitlesAnimationPacket(fadein, stay, fadeout);

        for (Player player : players) {
            ((ServerPlayer) player).connection.send(titleTextPacket);
            ((ServerPlayer) player).connection.send(titlesAnimationPacket);
        }
    }

    public static boolean isALinkedPlayer(Player player) {
        if (SplitPlay.splitPlayerPairs.isEmpty()) return false;
        AtomicBoolean returnVal = new AtomicBoolean(false);

        SplitPlay.splitPlayerPairs.forEach((splitPlayerPair -> {
            if (player.getUUID() == splitPlayerPair.player1uuid || player.getUUID() == splitPlayerPair.player2uuid) {
                returnVal.set(true);
            }
        }));


        return returnVal.get();

    }


}
