package cheesepaste.blocktasy.entity;

import cheesepaste.blocktasy.ability.BlockAbility;
import cheesepaste.blocktasy.component.ControlableComponent;
import cheesepaste.blocktasy.component.TargetableComponent;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

import net.minecraft.util.Arm;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

/**
 * 跟随实体类
 * 跟随目标实体移动的方块实体
 */
public class FollowingEntity extends BaseBlockEntity {

    // 配置参数

    private static final float HORIZONTAL_SPEED = 0.15f;
    private static final float JUMP_STRENGTH = 0.5f;
    private static final int JUMP_COOLDOWN = 20;
    private static final float MAX_SPEED = 0.5f;
    private static final float AIR_RESISTANCE = 0.98f;
    private static final float ROTATION_SPEED = 10.0f; // 旋转速度（度/秒）
    private static final float ROTATION_INTERPOLATION_FACTOR = 0.2f; // 旋转插值因子

    // 操纵模式参数
    private static final float CONTROL_DISTANCE = 5.0f; // 操纵时的默认距离
    private static final float CONTROL_SPEED = 0.3f; // 操纵移动速度
    private static final float CONTROL_SMOOTHING = 0.2f; // 操纵平滑因子
    private static final float LAUNCH_SPEED = 20.0f; // 发射速度

    // 数据跟踪
    private static final TrackedData<Optional<UUID>> TARGET_UUID =
            DataTracker.registerData(FollowingEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Float> TARGET_YAW =
            DataTracker.registerData(FollowingEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> CURRENT_YAW =
            DataTracker.registerData(FollowingEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> IS_CONTROLLED =
            DataTracker.registerData(FollowingEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Optional<UUID>> CONTROLLING_PLAYER_UUID =
            DataTracker.registerData(FollowingEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);

    // 状态字段
    @Nullable
    //private Entity target;
//    private int jumpTimer = 0;
//    private boolean shouldJump = false;
    private int targetLostCounter = 0;
    //private static final int MAX_TARGET_LOST_TICKS = 200; // 10秒后放弃跟随

    // 操纵模式状态
    @Nullable
    private UUID controllingPlayerId;

    // 旋转状态
    private float prevRenderYaw; // 用于渲染插值

    // 控制状态
    private Vec3d targetControlPos; // 控制目标位置
    private boolean justLaunched = false; // 刚刚被发射
    private int launchCooldown = 0; // 发射冷却

    public BlockAbility ability;
    private boolean shouldJump;
    private int jumpTimer;

    // ================= 构造方法 =================

    void Colli(){
        enableCollision = true;
        attackable = false;
        fireImmune = true;
        aliveCheckFromRemoved = true;
        pushOutOfBlocksEnabled = true;
        onBlockCollisionEnabled = true;
        collidesWithOtherEntities = true;
        moveVoluntarily = true;
        collidesWithBlockStates = true;
        checkBlockCollisionEnabled = true;
        adjustForPistonEnabled = true;
        adjustForSneakingEnabled = true;
        hittable = true;
        pushAwayEnabled = true;
        noGravity = false;
        collidable = true;
        pushable = true;
    }

    public FollowingEntity(EntityType<? extends BaseBlockEntity> type, World world, @Nullable Entity target,
                           @NotNull BlockPos pos, @NotNull BlockState state) {
        super(type, world, pos, state);
        //this.target = target;
        Colli();
        this.MAX_TRAIL_LENGTH = 200;
        this.targetControlPos = this.getPos();

        // 初始化旋转
        this.prevRenderYaw = 0;
        this.Components.put(TargetableComponent.class,new TargetableComponent(this));
        this.Components.put(ControlableComponent.class,new ControlableComponent(this));
    }

    public static DefaultAttributeContainer.Builder createFollowingAttributes() {
        return BaseBlockEntity.createBaseBlockAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0) // 更高的生命值
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25) // 移动速度
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0) // 完全抵抗击退
                .add(EntityAttributes.GENERIC_GRAVITY, 0.08) // 重力
                .add(EntityAttributes.GENERIC_STEP_HEIGHT, 1.0) // 可以走上1格高的方块
                .add(EntityAttributes.GENERIC_SAFE_FALL_DISTANCE, 3.0) // 安全坠落距离
                .add(EntityAttributes.GENERIC_FALL_DAMAGE_MULTIPLIER, 0.0); // 无坠落伤害
    }

    public FollowingEntity(EntityType<FollowingEntity> type, World world) {
        super(type, world);
        this.setNoGravity(false);
        Colli();
        this.MAX_TRAIL_LENGTH = 200;
        this.targetControlPos = this.getPos();

        // 初始化旋转
        this.prevRenderYaw = 0;
        this.Components.put(TargetableComponent.class,new TargetableComponent(this));
        this.Components.put(ControlableComponent.class,new ControlableComponent(this));
    }

    // ================= 数据跟踪 =================

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);

