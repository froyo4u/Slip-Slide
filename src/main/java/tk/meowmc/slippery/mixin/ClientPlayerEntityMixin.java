package tk.meowmc.slippery.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Flutterer;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.FluidState;
import net.minecraft.sound.SoundEvent;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import tk.meowmc.slippery.config.SlipperyConfig;


@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity {
    @Shadow
    public int ticksSinceSprintingChanged;
    @Shadow
    public Input input;
    @Shadow
    @Final
    public ClientPlayNetworkHandler networkHandler;
    @Shadow
    protected int ticksLeftToDoubleTapSprint;
    @Shadow
    @Final
    protected MinecraftClient client;
    @Shadow
    private boolean inSneakingPose;
    @Shadow
    private int ticksToNextAutojump;
    @Shadow
    private boolean falling;
    @Shadow
    private int underwaterVisibilityTicks;
    @Shadow
    private int field_3938;
    @Shadow
    private float mountJumpStrength;

    public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Shadow
    protected abstract void updateNausea();

    @Shadow
    protected abstract boolean isWalking();

    @Shadow
    public abstract boolean shouldSlowDown();

    @Shadow
    protected abstract boolean isCamera();

    @Shadow
    public abstract boolean hasJumpingMount();

    @Shadow
    public abstract float getMountJumpStrength();

    @Shadow
    protected abstract void startRidingJump();

    @Shadow
    protected abstract void pushOutOfBlocks(double x, double z);

    SlipperyConfig config = SlipperyConfig.get();

    @Override
    public void travel(Vec3d movementInput) {
        double g;
        double d = this.getX();
        double e = this.getY();
        double f = this.getZ();
        if (this.isSwimming() && !this.hasVehicle()) {
            double h;
            g = this.getRotationVector().y;
            double d2 = h = g < -0.2 ? 0.085 : 0.06;
            if (g <= 0.0 || this.jumping || !this.world.getBlockState(new BlockPos(this.getX(), this.getY() + 1.0 - 0.1, this.getZ())).getFluidState().isEmpty()) {
                Vec3d vec3d = this.getVelocity();
                this.setVelocity(vec3d.add(0.0, (g - vec3d.y) * h, 0.0));
            }
        }
        if (this.getAbilities().flying && !this.hasVehicle()) {
            g = this.getVelocity().y;
            float i = this.airStrafingSpeed;
            this.airStrafingSpeed = this.getAbilities().getFlySpeed() * (float) (this.isSprinting() ? 2 : 1);
            myTravel(movementInput);
            Vec3d vec3d2 = this.getVelocity();
            this.setVelocity(vec3d2.x, g * 0.6, vec3d2.z);
            this.airStrafingSpeed = i;
            this.onLanding();
            this.setFlag(Entity.FALL_FLYING_FLAG_INDEX, false);
        } else {
            myTravel(movementInput);
        }
        this.increaseTravelMotionStats(this.getX() - d, this.getY() - e, this.getZ() - f);
    }

    public void myTravel(Vec3d movementInput) {
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
                float h = EnchantmentHelper.getDepthStrider(this);
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
                if (p > 1.75f)
                    p = 1.75f;
                if (p < 0.1f)
                    p = 0.1f;
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
        this.updateLimbs(this, this instanceof Flutterer);
    }

    private SoundEvent getFallSound(int distance) {
        return distance > 4 ? this.getFallSounds().big() : this.getFallSounds().small();
    }
}
