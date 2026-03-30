package bikerboys.splitplay.networking;

import bikerboys.splitplay.*;
import net.minecraft.core.*;
import net.minecraft.network.*;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.*;
import net.minecraft.resources.*;

public record UpdateNumberPacket(String text) implements CustomPacketPayload {
    public static final Identifier SUMMON_LIGHTNING_PAYLOAD_ID = Identifier.fromNamespaceAndPath(SplitPlay.MOD_ID, "updatenumber");
    public static final CustomPacketPayload.Type<UpdateNumberPacket> ID = new CustomPacketPayload.Type<>(SUMMON_LIGHTNING_PAYLOAD_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateNumberPacket> CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, UpdateNumberPacket::text, UpdateNumberPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
