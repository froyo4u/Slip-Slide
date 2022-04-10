package tk.meowmc.slippery.mixin;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.FluidState;
import net.minecraft.sound.SoundEvent;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import tk.meowmc.slippery.config.SlipperyConfig;


@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {


    SlipperyConfig config = SlipperyConfig.get();

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Shadow
    public abstract boolean canMoveVoluntarily();

    @Shadow
    public abstract boolean hasStatusEffect(StatusEffect effect);

    @Shadow
    protected abstract boolean shouldSwimInFluids();

    @Shadow
    public abstract boolean canWalkOnFluid(FluidState fluidState);

    @Shadow
    protected abstract float getBaseMovementSpeedMultiplier();

    @Shadow
    public abstract float getMovementSpeed();

    @Shadow
    public abstract boolean isClimbing();

    @Shadow
    public abstract Vec3d method_26317(double d, boolean bl, Vec3d vec3d);

    @Shadow
    public abstract boolean isFallFlying();

    @Shadow
    public abstract Vec3d applyMovementInput(Vec3d movementInput, float slipperiness);

    @Shadow
    @Nullable
    public abstract StatusEffectInstance getStatusEffect(StatusEffect effect);

    @Shadow
    public abstract boolean hasNoDrag();

    @Shadow
    public abstract void updateLimbs(LivingEntity entity, boolean flutter);

    @Shadow
    public abstract LivingEntity.FallSounds getFallSounds();

    /**
     * @author me
     * @reason cuz
     */
    @Overwrite
    public void travel(Vec3d movementInput) {
        if (this.canMoveVoluntarily() || this.isLogicalSideForUpdatingMovement()) {
            boolean bl;
            double d = 0.08;
            boolean bl2 = bl = this.getVelocity().y <= 0.0;
            if (bl && this.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
                d = 0.01;
                this.onLanding();
            }
            FluidState fluidState = this.world.getFluidState(this.getBlockPos());
            if (this.isTouchingWater() && this.shouldSwimInFluids() && !this.canWalkOnFluid(fluidState)) {
                double e = this.getY();
                float f = this.isSprinting() ? 0.9f : this.getBaseMovementSpeedMultiplier();
                float g = 0.02f;
                float h = EnchantmentHelper.getDepthStrider((LivingEntity) (Object) this);
                if (h > 3.0f) {
                    h = 3.0f;
                }
                if (!this.onGround) {
                    h *= 0.5f;
                }
                if (h > 0.0f) {
                    f += (0.54600006f - f) * h / 3.0f;
                    g += (this.getMovementSpeed() - g) * h / 3.0f;
                }
                if (this.hasStatusEffect(StatusEffects.DOLPHINS_GRACE)) {
                    f = 0.96f;
                }
                this.updateVelocity(g, movementInput);
                this.move(MovementType.SELF, this.getVelocity());
                Vec3d vec3d = this.getVelocity();
                if (this.horizontalCollision && this.isClimbing()) {
                    vec3d = new Vec3d(vec3d.x, 0.2, vec3d.z);
                }
                this.setVelocity(vec3d.multiply(f, 0.8f, f));
                Vec3d vec3d2 = this.method_26317(d, bl, this.getVelocity());
                this.setVelocity(vec3d2);
                if (this.horizontalCollision && this.doesNotCollide(vec3d2.x, vec3d2.y + (double) 0.6f - this.getY() + e, vec3d2.z)) {
                    this.setVelocity(vec3d2.x, 0.3f, vec3d2.z);
                }
            } else if (this.isInLava() && this.shouldSwimInFluids() && !this.canWalkOnFluid(fluidState)) {
                Vec3d vec3d3;
                double e = this.getY();
                this.updateVelocity(0.02f, movementInput);
                this.move(MovementType.SELF, this.getVelocity());
                if (this.getFluidHeight(FluidTags.LAVA) <= this.getSwimHeight()) {
                    this.setVelocity(this.getVelocity().multiply(0.5, 0.8f, 0.5));
                    vec3d3 = this.method_26317(d, bl, this.getVelocity());
                    this.setVelocity(vec3d3);
                } else {
                    this.setVelocity(this.getVelocity().multiply(0.5));
                }
                if (!this.hasNoGravity()) {
                    this.setVelocity(this.getVelocity().add(0.0, -d / 4.0, 0.0));
                }
                vec3d3 = this.getVelocity();
                if (this.horizontalCollision && this.doesNotCollide(vec3d3.x, vec3d3.y + (double) 0.6f - this.getY() + e, vec3d3.z)) {
                    this.setVelocity(vec3d3.x, 0.3f, vec3d3.z);
                }
            } else if (this.isFallFlying()) {
                float o;
                double m;
                Vec3d vec3d4 = this.getVelocity();
                if (vec3d4.y > -0.5) {
                    this.fallDistance = 1.0f;
                }
                Vec3d vec3d5 = this.getRotationVector();
                float f = this.getPitch() * ((float) Math.PI / 180);
                double i = Math.sqrt(vec3d5.x * vec3d5.x + vec3d5.z * vec3d5.z);
                double j = vec3d4.horizontalLength();
                double k = vec3d5.length();
                double l = Math.cos(f);
                l = l * l * Math.min(1.0, k / 0.4);
                vec3d4 = this.getVelocity().add(0.0, d * (-1.0 + l * 0.75), 0.0);
                if (vec3d4.y < 0.0 && i > 0.0) {
                    m = vec3d4.y * -0.1 * l;
                    vec3d4 = vec3d4.add(vec3d5.x * m / i, m, vec3d5.z * m / i);
                }
                if (f < 0.0f && i > 0.0) {
                    m = j * (double) (-MathHelper.sin(f)) * 0.04;
                    vec3d4 = vec3d4.add(-vec3d5.x * m / i, m * 3.2, -vec3d5.z * m / i);
                }
                if (i > 0.0) {
                    vec3d4 = vec3d4.add((vec3d5.x / i * j - vec3d4.x) * 0.1, 0.0, (vec3d5.z / i * j - vec3d4.z) * 0.1);
                }
                this.setVelocity(vec3d4.multiply(0.99f, 0.98f, 0.99f));
                this.move(MovementType.SELF, this.getVelocity());
                if (this.horizontalCollision && !this.world.isClient && (o = (float) ((j - this.getVelocity().horizontalLength()) * 10.0 - 3.0)) > 0.0f) {
                    this.playSound(this.getFallSound((int) o), 1.0f, 1.0f);
                    this.damage(DamageSource.FLY_INTO_WALL, o);
                }
                if (this.onGround && !this.world.isClient) {
                    this.setFlag(Entity.FALL_FLYING_FLAG_INDEX, false);
                }
            } else {
                BlockPos blockPos = this.getVelocityAffectingPos();
                float p = config.slideValue;
                float f = this.onGround ? p * 0.91f : 0.91f;
                Vec3d vec3d6 = this.applyMovementInput(movementInput, p);
                double q = vec3d6.y;
                if (this.hasStatusEffect(StatusEffects.LEVITATION)) {
                    q += (0.05 * (double) (this.getStatusEffect(StatusEffects.LEVITATION).getAmplifier() + 1) - vec3d6.y) * 0.2;
                    this.onLanding();
                } else if (!this.world.isClient || this.world.isChunkLoaded(blockPos)) {
                    if (!this.hasNoGravity()) {
                        q -= d;
                    }
                } else {
                    q = this.getY() > (double) this.world.getBottomY() ? -0.1 : 0.0;
                }
                double vecX = vec3d6.x < 16384 ? vec3d6.x : 16384;
                double vecZ = vec3d6.z < 32767 ? vec3d6.z : 16384;
                if (this.hasNoDrag()) {
                    this.setVelocity(vecX, q, vecZ);
                } else {
                    this.setVelocity(vecX * (double) f, q * (double) 0.98f, vecZ * (double) f);
                }
            }
        }
        this.updateLimbs((LivingEntity) (Object) this, this instanceof Flutterer);
    }

    private SoundEvent getFallSound(int distance) {
        return distance > 4 ? this.getFallSounds().big() : this.getFallSounds().small();
    }
}
