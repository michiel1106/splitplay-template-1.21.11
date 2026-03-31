package bikerboys.splitplay.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public class SplitPlayState extends SavedData {

    private static final String NAME = "splitplay_groups";

    private Packed data;

    public SplitPlayState() {
        this(Packed.EMPTY);
    }

    public SplitPlayState(Packed data) {
        this.data = data;
    }

    public Packed getData() {
        return data;
    }

    public List<Group> getGroups() {
        return data.groups();
    }

    public void addGroup(List<UUID> players) {
        List<Group> newGroups = new ArrayList<>(data.groups());
        newGroups.add(new Group(players, 0));
        setData(new Packed(newGroups));
    }

    public void setActive(List<UUID> players, int activeIndex) {
        List<Group> newGroups = data.groups().stream()
                .map(g -> g.players().equals(players)
                        ? new Group(players, activeIndex)
                        : g)
                .toList();

        setData(new Packed(newGroups));
    }

    public void setData(Packed data) {
        this.data = data;
        setDirty();
    }

    // 🔴 REQUIRED in 1.20.1
    @Override
    public CompoundTag save(CompoundTag tag) {
        Packed.CODEC.encodeStart(NbtOps.INSTANCE, data)
                .result()
                .ifPresent(nbt -> tag.put("data", nbt));
        return tag;
    }

    // 🔴 REQUIRED loader
    public static SplitPlayState load(CompoundTag tag) {
        Packed data = Packed.CODEC.parse(NbtOps.INSTANCE, tag.get("data"))
                .result()
                .orElse(Packed.EMPTY);

        return new SplitPlayState(data);
    }

    // 🔴 Helper to access the data
    public static SplitPlayState get(net.minecraft.server.level.ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                SplitPlayState::load,
                SplitPlayState::new,
                NAME
        );
    }

    public record Packed(List<Group> groups) {
        public static final Packed EMPTY = new Packed(List.of());

        public static final Codec<Packed> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Group.CODEC.listOf().optionalFieldOf("groups", List.of()).forGetter(Packed::groups)
                ).apply(instance, Packed::new)
        );
    }

    public record Group(List<UUID> players, int activeIndex) {
        public static final Codec<Group> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        UUIDUtil.CODEC.listOf().fieldOf("players").forGetter(Group::players),
                        Codec.INT.optionalFieldOf("activeIndex", 0).forGetter(Group::activeIndex)
                ).apply(instance, Group::new)
        );
    }
}