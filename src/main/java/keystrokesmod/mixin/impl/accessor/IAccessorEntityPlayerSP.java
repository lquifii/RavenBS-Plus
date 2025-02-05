package keystrokesmod.mixin.impl.accessor;

import net.minecraftforge.fml.relauncher.*;
import org.spongepowered.asm.mixin.*;
import net.minecraft.client.entity.*;
import org.spongepowered.asm.mixin.gen.*;

@SideOnly(Side.CLIENT)
@Mixin(value = EntityPlayerSP.class, priority = 999)
public interface IAccessorEntityPlayerSP {
    @Accessor("lastReportedPosX")
    double getLastReportedPosX();

    @Accessor("lastReportedPosY")
    double getLastReportedPosY();

    @Accessor("lastReportedPosZ")
    double getLastReportedPosZ();

    @Accessor("lastReportedYaw")
    float getLastReportedYaw();
}