package keystrokesmod.mixin.impl.entity;

import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.Reduce;
import keystrokesmod.module.impl.movement.KeepSprint;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.boss.EntityDragonPart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.potion.Potion;
import net.minecraft.stats.AchievementList;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer extends EntityLivingBase {
    public MixinEntityPlayer(World p_i1594_1_) {
        super(p_i1594_1_);
    }

    @Shadow
    public abstract ItemStack getHeldItem();

    @Shadow
    public abstract void onCriticalHit(Entity p_onCriticalHit_1_);

    @Shadow
    public abstract void onEnchantmentCritical(Entity p_onEnchantmentCritical_1_);

    @Shadow
    public abstract void triggerAchievement(StatBase p_triggerAchievement_1_);

    @Shadow
    public abstract ItemStack getCurrentEquippedItem();

    @Shadow
    public abstract void destroyCurrentEquippedItem();

    @Shadow
    public abstract void addStat(StatBase p_addStat_1_, int p_addStat_2_);

    @Shadow
    public abstract void addExhaustion(float p_addExhaustion_1_);

    @Overwrite
    public void attackTargetEntityWithCurrentItem(Entity targetEntity) {
        if (ForgeHooks.onPlayerAttackTarget(((EntityPlayer) (Object) this), targetEntity)) {
            if (targetEntity.canAttackWithItem() && !targetEntity.hitByEntity(this)) {
                float f = (float) this.getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue();
                int i = 0;
                float f1 = 0.0F;
                if (targetEntity instanceof EntityLivingBase) {
                    f1 = EnchantmentHelper.getModifierForCreature(this.getHeldItem(), ((EntityLivingBase) targetEntity).getCreatureAttribute());
                } else {
                    f1 = EnchantmentHelper.getModifierForCreature(this.getHeldItem(), EnumCreatureAttribute.UNDEFINED);
                }

                i += EnchantmentHelper.getKnockbackModifier(this);
                if (this.isSprinting()) {
                    ++i;
                }

                if (f > 0.0F || f1 > 0.0F) {
                    boolean flag = this.fallDistance > 0.0F && !this.onGround && !this.isOnLadder() && !this.isInWater() && !this.isPotionActive(Potion.blindness) && this.ridingEntity == null && targetEntity instanceof EntityLivingBase;
                    if (flag && f > 0.0F) {
                        f *= 1.5F;
                    }

                    f += f1;
                    boolean flag1 = false;
                    int j = EnchantmentHelper.getFireAspectModifier(this);
                    if (targetEntity instanceof EntityLivingBase && j > 0 && !targetEntity.isBurning()) {
                        flag1 = true;
                        targetEntity.setFire(1);
                    }

                    double d0 = targetEntity.motionX;
                    double d1 = targetEntity.motionY;
                    double d2 = targetEntity.motionZ;
                    boolean flag2 = targetEntity.attackEntityFrom(DamageSource.causePlayerDamage(((EntityPlayer) (Object) this)), f);
                    if (flag2) {
                        if (i > 0) {
                            targetEntity.addVelocity((double) (-MathHelper.sin(this.rotationYaw * 3.1415927F / 180.0F) * (float) i * 0.5F), 0.1, (double) (MathHelper.cos(this.rotationYaw * 3.1415927F / 180.0F) * (float) i * 0.5F));
                            if (ModuleManager.reduce != null && ModuleManager.reduce.isEnabled()) {
                                Reduce.reduce(targetEntity);
                            }
                            else if (ModuleManager.keepSprint != null && ModuleManager.keepSprint.isEnabled()) {
                                KeepSprint.keepSprint(targetEntity);
                            }
                            else {
                                this.motionX *= 0.6D;
                                this.motionZ *= 0.6D;
                                this.setSprinting(false);
                            }
                        }

                        if (targetEntity instanceof EntityPlayerMP && targetEntity.velocityChanged) {
                            ((EntityPlayerMP) targetEntity).playerNetServerHandler.sendPacket(new S12PacketEntityVelocity(targetEntity));
                            targetEntity.velocityChanged = false;
                            targetEntity.motionX = d0;
                            targetEntity.motionY = d1;
                            targetEntity.motionZ = d2;
                        }

                        if (flag) {
                            this.onCriticalHit(targetEntity);
                        }

                        if (f1 > 0.0F) {
                            this.onEnchantmentCritical(targetEntity);
                        }

                        if (f >= 18.0F) {
                            this.triggerAchievement(AchievementList.overkill);
                        }

                        this.setLastAttacker(targetEntity);
                        if (targetEntity instanceof EntityLivingBase) {
                            EnchantmentHelper.applyThornEnchantments((EntityLivingBase) targetEntity, this);
                        }

                        EnchantmentHelper.applyArthropodEnchantments(this, targetEntity);
                        ItemStack itemstack = this.getCurrentEquippedItem();
                        Entity entity = targetEntity;
                        if (targetEntity instanceof EntityDragonPart) {
                            IEntityMultiPart ientitymultipart = ((EntityDragonPart) targetEntity).entityDragonObj;
                            if (ientitymultipart instanceof EntityLivingBase) {
                                entity = (EntityLivingBase) ientitymultipart;
                            }
                        }

                        if (itemstack != null && entity instanceof EntityLivingBase) {
                            itemstack.hitEntity((EntityLivingBase) entity, ((EntityPlayer) (Object) this));
                            if (itemstack.stackSize <= 0) {
                                this.destroyCurrentEquippedItem();
                            }
                        }

                        if (targetEntity instanceof EntityLivingBase) {
                            this.addStat(StatList.damageDealtStat, Math.round(f * 10.0F));
                            if (j > 0) {
                                targetEntity.setFire(j * 4);
                            }
                        }

                        this.addExhaustion(0.3F);
                    } else if (flag1) {
                        targetEntity.extinguish();
                    }
                }
            }

        }
    }

    @Inject(method = "isBlocking", at = @At("RETURN"), cancellable = true)
    private void isBlocking(CallbackInfoReturnable<Boolean> cir) {
        if (ModuleManager.killAura != null && ModuleManager.killAura.isEnabled() && ModuleManager.killAura.blockingClient && ((Object) this) == Minecraft.getMinecraft().thePlayer) {
            cir.setReturnValue(true);
        }
        cir.setReturnValue(cir.getReturnValue());
    }
}