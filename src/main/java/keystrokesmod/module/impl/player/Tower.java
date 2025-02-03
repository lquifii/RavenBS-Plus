package keystrokesmod.module.impl.player;

import keystrokesmod.event.PostPlayerInputEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.network.play.client.*;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Tower extends Module {
    private SliderSetting towerMove;
    private SliderSetting verticalTower;
    private SliderSetting slowedMotion;
    private SliderSetting slowedTicks;
    private ButtonSetting disableWhileHurt;
    private ButtonSetting disableInLiquid;
    private ButtonSetting disableWhileCollided;

    private String[] towerMoveModes = new String[]{"None", "Vanilla", "Low", "Edge", "2.5 tick"};
    private String[] verticalTowerModes = new String[]{"None", "Vanilla", "Extra"};
    private int slowTicks;
    private boolean wasTowering;
    private int towerTicks;
    public boolean tower;
    private boolean hasTowered, startedTowerInAir, setLowMotion, firstJump;
    private int cMotionTicks, placeTicks;
    public int upCount;
    public float yaw;

    public float pitch;

    //vertical tower
    private boolean aligning, aligned, placed;
    private int blockX;
    private double firstX, firstY, firstZ;
    public boolean placeExtraBlock;

    public boolean speed;

    public Tower() {
        super("Tower", category.player);
        this.registerSetting(towerMove = new SliderSetting("Move mode", 0, towerMoveModes));
        this.registerSetting(verticalTower = new SliderSetting("Vertical mode", 0, verticalTowerModes));
        this.registerSetting(slowedMotion = new SliderSetting("Slowed motion", "%", 0, 0, 100, 1));
        this.registerSetting(slowedTicks = new SliderSetting("Slowed ticks", 1, 0, 20, 1));
        this.registerSetting(disableInLiquid = new ButtonSetting("Disable in liquid", false));
        this.registerSetting(disableWhileCollided = new ButtonSetting("Disable while collided", false));
        this.registerSetting(disableWhileHurt = new ButtonSetting("Disable while hurt", false));

        this.canBeEnabled = false;
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {
        if (canTower() && Utils.keysDown()) {
            if (tower) {
                towerTicks = mc.thePlayer.onGround ? 0 : ++towerTicks;
            }
            switch ((int) towerMove.getInput()) {
                case 4:
                    if (tower) {
                        if (towerTicks == 6) {
                            e.setPosY(e.getPosY() + 0.000383527);
                            ModuleManager.scaffold.rotateForward();
                        }
                    }
                    break;
            }
        }
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        int valY = (int) Math.round((mc.thePlayer.posY % 1) * 10000);
        if (canTower() && Utils.keysDown()) {
            wasTowering = true;
            switch ((int) towerMove.getInput()) {
                case 1:
                    mc.thePlayer.motionY = 0.41965;
                    switch (towerTicks) {
                        case 1:
                            mc.thePlayer.motionY = 0.33;
                            break;
                        case 2:
                            mc.thePlayer.motionY = 1 - mc.thePlayer.posY % 1;
                            break;
                    }
                    if (towerTicks >= 3) {
                        towerTicks = 0;
                    }
                case 2:
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.motionY = 0.4196;
                    }
                    else {
                        switch (towerTicks) {
                            case 3:
                            case 4:
                                mc.thePlayer.motionY = 0;
                                break;
                            case 5:
                                mc.thePlayer.motionY = 0.4191;
                                break;
                            case 6:
                                mc.thePlayer.motionY = 0.3275;
                                break;
                            case 11:
                                mc.thePlayer.motionY = - 0.5;

                        }
                    }
                    break;
                case 3:
                    if (mc.thePlayer.posY % 1 == 0 && mc.thePlayer.onGround && !setLowMotion) {
                        tower = true;
                    }
                    if (tower) {
                        if (valY == 0) {
                            mc.thePlayer.motionY = 0.42f;
                            Utils.setSpeed(getTowerGroundSpeed(getSpeedLevel()));
                            startedTowerInAir = false;
                        }
                        else if (valY > 4000 && valY < 4300) {
                            mc.thePlayer.motionY = 0.33f;
                            Utils.setSpeed(getTowerSpeed(getSpeedLevel()));
                            speed = true;
                            hasTowered = true;
                        }
                        else if (valY > 7000) {
                            if (setLowMotion) {
                                tower = false;
                            }
                            speed = false;
                            mc.thePlayer.motionY = 1 - mc.thePlayer.posY % 1f;
                        }
                    }
                    else if (setLowMotion) {
                        ++cMotionTicks;
                        if (cMotionTicks == 1) {
                            mc.thePlayer.motionY = 0.06F;
                        }
                        else if (cMotionTicks == 3) {
                            cMotionTicks = 0;
                            setLowMotion = false;
                            tower = true;
                            Utils.setSpeed(getTowerGroundSpeed(getSpeedLevel()) - 0.02);
                        }
                    }
                    break;
                case 4:
                    speed = false;
                    int simpleY = (int) Math.round((mc.thePlayer.posY % 1.0D) * 100.0D);
                    if (mc.thePlayer.posY % 1 == 0 && mc.thePlayer.onGround) {
                        tower = true;
                    }
                    if (tower) {
                        switch (simpleY) {
                            case 0:
                                mc.thePlayer.motionY = 0.42f;
                                if (towerTicks == 6) {
                                    mc.thePlayer.motionY = -0.078400001525879;
                                }
                                Utils.setSpeed(getTowerSpeed(getSpeedLevel()));
                                speed = true;
                                break;
                            case 42:
                                mc.thePlayer.motionY = 0.33f;
                                Utils.setSpeed(getTowerSpeed(getSpeedLevel()));
                                speed = true;
                                break;
                            case 75:
                                mc.thePlayer.motionY = 1 - mc.thePlayer.posY % 1f;
                                break;
                        }
                    }
                    break;
            }
        }
        else {
            if (wasTowering && slowedTicks.getInput() > 0) {
                if (slowTicks++ < slowedTicks.getInput()) {
                    mc.thePlayer.motionX *= slowedMotion.getInput() / 100;
                    mc.thePlayer.motionZ *= slowedMotion.getInput() / 100;
                }
                else {
                    slowTicks = 0;
                    wasTowering = false;
                }
            }
            else {
                if (wasTowering) {
                    wasTowering = false;
                }
                slowTicks = 0;
            }
            hasTowered = tower = firstJump = startedTowerInAir = setLowMotion = speed = false;
            cMotionTicks = placeTicks = towerTicks = 0;
            reset();
        }
        if (canTower() && !Utils.keysDown()) {
            wasTowering = true;
            switch ((int) verticalTower.getInput()) {
                case 1:
                    mc.thePlayer.motionY = 0.42f;
                    break;
                case 2:
                    if (!aligned) {
                        if (mc.thePlayer.onGround) {
                            if (!aligning) {
                                blockX = (int) mc.thePlayer.posX;

                                firstX = mc.thePlayer.posX - 10;
                                firstY = mc.thePlayer.posY;
                                firstZ = mc.thePlayer.posZ;
                            }
                            mc.thePlayer.motionX = 0.22;
                            aligning = true;
                        }
                        if (aligning && (int) mc.thePlayer.posX > blockX) {
                            aligned = true;
                        }
                        yaw = RotationUtils.getRotations(firstX, firstY, firstZ)[0];
                        pitch = 0;
                    }
                    if (aligned) {
                        if (placed) {
                            yaw = 0;
                            pitch = 89.9F;
                        }
                        else {
                            yaw = RotationUtils.getRotations(firstX, firstY, firstZ)[0];
                            pitch = 0;
                        }
                        placeExtraBlock = true;
                        mc.thePlayer.motionX = 0;
                        mc.thePlayer.motionY = verticalTowerValue();
                        mc.thePlayer.motionZ = 0;
                    }
                    break;
            }
        } else {
            yaw = pitch = 0;
            aligning = aligned = placed = false;
            firstX = 0;
            placeExtraBlock = false;
        }
    }

    public boolean isVerticalTowering() {
        return canTower() && !Utils.keysDown() && verticalTower.getInput() == 2;
    }

    @SubscribeEvent
    public void onPostPlayerInput(PostPlayerInputEvent e) {
        if (canTower() && Utils.keysDown() && towerMove.getInput() > 0) {
            mc.thePlayer.movementInput.jump = false;
            if (!firstJump) {
                if (!mc.thePlayer.onGround) {
                    if (!startedTowerInAir) {
                        Utils.setSpeed(getTowerGroundSpeed(getSpeedLevel()) - 0.04);
                    }
                    startedTowerInAir = true;
                }
                else if (mc.thePlayer.onGround) {
                    Utils.setSpeed(getTowerGroundSpeed(getSpeedLevel()));
                    firstJump = true;
                }
            }
        }
        if (canTower() && !Utils.keysDown() && verticalTower.getInput() > 0) {
            mc.thePlayer.movementInput.jump = false;
        }
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent e) {
        if (e.getPacket() instanceof C08PacketPlayerBlockPlacement) {
            if (canTower() && Utils.isMoving()) {
                ++placeTicks;
                if (((C08PacketPlayerBlockPlacement) e.getPacket()).getPlacedBlockDirection() == 1 && placeTicks > 5 && hasTowered) {
                    upCount++;
                    if (upCount >= 2) {
                        setLowMotion = true;
                    }
                }
                else {
                    upCount = 0;
                }
            }
            else {
                placeTicks = 0;
            }
            if (aligned) {
                placed = true;
            }
        }
    }

    private void reset() {
        towerTicks = 0;
        tower = false;
        placeTicks = 0;
        setLowMotion = false;
    }

    public boolean canTower() {
        if (!Utils.nullCheck() || !Utils.jumpDown()) {
            return false;
        }
        else if (disableWhileHurt.isToggled() && mc.thePlayer.hurtTime > 9) {
            return false;
        }
        else if (mc.thePlayer.isCollidedHorizontally && disableWhileCollided.isToggled()) {
            return false;
        }
        else if ((mc.thePlayer.isInWater() || mc.thePlayer.isInLava()) && disableInLiquid.isToggled()) {
            return false;
        }
        else if (modulesEnabled()) {
            return true;
        }
        return false;
    }

    private boolean modulesEnabled() {
        return (ModuleManager.scaffold.moduleEnabled && ModuleManager.scaffold.holdingBlocks() && ModuleManager.scaffold.hasSwapped && !ModuleManager.longJump.function);
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

    private double verticalTowerValue() {
        int valY = (int) Math.round((mc.thePlayer.posY % 1) * 10000);
        double value = 0;
        if (valY == 0) {
            value = 0.42f;
        } else if (valY > 4000 && valY < 4300) {
            value = 0.33f;
        } else if (valY > 7000) {
            value = 1 - mc.thePlayer.posY % 1f;
        }
        return value;
    }

    private double[] towerSpeedLevels = {0.3, 0.34, 0.38, 0.42, 0.42};

    private double getTowerSpeed(int speedLevel) {
        if (speedLevel >= 0) {
            return towerSpeedLevels[speedLevel];
        }
        return towerSpeedLevels[0];
    }

    private final double[] towerGroundSpeedLevels = {0.22, 0.25, 0.3, 0.35, 0.4};

    private double getTowerGroundSpeed(int speedLevel) {
        if (speedLevel >= 0) {
            return towerGroundSpeedLevels[speedLevel];
        }
        return towerGroundSpeedLevels[0];
    }

}