package keystrokesmod.mixin.impl.render;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.utility.RotationUtils;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(RenderManager.class)
public class MixinRenderManager {
    @Unique
    private float cachedPrevRotationPitch;
    @Unique
    private float cachedRotationPitch;

    @Inject(method = "renderEntityStatic", at = @At("HEAD"))
    public void renderEntityStaticPre(final Entity entity, final float n, final boolean b, final CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (entity instanceof EntityPlayerSP && PreMotionEvent.setRenderYaw()) {
            final EntityPlayerSP player = (EntityPlayerSP)entity;
            cachedRotationPitch = player.rotationPitch;
            cachedPrevRotationPitch = player.prevRotationPitch;
            player.prevRotationPitch = RotationUtils.prevRenderPitch;
            player.rotationPitch = RotationUtils.renderPitch;
        }
    }

    @Inject(method = "renderEntityStatic", at = @At("RETURN"))
    public void renderEntityStaticPost(final Entity entity, final float n, final boolean b, final CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (entity instanceof EntityPlayerSP && PreMotionEvent.setRenderYaw()) {
            final EntityPlayerSP player = (EntityPlayerSP)entity;
            player.prevRotationPitch = this.cachedPrevRotationPitch;
            player.rotationPitch = this.cachedRotationPitch;
        }
    }
}
