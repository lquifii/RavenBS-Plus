package keystrokesmod.mixin.impl.render;

import keystrokesmod.utility.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.client.renderer.entity.RenderPlayer;

@Mixin(RenderPlayer.class)
public class MixinRenderPlayer {
    @Redirect(method = "setModelVisibilities(Lnet/minecraft/client/entity/AbstractClientPlayer;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/InventoryPlayer;getCurrentItem()Lnet/minecraft/item/ItemStack;"))
    private ItemStack redirectGetCurrentItem(InventoryPlayer inventory) {
        if (Minecraft.getMinecraft().gameSettings.thirdPersonView == 0) {
            return Utils.getSpoofedItem(inventory.getCurrentItem());
        }
        else {
            return inventory.getCurrentItem();
        }
    }
}