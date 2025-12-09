package cheesepaste.blocktasy.entity;

import cheesepaste.blocktasy.component.ControlableComponent;
import cheesepaste.blocktasy.component.EntityComponents;
import cheesepaste.blocktasy.component.TargetableComponent;
import cheesepaste.blocktasy.component.ability.BlockAbilityComponent;
import cheesepaste.blocktasy.component.ability.DefaultBlockAbility;
import com.llamalad7.mixinextras.lib.antlr.runtime.misc.FlexibleHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Arm;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import java.util.Collections;
import java.util.HashMap;

public class FollowingEntity extends BaseBlockEntity {


    static {
        initializeComponents();
        Components.forEach((k,v)->{
            v.registerDataTracker();
        });
    }

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
        Components.forEach((k,v)->{
            v.setParent(this);
        });
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
        Components.forEach((k,v)->{
            v.setParent(this);
        });
        OnSpawn();
    }

    private static void initializeComponents() {
        Components=new HashMap<>();

        Components.put(TargetableComponent.class, new TargetableComponent());
        Components.put(ControlableComponent.class, new ControlableComponent() );
        Components.put(BlockAbilityComponent.class, new DefaultBlockAbility());
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