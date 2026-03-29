package bikerboys.splitplay;

import bikerboys.splitplay.Util.PlayerUtils;
import bikerboys.splitplay.data.*;
import net.minecraft.network.chat.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;

import java.util.*;

public class SplitPlayerGroup {

    public List<UUID> players;
    public int activeIndex = 0;

    private final MinecraftServer server;
    public int teleportDelay = SplitConfig.swapDelay;

    public SplitPlayerGroup(List<UUID> players, MinecraftServer server) {
        this.players = new ArrayList<>(players);
        this.server = server;
    }

    public void tick(MinecraftServer server) {

        if (players.isEmpty()) return;

        if (teleportDelay >= 0) teleportDelay--;
        if (teleportDelay <= 0) initiateTeleport();

        for (ServerPlayer inactive : getInactivePlayers()) {
            inactive.teleportTo(0, -70, 0);
            inactive.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 20));
            inactive.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20, 20));
            inactive.setGameMode(GameType.SPECTATOR);
        }
    }

    private void initiateTeleport() {
        ServerPlayer current = getActivePlayer();
        ServerPlayer next = getNextPlayer();

        if (current == null || next == null) return;

        PlayerUtils.moveInventory(current, next);
        PlayerUtils.setToPlayerPosition(next, current);
        moveData(current, next);

        next.setGameMode(GameType.SURVIVAL);
        current.setGameMode(GameType.SPECTATOR);

        cyclePlayer();
        teleportDelay = SplitConfig.swapDelay;


        for (UUID player : players) {
            ServerPlayer player1 = server.getPlayerList().getPlayer(player);

            switch (teleportDelay) {
                case 200 -> PlayerUtils.sendTitle(Component.literal("10"), 20, 20, 20, player1);
                case 180 -> PlayerUtils.sendTitle(Component.literal("9"), 20, 20, 20, player1);
                case 160 -> PlayerUtils.sendTitle(Component.literal("8"), 20, 20, 20, player1);
                case 140 -> PlayerUtils.sendTitle(Component.literal("7"), 20, 20, 20, player1);
                case 120 -> PlayerUtils.sendTitle(Component.literal("6"), 20, 20, 20, player1);
                case 100 -> PlayerUtils.sendTitle(Component.literal("5"), 20, 20, 20, player1);
                case 80 -> PlayerUtils.sendTitle(Component.literal("4"), 20, 20, 20, player1);
                case 60 -> PlayerUtils.sendTitle(Component.literal("3"), 20, 20, 20, player1);
                case 40 -> PlayerUtils.sendTitle(Component.literal("2"), 20, 20, 20, player1);
                case 20 -> PlayerUtils.sendTitle(Component.literal("1"), 20, 20, 20, player1);
            }
        }



    }

    private void moveData(ServerPlayer fromActivePlayer, ServerPlayer toInActivePlayer) {
        double fallDistance = fromActivePlayer.fallDistance;
        float health = fromActivePlayer.getHealth();
        int airSupply = fromActivePlayer.getAirSupply();
        int totalExperience = fromActivePlayer.totalExperience;

        ServerPlayer.RespawnConfig respawnConfig = fromActivePlayer.getRespawnConfig();


        toInActivePlayer.fallDistance = fallDistance; toInActivePlayer.setHealth(health);
        toInActivePlayer.setAirSupply(airSupply);
        toInActivePlayer.giveExperiencePoints(totalExperience - toInActivePlayer.totalExperience);
        toInActivePlayer.setRespawnPosition(respawnConfig, false); }

    private void cyclePlayer() {
        activeIndex = (activeIndex + 1) % players.size();

        SplitPlayState state = SplitPlay.getState(server);
        state.setActive(players, activeIndex);
    }

    public ServerPlayer getActivePlayer() {
        return getPlayer(players.get(activeIndex));
    }

    public ServerPlayer getNextPlayer() {
        return getPlayer(players.get((activeIndex + 1) % players.size()));
    }

    public List<ServerPlayer> getInactivePlayers() {
        List<ServerPlayer> list = new ArrayList<>();

        for (int i = 0; i < players.size(); i++) {
            if (i != activeIndex) {
                ServerPlayer p = getPlayer(players.get(i));
                if (p != null) list.add(p);
            }
        }

        return list;
    }

    private ServerPlayer getPlayer(UUID uuid) {
        return server.getPlayerList().getPlayer(uuid);
    }
}