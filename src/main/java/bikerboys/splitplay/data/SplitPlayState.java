package bikerboys.splitplay.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.util.*;
import net.minecraft.util.datafix.*;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.List;
import java.util.UUID;

public class SplitPlayState extends SavedData {


    public static final SavedDataType<SplitPlayState> TYPE = new SavedDataType<>(
            "splitplay_pairs",
            SplitPlayState::new,
            Packed.CODEC.xmap(SplitPlayState::new, SplitPlayState::getData),
            DataFixTypes.LEVEL
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

    public void setData(Packed data) {
        if (!data.equals(this.data)) {
            this.data = data;
            setDirty();
        }
    }

    // ===== API =====

    public List<Pair> getPairs() {
        return data.pairs();
    }

    public void addPair(UUID p1, UUID p2) {
        List<Pair> newPairs = Util.copyAndAdd(data.pairs(), new Pair(p1, p2, true));
        setData(new Packed(newPairs));
    }

    public void clear() {
        setData(Packed.EMPTY);
    }

    public void setActive(UUID p1, UUID p2, boolean player1IsActive) {
        List<Pair> newPairs = data.pairs().stream()
                .map(pair -> (pair.player1().equals(p1) && pair.player2().equals(p2)) ?
                        new Pair(p1, p2, player1IsActive) : pair)
                .toList();
        setData(new Packed(newPairs));
    }

    // ===== DATA RECORD =====

    public record Packed(List<Pair> pairs) {
        public static final Packed EMPTY = new Packed(List.of());

        public static final Codec<Packed> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Pair.CODEC.listOf().optionalFieldOf("pairs", List.of()).forGetter(Packed::pairs)
                ).apply(instance, Packed::new)
        );
    }

    public record Pair(UUID player1, UUID player2, boolean player1IsActive) {
        public static final Codec<Pair> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        UUIDUtil.CODEC.fieldOf("p1").forGetter(Pair::player1),
                        UUIDUtil.CODEC.fieldOf("p2").forGetter(Pair::player2),
                        Codec.BOOL.optionalFieldOf("p1Active", true).forGetter(Pair::player1IsActive)
                ).apply(instance, Pair::new)
        );
    }
}