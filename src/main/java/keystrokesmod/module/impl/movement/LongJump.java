package keystrokesmod.module.impl.movement;

import keystrokesmod.event.PrePlayerInputEvent;
import keystrokesmod.event.*;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.KeySetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.ModHelper;
import keystrokesmod.utility.Utils;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.*;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

public class LongJump extends Module {
    private SliderSetting mode;

    private SliderSetting boostSetting;
    private SliderSetting verticalMotion;
    private SliderSetting motionDecay;

    private ButtonSetting manual;
    private KeySetting disableKey;

    private ButtonSetting allowStrafe;
    private ButtonSetting invertYaw;
    private ButtonSetting stopMovement;
    private ButtonSetting hideExplosion;

    private KeySetting temporaryFlightKey;

    public String[] modes = new String[]{"Float", "Boost"};

    private boolean manualWasOn;

    private float yaw;
    private float pitch;

    private boolean notMoving;
    private boolean enabled;
    public boolean function;

    private int boostTicks;
    private int lastSlot = -1;
    private int stopTime;
    private int rotateTick;
    private int motionDecayVal;

    private long fireballTime;
    private long MAX_EXPLOSION_DIST_SQ = 9;
    private long FIREBALL_TIMEOUT = 750L;

    public static boolean stopVelocity;
    public static boolean stopModules;
    public static boolean slotReset;
    public static int slotResetTicks;

    public LongJump() {
        super("Long Jump", category.movement);
        this.registerSetting(mode = new SliderSetting("Mode", 0, modes));

        this.registerSetting(manual = new ButtonSetting("Manual", false));
        this.registerSetting(disableKey = new KeySetting("Disable key", Keyboard.KEY_SPACE));

        this.registerSetting(boostSetting = new SliderSetting("Horizontal boost", 1.7, 0.0, 2.0, 0.05));
        this.registerSetting(verticalMotion = new SliderSetting("Vertical motion", 0, 0.4, 0.9, 0.01));
        this.registerSetting(motionDecay = new SliderSetting("Motion decay", 17, 1, 40, 1));
        this.registerSetting(allowStrafe = new ButtonSetting("Allow strafe", false));
        this.registerSetting(invertYaw = new ButtonSetting("Invert yaw", true));
        this.registerSetting(stopMovement = new ButtonSetting("Stop movement", false));
        this.registerSetting(hideExplosion = new ButtonSetting("Hide explosion", false));

        this.registerSetting(temporaryFlightKey = new KeySetting("Vertical key", Keyboard.KEY_SPACE));
    }

    public void guiUpdate() {
        this.disableKey.setVisible(manual.isToggled(), this);

        this.verticalMotion.setVisible(mode.getInput() == 0, this);
        this.motionDecay.setVisible(mode.getInput() == 0, this);
        this.temporaryFlightKey.setVisible(mode.getInput() == 0, this);
    }

    public void onEnable() {
        if (!manual.isToggled()) {
            enabled();
        }
    }

