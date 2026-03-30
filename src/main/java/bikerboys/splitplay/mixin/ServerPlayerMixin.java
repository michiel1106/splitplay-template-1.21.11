package bikerboys.splitplay.mixin;

import bikerboys.splitplay.*;
import com.mojang.authlib.*;
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

    @Shadow
    public abstract ClientInformation clientInformation();

    public ServerPlayerMixin(Level level, GameProfile gameProfile) {
        super(level, gameProfile);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        ServerPlayer serverPlayer = (ServerPlayer)(Object)this;

        if (SplitPlay.isInactivePlayer(serverPlayer)) {
            chatVisibility = ChatVisiblity.HIDDEN;
        } else {
            chatVisibility = ChatVisiblity.FULL;
        }



    }
}
