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

public class ControlableComponent extends EntityComponents {
    private UUID controllingPlayerId;
    private Vec3d targetControlPos;
    private boolean justLaunched = false;
    private int launchCooldown = 0;

    // 操纵模式参数
    private static final float LAUNCH_SPEED = 20.0f;
    private static final float CONTROL_DISTANCE = 5.0f;
    private static final float CONTROL_SPEED = 0.3f;
    private static final float CONTROL_SMOOTHING = 0.2f;

    // 数据跟踪器
    private static final TrackedData<Boolean> IS_CONTROLLED =
            DataTracker.registerData(FollowingEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Optional<UUID>> CONTROLLING_PLAYER_UUID =
            DataTracker.registerData(FollowingEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);

    public ControlableComponent(BaseBlockEntity parent) {
        super(parent);
        this.targetControlPos = parent.getPos();
    }

    // ================= 公共方法 =================

    /**
     * 发射实体
     */
    public void launchEntity(Vec3d direction) {
        Vec3d launchVelocity = direction.multiply(LAUNCH_SPEED);
        parent.setVelocity(launchVelocity);
        justLaunched = true;
        launchCooldown = 10;

        // 取消控制
        setControllingPlayer(null);
        parent.setNoGravity(false);
    }

    /**
     * 设置控制玩家
     */
    public void setControllingPlayer(@Nullable PlayerEntity player) {
        if (player != null) {
            this.controllingPlayerId = player.getUuid();
            parent.getDataTracker().set(CONTROLLING_PLAYER_UUID, Optional.of(player.getUuid()));
            parent.getDataTracker().set(IS_CONTROLLED, true);
            parent.setGlowing(true);

            // 取消跟随目标
            if (parent.Components.get(TargetableComponent.class) instanceof TargetableComponent targetable) {
                targetable.setTarget(null);
            }

            // 设置无重力
            parent.setNoGravity(true);

            // 初始化控制位置
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

            // 重置发射状态
            justLaunched = false;
            launchCooldown = 0;

        } else {
            this.controllingPlayerId = null;
            parent.getDataTracker().set(CONTROLLING_PLAYER_UUID, Optional.empty());
            parent.getDataTracker().set(IS_CONTROLLED, false);
            parent.setGlowing(false);
            parent.setNoGravity(false);
            this.targetControlPos = parent.getPos();
        }
    }

    /**
     * 检查是否被指定玩家控制
     */
    public boolean isControlledBy(PlayerEntity player) {
        if (player == null || this.controllingPlayerId == null) {
            return false;
        }
        return player.getUuid().equals(this.controllingPlayerId);
    }

    /**
     * 检查是否被控制
     */
    public boolean isControlled() {
        return parent.getDataTracker().get(IS_CONTROLLED);
    }

    /**
     * 获取控制玩家
     */
    @Nullable
    public PlayerEntity getControllingPlayer() {
        if (this.controllingPlayerId == null) {
            return null;
        }
        return parent.getWorld().getPlayerByUuid(this.controllingPlayerId);
    }

    // ================= 组件生命周期 =================

    @Override
    public boolean tick() {
        // 更新发射冷却
        if (launchCooldown > 0) {
            launchCooldown--;
        }

        // 刷新控制状态
        refreshControlState();

        // 执行控制逻辑
        if (isControlled()) {
            tickControlled();
            return true;
        }
        return false;
    }

    @Override
    public void initDT(DataTracker.Builder builder) {
        builder.add(IS_CONTROLLED, false);
        builder.add(CONTROLLING_PLAYER_UUID, Optional.empty());
    }

    @Override
    public void readNBT(NbtCompound nbt) {
        if (nbt.contains("ControllingPlayer", NbtElement.INT_ARRAY_TYPE)) {
            this.controllingPlayerId = nbt.getUuid("ControllingPlayer");
            parent.getDataTracker().set(CONTROLLING_PLAYER_UUID, Optional.of(this.controllingPlayerId));
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

        if (nbt.contains("JustLaunched", NbtElement.BYTE_TYPE)) {
            justLaunched = nbt.getBoolean("JustLaunched");
        }

        if (nbt.contains("LaunchCooldown", NbtElement.INT_TYPE)) {
            launchCooldown = nbt.getInt("LaunchCooldown");
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

    @Override
    public void OnSpawn() {

    }

    // ================= 私有方法 =================

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
                    setControllingPlayer(null);
                }
            } else {
                setControllingPlayer(null);
            }
        }
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
            double speed = Math.min(distance * CONTROL_SPEED, 1.0);
            Vec3d velocity = toTarget.normalize().multiply(speed);
            parent.setVelocity(velocity);
        } else {
            parent.setVelocity(Vec3d.ZERO);
        }

        // 应用移动
        parent.move(MovementType.SELF, parent.getVelocity());
    }
}