    public void onDisable() {
        disabled();
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (manual.isToggled()) {
            manualWasOn = true;
        }
        else {
            if (manualWasOn) {
                disabled();
            }
            manualWasOn = false;
        }

        if (manual.isToggled() && disableKey.isPressed() && Utils.jumpDown()) {
            function = false;
            disabled();
        }

        if (!function) {
            if (manual.isToggled() && !enabled) {
                if (ModHelper.threwFireballLow) {
                    ModuleManager.velocity.disable = true;
                    ModuleManager.antiKnockback.disable = true;
                    enabled();
                }
            }
            return;
        }

        if (enabled) {
            if (!Utils.isMoving()) notMoving = true;
            if (boostSetting.getInput() == 0 && verticalMotion.getInput() == 0) {
                Utils.sendMessage("&cValues are set to 0!");
                disabled();
                return;
            }
            int fireballSlot = setupFireballSlot(true);
            if (fireballSlot != -1) {
                if (!manual.isToggled()) {
                    lastSlot = mc.thePlayer.inventory.currentItem;
                    mc.thePlayer.inventory.currentItem = fireballSlot;
                }
                //("Set fireball slot");
                rotateTick = 1;
                if (stopMovement.isToggled()) {
                    stopTime = 1;
                }
            } // auto disables if -1
            enabled = false;
        }

        if (notMoving) {
            motionDecayVal = 21;
        } else {
            motionDecayVal = (int) motionDecay.getInput();
        }
        if (stopTime == -1 && ++boostTicks > (!temporaryFlightKey() ? 33/*flat motion ticks*/ : (!notMoving ? 32/*normal motion ticks*/ : 33/*vertical motion ticks*/))) {
            disabled();
            return;
        }

        if (fireballTime > 0 && (System.currentTimeMillis() - fireballTime) > FIREBALL_TIMEOUT) {
            Utils.sendMessage("&cFireball timed out.");
            disabled();
            return;
        }
        if (boostTicks > 0) {
            if (mode.getInput() == 0) {
                modifyVertical(); // has to be onPreUpdate
            }
            //Utils.sendMessage("Modifying vertical");
            if (allowStrafe.isToggled() && boostTicks < 32) {
                Utils.setSpeed(Utils.getHorizontalSpeed(mc.thePlayer));
                //Utils.sendMessage("Speed");
            }
        }

        if (stopMovement.isToggled() && !notMoving) {
            if (stopTime > 0) {
                ++stopTime;
            }
        }

        if (mc.thePlayer.onGround && boostTicks > 2) {
            disabled();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPreMotion(PreMotionEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (rotateTick == 1) {
            if ((invertYaw.isToggled() || stopMovement.isToggled()) && !notMoving) {
                if (!stopMovement.isToggled()) {
                    yaw = mc.thePlayer.rotationYaw - 180f;
                    pitch = 90f;
                } else {
                    yaw = mc.thePlayer.rotationYaw - 180f;
                    pitch = 66.3f;//(float) pitchVal.getInput();
                }
            } else {
                yaw = mc.thePlayer.rotationYaw;
                pitch = 90f;
            }
            e.setRotations(yaw, pitch);
        }
        if (rotateTick > 0 && ++rotateTick >= 3) {
            rotateTick = 0;
            int fireballSlot = setupFireballSlot(false);
            if (fireballSlot != -1) {
                if (!manual.isToggled()) {
                    mc.thePlayer.inventory.currentItem = fireballSlot; // we are probably already on the slot but make sure
                    fireballTime = System.currentTimeMillis();
                    ((IAccessorMinecraft) mc).callRightClickMouse();
                }
                mc.thePlayer.swingItem();
                mc.getItemRenderer().resetEquippedProgress();
                stopVelocity = true;
                //Utils.sendMessage("Right click");
            }
        }
        if (boostTicks == 1) {
            if (invertYaw.isToggled()) {
                //client.setMotion(client.getMotion().x, client.getMotion().y + 0.035d, client.getMotion().z);
            }
            modifyHorizontal();
            stopVelocity = false;
            if (!manual.isToggled() && !allowStrafe.isToggled() && mode.getInput() == 1) {
                disabled();
            }
        }

    }

    @SubscribeEvent(priority = EventPriority.LOWEST) // called last in order to apply fix
    public void onMoveInput(PrePlayerInputEvent e) {
        if (!function) {
            return;
        }
        mc.thePlayer.movementInput.jump = false;
        if (rotateTick > 0 || fireballTime > 0) {
            if (Utils.isMoving()) e.setForward(1);
            e.setStrafe(0);
        }
        if (notMoving && boostTicks < 3) {
            e.setForward(0);
            e.setStrafe(0);
            Utils.setSpeed(0);
        }
        if (stopMovement.isToggled() && !notMoving && stopTime >= 1) {
            e.setForward(0);
            e.setStrafe(0);
            Utils.setSpeed(0);
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        if (!function) {
            return;
        }
        Packet packet = e.getPacket();
        if (packet instanceof S27PacketExplosion) {
            S27PacketExplosion s27 = (S27PacketExplosion) packet;
            if (fireballTime == 0 || mc.thePlayer.getPosition().distanceSq(s27.getX(), s27.getY(), s27.getZ()) > MAX_EXPLOSION_DIST_SQ) {
                e.setCanceled(true);
                //Utils.sendMessage("0 fb time / out of dist");
            }

            stopTime = -1;
            fireballTime = 0;
            resetSlot();
            boostTicks = 0; // +1 on next pre update
            //Utils.sendMessage("set start vals");

            //client.print(client.getPlayer().getTicksExisted() + " s27 " + boostTicks + " " + client.getPlayer().getHurtTime() + " " + client.getPlayer().getSpeed());
        } else if (packet instanceof S08PacketPlayerPosLook) {
            Utils.sendMessage("&cReceived setback, disabling.");
            disabled();
        }

        if (hideExplosion.isToggled() && fireballTime != 0 && (packet instanceof S0EPacketSpawnObject || packet instanceof S2APacketParticles || packet instanceof S29PacketSoundEffect)) {
            e.setCanceled(true);
        }
    }

    private int getFireballSlot() {
        int n = -1;
        for (int i = 0; i < 9; ++i) {
            final ItemStack getStackInSlot = mc.thePlayer.inventory.getStackInSlot(i);
            if (getStackInSlot != null && getStackInSlot.getItem() == Items.fire_charge) {
                n = i;
                break;
            }
        }
        return n;
    }

    private void enabled() {
        slotReset = false;
        slotResetTicks = 0;
        enabled = function = true;
        ModuleManager.bHop.disable();

        stopModules = true;
    }

    private void disabled() {
        fireballTime = rotateTick = stopTime = 0;
        boostTicks = -1;
        resetSlot();
        enabled = function = notMoving = stopVelocity = stopModules = false;
        if (!manual.isToggled()) {
            disable();
        }
    }

    private int setupFireballSlot(boolean pre) {
        // only cancel bad packet right click on the tick we are sending it
        int fireballSlot = getFireballSlot();
        if (fireballSlot == -1) {
            Utils.sendMessage("&cFireball not found.");
            disabled();
        } else if (ModuleManager.scaffold.isEnabled || (pre && Utils.distanceToGround(mc.thePlayer) > 3)/* || (!pre && !PacketUtil.canRightClickItem())*/) { //needs porting
            Utils.sendMessage("&cCan't throw fireball right now.");
            disabled();
            fireballSlot = -1;
        }
        return fireballSlot;
    }

    private void resetSlot() {
        if (lastSlot != -1 && !manual.isToggled()) {
            mc.thePlayer.inventory.currentItem = lastSlot;
            lastSlot = -1;
        }
        slotReset = true;
    }

    private int getSpeedLevel() {
        for (PotionEffect potionEffect : mc.thePlayer.getActivePotionEffects()) {
            if (potionEffect.getEffectName().equals("potion.moveSpeed")) {
                return potionEffect.getAmplifier() + 1;
            }
            return 0;
        }
        return 0;
    }

    // only apply horizontal boost once
    private void modifyHorizontal() {
        if (boostSetting.getInput() != 0) {
            double speed = boostSetting.getInput() - Utils.randomizeDouble(0.0001, 0);
            if (Utils.isMoving()) {
                Utils.setSpeed(speed);
            }
        }
    }

    private void modifyVertical() {
        if (verticalMotion.getInput() != 0) {
            double ver = ((!notMoving ? verticalMotion.getInput() : 1.16 /*vertical*/) * (1.0 / (1.0 + (0.05 * getSpeedLevel())))) + Utils.randomizeDouble(0.0001, 0.1);
            double decay = motionDecay.getInput() / 1000;
            if (boostTicks > 1 && !temporaryFlightKey()) {
                if (boostTicks > 1 || boostTicks <= (!notMoving ? 32/*horizontal motion ticks*/ : 33/*vertical motion ticks*/)) {
                    mc.thePlayer.motionY = Utils.randomizeDouble(0.0101, 0.01);
                }
            } else {
                if (boostTicks >= 1 && boostTicks <= (!notMoving ? 32/*horizontal motion ticks*/ : 33/*vertical motion ticks*/)) {
                    mc.thePlayer.motionY = ver - boostTicks * decay;
                } else if (boostTicks >= (!notMoving ? 32/*horizontal motion ticks*/ : 33/*vertical motion ticks*/) + 3) {
                    mc.thePlayer.motionY = mc.thePlayer.motionY + 0.028;
                    Utils.sendMessage("?");
                }
            }
        }
    }

    private boolean temporaryFlightKey() {
        if (notMoving) return true;
        return temporaryFlightKey.isPressed();
    }
}