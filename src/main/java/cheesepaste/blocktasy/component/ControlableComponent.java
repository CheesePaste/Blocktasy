package cheesepaste.blocktasy.component;

import cheesepaste.blocktasy.entity.BaseBlockEntity;
import cheesepaste.blocktasy.entity.FollowingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class ControlableComponent extends EntityComponents{
    private UUID controllingPlayerId;

    private static final float LAUNCH_SPEED = 20.0f; // 发射速度

    private static final TrackedData<Boolean> IS_CONTROLLED =
            DataTracker.registerData(FollowingEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    // 操纵模式参数
    private static final float CONTROL_DISTANCE = 5.0f; // 操纵时的默认距离
    private static final float CONTROL_SPEED = 0.3f; // 操纵移动速度
    private static final float CONTROL_SMOOTHING = 0.2f; // 操纵平滑因子
    private static final TrackedData<Optional<UUID>> CONTROLLING_PLAYER_UUID =
            DataTracker.registerData(FollowingEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);

    // 控制状态
    private Vec3d targetControlPos; // 控制目标位置
    private boolean justLaunched = false; // 刚刚被发射
    private int launchCooldown = 0; // 发射冷却
    
    public ControlableComponent(BaseBlockEntity parent) {
        super(parent);
    }

    public void launchEntity(Vec3d direction) {
        // 计算发射速度
        Vec3d launchVelocity = direction.multiply(LAUNCH_SPEED);

        // 应用速度
        parent.setVelocity(launchVelocity);

        // 标记为刚刚发射
        justLaunched = true;
        launchCooldown = 10; // 10 tick的冷却，防止立即被重新控制

        // 取消控制

            setControllingPlayer(null);



        // 恢复重力
        parent.setNoGravity(false);

        // 播放发射音效（可选）
        // this.playSound(SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
    }

    @Override
    public boolean tick() {
        if (launchCooldown > 0) {
            launchCooldown--;
        }
        refreshControlState();
        if (isControlled()) {
            tickControlled();
            return true;
        }
        return false;
    }

    /**
     * 被控制时的逻辑
     */
    private void tickControlled() {
        PlayerEntity controller = getControllingPlayer();
        if (controller == null) {
            setControllingPlayer(null);
            return;
        }

        // 如果刚刚发射，跳过控制逻辑
        if (justLaunched && launchCooldown > 0) {
            return;
        }

        // 获取玩家准心指向的位置
        Vec3d lookDirection = controller.getRotationVec(1.0F);
        Vec3d eyePos = controller.getEyePos();

        // 计算目标位置：玩家面前一定距离
        Vec3d newTargetPos = eyePos.add(lookDirection.multiply(CONTROL_DISTANCE));

        // 平滑更新目标位置
        Vec3d currentTargetPos = this.targetControlPos;
        Vec3d toNewTarget = newTargetPos.subtract(currentTargetPos);
        double distanceToNewTarget = toNewTarget.length();

        if (distanceToNewTarget > 0.1) {
            // 平滑移动目标位置
            Vec3d smoothedToTarget = toNewTarget.multiply(CONTROL_SMOOTHING);
            this.targetControlPos = currentTargetPos.add(smoothedToTarget);
        } else {
            this.targetControlPos = newTargetPos;
        }

        // 平滑移动到目标位置
        Vec3d currentPos = parent.getPos();
        Vec3d toTarget = targetControlPos.subtract(currentPos);
        double distance = toTarget.length();

        if (distance > 0.1) {
            // 使用平滑移动
            double speed = Math.min(distance * CONTROL_SPEED, 1.0); // 限制最大速度
            Vec3d velocity = toTarget.normalize().multiply(speed);
            parent.setVelocity(velocity);
        } else {
            parent.setVelocity(Vec3d.ZERO);
        }

        // 设置实体的朝向
//        float yaw = controller.getYaw();
//        parent.setYaw(yaw);
//        parent.setHeadYaw(yaw);
//        parent.setBodyYaw(yaw);

        // 应用移动
        parent.move(MovementType.SELF, parent.getVelocity());

    }

    @Override
    public void initDT(DataTracker.Builder builder) {

        builder.add(IS_CONTROLLED, false);
        builder.add(CONTROLLING_PLAYER_UUID, Optional.empty());
    }

    @Override
    public void readNBT(NbtCompound nbt) {
        if (nbt.contains("ControllingPlayer", NbtElement.INT_ARRAY_TYPE)) {
            UUID controllingPlayerUuid = nbt.getUuid("ControllingPlayer");
            this.controllingPlayerId = controllingPlayerUuid;
            parent.getDataTracker().set(CONTROLLING_PLAYER_UUID, Optional.of(controllingPlayerUuid));
        }

        if (nbt.contains("IsControlled", NbtElement.BYTE_TYPE)) {
            boolean isControlled = nbt.getBoolean("IsControlled");
            parent.getDataTracker().set(IS_CONTROLLED, isControlled);
        }
        if (nbt.contains("ControlTargetX", NbtElement.DOUBLE_TYPE)) {
            double x = nbt.getDouble("ControlTargetX");
            double y = nbt.getDouble("ControlTargetY");
            double z = nbt.getDouble("ControlTargetZ");
            this.targetControlPos = new Vec3d(x, y, z);
        }
    }

    @Override
    public void writeNBT(NbtCompound nbt) {
        if (this.controllingPlayerId != null) {
            nbt.putUuid("ControllingPlayer", this.controllingPlayerId);
        }
        nbt.putBoolean("IsControlled", this.isControlled());
        if (this.targetControlPos != null) {
            nbt.putDouble("ControlTargetX", this.targetControlPos.x);
            nbt.putDouble("ControlTargetY", this.targetControlPos.y);
            nbt.putDouble("ControlTargetZ", this.targetControlPos.z);
        }
        nbt.putBoolean("JustLaunched", justLaunched);
        nbt.putInt("LaunchCooldown", launchCooldown);

    }

    public void setControllingPlayer(@Nullable PlayerEntity player) {
        if (player != null) {
            this.controllingPlayerId = player.getUuid();
            parent.getDataTracker().set(CONTROLLING_PLAYER_UUID, Optional.of(player.getUuid()));
            parent.getDataTracker().set(IS_CONTROLLED, true);

            // 启用发光效果
            parent.setGlowing(true);

            // 在操纵模式下，取消跟随目标
            if(parent.Components.get(TargetableComponent.class) instanceof TargetableComponent t){
                t.setTarget(null);
            };



            // 设置无重力，便于操纵
            parent.setNoGravity(true);

            // 初始位置：玩家面前一定距离
            Vec3d lookDirection = player.getRotationVec(1.0F);
            Vec3d eyePos = player.getEyePos();
            this.targetControlPos = eyePos.add(lookDirection.multiply(CONTROL_DISTANCE));

            // 平滑移动到目标位置
            Vec3d toTarget = targetControlPos.subtract(parent.getPos());
            double distance = toTarget.length();
            if (distance > 0.1) {
                Vec3d velocity = toTarget.normalize().multiply(CONTROL_SPEED);
                parent.setVelocity(velocity);
            } else {
                parent.setVelocity(Vec3d.ZERO);
            }

            // 清除发射状态
            justLaunched = false;
            launchCooldown = 0;

        } else {
            this.controllingPlayerId = null;
            parent.getDataTracker().set(CONTROLLING_PLAYER_UUID, Optional.empty());
            parent.getDataTracker().set(IS_CONTROLLED, false);

            // 取消发光效果
            parent.setGlowing(false);

            // 恢复重力
            parent.setNoGravity(false);

            // 重置控制目标位置
            targetControlPos = parent.getPos();
        }
    }

    public boolean isControlledBy(PlayerEntity player) {
        if (player == null || this.controllingPlayerId == null) {
            return false;
        }
        return player.getUuid().equals(this.controllingPlayerId);
    }

    public boolean isControlled() {
        return parent.getDataTracker().get(IS_CONTROLLED);
    }


    /**
     * 刷新控制状态
     */
    private void refreshControlState() {
        if (isControlled()) {
            Optional<UUID> uuid = parent.getDataTracker().get(CONTROLLING_PLAYER_UUID);
            if (uuid.isPresent()) {
                PlayerEntity player = parent.getWorld().getPlayerByUuid(uuid.get());
                if (player != null && player.isAlive()) {
                    this.controllingPlayerId = player.getUuid();
                } else {
                    // 玩家不存在或已死亡，取消控制
                    setControllingPlayer(null);
                }
            } else {
                setControllingPlayer(null);
            }
        }
    }

    @Nullable
    public PlayerEntity getControllingPlayer() {
        if (this.controllingPlayerId == null) {
            return null;
        }
        return parent.getWorld().getPlayerByUuid(this.controllingPlayerId);
    }
}