        builder.add(TARGET_YAW, 0f);
        builder.add(CURRENT_YAW, 0f);



    }

    // ================= 目标管理 =================

//    public void setTarget(@Nullable Entity target) {
//        Entity oldTarget = this.target;
//        this.target = target;
//
//        if (target != null) {
//            this.dataTracker.set(TARGET_UUID, Optional.of(target.getUuid()));
//            targetLostCounter = 0;
//        } else {
//            this.dataTracker.set(TARGET_UUID, Optional.empty());
//        }
//    }

//    @Nullable
//    public Entity getTarget() {
//        return target;
//    }

//    private void refreshTarget() {
//        Optional<UUID> uuid = this.dataTracker.get(TARGET_UUID);
//
//        if (uuid.isPresent()) {
//            Entity entity = this.getWorld().getPlayerByUuid(uuid.get());
//            if (entity == null) {
//                // 如果不是玩家，尝试查找其他实体
//                for (Entity e : this.getWorld().getPlayers()) {
//                    if (e.getUuid().equals(uuid.get())) {
//                        entity = e;
//                        break;
//                    }
//                }
//            }
//
//            if (entity != null && entity.isAlive()) {
//                this.target = entity;
//                targetLostCounter = 0;
//            } else {
//                targetLostCounter++;
//                if (targetLostCounter > MAX_TARGET_LOST_TICKS) {
//                    this.target = null;
//                    this.dataTracker.set(TARGET_UUID, Optional.empty());
//                }
//            }
//        }
//    }

    // ================= 操纵模式管理 =================

