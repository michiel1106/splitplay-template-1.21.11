package bikerboys.splitplay;

import bikerboys.splitplay.Util.*;
import net.minecraft.network.chat.*;
import net.minecraft.server.*;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.level.*;

import java.util.*;

public class SplitPlayerPair {
    public UUID player1uuid;
    public UUID player2uuid;

    Player player1;
    Player player2;

    MinecraftServer server;

    private ActivePlayer activePlayer = ActivePlayer.PLAYER1;


    public int teleportDelay = 1200;


    public SplitPlayerPair(UUID player1, UUID player2, MinecraftServer server) {
        this.player1uuid = player1;
        this.player2uuid = player2;

        this.server = server;

        this.player1 = getPlayer1();
        this.player2 = getPlayer2();
    }

    public void tick(MinecraftServer server) {
        DebugUtils.debug("tpDelay: " + teleportDelay);
        if (player1 == null && player2 == null) {
            return;
        }

        if (player1 == null) {
            PlayerUtils.sendTitle(Component.literal("Your pair has left the game!"), 20, 20, 20, player2);
        }

        if (player2 == null) {
            PlayerUtils.sendTitle(Component.literal("Your pair has left the game!"), 20, 20, 20, player1);
        }

        if (teleportDelay >= 0) teleportDelay--;

        if (teleportDelay <= 0) initiateTeleport();


        Player inactivePlayer = getInactivePlayer();

        inactivePlayer.teleportTo(0, -70, 0);
        inactivePlayer.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 20), null);
        inactivePlayer.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20, 20), null);

        ((ServerPlayer) inactivePlayer).setGameMode(GameType.SPECTATOR);

        switch (teleportDelay) {
            case 100 -> PlayerUtils.sendTitle(Component.literal("5"), 20, 20, 20, player1, player2);
            case 80 -> PlayerUtils.sendTitle(Component.literal("4"), 20, 20, 20, player1, player2);
            case 60 -> PlayerUtils.sendTitle(Component.literal("3"), 20, 20, 20, player1, player2);
            case 40 -> PlayerUtils.sendTitle(Component.literal("2"), 20, 20, 20, player1, player2);
            case 20 -> PlayerUtils.sendTitle(Component.literal("1"), 20, 20, 20, player1, player2);
        }



    }

    private void initiateTeleport() {
        PlayerUtils.moveInventory(getActivePlayer(), getInactivePlayer());
        PlayerUtils.setToPlayerPosition(getInactivePlayer(), getActivePlayer());
        ((ServerPlayer) getInactivePlayer()).setGameMode(GameType.SURVIVAL);
        ((ServerPlayer) getActivePlayer()).setGameMode(GameType.SPECTATOR);

        switchActivePlayer();
        teleportDelay = 1200;
    }


    public Player getActivePlayer() {
        return activePlayer == ActivePlayer.PLAYER1 ? player1 : player2;
    }

    public Player getInactivePlayer() {
        return activePlayer == ActivePlayer.PLAYER1 ? player2 : player1;
    }

    public Player getPlayer1() {
        return server.getPlayerList().getPlayer(player1uuid);
    }

    public Player getPlayer2() {
        return server.getPlayerList().getPlayer(player2uuid);
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
