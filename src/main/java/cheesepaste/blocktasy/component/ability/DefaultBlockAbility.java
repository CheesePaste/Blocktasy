package cheesepaste.blocktasy.component.ability;

import cheesepaste.blocktasy.Blocktasy;
import cheesepaste.blocktasy.entity.BaseBlockEntity;
import cheesepaste.blocktasy.entity.FollowingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.nbt.NbtCompound;

public class DefaultBlockAbility extends BlockAbilityComponent{


    public DefaultBlockAbility(BaseBlockEntity baseBlockEntity) {
        super(baseBlockEntity);
    }
    public DefaultBlockAbility(){
        super(null);
    }

    @Override
    public void OnSpawn() {
        BlockAbilityComponent ability=Blocktasy.abilityMap.get(parent.getBlockState());
        if(ability!=null)
        {
            parent.Components.replace(BlockAbilityComponent.class,ability);
            ability.OnSpawn();
        }
    }

    @Override
    public void registerDataTracker() {

    }

    @Override
    ActionResult Attack() {
        return ActionResult.Success;
    }

    @Override
    void OnDestroy() {

    }

    @Override
    public boolean tick() {
        return false;
    }

    @Override
    public void initDT(DataTracker.Builder builder) {

    }

    @Override
    public void readNBT(NbtCompound nbt) {

    }

    @Override
    public void writeNBT(NbtCompound nbt) {

    }
}
