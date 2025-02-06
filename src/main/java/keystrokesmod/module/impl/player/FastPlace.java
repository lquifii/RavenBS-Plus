package keystrokesmod.module.impl.player;

import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class FastPlace extends Module {
    public SliderSetting tickDelay;
    public ButtonSetting blocksOnly, pitchCheck;
    public ButtonSetting disablePearl;

    public FastPlace() {
        super("FastPlace", Module.category.player, 0);
        this.registerSetting(tickDelay = new SliderSetting("Tick delay", 1.0, 1.0, 3.0, 1.0));
        this.registerSetting(blocksOnly = new ButtonSetting("Blocks only", true));
        this.registerSetting(pitchCheck = new ButtonSetting("Pitch check", false));
        this.registerSetting(disablePearl = new ButtonSetting("Disable while holding pearl", true));
        this.closetModule = true;
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (ModuleManager.scaffold.stopFastPlace()) {
            return;
        }
        if (Utils.nullCheck() && mc.inGameHasFocus) {
            if (blocksOnly.isToggled()) {
                ItemStack item = mc.thePlayer.getHeldItem();
                if (item == null || !(item.getItem() instanceof ItemBlock)) {
                    return;
                }
            }
            if (pitchCheck.isToggled() && mc.thePlayer.rotationPitch < 70.0f) {
                return;
            }
            if (disablePearl.isToggled() && mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemEnderPearl) {
                return;
            }
            int delay = (int) tickDelay.getInput();
            if (delay == 0) {
                ((IAccessorMinecraft) mc).setRightClickDelayTimer(0);
            }
            else {
                if (delay == 4) {
                    return;
                }
                if (((IAccessorMinecraft) mc).getRightClickDelayTimer() == 4) {
                    ((IAccessorMinecraft) mc).setRightClickDelayTimer(delay);
                }
            }
        }
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent e) {
        if (!Utils.nullCheck() || ModuleManager.scaffold.stopFastPlace()) {
            return;
        }
        if (pitchCheck.isToggled() && mc.thePlayer.rotationPitch < 70.0f) {
            return;
        }
        if (e.getPacket() instanceof C08PacketPlayerBlockPlacement) {
            C08PacketPlayerBlockPlacement p = (C08PacketPlayerBlockPlacement) e.getPacket();
            if (p.getPlacedBlockDirection() != 255 || p.getStack() == null || !(p.getStack().getItem() instanceof ItemBlock)) {
                return;
            }
            if (Math.random() < 0.7) {
                e.setCanceled(true);
            }
        }
    }
}
