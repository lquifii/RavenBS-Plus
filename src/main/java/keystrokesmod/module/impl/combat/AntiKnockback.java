package keystrokesmod.module.impl.combat;

import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.movement.LongJump;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

public class AntiKnockback extends Module {
    private SliderSetting horizontal;
    private SliderSetting vertical;
    private ButtonSetting disableInLobby;
    private ButtonSetting cancelBurning;
    private ButtonSetting cancelExplosion;
    private ButtonSetting cancelWhileFalling;
    private ButtonSetting cancelOffGround;
    private SliderSetting boostMultiplier;
    private ButtonSetting boostWithLMB;

    public boolean disable;

    public AntiKnockback() {
        super("AntiKnockback", category.combat);
        this.registerSetting(new DescriptionSetting("Overrides Velocity."));
        this.registerSetting(horizontal = new SliderSetting("Horizontal", 0.0, 0.0, 100.0, 1.0));
        this.registerSetting(vertical = new SliderSetting("Vertical", 0.0, 0.0, 100.0, 1.0));
        this.registerSetting(disableInLobby = new ButtonSetting("Disable in lobby", false));
        this.registerSetting(cancelBurning = new ButtonSetting("Cancel burning", true));
        this.registerSetting(cancelExplosion = new ButtonSetting("Cancel explosion", true));
        this.registerSetting(cancelWhileFalling = new ButtonSetting("Cancel while falling", true));
        this.registerSetting(cancelOffGround = new ButtonSetting("Cancel off ground", true));
        this.registerSetting(boostMultiplier = new SliderSetting("Damage boost", "x", 1, 0.5, 2.5, 0.01));
        this.registerSetting(boostWithLMB = new ButtonSetting("Boost with LMB", false));
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        if (!Utils.nullCheck() || LongJump.stopVelocity || e.isCanceled()) {
            return;
        }
        if (e.getPacket() instanceof S12PacketEntityVelocity) {
            if (((S12PacketEntityVelocity) e.getPacket()).getEntityID() == mc.thePlayer.getEntityId() && !disable) {
                if (!cancelBurning.isToggled() && mc.thePlayer.isBurning()) {
                    return;
                }
                if (disableInLobby.isToggled() && Utils.isLobby()) {
                    return;
                }
                e.setCanceled(true);
                if (cancel()) {
                    return;
                }
                if (cancelConditions()) {
                    return;
                }
                S12PacketEntityVelocity s12PacketEntityVelocity = (S12PacketEntityVelocity) e.getPacket();
                if (horizontal.getInput() == 0 && vertical.getInput() > 0) {
                    mc.thePlayer.motionY = ((double) s12PacketEntityVelocity.getMotionY() / 8000) * vertical.getInput() / 100.0;
                }
                else if (horizontal.getInput() > 0 && vertical.getInput() == 0) {
                    mc.thePlayer.motionX = ((double) s12PacketEntityVelocity.getMotionX() / 8000) * horizontal.getInput() / 100.0;
                    mc.thePlayer.motionZ = ((double) s12PacketEntityVelocity.getMotionZ() / 8000) * horizontal.getInput() / 100.0;
                }
                else {
                    mc.thePlayer.motionX = ((double) s12PacketEntityVelocity.getMotionX() / 8000) * horizontal.getInput() / 100.0;
                    mc.thePlayer.motionY = ((double) s12PacketEntityVelocity.getMotionY() / 8000) * vertical.getInput() / 100.0;
                    mc.thePlayer.motionZ = ((double) s12PacketEntityVelocity.getMotionZ() / 8000) * horizontal.getInput() / 100.0;
                }
                if (boostMultiplier.getInput() != 1) {
                    if (boostWithLMB.isToggled() && !Mouse.isButtonDown(0)) {
                        return;
                    }
                    Utils.setSpeed(Utils.getHorizontalSpeed() * boostMultiplier.getInput());
                }
            }
        }
        else if (e.getPacket() instanceof S27PacketExplosion && !disable) {
            if (disableInLobby.isToggled() && Utils.isLobby()) {
                return;
            }
            e.setCanceled(true);
            if (cancelExplosion.isToggled() || cancel()) {
                return;
            }
            if (cancelConditions()) {
                return;
            }
            S27PacketExplosion s27PacketExplosion = (S27PacketExplosion) e.getPacket();
            if (horizontal.getInput() == 0 && vertical.getInput() > 0) {
                mc.thePlayer.motionY += s27PacketExplosion.func_149144_d() * vertical.getInput() / 100.0;
            }
            else if (horizontal.getInput() > 0 && vertical.getInput() == 0) {
                mc.thePlayer.motionX += s27PacketExplosion.func_149149_c() * horizontal.getInput() / 100.0;
                mc.thePlayer.motionZ += s27PacketExplosion.func_149147_e() * horizontal.getInput() / 100.0;
            }
            else {
                mc.thePlayer.motionX += s27PacketExplosion.func_149149_c() * horizontal.getInput() / 100.0;
                mc.thePlayer.motionY += s27PacketExplosion.func_149144_d() * vertical.getInput() / 100.0;
                mc.thePlayer.motionZ += s27PacketExplosion.func_149147_e() * horizontal.getInput() / 100.0;
            }
        }
    }

    private boolean cancel() {
        return (vertical.getInput() == 0 && horizontal.getInput() == 0) || ModuleManager.bedAura.cancelKnockback();
    }

    @Override
    public String getInfo() {
        return (int) horizontal.getInput() + "%" + " " + (int) vertical.getInput() + "%";
    }

    private boolean cancelConditions() {
        if (mc.thePlayer != null) {
            if (cancelWhileFalling.isToggled() && mc.thePlayer.fallDistance > 0) {
                return true;
            }
            if (cancelOffGround.isToggled() && !mc.thePlayer.onGround) {
                return true;
            }
        }
        return false;
    }
}