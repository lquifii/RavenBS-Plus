package keystrokesmod.mixin.impl.accessor;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@SideOnly(Side.CLIENT)
@Mixin(value =EntityPlayer.class, priority = 999)
public interface IAccessorEntityPlayer {
    @Accessor("itemInUseCount")
    void setItemInUseCount(int count);
}
