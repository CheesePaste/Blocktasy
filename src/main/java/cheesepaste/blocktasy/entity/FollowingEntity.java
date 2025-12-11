package cheesepaste.blocktasy.entity;

import cheesepaste.blocktasy.component.ControlableComponent;
import cheesepaste.blocktasy.component.EntityComponents;
import cheesepaste.blocktasy.component.TargetableComponent;
import cheesepaste.blocktasy.component.ability.BlockAbilityComponent;
import cheesepaste.blocktasy.component.ability.DefaultBlockAbility;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Arm;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

public class FollowingEntity extends BaseBlockEntity {


//    static {
//        initializeComponents();
//        Components.forEach((k,v)->{
//            v.registerDataTracker();
//        });
//    }
    private static final TrackedData<Optional<UUID>> TARGET_UUID = DataTracker.registerData(FollowingEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Boolean> IS_CONTROLLED=DataTracker.registerData(FollowingEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Optional<UUID>> CONTROLLING_PLAYER_UUID=DataTracker.registerData(FollowingEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);


    void InitCollider() {
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

    public FollowingEntity(EntityType<? extends BaseBlockEntity> type, World world,
                           @NotNull BlockPos pos, @NotNull BlockState state) {
        super(type, world, pos, state);
        InitCollider();
        this.MAX_TRAIL_LENGTH = 200;
//        Components.forEach((k,v)->{
//            v.setParent(this);
//        });
        initializeComponents();
        OnSpawn();
    }

    public static DefaultAttributeContainer.Builder createFollowingAttributes() {
        return BaseBlockEntity.createBaseBlockAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
                .add(EntityAttributes.GENERIC_GRAVITY, 0.08)
                .add(EntityAttributes.GENERIC_STEP_HEIGHT, 1.0)
                .add(EntityAttributes.GENERIC_SAFE_FALL_DISTANCE, 3.0)
                .add(EntityAttributes.GENERIC_FALL_DAMAGE_MULTIPLIER, 0.0);
    }

    public FollowingEntity(EntityType<FollowingEntity> type, World world) {
        super(type, world);
        this.setNoGravity(false);
        InitCollider();
        this.MAX_TRAIL_LENGTH = 200;
//        Components.forEach((k,v)->{
//            v.setParent(this);
//        });
        initializeComponents();
        OnSpawn();
    }

    private void initializeComponents() {

        Components=new HashMap<>();
        Components.put(TargetableComponent.class, new TargetableComponent(this,TARGET_UUID));
        Components.put(ControlableComponent.class, new ControlableComponent(this,IS_CONTROLLED,CONTROLLING_PLAYER_UUID) );
        Components.put(BlockAbilityComponent.class, new DefaultBlockAbility(this));
//        for(EntityComponents components : Components.values())
//        {
//            components.initDT(builder);
//        }
    }
    private void OnSpawn()
    {
        for(EntityComponents components : Components.values())
        {
            components.OnSpawn();
        }
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {

        builder.add(TARGET_UUID, Optional.empty());
        builder.add(IS_CONTROLLED, false);
        builder.add(CONTROLLING_PLAYER_UUID, Optional.empty());
        super.initDataTracker(builder);
    }

    @Override
    public void writeCustomDataToNbt(@NotNull NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
    }

    @Override
    public void readCustomDataFromNbt(@NotNull NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
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
    public void equipStack(EquipmentSlot slot, ItemStack stack) {}

    @Override
    public void tick() {
        super.tick();

        for(EntityComponents components : Components.values())
        {
            components.tick();
        }
    }

    @Override
    public Arm getMainArm() {
        return Arm.RIGHT;
    }

    @Override
    protected double getGravity() {
        return 0.08;
    }

    @Override
    public boolean hasNoGravity() {
        if(this.Components.get(ControlableComponent.class) instanceof ControlableComponent controlableComponent)
        {
            return controlableComponent.isControlled();
        }
        return false;
    }

    @Override
    protected void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {
        super.fall(heightDifference, onGround, state, landedPosition);
        if(this.Components.get(TargetableComponent.class) instanceof TargetableComponent targetableComponent)
        {
            targetableComponent.shouldJump=false;
        }
    }
}