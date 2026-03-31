package bikerboys.splitplay.mixin;

import bikerboys.splitplay.*;
import com.mojang.authlib.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.level.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player {

    @Shadow
    private ChatVisiblity chatVisibility;

    public ServerPlayerMixin(Level level, BlockPos blockPos, float f, GameProfile gameProfile) {
        super(level, blockPos, f, gameProfile);
    }


    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        ServerPlayer serverPlayer = (ServerPlayer)(Object)this;

        if (SplitPlay.isInactivePlayer(serverPlayer)) {
            if (!serverPlayer.isSpectator()) {}
            chatVisibility = ChatVisiblity.HIDDEN;
        } else {
            chatVisibility = ChatVisiblity.FULL;
        }



    }
}
