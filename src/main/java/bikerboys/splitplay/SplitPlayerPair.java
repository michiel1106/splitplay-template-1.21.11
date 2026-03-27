package bikerboys.splitplay;

import bikerboys.splitplay.Util.*;
import net.minecraft.server.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.player.*;

import java.util.*;

public class SplitPlayerPair {
    Player player1;
    Player player2;

    private ActivePlayer activePlayer = ActivePlayer.PLAYER1;


    public int teleportDelay = 1200;


    public SplitPlayerPair(Player player1, Player player2) {
        this.player1 = player1;
        this.player2 = player2;
    }

    public void tick(MinecraftServer server) {

        if (teleportDelay >= 0) teleportDelay--;

        if (teleportDelay <= 0) initiateTeleport();


        Player inactivePlayer = getInactivePlayer();

        inactivePlayer.setPos(0, -70, 0);
        inactivePlayer.forceAddEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 20), null);
        inactivePlayer.forceAddEffect(new MobEffectInstance(MobEffects.DARKNESS, 20, 20), null);


        if (teleportDelay == 100) {
            PlayerUtils.sendTitle(player1, player2, );
        }



    }

    private void initiateTeleport() {

        /*
        - set inventory from active player to inactive player
        - switch active players position with inactive player position


         */

        PlayerUtils.moveInventory(getActivePlayer(), getInactivePlayer());
        PlayerUtils.setToPlayerPosition(getInactivePlayer(), getActivePlayer());
        switchActivePlayer();
        teleportDelay = 1200;
    }


    public Player getActivePlayer() {
        return activePlayer == ActivePlayer.PLAYER1 ? player1 : player2;
    }

    public Player getInactivePlayer() {
        return activePlayer == ActivePlayer.PLAYER1 ? player2 : player1;
    }

    private void switchActivePlayer() {
        if (activePlayer == ActivePlayer.PLAYER1) {
            activePlayer = ActivePlayer.PLAYER2;
        } else {
            activePlayer = ActivePlayer.PLAYER1;
        }
    }

    public enum ActivePlayer {
        PLAYER1,
        PLAYER2;
    }

}