//    public void setControllingPlayer(@Nullable PlayerEntity player) {
//        if (player != null) {
//            this.controllingPlayerId = player.getUuid();
//            this.dataTracker.set(CONTROLLING_PLAYER_UUID, Optional.of(player.getUuid()));
//            this.dataTracker.set(IS_CONTROLLED, true);
//
//            // 启用发光效果
//            setGlowing(true);
//
//            // 在操纵模式下，取消跟随目标
//            this.setTarget(null);
//
//            // 设置无重力，便于操纵
//            this.setNoGravity(true);
//
//            // 初始位置：玩家面前一定距离
//            Vec3d lookDirection = player.getRotationVec(1.0F);
//            Vec3d eyePos = player.getEyePos();
//            this.targetControlPos = eyePos.add(lookDirection.multiply(CONTROL_DISTANCE));
//
//            // 平滑移动到目标位置
//            Vec3d toTarget = targetControlPos.subtract(this.getPos());
//            double distance = toTarget.length();
//            if (distance > 0.1) {
//                Vec3d velocity = toTarget.normalize().multiply(CONTROL_SPEED);
//                this.setVelocity(velocity);
//            } else {
//                this.setVelocity(Vec3d.ZERO);
//            }
//
//            // 清除发射状态
//            justLaunched = false;
//            launchCooldown = 0;
//
//        } else {
//            this.controllingPlayerId = null;
//            this.dataTracker.set(CONTROLLING_PLAYER_UUID, Optional.empty());
//            this.dataTracker.set(IS_CONTROLLED, false);
//
//            // 取消发光效果
//            setGlowing(false);
//
//            // 恢复重力
//            this.setNoGravity(false);
//
//            // 重置控制目标位置
//            this.targetControlPos = this.getPos();
//        }
//    }

    public boolean isControlledBy(PlayerEntity player) {
        if (player == null || this.controllingPlayerId == null) {
            return false;
        }
        return player.getUuid().equals(this.controllingPlayerId);
    }

    public boolean isControlled() {
        return this.dataTracker.get(IS_CONTROLLED);
    }

    @Nullable
    public PlayerEntity getControllingPlayer() {
        if (this.controllingPlayerId == null) {
            return null;
        }
        return this.getWorld().getPlayerByUuid(this.controllingPlayerId);
    }

    // ================= 发射功能 =================







    // ================= NBT序列化 =================

    @Override
    public void writeCustomDataToNbt(@NotNull NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);



        // 保存操纵状态



        // 保存控制目标位置




        nbt.putInt("JumpTimer", this.jumpTimer);
        nbt.putBoolean("ShouldJump", this.shouldJump);

        nbt.putFloat("TargetYaw", this.dataTracker.get(TARGET_YAW));
        nbt.putFloat("CurrentYaw", this.dataTracker.get(CURRENT_YAW));
    }

    @Override
    public void readCustomDataFromNbt(@NotNull NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);



        // 加载操纵状态


        if (nbt.contains("JumpTimer", NbtElement.INT_TYPE)) {
            this.jumpTimer = nbt.getInt("JumpTimer");
        }

        if (nbt.contains("ShouldJump", NbtElement.BYTE_TYPE)) {
            this.shouldJump = nbt.getBoolean("ShouldJump");
        }

        // 加载控制目标位置


        if (nbt.contains("JustLaunched", NbtElement.BYTE_TYPE)) {
            justLaunched = nbt.getBoolean("JustLaunched");
        }

        if (nbt.contains("LaunchCooldown", NbtElement.INT_TYPE)) {
            launchCooldown = nbt.getInt("LaunchCooldown");
        }



        if (nbt.contains("TargetLostCounter", NbtElement.INT_TYPE)) {
            this.targetLostCounter = nbt.getInt("TargetLostCounter");
        }

        if (nbt.contains("TargetYaw", NbtElement.FLOAT_TYPE)) {
            this.dataTracker.set(TARGET_YAW, nbt.getFloat("TargetYaw"));
        }

        if (nbt.contains("CurrentYaw", NbtElement.FLOAT_TYPE)) {
            this.dataTracker.set(CURRENT_YAW, nbt.getFloat("CurrentYaw"));
        }

        // 同步当前yaw
        this.setYaw(this.dataTracker.get(CURRENT_YAW));
        this.prevRenderYaw = this.getYaw();

    }

    @Override
    public Iterable<ItemStack> getArmorItems() {
        return Collections.singleton(ItemStack.EMPTY);
    }

    @Override
    public ItemStack getEquippedStack(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void equipStack(EquipmentSlot slot, ItemStack stack) {
        // 不穿戴装备
    }

    // ================= 旋转系统 =================

    /**
     * 获取用于渲染的插值yaw角度
     */
    public float getRenderYaw(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.prevRenderYaw, this.getYaw());
    }

    // ================= 主逻辑 =================

    @Override
    public void tick() {
        // 先调用父类的tick，这会更新位置、速度、重力等
        super.tick();

        // 更新发射冷却->组件中




        // 检查控制状态->组件中


        // 如果被控制，执行控制逻辑->组件
//        if (isControlled()) {
//            tickControlled();
//            return;
//        }

        // 确保目标是最新的 ->移动到组件中


        // 更新旋转系统
//        if (this.target != null) {
//            calculateTargetRotation();
//            updateRotation();
//        }

        // 如果没有目标，停止移动->components


        // 如果距离足够近，停止水平移动->com

        //processMovement();


    }

    @Override
    public Arm getMainArm() {
        return Arm.RIGHT;
    }

    // ================= 控制逻辑 =================

//    /**
//     * 刷新控制状态
//     */
//    private void refreshControlState() {
//        if (isControlled()) {
//            Optional<UUID> uuid = this.dataTracker.get(CONTROLLING_PLAYER_UUID);
//            if (uuid.isPresent()) {
//                PlayerEntity player = this.getWorld().getPlayerByUuid(uuid.get());
//                if (player != null && player.isAlive()) {
//                    this.controllingPlayerId = player.getUuid();
//                } else {
//                    // 玩家不存在或已死亡，取消控制
//                    setControllingPlayer(null);
//                }
//            } else {
//                setControllingPlayer(null);
//            }
//        }
//    }



    // ================= 粒子效果 =================






    @Override
    protected double getGravity() {
        return 0.08;
    }



    // ================= Targetable接口实现 =================


    //COMPONENT

    // ================= 其他方法 =================

    @Override
    protected void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {
        super.fall(heightDifference, onGround, state, landedPosition);

        if (onGround) {
            // 重置跳跃状态
            this.shouldJump = false;
            // 重置发射状态
            this.justLaunched = false;
        }
    }

    @Override
    public boolean hasNoGravity() {
        return this.isControlled(); // 被控制时无重力
    }

//    @Override
//    public String toString() {
//        return String.format("FollowingEntity{id=%d, pos=%s, target=%s, yaw=%.2f, controlled=%s}",
//                getId(), getPos(),
//                target != null ? target.getName().getString() : "null",
//                this.getYaw(),
//                isControlled() ? "true" : "false");
//    }
}