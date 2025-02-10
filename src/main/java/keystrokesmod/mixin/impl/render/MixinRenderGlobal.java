package keystrokesmod.mixin.impl.render;

import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.world.AntiBot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SideOnly(Side.CLIENT)
@Mixin(RenderGlobal.class)
public class MixinRenderGlobal { // credit: pablolnmak
    @Shadow
    @Final
    private Minecraft mc;

    @Unique
    private boolean shouldRender() {
        return ModuleManager.playerESP != null && ModuleManager.playerESP.isEnabled() && ModuleManager.playerESP.outline.isToggled();
    }

    @Redirect(method = "isRenderEntityOutlines", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;isSpectator()Z"))
    private boolean forceIsSpectator(EntityPlayerSP instance) {
        if (shouldRender()) {
            return true;
        }
        else {
            return instance.isSpectator();
        }
    }

    @Redirect(method = "isRenderEntityOutlines", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/settings/KeyBinding;isKeyDown()Z"))
    private boolean forceIsKeyDown(KeyBinding instance) {
        if (shouldRender()) {
            return true;
        }
        else {
            return instance.isKeyDown();
        }
    }

    @Redirect(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;shouldRenderInPass(I)Z", ordinal = 1))
    private boolean forceShouldRenderInPass(Entity instance, int pass, Entity renderViewEntity, ICamera camera, float partialTicks) {
        if (pass == 1) {
            return true;
        }
        else {
            return instance.shouldRenderInPass(pass);
        }
    }

    @Redirect(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isInRangeToRender3d(DDD)Z", ordinal = 1))
    private boolean forceIsInRangeToRender(Entity instance, double x, double y, double z, Entity renderViewEntity, ICamera camera, float partialTicks) {
        return instance.isInRangeToRender3d(x, y, z) && isOutlineActive(instance, renderViewEntity, camera);
    }

    @Unique
    private boolean isOutlineActive(Entity entityIn, Entity viewer, ICamera camera) {
        boolean flag = viewer instanceof EntityLivingBase && ((EntityLivingBase) viewer).isPlayerSleeping();
        if (entityIn == viewer && this.mc.gameSettings.thirdPersonView == 0 && !flag) {
            return false;
        }
        else if (shouldRender()) {
            return (entityIn != viewer && !AntiBot.isBot(entityIn)) || (entityIn == viewer && ModuleManager.playerESP.renderSelf.isToggled());
        }
        else {
            if (this.mc.thePlayer.isSpectator() && this.mc.gameSettings.keyBindSpectatorOutlines.isKeyDown() && entityIn instanceof EntityPlayer) {
                return entityIn.ignoreFrustumCheck || camera.isBoundingBoxInFrustum(entityIn.getEntityBoundingBox()) || entityIn.isRiding();
            }
            else {
                return false;
            }
        }
    }
}