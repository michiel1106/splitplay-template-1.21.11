package bikerboys.splitplay.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

public class SplitPlayState extends SavedData {

    public static final SavedDataType<SplitPlayState> TYPE = new SavedDataType<>(
            "splitplay_groups",
            SplitPlayState::new,
            Packed.CODEC.xmap(SplitPlayState::new, SplitPlayState::getData),
            null
    );

    private Packed data;

    private SplitPlayState() {
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