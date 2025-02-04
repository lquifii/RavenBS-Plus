package keystrokesmod.utility;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.impl.render.HUD;
import net.minecraft.client.Minecraft;
import keystrokesmod.module.ModuleManager;
import net.minecraft.item.ItemFireball;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

import java.util.Iterator;
import java.util.Map;

public class ModHelper {
    private final Minecraft mc;
    public static int inAirTicks;
    public static int groundTicks;
    private int unTargetTicks;
    public static boolean threwFireball;
    public static boolean threwFireballLow;
    public static long MAX_EXPLOSION_DIST_SQ = 10;
    private long FIREBALL_TIMEOUT = 500L;
    private long fireballTime = 0;

    public ModHelper(Minecraft mc) {
        this.mc = mc;
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {
        if (inAirTicks <= 20) {
            inAirTicks = mc.thePlayer.onGround ? 0 : ++inAirTicks;
        } else {
            inAirTicks = 19;
        }
        groundTicks = !mc.thePlayer.onGround ? 0 : ++groundTicks;

        int simpleY = (int) Math.round((e.posY % 1) * 10000);

        // 7 tick needs to always finish the motion or itll lag back
        if (!ModuleManager.bHop.isEnabled() && ModuleManager.bHop.mode.getInput() == 3 && ModuleManager.bHop.didMove) {
            if (mc.thePlayer.hurtTime == 0) {
                switch (simpleY) {
                    case 4200:
                        mc.thePlayer.motionY = 0.39;
                        break;
                    case 1138:
                        mc.thePlayer.motionY = mc.thePlayer.motionY - 0.13;
                        ModuleManager.bHop.lowhop = false;
                        ModuleManager.bHop.didMove = false;
                        break;
                }
            }
        }
        if (ModuleManager.bHop.setRotation) {
            if (KillAura.target == null && !ModuleManager.scaffold.isEnabled && ModuleManager.bHop.rotateYaw.isToggled()) {
                float yaw = mc.thePlayer.rotationYaw - 55;
                e.setYaw(yaw);
            }
            if (mc.thePlayer.onGround) {
                ModuleManager.bHop.setRotation = false;
            }
        }
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (e.getPacket() instanceof C08PacketPlayerBlockPlacement && mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemFireball) {
            if (Mouse.isButtonDown(1)) {
                fireballTime = System.currentTimeMillis();
                threwFireball = true;
                if (mc.thePlayer.rotationPitch > 50F) {
                    threwFireballLow = true;
                }
            }
        }
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (fireballTime > 0 && (System.currentTimeMillis() - fireballTime) > FIREBALL_TIMEOUT / 3) {
            threwFireballLow = false;
            ModuleManager.velocity.disable = false;
            ModuleManager.antiKnockback.disable = false;
        }

        if (fireballTime > 0 && (System.currentTimeMillis() - fireballTime) > FIREBALL_TIMEOUT) {
            threwFireball = threwFireballLow = false;
            fireballTime = 0;
            ModuleManager.velocity.disable = false;
            ModuleManager.antiKnockback.disable = false;
        }

        if (ModuleManager.killAura.stoppedTargeting) {
            if (++unTargetTicks >= 2) {
                unTargetTicks = 0;
                ModuleManager.killAura.stoppedTargeting = false;
            }
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        if (!Utils.nullCheck() || e.isCanceled() || ModuleManager.bedAura.cancelKnockback()) {
            return;
        }
        if (e.getPacket() instanceof S27PacketExplosion) {
            S27PacketExplosion s27 = (S27PacketExplosion) e.getPacket();
            if (threwFireball) {
                if ((mc.thePlayer.getPosition().distanceSq(s27.getX(), s27.getY(), s27.getZ()) <= MAX_EXPLOSION_DIST_SQ)) {
                    ModuleManager.velocity.disable = false;
                    ModuleManager.antiKnockback.disable = false;
                    threwFireball = false;
                    e.setCanceled(false);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (!Utils.nullCheck() || !ModuleManager.scaffold.highlightBlocks.isToggled() || ModuleManager.scaffold.highlight.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<BlockPos, Timer>> iterator = ModuleManager.scaffold.highlight.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Timer> entry = iterator.next();
            if (entry.getValue() == null) {
                entry.setValue(new Timer(750));
                entry.getValue().start();
            }
            int alpha = entry.getValue() == null ? 210 : 210 - entry.getValue().getValueInt(0, 210, 1);
            if (alpha == 0) {
                iterator.remove();
                continue;
            }
            RenderUtils.renderBlock(entry.getKey(), Utils.mergeAlpha(Theme.getGradient((int) HUD.theme.getInput(), 0), alpha), true, false);
        }
    }
}
