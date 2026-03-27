package bikerboys.splitplay.Util;

import net.minecraft.server.level.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;

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

        subject.setPos(position);
        subject.setYRot(yRot);
        subject.setXRot(xRot);

    }

    public static void sendTitle(Player... players) {

    }

}
