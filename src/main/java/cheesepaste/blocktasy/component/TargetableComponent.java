package cheesepaste.blocktasy.component;

import cheesepaste.blocktasy.Blocktasy;
import cheesepaste.blocktasy.entity.BaseBlockEntity;
import cheesepaste.blocktasy.entity.FollowingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class TargetableComponent extends EntityComponents{
    public Entity target;
    private TrackedData<Optional<UUID>> TARGET_UUID=null;
    int targetLostCounter=0;
    private static final float CLOSE_DISTANCE = Blocktasy.config.closeDistance;
    private static final int MAX_TARGET_LOST_TICKS = 200; // 10秒后放弃跟随

    private int jumpTimer = 0;
    public boolean shouldJump = false;

    private static final float HORIZONTAL_SPEED = 0.15f;
    private static final float JUMP_STRENGTH = 0.5f;
    private static final int JUMP_COOLDOWN = 20;
    private static final float MAX_SPEED = 0.5f;
    private static final float AIR_RESISTANCE = 0.98f;
    private static final float ROTATION_SPEED = 10.0f; // 旋转速度（度/秒）
    private static final float ROTATION_INTERPOLATION_FACTOR = 0.2f; // 旋转插值因子



    public TargetableComponent(BaseBlockEntity parent,TrackedData<Optional<UUID>> TARGET_UUID) {

        super(parent);
        this.TARGET_UUID=TARGET_UUID;

    }
    public TargetableComponent(){
        super(null);
    }

    @Override
    public boolean tick() {
        if (this.target == null || !this.target.isAlive()) {
            refreshTarget();
        }
        if(target!=null){
            //smoothLookAtPosition(target.getPos(),5);
        }

        if (this.target == null) {
            // 只停止水平移动，不影响重力
//            Vec3d currentVel = parent.getVelocity();
//            parent.setVelocity(new Vec3d(0, currentVel.y, 0));
//            applyMovement();
//            parent.move(MovementType.SELF, parent.getVelocity());
            return true;
        }

        if (isClose()) {
            Vec3d currentVel = parent.getVelocity();
            parent.setVelocity(new Vec3d(0, currentVel.y, 0));
            applyMovement();
            parent.move(MovementType.SELF, parent.getVelocity());
            return true;
        }
        processMovement();

        return false;
    }

    public boolean isClose() {
        if (target != null) {
            return parent.getPos().distanceTo(target.getPos()) <= CLOSE_DISTANCE;
        }
        return false;
    }

    private void applyMovement() {
        double e = parent.getVelocity().y;
        e -= parent.getFinalGravity();

        parent.setVelocity(parent.getVelocity().x, e * (double)0.98F, parent.getVelocity().z);
    }



    private void processMovement() {
        // 更新跳跃计时器
        if (jumpTimer > 0) {
            jumpTimer--;
        }

        // 如果在地面上且跳跃计时器为0，准备跳跃
        if (parent.isOnGround() && jumpTimer <= 0) {
            shouldJump = true;
            jumpTimer = JUMP_COOLDOWN;
        }

        // 执行跳跃
        if (shouldJump && parent.isOnGround()) {
            jumpTowardTarget();
            shouldJump = false;
        }



        // 应用水平移动
        Vec3d horizontalDir = getHorizontalDirection();
        if (horizontalDir != null) {
            applyHorizontalMovement(horizontalDir);
        }

        // 应用速度和限制
        applyMovement();
    }

    private void applyHorizontalMovement(@NotNull Vec3d direction) {
        Vec3d horizontalVelocity = direction.multiply(HORIZONTAL_SPEED);
        parent.setVelocity(new Vec3d(
                horizontalVelocity.x,
                parent.getVelocity().y,
                horizontalVelocity.z
        ));

        // 应用移动
        parent.move(MovementType.SELF, parent.getVelocity());
    }


    @Nullable
    private Vec3d getHorizontalDirection() {
        if (target == null) return null;

        Vec3d toTarget = target.getPos().subtract(parent.getPos());
        Vec3d horizontal = new Vec3d(toTarget.x, 0, toTarget.z);

        if (horizontal.lengthSquared() < 0.001) {
            return null;
        }

        return horizontal.normalize();
    }

    private void jumpTowardTarget() {
        Vec3d direction = getHorizontalDirection();
        if (direction == null) return;

        // 计算跳跃力量（根据距离调整）
        float jumpMultiplier = 1.0f;

        parent.setVelocity(
                direction.x * HORIZONTAL_SPEED * 2.0f * jumpMultiplier,
                JUMP_STRENGTH * jumpMultiplier + parent.getVelocity().y,
                direction.z * HORIZONTAL_SPEED * 2.0f * jumpMultiplier
        );
    }

    @Override
    public void initDT(DataTracker.Builder builder) {
        builder.add(TARGET_UUID, Optional.empty());
        Blocktasy.LOGGER.info("Target");
    }

    @Override
    public void readNBT(NbtCompound nbt) {
        if (nbt.contains("Target", NbtElement.INT_ARRAY_TYPE)) {
            UUID targetUuid = nbt.getUuid("Target");
            parent.getDataTracker().set(TARGET_UUID, Optional.of(targetUuid));
        }
        if (nbt.contains("TargetLostCounter", NbtElement.INT_TYPE)) {
            this.targetLostCounter = nbt.getInt("TargetLostCounter");
        }
    }

    @Override
    public void writeNBT(NbtCompound nbt) {
        if (this.target != null) {
            nbt.putUuid("Target", this.target.getUuid());
        }

        nbt.putInt("TargetLostCounter", targetLostCounter);

    }

    @Override
    public void OnSpawn() {

    }

    public void setTarget(PlayerEntity target) {
        Entity oldTarget = this.target;
        this.target = target;

        if (target != null) {
            parent.getDataTracker().set(TARGET_UUID, Optional.of(target.getUuid()));
            targetLostCounter = 0;
        } else {
            parent.getDataTracker().set(TARGET_UUID, Optional.empty());
        }
    }


    private void refreshTarget() {
        Optional<UUID> uuid = parent.getDataTracker().get(TARGET_UUID);

        if (uuid.isPresent()) {
            Entity entity = parent.getWorld().getPlayerByUuid(uuid.get());
            if (entity == null) {
//                // 如果不是玩家，尝试查找其他实体
                for (Entity e : parent.getWorld().getPlayers()) {
                    if (e.getUuid().equals(uuid.get())) {
                        entity = e;
                        break;
                    }
                }
            }

            if (entity != null && entity.isAlive()) {
                this.target = entity;
                targetLostCounter = 0;
            } else {
                targetLostCounter++;
                if (targetLostCounter > MAX_TARGET_LOST_TICKS) {
                    this.target = null;
                    parent.getDataTracker().set(TARGET_UUID, Optional.empty());
                }
            }
        }
    }


    /**
     * 简洁版本 - 直接更新yaw
     */
    private void lookAtTarget(Entity target) {
        // 计算看向目标所需的yaw角度
        double dx = target.getX() - parent.getX();
        double dz = target.getZ() - parent.getZ();

        // 计算目标yaw角度
        double targetYaw = MathHelper.atan2(dz, dx) * (180.0 / Math.PI) - 90.0;

        // 直接设置（不插值）
        parent.setYaw((float)targetYaw);
        parent.setHeadYaw((float)targetYaw);
        parent.setBodyYaw((float) targetYaw);
        //PuzzleRain.LOGGER.info(target.getRotationVector().toString());
    }
    /**
     * 基于Vec3d目标位置的版本
     * @param targetPos 目标位置
     * @param smoothFactor 平滑系数
     */
    private void smoothLookAtPosition(Vec3d targetPos, float smoothFactor) {
        // 计算方向向量
        double dx = targetPos.x - parent.getX();
        double dz = targetPos.z - parent.getZ();

        // 计算目标yaw角度
        double targetYaw = MathHelper.atan2(dz, dx) * (180.0 / Math.PI) - 90.0;

        float currentYaw = parent.getYaw();
        currentYaw = MathHelper.wrapDegrees(currentYaw);
        targetYaw = MathHelper.wrapDegrees((float)targetYaw);

        // 角度插值
        float angleDiff = MathHelper.wrapDegrees((float)targetYaw - currentYaw);
        float newYaw = currentYaw + angleDiff * MathHelper.clamp(smoothFactor, 0, 1);

        parent.setYaw(newYaw);
        parent.setHeadYaw(newYaw);
    }
}
