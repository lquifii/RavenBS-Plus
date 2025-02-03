package keystrokesmod.module.impl.movement;

import keystrokesmod.clickgui.ClickGui;
import keystrokesmod.event.JumpEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.concurrent.ConcurrentLinkedQueue;

public class InvMove extends Module {
    public SliderSetting inventory;
    private SliderSetting chestAndOthers;
    private SliderSetting motion;
    private ButtonSetting modifyMotionPost;
    private ButtonSetting slowWhenNecessary;
    private ButtonSetting allowJumping;
    private ButtonSetting allowSprinting;
    public ButtonSetting invManagerOnly;
    private ButtonSetting allowRotating;
    public int ticks;
    public boolean setMotion;
    private String[] inventoryModes = new String[] { "Disabled", "Vanilla", "Blink", "Close" };
    private String[] chestAndOtherModes = new String[] { "Disabled", "Vanilla", "Blink" };
    private ConcurrentLinkedQueue<Packet> blinkedPackets = new ConcurrentLinkedQueue<>();

    public InvMove() {
        super("InvMove", Module.category.movement);
        this.registerSetting(inventory = new SliderSetting("Inventory", 1, inventoryModes));
        this.registerSetting(chestAndOthers = new SliderSetting("Chest & others", 1, chestAndOtherModes));
        this.registerSetting(motion = new SliderSetting("Motion", "x", 1, 0.05, 1, 0.01));
        this.registerSetting(modifyMotionPost = new ButtonSetting("Modify motion after click", false));
        this.registerSetting(slowWhenNecessary = new ButtonSetting("Slow motion when necessary", false));
        this.registerSetting(allowJumping = new ButtonSetting("Allow jumping", true));
        this.registerSetting(allowRotating = new ButtonSetting("Allow rotating", true));
        this.registerSetting(allowSprinting = new ButtonSetting("Allow sprinting", true));
        this.registerSetting(invManagerOnly = new ButtonSetting("Only with inventory manager", false));
    }

    public void onDisable() {
        reset();
        releasePackets();
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (invManagerOnly.isToggled() && !ModuleManager.invManager.isEnabled()) {
            return;
        }
        if (!guiCheck()) {
            reset();
            return;
        }
        if (!(mc.currentScreen instanceof ClickGui)) {
            if (setMotion) {
                if (++ticks == 10) {
                    ticks = 0;
                    setMotion = false;
                }
            }

            if (setMotion && (motion.getInput() != 1 || (slowWhenNecessary.isToggled()))) {
                final int speedAmplifier = Utils.getSpeedAmplifier();
                double slowedMotion = 0.65;
                switch (speedAmplifier) {
                    case 1:
                        slowedMotion = 0.615;
                        break;
                    case 2:
                        slowedMotion = 0.568;
                        break;
                }
                Utils.setSpeed(Utils.getHorizontalSpeed() * (slowWhenNecessary.isToggled() ? slowedMotion : motion.getInput()));
            }
        }
        else {
            reset();
        }

        KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), isBindDown(mc.gameSettings.keyBindForward));
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), isBindDown(mc.gameSettings.keyBindBack));
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), isBindDown(mc.gameSettings.keyBindRight));
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), isBindDown(mc.gameSettings.keyBindLeft));
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), Utils.jumpDown());
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), isBindDown(mc.gameSettings.keyBindSprint));
        boolean foodLvlMet = (float)mc.thePlayer.getFoodStats().getFoodLevel() > 6.0F || mc.thePlayer.capabilities.allowFlying; // from mc
        if (((isBindDown(mc.gameSettings.keyBindSprint) || ModuleManager.sprint.isEnabled()) && mc.thePlayer.movementInput.moveForward >= 0.8F && foodLvlMet && !mc.thePlayer.isSprinting()) && allowSprinting.isToggled()) {
            mc.thePlayer.setSprinting(true);
        }
        if (!allowSprinting.isToggled()) {
            mc.thePlayer.setSprinting(false);
        }
        if (allowRotating.isToggled()) {
            if (Keyboard.isKeyDown(208) && mc.thePlayer.rotationPitch < 90.0F) {
                mc.thePlayer.rotationPitch += 6.0F;
            }
            if (Keyboard.isKeyDown(200) && mc.thePlayer.rotationPitch > -90.0F) {
                mc.thePlayer.rotationPitch -= 6.0F;
            }
            if (Keyboard.isKeyDown(205)) {
                mc.thePlayer.rotationYaw += 6.0F;
            }
            if (Keyboard.isKeyDown(203)) {
                mc.thePlayer.rotationYaw -= 6.0F;
            }
        }
    }

    @SubscribeEvent
    public void onJump(JumpEvent e) {
        if (!allowJumping.isToggled() && mc.currentScreen != null && !(mc.currentScreen instanceof ClickGui)) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent e) {
        if (e.getPacket() instanceof C0EPacketClickWindow) {
            if (modifyMotionPost.isToggled() || (slowWhenNecessary.isToggled() && !canBlink())) {
                setMotion = true;
                ticks = 0;
            }
            if (canBlink()) {
                blinkedPackets.add(e.getPacket());
                e.setCanceled(true);
            }
        }
        else if (e.getPacket() instanceof C0DPacketCloseWindow) {
            if (canBlink()) {
                if (inventory.getInput() == 3 && ModuleManager.invManager.isEnabled()) {
                    PacketUtils.sendPacketNoEvent(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
                }
                releasePackets();
            }
        }
    }

    private void reset() {
        ticks = 0;
        setMotion = false;
    }

    private boolean guiCheck() {
        if (mc.currentScreen == null) {
            return false;
        }
        if (Settings.inInventory()) {
            if (inventory.getInput() == 0) {
                return false;
            }
        }
        else if ((chestAndOthers.getInput() == 0 && !(mc.currentScreen instanceof ClickGui)) || (mc.currentScreen instanceof GuiChat)) {
            return false;
        }
        return true;
    }

    private boolean canBlink() {
        if (mc.currentScreen == null && inventory.getInput() != 3) {
            return false;
        }
        else if ((mc.currentScreen instanceof GuiInventory && inventory.getInput() == 2) || (inventory.getInput() == 3 && mc.currentScreen == null)) {
            return true;
        }
        else if (chestAndOthers.getInput() == 2 && !(mc.currentScreen instanceof ClickGui) && !(mc.currentScreen instanceof GuiChat)) {
            return true;
        }
        return false;
    }

    private void releasePackets() {
        synchronized (blinkedPackets) {
            for (Packet packet : blinkedPackets) {
                PacketUtils.sendPacketNoEvent(packet);
            }
        }
        blinkedPackets.clear();
    }

    private boolean isBindDown(KeyBinding keyBinding) {
        try {
            return Keyboard.isKeyDown(keyBinding.getKeyCode());
        }
        catch (IndexOutOfBoundsException e) {
            return Mouse.isButtonDown(100 + keyBinding.getKeyCode());
        }
    }
}